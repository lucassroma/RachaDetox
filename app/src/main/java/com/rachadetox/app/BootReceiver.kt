package com.rachadetox.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Vuelve a arrancar la vigilancia cuando se reinicia el móvil o se actualiza la app. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Store(context).goals().isNotEmpty() && UsageTracker.hasPermission(context)) {
            MonitorService.start(context)
        }
    }
}
