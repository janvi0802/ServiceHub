package com.example.servicehub.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.servicehub.BuildConfig
import com.example.servicehub.MainActivity
import com.example.servicehub.R
import com.example.servicehub.data.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Push notifications (Firebase Cloud Messaging, free).
 *
 * How it works:
 *  1. Each phone gets a "token" (its address for push) and saves it in users/{uid}.fcmToken.
 *  2. When the app creates an in-app notification, it also asks our Vercel server to push it
 *     ([Push.send]). The server checks the sender belongs to that booking, then sends the push.
 *  3. The phone shows it in the notification bar, even when the app is closed.
 */
class PushService : FirebaseMessagingService() {

    /** Firebase gave this phone a new token: save it for the logged-in user. */
    override fun onNewToken(token: String) {
        Push.scope.launch { runCatching { Push.saveToken(token) } }
    }

    /**
     * A push arrived while the app is OPEN (when it is closed, Android shows it by itself).
     * We show it in the notification bar too, so it looks the same either way.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"].orEmpty()
        Push.show(this, title, body, message.data["bookingId"].orEmpty(), message.data["type"].orEmpty())
    }
}

object Push {
    private const val CHANNEL_ID = "bookings"
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Push needs the admin server (the same ADMIN_API_URL as for passwords). */
    private val serverReady get() = BuildConfig.ADMIN_API_URL.startsWith("https://")

    /** After login: get this phone's token and save it on the user's document. */
    suspend fun registerThisPhone() {
        val token = FirebaseMessaging.getInstance().token.await()
        saveToken(token)
    }

    internal suspend fun saveToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // fcmTokenAt: if two accounts were used on this phone, the server pushes only to the
        // one that logged in LAST (so an old account's messages never pop up here).
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update(mapOf("fcmToken" to token, "fcmTokenAt" to System.currentTimeMillis())).await()
    }

    /**
     * Ask the server to push notification [notificationId] to its person.
     * Never throws and never waits: if push fails, the in-app notification is still there.
     */
    fun send(notificationId: String) {
        if (!serverReady) return
        scope.launch {
            runCatching {
                val user = FirebaseAuth.getInstance().currentUser ?: return@launch
                val idToken = user.getIdToken(false).await().token ?: return@launch
                withContext(Dispatchers.IO) {
                    val url = java.net.URL(BuildConfig.ADMIN_API_URL.trimEnd('/') + "/api/send-push")
                    val c = (url.openConnection() as java.net.HttpURLConnection).apply {
                        requestMethod = "POST"; connectTimeout = 10_000; readTimeout = 10_000; doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Authorization", "Bearer $idToken")
                    }
                    try {
                        c.outputStream.use { it.write(org.json.JSONObject().put("notificationId", notificationId).toString().toByteArray()) }
                        c.responseCode   // send the request; the answer itself is not needed
                    } finally {
                        c.disconnect()
                    }
                }
            }
        }
    }

    /** Shows one notification in the phone's notification bar; tapping it opens the app. */
    fun show(context: Context, title: String, body: String, bookingId: String = "", type: String = "") {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Bookings", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        // Android 13+: only if the user allowed notifications
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        // Tapping opens the app ON the right screen (see PushLink): the booking and the kind of page
        val tap = Intent(context, MainActivity::class.java).apply {
            putExtra(PushLink.EXTRA_BOOKING, bookingId)
            putExtra(PushLink.EXTRA_TYPE, type)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val open = PendingIntent.getActivity(
            context, bookingId.hashCode(), tap, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
}

/**
 * Which screen to open after tapping a push: the booking it is about and its kind:
 * "chat" -> the chat, "booking" -> booking / job details, "" -> just the app.
 * MainActivity fills [pending]; AppNavHost opens the screen once someone is logged in.
 */
object PushLink {
    const val EXTRA_BOOKING = "bookingId"   // same names as the push "data" sent by the server,
    const val EXTRA_TYPE = "type"           // so a push tapped while the app was closed works too
    const val TYPE_CHAT = "chat"

    val pending = kotlinx.coroutines.flow.MutableStateFlow<Pair<String, String>?>(null)

    /** Reads the extras of the Intent that opened the app (null-safe; ignored if no booking). */
    fun take(intent: Intent?) {
        val bookingId = intent?.getStringExtra(EXTRA_BOOKING).orEmpty()
        if (bookingId.isNotBlank()) pending.value = bookingId to intent?.getStringExtra(EXTRA_TYPE).orEmpty()
        intent?.removeExtra(EXTRA_BOOKING)   // so turning the phone does not open it again
    }

    /** "New message from …" opens the chat; everything else about a booking opens its details. */
    fun typeForTitle(title: String) = if (title.startsWith("New message", ignoreCase = true)) TYPE_CHAT else "booking"
}