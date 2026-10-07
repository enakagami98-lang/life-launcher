package com.enakagami.lifelauncher

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val cardPad = Modifier.padding(horizontal = 20.dp)

// ======================== 色 ========================
@Composable
fun ColorSection(store: Store) {
    var editing by remember { mutableStateOf<String?>(null) }
    val bg = Color(store.bgColor)
    val accent = Color(store.accentColor)
    Section("色")
    Column(cardPad.fillMaxWidth().card().padding(vertical = 8.dp)) {
        ColorRow("背景の色", bg) { editing = "bg" }
        ColorRow("アクセントの色（数字・ボタン）", accent) { editing = "accent" }
        val l1 = maxOf(bg.luminance(), accent.luminance()) + 0.05f
        val l2 = minOf(bg.luminance(), accent.luminance()) + 0.05f
        if (l1 / l2 < 1.8f) {
            Text(
                "背景とアクセントの色が近いため、数字が見えにくいかもしれません。",
                color = Danger, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        TextButton(onClick = { store.saveColors(DefaultBg.toArgb(), DefaultAccent.toArgb()) }, modifier = Modifier.padding(start = 8.dp)) {
            Text("元の色（白と青）に戻す")
        }
    }
    editing?.let { which ->
        ColorPickerDialog(
            title = if (which == "bg") "背景の色" else "アクセントの色",
            initial = if (which == "bg") bg else accent,
            onPick = { c ->
                if (which == "bg") store.saveColors(c.toArgb(), store.accentColor)
                else store.saveColors(store.bgColor, c.toArgb())
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun ColorRow(label: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Ink, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(color.hex(), color = SubInk, fontSize = 13.sp)
        Spacer(Modifier.width(10.dp))
        Swatch(color)
    }
}

// ======================== アプリリマインダー ========================
private val LimitOptions = listOf(5, 10, 15, 30, 45, 60, 90, 120)

@Composable
fun ReminderSection(apps: List<AppInfo>) {
    val context = LocalContext.current
    val resume = LocalResumeSignal.current
    val usageOk = remember(resume) { Guard.hasUsageAccess(context) }
    val overlayOk = remember(resume) { Guard.hasOverlay(context) }
    var picking by remember { mutableStateOf(false) }

    Section("アプリリマインダー")
    Column(cardPad.fillMaxWidth().card().padding(vertical = 12.dp)) {
        Text(
            "使いすぎてしまうアプリを選ぶと、開く前に少し待ってから、使う時間（5・10・15分）を決めるようになります。時間が来たら、もう一度待ってから延長するか決めます。1日の残りが5分・1分になると画面の下でお知らせします。",
            color = SubInk, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 20.dp),
        )
        if (!usageOk || !overlayOk) {
            Spacer(Modifier.height(12.dp))
            Text("使うには、次の2つの許可が必要です", color = Danger, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp))
            PermissionRow("使用状況へのアクセス", usageOk) {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PermissionRow("他のアプリの上に重ねて表示", overlayOk) {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Guard.limits.toList()
            .map { (pkg, min) -> Triple(pkg, min, apps.firstOrNull { it.pkg == pkg }?.label ?: pkg) }
            .sortedBy { it.third }
            .forEach { (pkg, min, label) -> LimitRow(pkg, label, min) }
        TextButton(onClick = { picking = true }, modifier = Modifier.padding(start = 8.dp)) { Text("＋ アプリを追加", fontWeight = FontWeight.Bold) }

        Text("開くまでの待ち時間", color = Ink, fontSize = 15.sp, modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp))
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20, 30).forEach { s -> Chip("${s}秒", Guard.waitSec == s) { Guard.updateWaitSec(s) } }
        }
        SwitchRow("フローティングプログレスバー（使っている間、画面の一番下に残り時間の線を出す）", Guard.floatingBar) { Guard.updateFloatingBar(it) }
    }

    if (picking) {
        AppPickerDialog(
            apps = apps.filter { !Guard.isLimited(it.pkg) }.distinctBy { it.pkg },
            onPick = { Guard.setLimit(it.pkg, 30); picking = false },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun PermissionRow(label: String, ok: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (ok) Text("許可済み", color = SubInk, fontSize = 14.sp)
        else TextButton(onClick = onClick) { Text("許可する", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun LimitRow(pkg: String, label: String, minutes: Int) {
    var menu by remember { mutableStateOf(false) }
    val used = Guard.usedMs(pkg) / 60_000
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("今日 ${used}分 使用", color = SubInk, fontSize = 12.sp)
        }
        Box {
            Text(
                "1日 ${minutes}分 ▾",
                color = Blue, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { menu = true }.padding(horizontal = 10.dp, vertical = 8.dp),
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                LimitOptions.forEach { m ->
                    DropdownMenuItem(text = { Text("${m}分") }, onClick = { Guard.setLimit(pkg, m); menu = false })
                }
                DropdownMenuItem(text = { Text("リマインダーから外す", color = Danger) }, onClick = { Guard.removeLimit(pkg); menu = false })
            }
        }
    }
}

@Composable
private fun AppPickerDialog(apps: List<AppInfo>, onPick: (AppInfo) -> Unit, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val shown = apps.filter { q.isBlank() || it.label.contains(q.trim(), ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text("アプリを選ぶ", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = q, onValueChange = { q = it }, singleLine = true, placeholder = { Text("アプリ名で検索") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(shown, key = { it.key }) { app ->
                        Text(
                            app.label, color = Ink, fontSize = 18.sp,
                            modifier = Modifier.fillMaxWidth().clickable { onPick(app) }.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}

// ======================== 使えない時間帯 ========================
@Composable
fun LockSection() {
    val context = LocalContext.current
    val resume = LocalResumeSignal.current
    val ok = remember(resume) { Guard.hasUsageAccess(context) && Guard.hasOverlay(context) }
    Section("使えない時間帯")
    Column(cardPad.fillMaxWidth().card().padding(vertical = 12.dp)) {
        Text(
            "設定した時間は、ホーム画面の代わりに「使えません」の画面が出ます。アプリを開いてもすぐ閉じます（電話と設定アプリは使えます）。今の時刻が入る時間にすると、すぐに始まります。",
            color = SubInk, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 20.dp),
        )
        if (!ok) {
            Text("※ アプリを閉じるには、上の「アプリリマインダー」にある2つの許可が必要です", color = Danger, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        }
        RangeEditor("朝", Guard.morning) { Guard.updateMorning(it) }
        RangeEditor("夜", Guard.night) { Guard.updateNight(it) }
        SwitchRow("画面を消したら寿命画面に戻す（次に画面をつけたとき必ず寿命画面から）", Guard.homeOnScreenOff) { Guard.updateHomeOnScreenOff(it) }
    }
}

private val DayNames = listOf("月", "火", "水", "木", "金", "土", "日")

@Composable
private fun RangeEditor(name: String, range: LockRange, onChange: (LockRange) -> Unit) {
    var editing by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            AccentSwitch(range.enabled) { onChange(range.copy(enabled = it)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeText("${name}の開始", range.start) { editing = "start" }
            Text("〜", color = Ink, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
            TimeText("${name}の終了", range.end) { editing = "end" }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayNames.forEachIndexed { i, d ->
                val day = i + 1
                val on = day in range.days
                Box(
                    Modifier.size(36.dp).clip(CircleShape)
                        .background(if (on) Blue else Color.Transparent)
                        .border(1.dp, if (on) Blue else SubInk, CircleShape)
                        .clickable { onChange(range.copy(days = if (on) range.days - day else range.days + day)) },
                    contentAlignment = Alignment.Center,
                ) { Text(d, color = if (on) OnAccent else Ink, fontSize = 14.sp) }
            }
        }
    }
    editing?.let { which ->
        TimeDialog(
            title = if (which == "start") "${name}の開始時刻" else "${name}の終了時刻",
            initial = if (which == "start") range.start else range.end,
            onPick = { m -> onChange(if (which == "start") range.copy(start = m) else range.copy(end = m)); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun TimeText(description: String, minutes: Int, onClick: () -> Unit) {
    Text(
        minutesText(minutes),
        color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black,
        modifier = Modifier
            .border(1.dp, SubInk, RoundedCornerShape(12.dp))
            .clickable(onClickLabel = description, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(title: String, initial: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial / 60, initialMinute = initial % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { TimeInput(state = state) },
        confirmButton = { TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) { Text("決定", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}

// ======================== 共通の部品 ========================
@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) OnAccent else Ink,
        fontSize = 14.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Blue else Color.Transparent)
            .border(1.dp, if (selected) Blue else SubInk, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        AccentSwitch(checked, onChange)
    }
}

@Composable
private fun AccentSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        colors = SwitchDefaults.colors(checkedTrackColor = Blue, checkedThumbColor = OnAccent),
    )
}
