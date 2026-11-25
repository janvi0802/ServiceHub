/*
 * ServiceHub push server: sends one in-app notification to the person's phone as a PUSH
 * (Firebase Cloud Messaging, free). Hosted on the same free Vercel project as set-password.
 *
 * Why a server? Sending a push needs the secret service-account key, which must never be
 * inside the app. The app only says "please push notification X"; the server checks it.
 *
 * Request:  POST /api/send-push
 *           Header  Authorization: Bearer <the sender's Firebase ID token>
 *           Body    { "notificationId": "<id in the notifications collection>" }
 * Response: { "ok": true, "sent": true|false }  or  { "error": "..." }
 */
const admin = require("firebase-admin");

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
    const db = fb.firestore();

    // 1. Who is asking? (a logged-in ServiceHub user)
    const header = req.headers.authorization || "";
    const token = header.startsWith("Bearer ") ? header.slice(7) : "";
    if (!token) return res.status(401).json({ error: "Please log in again." });
    const caller = await fb.auth().verifyIdToken(token);

    // 2. Read the notification the app just created
    const { notificationId } = req.body || {};
    if (typeof notificationId !== "string" || !notificationId) return res.status(400).json({ error: "Missing notification." });
    const note = await db.doc(`notifications/${notificationId}`).get();
    if (!note.exists) return res.status(404).json({ error: "Notification not found." });
    const n = note.data();

    // Only fresh notifications (no re-sending old ones)
    if (Date.now() - (n.createdAt || 0) > 5 * 60 * 1000) return res.status(200).json({ ok: true, sent: false });

    // 3. May this caller push it? The admin always; anyone else only about a booking they belong to
    //    (the same rule as firestore.rules for creating notifications).
    const callerDoc = await db.doc(`users/${caller.uid}`).get();
    const isAdmin = callerDoc.exists && callerDoc.get("role") === "admin";
    if (!isAdmin) {
      if (!n.bookingId) return res.status(403).json({ error: "Not allowed." });
      const booking = await db.doc(`bookings/${n.bookingId}`).get();
      const b = booking.data() || {};
      const onBooking = (uid) => uid === b.customerId || uid === b.providerId;
      if (!booking.exists || !onBooking(caller.uid) || !onBooking(n.userId)) {
        return res.status(403).json({ error: "Not allowed." });
      }
    }

    // 4. The receiver's phone address (token). If the same phone was later used by another
    //    account, only the account that logged in last gets pushes on it.
    const target = await db.doc(`users/${n.userId}`).get();
    const fcmToken = target.exists ? target.get("fcmToken") : "";
    if (!fcmToken) return res.status(200).json({ ok: true, sent: false });
    const sameToken = await db.collection("users").where("fcmToken", "==", fcmToken).get();
    const latest = sameToken.docs.sort((a, b) => (b.get("fcmTokenAt") || 0) - (a.get("fcmTokenAt") || 0))[0];
    if (latest && latest.id !== n.userId) return res.status(200).json({ ok: true, sent: false });

    // 5. Send it
    await fb.messaging().send({
      token: fcmToken,
      notification: { title: n.title, body: n.message },
      data: { bookingId: n.bookingId || "" },
      android: { priority: "high", notification: { channelId: "bookings" } },
    });
    return res.status(200).json({ ok: true, sent: true });
  } catch (e) {
    console.error(e);
    // A phone that uninstalled the app: its token no longer works. Not an error for the app.
    if (e.code === "messaging/registration-token-not-registered" || e.code === "messaging/invalid-argument") {
      return res.status(200).json({ ok: true, sent: false });
    }
    if (e.code && e.code.startsWith("auth/")) return res.status(401).json({ error: "Your login expired." });
    return res.status(500).json({ error: "Server error." });
  }
};
