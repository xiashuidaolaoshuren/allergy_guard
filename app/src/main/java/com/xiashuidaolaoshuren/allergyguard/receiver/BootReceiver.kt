package com.xiashuidaolaoshuren.allergyguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xiashuidaolaoshuren.allergyguard.util.NotificationHelper

/**
 * Manifest-registered receiver that fires when the device finishes booting.
 * Posts a daily-scan reminder notification so users are prompted to use the app.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        // Channels must exist before posting; createChannels() is idempotent.
        NotificationHelper.createChannels(context)
        NotificationHelper.postReminderNotification(context)
    }
}
