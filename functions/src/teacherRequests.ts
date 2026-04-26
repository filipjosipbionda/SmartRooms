import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { FUNCTIONS_REGION } from "./shared/config.js";
import { db } from "./shared/firebase.js";
import { TeacherRequestDocument } from "./shared/types.js";
import { normalizeTeacherRequestStatus } from "./shared/validation.js";

// Firestore trigger that mirrors the teacher request status into the user profile.
export const syncTeacherRequestToUserProfile = onDocumentWritten(
  {
    document: "teacherRequests/{uid}",
    region: FUNCTIONS_REGION
  },
  async (event) => {
    const uid = event.params.uid;
    const afterData = event.data?.after.exists ? event.data.after.data() as TeacherRequestDocument : null;
    const userRef = db.collection("users").doc(uid);

    if (!afterData) {
      await userRef.set({
        role: null,
        profileComplete: false,
        teacherApprovalStatus: "none",
        updatedAtEpochMillis: Date.now()
      }, { merge: true });
      return;
    }

    const status = normalizeTeacherRequestStatus(afterData.status);

    if (status === "approved") {
      await userRef.set({
        role: "teacher",
        profileComplete: true,
        teacherApprovalStatus: "approved",
        updatedAtEpochMillis: Date.now()
      }, { merge: true });
      return;
    }

    if (status === "rejected") {
      await userRef.set({
        role: null,
        profileComplete: false,
        teacherApprovalStatus: "rejected",
        updatedAtEpochMillis: Date.now()
      }, { merge: true });
      return;
    }

    await userRef.set({
      role: null,
      profileComplete: false,
      teacherApprovalStatus: "pending",
      updatedAtEpochMillis: Date.now()
    }, { merge: true });
  }
);
