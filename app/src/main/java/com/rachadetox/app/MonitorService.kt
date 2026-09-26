package com.rachadetox.app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Servicio que corre en segundo plano: cada 20 segundos mira cuánto llevas
 * en cada app y te avisa si te acercas al límite.
 */
class MonitorService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null
    private var lastOngoingText = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifier.createChannels(this)
        val notification = Notifier.ongoing(this, "Alba", "Contando tu tiempo de hoy…")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(Notifier.ONGOING_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifier.ONGOING_ID, notification)
        }

        if (loop?.isActive != true) {
            loop = scope.launch {
                while (isActive) {
                    val keepGoing = try {
                        tick()
                    } catch (e: Exception) {
                        true
                    }
                    if (!keepGoing) {
                        stopSelf()
                        break
                    }
                    delay(TICK_MS)
                }
            }
        }
        return START_STICKY
    }

    /** Devuelve false si ya no hay nada que vigilar. */
    private fun tick(): Boolean {
        val ctx: Context = this
        if (!UsageTracker.hasPermission(ctx)) return false
        val store = Store(ctx)
        val goals = store.goals()
        if (goals.isEmpty()) return false

        StreakEngine.evaluatePastDays(ctx)
        val usage = UsageTracker.usageToday(ctx, goals.map { it.pkg }.toSet())
        val info = StreakEngine.info(ctx, goals, usage)
        val today = LocalDate.now().toString()
        store.pruneAlerts(today)

        for (goal in goals) {
            val used = usage[goal.pkg] ?: 0L
            val left = goal.limitMillis - used
            val baseId = 1000 + (goal.pkg.hashCode() and 0xFFFF) * 4

            fun once(level: String, id: Int, title: String, text: String) {
                val key = "$today|${goal.pkg}|$level"
                if (!store.alertSent(key)) {
                    store.markAlert(key)
                    Notifier.alert(ctx, id, title, text)
                }
            }

            // Bloqueo automático: al llegar al límite, la app se cierra hasta mañana
            if (used >= goal.limitMillis && store.autoBlockActive() && BlockerService.isEnabled(ctx) &&
                !store.isBlockedToday(goal.pkg)
            ) {
                store.blockToday(listOf(goal.pkg))
                BlockerService.instance?.enforceNow()
                once("auto", baseId + 3, "Hasta mañana, ${goal.label}", "Hoy ya le has dado su tiempo.")
                continue
            }

            when {
                goal.isOver(used) -> if (!info.todaySaved) once(
                    "over", baseId + 3,
                    "Hoy se ha nublado",
                    "Te has pasado con ${goal.label}. Toca aquí si quieres salvar tu racha."
                )

                left <= 60_000L -> once(
                    "last", baseId + 2,
                    "Un minuto más de ${goal.label}",
                    if (info.current > 0) "¿Merece la pena? Llevas ${info.current} ${dias(info.current)} seguidos."
                    else "¿Merece la pena?"
                )

                used >= goal.limitMillis * 8 / 10 -> once(
                    "80", baseId + 1,
                    "Te quedan ${formatDuration(left)} de ${goal.label}",
                    "Llevas ${formatDuration(used)} de ${formatMinutes(goal.limitMinutes)} hoy."
                )
            }
        }

        val title = if (info.todayOk) "${info.current} ${dias(info.current)} seguidos"
        else "Hoy se ha nublado"
        val text = goals.joinToString(" · ") {
            "${it.label} ${formatDuration(usage[it.pkg] ?: 0L)}/${formatMinutes(it.limitMinutes)}"
        }
        if (title + text != lastOngoingText) {
            lastOngoingText = title + text
            Notifier.updateOngoing(ctx, title, text)
        }
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TICK_MS = 20_000L

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, MonitorService::class.java))
            } catch (_: Exception) {
                // Android puede bloquear el arranque en segundo plano; se reintentará al abrir la app
            }
        }
    }
}

fun dias(n: Int) = if (n == 1) "día" else "días"
