package com.rachadetox.app

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
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

private val ES = Locale.forLanguageTag("es-ES")
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
    var showAllApps by rememberSaveable { mutableStateOf(false) }

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
            TextButton(onClick = onBack) { Text("← Volver") }
            Text("Tu perfil", style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)

            val r = report
            if (r == null) {
                Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            FreedomCard(r)

            // Lo más impactante, arriba y bien grande
            val tiles = insights(r)
            if (tiles.isNotEmpty()) {
                Text("Tu semana en números", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                InsightGrid(tiles)
            }
            EquivalentsCard(r)

            Text("Día a día", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)

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
                        Text("Todavía no hay datos para este periodo.", style = MaterialTheme.typography.bodyMedium)
                    }
                    apps.take(if (showAllApps) 30 else 5).forEach { app ->
                        AppRow(app, colors[app.pkg] ?: other)
                    }
                    if (apps.size > 5) {
                        TextButton(onClick = { showAllApps = !showAllApps }) {
                            Text(if (showAllApps) "Ver menos" else "Ver todas (${apps.size})")
                        }
                    }
                }
            }

            Text(
                "Los vídeos se estiman suponiendo 45 s por vídeo en apps como TikTok, Instagram o YouTube. " +
                    "Android solo guarda unos días de detalle, así que algunos datos antiguos pueden faltar.",
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
        better -> "Esta semana has sido libre durante ${formatDuration(diff)} más." to
            "que la semana anterior, en apps de scroll."
        worse -> "Esta semana has pasado ${formatDuration(-diff)} más deslizando." to
            "que la anterior. La próxima puede ser distinta."
        else -> "Esta semana: ${formatDuration(r.scrollWeekMs)} en apps de scroll." to
            if (prev == null) "La semana que viene podrás compararla con esta." else "Casi igual que la semana anterior."
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
                Text(title, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
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
        FilterChip(selected = !weekMode, onClick = { onChange(false) }, label = { Text("Hoy") })
        FilterChip(selected = weekMode, onClick = { onChange(true) }, label = { Text("7 días") })
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
                "Abierta ${app.opens} ${if (app.opens == 1) "vez" else "veces"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(formatDuration(app.millis), style = MaterialTheme.typography.labelLarge)
    }
}

data class Insight(val value: String, val label: String)

/** Los datos más llamativos del perfil, como tarjetas con un número grande. */
fun insights(r: ProfileReport): List<Insight> {
    val out = mutableListOf<Insight>()
    if (r.videosWeek > 0) {
        out += Insight("≈ ${num(r.videosWeek)}", "vídeos esta semana")
        val meters = r.videosWeek * Analytics.CM_PER_SWIPE / 100.0
        out += Insight("${num(meters.toLong())} m", "deslizados con el pulgar: ${distancePhrase(meters)}")
    }
    r.todayApps.maxByOrNull { it.opens }?.takeIf { it.opens >= 3 }?.let { app ->
        val since = r.firstSessionTodayStart
        val every = if (since != null) (System.currentTimeMillis() - since) / 60_000L / app.opens else 0L
        out += Insight(
            "${app.opens} veces",
            "has abierto ${app.label} hoy" + if (every > 0) ": una cada $every min" else "",
        )
    }
    r.lastScreenLastNight?.let {
        out += Insight(hhmm(it.end), "tu última pantalla anoche (${r.labels[it.pkg] ?: it.pkg})")
    }
    if (r.nightScrollWeekMs >= 5 * 60_000L) {
        out += Insight(formatDuration(r.nightScrollWeekMs), "de scroll entre las 00:00 y las 6:00. Tiempo robado al sueño")
    }
    r.longest?.let {
        out += Insight(formatDuration(it.length), "seguidos en ${r.labels[it.pkg] ?: it.pkg} el ${dayName(it.start)}: tu sesión más larga")
    }
    r.firstScreenToday?.let {
        out += Insight(hhmm(it.start), "lo primero que abriste hoy: ${r.labels[it.pkg] ?: it.pkg}")
    }
    if (r.scrollWeekMs > 0) {
        val daysPerYear = r.scrollWeekMs * 52 / 86_400_000.0
        if (daysPerYear >= 1) {
            out += Insight(
                "${String.format(ES, "%.0f", daysPerYear)} días",
                "enteros al año en apps de scroll, a este ritmo",
            )
        }
    }
    return out
}

private val TILE_COLORS = listOf(
    AlbaColors.Alba to AlbaColors.Noche,
    AlbaColors.Bruma to AlbaColors.Arena,
    AlbaColors.Sol to AlbaColors.Noche,
    AlbaColors.Salvia to AlbaColors.Noche,
)

@Composable
private fun InsightGrid(items: List<Insight>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEachIndexed { row, pair ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEachIndexed { col, item ->
                    val (bg, fg) = TILE_COLORS[(row * 2 + col + row) % TILE_COLORS.size]
                    InsightTile(item, bg, fg, Modifier.weight(1f).fillMaxHeight())
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InsightTile(item: Insight, bg: Color, fg: Color, modifier: Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bg, contentColor = fg),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.value, fontSize = 30.sp, lineHeight = 34.sp, fontFamily = FontFamily.Serif)
            Text(item.label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EquivalentsCard(r: ProfileReport) {
    val items = equivalentsFor(r.scrollWeekMs)
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AlbaColors.Noche, contentColor = AlbaColors.Arena),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (items.isEmpty()) {
                Text(
                    "Menos de una hora de scroll esta semana.",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                )
                Text("Eso es mirar arriba.", style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(
                    "Con las ${formatDuration(r.scrollWeekMs)} de esta semana podrías haber…",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Serif,
                )
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(10.dp)) { drawCircle(AlbaColors.Sol) }
                        Spacer(Modifier.width(10.dp))
                        Text(item, style = MaterialTheme.typography.titleMedium)
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
