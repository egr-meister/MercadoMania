package com.mercadomania.game.integration

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mercadomania.game.MainActivity
import com.mercadomania.game.R

class PushService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // The token rotates on reinstall, restore and data-clear.
        IntegrationStorage.pushToken = token
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = PushPayload.from(message.data)
        // The whole data map goes into the notification's extras — exactly as the system
        // does when it draws the push itself for a backgrounded app. That gives the
        // Activity one entry point for both paths instead of two.
        showNotification(payload, message.data)
    }

    private fun showNotification(payload: PushPayload, data: Map<String, String>) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(manager)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(payload.title ?: getString(R.string.app_name))
            .setContentText(payload.body.orEmpty())
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private companion object {
        const val CHANNEL_ID = "offer_push"
        const val CHANNEL_NAME = "Notifications"
    }
}
