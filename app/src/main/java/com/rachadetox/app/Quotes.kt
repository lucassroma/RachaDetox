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

data class Quote(val es: String, val en: String, val it: String, val author: String) {
    val text: String get() = tr(es, en, it)
}

/** Frases que aparecen al abrir la app, una al azar cada vez. */
val QUOTES = listOf(
    Quote(
        "¿Qué es el hombre sin libertad? ¿Sin esa luz armoniosa y fija que se siente por dentro?",
        "What is man without freedom? Without that harmonious, steady light we feel within?",
        "Che cos'è l'uomo senza libertà? Senza quella luce armoniosa e ferma che si sente dentro?",
        "Federico García Lorca",
    ),
    Quote(
        "Aquellos que no son capaces de sacrificar algo, nunca podrán cambiar nada.",
        "Someone who can't sacrifice anything can never change anything.",
        "Chi non è capace di sacrificare qualcosa non potrà mai cambiare nulla.",
        "Armin, Attack on Titan",
    ),
    Quote(
        "El hombre nace libre, pero en todas partes está encadenado.",
        "Man is born free, and everywhere he is in chains.",
        "L'uomo è nato libero, ma ovunque è in catene.",
        "Jean-Jacques Rousseau",
    ),
    Quote(
        "El hombre está condenado a ser libre.",
        "Man is condemned to be free.",
        "L'uomo è condannato a essere libero.",
        "Jean-Paul Sartre",
    ),
    Quote(
        "No hay barrera, cerradura ni cerrojo que puedas imponer a la libertad de mi mente.",
        "There is no gate, no lock, no bolt that you can set upon the freedom of my mind.",
        "Non c'è cancello, serratura o chiavistello che tu possa imporre alla libertà della mia mente.",
        "Virginia Woolf",
    ),
    Quote(
        "No es que tengamos poco tiempo, sino que perdemos mucho.",
        "It is not that we have a short time to live, but that we waste a lot of it.",
        "Non è che abbiamo poco tempo, è che ne perdiamo molto.",
        "Séneca",
    ),
    Quote(
        "Mientras aplazamos, la vida pasa.",
        "While we put things off, life speeds by.",
        "Mentre rimandiamo, la vita passa.",
        "Séneca",
    ),
    Quote(
        "Toda la desgracia de los hombres viene de una sola cosa: no saber quedarse tranquilos en una habitación.",
        "All of humanity's problems stem from man's inability to sit quietly in a room alone.",
        "Tutta l'infelicità degli uomini deriva da una sola cosa: non saper restare tranquilli in una stanza.",
        "Blaise Pascal",
    ),
    Quote(
        "Nadie es más esclavo que quien se tiene por libre sin serlo.",
        "None are more hopelessly enslaved than those who falsely believe they are free.",
        "Nessuno è più schiavo di chi si crede libero senza esserlo.",
        "Goethe",
    ),
    Quote(
        "Ningún hombre es libre si no es dueño de sí mismo.",
        "No man is free who is not master of himself.",
        "Nessun uomo è libero se non è padrone di se stesso.",
        "Epicteto",
    ),
    Quote(
        "La atención es la forma más rara y pura de generosidad.",
        "Attention is the rarest and purest form of generosity.",
        "L'attenzione è la forma più rara e più pura di generosità.",
        "Simone Weil",
    ),
    Quote(
        "Caminante, no hay camino, se hace camino al andar.",
        "Traveler, there is no road; the road is made by walking.",
        "Viandante, non c'è cammino, il cammino si fa andando.",
        "Antonio Machado",
    ),
    Quote(
        "Cómo pasamos nuestros días es, por supuesto, cómo pasamos nuestras vidas.",
        "How we spend our days is, of course, how we spend our lives.",
        "Come trascorriamo i nostri giorni è, naturalmente, come trascorriamo la nostra vita.",
        "Annie Dillard",
    ),
)

private fun authorName(author: String): String = when (author) {
    "Séneca" -> tr("Séneca", "Seneca", "Seneca")
    "Epicteto" -> tr("Epicteto", "Epictetus", "Epitteto")
    else -> author
}

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
                "— ${authorName(quote.author)}",
                color = AlbaColors.Alba,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.alpha(authorAlpha),
            )
        }
    }
}
