// POST /api/notify
//   { type: "check", client_id, masked_text, verdict, scam_type, red_flags, input_type }
//     The elder's phone saves a check and alerts every linked guardian.
//   { type: "reply", check_id, reply: "safe" | "scam" }
//     A guardian answers, and the elder's phone gets the reply.
import { ALREADY_EXISTS, db, FieldValue, HttpError, route, sendTo, Timestamp } from "../lib/firebase.js";
import { maskNumbers } from "../lib/mask.js";

// Raw text is deleted after 30 days: /api/cleanup runs daily and removes checks past expire_at.
const KEEP_MS = 30 * 24 * 60 * 60 * 1000;
const MAX_TEXT = 2000;
const VERDICTS = ["HIGH", "MEDIUM", "LOW"];
const ENUM_NAME = /^[A-Z_]{1,40}$/;

export default route(async (user, body) => {
  if (body.type === "check") return sendCheck(user, body);
  if (body.type === "reply") return sendReply(user, body);
  throw new HttpError(400, "bad_request");
});

async function sendCheck(user, body) {
  const clientId = String(body.client_id ?? "");
  if (!/^[\w-]{8,64}$/.test(clientId) || !VERDICTS.includes(body.verdict)) throw new HttpError(400, "bad_request");

  const links = await db.collection("family_links").where("elder_id", "==", user.uid).get();
  if (links.empty) throw new HttpError(409, "not_linked");

  const elderName = links.docs[0].get("elder_name");
  const guardianIds = links.docs.map((d) => d.get("guardian_id"));
  const scamType = ENUM_NAME.test(body.scam_type ?? "") ? body.scam_type : null;
  const ref = db.collection("checks").doc(`${user.uid}_${clientId}`);
  try {
    await ref.create({
      elder_id: user.uid,
      elder_name: elderName,
      guardian_ids: guardianIds,
      input_type: body.input_type === "text" ? "text" : "other",
      masked_text: maskNumbers(String(body.masked_text ?? "").slice(0, MAX_TEXT)),
      verdict: body.verdict,
      scam_type: scamType,
      red_flags: (Array.isArray(body.red_flags) ? body.red_flags : []).filter((f) => ENUM_NAME.test(f)).slice(0, 30),
      guardian_reply: null,
      created_at: FieldValue.serverTimestamp(),
      expire_at: Timestamp.fromMillis(Date.now() + KEEP_MS),
    });
  } catch (e) {
    // The app retried a check it already sent: don't alert twice.
    if (e.code === ALREADY_EXISTS) return { check_id: ref.id, sent: 0 };
    throw e;
  }

  const sent = await sendTo(guardianIds, {
    type: "check",
    check_id: ref.id,
    elder_name: elderName,
    verdict: body.verdict,
    scam_type: scamType ?? "",
  });
  return { check_id: ref.id, sent };
}

async function sendReply(user, body) {
  if (!["safe", "scam"].includes(body.reply)) throw new HttpError(400, "bad_request");
  const ref = db.collection("checks").doc(String(body.check_id ?? "-"));
  const check = await ref.get();
  if (!check.exists || !(check.get("guardian_ids") ?? []).includes(user.uid)) throw new HttpError(404, "not_found");

  const elderId = check.get("elder_id");
  const link = await db.collection("family_links").doc(`${elderId}_${user.uid}`).get();
  if (!link.exists) throw new HttpError(403, "not_linked");

  const guardianName = link.get("guardian_name");
  await ref.update({
    guardian_reply: {
      guardian_id: user.uid,
      guardian_name: guardianName,
      reply: body.reply,
      at: FieldValue.serverTimestamp(),
    },
  });
  const sent = await sendTo([elderId], {
    type: "reply",
    check_id: ref.id,
    guardian_name: guardianName,
    reply: body.reply,
  });
  return { check_id: ref.id, sent };
}
