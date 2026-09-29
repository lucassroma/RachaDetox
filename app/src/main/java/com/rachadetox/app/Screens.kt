package com.rachadetox.app

import java.time.LocalDate
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.Switch
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.Canvas
import android.net.Uri
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val Green = AlbaColors.Salvia
private val Amber = AlbaColors.Alba
private val Muted = AlbaColors.Bruma

// ---------------------------------------------------------------- raíz

@Composable
fun RachaApp(resumeTick: Int) {
    val context = LocalContext.current
    var langChosen by remember { mutableStateOf(Lang.hasChosen) }
    if (!langChosen) {
        LanguagePicker { lang ->
            Lang.set(context, lang)
            langChosen = true
        }
        return
    }
    var showQuote by rememberSaveable { mutableStateOf(true) }
    if (showQuote) {
        QuoteSplash(onDone = { showQuote = false })
        return
    }
    val hasUsage = remember(resumeTick) { UsageTracker.hasPermission(context) }
    if (hasUsage) MainScreen(resumeTick) else PermissionScreen()
}

// ---------------------------------------------------------------- permiso

@Composable
fun PermissionScreen() {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.ic_sun), contentDescription = null, modifier = Modifier.size(96.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                "alba",
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                tr(
                    "Para saber cuánto tiempo pasas en cada app necesito el permiso «Acceso a datos de uso».\n\n" +
                        "Busca Alba en la lista, actívalo y vuelve aquí.",
                    "To know how much time you spend in each app, I need the «Usage access» permission.\n\n" +
                        "Find Alba in the list, turn it on and come back here.",
                    "Per sapere quanto tempo passi in ogni app mi serve l'autorizzazione «Accesso ai dati di utilizzo».\n\n" +
                        "Cerca Alba nell'elenco, attivala e torna qui.",
                ),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                openSettings(context, Settings.ACTION_USAGE_ACCESS_SETTINGS)
            }) {
                Text(tr("Dar permiso", "Grant permission", "Concedi autorizzazione"))
            }
        }
    }
}

private fun openSettings(context: Context, action: String) {
    try {
        context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

// ---------------------------------------------------------------- pantalla principal

@Composable
fun MainScreen(resumeTick: Int) {
    val context = LocalContext.current
    val store = remember { Store(context) }
    var goals by remember { mutableStateOf(store.goals()) }
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var info by remember { mutableStateOf<StreakInfo?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Goal?>(null) }
    var editingIsNew by remember { mutableStateOf(false) }
    var showWhy by rememberSaveable { mutableStateOf(false) }
    var showProfile by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var showBlockSetup by remember { mutableStateOf(false) }
    var pendingAction by rememberSaveable { mutableStateOf<String?>(null) }
    var streakAnim by remember { mutableStateOf<StreakAnim?>(null) }
    val autoState = remember(resumeTick, refresh) { store.autoBlockActive() to store.autoBlockTurningOff() }
    val blockedToday = remember(resumeTick, refresh) { store.blockedTodayPackages().toSet() }

    fun runAction(action: String) {
        when (action) {
            "save" -> {
                val pkgs = info?.overToday?.map { it.pkg }.orEmpty()
                if (pkgs.isNotEmpty()) {
                    store.blockToday(pkgs)
                    store.markSaved(LocalDate.now())
                    BlockerService.instance?.enforceNow()
                }
            }
            "auto" -> store.setAutoBlock(true)
        }
        refresh++
    }

    fun request(action: String) {
        if (BlockerService.isEnabled(context)) {
            runAction(action)
        } else {
            pendingAction = action
            showBlockSetup = true
        }
    }

    // Al volver de Ajustes con el permiso concedido, terminamos lo que estaba pendiente
    LaunchedEffect(resumeTick) {
        val action = pendingAction
        if (action != null && BlockerService.isEnabled(context)) {
            pendingAction = null
            showBlockSetup = false
            runAction(action)
        }
    }

    // Permiso de notificaciones (Android 13+)
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Arranca la vigilancia en segundo plano
    LaunchedEffect(goals, resumeTick) {
        if (goals.isNotEmpty()) MonitorService.start(context)
    }

    // Refresca el tiempo de hoy cada 5 segundos mientras la app está abierta
    LaunchedEffect(goals, resumeTick, refresh) {
        while (true) {
            val result = withContext(Dispatchers.Default) {
                StreakEngine.evaluatePastDays(context)
                val todayUsage = UsageTracker.usageToday(context, goals.map { it.pkg }.toSet())
                todayUsage to StreakEngine.info(context, goals, todayUsage)
            }
            usage = result.first
            info = result.second
            delay(5_000)
        }
    }

    // Animación de racha: una vez al día por tipo (ganar, perder, salvar)
    LaunchedEffect(info) {
        val i = info ?: return@LaunchedEffect
        if (streakAnim != null || goals.isEmpty()) return@LaunchedEffect
        val today = LocalDate.now()
        when {
            i.todaySaved -> if (!store.animShown("saved", today)) {
                store.markAnimShown("saved", today)
                streakAnim = StreakAnim(StreakAnimKind.Saved, i.completed)
            }
            !i.todayOk -> if (i.completed >= 1 && !store.animShown("lost", today)) {
                store.markAnimShown("lost", today)
                streakAnim = StreakAnim(StreakAnimKind.Lost, i.completed)
            }
            i.current >= 1 -> if (!store.animShown("rise", today)) {
                store.markAnimShown("rise", today)
                streakAnim = StreakAnim(StreakAnimKind.Rise, i.current)
            }
        }
    }

    if (showWhy) {
        WhyScreen(onBack = { showWhy = false })
        return
    }
    if (showPrivacy) {
        PrivacyScreen(onBack = { showPrivacy = false })
        return
    }
    if (showProfile) {
        ProfileScreen(goals = goals, onBack = { showProfile = false })
        return
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPicker = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text(tr("＋  Añadir app", "＋  Add app", "＋  Aggiungi app"))
            }
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "alba",
                            style = MaterialTheme.typography.headlineLarge,
                            fontFamily = FontFamily.Serif,
                        )
                        Text(
                            tr("Mira arriba.", "Look up.", "Guarda in alto."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    OutlinedButton(onClick = { showProfile = true }) { Text(tr("Tu perfil", "Your profile", "Il tuo profilo")) }
                }
            }
            item { StreakCard(info, hasGoals = goals.isNotEmpty()) }

            val over = info?.overToday.orEmpty()
            if (over.isNotEmpty() && info?.todaySaved != true) {
                item { SaveStreakCard(over) { request("save") } }
            }

            val currentInfo = info
            if (goals.isNotEmpty() && currentInfo != null) {
                item { WeekCard(currentInfo.week) }
            }

            item { WhyEntryCard { showWhy = true } }

            item {
                Text(
                    tr("Tus apps", "Your apps", "Le tue app"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (goals.isEmpty()) {
                item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Text(
                            tr(
                                "Todavía no has elegido ninguna app.\nPulsa «Añadir app» y decide cuánto tiempo al día le quieres dar.",
                                "You haven't chosen any app yet.\nTap «Add app» and decide how much time a day you want to give it.",
                                "Non hai ancora scelto nessuna app.\nTocca «Aggiungi app» e decidi quanto tempo al giorno vuoi darle.",
                            ),
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }

            items(goals, key = { it.pkg }) { goal ->
                GoalCard(goal, usage[goal.pkg] ?: 0L, blocked = goal.pkg in blockedToday) {
                    editingIsNew = false
                    editing = goal
                }
            }

            if (goals.isNotEmpty()) {
                item {
                    AutoBlockCard(active = autoState.first, turningOff = autoState.second) { on ->
                        if (on) request("auto") else {
                            store.setAutoBlock(false)
                            refresh++
                        }
                    }
                }
                item { BatteryTip() }
            }
            item { LanguageEntry { showLanguage = true } }
            item { PrivacyEntryCard { showPrivacy = true } }
        }
    }
    streakAnim?.let { anim -> StreakAnimationOverlay(anim) { streakAnim = null } }
    }

    if (showPicker) {
        AppPickerDialog(
            exclude = goals.map { it.pkg }.toSet(),
            onDismiss = { showPicker = false },
            onPick = { app ->
                showPicker = false
                editingIsNew = true
                editing = Goal(app.pkg, app.label, 30)
            },
        )
    }

    if (showLanguage) {
        LanguageDialog(
            onDismiss = { showLanguage = false },
            onPick = { lang ->
                Lang.set(context, lang)
                showLanguage = false
            },
        )
    }

    if (showBlockSetup) {
        BlockSetupDialog(onDismiss = {
            showBlockSetup = false
            pendingAction = null
        })
    }

    editing?.let { goal ->
        LimitDialog(
            goal = goal,
            isNew = editingIsNew,
            onDismiss = { editing = null },
            onSave = { minutes ->
                store.upsertGoal(goal.copy(limitMinutes = minutes))
                goals = store.goals()
                editing = null
            },
            onDelete = {
                store.removeGoal(goal.pkg)
                goals = store.goals()
                editing = null
            },
        )
    }
}

// ---------------------------------------------------------------- tarjetas

@Composable
fun StreakCard(info: StreakInfo?, hasGoals: Boolean) {
    val current = info?.current ?: 0
    val cloudy = hasGoals && info?.todayOk == false
    val bright = hasGoals && !cloudy && current > 0
    val container = if (bright) AlbaColors.Sol else MaterialTheme.colorScheme.surfaceVariant
    val content = if (bright) AlbaColors.Noche else MaterialTheme.colorScheme.onSurface

    val message = when {
        !hasGoals -> tr("Elige una app y cuánto tiempo al día quieres darle.", "Choose an app and how much time a day you want to give it.", "Scegli un'app e quanto tempo al giorno vuoi darle.")
        info == null -> "…"
        cloudy -> tr("Hoy se ha nublado. Mañana vuelve a salir el sol.", "Clouds rolled in today. Tomorrow the sun rises again.", "Oggi si è rannuvolato. Domani torna il sole.")
        info.todaySaved -> tr("Día salvado. Medio sol, pero sol.", "Day saved. Half a sun, but still a sun.", "Giornata salvata. Mezzo sole, ma pur sempre sole.")
        else -> tr("Aguanta hasta medianoche y mañana serán ${current + 1}.", "Hold on until midnight and tomorrow it will be ${current + 1}.", "Resisti fino a mezzanotte e domani saranno ${current + 1}.")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Box {
        StreakCardDecoration(bright = bright, ink = content, modifier = Modifier.matchParentSize())
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painterResource(R.drawable.ic_sun),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .alpha(if (cloudy) 0.35f else 1f),
            )
            Text("$current", fontSize = 64.sp, lineHeight = 68.sp, fontFamily = FontFamily.Serif)
            Text(
                if (current == 1) tr("día seguido", "day in a row", "giorno di fila") else tr("días seguidos", "days in a row", "giorni di fila"),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(10.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            if (info != null && info.best > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    tr("Tu mejor racha: ", "Your best streak: ", "La tua serie migliore: ") + "${info.best} ${dias(info.best)}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.alpha(0.8f),
                )
            }
        }
        }
    }
}

@Composable
fun WeekCard(week: List<DayStatus>) {
    val locale = Lang.locale
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(tr("Tu semana", "Your week", "La tua settimana"), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                week.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale).uppercase(locale),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                        )
                        Spacer(Modifier.height(6.dp))
                        DayDot(day)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDot(day: DayStatus) {
    // Día salvado: una nube. Día cumplido: un sol.
    if (day.saved && day.ok == true) {
        CloudDayIcon(Modifier.size(36.dp))
        return
    }
    if (day.ok == true) {
        SunDayIcon(Modifier.size(36.dp))
        return
    }
    val (bg, symbol, fg) = when (day.ok) {
        false -> Triple(Muted, "·", AlbaColors.Arena)
        else -> Triple(
            MaterialTheme.colorScheme.surface,
            if (day.isToday) "•" else "–",
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    var modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(bg)
    if (day.isToday && day.ok == null) {
        modifier = modifier.border(BorderStroke(2.dp, AlbaColors.Alba), CircleShape)
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(symbol, color = fg, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalCard(goal: Goal, used: Long, blocked: Boolean = false, onClick: () -> Unit) {
    val fraction = (used.toFloat() / goal.limitMillis).coerceIn(0f, 1f)
    val over = goal.isOver(used)
    val barColor = when {
        over -> Muted
        fraction >= 0.8f -> Amber
        else -> Green
    }
    val status = when {
        blocked -> tr("Cerrada hasta mañana", "Closed until tomorrow", "Chiusa fino a domani")
        over -> tr("Hoy te has pasado ", "Over by ", "Oggi hai sforato di ") + formatDuration(used - goal.limitMillis)
        goal.limitMillis - used < 60_000L -> tr("Te queda menos de 1 min", "Less than 1 min left", "Ti resta meno di 1 min")
        else -> tr("Te quedan ", "Left: ", "Ti restano ") + formatDuration(goal.limitMillis - used)
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(goal.pkg, 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        goal.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${formatDuration(used)} / ${formatMinutes(goal.limitMinutes)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surface,
                )
                Spacer(Modifier.height(6.dp))
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun BatteryTip() {
    val context = LocalContext.current
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(tr("🔋 Para que los avisos lleguen siempre", "🔋 So reminders always arrive", "🔋 Perché gli avvisi arrivino sempre"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                tr(
                    "Algunos móviles (Xiaomi, Samsung, Huawei…) cierran las apps en segundo plano. Quita la optimización de batería para Alba.",
                    "Some phones (Xiaomi, Samsung, Huawei…) close apps in the background. Turn off battery optimization for Alba.",
                    "Alcuni telefoni (Xiaomi, Samsung, Huawei…) chiudono le app in background. Disattiva l'ottimizzazione della batteria per Alba.",
                ),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = {
                openSettings(context, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            }) { Text(tr("Abrir ajustes de batería", "Open battery settings", "Apri impostazioni batteria")) }
        }
    }
}

// ---------------------------------------------------------------- iconos

@Composable
fun AppIcon(pkg: String, size: Dp) {
    val context = LocalContext.current
    val bitmap = remember(pkg) {
        try {
            context.packageManager.getApplicationIcon(pkg).toBitmap(128, 128).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
        )
    }
}

// ---------------------------------------------------------------- selector de apps

data class AppEntry(val pkg: String, val label: String)

/** Apps que suelen "enganchar": salen primero en la lista. */
private val POPULAR = listOf(
    "com.instagram.android",
    "com.zhiliaoapp.musically",      // TikTok
    "com.ss.android.ugc.trill",       // TikTok (algunas regiones)
    "com.google.android.youtube",
    "com.twitter.android",            // X
    "com.facebook.katana",
    "com.snapchat.android",
    "com.reddit.frontpage",
    "tv.twitch.android.app",
    "com.pinterest",
    "com.whatsapp",
)

fun loadLaunchableApps(context: Context): List<AppEntry> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
    }
    return resolved
        .map { AppEntry(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .filter { it.pkg != context.packageName }
        .distinctBy { it.pkg }
        .sortedWith(
            compareBy<AppEntry>(
                { POPULAR.indexOf(it.pkg).let { i -> if (i < 0) Int.MAX_VALUE else i } },
                { it.label.lowercase() },
            )
        )
}

@Composable
fun AppPickerDialog(exclude: Set<String>, onDismiss: () -> Unit, onPick: (AppEntry) -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(tr("¿Qué te quita más de lo que te da?", "What takes more from you than it gives?", "Cosa ti toglie più di quanto ti dà?"), style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(tr("Buscar…", "Search…", "Cerca…")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))

                val list = apps
                if (list == null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                } else {
                    val filtered = list.filter {
                        it.pkg !in exclude && (query.isBlank() || it.label.contains(query.trim(), ignoreCase = true))
                    }
                    LazyColumn(Modifier.weight(1f)) {
                        items(filtered, key = { it.pkg }) { app ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onPick(app) }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIcon(app.pkg, 36.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(app.label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text(tr("Cancelar", "Cancel", "Annulla"))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- límite

@Composable
fun LimitDialog(
    goal: Goal,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    var minutes by remember(goal.pkg) {
        mutableFloatStateOf(goal.limitMinutes.coerceIn(Store.MIN_LIMIT_MINUTES, Store.MAX_LIMIT_MINUTES).toFloat())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AppIcon(goal.pkg, 48.dp) },
        title = { Text(goal.label) },
        text = {
            Column {
                Text(tr("¿Cuánto tiempo al día le das?", "How much time a day do you give it?", "Quanto tempo al giorno le dai?"))
                Text(
                    formatMinutes(minutes.roundToInt()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = minutes,
                    onValueChange = { minutes = it },
                    valueRange = Store.MIN_LIMIT_MINUTES.toFloat()..Store.MAX_LIMIT_MINUTES.toFloat(),
                    steps = (Store.MAX_LIMIT_MINUTES - Store.MIN_LIMIT_MINUTES) / 5 - 1, // saltos de 5 minutos
                )
                Text(
                    tr("Máximo 1 hora al día.", "Maximum 1 hour a day.", "Massimo 1 ora al giorno."),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    tr("Te avisaré cuando te quede poco.", "I'll let you know when you're running low.", "Ti avviserò quando te ne resterà poco."),
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!isNew) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDelete) {
                        Text(tr("Dejar de contar esta app", "Stop tracking this app", "Smetti di contare questa app"), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(minutes.roundToInt()) }) {
                Text(if (isNew) tr("Empezar", "Start", "Inizia") else tr("Guardar", "Save", "Salva"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Cancelar", "Cancel", "Annulla")) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhyEntryCard(onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BrainArt(Modifier.size(56.dp), state = BrainState.Overloaded)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("¿Por qué me aburro?", "Why am I bored?", "Perché mi annoio?"), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif)
                Text(
                    tr("Lo que pasa en tu cabeza cuando haces scroll", "What happens in your head when you scroll", "Cosa succede nella tua testa quando scorri"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("→", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
fun SaveStreakCard(apps: List<Goal>, onSave: () -> Unit) {
    val names = apps.joinToString(tr(" y ", " and ", " e ")) { it.label }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AlbaColors.Bruma, contentColor = AlbaColors.Arena),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("¿Salvar tu racha?", "Save your streak?", "Salvare la tua serie?"), style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
            Text(
                tr(
                    "Cierra $names lo que queda de día y hoy seguirá contando. Será un día a medias: medio sol.",
                    "Close $names for the rest of the day and today will still count. A half day: half a sun.",
                    "Chiudi $names per il resto della giornata e oggi conterà lo stesso. Una giornata a metà: mezzo sole.",
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = AlbaColors.Sol, contentColor = AlbaColors.Noche),
            ) { Text(tr("Cerrar $names hasta mañana", "Close $names until tomorrow", "Chiudi $names fino a domani")) }
        }
    }
}

@Composable
fun AutoBlockCard(active: Boolean, turningOff: Boolean, onToggle: (Boolean) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("Que Alba cierre la puerta", "Let Alba close the door", "Lascia che Alba chiuda la porta"), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (turningOff) tr(
                        "Se apagará mañana. Hasta entonces, sigue de tu lado.",
                        "It will turn off tomorrow. Until then, it's still on your side.",
                        "Si spegnerà domani. Fino ad allora, resta dalla tua parte.",
                    )
                    else tr(
                        "Cuando llegues a tu límite, la app se queda cerrada hasta mañana. Así no tienes que pelearte tú.",
                        "When you reach your limit, the app stays closed until tomorrow. That way you don't have to fight it yourself.",
                        "Quando raggiungi il limite, l'app resta chiusa fino a domani. Così non devi combattere tu.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = active && !turningOff, onCheckedChange = onToggle)
        }
    }
}

@Composable
fun BlockSetupDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Un permiso más", "One more permission", "Un'altra autorizzazione"), fontFamily = FontFamily.Serif) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    tr(
                        "Para cerrar apps, Alba usa el permiso de Accesibilidad. Solo ve qué app se abre, no lo que hay en tu pantalla.",
                        "To close apps, Alba uses the Accessibility permission. It only sees which app opens, not what is on your screen.",
                        "Per chiudere le app, Alba usa l'autorizzazione Accessibilità. Vede solo quale app si apre, non cosa c'è sullo schermo.",
                    )
                )
                Text(
                    tr(
                        "1. Pulsa «Abrir Accesibilidad», busca Alba (a veces dentro de «Apps instaladas» o «Servicios») y actívalo.",
                        "1. Tap «Open Accessibility», find Alba (sometimes under «Installed apps» or «Services») and turn it on.",
                        "1. Tocca «Apri Accessibilità», cerca Alba (a volte in «App scaricate» o «Servizi») e attivala.",
                    )
                )
                Text(
                    tr(
                        "2. Si el interruptor sale gris: pulsa «Ajustes de Alba», toca los tres puntos de arriba a la derecha, elige «Permitir ajustes restringidos» y repite el paso 1.",
                        "2. If the switch is greyed out: tap «Alba settings», tap the three dots at the top right, choose «Allow restricted settings» and repeat step 1.",
                        "2. Se l'interruttore è grigio: tocca «Impostazioni di Alba», tocca i tre puntini in alto a destra, scegli «Consenti impostazioni con restrizioni» e ripeti il passo 1.",
                    )
                )
                TextButton(onClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                    }
                }) { Text(tr("Ajustes de Alba", "Alba settings", "Impostazioni di Alba")) }
            }
        },
        confirmButton = {
            TextButton(onClick = { openSettings(context, Settings.ACTION_ACCESSIBILITY_SETTINGS) }) {
                Text(tr("Abrir Accesibilidad", "Open Accessibility", "Apri Accessibilità"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Ahora no", "Not now", "Non ora")) } },
    )
}
