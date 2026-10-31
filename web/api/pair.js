// POST /api/pair { code, name } — the elder's phone (anonymous sign-in) joins the guardian who made the code.
import { db, FieldValue, HttpError, route } from "../lib/firebase.js";

const MAX_GUARDIANS = 3;
// Brute-force guard: a 6-digit code lives 15 minutes, and each account gets 5 wrong tries an hour.
const MAX_FAILURES = 5;
const HOUR_MS = 60 * 60 * 1000;

export default route(async (user, body) => {
  if (user.firebase.sign_in_provider !== "anonymous") throw new HttpError(403, "elder_only");
  const code = String(body.code ?? "").replace(/\D/g, "");
  const name = String(body.name ?? "").trim().slice(0, 40);
  if (code.length !== 6 || !name) throw new HttpError(400, "bad_request");

  const elderId = user.uid;
  const inviteRef = db.collection("invites").doc(code);
  const attemptsRef = db.collection("pair_attempts").doc(elderId);
  const linksQuery = db.collection("family_links").where("elder_id", "==", elderId);

  // Failures are recorded inside the transaction, so it returns them instead of throwing.
  const outcome = await db.runTransaction(async (tx) => {
    const [attempts, invite, links] = await Promise.all([
      tx.get(attemptsRef),
      tx.get(inviteRef),
      tx.get(linksQuery),
    ]);
    const now = Date.now();
    const failures = (attempts.get("failures") ?? []).filter((t) => t > now - HOUR_MS);
    if (failures.length >= MAX_FAILURES) return { error: [429, "too_many_tries"] };

    if (!invite.exists || invite.get("expires_at").toMillis() <= now) {
      tx.set(attemptsRef, { failures: [...failures, now] });
      return { error: [400, invite.exists ? "expired" : "bad_code"] };
    }
    const guardianId = invite.get("guardian_id");
    if (guardianId === elderId) return { error: [400, "bad_code"] };
    if (links.docs.some((d) => d.get("guardian_id") === guardianId)) return { error: [409, "already_linked"] };
    if (links.size >= MAX_GUARDIANS) return { error: [409, "too_many_guardians"] };

    const guardianName = invite.get("guardian_name");
    tx.set(db.collection("family_links").doc(`${elderId}_${guardianId}`), {
      elder_id: elderId,
      guardian_id: guardianId,
      elder_name: name,
      guardian_name: guardianName,
      invite_code: code,
      status: "active",
      created_at: FieldValue.serverTimestamp(),
    });
    // Same elder name on every link, so all guardians see what the elder typed last.
    links.docs.forEach((d) => tx.update(d.ref, { elder_name: name }));
    tx.delete(inviteRef);
    return { guardian_name: guardianName };
  });

  if (outcome.error) throw new HttpError(...outcome.error);
  return outcome;
});
