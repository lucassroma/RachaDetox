package com.rachadetox.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

// ------------------------------------------------------------------ pantalla

@Composable
fun WhyScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TextButton(onClick = onBack) { Text("← Volver") }

            // 1. Portada
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BrainArt(Modifier.size(170.dp), state = BrainState.Overloaded)
                Spacer(Modifier.height(8.dp))
                Text(
                    "¿Por qué me aburro?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Serif,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Lo que pasa en tu cabeza cada vez que deslizas el dedo.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // 2. Tragaperras
            InfoCard(
                step = "1",
                title = "Llevas una tragaperras en el bolsillo",
                body = "Cada vez que deslizas, tu cerebro no sabe qué va a salir: algo increíble o nada. " +
                    "Esa incertidumbre es el mismo mecanismo que usan las máquinas tragaperras: " +
                    "la recompensa variable. Es lo que más engancha, y no es casualidad. Está diseñado así.",
            ) { SlotMachineArt(Modifier.fillMaxWidth().aspectRatio(1.6f)) }

            // 3. Picos de dopamina
            InfoCard(
                step = "2",
                title = "Pico, caída, otra vez",
                body = "La dopamina no es placer: es las ganas de más. Cada vídeo es un pequeño pico, " +
                    "y después de cada pico hay una caída que te deja por debajo de donde estabas. " +
                    "Por eso nunca es solo un vídeo.",
            ) {
                DopamineChart(Modifier.fillMaxWidth().aspectRatio(1.7f))
                Legend(
                    listOf(AlbaColors.Alba to "Scroll", AlbaColors.Salvia to "Un paseo, un libro, una charla")
                )
            }

            // 4. El listón
            InfoCard(
                step = "3",
                title = "Te han subido el listón",
                body = "Tu cerebro se adapta a lo que le das. Después de cientos de estímulos al día, " +
                    "lo normal ya no llega al listón: un libro, una conversación, un paseo. " +
                    "No es que el mundo sea aburrido. Es que te han subido el listón.",
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BrainArt(Modifier.size(96.dp), state = BrainState.Calm)
                        Text("Descansado", style = MaterialTheme.typography.labelMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BrainArt(Modifier.size(96.dp), state = BrainState.Overloaded)
                        Text("Saturado", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                ThresholdChart(Modifier.fillMaxWidth().aspectRatio(1.5f))
            }

            // 5. El dato
            InfoCard(
                step = "4",
                title = "Las horas pesan",
                body = "En un estudio con más de 6.500 adolescentes de EE. UU., quienes pasaban muchas horas " +
                    "al día en redes tenían más riesgo de problemas como ansiedad o depresión que quienes no las usaban. " +
                    "Y el riesgo subía con las horas.",
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BigStat("+60 %", "de 3 a 6 h\nal día")
                    BigStat("+78 %", "más de 6 h\nal día")
                }
            }

            // 6. Recuperación
            InfoCard(
                step = "5",
                title = "El listón baja",
                body = "Esto no es para siempre. En un experimento, universitarios que limitaron las redes a " +
                    "unos 30 minutos al día se sintieron menos solos y menos deprimidos en solo tres semanas. " +
                    "El cerebro vuelve a disfrutar de lo sencillo si le das tiempo.",
            ) {
                RecoveryChart(Modifier.fillMaxWidth().aspectRatio(1.7f))
            }

            // 7. Cierre
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AlbaColors.Sol, contentColor = AlbaColors.Noche),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BrainArt(Modifier.size(110.dp), state = BrainState.Sunrise)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Cada día de tu racha es un día bajando el listón.",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Serif,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = AlbaColors.Noche, contentColor = AlbaColors.Arena),
                    ) { Text("Mira arriba") }
                }
            }

            // Fuentes
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Fuentes", style = MaterialTheme.typography.labelLarge)
                SourceLink(
                    "Riehm et al. (2019). JAMA Psychiatry. Tiempo en redes y problemas de salud mental en adolescentes.",
                    "https://jamanetwork.com/journals/jamapsychiatry/fullarticle/2749480",
                ) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                SourceLink(
                    "Hunt et al. (2018). No More FOMO: limitar las redes reduce la soledad y la depresión. J. Social and Clinical Psychology.",
                    "https://guilfordjournals.com/doi/10.1521/jscp.2018.37.10.751",
                ) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                Text(
                    "Los estudios muestran asociación y efectos medios; cada persona es distinta. " +
                        "Si te sientes mal a menudo, hablar con un profesional ayuda.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ------------------------------------------------------------------ piezas

@Composable
private fun InfoCard(step: String, title: String, body: String, art: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                step,
                color = AlbaColors.Alba,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(title, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
            art()
            Text(body, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)
        }
    }
}

@Composable
private fun BigStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 44.sp, fontFamily = FontFamily.Serif, color = AlbaColors.Alba)
        Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Legend(items: List<Pair<Color, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (color, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(width = 18.dp, height = 4.dp)) {
                    drawRoundRect(color, cornerRadius = CornerRadius(4f, 4f))
                }
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SourceLink(text: String, url: String, open: (String) -> Unit) {
    TextButton(onClick = { open(url) }, modifier = Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth())
    }
}

// ------------------------------------------------------------------ ilustraciones

enum class BrainState { Calm, Overloaded, Sunrise }

/** Cerebro visto desde arriba: dos hemisferios con circunvoluciones. */
@Composable
fun BrainArt(modifier: Modifier, state: BrainState) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val fill = when (state) {
            BrainState.Calm -> AlbaColors.Salvia
            BrainState.Overloaded -> AlbaColors.Alba
            BrainState.Sunrise -> AlbaColors.Arena
        }
        val line = AlbaColors.Noche.copy(alpha = 0.55f)
        val stroke = w * 0.022f

        // Sol que sale detrás (cierre)
        if (state == BrainState.Sunrise) {
            drawCircle(AlbaColors.Alba, radius = w * 0.46f, center = Offset(w / 2, h * 0.55f))
        }

        // Hemisferios
        val lobeW = w * 0.36f
        val lobeH = h * 0.62f
        val top = h * 0.2f
        drawOval(fill, Offset(w * 0.5f - lobeW - w * 0.01f, top), Size(lobeW, lobeH))
        drawOval(fill, Offset(w * 0.5f + w * 0.01f, top), Size(lobeW, lobeH))
        drawOval(line, Offset(w * 0.5f - lobeW - w * 0.01f, top), Size(lobeW, lobeH), style = Stroke(stroke))
        drawOval(line, Offset(w * 0.5f + w * 0.01f, top), Size(lobeW, lobeH), style = Stroke(stroke))

        // Circunvoluciones
        fun fold(x0: Float, y: Float, width: Float, amp: Float) {
            val p = Path()
            val steps = 4
            p.moveTo(x0, y)
            for (i in 1..steps) {
                val x = x0 + width * i / steps
                val cx = x0 + width * (i - 0.5f) / steps
                val cy = y + if (i % 2 == 0) amp else -amp
                p.quadraticBezierTo(cx, cy, x, y)
            }
            drawPath(p, line, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        for (side in 0..1) {
            val x0 = if (side == 0) w * 0.5f - lobeW + w * 0.03f else w * 0.5f + w * 0.05f
            val fw = lobeW - w * 0.08f
            fold(x0, top + lobeH * 0.28f, fw, h * 0.035f)
            fold(x0 + w * 0.01f, top + lobeH * 0.5f, fw - w * 0.02f, h * 0.04f)
            fold(x0, top + lobeH * 0.72f, fw, h * 0.035f)
        }

        when (state) {
            BrainState.Overloaded -> {
                // Rayos alrededor: sobreestimulación
                val bolt = AlbaColors.Sol
                listOf(
                    Offset(w * 0.08f, h * 0.18f), Offset(w * 0.86f, h * 0.12f),
                    Offset(w * 0.04f, h * 0.62f), Offset(w * 0.9f, h * 0.66f),
                ).forEach { o -> drawBolt(o, w * 0.1f, bolt, stroke * 1.4f) }
                // Notificaciones
                drawCircle(AlbaColors.Bruma, w * 0.05f, Offset(w * 0.78f, h * 0.9f))
                drawCircle(AlbaColors.Bruma, w * 0.035f, Offset(w * 0.2f, h * 0.92f))
            }

            BrainState.Calm -> {
                drawCircle(AlbaColors.Sol, w * 0.06f, Offset(w * 0.85f, h * 0.14f))
            }

            BrainState.Sunrise -> Unit
        }
    }
}

private fun DrawScope.drawBolt(origin: Offset, s: Float, color: Color, stroke: Float) {
    val p = Path().apply {
        moveTo(origin.x + s * 0.6f, origin.y)
        lineTo(origin.x + s * 0.2f, origin.y + s * 0.55f)
        lineTo(origin.x + s * 0.6f, origin.y + s * 0.55f)
        lineTo(origin.x + s * 0.2f, origin.y + s * 1.1f)
    }
    drawPath(p, color, style = Stroke(stroke, cap = StrokeCap.Round))
}

/** Un móvil convertido en tragaperras. */
@Composable
private fun SlotMachineArt(modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val phoneW = w * 0.62f
        val phoneH = h * 0.92f
        val left = (w - phoneW) / 2 - w * 0.04f
        val top = h * 0.04f
        // Cuerpo
        drawRoundRect(AlbaColors.Noche, Offset(left, top), Size(phoneW, phoneH), CornerRadius(w * 0.06f))
        drawRoundRect(AlbaColors.Bruma, Offset(left + w * 0.03f, top + h * 0.1f), Size(phoneW - w * 0.06f, phoneH - h * 0.2f), CornerRadius(w * 0.03f))
        // Rodillos
        val reelW = (phoneW - w * 0.12f) / 3
        val symbols = listOf("♥", "?", "★")
        for (i in 0..2) {
            val rx = left + w * 0.045f + i * (reelW + w * 0.015f)
            val ry = top + h * 0.3f
            drawRoundRect(AlbaColors.Arena, Offset(rx, ry), Size(reelW, h * 0.34f), CornerRadius(w * 0.02f))
            val layout = measurer.measure(
                symbols[i],
                TextStyle(fontSize = 30.sp, color = if (i == 1) AlbaColors.Alba else AlbaColors.Noche),
            )
            drawText(
                layout,
                topLeft = Offset(rx + (reelW - layout.size.width) / 2, ry + (h * 0.34f - layout.size.height) / 2),
            )
        }
        // Palanca
        val lx = left + phoneW + w * 0.04f
        drawLine(AlbaColors.Bruma, Offset(left + phoneW, top + h * 0.5f), Offset(lx, top + h * 0.5f), strokeWidth = w * 0.02f)
        drawLine(AlbaColors.Bruma, Offset(lx, top + h * 0.5f), Offset(lx, top + h * 0.2f), strokeWidth = w * 0.02f, cap = StrokeCap.Round)
        drawCircle(AlbaColors.Alba, w * 0.04f, Offset(lx, top + h * 0.18f))
        // Dedo deslizando (flecha)
        val ay = top + h * 0.78f
        drawLine(AlbaColors.Sol, Offset(left + phoneW * 0.3f, ay), Offset(left + phoneW * 0.7f, ay), strokeWidth = w * 0.015f, cap = StrokeCap.Round)
        drawLine(AlbaColors.Sol, Offset(left + phoneW * 0.3f, ay), Offset(left + phoneW * 0.38f, ay - h * 0.05f), strokeWidth = w * 0.015f, cap = StrokeCap.Round)
    }
}

/** Picos de dopamina con scroll frente a una actividad tranquila. */
@Composable
private fun DopamineChart(modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val base = h * 0.6f
        val pad = w * 0.04f

        // Línea base
        drawLine(
            axisColor.copy(alpha = 0.6f), Offset(pad, base), Offset(w - pad, base),
            strokeWidth = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
        )
        drawText(measurer, "tu nivel normal", Offset(pad, base + 6f), TextStyle(fontSize = 11.sp, color = axisColor))

        // Actividad tranquila: ola suave por encima de la base
        val calm = Path()
        val n = 120
        for (i in 0..n) {
            val t = i / n.toFloat()
            val x = pad + t * (w - 2 * pad)
            val y = base - h * 0.14f * sin(PI.toFloat() * t) - h * 0.03f * sin(6f * PI.toFloat() * t)
            if (i == 0) calm.moveTo(x, y) else calm.lineTo(x, y)
        }
        drawPath(calm, AlbaColors.Salvia, style = Stroke(6f, cap = StrokeCap.Round))

        // Scroll: picos y caídas cada vez más hondas
        val spikes = Path()
        val peaks = 7
        for (i in 0..n) {
            val t = i / n.toFloat()
            val x = pad + t * (w - 2 * pad)
            val phase = (t * peaks) % 1f
            val depth = 0.05f + 0.12f * t
            val y = if (phase < 0.15f) {
                base - h * 0.45f * (phase / 0.15f)
            } else {
                val k = (phase - 0.15f) / 0.85f
                base - h * 0.45f * exp(-6f * k) + h * depth * sin(PI.toFloat() * k)
            }
            if (i == 0) spikes.moveTo(x, base) else spikes.lineTo(x, y)
        }
        drawPath(spikes, AlbaColors.Alba, style = Stroke(6f, cap = StrokeCap.Round))
    }
}

/** El listón: actividades normales frente a un umbral que ha subido. */
@Composable
private fun ThresholdChart(modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bottom = h * 0.82f
        val chartH = h * 0.72f
        val items = listOf("Paseo" to 0.42f, "Libro" to 0.38f, "Charla" to 0.46f, "Scroll" to 0.95f)
        val oldBar = 0.3f
        val newBar = 0.7f
        val slot = w / items.size
        val barW = slot * 0.5f

        items.forEachIndexed { i, (label, v) ->
            val x = slot * i + (slot - barW) / 2
            val barH = chartH * v
            val color = when {
                label == "Scroll" -> AlbaColors.Alba
                v < newBar -> AlbaColors.Bruma.copy(alpha = 0.45f)
                else -> AlbaColors.Salvia
            }
            drawRoundRect(color, Offset(x, bottom - barH), Size(barW, barH), CornerRadius(10f))
            val layout = measurer.measure(label, TextStyle(fontSize = 12.sp, color = labelColor))
            drawText(layout, topLeft = Offset(x + (barW - layout.size.width) / 2, bottom + 6f))
        }

        // Listón de antes
        val oldY = bottom - chartH * oldBar
        drawLine(
            AlbaColors.Salvia, Offset(0f, oldY), Offset(w, oldY), strokeWidth = 5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
        )
        drawText(measurer, "listón de antes", Offset(4f, oldY + 4f), TextStyle(fontSize = 11.sp, color = muted))

        // Listón de ahora
        val newY = bottom - chartH * newBar
        drawLine(AlbaColors.Alba, Offset(0f, newY), Offset(w, newY), strokeWidth = 6f)
        drawText(measurer, "listón de ahora", Offset(4f, newY - 34f), TextStyle(fontSize = 11.sp, color = AlbaColors.Alba))
    }
}

/** El listón baja en unas semanas. */
@Composable
private fun RecoveryChart(modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val left = w * 0.05f
        val right = w * 0.95f
        val top = h * 0.12f
        val bottom = h * 0.78f

        val p = Path()
        val n = 80
        for (i in 0..n) {
            val t = i / n.toFloat()
            val x = left + t * (right - left)
            val y = top + (bottom - top) * (1f - exp(-3.2f * t)) / (1f - exp(-3.2f))
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        drawPath(p, AlbaColors.Salvia, style = Stroke(8f, cap = StrokeCap.Round))
        drawCircle(AlbaColors.Alba, 12f, Offset(left, top))
        drawCircle(AlbaColors.Sol, 16f, Offset(right, bottom))

        listOf("Hoy", "Semana 1", "Semana 2", "Semana 3").forEachIndexed { i, label ->
            val x = left + i * (right - left) / 3
            val layout = measurer.measure(label, TextStyle(fontSize = 11.sp, color = muted))
            val lx = (x - layout.size.width / 2).coerceIn(0f, w - layout.size.width)
            drawText(layout, topLeft = Offset(lx, bottom + 18f))
        }
        drawText(measurer, "listón", Offset(left + 20f, top - 8f), TextStyle(fontSize = 11.sp, color = AlbaColors.Alba))
    }
}
