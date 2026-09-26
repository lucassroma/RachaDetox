package com.rachadetox.app

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

private const val REPO_URL = "https://github.com/lucassroma/RachaDetox"

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    fun open(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TextButton(onClick = onBack) { Text("← Volver") }
            Text("Tus datos son tuyos", style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)

            // El compromiso
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AlbaColors.Noche, contentColor = AlbaColors.Arena),
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Alba no vende, no comparte y no envía tus datos a nadie. Nunca.",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Serif,
                    )
                    Text(
                        "Y no es solo una promesa: Alba no tiene permiso para conectarse a Internet. " +
                            "Aunque quisiera, no podría mandar tus datos a ningún sitio.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text("— El equipo de Alba", style = MaterialTheme.typography.labelLarge, color = AlbaColors.Sol)
                }
            }

            PrivacyBlock(
                "Compruébalo tú",
                listOf(
                    "En Ajustes → App → Alba, el uso de datos móviles y de Wi-Fi es 0 B.",
                    "Pon el móvil en modo avión: Alba funciona exactamente igual.",
                    "El código es público. Cualquiera puede leerlo y ver que no hay Internet, anuncios ni analíticas.",
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)))
                }) { Text("Ver en Ajustes") }
                OutlinedButton(onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))) }) {
                    Text("Ver el código")
                }
            }

            PrivacyBlock(
                "Qué guarda Alba",
                listOf(
                    "Las apps que eliges y sus límites.",
                    "Tu racha y qué días la cumpliste.",
                    "Qué apps has bloqueado hoy.",
                    "Todo se queda en tu móvil. Si desinstalas Alba, se borra.",
                ),
            )

            PrivacyBlock(
                "Qué lee Alba y para qué",
                listOf(
                    "Tiempo de uso de las apps (permiso «Acceso a datos de uso»): para tu racha, tus avisos y tu perfil.",
                    "Qué app se abre (permiso de Accesibilidad, solo si activas el bloqueo): para cerrar las apps bloqueadas. No lee lo que hay en tu pantalla.",
                ),
            )

            PrivacyBlock(
                "Qué no hay",
                listOf("Ni cuentas.", "Ni anuncios.", "Ni analíticas ni rastreadores.", "Ni servidores."),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrivacyBlock(title: String, lines: List<String>) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif)
            lines.forEach { line ->
                Row {
                    Text("·  ", color = AlbaColors.Alba, style = MaterialTheme.typography.bodyLarge)
                    Text(line, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** Tarjeta pequeña de la pantalla principal que lleva a la sección de privacidad. */
@Composable
fun PrivacyEntryCard(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(
            "Tus datos no se venden. Alba ni siquiera tiene acceso a Internet →",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
