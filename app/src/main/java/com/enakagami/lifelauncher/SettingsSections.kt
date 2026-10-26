package com.enakagami.lifelauncher

import android.content.Intent
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    Section(stringResource(R.string.s_color))
    Column(cardPad.fillMaxWidth().card().padding(vertical = 8.dp)) {
        ColorRow(stringResource(R.string.s_bg), bg) { editing = "bg" }
        ColorRow(stringResource(R.string.s_accent), accent) { editing = "accent" }
        val l1 = maxOf(bg.luminance(), accent.luminance()) + 0.05f
        val l2 = minOf(bg.luminance(), accent.luminance()) + 0.05f
        if (l1 / l2 < 1.8f) {
            Text(
                stringResource(R.string.s_contrast),
                color = Danger, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        TextButton(onClick = { store.saveColors(DefaultBg.toArgb(), DefaultAccent.toArgb()) }, modifier = Modifier.padding(start = 8.dp)) {
            Text(stringResource(R.string.s_color_reset))
        }
    }
    editing?.let { which ->
        ColorPickerDialog(
            title = stringResource(if (which == "bg") R.string.s_bg else R.string.s_accent_short),
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

    Section(stringResource(R.string.s_reminder))
    Column(cardPad.fillMaxWidth().card().padding(vertical = 12.dp)) {
        MoreInfo(stringResource(R.string.s_reminder_desc), Modifier.padding(horizontal = 20.dp), fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        // 必須：重ねて表示（お知らせ・進捗バー・時間切れでホームに戻すため）
        if (!overlayOk) {
            Text(stringResource(R.string.s_need_perm), color = Danger, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp))
        }
        PermissionRow(stringResource(R.string.s_overlay), overlayOk) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        // 任意：使用状況へのアクセス（履歴・通知から直接開いた場合も見張れる）
        PermissionRow(stringResource(R.string.s_usage), usageOk) {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        if (!usageOk) {
            Text(
                stringResource(R.string.s_simple_mode),
                color = SubInk, fontSize = 13.sp, lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                modifier = Modifier.padding(start = 8.dp),
            ) { Text(stringResource(R.string.open_app_info)) }
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.s_limited_apps), color = SubInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        if (!usageOk && Guard.limits.isNotEmpty()) {
            Text(stringResource(R.string.s_count_warning), color = Danger, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
        }
        Guard.limits.toList()
            .map { (pkg, min) -> Triple(pkg, min, apps.firstOrNull { it.pkg == pkg }?.label ?: pkg) }
            .sortedBy { it.third }
            .forEach { (pkg, min, label) -> LimitRow(pkg, label, min) }
        TextButton(onClick = { picking = true }, modifier = Modifier.padding(start = 8.dp)) { Text(stringResource(R.string.s_add_app), fontWeight = FontWeight.Bold) }

        Text(stringResource(R.string.s_wait), color = Ink, fontSize = 15.sp, modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp))
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20, 30).forEach { s -> Chip(stringResource(R.string.seconds, s), Guard.waitSec == s) { Guard.updateWaitSec(s) } }
        }
        SwitchRow(stringResource(R.string.s_bar), Guard.floatingBar) { Guard.updateFloatingBar(it) }
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
        if (ok) Text(stringResource(R.string.allowed), color = SubInk, fontSize = 14.sp)
        else TextButton(onClick = onClick) { Text(stringResource(R.string.allow), fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun LimitRow(pkg: String, label: String, minutes: Int) {
    var menu by remember { mutableStateOf(false) }
    val used = Guard.usedMs(pkg) / 60_000
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.s_used_today, used.toInt()), color = SubInk, fontSize = 12.sp)
        }
        Box {
            Text(
                stringResource(R.string.s_per_day, minutes) + " ▾",
                color = Blue, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { menu = true }.padding(horizontal = 10.dp, vertical = 8.dp),
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                LimitOptions.forEach { m ->
                    DropdownMenuItem(text = { Text(stringResource(R.string.dur_m, m)) }, onClick = { Guard.setLimit(pkg, m); menu = false })
                }
                DropdownMenuItem(text = { Text(stringResource(R.string.limit_remove), color = Danger) }, onClick = { Guard.removeLimit(pkg); menu = false })
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
        title = { Text(stringResource(R.string.s_pick_app), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = q, onValueChange = { q = it }, singleLine = true, placeholder = { Text(stringResource(R.string.search_apps)) }, modifier = Modifier.fillMaxWidth())
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ======================== 使えない時間帯 ========================
@Composable
fun LockSection() {
    val context = LocalContext.current
    val resume = LocalResumeSignal.current
    val ok = remember(resume) { Guard.hasOverlay(context) }
    Section(stringResource(R.string.s_lock))
    Column(cardPad.fillMaxWidth().card().padding(vertical = 12.dp)) {
        MoreInfo(stringResource(R.string.s_lock_desc), Modifier.padding(horizontal = 20.dp), fontSize = 14.sp)
        if (!ok) {
            Text(stringResource(R.string.s_lock_need), color = Danger, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        }
        RangeEditor(stringResource(R.string.night), Guard.night) { Guard.updateNight(it) }
        SwitchRow(stringResource(R.string.s_goal_on), Guard.goalOn) { Guard.updateGoalOn(it) }
        SwitchRow(stringResource(R.string.s_home_on_off), Guard.homeOnScreenOff) { Guard.updateHomeOnScreenOff(it) }
    }
}

/** 月〜日の短い名前（スマホの言語で） */
private val DayNames get() = java.time.DayOfWeek.values().map { it.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()) }

@Composable
private fun RangeEditor(name: String, range: LockRange, onChange: (LockRange) -> Unit) {
    var editing by remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            AccentSwitch(range.enabled, stringResource(R.string.range_on, name)) { onChange(range.copy(enabled = it)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeText(stringResource(R.string.range_start, name), range.start) { editing = "start" }
            Text("〜", color = Ink, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 8.dp))
            TimeText(stringResource(R.string.range_end, name), range.end) { editing = "end" }
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
            title = stringResource(if (which == "start") R.string.range_start_time else R.string.range_end_time, name),
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

@Composable
private fun TimeDialog(title: String, initial: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    var hour by remember { mutableStateOf(initial / 60) }
    var minute by remember { mutableStateOf(initial % 60 / 5 * 5) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text("$title  ${minutesText(hour * 60 + minute)}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.hour_label), color = SubInk, fontSize = 13.sp)
                (0 until 24).chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { h -> GridChip(stringResource(R.string.hour_value, h), hour == h, Modifier.weight(1f)) { hour = h } }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.minute_label), color = SubInk, fontSize = 13.sp)
                (0 until 60 step 5).chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { m -> GridChip(stringResource(R.string.minute_value, "%02d".format(m)), minute == m, Modifier.weight(1f)) { minute = m } }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(hour * 60 + minute) }) { Text(stringResource(R.string.decide), fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun GridChip(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Blue else CardBg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (selected) OnAccent else Ink, fontSize = 12.sp, maxLines = 1) }
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
        AccentSwitch(checked, label, onChange)
    }
}

@Composable
private fun AccentSwitch(checked: Boolean, description: String, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        modifier = Modifier.semantics { contentDescription = description },
        colors = SwitchDefaults.colors(checkedTrackColor = Blue, checkedThumbColor = OnAccent),
    )
}
