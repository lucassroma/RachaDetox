package com.rachadetox.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object Notifier {
    private const val CHANNEL_MONITOR = "monitor"
    private const val CHANNEL_ALERTS = "alerts"
    const val ONGOING_ID = 1

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_MONITOR, "Alba en segundo plano", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Muestra tu racha y tu tiempo de hoy"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Avisos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Te avisa cuando te queda poco tiempo en una app"
            }
        )
    }

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun ongoing(context: Context, title: String, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_stat_flame)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppIntent(context))
            .build()

    fun updateOngoing(context: Context, title: String, text: String) {
        notify(context, ONGOING_ID, ongoing(context, title, text))
    }

    fun alert(context: Context, id: Int, title: String, text: String) {
        val n = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_flame)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()
        notify(context, id, n)
    }

    private fun notify(context: Context, id: Int, notification: Notification) {
        try {
            context.getSystemService(NotificationManager::class.java)?.notify(id, notification)
        } catch (_: SecurityException) {
            // Sin permiso de notificaciones: no podemos avisar
        }
    }
}

/** 95 min -> "1 h 35 min" */
fun formatDuration(millis: Long): String {
    val totalMin = (millis / 60_000L).coerceAtLeast(0L)
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h == 0L -> "$m min"
        m == 0L -> "$h h"
        else -> "$h h $m min"
    }
}

fun formatMinutes(minutes: Int): String = formatDuration(minutes * 60_000L)
