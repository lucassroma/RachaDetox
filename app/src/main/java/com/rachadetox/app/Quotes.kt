package com.rachadetox.app

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Quote(val text: String, val author: String)

/** Frases que aparecen al abrir la app, una al azar cada vez. */
val QUOTES = listOf(
    Quote("¿Qué es el hombre sin libertad? ¿Sin esa luz armoniosa y fija que se siente por dentro?", "Federico García Lorca"),
    Quote("Aquellos que no son capaces de sacrificar algo, nunca podrán cambiar nada.", "Armin, Attack on Titan"),
    Quote("El hombre nace libre, pero en todas partes está encadenado.", "Jean-Jacques Rousseau"),
    Quote("El hombre está condenado a ser libre.", "Jean-Paul Sartre"),
    Quote("No hay barrera, cerradura ni cerrojo que puedas imponer a la libertad de mi mente.", "Virginia Woolf"),
    Quote("No es que tengamos poco tiempo, sino que perdemos mucho.", "Séneca"),
    Quote("Mientras aplazamos, la vida pasa.", "Séneca"),
    Quote("Toda la desgracia de los hombres viene de una sola cosa: no saber quedarse tranquilos en una habitación.", "Blaise Pascal"),
    Quote("Nadie es más esclavo que quien se tiene por libre sin serlo.", "Goethe"),
    Quote("Ningún hombre es libre si no es dueño de sí mismo.", "Epicteto"),
    Quote("La atención es la forma más rara y pura de generosidad.", "Simone Weil"),
    Quote("Caminante, no hay camino, se hace camino al andar.", "Antonio Machado"),
    Quote("Cómo pasamos nuestros días es, por supuesto, cómo pasamos nuestras vidas.", "Annie Dillard"),
)

/**
 * Pantalla de entrada: escribe una frase letra a letra, como en una máquina de escribir,
 * y después pasa a la app. Tocando la pantalla se salta.
 */
@Composable
fun QuoteSplash(onDone: () -> Unit) {
    val quote = remember { QUOTES.random() }
    var typed by remember { mutableIntStateOf(0) }
    var cursorOn by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    val authorAlpha by animateFloatAsState(if (finished) 1f else 0f, tween(700), label = "author")

    LaunchedEffect(Unit) {
        launch {
            while (true) {
                delay(480)
                cursorOn = !cursorOn
            }
        }
        delay(500)
        for (i in 1..quote.text.length) {
            typed = i
            val c = quote.text[i - 1]
            delay(
                when (c) {
                    ',', ';', ':' -> 220L
                    '.', '?', '!' -> 360L
                    ' ' -> 55L
                    else -> 42L
                }
            )
        }
        finished = true
        delay(2200)
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(AlbaColors.Noche)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onDone() }
            .systemBarsPadding()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
            Image(
                painter = painterResource(R.drawable.ic_sun),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .alpha(0.9f),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                buildAnnotatedString {
                    append(quote.text.take(typed))
                    withStyle(SpanStyle(color = if (cursorOn) AlbaColors.Alba else AlbaColors.Noche)) {
                        append("▌")
                    }
                },
                color = AlbaColors.Arena,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                lineHeight = 30.sp,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "— ${quote.author}",
                color = AlbaColors.Alba,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.alpha(authorAlpha),
            )
        }
    }
}
