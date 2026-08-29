import { onCall } from "firebase-functions/v2/https";
import { FUNCTIONS_REGION } from "./shared/config.js";

// Small callable function used to verify that the Functions runtime is reachable.
export const ping = onCall(
  { cors: true, region: FUNCTIONS_REGION },
  async () => {
    return {
      ok: true,
      message: "SmartRooms Functions are running."
    };
  }
);
