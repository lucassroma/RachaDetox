package com.rachadetox.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

/** Paleta "amanecer" de Alba. */
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

@Composable
fun AlbaTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = AlbaColors.Sol,
            onPrimary = AlbaColors.Noche,
            primaryContainer = AlbaColors.Sol,
            onPrimaryContainer = AlbaColors.Noche,
            secondary = AlbaColors.Alba,
            onSecondary = AlbaColors.Noche,
            secondaryContainer = AlbaColors.Bruma,
            onSecondaryContainer = AlbaColors.Arena,
            tertiary = AlbaColors.Salvia,
            background = AlbaColors.Noche,
            onBackground = AlbaColors.Arena,
            surface = AlbaColors.Noche,
            onSurface = AlbaColors.Arena,
            surfaceVariant = AlbaColors.NocheClara,
            onSurfaceVariant = Color(0xFFC9C2D8),
            surfaceContainerLowest = AlbaColors.Noche,
            surfaceContainerLow = AlbaColors.NocheClara,
            surfaceContainer = AlbaColors.NocheClara,
            surfaceContainerHigh = AlbaColors.NocheClara,
            surfaceContainerHighest = AlbaColors.NocheClara,
            outline = AlbaColors.Bruma,
            outlineVariant = AlbaColors.Bruma,
            error = AlbaColors.Alba,
        )
    } else {
        lightColorScheme(
            primary = AlbaColors.Noche,
            onPrimary = AlbaColors.Arena,
            primaryContainer = AlbaColors.Sol,
            onPrimaryContainer = AlbaColors.Noche,
            secondary = AlbaColors.Bruma,
            onSecondary = AlbaColors.Arena,
            secondaryContainer = AlbaColors.Alba,
            onSecondaryContainer = AlbaColors.Noche,
            tertiary = AlbaColors.Salvia,
            background = AlbaColors.Arena,
            onBackground = AlbaColors.Noche,
            surface = AlbaColors.Arena,
            onSurface = AlbaColors.Noche,
            surfaceVariant = AlbaColors.ArenaOscura,
            onSurfaceVariant = AlbaColors.Bruma,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = AlbaColors.ArenaOscura,
            surfaceContainer = AlbaColors.ArenaOscura,
            surfaceContainerHigh = AlbaColors.ArenaOscura,
            surfaceContainerHighest = AlbaColors.ArenaOscura,
            outline = AlbaColors.Bruma,
            outlineVariant = Color(0xFFD9CFC0),
            error = Color(0xFFB5654A),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
