@file:OptIn(ExperimentalTextApi::class)

package com.rachadetox.app

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// ============================================================================
// Los tres estilos de Alba
//
// Aurora  → Magic UI / Motion / Skecher: noche, cristal, luz que se mueve.
// Lavanda → Sleeko Studio (+ anillos de Anime.js): claro, violeta, tipografía gruesa.
// Retro   → RetroUI / NeoBrutalism: crema, bordes negros, sombras duras, amarillo.
// ============================================================================

enum class AlbaStyle(val key: String, val label: String) {
    Aurora("aurora", "Aurora"),
    Lavanda("lavanda", "Lavanda"),
    Retro("retro", "Retro");

    val description: String
        get() = when (this) {
            Aurora -> tr(
                "Noche y cristal, con luz que se mueve.",
                "Night and glass, with moving light.",
                "Notte e vetro, con luce in movimento.",
            )
            Lavanda -> tr(
                "Claro y limpio, violeta y letras grandes.",
                "Light and clean, violet and big type.",
                "Chiaro e pulito, viola e caratteri grandi.",
            )
            Retro -> tr(
                "Bordes gruesos, sombras duras y mucho amarillo.",
                "Thick borders, hard shadows and lots of yellow.",
                "Bordi spessi, ombre nette e tanto giallo.",
            )
        }
}

/** Estilo elegido. Es estado de Compose: al cambiarlo, toda la app se redibuja. */
object Styles {
    private const val PREFS = "racha_detox"
    private const val KEY = "style"

    private val state = mutableStateOf(AlbaStyle.Aurora)
    private var chosen = mutableStateOf(false)

    val current: AlbaStyle get() = state.value
    val hasChosen: Boolean get() = chosen.value

    private const val KEY_CHOSEN = "style_chosen"

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = prefs.getString(KEY, null)
        chosen.value = prefs.getBoolean(KEY_CHOSEN, false)
        state.value = AlbaStyle.entries.firstOrNull { it.key == key } ?: AlbaStyle.Aurora
    }

    /** Probar un estilo sin decidirse todavía. */
    fun preview(context: Context, style: AlbaStyle) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, style.key).apply()
        state.value = style
    }

    /** Quedarse con un estilo. */
    fun set(context: Context, style: AlbaStyle) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, style.key).putBoolean(KEY_CHOSEN, true).apply()
        chosen.value = true
        state.value = style
    }
}

enum class EnterKind { BlurFade, FadeUp, Pop }

/** Todo lo que cambia de un estilo a otro. */
data class StyleSpec(
    val style: AlbaStyle,
    val dark: Boolean,
    val scheme: ColorScheme,
    val heading: FontFamily,
    val body: FontFamily,
    // Fondo y su tono según cómo va el día
    val background: Color,
    val cloudyBackground: Color,
    val savedBackground: Color,
    // Tarjetas
    val shape: Shape,
    val card: Color,
    val onCard: Color,
    val muted: Color,
    val border: BorderStroke?,
    val softShadow: Dp,
    val shadowColor: Color,
    val hardShadow: Dp,
    // Panel de la racha
    val heroBright: Brush,
    val onHeroBright: Color,
    // Avisos
    val alert: Color,
    val onAlert: Color,
    val warn: Color,
    val onWarn: Color,
    // Botones
    val buttonShape: Shape,
    val button: Brush,
    val onButton: Color,
    val tonal: Color,
    val onTonal: Color,
    // Movimiento
    val enter: EnterKind,
    val springPress: Boolean,
    val shimmer: Boolean,
    val borderBeam: Boolean,
    val segmentRing: Boolean,
    val uppercaseLabels: Boolean,
)

private fun variable(res: Int, weights: List<Int> = listOf(400, 500, 600, 700, 800)) = FontFamily(
    weights.map { w -> Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) }
)

private val Manrope by lazy { variable(R.font.manrope) }
private val SpaceGrotesk by lazy { variable(R.font.space_grotesk, listOf(400, 500, 600, 700)) }
private val Fraunces by lazy { variable(R.font.fraunces, listOf(400, 500, 600, 700)) }
private val ArchivoBlack by lazy { FontFamily(Font(R.font.archivo_black, FontWeight.Normal), Font(R.font.archivo_black, FontWeight.Bold)) }

// ---- Aurora
private val AuroraNight = Color(0xFF0D0F24)
private val AuroraInk = Color(0xFFF4EEFF)
private val AuroraMuted = Color(0xFFB7B1CC)
private val AuroraGlass = Color.White.copy(alpha = 0.07f)

// ---- Lavanda
private val LavViolet = Color(0xFF6C4CF1)
private val LavVioletLight = Color(0xFFA996FF)
private val LavInk = Color(0xFF1B1530)
private val LavMuted = Color(0xFF6E6787)
private val LavBg = Color(0xFFF5F3FF)

// ---- Retro
private val RetroCream = Color(0xFFFFF4E0)
private val RetroInk = Color(0xFF111111)
private val RetroYellow = Color(0xFFFFD23F)
private val RetroCoral = Color(0xFFFF8A65)
private val RetroMint = Color(0xFF7ED6C1)

fun specFor(style: AlbaStyle): StyleSpec = when (style) {
    AlbaStyle.Aurora -> StyleSpec(
        style = style,
        dark = true,
        scheme = darkColorScheme(
            primary = AlbaColors.Sol, onPrimary = AlbaColors.Noche,
            primaryContainer = AlbaColors.Sol, onPrimaryContainer = AlbaColors.Noche,
            secondary = AlbaColors.Alba, onSecondary = AlbaColors.Noche,
            secondaryContainer = Color(0xFF2A2D52), onSecondaryContainer = AuroraInk,
            tertiary = AlbaColors.Salvia,
            background = AuroraNight, onBackground = AuroraInk,
            surface = AuroraNight, onSurface = AuroraInk,
            surfaceVariant = Color(0xFF1C1F3D), onSurfaceVariant = AuroraMuted,
            surfaceContainerLowest = AuroraNight, surfaceContainerLow = Color(0xFF171A35),
            surfaceContainer = Color(0xFF1C1F3D), surfaceContainerHigh = Color(0xFF22254A),
            surfaceContainerHighest = Color(0xFF282B52),
            outline = Color(0xFF4A4770), outlineVariant = Color(0xFF34325A),
            error = AlbaColors.Alba,
        ),
        heading = Fraunces, body = Manrope,
        background = AuroraNight,
        cloudyBackground = Color(0xFF1B1B21),
        savedBackground = Color(0xFF151733),
        shape = RoundedCornerShape(24.dp),
        card = AuroraGlass, onCard = AuroraInk, muted = AuroraMuted,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        softShadow = 0.dp, shadowColor = Color.Transparent, hardShadow = 0.dp,
        heroBright = Brush.verticalGradient(listOf(Color(0xFF2B2350), Color(0xFF3B2A4F))),
        onHeroBright = AuroraInk,
        alert = Color(0xFF26294F), onAlert = AuroraInk,
        warn = Color(0xFF4A2A2E), onWarn = Color(0xFFFFD9CC),
        buttonShape = RoundedCornerShape(50),
        button = Brush.horizontalGradient(listOf(AlbaColors.Sol, AlbaColors.Alba)),
        onButton = AlbaColors.Noche,
        tonal = Color.White.copy(alpha = 0.10f), onTonal = AuroraInk,
        enter = EnterKind.BlurFade,
        springPress = true, shimmer = true, borderBeam = true, segmentRing = false,
        uppercaseLabels = false,
    )

    AlbaStyle.Lavanda -> StyleSpec(
        style = style,
        dark = false,
        scheme = lightColorScheme(
            primary = LavViolet, onPrimary = Color.White,
            primaryContainer = Color(0xFFE9E3FF), onPrimaryContainer = LavInk,
            secondary = LavViolet, onSecondary = Color.White,
            secondaryContainer = Color(0xFFE9E3FF), onSecondaryContainer = LavInk,
            tertiary = Color(0xFF3CC79A),
            background = LavBg, onBackground = LavInk,
            surface = LavBg, onSurface = LavInk,
            surfaceVariant = Color(0xFFEDE9FB), onSurfaceVariant = LavMuted,
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
            surfaceContainer = Color.White, surfaceContainerHigh = Color.White,
            surfaceContainerHighest = Color(0xFFF1EEFC),
            outline = Color(0xFFD9D3F0), outlineVariant = Color(0xFFE6E1F7),
            error = Color(0xFFE5484D),
        ),
        heading = Manrope, body = Manrope,
        background = LavBg,
        cloudyBackground = Color(0xFFB9B6C6),
        savedBackground = Color(0xFFE9E7F0),
        shape = RoundedCornerShape(28.dp),
        card = Color.White, onCard = LavInk, muted = LavMuted,
        border = null,
        softShadow = 14.dp, shadowColor = LavViolet.copy(alpha = 0.18f), hardShadow = 0.dp,
        heroBright = Brush.linearGradient(listOf(Color(0xFF5B3DF0), Color(0xFF8E6BFF), Color(0xFFC3B2FF))),
        onHeroBright = Color.White,
        alert = Color(0xFF241A55), onAlert = Color.White,
        warn = Color(0xFFFFE6DE), onWarn = Color(0xFF7A2A14),
        buttonShape = RoundedCornerShape(50),
        button = Brush.horizontalGradient(listOf(LavViolet, Color(0xFF8E6BFF))),
        onButton = Color.White,
        tonal = Color(0xFFECE7FF), onTonal = LavViolet,
        enter = EnterKind.FadeUp,
        springPress = true, shimmer = false, borderBeam = false, segmentRing = true,
        uppercaseLabels = false,
    )

    AlbaStyle.Retro -> StyleSpec(
        style = style,
        dark = false,
        scheme = lightColorScheme(
            primary = RetroInk, onPrimary = RetroYellow,
            primaryContainer = RetroYellow, onPrimaryContainer = RetroInk,
            secondary = RetroInk, onSecondary = Color.White,
            secondaryContainer = RetroYellow, onSecondaryContainer = RetroInk,
            tertiary = RetroMint,
            background = RetroCream, onBackground = RetroInk,
            surface = RetroCream, onSurface = RetroInk,
            surfaceVariant = Color.White, onSurfaceVariant = Color(0xFF4A4A4A),
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White,
            surfaceContainer = Color.White, surfaceContainerHigh = Color.White,
            surfaceContainerHighest = Color.White,
            outline = RetroInk, outlineVariant = RetroInk,
            error = Color(0xFFD64545),
        ),
        heading = ArchivoBlack, body = SpaceGrotesk,
        background = RetroCream,
        cloudyBackground = Color(0xFFC4BCAE),
        savedBackground = Color(0xFFEDE3CF),
        shape = RoundedCornerShape(10.dp),
        card = Color.White, onCard = RetroInk, muted = Color(0xFF4A4A4A),
        border = BorderStroke(2.5.dp, RetroInk),
        softShadow = 0.dp, shadowColor = Color.Transparent, hardShadow = 5.dp,
        heroBright = Brush.linearGradient(listOf(RetroYellow, RetroYellow)),
        onHeroBright = RetroInk,
        alert = RetroCoral, onAlert = RetroInk,
        warn = RetroMint, onWarn = RetroInk,
        buttonShape = RoundedCornerShape(8.dp),
        button = Brush.linearGradient(listOf(RetroYellow, RetroYellow)),
        onButton = RetroInk,
        tonal = Color.White, onTonal = RetroInk,
        enter = EnterKind.Pop,
        springPress = false, shimmer = false, borderBeam = false, segmentRing = false,
        uppercaseLabels = true,
    )
}

val LocalAlbaStyle = staticCompositionLocalOf { specFor(AlbaStyle.Aurora) }

/** Tipografía de títulos del estilo actual (sustituye a la serif de siempre). */
object AlbaType {
    val heading: FontFamily
        @Composable @ReadOnlyComposable get() = LocalAlbaStyle.current.heading
}

/** La tipografía de Material con la letra del estilo. */
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

/** Etiquetas: en Retro, en mayúsculas. */
@Composable
@ReadOnlyComposable
fun label(text: String): String = if (LocalAlbaStyle.current.uppercaseLabels) text.uppercase(Lang.locale) else text

// ============================================================================
// Fondo
// ============================================================================

/**
 * Fondo de la pantalla principal.
 * Aurora: manchas de luz que se mueven despacio (Magic UI «Aurora» / «Particles»).
 * Lavanda: un resplandor violeta arriba.
 * Retro: papel con puntos (Magic UI «Dot Pattern»).
 */
@Composable
fun StyleBackground(cloud: Float, saved: Boolean, content: @Composable BoxScope.() -> Unit) {
    val s = LocalAlbaStyle.current
    val base by androidx.compose.animation.animateColorAsState(s.moodBackground(cloud, saved), tween(1200), label = "bg")
    val glow = 1f - cloud.coerceIn(0f, 1f) * 0.85f
    Box(
        Modifier
            .fillMaxSize()
            .background(base),
    ) {
        when (s.style) {
            AlbaStyle.Aurora -> AuroraBlobs(glow)
            AlbaStyle.Lavanda -> Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(LavVioletLight.copy(alpha = 0.35f * glow), Color.Transparent),
                        center = Offset(size.width * 0.85f, 0f),
                        radius = size.width * 0.9f,
                    ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * 0.85f, 0f),
                )
            }
            AlbaStyle.Retro -> Canvas(Modifier.fillMaxSize()) {
                val step = 22.dp.toPx()
                val r = 1.3.dp.toPx()
                var y = step / 2
                while (y < size.height) {
                    var x = step / 2
                    while (x < size.width) {
                        drawCircle(RetroInk.copy(alpha = 0.12f), r, Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }
        }
        content()
    }
}

@Composable
private fun AuroraBlobs(glow: Float) {
    val inf = rememberInfiniteTransition(label = "aurora")
    val t by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "t",
    )
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        fun blob(color: Color, cx: Float, cy: Float, r: Float) = drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = color.alpha * glow), Color.Transparent), Offset(cx, cy), r),
            radius = r, center = Offset(cx, cy),
        )
        blob(AlbaColors.Alba.copy(alpha = 0.28f), w * (0.15f + 0.25f * t), h * (0.10f + 0.06f * t), w * 0.75f)
        blob(Color(0xFF6C4CF1).copy(alpha = 0.30f), w * (0.95f - 0.30f * t), h * (0.22f + 0.10f * t), w * 0.80f)
        blob(AlbaColors.Sol.copy(alpha = 0.14f), w * (0.40f + 0.20f * t), h * (0.62f - 0.08f * t), w * 0.70f)
    }
}

// ============================================================================
// Tarjeta y botón
// ============================================================================

/** Sombra dura desplazada (NeoBrutalism). */
private fun Modifier.hardShadow(offset: Dp, shape: Shape, color: Color = RetroInk): Modifier =
    if (offset <= 0.dp) this else drawBehind {
        val o = offset.toPx()
        translate(o, o) { drawOutline(shape.createOutline(size, layoutDirection, this), color) }
    }

/**
 * La tarjeta de Alba según el estilo: cristal (Aurora), blanca con sombra suave (Lavanda)
 * o con borde negro y sombra dura (Retro). Si se puede tocar, responde con un rebote
 * (Aurora, Lavanda) o «hundiéndose» sobre su sombra (Retro).
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
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && s.springPress && onClick != null) 0.97f else 1f,
        spring(dampingRatio = 0.45f, stiffness = 500f), label = "press",
    )
    val sink by animateDpAsState(
        if (pressed && s.hardShadow > 0.dp && onClick != null) s.hardShadow else 0.dp,
        spring(stiffness = 900f), label = "sink",
    )
    var m = modifier
        .padding(end = s.hardShadow, bottom = s.hardShadow)
        .hardShadow(s.hardShadow, s.shape)
        .offset(sink, sink)
        .graphicsLayer { scaleX = scale; scaleY = scale }
    if (s.softShadow > 0.dp) m = m.shadow(s.softShadow, s.shape, ambientColor = s.shadowColor, spotColor = s.shadowColor)
    m = m.clip(s.shape)
    m = if (brush != null) m.background(brush) else m.background(container ?: s.card)
    s.border?.let { m = m.border(it, s.shape) }
    if (onClick != null) {
        m = m.clickable(interaction, if (s.hardShadow > 0.dp) null else LocalIndication.current, onClick = onClick)
    }
    CompositionLocalProvider(LocalContentColor provides (contentColor ?: s.onCard)) {
        Box(m, content = content)
    }
}

/**
 * Botón de Alba. Aurora: degradado dorado con un brillo que lo recorre (Magic UI «Shimmer Button»).
 * Lavanda: píldora violeta. Retro: bloque amarillo con borde y sombra dura.
 * [tonal]: versión discreta para acciones secundarias.
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
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && s.springPress) 0.95f else 1f,
        spring(dampingRatio = 0.4f, stiffness = 600f), label = "btn",
    )
    val shadow = if (s.hardShadow > 0.dp) 3.dp else 0.dp
    val sink by animateDpAsState(if (pressed && shadow > 0.dp) shadow else 0.dp, spring(stiffness = 900f), label = "btnSink")
    val shimmerX = if (s.shimmer && !tonal && enabled) {
        val inf = rememberInfiniteTransition(label = "shimmer")
        inf.animateFloat(
            -0.4f, 1.4f,
            infiniteRepeatable(tween(2600, easing = LinearEasing)),
            label = "sx",
        ).value
    } else null

    var m = modifier
        .padding(end = shadow, bottom = shadow)
        .hardShadow(shadow, s.buttonShape)
        .offset(sink, sink)
        .graphicsLayer {
            scaleX = scale; scaleY = scale
            alpha = if (enabled) 1f else 0.45f
        }
        .clip(s.buttonShape)
    m = if (tonal) m.background(s.tonal) else m.background(s.button)
    if (shimmerX != null) {
        m = m.drawWithContent {
            drawContent()
            val x = size.width * shimmerX
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                    start = Offset(x - size.width * 0.25f, 0f),
                    end = Offset(x + size.width * 0.25f, size.height),
                ),
            )
        }
    }
    if (s.border != null && s.hardShadow > 0.dp) m = m.border(s.border, s.buttonShape)
    else if (tonal && s.border != null) m = m.border(s.border, s.buttonShape)
    m = m.clickable(interaction, if (s.hardShadow > 0.dp) null else LocalIndication.current, enabled = enabled, onClick = onClick)
        .padding(padding)

    Box(m) {
        Text(
            label(text),
            color = if (tonal) s.onTonal else s.onButton,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            fontWeight = if (s.style == AlbaStyle.Retro) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

// ============================================================================
// Movimiento
// ============================================================================

/** Las piezas que ya han aparecido no vuelven a animarse al hacer scroll. */
val LocalRevealed = staticCompositionLocalOf<MutableSet<String>> { mutableSetOf() }

/**
 * Aparición escalonada: con desenfoque (Magic UI «Blur Fade»), subiendo (Lavanda)
 * o con un pequeño salto (Retro).
 */
fun Modifier.reveal(key: String, index: Int): Modifier = composed {
    val s = LocalAlbaStyle.current
    val revealed = LocalRevealed.current
    val already = remember(key) { key in revealed }
    val p = remember(key) { Animatable(if (already) 1f else 0f) }
    LaunchedEffect(key) {
        if (!already) {
            delay(index.coerceAtMost(8) * 70L)
            when (s.enter) {
                EnterKind.Pop -> p.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 380f))
                else -> p.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            }
            revealed.add(key)
        }
    }
    val v = p.value
    val base = this.graphicsLayer {
        alpha = v.coerceIn(0f, 1f)
        when (s.enter) {
            EnterKind.Pop -> {
                val sc = 0.9f + 0.1f * v
                scaleX = sc; scaleY = sc
            }
            else -> translationY = (1f - v) * 22.dp.toPx()
        }
    }
    if (s.enter == EnterKind.BlurFade && v < 0.99f) base.blur(((1f - v) * 10f).dp, BlurredEdgeTreatment.Unbounded) else base
}

/** Número que cuenta hasta su valor (Magic UI «Number Ticker»). */
@Composable
fun tickerValue(target: Int): Int {
    var shown by remember { mutableIntStateOf(0) }
    LaunchedEffect(target) { shown = target }
    val v by animateIntAsState(shown, tween(900, easing = FastOutSlowInEasing), label = "ticker")
    return v
}

/** Borde de luz que da vueltas (Magic UI «Border Beam»). */
fun Modifier.borderBeam(shape: Shape, colors: List<Color> = listOf(AlbaColors.Sol, AlbaColors.Alba)): Modifier = composed {
    val inf = rememberInfiniteTransition(label = "beam")
    val angle by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "angle")
    drawWithContent {
        drawContent()
        val outline = shape.createOutline(size, layoutDirection, this)
        val c = Offset(size.width / 2f, size.height / 2f)
        val brush = object : ShaderBrush() {
            override fun createShader(size: Size): Shader {
                val shader = SweepGradientShader(
                    c,
                    listOf(Color.Transparent, Color.Transparent, colors[0], colors.getOrElse(1) { colors[0] }, Color.Transparent),
                    listOf(0f, 0.70f, 0.85f, 0.95f, 1f),
                )
                shader.setLocalMatrix(android.graphics.Matrix().apply { setRotate(angle, c.x, c.y) })
                return shader
            }
        }
        drawOutline(outline, brush, style = Stroke(width = 2.dp.toPx()))
    }
}

/**
 * Anillo de la semana (estilo Anime.js): siete arcos alrededor del número que se dibujan
 * al aparecer, con marcas finas que giran despacio.
 */
@Composable
fun SegmentRing(week: List<DayStatus>, modifier: Modifier, ink: Color) {
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) { draw.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
    val inf = rememberInfiniteTransition(label = "ring")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing)), label = "spin")
    Canvas(modifier) {
        val stroke = size.minDimension * 0.045f
        val r = size.minDimension / 2f - stroke
        val c = center
        // Marcas
        val ticks = 60
        for (i in 0 until ticks) {
            val a = Math.toRadians((i * 360.0 / ticks) + spin)
            val inner = r - stroke * 1.8f
            val outer = r - stroke * (if (i % 5 == 0) 1.0f else 1.3f)
            drawLine(
                ink.copy(alpha = 0.25f),
                Offset(c.x + (inner * kotlin.math.cos(a)).toFloat(), c.y + (inner * kotlin.math.sin(a)).toFloat()),
                Offset(c.x + (outer * kotlin.math.cos(a)).toFloat(), c.y + (outer * kotlin.math.sin(a)).toFloat()),
                strokeWidth = 1.2.dp.toPx(),
            )
        }
        // Siete días
        val gap = 6f
        val sweep = 360f / 7f - gap
        week.forEachIndexed { i, day ->
            val color = when {
                day.ok == true && day.saved -> ink.copy(alpha = 0.55f)
                day.ok == true -> AlbaColors.Sol
                day.ok == false -> ink.copy(alpha = 0.18f)
                day.isToday -> AlbaColors.Alba
                else -> ink.copy(alpha = 0.10f)
            }
            val start = -90f + i * (sweep + gap) + gap / 2
            val p = (draw.value * 7f - i).coerceIn(0f, 1f)
            if (p > 0f) {
                drawArc(
                    color, start, sweep * p, useCenter = false,
                    topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
    }
}

/** Esquina redondeada sencilla para dibujos. */
internal fun cornerOf(dp: Float) = CornerRadius(dp, dp)
