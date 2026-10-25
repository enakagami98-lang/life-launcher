package com.enakagami.lifelauncher

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

fun yenText(n: Int) = "¥%,d".format(n)

/** 残り日数の下に並べる部品（並び順・表示する部品は「表示する項目を編集」で選ぶ） */
@Composable
fun TopWidgets(store: Store, today: LocalDate, openCondition: () -> Unit) {
    var editingMotto by remember { mutableStateOf(false) }
    var showSpend by remember { mutableStateOf(false) }
    var editingLayout by remember { mutableStateOf(false) }
    var editingCountdown by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf(false) }
    var goalMenu by remember { mutableStateOf(false) }
    var celebrating by remember { mutableStateOf(false) }
    var showUsage by remember { mutableStateOf(false) }

    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        store.widgets.forEach { id ->
            when (id) {
                "motto" -> MottoCard(store.motto) { editingMotto = true }
                "goal" -> GoalCard(Guard.dailyGoal(today), Guard.isGoalDone(today)) {
                    if (Guard.dailyGoal(today).isBlank()) editingGoal = true else goalMenu = true
                }
                "screen" -> ScreenCard(today) { showUsage = true }
                "spend" -> SpendCard { showSpend = true }
                "tasks" -> TasksCard(store, today)
                "countdown" -> CountdownCard(store, today) { editingCountdown = true }
                "weather" -> WeatherCard()
                "habits" -> HabitsCard(store, today)
                "steps" -> StepsCard()
                "yesterday" -> YesterdayCard(store, today, openCondition)
            }
        }
        Text(
            stringResource(R.string.w_edit),
            color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally).clip(CircleShape)
                .clickable { editingLayout = true }.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    if (editingMotto) {
        TextInputDialog(
            title = stringResource(R.string.w_motto),
            initial = store.motto,
            onSave = { store.saveMotto(it); editingMotto = false },
            onDismiss = { editingMotto = false },
            onDelete = if (store.motto.isNotEmpty()) ({ store.saveMotto(""); editingMotto = false }) else null,
        )
    }
    if (editingGoal) {
        val current = Guard.dailyGoal(today)
        TextInputDialog(
            title = stringResource(R.string.goal_title),
            initial = current,
            onSave = { Guard.setDailyGoal(today, it); editingGoal = false },
            onDismiss = { editingGoal = false },
            onDelete = if (current.isNotEmpty()) ({ Guard.setDailyGoal(today, ""); editingGoal = false }) else null,
        )
    }
    if (goalMenu) {
        GoalMenuDialog(
            goal = Guard.dailyGoal(today),
            done = Guard.isGoalDone(today),
            onDone = { Guard.setGoalDone(today, true); goalMenu = false; celebrating = true },
            onUndo = { Guard.setGoalDone(today, false); goalMenu = false },
            onEdit = { goalMenu = false; editingGoal = true },
            onDismiss = { goalMenu = false },
        )
    }
    if (celebrating) CongratsDialog { celebrating = false }
    if (showUsage) AppUsageDialog { showUsage = false }
    if (showSpend) SpendDialog { showSpend = false }
    if (editingLayout) WidgetEditDialog(store) { editingLayout = false }
    if (editingCountdown) CountdownDialog(store, today) { editingCountdown = false }
}

@Composable
private fun SmallLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SubInk, modifier = modifier)
}

@Composable
private fun CardColumn(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().card()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 22.dp, vertical = 16.dp),
    ) { content() }
}

// ---- 人生のモットー ----
@Composable
private fun MottoCard(motto: String, onEdit: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().card().clickable(onClick = onEdit).padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (motto.isBlank()) {
            Text(stringResource(R.string.motto_empty), fontSize = 15.sp, color = SubInk)
        } else {
            Text(
                "“$motto”",
                fontSize = 19.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
                color = Ink, textAlign = TextAlign.Center, lineHeight = 28.sp,
            )
        }
    }
}

// ---- 今日の目標 ----
@Composable
private fun GoalCard(goal: String, done: Boolean, onClick: () -> Unit) {
    CardColumn(onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallLabel(stringResource(R.string.goal_title), Modifier.weight(1f))
            if (done) {
                Row(
                    Modifier.clip(CircleShape).background(Blue).padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Check, null, tint = OnAccent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(stringResource(R.string.goal_done), fontSize = 12.sp, fontWeight = FontWeight.Black, color = OnAccent)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (goal.isBlank()) Text(stringResource(R.string.goal_empty), fontSize = 15.sp, color = SubInk)
        else Text(goal, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink, lineHeight = 28.sp)
    }
}

/** 目標をタップしたとき：達成した／書き直す */
@Composable
private fun GoalMenuDialog(goal: String, done: Boolean, onDone: () -> Unit, onUndo: () -> Unit, onEdit: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text(stringResource(R.string.goal_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(goal, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink, lineHeight = 28.sp)
                Spacer(Modifier.height(16.dp))
                if (!done) {
                    androidx.compose.material3.Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Blue, contentColor = OnAccent),
                    ) { Text(stringResource(R.string.goal_done_btn), fontWeight = FontWeight.Black, fontSize = 17.sp) }
                } else {
                    TextButton(onClick = onUndo) { Text(stringResource(R.string.goal_undo)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onEdit) { Text(stringResource(R.string.goal_edit_btn)) } },
    )
}

/** 目標を達成したときのお祝い */
@Composable
private fun CongratsDialog(onDismiss: () -> Unit) {
    val scale = remember { androidx.compose.animation.core.Animatable(0.4f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 220f))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "🎉", fontSize = 72.sp,
                    modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.congrats_title), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Blue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value },
                )
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.congrats_body), fontSize = 15.sp, color = Ink, textAlign = TextAlign.Center, lineHeight = 22.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), fontWeight = FontWeight.Bold) } },
    )
}

// ---- 今日のスマホ ----
@Composable
private fun ScreenCard(today: LocalDate, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().card().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            SmallLabel(stringResource(R.string.screen_today))
            Text(durationText(Guard.screenMinutes(today)), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Blue)
        }
        Text(stringResource(R.string.unlocks_short, Guard.unlockCount(today)), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SubInk)
    }
}

/** 円グラフの色（多い順に使う。最後の灰色は「その他」） */
private val PieColors = listOf(
    Color(0xFF1C2E5C), Color(0xFFE5484D), Color(0xFFF2A93B), Color(0xFF2E9E6B), Color(0xFF7B5CD6), Color(0xFF3AA6D9),
)
private val PieOther = Color(0xFFB8B8C0)

/** 今日、どのアプリにどれだけ使ったか（スマホ本体の使用記録から） */
@Composable
private fun AppUsageDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resume = LocalResumeSignal.current
    val access = remember(resume) { Guard.hasUsageAccess(context) }
    val other = stringResource(R.string.pie_other)
    val slices = remember(resume, access) {
        if (!access) emptyList()
        else {
            val pm = context.packageManager
            // このアプリ（ホーム画面）と画面の飾り（ステータスバーなど）は除く
            // 数え方は「今日のスマホ」と同じ（Guard.appTotalMs）。合計がずれないよう、1分未満のアプリも「その他」に入れる
            val usage = (Guard.systemUsage() ?: emptyMap()).filterKeys { it !in Guard.notCounted() }
            val totalMin = (Guard.appTotalMs(usage) / 60_000).toInt()
            val apps = usage.entries.filter { it.value >= 60_000 }.sortedByDescending { it.value }
            val top = apps.take(PieColors.size).map { e ->
                val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(e.key, 0)).toString() }.getOrDefault(e.key)
                label to (e.value / 60_000).toInt()
            }
            val rest = totalMin - top.sumOf { it.second }
            if (rest > 0) top + (other to rest) else top
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text(stringResource(R.string.pie_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    !access -> {
                        Text(stringResource(R.string.pie_need), fontSize = 14.sp, color = Ink, lineHeight = 20.sp)
                        TextButton(onClick = {
                            runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }) { Text(stringResource(R.string.allow), fontWeight = FontWeight.Bold) }
                    }
                    slices.isEmpty() -> Text(stringResource(R.string.pie_none), fontSize = 14.sp, color = SubInk)
                    else -> {
                        val total = slices.sumOf { it.second }.coerceAtLeast(1)
                        val colors = slices.indices.map { i -> if (slices[i].first == other && i == slices.lastIndex) PieOther else PieColors[i % PieColors.size] }
                        androidx.compose.foundation.Canvas(Modifier.size(180.dp)) {
                            var start = -90f
                            slices.forEachIndexed { i, (_, m) ->
                                val sweep = 360f * m / total
                                drawArc(colors[i], start, sweep, useCenter = true)
                                start += sweep
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        slices.forEachIndexed { i, (name, m) ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).clip(CircleShape).background(colors[i]))
                                Spacer(Modifier.width(8.dp))
                                Text(name, fontSize = 14.sp, color = Ink, maxLines = 1, modifier = Modifier.weight(1f))
                                Text(durationText(m), fontSize = 14.sp, fontWeight = FontWeight.Black, color = Ink)
                                Text("  ${m * 100 / total}%", fontSize = 12.sp, color = SubInk, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.pie_note), fontSize = 11.sp, color = SubInk, lineHeight = 16.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), fontWeight = FontWeight.Bold) } },
    )
}

// ---- 今日のタスク ----
@Composable
private fun TasksCard(store: Store, today: LocalDate) {
    val open = store.tasks.filter { it.doneDate == null }
    CardColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallLabel(stringResource(R.string.page_tasks), Modifier.weight(1f))
            if (open.isNotEmpty()) Text(stringResource(R.string.tasks_left, open.size), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Blue)
        }
        Spacer(Modifier.height(6.dp))
        when {
            store.tasks.isEmpty() -> Text(stringResource(R.string.tasks_none), fontSize = 14.sp, color = SubInk)
            open.isEmpty() -> Text(stringResource(R.string.all_done), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Blue)
            else -> open.take(3).forEach { t ->
                val desc = stringResource(R.string.task_do, t.text)
                CheckRow(t.text, done = false, desc = desc) { store.toggleTask(t.id, today) }
            }
        }
        if (open.size > 3) Text(stringResource(R.string.more_items, open.size - 3), fontSize = 12.sp, color = SubInk)
    }
}

@Composable
private fun CheckRow(text: String, done: Boolean, desc: String, trailing: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 5.dp).semantics { contentDescription = desc },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape)
                .then(if (done) Modifier.border(10.dp, Blue, CircleShape) else Modifier.border(2.dp, SubInk, CircleShape)),
            contentAlignment = Alignment.Center,
        ) { if (done) Icon(Icons.Filled.Check, null, tint = OnAccent, modifier = Modifier.size(14.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (done) SubInk else Ink, maxLines = 1, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Blue)
    }
}

// ---- 今月の支払額 ----
@Composable
private fun SpendCard(onOpen: () -> Unit) {
    val context = LocalContext.current
    val resume = LocalResumeSignal.current
    val access = remember(resume) { Spend.hasAccess(context) }
    val list = Spend.inMonth()
    CardColumn(onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SmallLabel(stringResource(R.string.w_spend))
                Text(yenText(list.sumOf { it.amount }), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Blue)
            }
            Text(stringResource(R.string.count_items, list.size) + " ›", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SubInk)
        }
        if (!access) Text(stringResource(R.string.spend_need_access), fontSize = 12.sp, color = Danger, lineHeight = 17.sp)
    }
}

/** 今月の支払いの一覧。読み間違いを消す・手で追加する */
@Composable
private fun SpendDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resume = LocalResumeSignal.current
    val access = remember(resume) { Spend.hasAccess(context) }
    var adding by remember { mutableStateOf(false) }
    val list = Spend.inMonth()
    val fmt = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
    val zone = ZoneId.systemDefault()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text(stringResource(R.string.spend_title, yenText(list.sumOf { it.amount })), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (!access) {
                    Text(stringResource(R.string.spend_how), fontSize = 13.sp, color = SubInk, lineHeight = 19.sp)
                    Text(stringResource(R.string.restricted_help), fontSize = 12.sp, color = SubInk, lineHeight = 18.sp)
                    TextButton(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }) { Text(stringResource(R.string.spend_allow), fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { openAppInfo(context) }) { Text(stringResource(R.string.open_app_info)) }
                    Spacer(Modifier.height(8.dp))
                }
                if (list.isEmpty()) Text(stringResource(R.string.spend_none), fontSize = 14.sp, color = SubInk)
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(list, key = { it.id }) { p ->
                        val source = if (p.source == "@manual") stringResource(R.string.spend_manual) else p.source
                        val auto = if (p.auto) stringResource(R.string.spend_auto) else ""
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(yenText(p.amount), fontSize = 17.sp, fontWeight = FontWeight.Black, color = Ink)
                                Text(
                                    Instant.ofEpochMilli(p.time).atZone(zone).format(fmt) + " · " + source + auto +
                                        (if (p.note.isNotBlank() && !p.auto) " · ${p.note}" else ""),
                                    fontSize = 12.sp, color = SubInk, maxLines = 1,
                                )
                            }
                            IconButton(onClick = { Spend.remove(p.id) }) {
                                Icon(Icons.Filled.Close, stringResource(R.string.remove_x, yenText(p.amount)), tint = SubInk)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = { adding = true }) { Text(stringResource(R.string.spend_add_manual)) } },
    )
    if (adding) AddPaymentDialog { adding = false }
}

fun openAppInfo(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
private fun AddPaymentDialog(onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    val n = amount.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text(stringResource(R.string.spend_add_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount, onValueChange = { v -> amount = v.filter { it.isDigit() }.take(8) },
                    label = { Text(stringResource(R.string.spend_amount)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = memo, onValueChange = { memo = it.replace("\n", "") },
                    label = { Text(stringResource(R.string.spend_memo)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { Spend.add(n!!, "@manual", memo.trim(), auto = false); onDismiss() }, enabled = n != null && n > 0) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ---- カウントダウン ----
@Composable
private fun CountdownCard(store: Store, today: LocalDate, onEdit: () -> Unit) {
    CardColumn(onEdit) {
        SmallLabel(stringResource(R.string.w_countdown))
        Spacer(Modifier.height(4.dp))
        if (store.countdowns.isEmpty()) {
            Text(stringResource(R.string.cd_empty), fontSize = 14.sp, color = SubInk)
        }
        store.countdowns.sortedBy { it.date }.forEach { c ->
            val days = ChronoUnit.DAYS.between(today, c.date).toInt()
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(c.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, modifier = Modifier.weight(1f))
                Text(
                    when {
                        days > 0 -> stringResource(R.string.cd_left, days)
                        days == 0 -> stringResource(R.string.cd_today)
                        else -> stringResource(R.string.cd_ago, -days)
                    },
                    fontSize = 20.sp, fontWeight = FontWeight.Black, color = if (days >= 0) Blue else SubInk,
                )
            }
        }
    }
}

@Composable
private fun CountdownDialog(store: Store, today: LocalDate, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var y by remember { mutableStateOf("") }
    var m by remember { mutableStateOf("") }
    var d by remember { mutableStateOf("") }
    val date = runCatching { LocalDate.of(y.toInt(), m.toInt(), d.toInt()) }.getOrNull()
    val ok = title.isNotBlank() && date != null && date.year in 1900..2200
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text(stringResource(R.string.w_countdown), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                store.countdowns.sortedBy { it.date }.forEach { c ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text(longDate(c.date), fontSize = 12.sp, color = SubInk)
                        }
                        IconButton(onClick = { store.removeCountdown(c.id) }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.remove_x, c.title), tint = SubInk)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = title, onValueChange = { title = it.replace("\n", "").take(20) },
                    label = { Text(stringResource(R.string.cd_title)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumField(stringResource(R.string.year), y, 4, Modifier.weight(1.4f)) { y = it }
                    NumField(stringResource(R.string.month), m, 2, Modifier.weight(1f)) { m = it }
                    NumField(stringResource(R.string.day), d, 2, Modifier.weight(1f)) { d = it }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { store.addCountdown(title.trim(), date!!); title = ""; y = ""; m = ""; d = "" }, enabled = ok) {
                Text(stringResource(R.string.add), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun NumField(label: String, value: String, max: Int, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(max)) },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = modifier,
    )
}

// ---- 天気 ----
@Composable
private fun WeatherCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resume = LocalResumeSignal.current
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) scope.launch { Weather.refresh(context, force = true) }
    }
    LaunchedEffect(resume) { Weather.refresh(context) }
    val w = Weather.data
    CardColumn(onClick = {
        if (!Weather.hasPermission(context)) ask.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        else scope.launch { Weather.refresh(context, force = true) }
    }) {
        SmallLabel(stringResource(R.string.w_weather))
        if (w != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(stringResource(Weather.nameOf(w.code)), fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
                Spacer(Modifier.width(10.dp))
                Text("${w.temp.roundToInt()}°", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Blue)
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.wx_maxmin, w.max.roundToInt(), w.min.roundToInt()), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SubInk)
            }
        }
        val err = Weather.error
        when {
            !Weather.hasPermission(context) -> Text(stringResource(R.string.weather_tap_allow), fontSize = 13.sp, color = Danger, lineHeight = 18.sp)
            err != null && w == null -> Text(stringResource(err), fontSize = 13.sp, color = SubInk)
            w == null -> Text(stringResource(R.string.loading), fontSize = 13.sp, color = SubInk)
        }
    }
}

// ---- 今日の習慣チェック ----
@Composable
private fun HabitsCard(store: Store, today: LocalDate) {
    CardColumn {
        val done = store.habits.count { store.isHabitDone(it.id, today) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallLabel(stringResource(R.string.w_habits), Modifier.weight(1f))
            if (store.habits.isNotEmpty()) Text("$done / ${store.habits.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Blue)
        }
        Spacer(Modifier.height(4.dp))
        if (store.habits.isEmpty()) Text(stringResource(R.string.habits_none), fontSize = 14.sp, color = SubInk)
        store.habits.forEach { h ->
            val isDone = store.isHabitDone(h.id, today)
            val streak = store.habitStreak(h.id, today)
            CheckRow(
                h.text, isDone,
                desc = stringResource(if (isDone) R.string.task_undo else R.string.task_do, h.text),
                trailing = if (streak > 0) stringResource(R.string.streak, streak) else null,
            ) { store.toggleHabit(h.id, today) }
        }
    }
}

// ---- 歩数 ----
@Composable
private fun StepsCard() {
    val context = LocalContext.current
    val resume = LocalResumeSignal.current
    var granted by remember(resume) { mutableStateOf(Steps.hasPermission(context)) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) Steps.ensureListening(context)
    }
    LaunchedEffect(granted) { if (granted) Steps.ensureListening(context) }
    CardColumn(onClick = if (!granted && Build.VERSION.SDK_INT >= 29) ({ ask.launch(Manifest.permission.ACTIVITY_RECOGNITION) }) else null) {
        SmallLabel(stringResource(R.string.w_steps))
        when {
            !Steps.hasSensor(context) -> Text(stringResource(R.string.steps_no_sensor), fontSize = 13.sp, color = SubInk)
            !granted -> Text(stringResource(R.string.steps_tap_allow), fontSize = 13.sp, color = Danger, lineHeight = 18.sp)
            else -> Text(stringResource(R.string.steps_value, Steps.todaySteps()), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Blue)
        }
    }
}

// ---- 昨日のふり返り ----
@Composable
private fun YesterdayCard(store: Store, today: LocalDate, openCondition: () -> Unit) {
    val y = today.minusDays(1)
    val log = store.day(y)
    val screen = Guard.screenMinutes(y)
    val none = "–"
    CardColumn {
        SmallLabel(stringResource(R.string.w_yesterday))
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            MiniStat(stringResource(R.string.yd_condition), log.overall?.toString() ?: none, Modifier.weight(1f))
            MiniStat(stringResource(R.string.yd_sleep), log.sleepHours?.let { String.format(Locale.getDefault(), "%.1fh", it) } ?: none, Modifier.weight(1f))
            MiniStat(stringResource(R.string.yd_phone), if (screen > 0) durationText(screen) else none, Modifier.weight(1.3f))
        }
        if (store.day(today).overall == null) {
            Text(
                stringResource(R.string.yd_input_today),
                color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp).clip(CircleShape).clickable(onClick = openCondition).padding(vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = SubInk)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Ink, maxLines = 1)
    }
}

/** どの部品を、どの順で出すか */
@Composable
private fun WidgetEditDialog(store: Store, onDismiss: () -> Unit) {
    // 表示中のものを上に、非表示のものを下に
    val order = remember { mutableStateListOf<String>().apply { addAll(store.widgets + ALL_WIDGETS.keys.filter { it !in store.widgets }) } }
    val on = remember { mutableStateListOf<String>().apply { addAll(store.widgets) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text(stringResource(R.string.w_edit_title), fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(Modifier.heightIn(max = 460.dp)) {
                item { Text(stringResource(R.string.w_edit_hint), fontSize = 13.sp, color = SubInk); Spacer(Modifier.height(8.dp)) }
                items(order.size) { i ->
                    val id = order[i]
                    val name = stringResource(ALL_WIDGETS[id] ?: R.string.w_motto)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val show = stringResource(R.string.w_show, name)
                        Switch(
                            checked = id in on,
                            onCheckedChange = { if (it) on.add(id) else on.remove(id) },
                            modifier = Modifier.semantics { contentDescription = show },
                            colors = SwitchDefaults.colors(checkedTrackColor = Blue, checkedThumbColor = OnAccent),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(name, fontSize = 15.sp, color = Ink, modifier = Modifier.weight(1f))
                        IconButton(onClick = { order.add(i - 1, order.removeAt(i)) }, enabled = i > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.w_up, name), tint = if (i > 0) Ink else SubInk.copy(alpha = 0.25f))
                        }
                        IconButton(onClick = { order.add(i + 1, order.removeAt(i)) }, enabled = i < order.size - 1) {
                            Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.w_down, name), tint = if (i < order.size - 1) Ink else SubInk.copy(alpha = 0.25f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { store.saveWidgets(order.filter { it in on }); onDismiss() }) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
