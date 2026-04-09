package com.xiashuidaolaoshuren.allergyguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Dynamically-registered receiver that notifies a callback when the device connects to Wi-Fi.
 *
 * Register in onStart() and unregister in onStop() of the host component to avoid leaks.
 * Uses [ConnectivityManager.CONNECTIVITY_ACTION] which is only reliable for dynamic receivers.
 *
 * @param onWifiConnected Invoked on the main thread when an active Wi-Fi connection is detected.
 */
class ConnectivityReceiver(private val onWifiConnected: () -> Unit) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (isWifiConnected(context)) {
            onWifiConnected()
        }
    }

    companion object {
        fun isWifiConnected(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        }
    }
}
