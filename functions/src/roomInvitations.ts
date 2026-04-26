import { FieldValue } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import {
  FUNCTIONS_REGION,
  ROOM_INVITATIONS_COLLECTION,
  ROOMS_COLLECTION,
  USERS_COLLECTION
} from "./shared/config.js";
import { db } from "./shared/firebase.js";
import {
  RoomDocument,
  RoomInvitationAccess,
  RoomInvitationDocument,
  UserDocument
} from "./shared/types.js";
import { readRequiredString } from "./shared/validation.js";

// This file contains the full room-invitation lifecycle.

export const sendRoomInvitation = onCall(
  { cors: true, region: FUNCTIONS_REGION },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const roomId = readRequiredString(request.data.roomId, "roomId");
    const inviteeId = readRequiredString(request.data.inviteeId, "inviteeId");
    const inviterId = request.auth.uid;

    if (inviteeId === inviterId) {
      throw new HttpsError("failed-precondition", "You cannot invite yourself.");
    }

    const roomRef = db.collection(ROOMS_COLLECTION).doc(roomId);
    const roomSnapshot = await roomRef.get();

    if (!roomSnapshot.exists) {
      throw new HttpsError("not-found", "Room was not found.");
    }

    const room = roomSnapshot.data() as RoomDocument;
    if (!room.ownerId) {
      throw new HttpsError("failed-precondition", "Room is missing owner information.");
    }
    if (room.ownerId !== inviterId) {
      throw new HttpsError("permission-denied", "Only the room owner can invite users.");
    }

    const memberIds = Array.isArray(room.memberIds) ? room.memberIds : [];
    const collaboratorIds = Array.isArray(room.collaboratorIds) ? room.collaboratorIds : [];
    if (memberIds.includes(inviteeId) || collaboratorIds.includes(inviteeId) || room.ownerId === inviteeId) {
      throw new HttpsError("failed-precondition", "The user is already part of this room.");
    }

    const inviteeSnapshot = await db.collection(USERS_COLLECTION).doc(inviteeId).get();
    if (!inviteeSnapshot.exists) {
      throw new HttpsError("not-found", "Invitee profile was not found.");
    }

    const invitee = inviteeSnapshot.data() as UserDocument;
    const inviterSnapshot = await db.collection(USERS_COLLECTION).doc(inviterId).get();
    const inviter = inviterSnapshot.exists ? inviterSnapshot.data() as UserDocument : null;
    const access: RoomInvitationAccess = invitee.role === "teacher" ? "collaborator" : "member";
    const invitationRef = db
      .collection(USERS_COLLECTION)
      .doc(inviteeId)
      .collection(ROOM_INVITATIONS_COLLECTION)
      .doc(`${roomId}_${inviteeId}`);

    await invitationRef.set({
      id: invitationRef.id,
      roomId,
      roomName: room.name || "Untitled room",
      inviterId,
      inviterName: inviter?.displayName || request.auth.token.name || "Room owner",
      inviteeId,
      inviteeEmail: invitee.email || "",
      inviteeDisplayName: invitee.displayName || invitee.email || "Invited user",
      access,
      status: "pending",
      createdAt: FieldValue.serverTimestamp(),
      createdAtEpochMillis: Date.now(),
      updatedAtEpochMillis: Date.now()
    }, { merge: true });

    return {
      ok: true,
      invitationId: invitationRef.id,
      roomId,
      inviteeId,
      access
    };
  }
);

export const acceptRoomInvitation = onCall(
  { cors: true, region: FUNCTIONS_REGION },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const invitationId = readRequiredString(request.data.invitationId, "invitationId");
    const invitationRef = db
      .collection(USERS_COLLECTION)
      .doc(request.auth!.uid)
      .collection(ROOM_INVITATIONS_COLLECTION)
      .doc(invitationId);

    await db.runTransaction(async (transaction) => {
      const invitationSnapshot = await transaction.get(invitationRef);
      if (!invitationSnapshot.exists) {
        throw new HttpsError("not-found", "Invitation was not found.");
      }

      const invitation = invitationSnapshot.data() as RoomInvitationDocument;
      if (invitation.inviteeId !== request.auth!.uid) {
        throw new HttpsError("permission-denied", "Only the invited user can accept this invitation.");
      }
      if (invitation.status !== "pending") {
        throw new HttpsError("failed-precondition", "Invitation is no longer pending.");
      }

      const roomRef = db.collection(ROOMS_COLLECTION).doc(invitation.roomId);
      const roomSnapshot = await transaction.get(roomRef);
      if (!roomSnapshot.exists) {
        throw new HttpsError("not-found", "Room was not found.");
      }

      const room = roomSnapshot.data() as RoomDocument;
      const memberIds = Array.isArray(room.memberIds) ? room.memberIds : [];
      const collaboratorIds = Array.isArray(room.collaboratorIds) ? room.collaboratorIds : [];
      if (memberIds.includes(request.auth!.uid) || collaboratorIds.includes(request.auth!.uid)) {
        transaction.delete(invitationRef);
        return;
      }

      if (invitation.access === "collaborator") {
        transaction.update(roomRef, {
          collaboratorIds: FieldValue.arrayUnion(request.auth!.uid)
        });
      } else {
        transaction.update(roomRef, {
          memberIds: FieldValue.arrayUnion(request.auth!.uid),
          participantCount: FieldValue.increment(1)
        });
      }

      transaction.delete(invitationRef);
    });

    return {
      ok: true,
      invitationId
    };
  }
);

export const rejectRoomInvitation = onCall(
  { cors: true, region: FUNCTIONS_REGION },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Authentication is required.");
    }

    const invitationId = readRequiredString(request.data.invitationId, "invitationId");
    const invitationRef = db
      .collection(USERS_COLLECTION)
      .doc(request.auth.uid)
      .collection(ROOM_INVITATIONS_COLLECTION)
      .doc(invitationId);
    const invitationSnapshot = await invitationRef.get();

    if (!invitationSnapshot.exists) {
      throw new HttpsError("not-found", "Invitation was not found.");
    }

    const invitation = invitationSnapshot.data() as RoomInvitationDocument;
    if (invitation.inviteeId !== request.auth.uid) {
      throw new HttpsError("permission-denied", "Only the invited user can reject this invitation.");
    }

    await invitationRef.delete();

    return {
      ok: true,
      invitationId
    };
  }
);
