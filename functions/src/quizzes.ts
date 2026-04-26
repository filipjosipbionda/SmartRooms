import { FieldValue } from "firebase-admin/firestore";
import { GoogleGenAI } from "@google/genai";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  DEFAULT_TOPIC,
  FUNCTIONS_REGION,
  MODEL_NAME,
  QUIZZES_COLLECTION,
  ROOMS_COLLECTION,
  CEFR_LEVELS
} from "./shared/config.js";
import { db, geminiApiKey } from "./shared/firebase.js";
import { CefrLevel, GeneratedQuiz, QuestionType, RoomDocument } from "./shared/types.js";
import {
  normalizeCefrLevel,
  normalizeQuestionCount,
  normalizeQuestionType,
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

    const roomId = readRequiredString(request.data.roomId, "roomId");
    const cefrLevel = normalizeCefrLevel(request.data.cefrLevel);
    const questionCount = normalizeQuestionCount(request.data.questionCount);
    const questionType = normalizeQuestionType(request.data.questionType);

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
    if (room.ownerId !== request.auth.uid && !collaboratorIds.includes(request.auth.uid)) {
      throw new HttpsError("permission-denied", "Only the room owner or a collaborator can generate a quiz.");
    }

    const quiz = await generateQuiz({
      topic: room.topic || DEFAULT_TOPIC,
      roomName: room.name || "Untitled room",
      cefrLevel,
      questionCount,
      questionType
    });

    const quizRef = roomRef.collection(QUIZZES_COLLECTION).doc();
    await quizRef.set({
      id: quizRef.id,
      roomId,
      roomName: room.name || "Untitled room",
      roomTopic: room.topic || DEFAULT_TOPIC,
      cefrLevel: quiz.cefrLevel,
      title: quiz.title,
      questionType,
      questions: quiz.questions.map((question, index) => ({
        id: `q${index + 1}`,
        prompt: question.prompt,
        questionType,
        options: question.options ?? [],
        correctOptionIndex: question.correctOptionIndex ?? null,
        answerText: question.answerText ?? null,
        explanation: question.explanation
      })),
      questionCount: quiz.questions.length,
      createdBy: request.auth.uid,
      createdAt: FieldValue.serverTimestamp(),
      createdAtEpochMillis: Date.now()
    });

    await roomRef.update({
      unansweredQuizCount: FieldValue.increment(1)
    });

    return {
      ok: true,
      roomId,
      quizId: quizRef.id,
      cefrLevel: quiz.cefrLevel,
      questionCount: quiz.questions.length,
      questionType
    };
  }
);

async function generateQuiz(input: {
  topic: string;
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
  topic: string;
  roomName: string;
  cefrLevel: CefrLevel;
  questionCount: number;
  questionType: QuestionType;
}): string {
  const questionTypeInstructions = input.questionType === "fill_in_blank"
    ? [
      `Create exactly ${input.questionCount} fill-in-the-blank quiz questions.`,
      "Use this exact schema:",
      "{\"title\":\"string\",\"cefrLevel\":\"A1|A2|B1|B2|C1|C2\",\"questions\":[{\"prompt\":\"string with exactly one blank written as ____\",\"answerText\":\"string\",\"explanation\":\"string\"}]}",
      "Rules:",
      "- Each prompt must contain exactly one blank written as ____.",
      "- answerText must be the exact missing phrase.",
      "- Do not include answer choices."
    ]
    : [
      `Create exactly ${input.questionCount} multiple-choice quiz questions.`,
      "Use this exact schema:",
      "{\"title\":\"string\",\"cefrLevel\":\"A1|A2|B1|B2|C1|C2\",\"questions\":[{\"prompt\":\"string\",\"options\":[\"string\",\"string\",\"string\",\"string\"],\"correctOptionIndex\":0,\"explanation\":\"string\"}]}",
      "Rules:",
      "- Ensure each question has exactly 4 options.",
      "- correctOptionIndex must be 0, 1, 2, or 3."
    ];

  return [
    "Return valid JSON only, without markdown fences.",
    ...questionTypeInstructions,
    `Room name: ${input.roomName}`,
    `Topic: ${input.topic}`,
    `CEFR level: ${input.cefrLevel}`,
    "- Keep all text in English.",
    "- Explanations should be short and clear.",
    "- Make the language difficulty appropriate for the requested CEFR level."
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

function validateQuiz(
  quiz: GeneratedQuiz,
  requestedCefrLevel: CefrLevel,
  requestedQuestionCount: number,
  requestedQuestionType: QuestionType
): void {
  if (!quiz.title || !Array.isArray(quiz.questions) || quiz.questions.length !== requestedQuestionCount) {
    throw new HttpsError("internal", "AI returned an invalid quiz structure.");
  }

  if (quiz.cefrLevel !== requestedCefrLevel) {
    quiz.cefrLevel = requestedCefrLevel;
  }

  quiz.questions.forEach((question, index) => {
    if (!question.prompt || !question.explanation) {
      throw new HttpsError("internal", `AI returned an invalid question at index ${index}.`);
    }

    if (requestedQuestionType === "multiple_choice") {
      if (
        !Array.isArray(question.options) ||
        question.options.length !== 4 ||
        question.correctOptionIndex == null ||
        question.correctOptionIndex < 0 ||
        question.correctOptionIndex > 3
      ) {
        throw new HttpsError("internal", `AI returned an invalid question at index ${index}.`);
      }
      return;
    }

    if (!question.answerText || question.prompt.split("____").length !== 2) {
      throw new HttpsError("internal", `AI returned an invalid question at index ${index}.`);
    }
  });
}

function buildQuizResponseSchema(questionCount: number, questionType: QuestionType) {
  return {
    type: "object",
    properties: {
      title: { type: "string" },
      cefrLevel: { type: "string", enum: CEFR_LEVELS },
      questions: {
        type: "array",
        minItems: questionCount,
        maxItems: questionCount,
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
    required: ["title", "cefrLevel", "questions"]
  };
}
