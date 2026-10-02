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
    /** Lo máximo que te has pasado hoy en una app. */
    val maxOverMillis: Long = 0L,
    /** Si ayer se rompió la racha, cuántos días tenía (0 si no). */
    val lostYesterday: Int = 0,
    /** Segunda oportunidad de hoy (racha perdida hoy) o de ayer (hoy es el día de prueba). */
    val recovery: Recovery? = null,
)

/**
 * Reglas de la racha:
 * - Un día cuenta si TODAS las apps vigiladas se quedaron dentro de su límite.
 * - Los días se cierran a medianoche; la racha es el número de días cerrados seguidos cumpliendo.
 * - Si hoy te pasas de un límite y no salvas la racha, la pierdes.
 * - Solo puedes salvarla si no te has pasado más de 15 minutos.
 * - Un cambio de límite no se aplica hasta el día siguiente.
 * - Si la pierdes del todo, esas apps se cierran ese día; si al día siguiente no las abres,
 *   la racha se recupera.
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
            // Segunda oportunidad: si el día después de perderla no abriste las apps cerradas,
            // el día perdido pasa a contar como salvado y la racha vuelve
            val r = store.recovery()
            if (r != null && !r.forfeited && r.day == day.minusDays(1)) {
                val used = UsageTracker.usageForDay(context, r.pkgs, day).values.sum()
                if (used <= Goal.GRACE_MS) {
                    store.markSaved(r.day)
                    store.putDayResult(r.day, true)
                } else {
                    store.forfeitRecovery()
                }
            }
            // Cada día se juzga con el límite que valía ese día
            val dayGoals = store.goals(day)
            val usage = UsageTracker.countedForDay(context, dayGoals.map { it.pkg }.toSet(), day)
            store.putDayResult(day, allWithinLimits(dayGoals, usage) || store.isSaved(day))
            store.setLastEvaluated(day)
            day = day.plusDays(1)
        }

        val streak = completedStreak(store, today)
        if (streak > store.bestStreak()) store.setBestStreak(streak)
    }

    /** Calcula cómo va hoy y aplica el bloqueo si la racha se ha perdido del todo. */
    fun refreshToday(context: Context): StreakInfo? {
        if (!UsageTracker.hasPermission(context)) return null
        val goals = Store(context).goals()
        if (goals.isEmpty()) return null
        evaluatePastDays(context)
        val usage = UsageTracker.countedToday(context, goals.map { it.pkg }.toSet())
        val info = info(context, goals, usage)
        applyTotalLoss(context, info)
        return info
    }

    /**
     * Racha perdida del todo (más de 15 minutos de más): se cierran hasta mañana las apps en
     * las que te has pasado y se apunta la segunda oportunidad para mañana.
     */
    fun applyTotalLoss(context: Context, info: StreakInfo) {
        if (info.todayOk || info.canSave || info.completed < 1 || info.overToday.isEmpty()) return
        val store = Store(context)
        val today = LocalDate.now()
        val previous = store.recovery()?.takeIf { it.day == today }
        if (previous?.forfeited == true) return
        val pkgs = info.overToday.map { it.pkg }.toSet() + previous?.pkgs.orEmpty()
        if (previous == null || pkgs != previous.pkgs) {
            store.setRecovery(Recovery(today, pkgs, info.completed))
        }
        val toBlock = pkgs.filter { !store.isBlockedToday(it) && !store.wasUnblockedToday(it) }
        if (toBlock.isNotEmpty()) {
            store.blockToday(toBlock)
            BlockerService.instance?.enforceNow()
        }
    }

    fun allWithinLimits(goals: List<Goal>, usage: Map<String, Long>): Boolean =
        goals.none { it.isOver(usage[it.pkg] ?: 0L) }

    /** Días cerrados seguidos cumpliendo antes de [today] (sin contarlo). */
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
        val yesterday = today.minusDays(1)
        val lostYesterday = if (store.dayResult(yesterday) == false) completedStreak(store, yesterday) else 0

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
            maxOverMillis = maxOver,
            lostYesterday = lostYesterday,
            recovery = store.recovery()?.takeIf { it.day == today || it.day == yesterday },
        )
    }
}
