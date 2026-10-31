// POST /api/invite — a guardian (Google sign-in) gets a 6-digit code for the elder's phone.
import { randomInt } from "node:crypto";
import { db, FieldValue, HttpError, route, Timestamp } from "../lib/firebase.js";

const VALID_MS = 15 * 60 * 1000;

export default route(async (user) => {
  if (user.firebase.sign_in_provider !== "google.com") throw new HttpError(403, "guardian_only");

  for (let attempt = 0; attempt < 5; attempt++) {
    const code = String(randomInt(0, 1_000_000)).padStart(6, "0");
    const ref = db.collection("invites").doc(code);
    const expiresAt = Timestamp.fromMillis(Date.now() + VALID_MS);
    const taken = await db.runTransaction(async (tx) => {
      const existing = await tx.get(ref);
      // /api/cleanup removes expired invites daily; until then, an expired code can be reused.
      if (existing.exists && existing.get("expires_at").toMillis() > Date.now()) return true;
      tx.set(ref, {
        guardian_id: user.uid,
        guardian_name: user.name ?? user.email ?? "",
        created_at: FieldValue.serverTimestamp(),
        expires_at: expiresAt,
      });
      return false;
    });
    if (!taken) return { code, expires_at: expiresAt.toMillis() };
  }
  throw new HttpError(503, "busy");
});
