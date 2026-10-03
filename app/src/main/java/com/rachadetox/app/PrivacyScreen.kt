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
            TextButton(onClick = onBack) { Text(tr("← Volver", "← Back", "← Indietro")) }
            Text(tr("Tus datos son tuyos", "Your data is yours", "I tuoi dati sono tuoi"), style = MaterialTheme.typography.headlineMedium, fontFamily = AlbaType.heading)

            // El compromiso
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AlbaColors.Noche, contentColor = AlbaColors.Arena),
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        tr(
                            "Alba no vende, no comparte y no envía tus datos a nadie. Nunca.",
                            "Alba doesn't sell, share or send your data to anyone. Ever.",
                            "Alba non vende, non condivide e non invia i tuoi dati a nessuno. Mai.",
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = AlbaType.heading,
                    )
                    Text(
                        tr(
                            "Y no es solo una promesa: Alba no tiene permiso para conectarse a Internet. " +
                                "Aunque quisiera, no podría mandar tus datos a ningún sitio.",
                            "And it's not just a promise: Alba has no permission to connect to the Internet. " +
                                "Even if it wanted to, it couldn't send your data anywhere.",
                            "E non è solo una promessa: Alba non ha l'autorizzazione per connettersi a Internet. " +
                                "Anche volendo, non potrebbe inviare i tuoi dati da nessuna parte.",
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(tr("— El equipo de Alba", "— The Alba team", "— Il team di Alba"), style = MaterialTheme.typography.labelLarge, color = AlbaColors.Sol)
                }
            }

            PrivacyBlock(
                tr("Compruébalo tú", "Check it yourself", "Verificalo tu"),
                listOf(
                    tr(
                        "En Ajustes → App → Alba, el uso de datos móviles y de Wi-Fi es 0 B.",
                        "In Settings → Apps → Alba, mobile and Wi-Fi data usage is 0 B.",
                        "In Impostazioni → App → Alba, l'uso di dati mobili e Wi-Fi è 0 B.",
                    ),
                    tr(
                        "Pon el móvil en modo avión: Alba funciona exactamente igual.",
                        "Put your phone in airplane mode: Alba works exactly the same.",
                        "Metti il telefono in modalità aereo: Alba funziona esattamente allo stesso modo.",
                    ),
                    tr(
                        "El código es público. Cualquiera puede leerlo y ver que no hay Internet, anuncios ni analíticas.",
                        "The code is public. Anyone can read it and see there's no Internet, ads or analytics.",
                        "Il codice è pubblico. Chiunque può leggerlo e vedere che non ci sono Internet, pubblicità né analisi.",
                    ),
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName)))
                }) { Text(tr("Ver en Ajustes", "See in Settings", "Vedi in Impostazioni")) }
                OutlinedButton(onClick = { open(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))) }) {
                    Text(tr("Ver el código", "See the code", "Vedi il codice"))
                }
            }

            PrivacyBlock(
                tr("Qué guarda Alba", "What Alba stores", "Cosa salva Alba"),
                listOf(
                    tr("Las apps que eliges y sus límites.", "The apps you choose and their limits.", "Le app che scegli e i loro limiti."),
                    tr("Tu racha y qué días la cumpliste.", "Your streak and which days you kept it.", "La tua serie e in quali giorni l'hai rispettata."),
                    tr("Qué apps has bloqueado hoy.", "Which apps you've blocked today.", "Quali app hai bloccato oggi."),
                    tr(
                        "Todo se queda en tu móvil. Si desinstalas Alba, se borra.",
                        "Everything stays on your phone. If you uninstall Alba, it's deleted.",
                        "Tutto resta sul tuo telefono. Se disinstalli Alba, viene cancellato.",
                    ),
                ),
            )

            PrivacyBlock(
                tr("Qué lee Alba y para qué", "What Alba reads and why", "Cosa legge Alba e perché"),
                listOf(
                    tr(
                        "Tiempo de uso de las apps (permiso «Acceso a datos de uso»): para tu racha, tus avisos y tu perfil.",
                        "App usage time («Usage access» permission): for your streak, your reminders and your profile.",
                        "Tempo di utilizzo delle app (autorizzazione «Accesso ai dati di utilizzo»): per la tua serie, i tuoi avvisi e il tuo profilo.",
                    ),
                    tr(
                        "Qué app se abre (permiso de Accesibilidad, solo si activas el bloqueo): para cerrar las apps bloqueadas. No lee lo que hay en tu pantalla.",
                        "Which app opens (Accessibility permission, only if you turn on blocking): to close blocked apps. It doesn't read what's on your screen.",
                        "Quale app si apre (autorizzazione Accessibilità, solo se attivi il blocco): per chiudere le app bloccate. Non legge cosa c'è sullo schermo.",
                    ),
                ),
            )

            PrivacyBlock(
                tr("Qué no hay", "What there isn't", "Cosa non c'è"),
                listOf(
                    tr("Ni cuentas.", "No accounts.", "Nessun account."),
                    tr("Ni anuncios.", "No ads.", "Nessuna pubblicità."),
                    tr("Ni analíticas ni rastreadores.", "No analytics or trackers.", "Nessuna analisi né tracciamento."),
                    tr("Ni servidores.", "No servers.", "Nessun server."),
                ),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PrivacyBlock(title: String, lines: List<String>) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontFamily = AlbaType.heading)
            lines.forEach { line ->
                Row {
                    Text("·  ", color = AlbaColors.Alba, style = MaterialTheme.typography.bodyLarge)
                    Text(line, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

