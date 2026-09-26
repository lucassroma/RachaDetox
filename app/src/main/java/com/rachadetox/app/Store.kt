package com.rachadetox.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Una app vigilada y su límite diario. */
data class Goal(val pkg: String, val label: String, val limitMinutes: Int) {
    val limitMillis: Long get() = limitMinutes * 60_000L
}

/** Guarda todo en el propio móvil (SharedPreferences). Nada sale del teléfono. */
class Store(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("racha_detox", Context.MODE_PRIVATE)

    // ---------- Apps y límites ----------

    fun goals(): List<Goal> {
        val arr = JSONArray(prefs.getString(KEY_GOALS, "[]"))
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Goal(o.getString("pkg"), o.getString("label"), o.getInt("limit"))
        }
    }

    private fun saveGoals(goals: List<Goal>) {
        val arr = JSONArray()
        goals.forEach {
            arr.put(JSONObject().put("pkg", it.pkg).put("label", it.label).put("limit", it.limitMinutes))
        }
        prefs.edit().putString(KEY_GOALS, arr.toString()).apply()
    }

    fun upsertGoal(goal: Goal) {
        val current = goals()
        val updated = if (current.any { it.pkg == goal.pkg }) {
            current.map { if (it.pkg == goal.pkg) goal else it }
        } else {
            current + goal
        }
        saveGoals(updated)
        if (startDate() == null) {
            prefs.edit().putString(KEY_START, LocalDate.now().toString()).apply()
        }
    }

    fun removeGoal(pkg: String) = saveGoals(goals().filter { it.pkg != pkg })

    // ---------- Historial de días ----------

    /** Primer día en que se empezó a usar la app (la racha no mira antes). */
    fun startDate(): LocalDate? = prefs.getString(KEY_START, null)?.let { LocalDate.parse(it) }

    fun lastEvaluated(): LocalDate? = prefs.getString(KEY_LAST_EVAL, null)?.let { LocalDate.parse(it) }

    fun setLastEvaluated(day: LocalDate) {
        prefs.edit().putString(KEY_LAST_EVAL, day.toString()).apply()
    }

    private fun history(): JSONObject = JSONObject(prefs.getString(KEY_HISTORY, "{}") ?: "{}")

    /** true = cumplió, false = se pasó, null = sin datos. */
    fun dayResult(day: LocalDate): Boolean? {
        val h = history()
        val key = day.toString()
        return if (h.has(key)) h.getBoolean(key) else null
    }

    fun putDayResult(day: LocalDate, ok: Boolean) {
        val h = history().put(day.toString(), ok)
        prefs.edit().putString(KEY_HISTORY, h.toString()).apply()
    }

    fun bestStreak(): Int = prefs.getInt(KEY_BEST, 0)

    fun setBestStreak(value: Int) {
        prefs.edit().putInt(KEY_BEST, value).apply()
    }

    // ---------- Avisos ya enviados (para no repetirlos) ----------

    fun alertSent(key: String): Boolean =
        prefs.getStringSet(KEY_ALERTS, emptySet())?.contains(key) == true

    fun markAlert(key: String) {
        val set = HashSet(prefs.getStringSet(KEY_ALERTS, emptySet()) ?: emptySet())
        set.add(key)
        prefs.edit().putStringSet(KEY_ALERTS, set).apply()
    }

    /** Borra los avisos de días anteriores. */
    fun pruneAlerts(today: String) {
        val set = prefs.getStringSet(KEY_ALERTS, emptySet()) ?: return
        val kept = set.filter { it.startsWith(today) }.toSet()
        if (kept.size != set.size) prefs.edit().putStringSet(KEY_ALERTS, HashSet(kept)).apply()
    }

    companion object {
        private const val KEY_GOALS = "goals"
        private const val KEY_START = "start_date"
        private const val KEY_LAST_EVAL = "last_evaluated"
        private const val KEY_HISTORY = "history"
        private const val KEY_BEST = "best_streak"
        private const val KEY_ALERTS = "alerts"
    }
}
