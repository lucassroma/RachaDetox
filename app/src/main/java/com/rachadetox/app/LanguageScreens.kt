package com.rachadetox.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Primera pantalla al instalar: elegir idioma. */
@Composable
fun LanguagePicker(onPick: (AppLang) -> Unit) {
    val suggested = Lang.systemDefault()
    Column(
        Modifier
            .fillMaxSize()
            .background(AlbaColors.Noche)
            .systemBarsPadding()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(R.drawable.ic_sun), contentDescription = null, modifier = Modifier.size(96.dp))
        Text("alba", color = AlbaColors.Arena, fontFamily = AlbaType.heading, fontSize = 40.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            "Elige tu idioma\nChoose your language\nScegli la lingua",
            color = AlbaColors.Arena.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
            fontSize = 17.sp,
        )
        Spacer(Modifier.height(28.dp))
        AppLang.entries.forEach { lang ->
            val main = lang == suggested
            if (main) {
                Button(
                    onClick = { onPick(lang) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AlbaColors.Sol, contentColor = AlbaColors.Noche),
                ) { Text(lang.label, fontSize = 18.sp) }
            } else {
                OutlinedButton(
                    onClick = { onPick(lang) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlbaColors.Arena),
                ) { Text(lang.label, fontSize = 18.sp) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun LanguageDialog(onDismiss: () -> Unit, onPick: (AppLang) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Idioma", "Language", "Lingua"), fontFamily = AlbaType.heading) },
        text = {
            Column {
                AppLang.entries.forEach { lang ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPick(lang) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = Lang.current == lang, onClick = { onPick(lang) })
                        Spacer(Modifier.width(8.dp))
                        Text(lang.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}
