// index.ts is now only the entry point that re-exports the individual Functions files.

export { ping } from "./ping.js";
export { resolveStartupDestination } from "./startup.js";
export {
  sendRoomInvitation,
  acceptRoomInvitation,
  rejectRoomInvitation,
  removeRoomMember
} from "./roomInvitations.js";
export { generateQuizForRoom, retryQuizForRoom } from "./quizzes.js";
export { syncTeacherRequestToUserProfile } from "./teacherRequests.js";
