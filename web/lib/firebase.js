// Shared setup for the /api functions: Firebase Admin, token checks and FCM sends.
// FIREBASE_SERVICE_ACCOUNT holds the service-account JSON (Vercel env var, never committed).
import { cert, getApps, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore, Timestamp } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";

const app =
  getApps()[0] ?? initializeApp({ credential: cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)) });

export { FieldValue, Timestamp };
export const auth = getAuth(app);
export const db = getFirestore(app);
const messaging = getMessaging(app);

/** An error the app shows to the person. `code` matches errorRes() in FamilyScreen.kt. */
export class HttpError extends Error {
  constructor(status, code) {
    super(code);
    this.status = status;
    this.code = code;
  }
}

/** Wraps a handler: POST only, verifies the Firebase ID token, turns errors into JSON. */
export function route(handler) {
  return async (req, res) => {
    if (req.method !== "POST") return res.status(405).json({ error: "method" });
    try {
      const header = req.headers.authorization ?? "";
      const token = header.startsWith("Bearer ") ? header.slice(7) : "";
      const user = await auth.verifyIdToken(token).catch(() => {
        throw new HttpError(401, "signed_out");
      });
      res.status(200).json(await handler(user, req.body ?? {}));
    } catch (e) {
      if (e instanceof HttpError) return res.status(e.status).json({ error: e.code });
      console.error(e);
      res.status(500).json({ error: "server" });
    }
  };
}

/** Firestore's gRPC code when create() hits an existing document. */
export const ALREADY_EXISTS = 6;

/** Sends a data-only, high-priority message to every device of these users. Returns how many got it. */
export async function sendTo(uids, data) {
  const users = await Promise.all(uids.map((uid) => db.collection("users").doc(uid).get()));
  const targets = users.flatMap((u) => (u.get("fcm_tokens") ?? []).map((token) => ({ ref: u.ref, token })));
  if (targets.length === 0) return 0;

  const result = await messaging.sendEachForMulticast({
    tokens: targets.map((t) => t.token),
    data,
    android: { priority: "high", ttl: 24 * 60 * 60 * 1000 },
  });
  // Drop tokens from uninstalled apps so they don't pile up.
  const stale = result.responses
    .map((r, i) => (!r.success && isStale(r.error) ? targets[i] : null))
    .filter(Boolean);
  await Promise.all(
    stale.map(({ ref, token }) => ref.update({ fcm_tokens: FieldValue.arrayRemove(token) })),
  );
  return result.successCount;
}

function isStale(error) {
  return [
    "messaging/registration-token-not-registered",
    "messaging/invalid-registration-token",
  ].includes(error?.code);
}
