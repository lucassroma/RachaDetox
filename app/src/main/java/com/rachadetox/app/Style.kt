@file:OptIn(ExperimentalTextApi::class)

package com.rachadetox.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================================
// Estilo de Alba: cristal líquido con los colores de siempre (naranja, blanco, gris).
// Sin luces de neón. Botones con el rebote clásico al pulsarlos.
// Sigue el modo claro / oscuro del móvil.
// ============================================================================

/** Todo lo que define el aspecto de Alba. */
data class StyleSpec(
    val dark: Boolean,
    val scheme: ColorScheme,
    val heading: FontFamily,
    val body: FontFamily,
    // Fondo y su tono según cómo va el día
    val background: Color,
    val cloudyBackground: Color,
    val savedBackground: Color,
    // Cristal
    val shape: Shape,
    val glass: Brush,
    val glassBorder: BorderStroke,
    val onCard: Color,
    val muted: Color,
    // Panel de la racha cuando brilla
    val heroBright: Brush,
    val onHeroBright: Color,
    // Panel de la racha cuando la has perdido: el azul opuesto al naranja (círculo cromático)
    val heroLost: Brush,
    val onHeroLost: Color,
    // Avisos
    val alert: Color,
    val onAlert: Color,
    val warn: Color,
    val onWarn: Color,
    // Botones
    val buttonShape: Shape,
    val button: Brush,
    val onButton: Color,
    val onTonal: Color,
)

private fun variable(res: Int, weights: List<Int>) = FontFamily(
    weights.map { w -> Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) }
)

private val Manrope by lazy { variable(R.font.manrope, listOf(400, 500, 600, 700, 800)) }
private val Fraunces by lazy { variable(R.font.fraunces, listOf(400, 500, 600, 700)) }

// Colores de siempre
private val Naranja = AlbaColors.Sol           // el naranja de Alba
private val NaranjaSuave = AlbaColors.Alba    // el mismo naranja, más claro
private val Tinta = Color(0xFF1F1F23)          // casi negro
private val Gris = Color(0xFF6B6B73)
private val GrisClaro = Color(0xFFB9B9C0)
private val Blanco = Color(0xFFF7F5F2)         // blanco cálido
private val Carbon = Color(0xFF141416)          // fondo oscuro

// Opuesto del naranja en el círculo cromático: F28A3E (tono 25°) → 3EA6F2 (tono 205°).
// En modo oscuro, el opuesto del naranja tostado B85E22 → 227CB8.
private val AzulOpuesto = Color(0xFF3EA6F2)
private val AzulOpuestoOscuro = Color(0xFF227CB8)

fun specFor(dark: Boolean): StyleSpec = if (!dark) {
    StyleSpec(
        dark = false,
        scheme = lightColorScheme(
            primary = Tinta, onPrimary = Blanco,
            primaryContainer = Naranja, onPrimaryContainer = Tinta,
            secondary = Naranja, onSecondary = Tinta,
            secondaryContainer = Color(0xFFFBE3D3), onSecondaryContainer = Tinta,
            tertiary = AlbaColors.Salvia,
            background = Blanco, onBackground = Tinta,
            surface = Blanco, onSurface = Tinta,
            surfaceVariant = Color(0xFFEDEBE8), onSurfaceVariant = Gris,
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF3F1EE),
            surfaceContainer = Color(0xFFF0EEEB), surfaceContainerHigh = Color(0xFFF3F1EE),
            surfaceContainerHighest = Color(0xFFE8E6E3),
            outline = GrisClaro, outlineVariant = Color(0xFFE2E0DD),
            error = Color(0xFFC4573A),
        ),
        heading = Fraunces, body = Manrope,
        background = Blanco,
        cloudyBackground = Color(0xFFA3A3A9),
        savedBackground = Color(0xFFE6E4E1),
        shape = RoundedCornerShape(26.dp),
        glass = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.78f), Color.White.copy(alpha = 0.52f))),
        glassBorder = BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(Color.White, Color.White.copy(alpha = 0.35f), GrisClaro.copy(alpha = 0.35f))),
        ),
        onCard = Tinta, muted = Gris,
        heroBright = SolidColor(Color.White.copy(alpha = 0.86f)),
        heroLost = Brush.verticalGradient(listOf(AzulOpuesto.copy(alpha = 0.90f), AzulOpuesto.copy(alpha = 0.74f))),
        onHeroLost = Tinta,
        onHeroBright = Tinta,
        alert = Color(0xFF2A2A2E).copy(alpha = 0.92f), onAlert = Blanco,
        warn = Naranja.copy(alpha = 0.92f), onWarn = Tinta,
        buttonShape = RoundedCornerShape(50),
        button = SolidColor(Naranja),
        onButton = Tinta,
        onTonal = Tinta,
    )
} else {
    StyleSpec(
        dark = true,
        scheme = darkColorScheme(
            primary = Naranja, onPrimary = Tinta,
            primaryContainer = Naranja, onPrimaryContainer = Tinta,
            secondary = Naranja, onSecondary = Tinta,
            secondaryContainer = Color(0xFF2C2C31), onSecondaryContainer = Blanco,
            tertiary = AlbaColors.Salvia,
            background = Carbon, onBackground = Blanco,
            surface = Carbon, onSurface = Blanco,
            surfaceVariant = Color(0xFF232327), onSurfaceVariant = GrisClaro,
            surfaceContainerLowest = Carbon, surfaceContainerLow = Color(0xFF1B1B1E),
            surfaceContainer = Color(0xFF202023), surfaceContainerHigh = Color(0xFF26262A),
            surfaceContainerHighest = Color(0xFF2C2C31),
            outline = Color(0xFF4A4A50), outlineVariant = Color(0xFF34343A),
            error = Naranja,
        ),
        heading = Fraunces, body = Manrope,
        background = Carbon,
        cloudyBackground = Color(0xFF2A2A2E),
        savedBackground = Color(0xFF1E1E22),
        shape = RoundedCornerShape(26.dp),
        glass = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.05f))),
        glassBorder = BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.12f))),
        ),
        onCard = Blanco, muted = GrisClaro,
        heroBright = SolidColor(Color.White.copy(alpha = 0.14f)),
        heroLost = Brush.verticalGradient(listOf(AzulOpuestoOscuro.copy(alpha = 0.92f), AzulOpuestoOscuro.copy(alpha = 0.78f))),
        onHeroLost = Blanco,
        onHeroBright = Blanco,
        alert = Color(0xFF2C2C31).copy(alpha = 0.95f), onAlert = Blanco,
        warn = Naranja.copy(alpha = 0.92f), onWarn = Tinta,
        buttonShape = RoundedCornerShape(50),
        button = SolidColor(Naranja),
        onButton = Tinta,
        onTonal = Blanco,
    )
}

val LocalAlbaStyle = staticCompositionLocalOf { specFor(false) }

/** Tipografía de títulos de Alba. */
object AlbaType {
    val heading: FontFamily
        @Composable @ReadOnlyComposable get() = LocalAlbaStyle.current.heading
}

/** La tipografía de Material con la letra de Alba. */
fun typographyFor(body: FontFamily): Typography {
    val t = Typography()
    fun TextStyle.f() = copy(fontFamily = body)
    return Typography(
        displayLarge = t.displayLarge.f(), displayMedium = t.displayMedium.f(), displaySmall = t.displaySmall.f(),
        headlineLarge = t.headlineLarge.f(), headlineMedium = t.headlineMedium.f(), headlineSmall = t.headlineSmall.f(),
        titleLarge = t.titleLarge.f(), titleMedium = t.titleMedium.f(), titleSmall = t.titleSmall.f(),
        bodyLarge = t.bodyLarge.f(), bodyMedium = t.bodyMedium.f(), bodySmall = t.bodySmall.f(),
        labelLarge = t.labelLarge.f(), labelMedium = t.labelMedium.f(), labelSmall = t.labelSmall.f(),
    )
}

/** Color de fondo según cómo va el día (se oscurece al pasarte, gris claro si la salvaste). */
fun StyleSpec.moodBackground(cloud: Float, saved: Boolean): Color = when {
    cloud > 0f -> lerp(background, cloudyBackground, cloud.coerceIn(0f, 1f))
    saved -> savedBackground
    else -> background
}

/** Tema de Alba: sigue el modo claro / oscuro del móvil. */
@Composable
fun AlbaStyleProvider(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val spec = remember(dark) { specFor(dark) }
    CompositionLocalProvider(LocalAlbaStyle provides spec) {
        MaterialTheme(colorScheme = spec.scheme, typography = typographyFor(spec.body), content = content)
    }
}

// ============================================================================
// Fondo: liso, con un único resplandor naranja muy suave arriba que se mueve
// despacio para que el cristal se note. Minimalista: un solo color.
// ============================================================================

@Composable
fun StyleBackground(cloud: Float, saved: Boolean, content: @Composable BoxScope.() -> Unit) {
    val s = LocalAlbaStyle.current
    val base by animateColorAsState(s.moodBackground(cloud, saved), tween(1200), label = "bg")
    val warmth = 1f - cloud.coerceIn(0f, 1f) * 0.9f
    val inf = rememberInfiniteTransition(label = "bgDrift")
    val t by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t",
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(base),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val k = if (s.dark) 0.45f else 1f
            fun blob(color: Color, alpha: Float, cx: Float, cy: Float, r: Float) = drawCircle(
                Brush.radialGradient(listOf(color.copy(alpha = alpha * k), Color.Transparent), Offset(cx, cy), r),
                radius = r, center = Offset(cx, cy),
            )
            // Franja naranja arriba (≈38 % de la pantalla, fija aunque hagas scroll):
            // la cabecera y el panel de la racha flotan encima como cristal.
            // Al pasarte se apaga hacia gris; con la racha salvada se aclara.
            // En modo oscuro, un naranja tostado para que el texto blanco se lea bien
            val orange = if (s.dark) Color(0xFFB85E22) else Naranja
            val sheet = when {
                cloud > 0f -> lerp(orange, s.cloudyBackground, cloud.coerceIn(0f, 1f))
                saved -> lerp(orange, if (s.dark) s.savedBackground else NaranjaSuave, 0.6f)
                else -> orange
            }
            val radius = 40.dp.toPx()
            drawRoundRect(
                sheet,
                topLeft = Offset(0f, -radius),
                size = androidx.compose.ui.geometry.Size(w, h * 0.38f + radius),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
            // Un brillo claro que se mueve despacio sobre el naranja, para que el cristal se note
            blob(Color.White, 0.22f * warmth, w * (0.80f - 0.25f * t), h * (0.04f + 0.05f * t), w * 0.75f)
        }
        content()
    }
}

// ============================================================================
// Rebote al pulsar (el clásico «tap / bounce» de Motion o Magic UI):
// al pulsar se encoge; al soltar vuelve con un pequeño rebote.
// ============================================================================

@Composable
fun Modifier.bounce(interaction: MutableInteractionSource, depth: Float = 0.9f): Modifier {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(interaction) {
        var job: Job? = null
        interaction.interactions.collect { i ->
            when (i) {
                is PressInteraction.Press -> {
                    job?.cancel()
                    job = launch { scale.animateTo(depth, spring(stiffness = 1800f)) }
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    job?.cancel()
                    job = launch {
                        // Aunque el toque sea muy rápido, que se note el apretón antes del rebote
                        if (scale.value > depth + 0.02f) scale.animateTo(depth, tween(70))
                        scale.animateTo(1f, spring(dampingRatio = 0.32f, stiffness = 420f))
                    }
                }
            }
        }
    }
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

// ============================================================================
// Tarjeta y botón de cristal
// ============================================================================

/** Reflejo de cristal: una luz suave arriba, como la superficie de un líquido. */
private fun Modifier.glassSheen(dark: Boolean): Modifier = drawWithContent {
    drawContent()
    drawRect(
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = if (dark) 0.10f else 0.35f),
            0.35f to Color.Transparent,
            1f to Color.Transparent,
        ),
    )
}

/**
 * Tarjeta de cristal líquido. Con [brush] o [container] se tiñe (panel de la racha, avisos)
 * pero conserva el borde y el reflejo. Si se puede tocar, rebota un poco.
 */
@Composable
fun AlbaCard(
    modifier: Modifier = Modifier,
    container: Color? = null,
    brush: Brush? = null,
    contentColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val s = LocalAlbaStyle.current
    val interaction = remember { MutableInteractionSource() }
    var m = modifier
    if (onClick != null) m = m.bounce(interaction, depth = 0.96f)
    m = m.clip(s.shape)
    m = when {
        brush != null -> m.background(brush)
        container != null -> m.background(container)
        else -> m.background(s.glass)
    }
    m = m.glassSheen(s.dark).border(s.glassBorder, s.shape)
    if (onClick != null) m = m.clickable(interaction, LocalIndication.current, onClick = onClick)
    CompositionLocalProvider(LocalContentColor provides (contentColor ?: s.onCard)) {
        Box(m, content = content)
    }
}

/**
 * Botón de Alba con el rebote clásico. Principal: naranja de siempre con reflejo de cristal.
 * [tonal]: de cristal, para acciones secundarias.
 */
@Composable
fun AlbaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tonal: Boolean = false,
    padding: PaddingValues = PaddingValues(horizontal = 22.dp, vertical = 13.dp),
) {
    val s = LocalAlbaStyle.current
    val interaction = remember { MutableInteractionSource() }
    var m = modifier
        .bounce(interaction, depth = 0.88f)
        .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
        .clip(s.buttonShape)
    m = if (tonal) m.background(s.glass) else m.background(s.button)
    m = m.glassSheen(s.dark).border(s.glassBorder, s.buttonShape)
        .clickable(interaction, LocalIndication.current, enabled = enabled, onClick = onClick)
        .padding(padding)
    Box(m) {
        Text(
            text,
            color = if (tonal) s.onTonal else s.onButton,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ============================================================================
// Movimiento
// ============================================================================

/** Las piezas que ya han aparecido no vuelven a animarse al hacer scroll. */
val LocalRevealed = staticCompositionLocalOf<MutableSet<String>> { mutableSetOf() }

/** Aparición escalonada con un leve desenfoque (Magic UI «Blur Fade»). */
fun Modifier.reveal(key: String, index: Int): Modifier = composed {
    val revealed = LocalRevealed.current
    val already = remember(key) { key in revealed }
    val p = remember(key) { Animatable(if (already) 1f else 0f) }
    LaunchedEffect(key) {
        if (!already) {
            delay(index.coerceAtMost(8) * 70L)
            p.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            revealed.add(key)
        }
    }
    val v = p.value
    val base = this.graphicsLayer {
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 22.dp.toPx()
    }
    if (v < 0.99f) base.blur(((1f - v) * 8f).dp, BlurredEdgeTreatment.Unbounded) else base
}

/** Número que cuenta hasta su valor (Magic UI «Number Ticker»). */
@Composable
fun tickerValue(target: Int): Int {
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(target) { shown = target }
    val v by animateIntAsState(shown, tween(900, easing = FastOutSlowInEasing), label = "ticker")
    return v
}
