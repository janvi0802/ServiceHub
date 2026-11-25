/*
 * ServiceHub admin server: lets the ADMIN set a new password for a customer or provider.
 *
 * Why a server? A phone app can only change its OWN password (Firebase's protection).
 * Changing someone else's needs the Firebase Admin SDK, which must run on a server,
 * because it uses a secret "service account key" that must never be inside an app.
 *
 * Hosted free on Vercel. The secret key lives only in Vercel's settings
 * (environment variable FIREBASE_SERVICE_ACCOUNT), never in code or on GitHub.
 *
 * Request:  POST /api/set-password
 *           Header  Authorization: Bearer <the admin's Firebase ID token>
 *           Body    { "uid": "<user id>", "newPassword": "<6-64 characters>" }
 * Response: { "ok": true }  or  { "error": "..." }
 */
const admin = require("firebase-admin");

// Start the Admin SDK once (Vercel may reuse this between requests)
function firebase() {
  if (!admin.apps.length) {
    admin.initializeApp({
      credential: admin.credential.cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)),
    });
  }
  return admin;
}

module.exports = async (req, res) => {
  if (req.method !== "POST") return res.status(405).json({ error: "Use POST" });

  try {
    const fb = firebase();

    // 1. Who is asking? The app sends the logged-in admin's ID token; Firebase checks it is genuine.
    const header = req.headers.authorization || "";
    const token = header.startsWith("Bearer ") ? header.slice(7) : "";
    if (!token) return res.status(401).json({ error: "Please log in again." });
    const caller = await fb.auth().verifyIdToken(token);

    // 2. Is that person really the admin (and not blocked)? We check the database, not the app.
    const callerDoc = await fb.firestore().doc(`users/${caller.uid}`).get();
    if (!callerDoc.exists || callerDoc.get("role") !== "admin" || callerDoc.get("isBlocked") === true) {
      return res.status(403).json({ error: "Only the admin can change passwords." });
    }

    // 3. Check what was sent
    const { uid, newPassword } = req.body || {};
    if (typeof uid !== "string" || uid.length === 0) {
      return res.status(400).json({ error: "Missing user." });
    }
    if (typeof newPassword !== "string" || newPassword.length < 6 || newPassword.length > 64) {
      return res.status(400).json({ error: "The new password must be 6 to 64 characters." });
    }

    // 4. Only customers and providers (an admin's password is changed by that admin themselves)
    const target = await fb.firestore().doc(`users/${uid}`).get();
    if (!target.exists) return res.status(404).json({ error: "User not found." });
    if (target.get("role") === "admin") {
      return res.status(403).json({ error: "An admin's password cannot be changed here." });
    }

    // 5. Change the password and sign the user out on every phone
    await fb.auth().updateUser(uid, { password: newPassword });
    await fb.auth().revokeRefreshTokens(uid);

    // 6. Tell the user inside the app (the password itself is never stored or shown)
    const ref = fb.firestore().collection("notifications").doc();
    await ref.set({
      id: ref.id,
      userId: uid,
      title: "Password changed",
      message: "Your password was changed by the ServiceHub admin. Please log in with the new password. " +
        "If you did not expect this, contact the admin.",
      bookingId: "",
      isRead: false,
      createdAt: Date.now(),
    });

    return res.status(200).json({ ok: true });
  } catch (e) {
    console.error(e);
    if (e.code === "auth/user-not-found") {
      return res.status(404).json({ error: "This account has no login (for example a sample provider)." });
    }
    if (e.code === "auth/id-token-expired" || e.code === "auth/argument-error" || e.code === "auth/id-token-revoked") {
      return res.status(401).json({ error: "Your login expired. Please log in again." });
    }
    return res.status(500).json({ error: "Server error. Try again later." });
  }
};
