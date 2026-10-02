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
        val notification = Notifier.ongoing(this, "Alba", tr("Contando tu tiempo de hoy…", "Counting your time today…", "Sto contando il tuo tempo di oggi…"))
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
                // Con los minutos de regalo, los avisos pueden volver a salir
                val key = "$today|${goal.pkg}|$level" + if (goal.extraMinutes > 0) "+" else ""
                if (!store.alertSent(key)) {
                    store.markAlert(key)
                    Notifier.alert(ctx, id, title, text)
                }
            }

            // Bloqueo automático: al llegar al límite, la app se cierra hasta mañana
            if (used >= goal.limitMillis && store.autoBlockActive() && BlockerService.isEnabled(ctx) &&
                !store.isBlockedToday(goal.pkg) && !store.wasUnblockedToday(goal.pkg)
            ) {
                store.blockToday(listOf(goal.pkg))
                BlockerService.instance?.enforceNow()
                once(
                    "auto", baseId + 3,
                    tr("Hasta mañana, ${goal.label}", "See you tomorrow, ${goal.label}", "A domani, ${goal.label}"),
                    tr("Hoy ya le has dado su tiempo.", "You have already given it its time today.", "Oggi gli hai già dato il suo tempo."),
                )
                continue
            }

            when {
                goal.isOver(used) -> if (!info.todaySaved) once(
                    "over", baseId + 3,
                    tr("Hoy se ha nublado", "Clouds rolled in today", "Oggi si è rannuvolato"),
                    tr(
                        "Te has pasado con ${goal.label}. Si no salvas tu racha, la pierdes, y pasados 15 minutos más ya no se puede. Toca aquí.",
                        "You went over on ${goal.label}. If you don't save your streak, you lose it, and after 15 more minutes you can't. Tap here.",
                        "Hai superato il limite con ${goal.label}. Se non salvi la tua serie, la perdi, e dopo altri 15 minuti non si può più. Tocca qui.",
                    )
                )

                left <= 60_000L -> once(
                    "last", baseId + 2,
                    tr("Un minuto más de ${goal.label}", "One more minute of ${goal.label}", "Ancora un minuto di ${goal.label}"),
                    if (info.current > 0) tr("¿Merece la pena? ", "Is it worth it? ", "Ne vale la pena? ") + inARow(info.current) + "."
                    else tr("¿Merece la pena?", "Is it worth it?", "Ne vale la pena?")
                )

                used >= goal.limitMillis * 8 / 10 -> once(
                    "80", baseId + 1,
                    tr(
                        "Te quedan ${formatDuration(left)} de ${goal.label}",
                        "${formatDuration(left)} of ${goal.label} left",
                        "Ti restano ${formatDuration(left)} di ${goal.label}",
                    ),
                    tr(
                        "Llevas ${formatDuration(used)} de ${formatMinutes(goal.todayMinutes)} hoy.",
                        "You have used ${formatDuration(used)} of ${formatMinutes(goal.todayMinutes)} today.",
                        "Hai usato ${formatDuration(used)} di ${formatMinutes(goal.todayMinutes)} oggi.",
                    )
                )
            }
        }

        val title = if (info.todayOk) inARow(info.current)
        else tr("Hoy se ha nublado", "Clouds rolled in today", "Oggi si è rannuvolato")
        val text = goals.joinToString(" · ") {
            "${it.label} ${formatDuration(usage[it.pkg] ?: 0L)}/${formatMinutes(it.todayMinutes)}"
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

fun dias(n: Int) = if (n == 1) tr("día", "day", "giorno") else tr("días", "days", "giorni")

/** "3 días seguidos" / "3 days in a row" / "3 giorni di fila" */
fun inARow(n: Int) = tr(
    "$n ${dias(n)} seguidos".replace("día seguidos", "día seguido"),
    "$n ${dias(n)} in a row",
    "$n ${dias(n)} di fila",
)
