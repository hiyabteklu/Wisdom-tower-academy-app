package com.wisdomtower.academy.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wisdomtower.academy.MainActivity
import com.wisdomtower.academy.R

class AcademyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        FcmTokenRegistrar.sendTokenToBackend(applicationContext, token, force = true)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val targetUrl = data["url"]
            ?: data["link"]
            ?: data["path"]
            ?: data["target_url"]
            ?: "/notifications"

        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: getString(R.string.app_name)

        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: data["message"]
            ?: "You have a new update from Wisdom Tower Academy"

        showSystemNotification(title, body, targetUrl)
    }

    private fun showSystemNotification(title: String, body: String, targetUrl: String) {
        val channelId = CHANNEL_ID
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_URL, targetUrl)
        }

        val requestCode = (System.currentTimeMillis() % 100000).toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val accentColor = ContextCompat.getColor(this, R.color.brand_accent)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(accentColor)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Wisdom Tower Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Updates, course announcements, and notifications from Wisdom Tower Academy"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "AcademyFCM"
        const val CHANNEL_ID = "wisdom_tower_notifications"
        const val EXTRA_TARGET_URL = "extra_target_url"
    }
}
