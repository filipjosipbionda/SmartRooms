import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";
import { defineSecret } from "firebase-functions/params";

// Firebase Admin is initialized once for the whole Functions process.
initializeApp();

// Shared singleton instances used by the feature-specific files.
export const db = getFirestore();
export const geminiApiKey = defineSecret("GEMINI_API_KEY");
