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
                "Para saber cuánto tiempo pasas en cada app necesito el permiso " +
                    "«Acceso a datos de uso».\n\n" +
                    "Busca Alba en la lista, actívalo y vuelve aquí.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = {
                openSettings(context, Settings.ACTION_USAGE_ACCESS_SETTINGS)
            }) {
                Text("Dar permiso")
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
    var weekTeaser by remember { mutableStateOf<Insight?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var showBlockSetup by remember { mutableStateOf(false) }
    var pendingAction by rememberSaveable { mutableStateOf<String?>(null) }
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

    // Un dato de tu semana para la pantalla principal
    LaunchedEffect(resumeTick) {
        weekTeaser = withContext(Dispatchers.Default) {
            try {
                insights(Analytics.build(context, goals)).firstOrNull()
            } catch (_: Exception) {
                null
            }
        }
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

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showPicker = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text("＋  Añadir app")
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
                            "Mira arriba.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    OutlinedButton(onClick = { showProfile = true }) { Text("Tu perfil") }
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

            weekTeaser?.let { teaser ->
                item { WeekTeaserCard(teaser) { showProfile = true } }
            }

            item { WhyEntryCard { showWhy = true } }

            item {
                Text(
                    "Tus apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (goals.isEmpty()) {
                item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Text(
                            "Todavía no has elegido ninguna app.\nPulsa «Añadir app» y decide cuánto tiempo al día le quieres dar.",
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
            item { PrivacyEntryCard { showPrivacy = true } }
        }
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
        !hasGoals -> "Elige una app y cuánto tiempo al día quieres darle."
        info == null -> "…"
        cloudy -> "Hoy se ha nublado. Mañana vuelve a salir el sol."
        info.todaySaved -> "Día salvado. Medio sol, pero sol."
        else -> "Aguanta hasta medianoche y mañana serán ${current + 1}."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
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
                if (current == 1) "día seguido" else "días seguidos",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(10.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            if (info != null && info.best > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tu mejor racha: ${info.best} ${dias(info.best)}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.alpha(0.8f),
                )
            }
        }
    }
}

@Composable
fun WeekCard(week: List<DayStatus>) {
    val locale = Locale.forLanguageTag("es-ES")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Tu semana", style = MaterialTheme.typography.labelLarge)
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
    if (day.saved && day.ok == true) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(2.dp, Green), CircleShape),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(AlbaColors.Sol, startAngle = 180f, sweepAngle = 180f, useCenter = true)
            }
        }
        return
    }
    val (bg, symbol, fg) = when (day.ok) {
        true -> Triple(Green, "✓", AlbaColors.Noche)
        false -> Triple(Muted, "·", AlbaColors.Arena)
        null -> Triple(
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
        blocked -> "Cerrada hasta mañana"
        over -> "Hoy te has pasado ${formatDuration(used - goal.limitMillis)}"
        goal.limitMillis - used < 60_000L -> "Te queda menos de 1 min"
        else -> "Te quedan ${formatDuration(goal.limitMillis - used)}"
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
            Text("🔋 Para que los avisos lleguen siempre", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Algunos móviles (Xiaomi, Samsung, Huawei…) cierran las apps en segundo plano. " +
                    "Quita la optimización de batería para Alba.",
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = {
                openSettings(context, Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            }) { Text("Abrir ajustes de batería") }
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
                Text("¿Qué te quita más de lo que te da?", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar…") },
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
                    Text("Cancelar")
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
        mutableFloatStateOf(goal.limitMinutes.coerceIn(5, 180).toFloat())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AppIcon(goal.pkg, 48.dp) },
        title = { Text(goal.label) },
        text = {
            Column {
                Text("¿Cuánto tiempo al día le das?")
                Text(
                    formatMinutes(minutes.roundToInt()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = minutes,
                    onValueChange = { minutes = it },
                    valueRange = 5f..180f,
                    steps = 34, // saltos de 5 minutos
                )
                Text(
                    "Te avisaré cuando te quede poco.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!isNew) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onDelete) {
                        Text("Dejar de contar esta app", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(minutes.roundToInt()) }) {
                Text(if (isNew) "Empezar" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
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
                Text("¿Por qué me aburro?", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif)
                Text(
                    "Lo que pasa en tu cabeza cuando haces scroll",
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
    val names = apps.joinToString(" y ") { it.label }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AlbaColors.Bruma, contentColor = AlbaColors.Arena),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("¿Salvar tu racha?", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
            Text(
                "Cierra $names lo que queda de día y hoy seguirá contando. Será un día a medias: medio sol.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = AlbaColors.Sol, contentColor = AlbaColors.Noche),
            ) { Text("Cerrar $names hasta mañana") }
        }
    }
}

@Composable
fun AutoBlockCard(active: Boolean, turningOff: Boolean, onToggle: (Boolean) -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Que Alba cierre la puerta", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (turningOff) "Se apagará mañana. Hasta entonces, sigue de tu lado."
                    else "Cuando llegues a tu límite, la app se queda cerrada hasta mañana. Así no tienes que pelearte tú.",
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
        title = { Text("Un permiso más", fontFamily = FontFamily.Serif) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Para cerrar apps, Alba usa el permiso de Accesibilidad. Solo ve qué app se abre, no lo que hay en tu pantalla.")
                Text("1. Pulsa «Abrir Accesibilidad», busca Alba (a veces dentro de «Apps instaladas» o «Servicios») y actívalo.")
                Text("2. Si el interruptor sale gris: pulsa «Ajustes de Alba», toca los tres puntos de arriba a la derecha, elige «Permitir ajustes restringidos» y repite el paso 1.")
                TextButton(onClick = {
                    try {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {
                    }
                }) { Text("Ajustes de Alba") }
            }
        },
        confirmButton = {
            TextButton(onClick = { openSettings(context, Settings.ACTION_ACCESSIBILITY_SETTINGS) }) {
                Text("Abrir Accesibilidad")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ahora no") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekTeaserCard(insight: Insight, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AlbaColors.Alba, contentColor = AlbaColors.Noche),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(insight.value, fontSize = 34.sp, lineHeight = 38.sp, fontFamily = FontFamily.Serif)
                Text(insight.label, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text("Ver tu semana", style = MaterialTheme.typography.labelLarge)
            }
            Text("→", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
