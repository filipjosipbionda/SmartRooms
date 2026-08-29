import { FieldValue } from "firebase-admin/firestore";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { FUNCTIONS_REGION, ROOMS_COLLECTION } from "./shared/config.js";
import { db } from "./shared/firebase.js";

// Keeps announcement feed metadata in sync without requiring clients to update parent posts.
export const syncLatestAnnouncementComment = onDocumentCreated(
  {
    document: `${ROOMS_COLLECTION}/{roomId}/announcements/{announcementId}/comments/{commentId}`,
    region: FUNCTIONS_REGION
  },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const comment = snapshot.data() as Record<string, unknown>;
    const roomId = event.params.roomId;
    const announcementId = event.params.announcementId;
    const commentId = event.params.commentId;
    const createdAtEpochMillis = readNumber(comment.createdAtEpochMillis);

    const latestComment = {
      id: commentId,
      authorName: readString(comment.authorName, "Comment author"),
      message: readString(comment.message, ""),
      createdAtEpochMillis
    };

    const announcementRef = db
      .collection(ROOMS_COLLECTION)
      .doc(roomId)
      .collection("announcements")
      .doc(announcementId);

    await db.runTransaction(async (transaction) => {
      const announcementSnapshot = await transaction.get(announcementRef);
      if (!announcementSnapshot.exists) return;

      const announcement = announcementSnapshot.data() as Record<string, unknown>;
      const currentLatestComment = announcement.latestComment as Record<string, unknown> | undefined;
      const currentLatestCreatedAt = readNumber(currentLatestComment?.createdAtEpochMillis);
      const shouldUpdateLatestComment = createdAtEpochMillis >= currentLatestCreatedAt;

      transaction.update(
        announcementRef,
        {
          commentCount: FieldValue.increment(1),
          updatedAtEpochMillis: Date.now(),
          ...(shouldUpdateLatestComment ? { latestComment } : {})
        }
      );
    });
  }
);

function readString(
  value: unknown,
  fallback: string
): string {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : fallback;
}

function readNumber(value: unknown): number {
  return typeof value === "number" && Number.isFinite(value) ? value : 0;
}
