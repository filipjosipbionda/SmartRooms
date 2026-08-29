import { HttpsError, onCall } from "firebase-functions/v2/https";
import { FUNCTIONS_REGION, USERS_COLLECTION } from "./shared/config.js";
import { db } from "./shared/firebase.js";
import { buildMissingUserProfilePatch, normalizeUserDocument } from "./shared/startupProfile.js";
import { UserDocument } from "./shared/types.js";
import { readOptionalString } from "./shared/validation.js";

// The startup resolver returns only the minimal information the app needs:
// whether the authenticated user is ready to enter the Home screen.
export const resolveStartupDestination = onCall(
  { cors: true, region: FUNCTIONS_REGION },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const uid = request.auth.uid;
    const now = Date.now();
    const userRef = db.collection(USERS_COLLECTION).doc(uid);
    const userSnapshot = await userRef.get();
    const resolvedUser = normalizeUserDocument(
      userSnapshot.exists ? userSnapshot.data() as UserDocument : null,
      uid,
      readOptionalString(request.auth.token.email),
      readOptionalString(request.auth.token.name)
    );

    if (!userSnapshot.exists) {
      await userRef.set({
        uid: resolvedUser.uid,
        email: resolvedUser.email,
        displayName: resolvedUser.displayName,
        role: resolvedUser.role,
        profileComplete: resolvedUser.profileComplete,
        teacherApprovalStatus: resolvedUser.teacherApprovalStatus,
        createdAtEpochMillis: now,
        updatedAtEpochMillis: now
      });
    } else {
      const patch = buildMissingUserProfilePatch(userSnapshot.data() as UserDocument, resolvedUser);
      if (Object.keys(patch).length > 0) {
        await userRef.set({
          ...patch,
          updatedAtEpochMillis: now
        }, { merge: true });
      }
    }

    return {
      ok: true,
      isReady: resolvedUser.role !== null,
      role: resolvedUser.role,
      teacherApprovalStatus: resolvedUser.teacherApprovalStatus
    };
  }
);
