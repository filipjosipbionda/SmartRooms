// Shared constants live here so they are not duplicated across feature files.

export const FUNCTIONS_REGION = "europe-west3";

export const ROOMS_COLLECTION = "rooms";
export const USERS_COLLECTION = "users";
export const ROOM_INVITATIONS_COLLECTION = "roomInvitations";
export const QUIZZES_COLLECTION = "quizzes";

export const DEFAULT_TOPIC = "English";
export const DEFAULT_QUESTION_COUNT = 5;
export const MIN_QUESTION_COUNT = 1;
export const MAX_QUESTION_COUNT = 50;
export const MODEL_NAME = "gemini-3-flash-preview";
export const CEFR_LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"] as const;
export const MIN_QUESTION_TIME_LIMIT_SECONDS = 1;
export const MAX_QUESTION_TIME_LIMIT_SECONDS = 3600;
