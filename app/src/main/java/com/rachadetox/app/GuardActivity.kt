package com.rachadetox.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Día de prueba tras perder la racha: aparece al abrir una de las apps que se cerraron ayer.
 * Si sigues, ya no podrás recuperar la racha.
 */
class GuardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val pkg = intent.getStringExtra(EXTRA_PKG)
        val label = pkg?.let {
            try {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(it, 0)).toString()
            } catch (_: Exception) {
                null
            }
        } ?: tr("esta app", "this app", "questa app")
        val streak = Store(this).recovery()?.streak ?: 0

        setContent {
            AlbaTheme {
                BackHandler { goHome() }
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(AlbaColors.Noche)
                        .systemBarsPadding()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LockDayIcon(Modifier.size(110.dp))
                    Spacer(Modifier.height(20.dp))
                    Text(
                        tr("¿Seguro que quieres seguir?", "Are you sure you want to continue?", "Sei sicuro di voler continuare?"),
                        color = AlbaColors.Arena,
                        fontFamily = FontFamily.Serif,
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        tr(
                            "No podrás recuperar tu racha de $streak ${dias(streak)}.\nSi hoy no abres $label, a medianoche vuelve.",
                            "You won't be able to get back your streak of $streak ${dias(streak)}.\nIf you don't open $label today, it comes back at midnight.",
                            "Non potrai recuperare la tua serie di $streak ${dias(streak)}.\nSe oggi non apri $label, a mezzanotte torna.",
                        ),
                        color = AlbaColors.Arena.copy(alpha = 0.85f),
                        fontSize = 18.sp,
                        lineHeight = 27.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(36.dp))
                    Button(
                        onClick = { goHome() },
                        colors = ButtonDefaults.buttonColors(containerColor = AlbaColors.Sol, contentColor = AlbaColors.Noche),
                    ) { Text(tr("No, quiero mi racha", "No, I want my streak", "No, voglio la mia serie")) }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { continueTo(pkg) }) {
                        Text(
                            tr("Seguir y perder la racha", "Continue and lose the streak", "Continua e perdi la serie"),
                            color = AlbaColors.Arena.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        recreate()
    }

    private fun continueTo(pkg: String?) {
        Store(this).forfeitRecovery()
        pkg?.let { packageManager.getLaunchIntentForPackage(it) }?.let {
            startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        finish()
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        finish()
    }

    companion object {
        const val EXTRA_PKG = "pkg"
    }
}
