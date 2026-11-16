package com.enakagami.lifelauncher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/*
 * 集中タイム（2026-11-16）。始めると、終わるまでホーム画面の代わりに「集中タイム中」の画面を出し、
 * 電話と、自分で選んだ「使えるアプリ」以外は開けなくする（見守りサービスがホームに戻す）。
 * 夜のおやすみ時間と同じ仕組みで、こちらは好きなときに好きな長さで始められる。
 * 集中した時間は Guard.focusLog に残し、「AIに分析してもらう」の質問文にも入れる。
 */

private val FocusTop = Color(0xFF0D2620)
private val FocusBottom = Color(0xFF1E4A3F)
private val FocusText = Color(0xFFF3F7EE)
private val Mint = Color(0xFF8FD3B6)

/** 選べる長さ（分） */
val FOCUS_LENGTHS = listOf(15, 25, 50, 90, 120, 180)

/** 60 → 「1時間」、90 → 「1時間30分」、25 → 「25分」 */
@Composable
fun focusLengthText(minutes: Int): String =
    if (minutes >= 60 && minutes % 60 == 0) stringResource(R.string.dur_h, minutes / 60) else durationText(minutes)

private fun clockText(ms: Long): String {
    val t = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%d:%02d".format(t.hour, t.minute)
}

/** 残り時間 1:05:09 / 42:13 */
private fun remainText(ms: Long): String {
    val s = (ms + 999) / 1000
    return if (s >= 3600) "%d:%02d:%02d".format(s / 3600, s / 60 % 60, s % 60) else "%02d:%02d".format(s / 60, s % 60)
}

/** 暗い背景の上では、時計や電池のアイコンを白にする（この画面を閉じたら元に戻す） */
@Composable
private fun LightStatusIcons() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val bars = window?.let { WindowCompat.getInsetsController(it, view) }
        val before = bars?.isAppearanceLightStatusBars
        bars?.isAppearanceLightStatusBars = false
        onDispose { before?.let { bars.isAppearanceLightStatusBars = it } }
    }
}

/** 集中タイム中に、ホーム画面の代わりに出す画面 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusScreen(apps: List<AppInfo>) {
    val context = LocalContext.current
    val openApp = LocalOpenApp.current
    LightStatusIcons()
    val start = Guard.focusStart
    val until = Guard.focusUntil
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(until) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= until) { Guard.focusTimeUp(); break }
            delay(250)
        }
    }
    val total = (until - start).coerceAtLeast(1)
    val progress = ((now - start).toFloat() / total).coerceIn(0f, 1f)
    val allowed = Guard.focusApps.mapNotNull { pkg -> apps.firstOrNull { it.pkg == pkg } }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(FocusTop, FocusBottom)))) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.focus_now), color = Mint, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(28.dp))
            // 残り時間の輪：時間がたつほど輪が満ちていく
            Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = 10.dp.toPx()
                    val d = size.minDimension - w
                    val topLeft = Offset((size.width - d) / 2, (size.height - d) / 2)
                    drawArc(FocusText.copy(alpha = 0.12f), 0f, 360f, false, topLeft, Size(d, d), style = Stroke(w))
                    drawArc(Mint, -90f, 360f * progress, false, topLeft, Size(d, d), style = Stroke(w, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(remainText((until - now).coerceAtLeast(0)), color = FocusText, fontSize = 56.sp, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.focus_until, clockText(until)), color = FocusText.copy(alpha = 0.75f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(32.dp))
            // 使えるアプリ
            Text(stringResource(R.string.focus_apps), color = FocusText.copy(alpha = 0.75f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                CallButton(FocusText.copy(alpha = 0.9f)) { Guard.allowUntil = System.currentTimeMillis() + 10 * 60_000; dial(context) }
                allowed.forEach { app ->
                    Text(
                        app.label, color = FocusTop, fontSize = 16.sp, fontWeight = FontWeight.Bold, style = userTextStyle(),
                        modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(FocusText.copy(alpha = 0.9f))
                            .clickable { Guard.focusOpened = app.pkg; openApp(app.key) }
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                    )
                }
            }
            Spacer(Modifier.height(48.dp))
            StopButton()
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 長押し（2.5秒）でやめる。うっかりやめないよう、押している間だけ線が伸びる */
@Composable
private fun StopButton() {
    var hold by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val label = stringResource(R.string.focus_stop)
    Box(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, FocusText.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            .drawBehind { drawRect(Mint.copy(alpha = 0.35f), size = Size(size.width * hold, size.height)) }
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val job = scope.launch {
                        val t0 = System.currentTimeMillis()
                        while (true) {
                            hold = ((System.currentTimeMillis() - t0) / 2500f).coerceAtMost(1f)
                            if (hold >= 1f) { Guard.stopFocus(); break }
                            delay(16)
                        }
                    }
                    tryAwaitRelease()
                    job.cancel()
                    hold = 0f
                })
            },
    ) {
        Text(label, color = FocusText.copy(alpha = 0.8f), fontSize = 15.sp, modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp))
    }
}

/** 集中タイムが最後まで終わったときの画面。閉じるとホーム画面に戻る */
@Composable
fun FocusDoneScreen() {
    LightStatusIcons()
    val minutes = ((Guard.focusUntil - Guard.focusStart) / 60_000).toInt()
    val today = Guard.focusMinutes(LocalDate.now())
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(FocusTop, FocusBottom))).clickable { Guard.dismissFocusCelebration() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text(stringResource(R.string.focus_done_title), color = Mint, fontSize = 34.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.focus_done_body, focusLengthText(minutes)), color = FocusText, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.focus_today, durationText(today)), color = FocusText.copy(alpha = 0.75f), fontSize = 16.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(40.dp))
            Text(
                stringResource(R.string.close), color = FocusTop, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(FocusText)
                    .clickable { Guard.dismissFocusCelebration() }.padding(horizontal = 36.dp, vertical = 12.dp),
            )
        }
    }
}

/** 集中タイムを始める画面：長さ・使えるアプリ・最近の記録 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusDialog(apps: List<AppInfo>, onDismiss: () -> Unit) {
    var minutes by remember { mutableIntStateOf(Guard.focusLastMin) }
    var picking by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text(stringResource(R.string.w_focus), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MoreInfo(stringResource(R.string.focus_desc), fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.focus_how_long), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FOCUS_LENGTHS.forEach { m -> FocusChip(focusLengthText(m), m == minutes) { minutes = m } }
                }
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.focus_apps), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                val allowed = Guard.focusApps.map { pkg -> pkg to (apps.firstOrNull { it.pkg == pkg }?.label ?: pkg) }
                if (allowed.isEmpty()) Text(stringResource(R.string.focus_apps_none), color = SubInk, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
                allowed.forEach { (pkg, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = Ink, fontSize = 15.sp, modifier = Modifier.weight(1f), style = userTextStyle())
                        IconButton(onClick = { Guard.setFocusApp(pkg, false) }) {
                            Icon(Icons.Filled.Close, stringResource(R.string.delete), tint = SubInk, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                TextButton(onClick = { picking = true }) { Text("＋ " + stringResource(R.string.focus_pick), color = Blue, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.focus_history), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                for (back in 0L..6L) {
                    val d = today.minusDays(back)
                    val m = Guard.focusMinutes(d)
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(dateWithWeekday(d), color = SubInk, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        Text(if (m > 0) durationText(m) else "–", color = if (m > 0) Blue else SubInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { Guard.startFocus(minutes); onDismiss() },
                colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = OnAccent),
            ) { Text(stringResource(R.string.focus_start), fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    if (picking) FocusAppPicker(apps) { picking = false }
}

@Composable
private fun FocusChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) OnAccent else Ink,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Blue else Color.Transparent)
            .border(1.dp, if (selected) Blue else SubInk, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

/** 集中タイム中も使えるアプリを選ぶ（チェックを付け外しした時点で保存） */
@Composable
private fun FocusAppPicker(apps: List<AppInfo>, onDismiss: () -> Unit) {
    var q by remember { mutableStateOf("") }
    val shown = apps.distinctBy { it.pkg }.filter { q.isBlank() || it.label.contains(q.trim(), ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text(stringResource(R.string.focus_pick), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(textStyle = userTextStyle(), value = q, onValueChange = { q = it }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_apps)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(shown, key = { it.pkg }) { app ->
                        val on = Guard.focusAllows(app.pkg)
                        Row(
                            Modifier.fillMaxWidth().clickable { Guard.setFocusApp(app.pkg, !on) }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = on, onCheckedChange = { Guard.setFocusApp(app.pkg, it) },
                                colors = CheckboxDefaults.colors(checkedColor = Blue, checkmarkColor = OnAccent))
                            Spacer(Modifier.width(4.dp))
                            Text(app.label, color = Ink, fontSize = 17.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.decide), fontWeight = FontWeight.Bold) } },
    )
}
