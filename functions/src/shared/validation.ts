import { HttpsError } from "firebase-functions/v2/https";
import {
  CEFR_LEVELS,
  DEFAULT_QUESTION_COUNT,
  MAX_QUESTION_COUNT,
  MAX_QUESTION_TIME_LIMIT_SECONDS,
  MIN_QUESTION_COUNT,
  MIN_QUESTION_TIME_LIMIT_SECONDS
} from "./config.js";
import {
  CefrLevel,
  QuizKind,
  QuestionType,
  TeacherApprovalStatus,
  TeacherRequestStatus
} from "./types.js";

// Helpers for validating and normalizing values coming from callable function requests.

export function readRequiredString(value: unknown, fieldName: string): string {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new HttpsError("invalid-argument", `${fieldName} is required.`);
  }

  return value.trim();
}

export function readOptionalString(value: unknown): string | undefined {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : undefined;
}

export function normalizeCefrLevel(value: unknown): CefrLevel {
  if (typeof value !== "string") {
    throw new HttpsError("invalid-argument", "cefrLevel is required.");
  }

  const normalized = value.trim().toUpperCase() as CefrLevel;
  if (CEFR_LEVELS.includes(normalized)) {
    return normalized;
  }

  throw new HttpsError("invalid-argument", "cefrLevel must be one of A1, A2, B1, B2, C1, or C2.");
}

export function normalizeQuestionCount(value: unknown): number {
  if (value == null) {
    return DEFAULT_QUESTION_COUNT;
  }

  if (typeof value !== "number" || !Number.isInteger(value)) {
    throw new HttpsError("invalid-argument", "questionCount must be a whole number.");
  }

  if (value >= MIN_QUESTION_COUNT && value <= MAX_QUESTION_COUNT) {
    return value;
  }

  throw new HttpsError(
    "invalid-argument",
    `questionCount must be between ${MIN_QUESTION_COUNT} and ${MAX_QUESTION_COUNT}.`
  );
}

export function normalizeQuestionType(value: unknown): QuestionType {
  if (value === "fill_in_blank") {
    return "fill_in_blank";
  }
  if (value === "mixed") {
    return "mixed";
  }
  if (value === "word_scramble") {
    return "word_scramble";
  }
  return "multiple_choice";
}

export function normalizeQuestionTimeLimitSeconds(value: unknown): number | null {
  if (value == null) {
    return null;
  }

  if (typeof value !== "number" || !Number.isInteger(value)) {
    throw new HttpsError("invalid-argument", "questionTimeLimitSeconds must be a whole number.");
  }

  if (value >= MIN_QUESTION_TIME_LIMIT_SECONDS && value <= MAX_QUESTION_TIME_LIMIT_SECONDS) {
    return value;
  }

  throw new HttpsError(
    "invalid-argument",
    `questionTimeLimitSeconds must be between ${MIN_QUESTION_TIME_LIMIT_SECONDS} and ${MAX_QUESTION_TIME_LIMIT_SECONDS}.`
  );
}

export function normalizeQuizKind(value: unknown): QuizKind {
  return value === "vocabulary" ? "vocabulary" : "grammar";
}

export function readStringList(value: unknown, fieldName: string): string[] {
  if (!Array.isArray(value)) {
    throw new HttpsError("invalid-argument", `${fieldName} must be an array of strings.`);
  }

  const values = value
    .map((item) => typeof item === "string" ? item.trim() : "")
    .filter((item) => item.length > 0);

  if (values.length === 0) {
    throw new HttpsError("invalid-argument", `${fieldName} must contain at least one value.`);
  }

  return values;
}

export function normalizeTeacherRequestStatus(value: unknown): TeacherRequestStatus {
  return value === "approved" || value === "rejected" ? value : "pending";
}

export function normalizeTeacherApprovalStatus(value: unknown): TeacherApprovalStatus {
  return value === "pending" || value === "approved" || value === "rejected" ? value : "none";
}
