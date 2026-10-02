package com.rachadetox.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Dibujos sueltos: sol y nube (iconos de la semana y animaciones)

private val CloudColor = Color(0xFF8F89AD)
private val CloudLight = Color(0xFFC9C2D8)
private val ChainColor = Color(0xFFC9C2D8)
private val BarColor = Color(0xFF3A3D63)

private fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun Float.ease(): Float = FastOutSlowInEasing.transform(this.coerceIn(0f, 1f))

private fun Float.seg(from: Float, to: Float): Float = ((this - from) / (to - from)).coerceIn(0f, 1f)

/** Sol con rayos de trazo redondeado. */
fun DrawScope.drawSun(
    center: Offset,
    radius: Float,
    color: Color = AlbaColors.Sol,
    alpha: Float = 1f,
    rays: Boolean = true,
    rotationDeg: Float = 0f,
    rayProgress: Float = 1f,
) {
    if (rays && rayProgress > 0f) {
        val n = 12
        for (i in 0 until n) {
            val a = (rotationDeg + i * 360f / n) * (PI.toFloat() / 180f)
            val dir = Offset(cos(a), sin(a))
            val from = center + dir * (radius * 1.3f)
            val to = center + dir * (radius * (1.3f + 0.45f * rayProgress))
            drawLine(
                color.copy(alpha = alpha),
                from,
                to,
                strokeWidth = radius * 0.16f,
                cap = StrokeCap.Round,
            )
        }
    }
    drawCircle(color.copy(alpha = alpha), radius, center)
}

/** Nube redondeada: tres círculos sobre una base plana. width es el ancho total. */
fun DrawScope.drawCloud(topLeft: Offset, width: Float, color: Color, alpha: Float = 1f) {
    val h = width * 0.55f
    val c = color.copy(alpha = alpha)
    drawRoundRect(
        c,
        topLeft = Offset(topLeft.x, topLeft.y + h * 0.45f),
        size = Size(width, h * 0.55f),
        cornerRadius = CornerRadius(h * 0.275f, h * 0.275f),
    )
    drawCircle(c, h * 0.32f, Offset(topLeft.x + width * 0.30f, topLeft.y + h * 0.50f))
    drawCircle(c, h * 0.42f, Offset(topLeft.x + width * 0.55f, topLeft.y + h * 0.38f))
    drawCircle(c, h * 0.27f, Offset(topLeft.x + width * 0.76f, topLeft.y + h * 0.56f))
}

/** Icono de día cumplido en la semana. */
@Composable
fun SunDayIcon(modifier: Modifier) {
    Canvas(modifier) {
        val c = center
        val r = size.minDimension * 0.24f
        drawCircle(AlbaColors.Sol.copy(alpha = 0.22f), size.minDimension * 0.5f, c)
        drawSun(c, r)
    }
}

/** Icono de día salvado en la semana. */
@Composable
fun CloudDayIcon(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width * 0.82f
        val h = w * 0.55f
        drawCloud(Offset((size.width - w) / 2f, (size.height - h) / 2f), w, CloudColor)
    }
}

// Decoración del panel amarillo de la racha

@Composable
fun StreakCardDecoration(bright: Boolean, ink: Color, modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "streakDecor")
    val rot by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(120_000, easing = LinearEasing)),
        label = "rays",
    )
    val drift by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(45_000, easing = LinearEasing)),
        label = "clouds",
    )
    val pulse by inf.animateFloat(
        0.85f, 1f,
        infiniteRepeatable(tween(3_200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sunCenter = Offset(w / 2f, 56.dp.toPx())

        if (bright) {
            drawCircle(
                Brush.radialGradient(
                    listOf(AlbaColors.Alba.copy(alpha = 0.55f * pulse), Color.Transparent),
                    center = sunCenter,
                    radius = w * 0.75f,
                ),
                radius = w * 0.75f,
                center = sunCenter,
            )
            val rays = 14
            for (i in 0 until rays) {
                val a = (rot + i * 360f / rays) * (PI.toFloat() / 180f)
                val spread = 0.045f
                val len = w * 1.2f
                val p = Path().apply {
                    moveTo(sunCenter.x, sunCenter.y)
                    lineTo(sunCenter.x + cos(a - spread) * len, sunCenter.y + sin(a - spread) * len)
                    lineTo(sunCenter.x + cos(a + spread) * len, sunCenter.y + sin(a + spread) * len)
                    close()
                }
                drawPath(p, Color.White.copy(alpha = 0.16f))
            }
            val far = Path().apply {
                moveTo(0f, h * 0.86f)
                cubicTo(w * 0.25f, h * 0.74f, w * 0.55f, h * 0.94f, w, h * 0.80f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(far, AlbaColors.Alba.copy(alpha = 0.32f))
            val near = Path().apply {
                moveTo(0f, h * 0.93f)
                cubicTo(w * 0.30f, h * 0.84f, w * 0.65f, h * 1.02f, w, h * 0.90f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(near, AlbaColors.Alba.copy(alpha = 0.45f))
        } else {
            val far = Path().apply {
                moveTo(0f, h * 0.88f)
                cubicTo(w * 0.25f, h * 0.78f, w * 0.55f, h * 0.96f, w, h * 0.84f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(far, ink.copy(alpha = 0.07f))
        }

        fun cloudAt(phase: Float, y: Float, width: Float, alpha: Float) {
            val t = (drift + phase) % 1f
            val x = lerpF(-width, w, t)
            val edge = minOf(t / 0.12f, (1f - t) / 0.12f, 1f).coerceIn(0f, 1f)
            drawCloud(Offset(x, y), width, if (bright) Color.White else ink, alpha * edge)
        }
        cloudAt(0.05f, h * 0.10f, w * 0.20f, if (bright) 0.55f else 0.12f)
        cloudAt(0.55f, h * 0.30f, w * 0.14f, if (bright) 0.40f else 0.09f)
        cloudAt(0.30f, h * 0.62f, w * 0.17f, if (bright) 0.35f else 0.08f)
    }
}

// Animaciones a pantalla completa

enum class StreakAnimKind(val key: String) { Rise("rise"), Lost("lost"), Saved("saved") }

data class StreakAnim(val kind: StreakAnimKind, val days: Int)

private const val ANIM_MS = 3600

/** La racha perdida: los días bajan hasta cero en 3,45 s. */
private const val LOST_ANIM_MS = 3450

/**
 * Rise: cadenas que se rompen y un sol que amanece.
 * Lost: el contador de días baja hasta cero mientras el sol se pone.
 * Saved: un sol que se nubla y barras de cárcel que se cierran.
 * Se cierra sola o al tocar la pantalla.
 */
@Composable
fun StreakAnimationOverlay(anim: StreakAnim, onDone: () -> Unit) {
    val progress = remember(anim) { Animatable(0f) }
    LaunchedEffect(anim) {
        val ms = if (anim.kind == StreakAnimKind.Lost) LOST_ANIM_MS else ANIM_MS
        progress.animateTo(1f, tween(ms, easing = LinearEasing))
        delay(1600)
        onDone()
    }
    val p = progress.value

    val (top, bottom) = when (anim.kind) {
        StreakAnimKind.Rise -> AlbaColors.Noche to AlbaColors.Bruma
        else -> AlbaColors.Noche to AlbaColors.NocheClara
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDone() },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when (anim.kind) {
                StreakAnimKind.Rise -> drawRise(p)
                StreakAnimKind.Lost -> drawSunset(p)
                StreakAnimKind.Saved -> drawClouded(p, saved = true)
            }
        }

        val (title, body) = when (anim.kind) {
            StreakAnimKind.Rise -> Pair(
                tr("Otro día libre", "Another day free", "Un altro giorno libero"),
                tr(
                    "Llevas ${anim.days} ${dias(anim.days)} seguidos.",
                    "${anim.days} ${dias(anim.days)} in a row.",
                    "${anim.days} ${dias(anim.days)} di fila.",
                ),
            )
            StreakAnimKind.Lost -> Pair(
                tr("Racha perdida", "Streak lost", "Serie persa"),
                tr(
                    "Tu racha de ${anim.days} ${dias(anim.days)} se ha roto. El sol siempre vuelve a salir.",
                    "Your streak of ${anim.days} ${dias(anim.days)} is over. The sun always rises again.",
                    "La tua serie di ${anim.days} ${dias(anim.days)} è finita. Il sole torna sempre.",
                ),
            )
            StreakAnimKind.Saved -> Pair(
                tr("Día salvado", "Day saved", "Giornata salvata"),
                tr(
                    "Medio sol, pero sol. Tu racha sigue en ${anim.days} ${dias(anim.days)}.",
                    "Half a sun, but still a sun. Your streak stays at ${anim.days} ${dias(anim.days)}.",
                    "Mezzo sole, ma pur sempre sole. La tua serie resta a ${anim.days} ${dias(anim.days)}.",
                ),
            )
        }
        if (anim.kind == StreakAnimKind.Lost) {
            // Cuenta atrás: empieza despacio y acelera, como algo que se escapa
            val shown = kotlin.math.ceil(anim.days * (1f - FastOutLinearInEasing.transform(p))).toInt()
            val gone = p >= 1f
            Column(
                Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "$shown",
                    color = lerp(AlbaColors.Sol, CloudLight, p),
                    fontFamily = FontFamily.Serif,
                    fontSize = 120.sp,
                    lineHeight = 124.sp,
                )
                Text(
                    if (shown == 1) tr("día seguido", "day in a row", "giorno di fila") else tr("días seguidos", "days in a row", "giorni di fila"),
                    color = AlbaColors.Arena.copy(alpha = if (gone) 0.6f else 0.85f),
                    fontSize = 18.sp,
                )
            }
        }

        val textAlpha = if (anim.kind == StreakAnimKind.Lost) p.seg(0.80f, 1f) else p.seg(0.55f, 0.85f)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 32.dp, end = 32.dp, bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                color = AlbaColors.Arena.copy(alpha = textAlpha),
                fontFamily = FontFamily.Serif,
                fontSize = 32.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                body,
                color = AlbaColors.Arena.copy(alpha = 0.85f * textAlpha),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// cadenas + amanecer

private fun DrawScope.drawRise(p: Float) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.60f
    val sunR = w * 0.13f

    val sunP = p.seg(0.30f, 0.90f).ease()
    val sunCenter = Offset(w / 2f, lerpF(horizon + sunR * 1.6f, h * 0.30f, sunP))
    drawCircle(
        Brush.radialGradient(
            listOf(AlbaColors.Alba.copy(alpha = 0.55f * sunP), Color.Transparent),
            center = sunCenter,
            radius = w * 0.75f,
        ),
        radius = w * 0.75f,
        center = sunCenter,
    )
    drawSun(sunCenter, sunR, rotationDeg = p * 40f, rayProgress = sunP)

    drawRect(
        AlbaColors.Noche,
        topLeft = Offset(0f, horizon),
        size = Size(w, h - horizon),
    )
    drawLine(
        AlbaColors.Alba.copy(alpha = 0.7f * sunP),
        Offset(0f, horizon), Offset(w, horizon),
        strokeWidth = 3f, cap = StrokeCap.Round,
    )

    val n = 9
    val chainY = h * 0.30f
    val x0 = w * 0.06f
    val span = w * 0.88f
    val step = span / (n - 1)
    val breakP = p.seg(0.22f, 0.62f)
    val shake = if (p < 0.22f) sin(p * 220f) * w * 0.006f * (p / 0.22f) else 0f
    val mid = n / 2

    for (i in 0 until n) {
        if (i == mid && breakP > 0f) continue
        val dir = if (i < mid) -1f else 1f
        val dist = kotlin.math.abs(i - mid).toFloat() / mid
        val b = breakP.ease()
        val dx = dir * b * w * (0.10f + 0.20f * dist)
        val dy = b * b * h * (0.30f + 0.20f * dist)
        val rot = dir * b * (35f + 30f * dist)
        val fade = 1f - (breakP - 0.55f).coerceAtLeast(0f) / 0.45f
        val cx = x0 + step * i + dx
        val cy = chainY + dy + shake * (if (i % 2 == 0) 1f else -1f)
        drawLink(Offset(cx, cy), step, flat = i % 2 == 0, degrees = rot, alpha = fade.coerceIn(0f, 1f))
    }

    val spark = p.seg(0.22f, 0.50f)
    if (spark > 0f && spark < 1f) {
        val c = Offset(x0 + step * mid, chainY)
        for (i in 0 until 8) {
            val a = i * (2f * PI.toFloat() / 8f)
            val d = Offset(cos(a), sin(a))
            drawLine(
                AlbaColors.Sol.copy(alpha = 1f - spark),
                c + d * (step * (0.4f + spark)),
                c + d * (step * (0.7f + 1.4f * spark)),
                strokeWidth = 6f, cap = StrokeCap.Round,
            )
        }
    }
}

private fun DrawScope.drawLink(center: Offset, step: Float, flat: Boolean, degrees: Float, alpha: Float) {
    if (alpha <= 0f) return
    val lw = if (flat) step * 1.25f else step * 0.55f
    val lh = step * 0.62f
    val stroke = step * 0.13f
    rotate(degrees, pivot = center) {
        drawRoundRect(
            ChainColor.copy(alpha = alpha),
            topLeft = Offset(center.x - lw / 2f, center.y - lh / 2f),
            size = Size(lw, lh),
            cornerRadius = CornerRadius(lh / 2f, lh / 2f),
            style = Stroke(stroke),
        )
    }
}

// puesta de sol (racha perdida)

private fun DrawScope.drawSunset(p: Float) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.78f
    val sunR = w * 0.10f
    val sinkP = p.ease()

    // El cielo se apaga hacia el gris
    drawRect(Color(0xFF2C2C33).copy(alpha = 0.85f * sinkP))

    val sunCenter = Offset(w / 2f, lerpF(h * 0.16f, horizon + sunR * 1.2f, sinkP))
    drawCircle(
        Brush.radialGradient(
            listOf(AlbaColors.Alba.copy(alpha = 0.45f * (1f - sinkP)), Color.Transparent),
            center = sunCenter,
            radius = w * 0.6f,
        ),
        radius = w * 0.6f,
        center = sunCenter,
    )
    drawSun(sunCenter, sunR, alpha = 1f - 0.6f * sinkP, rotationDeg = -p * 30f, rayProgress = 1f - sinkP)

    drawRect(
        AlbaColors.Noche,
        topLeft = Offset(0f, horizon),
        size = Size(w, h - horizon),
    )
    drawLine(
        CloudLight.copy(alpha = 0.5f),
        Offset(0f, horizon), Offset(w, horizon),
        strokeWidth = 3f, cap = StrokeCap.Round,
    )
}

// nube + barras

private fun DrawScope.drawClouded(p: Float, saved: Boolean) {
    val w = size.width
    val h = size.height
    val sunR = w * 0.13f
    val sunCenter = Offset(w / 2f, h * 0.30f)

    val cloudP = p.seg(0.05f, 0.50f).ease()
    val sunAlpha = 1f - (if (saved) 0.45f else 0.80f) * cloudP
    drawSun(sunCenter, sunR, alpha = sunAlpha, rayProgress = 1f - cloudP)

    val cw = w * 0.78f
    val cloudX = lerpF(-cw, sunCenter.x - cw * 0.55f, cloudP)
    drawCloud(Offset(cloudX, sunCenter.y - cw * 0.16f), cw, CloudColor, 0.95f)
    val cw2 = w * 0.55f
    val cloud2X = lerpF(w, sunCenter.x - cw2 * 0.25f, cloudP)
    drawCloud(Offset(cloud2X, sunCenter.y - cw2 * 0.02f), cw2, CloudLight, if (saved) 0.55f else 0.85f)

    val bars = 7
    val left = w * 0.10f
    val right = w * 0.90f
    val topY = h * 0.08f
    val botY = h * 0.66f
    val gap = (right - left) / (bars - 1)
    val barW = w * 0.030f
    val closeP = p.seg(0.48f, 0.92f)
    for (i in 0 until bars) {
        val bp = ((closeP - i * 0.07f) / 0.45f).coerceIn(0f, 1f).ease()
        if (bp <= 0f) continue
        val x = left + gap * i
        val bottom = lerpF(topY, botY, bp)
        drawRoundRect(
            BarColor,
            topLeft = Offset(x - barW / 2f, topY - h * 0.05f),
            size = Size(barW, bottom - topY + h * 0.05f),
            cornerRadius = CornerRadius(barW / 2f, barW / 2f),
        )
        drawLine(
            CloudLight.copy(alpha = 0.35f),
            Offset(x - barW * 0.22f, topY), Offset(x - barW * 0.22f, bottom - barW),
            strokeWidth = barW * 0.16f, cap = StrokeCap.Round,
        )
    }
    val railP = closeP.seg(0.7f, 1f).ease()
    if (railP > 0f) {
        for (y in listOf(topY + h * 0.03f, botY - h * 0.05f)) {
            val len = (right - left + barW) * railP
            drawRoundRect(
                BarColor,
                topLeft = Offset(w / 2f - len / 2f, y - barW * 0.4f),
                size = Size(len, barW * 0.8f),
                cornerRadius = CornerRadius(barW * 0.4f, barW * 0.4f),
            )
        }
    }
    if (!saved) drawRect(AlbaColors.Noche.copy(alpha = 0.25f * closeP))
}
