import { ResolvedUserDocument, TeacherApprovalStatus, UserDocument } from "./types.js";
import { normalizeTeacherApprovalStatus } from "./validation.js";

// The startup resolver uses these helpers to build one normalized user shape
// from auth metadata and Firestore profile data.

export function normalizeUserDocument(
  user: UserDocument | null,
  uid: string,
  authEmail?: string,
  authDisplayName?: string
): ResolvedUserDocument {
  const role = user?.role === "teacher" || user?.role === "student" ? user.role : null;
  const email = user?.email || authEmail || "";
  const displayName = user?.displayName || authDisplayName || email.split("@")[0] || "";

  return {
    uid,
    email,
    displayName,
    role,
    profileComplete: typeof user?.profileComplete === "boolean" ? user.profileComplete : role !== null,
    teacherApprovalStatus: normalizeTeacherApprovalStatus(user?.teacherApprovalStatus)
  };
}

export function buildMissingUserProfilePatch(
  current: UserDocument,
  resolved: ResolvedUserDocument
): Partial<UserDocument> {
  const patch: Partial<UserDocument> = {};

  if (!current.uid) {
    patch.uid = resolved.uid;
  }
  if (!current.email && resolved.email) {
    patch.email = resolved.email;
  }
  if (!current.displayName && resolved.displayName) {
    patch.displayName = resolved.displayName;
  }
  if (typeof current.profileComplete !== "boolean") {
    patch.profileComplete = resolved.profileComplete;
  }
  if (!current.teacherApprovalStatus) {
    patch.teacherApprovalStatus = resolved.teacherApprovalStatus as TeacherApprovalStatus;
  }

  return patch;
}
