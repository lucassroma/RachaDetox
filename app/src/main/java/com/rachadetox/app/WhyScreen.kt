package com.rachadetox.app

import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
            TextButton(onClick = onBack) { Text(tr("← Volver", "← Back", "← Indietro")) }

            // 1. Portada
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                BrainArt(Modifier.size(170.dp), state = BrainState.Overloaded)
                Spacer(Modifier.height(8.dp))
                Text(
                    tr("¿Por qué me aburro?", "Why am I bored?", "Perché mi annoio?"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = AlbaType.heading,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    tr(
                        "Lo que pasa en tu cabeza cada vez que deslizas el dedo.",
                        "What happens in your head every time you swipe.",
                        "Cosa succede nella tua testa ogni volta che scorri con il dito.",
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // 2. Tragaperras
            InfoCard(
                step = "1",
                title = tr("Llevas una tragaperras en el bolsillo", "There's a slot machine in your pocket", "Hai una slot machine in tasca"),
                body = tr(
                    "Cada vez que deslizas, tu cerebro no sabe qué va a salir: algo increíble o nada. " +
                        "Esa incertidumbre es el mismo mecanismo que usan las máquinas tragaperras: " +
                        "la recompensa variable. Es lo que más engancha, y no es casualidad. Está diseñado así.",
                    "Every time you swipe, your brain doesn't know what's coming: something amazing or nothing. " +
                        "That uncertainty is the same mechanism slot machines use: " +
                        "variable reward. It's what hooks you the most, and it's no accident. It's designed that way.",
                    "Ogni volta che scorri, il tuo cervello non sa cosa uscirà: qualcosa di incredibile o niente. " +
                        "Questa incertezza è lo stesso meccanismo delle slot machine: " +
                        "la ricompensa variabile. È ciò che crea più dipendenza, e non è un caso. È progettato così.",
                ),
            ) { SlotMachineArt(Modifier.fillMaxWidth().aspectRatio(1.6f)) }

            // 3. Picos de dopamina
            InfoCard(
                step = "2",
                title = tr("Pico, caída, otra vez", "Spike, drop, again", "Picco, caduta, di nuovo"),
                body = tr(
                    "La dopamina no es placer: es las ganas de más. Cada vídeo es un pequeño pico, " +
                        "y después de cada pico hay una caída que te deja por debajo de donde estabas. " +
                        "Por eso nunca es solo un vídeo.",
                    "Dopamine isn't pleasure: it's the craving for more. Every video is a small spike, " +
                        "and after every spike comes a drop that leaves you below where you started. " +
                        "That's why it's never just one video.",
                    "La dopamina non è piacere: è la voglia di averne ancora. Ogni video è un piccolo picco, " +
                        "e dopo ogni picco arriva una caduta che ti lascia più in basso di prima. " +
                        "Per questo non è mai un solo video.",
                ),
            ) {
                DopamineChart(Modifier.fillMaxWidth().aspectRatio(1.7f))
                Legend(
                    listOf(
                        AlbaColors.Alba to "Scroll",
                        AlbaColors.Salvia to tr("Un paseo, un libro, una charla", "A walk, a book, a chat", "Una passeggiata, un libro, una chiacchierata"),
                    )
                )
            }

            // 4. El listón
            InfoCard(
                step = "3",
                title = tr("Te han subido el listón", "They've raised your bar", "Ti hanno alzato l'asticella"),
                body = tr(
                    "Tu cerebro se adapta a lo que le das. Después de cientos de estímulos al día, " +
                        "lo normal ya no llega al listón: un libro, una conversación, un paseo. " +
                        "No es que el mundo sea aburrido. Es que te han subido el listón.",
                    "Your brain adapts to what you feed it. After hundreds of hits a day, " +
                        "ordinary things no longer clear the bar: a book, a conversation, a walk. " +
                        "It's not that the world is boring. It's that they've raised your bar.",
                    "Il tuo cervello si adatta a ciò che gli dai. Dopo centinaia di stimoli al giorno, " +
                        "le cose normali non superano più l'asticella: un libro, una conversazione, una passeggiata. " +
                        "Non è che il mondo sia noioso. È che ti hanno alzato l'asticella.",
                ),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BrainArt(Modifier.size(96.dp), state = BrainState.Calm)
                        Text(tr("Descansado", "Rested", "Riposato"), style = MaterialTheme.typography.labelMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BrainArt(Modifier.size(96.dp), state = BrainState.Overloaded)
                        Text(tr("Saturado", "Overloaded", "Sovraccarico"), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                ThresholdChart(Modifier.fillMaxWidth().aspectRatio(1.5f))
            }

            // 5. El dato
            InfoCard(
                step = "4",
                title = tr("Las horas pesan", "Hours add up", "Le ore pesano"),
                body = tr(
                    "En un estudio con más de 6.500 adolescentes de EE. UU., quienes pasaban muchas horas " +
                        "al día en redes tenían más riesgo de problemas como ansiedad o depresión que quienes no las usaban. " +
                        "Y el riesgo subía con las horas.",
                    "In a study of more than 6,500 US teenagers, those who spent many hours a day on social media " +
                        "had a higher risk of problems like anxiety or depression than those who didn't use it. " +
                        "And the risk went up with the hours.",
                    "In uno studio su oltre 6.500 adolescenti statunitensi, chi passava molte ore al giorno sui social " +
                        "aveva un rischio maggiore di problemi come ansia o depressione rispetto a chi non li usava. " +
                        "E il rischio cresceva con le ore.",
                ),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    BigStat("+60 %", tr("de 3 a 6 h\nal día", "3 to 6 h\na day", "da 3 a 6 h\nal giorno"))
                    BigStat("+78 %", tr("más de 6 h\nal día", "more than 6 h\na day", "più di 6 h\nal giorno"))
                }
            }

            // 6. Recuperación
            InfoCard(
                step = "5",
                title = tr("El listón baja", "The bar comes down", "L'asticella si abbassa"),
                body = tr(
                    "Esto no es para siempre. En un experimento, universitarios que limitaron las redes a " +
                        "unos 30 minutos al día se sintieron menos solos y menos deprimidos en solo tres semanas. " +
                        "El cerebro vuelve a disfrutar de lo sencillo si le das tiempo.",
                    "This isn't forever. In an experiment, college students who limited social media to " +
                        "about 30 minutes a day felt less lonely and less depressed in just three weeks. " +
                        "Your brain learns to enjoy simple things again if you give it time.",
                    "Non è per sempre. In un esperimento, studenti universitari che hanno limitato i social a " +
                        "circa 30 minuti al giorno si sono sentiti meno soli e meno depressi in sole tre settimane. " +
                        "Il cervello torna a godersi le cose semplici se gli dai tempo.",
                ),
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
                        tr(
                            "Cada día de tu racha es un día bajando el listón.",
                            "Every day of your streak is a day bringing the bar down.",
                            "Ogni giorno della tua serie è un giorno in cui l'asticella si abbassa.",
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = AlbaType.heading,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    AlbaButton(tr("Mira arriba", "Look up", "Guarda in alto"), onClick = onBack)
                }
            }

            // Fuentes
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tr("Fuentes", "Sources", "Fonti"), style = MaterialTheme.typography.labelLarge)
                SourceLink(
                    "Riehm et al. (2019). JAMA Psychiatry." + tr(" Tiempo en redes y problemas de salud mental en adolescentes.", " Time on social media and mental health problems in teens.", " Tempo sui social e problemi di salute mentale negli adolescenti."),
                    "https://jamanetwork.com/journals/jamapsychiatry/fullarticle/2749480",
                ) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                SourceLink(
                    "Hunt et al. (2018). No More FOMO: Limiting Social Media Decreases Loneliness and Depression. J. Social and Clinical Psychology.",
                    "https://guilfordjournals.com/doi/10.1521/jscp.2018.37.10.751",
                ) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                Text(
                    tr(
                        "Los estudios muestran asociación y efectos medios; cada persona es distinta. " +
                            "Si te sientes mal a menudo, hablar con un profesional ayuda.",
                        "Studies show associations and average effects; everyone is different. " +
                            "If you often feel down, talking to a professional helps.",
                        "Gli studi mostrano associazioni ed effetti medi; ogni persona è diversa. " +
                            "Se ti senti spesso giù, parlarne con un professionista aiuta.",
                    ),
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
            Text(title, style = MaterialTheme.typography.titleLarge, fontFamily = AlbaType.heading)
            art()
            Text(body, style = MaterialTheme.typography.bodyLarge, lineHeight = 24.sp)
        }
    }
}

@Composable
private fun BigStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 44.sp, fontFamily = AlbaType.heading, color = AlbaColors.Alba)
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
        drawText(measurer, tr("tu nivel normal", "your normal level", "il tuo livello normale"), Offset(pad, base + 6f), TextStyle(fontSize = 11.sp, color = axisColor))

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
        val items = listOf(
            tr("Paseo", "Walk", "Passeggiata") to 0.42f,
            tr("Libro", "Book", "Libro") to 0.38f,
            tr("Charla", "Chat", "Chiacchiere") to 0.46f,
            "Scroll" to 0.95f,
        )
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
        drawText(measurer, tr("listón de antes", "bar before", "asticella prima"), Offset(4f, oldY + 4f), TextStyle(fontSize = 11.sp, color = muted))

        // Listón de ahora
        val newY = bottom - chartH * newBar
        drawLine(AlbaColors.Alba, Offset(0f, newY), Offset(w, newY), strokeWidth = 6f)
        drawText(measurer, tr("listón de ahora", "bar now", "asticella ora"), Offset(4f, newY - 34f), TextStyle(fontSize = 11.sp, color = AlbaColors.Alba))
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

        listOf(
            tr("Hoy", "Today", "Oggi"),
            tr("Semana 1", "Week 1", "Settimana 1"),
            tr("Semana 2", "Week 2", "Settimana 2"),
            tr("Semana 3", "Week 3", "Settimana 3"),
        ).forEachIndexed { i, label ->
            val x = left + i * (right - left) / 3
            val layout = measurer.measure(label, TextStyle(fontSize = 11.sp, color = muted))
            val lx = (x - layout.size.width / 2).coerceIn(0f, w - layout.size.width)
            drawText(layout, topLeft = Offset(lx, bottom + 18f))
        }
        drawText(measurer, tr("listón", "bar", "asticella"), Offset(left + 20f, top - 8f), TextStyle(fontSize = 11.sp, color = AlbaColors.Alba))
    }
}
