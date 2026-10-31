// GET /api/cleanup — daily Vercel Cron (vercel.json). Deletes checks past their 30 days and expired invites.
// Firestore TTL policies would do this, but they need billing; the Spark plan has none.
import { db, Timestamp } from "../lib/firebase.js";

const BATCH = 400;

export default async function handler(req, res) {
  // Vercel sends CRON_SECRET as a bearer token on cron runs.
  if (req.headers.authorization !== `Bearer ${process.env.CRON_SECRET}`) return res.status(401).end();
  const now = Timestamp.now();
  const checks = await deleteWhere(db.collection("checks").where("expire_at", "<=", now));
  const invites = await deleteWhere(db.collection("invites").where("expires_at", "<=", now));
  res.status(200).json({ checks, invites });
}

async function deleteWhere(query) {
  let deleted = 0;
  for (;;) {
    const snap = await query.limit(BATCH).get();
    if (snap.empty) return deleted;
    const batch = db.batch();
    snap.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
    deleted += snap.size;
  }
}
