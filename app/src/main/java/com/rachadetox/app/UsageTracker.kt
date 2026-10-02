package com.rachadetox.app

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

/**
 * Calcula cuánto tiempo ha estado cada app en primer plano,
 * a partir de los eventos que registra Android (UsageStatsManager).
 */
object UsageTracker {

    // Tipos de evento (valores fijos de Android, usados como números para funcionar desde Android 8)
    private const val ACTIVITY_RESUMED = 1      // la app pasa a primer plano
    private const val ACTIVITY_PAUSED = 2       // la app deja el primer plano
    private const val SCREEN_NON_INTERACTIVE = 16 // pantalla apagada
    private const val ACTIVITY_STOPPED = 23
    private const val DEVICE_SHUTDOWN = 26

    /** Miramos 3 horas hacia atrás para no perder sesiones que empezaron antes de medianoche. */
    private const val LOOKBACK_MS = 3 * 60 * 60 * 1000L

    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun dayBounds(day: LocalDate): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return start to end
    }

    fun usageForDay(context: Context, packages: Set<String>, day: LocalDate): Map<String, Long> {
        val (start, end) = dayBounds(day)
        return usage(context, packages, start, minOf(end, System.currentTimeMillis()))
    }

    fun usageToday(context: Context, packages: Set<String>): Map<String, Long> =
        usageForDay(context, packages, LocalDate.now())

    /**
     * Uso que cuenta para los límites: el real menos los 5 minutos de regalo
     * («cinco minutos más, por favor») que se hayan gastado ese día.
     */
    fun countedForDay(context: Context, packages: Set<String>, day: LocalDate): Map<String, Long> {
        val store = Store(context)
        return usageForDay(context, packages, day).mapValues { (pkg, used) ->
            val base = store.extraBaseline(pkg, day) ?: return@mapValues used
            used - (used - base).coerceIn(0L, Store.EXTRA_MS)
        }
    }

    fun countedToday(context: Context, packages: Set<String>): Map<String, Long> =
        countedForDay(context, packages, LocalDate.now())

    /** Minutos de regalo que le quedan hoy a [pkg] (0 si no hay o ya se acabaron). */
    fun extraLeftToday(context: Context, pkg: String): Long {
        val base = Store(context).extraBaseline(pkg, LocalDate.now()) ?: return 0L
        val used = usageToday(context, setOf(pkg))[pkg] ?: 0L
        return (Store.EXTRA_MS - (used - base)).coerceAtLeast(0L)
    }

    /** Milisegundos en primer plano de cada paquete entre [start] y [end]. */
    fun usage(context: Context, packages: Set<String>, start: Long, end: Long): Map<String, Long> {
        val totals = HashMap<String, Long>()
        packages.forEach { totals[it] = 0L }
        if (packages.isEmpty() || end <= start) return totals

        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return totals
        val events = usm.queryEvents(start - LOOKBACK_MS, end) ?: return totals

        val openSince = HashMap<String, Long>()                 // paquete -> desde cuándo está delante
        val openActivities = HashMap<String, MutableSet<String>>() // pantallas abiertas del paquete

        fun addSession(pkg: String, from: Long, to: Long) {
            val clippedFrom = maxOf(from, start)
            val clippedTo = minOf(to, end)
            if (clippedTo > clippedFrom) {
                totals[pkg] = (totals[pkg] ?: 0L) + (clippedTo - clippedFrom)
            }
        }

        fun closeAll(at: Long) {
            for ((pkg, set) in openActivities) {
                if (set.isNotEmpty()) {
                    addSession(pkg, openSince[pkg] ?: at, at)
                    set.clear()
                }
            }
        }

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val time = event.timeStamp
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                ACTIVITY_RESUMED -> {
                    if (pkg !in packages) continue
                    val set = openActivities.getOrPut(pkg) { mutableSetOf() }
                    if (set.isEmpty()) openSince[pkg] = time
                    set.add(event.className ?: "")
                }

                ACTIVITY_PAUSED, ACTIVITY_STOPPED -> {
                    if (pkg !in packages) continue
                    val set = openActivities[pkg] ?: continue
                    if (set.remove(event.className ?: "") && set.isEmpty()) {
                        addSession(pkg, openSince[pkg] ?: time, time)
                    }
                }

                SCREEN_NON_INTERACTIVE, DEVICE_SHUTDOWN -> closeAll(time)
            }
        }
        // Lo que sigue abierto ahora mismo cuenta hasta el final del intervalo
        closeAll(end)
        return totals
    }
}
