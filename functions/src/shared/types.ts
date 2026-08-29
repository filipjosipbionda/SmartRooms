// These types describe the data we read/write in Firestore and pass through helpers.

export type CefrLevel = "A1" | "A2" | "B1" | "B2" | "C1" | "C2";
export type QuestionType = "multiple_choice" | "fill_in_blank" | "mixed" | "word_scramble";
export type QuizKind = "grammar" | "vocabulary";

export type MultipleChoiceGeneratedQuestion = {
  type?: "multiple_choice";
  prompt: string;
  options: string[];
  correctOptionIndex: number;
  explanation: string;
};

export type FillInBlankGeneratedQuestion = {
  type?: "fill_in_blank";
  prompt: string;
  answerText: string;
  explanation: string;
};

export type WordScrambleGeneratedQuestion = {
  type?: "word_scramble";
  prompt: string;
  answerWord: string;
  shuffledLetters: string[];
  explanation: string;
};

export type GeneratedQuiz = {
  cefrLevel: CefrLevel;
  questions: Array<MultipleChoiceGeneratedQuestion | FillInBlankGeneratedQuestion | WordScrambleGeneratedQuestion>;
};

export type RoomDocument = {
  name?: string;
  topic?: string;
  cefrLevel?: CefrLevel;
  ownerId?: string;
  memberIds?: string[];
  collaboratorIds?: string[];
  unansweredQuizCount?: number;
};

export type TeacherApprovalStatus = "none" | "pending" | "approved" | "rejected";
export type TeacherRequestStatus = "pending" | "approved" | "rejected";

export type UserDocument = {
  uid?: string;
  email?: string;
  displayName?: string;
  role?: "teacher" | "student" | null;
  profileComplete?: boolean;
  teacherApprovalStatus?: TeacherApprovalStatus;
};

export type ResolvedUserDocument = {
  uid: string;
  email: string;
  displayName: string;
  role: "teacher" | "student" | null;
  profileComplete: boolean;
  teacherApprovalStatus: TeacherApprovalStatus;
};

export type RoomInvitationAccess = "member" | "collaborator";

export type RoomInvitationDocument = {
  roomId: string;
  inviteeId: string;
  access: RoomInvitationAccess;
  status: "pending" | "accepted" | "rejected";
};

export type TeacherRequestDocument = {
  status?: TeacherRequestStatus;
};
