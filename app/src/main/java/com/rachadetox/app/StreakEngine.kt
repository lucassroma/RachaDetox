package com.rachadetox.app

import android.content.Context
import java.time.LocalDate

data class DayStatus(val date: LocalDate, val ok: Boolean?, val isToday: Boolean, val saved: Boolean = false)

data class StreakInfo(
    /** Días seguidos cumpliendo (si hoy ya te has pasado, vale 0). */
    val current: Int,
    val best: Int,
    /** ¿Vas bien hoy? (ninguna app por encima de su límite) */
    val todayOk: Boolean,
    /** Últimos 7 días, del más antiguo a hoy. */
    val week: List<DayStatus>,
    /** Hoy te pasaste pero aceptaste el bloqueo. */
    val todaySaved: Boolean = false,
    /** Apps en las que hoy te has pasado. */
    val overToday: List<Goal> = emptyList(),
    /** Días cerrados seguidos cumpliendo hasta ayer (no cuenta hoy). */
    val completed: Int = 0,
    /** ¿Aún puedes salvar la racha? (no te has pasado más de 15 minutos en ninguna app) */
    val canSave: Boolean = false,
    /** Cuánto más te puedes pasar antes de que ya no se pueda salvar. */
    val saveLeftMillis: Long = 0L,
)

/**
 * Reglas de la racha:
 * - Un día cuenta si TODAS las apps vigiladas se quedaron dentro de su límite.
 * - Los días se cierran a medianoche; la racha es el número de días cerrados seguidos cumpliendo.
 * - Si hoy te pasas de un límite y no salvas la racha, la pierdes.
 * - Solo puedes salvarla si no te has pasado más de 15 minutos.
 * - Un cambio de límite no se aplica hasta el día siguiente.
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

        while (day.isBefore(today)) {
            // Cada día se juzga con el límite que valía ese día
            val dayGoals = store.goals(day)
            val usage = UsageTracker.usageForDay(context, dayGoals.map { it.pkg }.toSet(), day)
            store.putDayResult(day, allWithinLimits(dayGoals, usage) || store.isSaved(day))
            store.setLastEvaluated(day)
            day = day.plusDays(1)
        }

        val streak = completedStreak(store, today)
        if (streak > store.bestStreak()) store.setBestStreak(streak)
    }

    fun allWithinLimits(goals: List<Goal>, usage: Map<String, Long>): Boolean =
        goals.none { it.isOver(usage[it.pkg] ?: 0L) }

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
        val within = allWithinLimits(goals, usageToday)
        val overToday = goals.filter { it.isOver(usageToday[it.pkg] ?: 0L) }
        // Salvar la racha exige tener cerradas TODAS las apps en las que te has pasado.
        // Si desbloqueas una o te pasas con otra después de salvar, deja de estar salvada.
        // Y si con los minutos de regalo ya no te pasas, no hace falta tenerla salvada.
        if (store.isSaved(today) && (within || overToday.any { !store.isBlockedToday(it.pkg) })) {
            store.unmarkSaved(today)
        }
        val todaySaved = !within && store.isSaved(today)
        val maxOver = overToday.maxOfOrNull { it.overBy(usageToday[it.pkg] ?: 0L) } ?: 0L
        val saveLeft = (Goal.SAVE_WINDOW_MS - maxOver).coerceAtLeast(0L)
        val todayOk = within || todaySaved
        val completed = completedStreak(store, today)
        val current = if (todayOk) completed else 0

        val week = (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            if (back == 0) {
                DayStatus(d, if (todaySaved) true else if (todayOk) null else false, isToday = true, saved = todaySaved)
            } else {
                DayStatus(d, store.dayResult(d), isToday = false, saved = store.isSaved(d))
            }
        }
        return StreakInfo(
            current, maxOf(store.bestStreak(), completed), todayOk, week,
            todaySaved = todaySaved,
            overToday = overToday,
            completed = completed,
            canSave = overToday.isNotEmpty() && maxOver <= Goal.SAVE_WINDOW_MS,
            saveLeftMillis = saveLeft,
        )
    }
}
