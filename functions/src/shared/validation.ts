import { HttpsError } from "firebase-functions/v2/https";
import {
  ALLOWED_QUESTION_COUNTS,
  CEFR_LEVELS,
  DEFAULT_CEFR_LEVEL,
  DEFAULT_QUESTION_COUNT
} from "./config.js";
import {
  CefrLevel,
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
    return DEFAULT_CEFR_LEVEL;
  }

  const normalized = value.trim().toUpperCase() as CefrLevel;
  if (CEFR_LEVELS.includes(normalized)) {
    return normalized;
  }

  return DEFAULT_CEFR_LEVEL;
}

export function normalizeQuestionCount(value: unknown): number {
  if (typeof value !== "number" || !Number.isInteger(value)) {
    return DEFAULT_QUESTION_COUNT;
  }

  if (ALLOWED_QUESTION_COUNTS.includes(value as 5 | 10 | 15)) {
    return value;
  }

  return DEFAULT_QUESTION_COUNT;
}

export function normalizeQuestionType(value: unknown): QuestionType {
  return value === "fill_in_blank" ? "fill_in_blank" : "multiple_choice";
}

export function normalizeTeacherRequestStatus(value: unknown): TeacherRequestStatus {
  return value === "approved" || value === "rejected" ? value : "pending";
}

export function normalizeTeacherApprovalStatus(value: unknown): TeacherApprovalStatus {
  return value === "pending" || value === "approved" || value === "rejected" ? value : "none";
}
