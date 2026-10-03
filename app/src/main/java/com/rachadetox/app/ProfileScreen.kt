package com.rachadetox.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DayStyle
import java.util.Locale

private val ES: Locale get() = Lang.locale
private val APP_COLORS = listOf(AlbaColors.Alba, AlbaColors.Sol, AlbaColors.Salvia, AlbaColors.Bruma, Color(0xFFB7A6D6))

private fun hhmm(millis: Long): String =
    DateTimeFormatter.ofPattern("H:mm").format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

private fun dayName(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).dayOfWeek.getDisplayName(DayStyle.FULL, ES)

private fun num(n: Long): String = NumberFormat.getIntegerInstance(ES).format(n)

@Composable
fun ProfileScreen(goals: List<Goal>, onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var report by remember { mutableStateOf<ProfileReport?>(null) }
    var weekMode by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        report = withContext(Dispatchers.Default) { Analytics.build(context, goals) }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TextButton(onClick = onBack) { Text(tr("← Volver", "← Back", "← Indietro")) }
            Text(tr("Tu perfil", "Your profile", "Il tuo profilo"), style = MaterialTheme.typography.headlineMedium, fontFamily = AlbaType.heading)

            val r = report
            if (r == null) {
                Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            FreedomCard(r)

            // Gráfico día / semana
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModeChips(weekMode) { weekMode = it }
                    val apps = if (weekMode) r.weekApps else r.todayApps
                    val top = apps.take(APP_COLORS.size).map { it.pkg }
                    val colors = top.mapIndexed { i, p -> p to APP_COLORS[i] }.toMap()
                    val other = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    if (weekMode) {
                        StackedBars(
                            bars = r.weekDaily.map { it.second },
                            labels = r.weekDaily.map { it.first.dayOfWeek.getDisplayName(DayStyle.NARROW, ES).uppercase(ES) },
                            order = top, colors = colors, otherColor = other,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.6f),
                        )
                    } else {
                        StackedBars(
                            bars = r.todayHourly,
                            labels = (0 until 24).map { if (it % 6 == 0) "$it h" else "" },
                            order = top, colors = colors, otherColor = other,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.6f),
                        )
                    }
                    if (apps.isEmpty()) {
                        Text(tr("Todavía no hay datos para este periodo.", "No data for this period yet.", "Ancora nessun dato per questo periodo."), style = MaterialTheme.typography.bodyMedium)
                    }
                    apps.take(8).forEachIndexed { i, app ->
                        AppRow(app, colors[app.pkg] ?: other)
                    }
                }
            }

            ScrollCard(r, weekMode)
            HabitsCard(r)
            EquivalentsCard(r)

            Text(
                tr(
                    "Los vídeos se estiman suponiendo 45 s por vídeo en apps como TikTok, Instagram o YouTube. " +
                        "Android solo guarda unos días de detalle, así que algunos datos antiguos pueden faltar.",
                    "Videos are estimated assuming 45 s per video in apps like TikTok, Instagram or YouTube. " +
                        "Android only keeps a few days of detail, so some older data may be missing.",
                    "I video sono stimati considerando 45 s per video in app come TikTok, Instagram o YouTube. " +
                        "Android conserva solo pochi giorni di dettaglio, quindi alcuni dati più vecchi potrebbero mancare.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ------------------------------------------------------------------ tarjetas

@Composable
private fun FreedomCard(r: ProfileReport) {
    val prev = r.scrollPrevWeekMs
    val diff = if (prev != null) prev - r.scrollWeekMs else 0L
    val better = prev != null && diff >= 15 * 60_000L
    val worse = prev != null && -diff >= 15 * 60_000L

    val (title, sub) = when {
        better -> tr(
            "Esta semana has sido libre durante ${formatDuration(diff)} más.",
            "This week you've been free for ${formatDuration(diff)} more.",
            "Questa settimana sei stato libero per ${formatDuration(diff)} in più.",
        ) to tr(
            "que la semana anterior, en apps de scroll.",
            "than last week, in scrolling apps.",
            "rispetto alla settimana scorsa, nelle app di scroll.",
        )
        worse -> tr(
            "Esta semana has pasado ${formatDuration(-diff)} más deslizando.",
            "This week you spent ${formatDuration(-diff)} more scrolling.",
            "Questa settimana hai passato ${formatDuration(-diff)} in più a scorrere.",
        ) to tr(
            "que la anterior. La próxima puede ser distinta.",
            "than the week before. Next week can be different.",
            "rispetto alla precedente. La prossima può essere diversa.",
        )
        else -> tr(
            "Esta semana: ${formatDuration(r.scrollWeekMs)} en apps de scroll.",
            "This week: ${formatDuration(r.scrollWeekMs)} in scrolling apps.",
            "Questa settimana: ${formatDuration(r.scrollWeekMs)} nelle app di scroll.",
        ) to if (prev == null) {
            tr(
                "La semana que viene podrás compararla con esta.",
                "Next week you'll be able to compare it with this one.",
                "La prossima settimana potrai confrontarla con questa.",
            )
        } else {
            tr("Casi igual que la semana anterior.", "About the same as last week.", "Quasi uguale alla settimana scorsa.")
        }
    }

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (better) AlbaColors.Sol else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (better) AlbaColors.Noche else MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            BrainArt(
                Modifier.size(72.dp),
                state = if (better) BrainState.Sunrise else if (worse) BrainState.Overloaded else BrainState.Calm,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontFamily = AlbaType.heading)
                Spacer(Modifier.height(4.dp))
                Text(sub, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeChips(weekMode: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !weekMode, onClick = { onChange(false) }, label = { Text(tr("Hoy", "Today", "Oggi")) })
        FilterChip(selected = weekMode, onClick = { onChange(true) }, label = { Text(tr("7 días", "7 days", "7 giorni")) })
    }
}

@Composable
private fun AppRow(app: AppUsage, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
        ) { Canvas(Modifier.fillMaxSize()) { drawRect(color) } }
        Spacer(Modifier.width(10.dp))
        AppIcon(app.pkg, 28.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                tr("Abierta ", "Opened ", "Aperta ") + "${app.opens} " + if (app.opens == 1) tr("vez", "time", "volta") else tr("veces", "times", "volte"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(formatDuration(app.millis), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ScrollCard(r: ProfileReport, weekMode: Boolean) {
    val videos = if (weekMode) r.videosWeek else r.videosToday
    if (videos <= 0) return
    val meters = videos * Analytics.CM_PER_SWIPE / 100.0
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (weekMode) tr("Estos 7 días", "These 7 days", "Questi 7 giorni") else tr("Hoy", "Today", "Oggi"), style = MaterialTheme.typography.labelLarge, color = AlbaColors.Alba)
            Text("≈ ${num(videos)} " + tr("vídeos", "videos", "video"), fontSize = 40.sp, fontFamily = AlbaType.heading)
            Text(
                tr(
                    "Has deslizado unos ${num(meters.toLong())} metros con el pulgar: ${distancePhrase(meters)}.",
                    "You've swiped about ${num(meters.toLong())} meters with your thumb: ${distancePhrase(meters)}.",
                    "Hai scorso circa ${num(meters.toLong())} metri con il pollice: ${distancePhrase(meters)}.",
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun HabitsCard(r: ProfileReport) {
    val lines = mutableListOf<String>()

    r.todayApps.maxByOrNull { it.opens }?.takeIf { it.opens >= 3 }?.let { app ->
        val since = r.firstSessionTodayStart
        val every = if (since != null) (System.currentTimeMillis() - since) / 60_000L / app.opens else 0L
        lines += tr(
            "Hoy has abierto ${app.label} ${app.opens} veces",
            "Today you've opened ${app.label} ${app.opens} times",
            "Oggi hai aperto ${app.label} ${app.opens} volte",
        ) + if (every > 0) tr(": una cada $every minutos.", ": once every $every minutes.", ": una ogni $every minuti.") else "."
    }
    r.lastScreenLastNight?.let {
        val app = r.labels[it.pkg] ?: it.pkg
        lines += tr(
            "Anoche tu última pantalla fue a las ${hhmm(it.end)} ($app).",
            "Last night your last screen was at ${hhmm(it.end)} ($app).",
            "Ieri sera il tuo ultimo schermo è stato alle ${hhmm(it.end)} ($app).",
        )
    }
    r.firstScreenToday?.let {
        val app = r.labels[it.pkg] ?: it.pkg
        lines += tr(
            "Hoy lo primero que abriste fue $app, a las ${hhmm(it.start)}.",
            "Today the first thing you opened was $app, at ${hhmm(it.start)}.",
            "Oggi la prima cosa che hai aperto è stata $app, alle ${hhmm(it.start)}.",
        )
    }
    if (r.nightScrollWeekMs >= 5 * 60_000L) {
        val t = formatDuration(r.nightScrollWeekMs)
        lines += tr(
            "Esta semana, $t de scroll entre las 00:00 y las 6:00. Tiempo robado al sueño.",
            "This week, $t of scrolling between midnight and 6:00. Time stolen from sleep.",
            "Questa settimana, $t di scroll tra mezzanotte e le 6:00. Tempo rubato al sonno.",
        )
    }
    r.longest?.let {
        val app = r.labels[it.pkg] ?: it.pkg
        val t = formatDuration(it.length)
        lines += tr(
            "Tu sesión más larga: $t seguidos en $app, el ${dayName(it.start)}.",
            "Your longest session: $t straight on $app, on ${dayName(it.start)}.",
            "La tua sessione più lunga: $t di fila su $app, ${dayName(it.start)}.",
        )
    }
    if (r.scrollWeekMs > 0) {
        val daysPerYear = r.scrollWeekMs * 52 / 86_400_000.0
        if (daysPerYear >= 1) {
            val d = String.format(ES, "%.0f", daysPerYear)
            lines += tr(
                "A este ritmo, este año pasarás $d días enteros, día y noche, en apps de scroll.",
                "At this pace, this year you'll spend $d whole days, day and night, in scrolling apps.",
                "A questo ritmo, quest'anno passerai $d giorni interi, giorno e notte, nelle app di scroll.",
            )
        }
    }
    if (lines.isEmpty()) return

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tr("Tus costumbres", "Your habits", "Le tue abitudini"), style = MaterialTheme.typography.titleLarge, fontFamily = AlbaType.heading)
            lines.forEach { line ->
                Row {
                    Text("·  ", color = AlbaColors.Alba, style = MaterialTheme.typography.bodyLarge)
                    Text(line, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun EquivalentsCard(r: ProfileReport) {
    val items = equivalentsFor(r.scrollWeekMs)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (items.isEmpty()) {
                Text(
                    tr("Menos de una hora de scroll esta semana.", "Less than an hour of scrolling this week.", "Meno di un'ora di scroll questa settimana."),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = AlbaType.heading,
                )
                Text(tr("Eso es mirar arriba.", "That's looking up.", "Questo è guardare in alto."), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(
                    tr(
                        "En las ${formatDuration(r.scrollWeekMs)} de esta semana te daba tiempo a…",
                        "The ${formatDuration(r.scrollWeekMs)} of this week were enough to…",
                        "Con le ${formatDuration(r.scrollWeekMs)} di questa settimana avresti potuto…",
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = AlbaType.heading,
                )
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(10.dp)) { drawCircle(AlbaColors.Sol) }
                        Spacer(Modifier.width(10.dp))
                        Text(item, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ gráfico

@Composable
private fun StackedBars(
    bars: List<Map<String, Long>>,
    labels: List<String>,
    order: List<String>,
    colors: Map<String, Color>,
    otherColor: Color,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bottom = h - 26.dp.toPx()
        val top = 18.dp.toPx()
        val maxMs = (bars.maxOfOrNull { it.values.sum() } ?: 0L).coerceAtLeast(60_000L)
        val slot = w / bars.size
        val barW = slot * if (bars.size > 12) 0.7f else 0.55f

        // Línea del máximo
        drawLine(gridColor, Offset(0f, top), Offset(w, top), strokeWidth = 2f)
        drawText(measurer, formatDuration(maxMs), Offset(0f, 0f), TextStyle(fontSize = 11.sp, color = labelColor))
        drawLine(gridColor, Offset(0f, bottom), Offset(w, bottom), strokeWidth = 2f)

        bars.forEachIndexed { i, m ->
            val x = slot * i + (slot - barW) / 2
            var y = bottom
            val segments = order.map { it to (m[it] ?: 0L) } +
                ("__other" to (m.values.sum() - order.sumOf { m[it] ?: 0L }))
            for ((pkg, ms) in segments) {
                if (ms <= 0) continue
                val segH = (bottom - top) * ms / maxMs
                val color = if (pkg == "__other") otherColor else colors[pkg] ?: otherColor
                drawRoundRect(color, Offset(x, y - segH), Size(barW, segH), CornerRadius(4f, 4f))
                y -= segH
            }
            val label = labels[i]
            if (label.isNotEmpty()) {
                val layout = measurer.measure(label, TextStyle(fontSize = 11.sp, color = labelColor))
                val lx = (x + barW / 2 - layout.size.width / 2).coerceIn(0f, w - layout.size.width)
                drawText(layout, topLeft = Offset(lx, bottom + 6.dp.toPx()))
            }
        }
    }
}
