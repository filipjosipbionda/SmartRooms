// These types describe the data we read/write in Firestore and pass through helpers.

export type CefrLevel = "A1" | "A2" | "B1" | "B2" | "C1" | "C2";
export type QuestionType = "multiple_choice" | "fill_in_blank";

export type GeneratedQuiz = {
  title: string;
  cefrLevel: CefrLevel;
  questions: Array<{
    prompt: string;
    options?: string[];
    correctOptionIndex?: number;
    answerText?: string;
    explanation: string;
  }>;
};

export type RoomDocument = {
  name?: string;
  topic?: string;
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
