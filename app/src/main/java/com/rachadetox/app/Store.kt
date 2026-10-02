package com.rachadetox.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Una app vigilada y su límite diario.
 * [nextLimitMinutes]: límite nuevo que se ha pedido y que empieza a contar mañana.
 */
data class Goal(
    val pkg: String,
    val label: String,
    val limitMinutes: Int,
    val nextLimitMinutes: Int? = null,
) {
    val limitMillis: Long get() = limitMinutes * 60_000L

    /** Te has pasado (con 1 minuto de margen, porque la comprobación no es instantánea). */
    fun isOver(usedMillis: Long): Boolean = usedMillis > limitMillis + GRACE_MS

    /** Cuánto te has pasado del límite (0 si no). */
    fun overBy(usedMillis: Long): Long = (usedMillis - limitMillis).coerceAtLeast(0L)

    companion object {
        const val GRACE_MS = 60_000L

        /** Solo puedes salvar la racha si no te has pasado más de esto. */
        const val SAVE_WINDOW_MS = 15 * 60_000L
    }
}

/**
 * Racha perdida del todo el día [day]: esas [pkgs] se cerraron ese día. Si al día siguiente
 * no las abres, la racha de [streak] días se recupera. [forfeited]: las abriste, ya no se puede.
 */
data class Recovery(val day: LocalDate, val pkgs: Set<String>, val streak: Int, val forfeited: Boolean = false)

/** Guarda todo en el propio móvil (SharedPreferences). Nada sale del teléfono. */
class Store(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("racha_detox", Context.MODE_PRIVATE)

    // ---------- Apps y límites ----------

    /**
     * Cómo se guarda cada app: "limit" es el límite vigente y, si se ha cambiado,
     * "next" es el nuevo, que vale a partir del día "nextFrom".
     */
    private fun rawGoals(): List<JSONObject> {
        val arr = JSONArray(prefs.getString(KEY_GOALS, "[]"))
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    private fun saveRaw(goals: List<JSONObject>) {
        prefs.edit().putString(KEY_GOALS, JSONArray(goals).toString()).apply()
    }

    /** Las apps con el límite que vale el día [day] (por defecto, hoy). */
    fun goals(day: LocalDate = LocalDate.now()): List<Goal> = rawGoals().map { o ->
        val next = if (o.has("next")) o.getInt("next") else null
        val from = o.optString("nextFrom").takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) }
        val nextApplies = next != null && from != null && !day.isBefore(from)
        Goal(
            o.getString("pkg"),
            o.getString("label"),
            if (nextApplies) next!! else o.getInt("limit"),
            nextLimitMinutes = if (next != null && !nextApplies) next else null,
        )
    }

    // ---------- «Cinco minutos más, por favor» (una vez al día) ----------
    // No sube el límite: desde que lo pulsas, los siguientes 5 minutos de cada app son de
    // regalo y no cuentan, aunque ya te hayas pasado o la app esté cerrada.
    // Se guarda, por día, cuánto llevaba cada app al pulsarlo.

    private fun extraWindows(): JSONObject = try {
        JSONObject(prefs.getString(KEY_EXTRA_WINDOWS, "{}") ?: "{}")
    } catch (_: Exception) {
        JSONObject()
    }

    fun extraUsed(day: LocalDate): Boolean =
        extraWindows().has(day.toString()) ||
            prefs.getStringSet(KEY_EXTRA, emptySet())?.contains(day.toString()) == true

    /** Cuánto llevaba [pkg] el día [day] al pulsar «cinco minutos más» (null si no se pulsó). */
    fun extraBaseline(pkg: String, day: LocalDate): Long? {
        val o = extraWindows().optJSONObject(day.toString()) ?: return null
        return if (o.has(pkg)) o.getLong(pkg) else null
    }

    /** Pulsa «cinco minutos más» con lo que lleva ahora cada app. Devuelve false si ya se usó hoy. */
    fun useExtra(usedNow: Map<String, Long>): Boolean {
        val today = LocalDate.now()
        if (extraUsed(today)) return false
        val all = extraWindows()
        // Guardamos solo los últimos días: hacen falta para cerrar los días pendientes
        val keepFrom = today.minusDays(15)
        all.keys().asSequence().toList()
            .filter { LocalDate.parse(it).isBefore(keepFrom) }
            .forEach { all.remove(it) }
        val day = JSONObject()
        usedNow.forEach { (pkg, used) -> day.put(pkg, used) }
        all.put(today.toString(), day)
        prefs.edit().putString(KEY_EXTRA_WINDOWS, all.toString()).apply()
        return true
    }

    /** Añade una app nueva. Si la quitaste hoy, vuelve con el límite que tenía y el nuevo vale desde mañana. */
    fun addGoal(goal: Goal) {
        val today = LocalDate.now()
        val removed = JSONObject(prefs.getString(KEY_REMOVED, "{}") ?: "{}").optJSONObject(goal.pkg)
        val o = JSONObject().put("pkg", goal.pkg).put("label", goal.label)
        if (removed != null && removed.optString("day") == today.toString()) {
            val old = removed.getInt("limit")
            o.put("limit", old)
            if (old != goal.limitMinutes) o.put("next", goal.limitMinutes).put("nextFrom", today.plusDays(1).toString())
        } else {
            o.put("limit", goal.limitMinutes)
        }
        saveRaw(rawGoals().filter { it.getString("pkg") != goal.pkg } + o)
        if (startDate() == null) {
            prefs.edit().putString(KEY_START, today.toString()).apply()
        }
    }

    /** Cambia el límite de una app. El cambio no se aplica hasta mañana. */
    fun changeLimit(pkg: String, minutes: Int) {
        val today = LocalDate.now()
        val todayLimit = goals(today).firstOrNull { it.pkg == pkg }?.limitMinutes ?: return
        saveRaw(rawGoals().map { o ->
            if (o.getString("pkg") == pkg) {
                o.put("limit", todayLimit)
                if (minutes == todayLimit) {
                    o.remove("next")
                    o.remove("nextFrom")
                } else {
                    o.put("next", minutes).put("nextFrom", today.plusDays(1).toString())
                }
            }
            o
        })
    }

    fun removeGoal(pkg: String) {
        // Recordamos el límite de hoy para que quitar y volver a añadir no sirva para saltárselo
        goals().firstOrNull { it.pkg == pkg }?.let {
            val removed = JSONObject(prefs.getString(KEY_REMOVED, "{}") ?: "{}")
            removed.put(pkg, JSONObject().put("day", LocalDate.now().toString()).put("limit", it.limitMinutes))
            prefs.edit().putString(KEY_REMOVED, removed.toString()).apply()
        }
        saveRaw(rawGoals().filter { it.getString("pkg") != pkg })
    }

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

    // ---------- Bloqueos ----------

    /** Apps bloqueadas hoy (hasta medianoche). */
    fun isBlockedToday(pkg: String): Boolean =
        prefs.getStringSet(KEY_BLOCKS, emptySet())?.contains("${LocalDate.now()}|$pkg") == true

    fun blockToday(pkgs: Collection<String>) {
        val today = LocalDate.now().toString()
        val set = HashSet((prefs.getStringSet(KEY_BLOCKS, emptySet()) ?: emptySet()).filter { it.startsWith(today) })
        pkgs.forEach { set.add("$today|$it") }
        prefs.edit().putStringSet(KEY_BLOCKS, set).apply()
    }

    fun blockedTodayPackages(): List<String> {
        val prefix = "${LocalDate.now()}|"
        return (prefs.getStringSet(KEY_BLOCKS, emptySet()) ?: emptySet())
            .filter { it.startsWith(prefix) }
            .map { it.removePrefix(prefix) }
    }

    /**
     * Desbloquea una app por hoy. Si lo haces tú ([manual]), el bloqueo automático ya no la
     * vuelve a cerrar hoy; si es por los minutos de regalo, la cerrará cuando se acaben.
     */
    fun unblockToday(pkg: String, manual: Boolean = true) {
        val today = LocalDate.now().toString()
        val blocks = HashSet((prefs.getStringSet(KEY_BLOCKS, emptySet()) ?: emptySet()).filter { it.startsWith(today) })
        blocks.remove("$today|$pkg")
        val edit = prefs.edit().putStringSet(KEY_BLOCKS, blocks)
        if (manual) {
            val unblocked = HashSet((prefs.getStringSet(KEY_UNBLOCKED, emptySet()) ?: emptySet()).filter { it.startsWith(today) })
            unblocked.add("$today|$pkg")
            edit.putStringSet(KEY_UNBLOCKED, unblocked)
        }
        edit.apply()
    }

    fun wasUnblockedToday(pkg: String): Boolean =
        prefs.getStringSet(KEY_UNBLOCKED, emptySet())?.contains("${LocalDate.now()}|$pkg") == true

    // ---------- Segunda oportunidad tras perder la racha del todo ----------

    fun recovery(): Recovery? {
        val raw = prefs.getString(KEY_RECOVERY, null) ?: return null
        return try {
            val o = JSONObject(raw)
            val arr = o.getJSONArray("pkgs")
            Recovery(
                LocalDate.parse(o.getString("day")),
                (0 until arr.length()).map { arr.getString(it) }.toSet(),
                o.getInt("streak"),
                o.optBoolean("forfeited", false),
            )
        } catch (_: Exception) {
            null
        }
    }

    fun setRecovery(r: Recovery) {
        val o = JSONObject()
            .put("day", r.day.toString())
            .put("pkgs", JSONArray(r.pkgs.toList()))
            .put("streak", r.streak)
            .put("forfeited", r.forfeited)
        prefs.edit().putString(KEY_RECOVERY, o.toString()).apply()
    }

    /** Has abierto una app cerrada: esta racha ya no se puede recuperar. */
    fun forfeitRecovery() {
        recovery()?.let { setRecovery(it.copy(forfeited = true)) }
    }

    /** Hoy es el día de prueba y esta app es una de las que no debes abrir. */
    fun isGuardedToday(pkg: String): Boolean {
        val r = recovery() ?: return false
        return !r.forfeited && r.day == LocalDate.now().minusDays(1) && pkg in r.pkgs
    }

    /** Días en los que te pasaste pero aceptaste el bloqueo: cuentan para la racha. */
    fun isSaved(day: LocalDate): Boolean =
        prefs.getStringSet(KEY_SAVED, emptySet())?.contains(day.toString()) == true

    fun markSaved(day: LocalDate) {
        val set = HashSet(prefs.getStringSet(KEY_SAVED, emptySet()) ?: emptySet())
        set.add(day.toString())
        prefs.edit().putStringSet(KEY_SAVED, set).apply()
    }

    fun unmarkSaved(day: LocalDate) {
        val set = HashSet(prefs.getStringSet(KEY_SAVED, emptySet()) ?: emptySet())
        if (set.remove(day.toString())) prefs.edit().putStringSet(KEY_SAVED, set).apply()
    }

    /** Bloqueo automático al llegar al límite. Apagarlo solo surte efecto al día siguiente. */
    fun autoBlockActive(): Boolean {
        if (!prefs.getBoolean(KEY_AUTO, false)) return false
        val offFrom = prefs.getString(KEY_AUTO_OFF, null)?.let { LocalDate.parse(it) } ?: return true
        if (!LocalDate.now().isBefore(offFrom)) {
            prefs.edit().putBoolean(KEY_AUTO, false).remove(KEY_AUTO_OFF).apply()
            return false
        }
        return true
    }

    fun autoBlockTurningOff(): Boolean = autoBlockActive() && prefs.getString(KEY_AUTO_OFF, null) != null

    fun setAutoBlock(on: Boolean) {
        if (on) {
            prefs.edit().putBoolean(KEY_AUTO, true).remove(KEY_AUTO_OFF).apply()
        } else if (autoBlockActive()) {
            prefs.edit().putString(KEY_AUTO_OFF, LocalDate.now().plusDays(1).toString()).apply()
        }
    }

    // ---------- Animaciones de racha (una vez al día por tipo) ----------

    fun animShown(kind: String, day: LocalDate): Boolean =
        prefs.getString("anim_$kind", null) == day.toString()

    fun markAnimShown(kind: String, day: LocalDate) {
        prefs.edit().putString("anim_$kind", day.toString()).apply()
    }

    companion object {
        /** Límite máximo que se puede poner a una app. */
        const val MAX_LIMIT_MINUTES = 60
        const val MIN_LIMIT_MINUTES = 5

        private const val KEY_BLOCKS = "blocks"
        private const val KEY_UNBLOCKED = "unblocked"
        private const val KEY_EXTRA = "extra_days"
        private const val KEY_EXTRA_WINDOWS = "extra_windows"
        const val EXTRA_MS = 5 * 60_000L
        private const val KEY_RECOVERY = "recovery"        private const val KEY_REMOVED = "removed_goals"
        private const val KEY_SAVED = "saved_days"
        private const val KEY_AUTO = "auto_block"
        private const val KEY_AUTO_OFF = "auto_block_off_from"
        private const val KEY_GOALS = "goals"
        private const val KEY_START = "start_date"
        private const val KEY_LAST_EVAL = "last_evaluated"
        private const val KEY_HISTORY = "history"
        private const val KEY_BEST = "best_streak"
        private const val KEY_ALERTS = "alerts"
    }
}
