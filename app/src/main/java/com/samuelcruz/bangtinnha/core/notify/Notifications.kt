package com.samuelcruz.bangtinnha.core.notify

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
import com.samuelcruz.bangtinnha.MainActivity
import com.samuelcruz.bangtinnha.R

/** One channel per kind of notification, so each can be silenced separately in Android's settings. */
enum class Channel(val id: String, val title: String, val importance: Int) {
    MORNING("morning", "Bản tin buổi sáng", NotificationManager.IMPORTANCE_DEFAULT),
    POWER("power", "Cúp điện", NotificationManager.IMPORTANCE_HIGH),
    FUEL("fuel", "Giá xăng", NotificationManager.IMPORTANCE_DEFAULT),
    WEATHER("weather", "Thời tiết xấu", NotificationManager.IMPORTANCE_HIGH),
}

class Notifications(private val context: Context) {
    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        Channel.entries.forEach { manager.createNotificationChannel(NotificationChannel(it.id, it.title, it.importance)) }
    }

    fun canPost(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Shows [lines] (first line is the summary) in an expandable notification; tapping opens the app. */
    fun post(channel: Channel, notificationId: Int, title: String, lines: List<String>) {
        if (!canPost() || lines.isEmpty()) return
        val open = PendingIntent.getActivity(
            context,
            notificationId,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, channel.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call: nothing to show.
        }
    }
}
