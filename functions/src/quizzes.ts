import { FieldValue } from "firebase-admin/firestore";
import { GoogleGenAI } from "@google/genai";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  MAX_QUESTION_COUNT,
  DEFAULT_TOPIC,
  FUNCTIONS_REGION,
  MODEL_NAME,
  QUIZZES_COLLECTION,
  ROOMS_COLLECTION,
  CEFR_LEVELS
} from "./shared/config.js";
import { db, geminiApiKey } from "./shared/firebase.js";
import {
  FillInBlankGeneratedQuestion,
  CefrLevel,
  GeneratedQuiz,
  MultipleChoiceGeneratedQuestion,
  QuizKind,
  QuestionType,
  RoomDocument,
  WordScrambleGeneratedQuestion
} from "./shared/types.js";
import {
  normalizeCefrLevel,
  normalizeQuizKind,
  normalizeQuestionCount,
  normalizeQuestionTimeLimitSeconds,
  normalizeQuestionType,
  readStringList,
  readRequiredString
} from "./shared/validation.js";

// This file contains the quiz-generation flow powered by the Gemini model.
export const generateQuizForRoom = onCall(
  {
    cors: true,
    region: FUNCTIONS_REGION,
    secrets: [geminiApiKey]
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const clientRequestId = typeof request.data.clientRequestId === "string"
      ? request.data.clientRequestId.trim()
      : "";
    const roomId = readRequiredString(request.data.roomId, "roomId");
    const title = readRequiredString(request.data.title, "title");
    const quizKind = normalizeQuizKind(request.data.quizKind);
    const topic = typeof request.data.topic === "string" ? request.data.topic.trim() : "";
    const vocabularyWords = quizKind === "vocabulary"
      ? deduplicateWords(readStringList(request.data.vocabularyWords, "vocabularyWords"))
      : [];
    if (quizKind === "vocabulary" && vocabularyWords.length > MAX_QUESTION_COUNT) {
      throw new HttpsError(
        "invalid-argument",
        `vocabularyWords can contain at most ${MAX_QUESTION_COUNT} values.`
      );
    }
    const normalizedTopic = quizKind === "grammar"
      ? readRequiredString(topic, "topic")
      : buildVocabularyTopic(vocabularyWords);
    const questionCount = quizKind === "vocabulary"
      ? vocabularyWords.length
      : normalizeQuestionCount(request.data.questionCount);
    const questionType = quizKind === "vocabulary"
      ? "word_scramble"
      : normalizeQuestionType(request.data.questionType);
    if (questionType === "mixed" && questionCount < 2) {
      throw new HttpsError("invalid-argument", "Mixed quizzes need at least 2 questions.");
    }
    const questionTimeLimitSeconds = normalizeQuestionTimeLimitSeconds(request.data.questionTimeLimitSeconds);

    const roomRef = db.collection(ROOMS_COLLECTION).doc(roomId);
    const roomSnapshot = await roomRef.get();

    if (!roomSnapshot.exists) {
      throw new HttpsError("not-found", "Room was not found.");
    }

    const room = roomSnapshot.data() as RoomDocument;
    if (!room.ownerId) {
      throw new HttpsError("failed-precondition", "Room is missing owner information.");
    }
    const roomCefrLevel = normalizeCefrLevel(room.cefrLevel);
    const collaboratorIds = Array.isArray(room.collaboratorIds) ? room.collaboratorIds : [];
    if (room.ownerId !== request.auth.uid && !collaboratorIds.includes(request.auth.uid)) {
      throw new HttpsError("permission-denied", "Only the room owner or a collaborator can generate a quiz.");
    }

    const quizRef = roomRef.collection(QUIZZES_COLLECTION).doc();
    const now = Date.now();
    await quizRef.set({
      id: quizRef.id,
      clientRequestId,
      roomId,
      roomName: room.name || "Untitled room",
      quizKind,
      topic: normalizedTopic || room.topic || DEFAULT_TOPIC,
      vocabularyWords,
      cefrLevel: roomCefrLevel,
      title,
      questionType,
      questionTimeLimitSeconds,
      questions: [],
      questionCount,
      failureReason: null,
      status: "generating",
      createdBy: request.auth.uid,
      createdAt: FieldValue.serverTimestamp(),
      createdAtEpochMillis: now,
      updatedAtEpochMillis: now
    });

    try {
      const quiz = await generateQuiz({
        quizKind,
        topic: normalizedTopic || room.topic || DEFAULT_TOPIC,
        vocabularyWords,
        roomName: room.name || "Untitled room",
        cefrLevel: roomCefrLevel,
        questionCount,
        questionType
      });

      await quizRef.update({
        quizKind,
        topic: normalizedTopic || room.topic || DEFAULT_TOPIC,
        vocabularyWords,
        cefrLevel: quiz.cefrLevel,
        title,
        questionType,
        questions: quiz.questions.map((question, index) =>
          toStoredQuestion(question, questionType, index, questionTimeLimitSeconds)
        ),
        questionCount: quiz.questions.length,
        failureReason: null,
        status: "review",
        updatedAtEpochMillis: Date.now()
      });
    } catch (error) {
      const failureReason = toQuizGenerationFailureReason(error);
      await quizRef.update({
        failureReason,
        status: "failed",
        updatedAtEpochMillis: Date.now()
      });

      throw toClientVisibleQuizError(error, failureReason);
    }

    try {
      await roomRef.update({
        unansweredQuizCount: FieldValue.increment(1)
      });
    } catch {
      // Keep the generated quiz usable even if this secondary room counter update fails.
    }

    return {
      ok: true,
      roomId,
      quizId: quizRef.id,
      cefrLevel: roomCefrLevel,
      questionCount,
      questionType
    };
  }
);

export const retryQuizForRoom = onCall(
  {
    cors: true,
    region: FUNCTIONS_REGION,
    secrets: [geminiApiKey]
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const roomId = readRequiredString(request.data.roomId, "roomId");
    const quizId = readRequiredString(request.data.quizId, "quizId");
    const { roomRef, room, roomCefrLevel } = await loadRoomForQuizGeneration(roomId, request.auth.uid);
    const quizRef = roomRef.collection(QUIZZES_COLLECTION).doc(quizId);
    const quizSnapshot = await quizRef.get();

    if (!quizSnapshot.exists) {
      throw new HttpsError("not-found", "Quiz was not found.");
    }

    const quiz = quizSnapshot.data() as Record<string, unknown>;
    const title = readRequiredString(quiz.title, "title");
    const quizKind = normalizeQuizKind(quiz.quizKind);
    const vocabularyWords = quizKind === "vocabulary"
      ? deduplicateWords(readStringList(quiz.vocabularyWords, "vocabularyWords"))
      : [];
    const normalizedTopic = quizKind === "grammar"
      ? readRequiredString(quiz.topic, "topic")
      : buildVocabularyTopic(vocabularyWords);
    const questionCount = quizKind === "vocabulary"
      ? vocabularyWords.length
      : normalizeQuestionCount(quiz.questionCount);
    const questionType = quizKind === "vocabulary"
      ? "word_scramble"
      : normalizeQuestionType(quiz.questionType);
    if (questionType === "mixed" && questionCount < 2) {
      throw new HttpsError("invalid-argument", "Mixed quizzes need at least 2 questions.");
    }
    const questionTimeLimitSeconds = normalizeQuestionTimeLimitSeconds(quiz.questionTimeLimitSeconds);
    const status = typeof quiz.status === "string" ? quiz.status : "";

    if (status !== "failed") {
      throw new HttpsError("failed-precondition", "Only failed quizzes can be retried.");
    }

    await quizRef.update({
      topic: normalizedTopic || room.topic || DEFAULT_TOPIC,
      vocabularyWords,
      cefrLevel: roomCefrLevel,
      title,
      questionType,
      questions: [],
      questionCount,
      failureReason: null,
      status: "generating",
      updatedAtEpochMillis: Date.now()
    });

    try {
      const generatedQuiz = await generateQuiz({
        quizKind,
        topic: normalizedTopic || room.topic || DEFAULT_TOPIC,
        vocabularyWords,
        roomName: room.name || "Untitled room",
        cefrLevel: roomCefrLevel,
        questionCount,
        questionType
      });

      await quizRef.update({
        quizKind,
        topic: normalizedTopic || room.topic || DEFAULT_TOPIC,
        vocabularyWords,
        cefrLevel: generatedQuiz.cefrLevel,
        title,
        questionType,
        questions: generatedQuiz.questions.map((question, index) =>
          toStoredQuestion(question, questionType, index, questionTimeLimitSeconds)
        ),
        questionCount: generatedQuiz.questions.length,
        failureReason: null,
        status: "review",
        updatedAtEpochMillis: Date.now()
      });
    } catch (error) {
      const failureReason = toQuizGenerationFailureReason(error);
      await quizRef.update({
        failureReason,
        status: "failed",
        updatedAtEpochMillis: Date.now()
      });

      throw toClientVisibleQuizError(error, failureReason);
    }

    try {
      await roomRef.update({
        unansweredQuizCount: FieldValue.increment(1)
      });
    } catch {
      // Keep the generated quiz usable even if this secondary room counter update fails.
    }

    return {
      ok: true,
      roomId,
      quizId,
      cefrLevel: roomCefrLevel,
      questionCount,
      questionType
    };
  }
);

async function generateQuiz(input: {
  quizKind: QuizKind;
  topic: string;
  vocabularyWords: string[];
  roomName: string;
  cefrLevel: CefrLevel;
  questionCount: number;
  questionType: QuestionType;
}): Promise<GeneratedQuiz> {
  const ai = new GoogleGenAI({
    apiKey: geminiApiKey.value()
  });

  const response = await ai.models.generateContent({
    model: MODEL_NAME,
    contents: buildQuizPrompt(input),
    config: {
      responseMimeType: "application/json",
      responseSchema: buildQuizResponseSchema(input.questionCount, input.questionType)
    }
  });

  const text = response.text?.trim();
  if (!text) {
    throw new HttpsError("internal", "AI returned an empty response.");
  }

  const parsed = parseQuizJson(text);
  validateQuiz(parsed, input.cefrLevel, input.questionCount, input.questionType);
  return parsed;
}

// The prompt is extracted into a helper so the instructions can evolve without touching the handler.
function buildQuizPrompt(input: {
  quizKind: QuizKind;
  topic: string;
  vocabularyWords: string[];
  roomName: string;
  cefrLevel: CefrLevel;
  questionCount: number;
  questionType: QuestionType;
}): string {
  if (input.quizKind === "vocabulary") {
    return buildVocabularyQuizPrompt(input);
  }

  const grammarFocusInstructions = [
    `Requested grammar or language focus: ${input.topic}`,
    "- The quiz must focus specifically on the requested grammar or language focus.",
    "- If the requested focus is a grammar structure such as conditionals, passive voice, reported speech, or tenses, every question must directly test that structure.",
    "- Do not drift into generic vocabulary questions unless the requested focus is explicitly vocabulary-based.",
    "- Use natural, learner-friendly English examples that clearly demonstrate the requested focus."
  ];

  const questionTypeInstructions = input.questionType === "fill_in_blank"
    ? [
      `Create exactly ${input.questionCount} fill-in-the-blank quiz questions.`,
      "Use this exact schema:",
      "{\"cefrLevel\":\"A1|A2|B1|B2|C1|C2\",\"questions\":[{\"prompt\":\"string with exactly one blank written as ____\",\"answerText\":\"string\",\"explanation\":\"string\"}]}",
      "Rules:",
      "- Each prompt must contain exactly one blank written as ____.",
      "- answerText must be the exact missing phrase.",
      "- Do not include answer choices."
    ]
    : input.questionType === "mixed"
      ? [
        `Create exactly ${input.questionCount} quiz questions with both multiple-choice and fill-in-the-blank questions.`,
        "Use this exact schema:",
        "{\"cefrLevel\":\"A1|A2|B1|B2|C1|C2\",\"questions\":[{\"type\":\"multiple_choice\",\"prompt\":\"string\",\"options\":[\"string\",\"string\",\"string\",\"string\"],\"correctOptionIndex\":0,\"explanation\":\"string\"},{\"type\":\"fill_in_blank\",\"prompt\":\"string with exactly one blank written as ____\",\"answerText\":\"string\",\"explanation\":\"string\"}]}",
        "Rules:",
        "- Include at least one multiple_choice question and at least one fill_in_blank question.",
        "- Keep the two question types as evenly balanced as possible.",
        "- For multiple_choice questions, include exactly 4 options and correctOptionIndex must be 0, 1, 2, or 3.",
        "- For fill_in_blank questions, each prompt must contain exactly one blank written as ____.",
        "- Do not include answer choices on fill_in_blank questions."
      ]
    : [
      `Create exactly ${input.questionCount} multiple-choice quiz questions.`,
      "Use this exact schema:",
      "{\"cefrLevel\":\"A1|A2|B1|B2|C1|C2\",\"questions\":[{\"prompt\":\"string\",\"options\":[\"string\",\"string\",\"string\",\"string\"],\"correctOptionIndex\":0,\"explanation\":\"string\"}]}",
      "Rules:",
      "- Ensure each question has exactly 4 options.",
      "- correctOptionIndex must be 0, 1, 2, or 3."
    ];

  return [
    "Return valid JSON only, without markdown fences.",
    ...questionTypeInstructions,
    ...grammarFocusInstructions,
    `Room name: ${input.roomName}`,
    `CEFR level: ${input.cefrLevel}`,
    "- Keep all text in English.",
    "- Explanations should be short and clear.",
    "- Make the language difficulty appropriate for the requested CEFR level."
  ].join("\n");
}

function buildVocabularyQuizPrompt(input: {
  topic: string;
  vocabularyWords: string[];
  roomName: string;
  cefrLevel: CefrLevel;
  questionCount: number;
}): string {
  return [
    "Return valid JSON only, without markdown fences.",
    `Create exactly ${input.questionCount} vocabulary spelling quiz questions.`,
    "Use this exact schema:",
    "{\"cefrLevel\":\"A1|A2|B1|B2|C1|C2\",\"questions\":[{\"prompt\":\"short description or definition of the word without using the word itself\",\"answerWord\":\"string\",\"shuffledLetters\":[\"a\",\"p\",\"p\",\"l\",\"e\"],\"explanation\":\"short explanation of meaning or usage\"}]}",
    "Rules:",
    "- Create exactly one question for each provided vocabulary word.",
    "- prompt must describe the meaning of the target word without revealing the target word or obvious derivatives of it.",
    "- answerWord must exactly match one of the provided words.",
    "- shuffledLetters must contain every character from answerWord, in shuffled order, one character per array item.",
    "- explanation should be short and help the learner remember the word.",
    `Vocabulary set focus: ${input.topic}`,
    `Vocabulary words: ${input.vocabularyWords.join(", ")}`,
    `Room name: ${input.roomName}`,
    `CEFR level: ${input.cefrLevel}`,
    "- Keep all text in English.",
    "- Make definitions learner-friendly and appropriate for the requested CEFR level."
  ].join("\n");
}

function parseQuizJson(text: string): GeneratedQuiz {
  try {
    return JSON.parse(text) as GeneratedQuiz;
  } catch (error) {
    throw new HttpsError(
      "internal",
      `AI returned invalid JSON: ${error instanceof Error ? error.message : "Unknown parse error"}`
    );
  }
}

function toQuizGenerationFailureReason(error: unknown): string {
  if (error instanceof HttpsError) {
    return error.message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return "Quiz generation failed.";
}

async function loadRoomForQuizGeneration(roomId: string, userId: string) {
  const roomRef = db.collection(ROOMS_COLLECTION).doc(roomId);
  const roomSnapshot = await roomRef.get();

  if (!roomSnapshot.exists) {
    throw new HttpsError("not-found", "Room was not found.");
  }

  const room = roomSnapshot.data() as RoomDocument;
  if (!room.ownerId) {
    throw new HttpsError("failed-precondition", "Room is missing owner information.");
  }

  const collaboratorIds = Array.isArray(room.collaboratorIds) ? room.collaboratorIds : [];
  if (room.ownerId !== userId && !collaboratorIds.includes(userId)) {
    throw new HttpsError("permission-denied", "Only the room owner or a collaborator can generate a quiz.");
  }

  return {
    roomRef,
    room,
    roomCefrLevel: normalizeCefrLevel(room.cefrLevel)
  };
}

function toClientVisibleQuizError(error: unknown, failureReason: string): HttpsError {
  if (error instanceof HttpsError) {
    return new HttpsError(error.code, failureReason, failureReason);
  }

  return new HttpsError("internal", failureReason, failureReason);
}

function validateQuiz(
  quiz: GeneratedQuiz,
  requestedCefrLevel: CefrLevel,
  requestedQuestionCount: number,
  requestedQuestionType: QuestionType
): void {
  if (!Array.isArray(quiz.questions) || quiz.questions.length !== requestedQuestionCount) {
    throw new HttpsError("internal", "AI returned an invalid quiz structure.");
  }

  if (quiz.cefrLevel !== requestedCefrLevel) {
    quiz.cefrLevel = requestedCefrLevel;
  }

  let multipleChoiceCount = 0;
  let fillInBlankCount = 0;

  quiz.questions.forEach((question, index) => {
    if (!question.prompt || !question.explanation) {
      throw new HttpsError("internal", `AI returned an invalid question at index ${index}.`);
    }

    const questionType = resolveGeneratedQuestionType(question, requestedQuestionType, index);

    if (questionType === "multiple_choice") {
      multipleChoiceCount += 1;
      const multipleChoiceQuestion = question as MultipleChoiceGeneratedQuestion;
      if (
        !Array.isArray(multipleChoiceQuestion.options) ||
        multipleChoiceQuestion.options.length !== 4 ||
        multipleChoiceQuestion.correctOptionIndex == null ||
        multipleChoiceQuestion.correctOptionIndex < 0 ||
        multipleChoiceQuestion.correctOptionIndex > 3
      ) {
        throw new HttpsError("internal", `AI returned an invalid question at index ${index}.`);
      }
      return;
    }

    if (questionType === "fill_in_blank") {
      fillInBlankCount += 1;
      const fillInBlankQuestion = question as FillInBlankGeneratedQuestion;
      if (!fillInBlankQuestion.answerText || question.prompt.split("____").length !== 2) {
        throw new HttpsError("internal", `AI returned an invalid question at index ${index}.`);
      }
      return;
    }

    const wordScrambleQuestion = question as WordScrambleGeneratedQuestion;
    if (
      !wordScrambleQuestion.answerWord ||
      !Array.isArray(wordScrambleQuestion.shuffledLetters) ||
      wordScrambleQuestion.shuffledLetters.length !== wordScrambleQuestion.answerWord.length
    ) {
      throw new HttpsError("internal", `AI returned an invalid question at index ${index}.`);
    }
  });

  if (requestedQuestionType === "mixed" && (multipleChoiceCount === 0 || fillInBlankCount === 0)) {
    throw new HttpsError("internal", "AI returned a mixed quiz without both question types.");
  }
}

function resolveGeneratedQuestionType(
  question: MultipleChoiceGeneratedQuestion | FillInBlankGeneratedQuestion | WordScrambleGeneratedQuestion,
  requestedQuestionType: QuestionType,
  index: number
): QuestionType {
  if (requestedQuestionType !== "mixed") {
    return requestedQuestionType;
  }

  if (question.type === "multiple_choice" || question.type === "fill_in_blank") {
    return question.type;
  }

  throw new HttpsError("internal", `AI returned an invalid mixed question type at index ${index}.`);
}

function toStoredQuestion(
  question: MultipleChoiceGeneratedQuestion | FillInBlankGeneratedQuestion | WordScrambleGeneratedQuestion,
  questionType: QuestionType,
  index: number,
  questionTimeLimitSeconds: number | null
) {
  const storedQuestionType = resolveGeneratedQuestionType(question, questionType, index);

  if (storedQuestionType === "multiple_choice") {
    const multipleChoiceQuestion = question as MultipleChoiceGeneratedQuestion;
    return {
      id: `q${index + 1}`,
      type: "multiple_choice",
      prompt: multipleChoiceQuestion.prompt,
      options: multipleChoiceQuestion.options,
      correctOptionIndex: multipleChoiceQuestion.correctOptionIndex,
      explanation: multipleChoiceQuestion.explanation,
      ...(questionTimeLimitSeconds == null ? {} : { timeLimitSeconds: questionTimeLimitSeconds })
    };
  }

  if (storedQuestionType === "fill_in_blank") {
    const fillInBlankQuestion = question as FillInBlankGeneratedQuestion;
    return {
      id: `q${index + 1}`,
      type: "fill_in_blank",
      prompt: fillInBlankQuestion.prompt,
      answerText: fillInBlankQuestion.answerText,
      explanation: fillInBlankQuestion.explanation,
      ...(questionTimeLimitSeconds == null ? {} : { timeLimitSeconds: questionTimeLimitSeconds })
    };
  }

  const wordScrambleQuestion = question as WordScrambleGeneratedQuestion;
  return {
    id: `q${index + 1}`,
    type: "word_scramble",
    prompt: wordScrambleQuestion.prompt,
    answerWord: wordScrambleQuestion.answerWord,
    shuffledLetters: wordScrambleQuestion.shuffledLetters,
    explanation: wordScrambleQuestion.explanation,
    ...(questionTimeLimitSeconds == null ? {} : { timeLimitSeconds: questionTimeLimitSeconds })
  };
}

function buildQuizResponseSchema(questionCount: number, questionType: QuestionType) {
  return {
    type: "object",
    properties: {
      cefrLevel: { type: "string", enum: CEFR_LEVELS },
      questions: {
        type: "array",
        items: questionType === "fill_in_blank"
          ? {
            type: "object",
            properties: {
              prompt: { type: "string" },
              answerText: { type: "string" },
              explanation: { type: "string" }
            },
            required: ["prompt", "answerText", "explanation"]
          }
          : questionType === "mixed"
            ? {
              type: "object",
              properties: {
                type: { type: "string", enum: ["multiple_choice", "fill_in_blank"] },
                prompt: { type: "string" },
                options: {
                  type: "array",
                  minItems: 4,
                  maxItems: 4,
                  items: { type: "string" }
                },
                correctOptionIndex: { type: "integer" },
                answerText: { type: "string" },
                explanation: { type: "string" }
              },
              required: ["type", "prompt", "explanation"]
            }
          : questionType === "word_scramble"
            ? {
              type: "object",
              properties: {
                prompt: { type: "string" },
                answerWord: { type: "string" },
                shuffledLetters: {
                  type: "array",
                  minItems: 1,
                  items: { type: "string" }
                },
                explanation: { type: "string" }
              },
              required: ["prompt", "answerWord", "shuffledLetters", "explanation"]
            }
          : {
            type: "object",
            properties: {
              prompt: { type: "string" },
              options: {
                type: "array",
                minItems: 4,
                maxItems: 4,
                items: { type: "string" }
              },
              correctOptionIndex: { type: "integer" },
              explanation: { type: "string" }
            },
            required: ["prompt", "options", "correctOptionIndex", "explanation"]
          }
      }
    },
    required: ["cefrLevel", "questions"]
  };
}

function deduplicateWords(words: string[]): string[] {
  return Array.from(new Set(words.map((word) => word.trim()).filter((word) => word.length > 0)));
}

function buildVocabularyTopic(words: string[]): string {
  return `Vocabulary spelling: ${words.join(", ")}`;
}
