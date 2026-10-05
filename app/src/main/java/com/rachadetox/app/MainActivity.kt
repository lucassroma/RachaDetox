package com.rachadetox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    /** Cambia cada vez que vuelves a la app (p. ej. tras conceder un permiso en Ajustes). */
    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlbaTheme {
                RachaApp(resumeTick = resumeTick.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }
}

/** Paleta "amanecer" de Alba (dibujos y animaciones). */
object AlbaColors {
    val Noche = Color(0xFF1E2140)
    val NocheClara = Color(0xFF2B2F55)
    val Bruma = Color(0xFF5B5478)
    val Alba = Color(0xFFF2A48A)
    val Sol = Color(0xFFF6C667)
    val Salvia = Color(0xFF8DB9A0)
    val Arena = Color(0xFFF7F1E8)
    val ArenaOscura = Color(0xFFEDE4D6)
}

/**
 * Tiñe el fondo según cómo va el día:
 * - [cloud] de 0 a 1: cuánto te has pasado sin salvar (1 = 15 minutos, racha perdida).
 * - [saved]: hoy la racha está salvada (gris muy claro).
 * Al día siguiente vuelve solo a la normalidad, porque se calcula con los datos de hoy.
 * Cada estilo tiene sus propios tonos (ver [StyleSpec.moodBackground]).
 */
@Composable
fun MoodTheme(cloud: Float, saved: Boolean, content: @Composable () -> Unit) {
    val spec = LocalAlbaStyle.current
    val background by animateColorAsState(spec.moodBackground(cloud, saved), tween(1200), label = "dayMood")
    val scheme = MaterialTheme.colorScheme.copy(background = background, surface = background)
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes, content = content)
}

/** Tema de Alba: cristal líquido con naranja, blanco y gris (ver Style.kt). */
@Composable
fun AlbaTheme(content: @Composable () -> Unit) = AlbaStyleProvider(content)
