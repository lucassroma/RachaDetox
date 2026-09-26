package com.rachadetox.app

import android.content.Context
import java.time.LocalDate

data class DayStatus(val date: LocalDate, val ok: Boolean?, val isToday: Boolean)

data class StreakInfo(
    /** Días seguidos cumpliendo (si hoy ya te has pasado, vale 0). */
    val current: Int,
    val best: Int,
    /** ¿Vas bien hoy? (ninguna app por encima de su límite) */
    val todayOk: Boolean,
    /** Últimos 7 días, del más antiguo a hoy. */
    val week: List<DayStatus>,
)

/**
 * Reglas de la racha:
 * - Un día cuenta si TODAS las apps vigiladas se quedaron dentro de su límite.
 * - Los días se cierran a medianoche; la racha es el número de días cerrados seguidos cumpliendo.
 * - Si hoy te pasas de un límite, la racha vuelve a 0.
 */
object StreakEngine {

    /** Revisa los días pasados que aún no se han evaluado y guarda si se cumplieron. */
    @Synchronized
    fun evaluatePastDays(context: Context) {
        if (!UsageTracker.hasPermission(context)) return
        val store = Store(context)
        val goals = store.goals()
        if (goals.isEmpty()) return
        val start = store.startDate() ?: return

        val today = LocalDate.now()
        var day = store.lastEvaluated()?.plusDays(1) ?: start
        if (day.isBefore(start)) day = start
        // Android solo guarda unos días de historial detallado
        val earliest = today.minusDays(10)
        if (day.isBefore(earliest)) day = earliest

        val packages = goals.map { it.pkg }.toSet()
        while (day.isBefore(today)) {
            val usage = UsageTracker.usageForDay(context, packages, day)
            store.putDayResult(day, allWithinLimits(goals, usage))
            store.setLastEvaluated(day)
            day = day.plusDays(1)
        }

        val streak = completedStreak(store, today)
        if (streak > store.bestStreak()) store.setBestStreak(streak)
    }

    fun allWithinLimits(goals: List<Goal>, usage: Map<String, Long>): Boolean =
        goals.all { (usage[it.pkg] ?: 0L) <= it.limitMillis }

    private fun completedStreak(store: Store, today: LocalDate): Int {
        var count = 0
        var day = today.minusDays(1)
        while (store.dayResult(day) == true) {
            count++
            day = day.minusDays(1)
        }
        return count
    }

    fun info(context: Context, goals: List<Goal>, usageToday: Map<String, Long>): StreakInfo {
        val store = Store(context)
        val today = LocalDate.now()
        val todayOk = allWithinLimits(goals, usageToday)
        val completed = completedStreak(store, today)
        val current = if (todayOk) completed else 0

        val week = (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            if (back == 0) {
                DayStatus(d, if (todayOk) null else false, isToday = true)
            } else {
                DayStatus(d, store.dayResult(d), isToday = false)
            }
        }
        return StreakInfo(current, maxOf(store.bestStreak(), completed), todayOk, week)
    }
}
