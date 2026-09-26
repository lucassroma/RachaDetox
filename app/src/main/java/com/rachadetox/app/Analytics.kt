package com.rachadetox.app

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import java.time.LocalDate
import java.time.ZoneId

/** Un rato seguido con una app delante. */
data class Session(val pkg: String, val start: Long, val end: Long) {
    val length: Long get() = end - start
}

data class AppUsage(val pkg: String, val label: String, val millis: Long, val opens: Int)

data class ProfileReport(
    val labels: Map<String, String>,
    /** Hoy, 24 horas: paquete -> ms. */
    val todayHourly: List<Map<String, Long>>,
    /** Últimos 7 días (del más antiguo a hoy): paquete -> ms. */
    val weekDaily: List<Pair<LocalDate, Map<String, Long>>>,
    val weekApps: List<AppUsage>,
    val todayApps: List<AppUsage>,
    val scrollWeekMs: Long,
    val scrollPrevWeekMs: Long?,
    val scrollTodayMs: Long,
    val videosWeek: Long,
    val videosToday: Long,
    val nightScrollWeekMs: Long,
    val longest: Session?,
    val lastScreenLastNight: Session?,
    val firstScreenToday: Session?,
    val firstSessionTodayStart: Long?,
)

/**
 * Lee el mismo registro de uso que Bienestar digital y lo resume.
 * Todo se calcula en el móvil.
 */
object Analytics {

    /** Apps de vídeos cortos / feed infinito: para ellas estimamos vídeos vistos. */
    val SCROLL_APPS = setOf(
        "com.zhiliaoapp.musically", "com.ss.android.ugc.trill", // TikTok
        "com.instagram.android",
        "com.google.android.youtube",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.pinterest",
        "tv.twitch.android.app",
    )

    const val SECONDS_PER_VIDEO = 45L
    const val CM_PER_SWIPE = 12.0

    private const val HOUR = 3_600_000L
    private const val DAY = 24 * HOUR

    private const val ACTIVITY_RESUMED = 1
    private const val ACTIVITY_PAUSED = 2
    private const val SCREEN_NON_INTERACTIVE = 16
    private const val ACTIVITY_STOPPED = 23
    private const val DEVICE_SHUTDOWN = 26

    /** Todas las sesiones entre [start] y [end], juntando cortes de menos de 10 s. */
    fun sessions(context: Context, start: Long, end: Long): List<Session> {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val events = usm.queryEvents(start - 3 * HOUR, end) ?: return emptyList()
        val openSince = HashMap<String, Long>()
        val open = HashMap<String, MutableSet<String>>()
        val raw = ArrayList<Session>()

        fun add(pkg: String, from: Long, to: Long) {
            val a = maxOf(from, start)
            val b = minOf(to, end)
            if (b > a) raw.add(Session(pkg, a, b))
        }

        fun closeAll(at: Long) {
            for ((pkg, set) in open) {
                if (set.isNotEmpty()) {
                    add(pkg, openSince[pkg] ?: at, at)
                    set.clear()
                }
            }
        }

        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val pkg = e.packageName ?: continue
            val t = e.timeStamp
            when (e.eventType) {
                ACTIVITY_RESUMED -> {
                    val set = open.getOrPut(pkg) { mutableSetOf() }
                    if (set.isEmpty()) openSince[pkg] = t
                    set.add(e.className ?: "")
                }

                ACTIVITY_PAUSED, ACTIVITY_STOPPED -> {
                    val set = open[pkg] ?: continue
                    if (set.remove(e.className ?: "") && set.isEmpty()) add(pkg, openSince[pkg] ?: t, t)
                }

                SCREEN_NON_INTERACTIVE, DEVICE_SHUTDOWN -> closeAll(t)
            }
        }
        closeAll(end)

        // Juntar trozos de la misma app separados por menos de 10 s
        val merged = ArrayList<Session>()
        raw.groupBy { it.pkg }.forEach { (_, list) ->
            var cur: Session? = null
            for (s in list.sortedBy { it.start }) {
                val c = cur
                cur = if (c != null && s.start - c.end < 10_000L) c.copy(end = maxOf(c.end, s.end)) else {
                    if (c != null) merged.add(c)
                    s
                }
            }
            cur?.let { merged.add(it) }
        }
        return merged.sortedBy { it.start }
    }

    private fun overlap(s: Session, a: Long, b: Long): Long = maxOf(0L, minOf(s.end, b) - maxOf(s.start, a))

    /** Apps que tiene sentido contar: las que salen en el cajón de apps, sin el launcher ni Alba. */
    private fun countablePackages(context: Context): Set<String> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        fun query(intent: Intent): List<String> {
            val list = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
            return list.map { it.activityInfo.packageName }
        }
        val apps = query(launcher).toMutableSet()
        val homes = query(home)
        apps.removeAll(homes.toSet())
        apps.remove(context.packageName)
        return apps
    }

    fun build(context: Context, goals: List<Goal>): ProfileReport {
        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val today = LocalDate.now()
        fun startOf(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()

        val todayStart = startOf(today)
        val weekStart = startOf(today.minusDays(6))
        val prevStart = startOf(today.minusDays(13))

        val countable = countablePackages(context)
        val all = sessions(context, prevStart, now).filter { it.pkg in countable }
        val scrollSet = SCROLL_APPS + goals.map { it.pkg }

        val pm = context.packageManager
        val labels = HashMap<String, String>()
        all.map { it.pkg }.toSet().forEach { pkg ->
            labels[pkg] = try {
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } catch (_: Exception) {
                pkg
            }
        }

        fun totals(a: Long, b: Long): Map<String, Long> {
            val m = HashMap<String, Long>()
            for (s in all) {
                val o = overlap(s, a, b)
                if (o > 0) m[s.pkg] = (m[s.pkg] ?: 0L) + o
            }
            return m
        }

        fun apps(a: Long, b: Long): List<AppUsage> {
            val t = totals(a, b)
            val opens = all.filter { it.start in a until b }.groupingBy { it.pkg }.eachCount()
            return t.entries
                .filter { it.value >= 60_000L }
                .map { AppUsage(it.key, labels[it.key] ?: it.key, it.value, opens[it.key] ?: 0) }
                .sortedByDescending { it.millis }
        }

        val weekDaily = (6 downTo 0).map { back ->
            val d = today.minusDays(back.toLong())
            d to totals(startOf(d), startOf(d.plusDays(1)))
        }

        val todayHourly = (0 until 24).map { h ->
            val a = today.atTime(h, 0).atZone(zone).toInstant().toEpochMilli()
            totals(a, a + HOUR)
        }

        fun scrollMs(a: Long, b: Long) = all.filter { it.pkg in scrollSet }.sumOf { overlap(it, a, b) }
        fun videoMs(a: Long, b: Long) = all.filter { it.pkg in SCROLL_APPS }.sumOf { overlap(it, a, b) }

        // ¿Hay datos de la semana anterior? (Android guarda solo unos días de detalle)
        val prevCovered = all.any { it.start < weekStart - 4 * DAY }

        var night = 0L
        for (back in 0..6) {
            val d = today.minusDays(back.toLong())
            val a = startOf(d)
            night += scrollMs(a, a + 6 * HOUR)
        }

        // Última pantalla de anoche y primera de hoy: el primer hueco de 3 h a partir de las 20:00 de ayer
        val nightFrom = startOf(today.minusDays(1)) + 20 * HOUR
        val around = all.filter { it.end > nightFrom }.sortedBy { it.start }
        var last: Session? = null
        var first: Session? = null
        var prevEnd = nightFrom
        var prevSession: Session? = null
        for (s in around) {
            if (s.start - prevEnd >= 3 * HOUR && prevSession != null) {
                last = prevSession
                first = s
                break
            }
            if (s.end > prevEnd) {
                prevEnd = s.end
                prevSession = s
            }
        }
        if (first != null && first.start < todayStart) first = null

        val weekScroll = all.filter { it.pkg in scrollSet && it.end > weekStart }
        val longest = weekScroll
            .map { it.copy(start = maxOf(it.start, weekStart)) }
            .maxByOrNull { it.length }
            ?.takeIf { it.length >= 10 * 60_000L }

        return ProfileReport(
            labels = labels,
            todayHourly = todayHourly,
            weekDaily = weekDaily,
            weekApps = apps(weekStart, now),
            todayApps = apps(todayStart, now),
            scrollWeekMs = scrollMs(weekStart, now),
            scrollPrevWeekMs = if (prevCovered) scrollMs(prevStart, weekStart) else null,
            scrollTodayMs = scrollMs(todayStart, now),
            videosWeek = videoMs(weekStart, now) / (SECONDS_PER_VIDEO * 1000),
            videosToday = videoMs(todayStart, now) / (SECONDS_PER_VIDEO * 1000),
            nightScrollWeekMs = night,
            longest = longest,
            lastScreenLastNight = last,
            firstScreenToday = first,
            firstSessionTodayStart = all.filter { it.start >= todayStart }.minOfOrNull { it.start },
        )
    }
}

// ------------------------------------------------------------------ frases

/** Qué podrías haber hecho con ese tiempo. De menor a mayor. */
private val EQUIVALENTS = listOf(
    1.0 to "caminar 5 km",
    1.0 to "llamar a tu abuela sin prisa",
    1.5 to "cocinar algo de verdad",
    3.0 to "ver una película y media",
    3.0 to "leer 100 páginas",
    7.0 to "leer una novela entera",
    8.0 to "dormir una noche completa",
    11.0 to "ver El Señor de los Anillos entero, en versión extendida",
    14.0 to "caminar de Madrid a Toledo",
    14.0 to "aprender a correr 5 km seguidos",
    20.0 to "tocar tus primeras canciones al piano",
    40.0 to "escuchar el Quijote entero",
    100.0 to "llegar a un nivel básico de un idioma nuevo",
)

fun equivalentsFor(millis: Long): List<String> {
    val hours = millis / 3_600_000.0
    return EQUIVALENTS.filter { it.first <= hours }.takeLast(3).reversed().map { it.second }
}

private val LANDMARKS = listOf(
    "la Torre de Pisa" to 56.0,
    "la Giralda" to 104.0,
    "la Sagrada Família" to 172.0,
    "la Torre Eiffel" to 330.0,
    "el Burj Khalifa" to 828.0,
    "el Teide" to 3715.0,
    "el Everest" to 8849.0,
)

fun distancePhrase(meters: Double): String {
    if (meters < 56) return "como un edificio de ${maxOf(1, (meters / 3).toInt())} pisos"
    val (name, h) = LANDMARKS.last { it.second <= meters }
    val ratio = meters / h
    return if (ratio < 1.5) "más que ${name} (${h.toInt()} m)"
    else "${String.format(java.util.Locale.forLanguageTag("es-ES"), "%.1f", ratio)} veces ${name}"
}
