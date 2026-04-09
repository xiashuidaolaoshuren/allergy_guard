package com.xiashuidaolaoshuren.allergyguard.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.xiashuidaolaoshuren.allergyguard.MainActivity
import com.xiashuidaolaoshuren.allergyguard.R

object NotificationHelper {

    const val CHANNEL_REMINDERS = "channel_reminders"
    const val CHANNEL_ALLERGEN_ALERTS = "channel_allergen_alerts"

    private const val NOTIF_ID_BOOT_REMINDER = 1001

    fun createChannels(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.notif_channel_reminders_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notif_channel_reminders_desc)
            }
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALLERGEN_ALERTS,
                context.getString(R.string.notif_channel_allergen_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_allergen_desc)
            }
        )
    }

    fun postReminderNotification(context: Context) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(context.getString(R.string.notif_reminder_title))
            .setContentText(context.getString(R.string.notif_reminder_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID_BOOT_REMINDER, notification)
    }
}
