package com.enakagami.lifelauncher

import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * 長押しで画面の下からせり上がるメニュー。
 * content には「閉じてから実行する」関数 close を渡す（閉じるアニメーションを見せるため）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    title: String,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(close: (() -> Unit) -> Unit) -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val close: (() -> Unit) -> Unit = { then ->
        scope.launch { state.hide() }.invokeOnCompletion { onDismiss(); then() }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(Modifier.padding(start = 28.dp, end = 28.dp, bottom = 12.dp)) {
            Text(title, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = SubInk, fontSize = 13.sp)
            }
        }
        HorizontalDivider(color = SubInk.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 20.dp))
        Column(Modifier.padding(top = 8.dp, bottom = 28.dp)) { content(close) }
    }
}

/** メニューの1行：丸いアイコン＋項目名＋小さな説明 */
@Composable
fun SheetItem(
    icon: ImageVector,
    text: String,
    sub: String? = null,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val color = if (danger) Danger else Ink
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).background(if (danger) Danger.copy(alpha = 0.12f) else CardBg, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = color, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(text, color = color, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            if (sub != null) Text(sub, color = SubInk, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

private val LimitChoices = listOf(5, 10, 15, 30, 45, 60, 90, 120)

/** 1日に使ってよい時間を選ぶ（パネルの中身を差し替えて表示） */
@Composable
fun LimitChooser(pkg: String, onBack: () -> Unit, onDone: () -> Unit) {
    val current = Guard.limits[pkg]
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onBack).padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.ArrowBack, stringResource(R.string.back), tint = SubInk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.limit_title), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
    MoreInfo(stringResource(R.string.limit_desc), Modifier.padding(horizontal = 24.dp, vertical = 4.dp), fontSize = 13.sp)
    Spacer(Modifier.height(10.dp))
    LimitChoices.chunked(4).forEach { row ->
        Row(Modifier.padding(horizontal = 20.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { m ->
                val selected = current == m
                Column(
                    Modifier.weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) Blue else CardBg)
                        .clickable { Guard.setLimit(pkg, m); onDone() }
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("$m", color = if (selected) OnAccent else Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.unit_min), color = if (selected) OnAccent else SubInk, fontSize = 12.sp)
                }
            }
        }
    }
}

/** 使いすぎ防止（リマインダー）の項目。登録済みなら上限の変更と解除 */
@Composable
fun ReminderItems(pkg: String, onChoose: () -> Unit, close: (() -> Unit) -> Unit) {
    val limit = Guard.limits[pkg]
    if (limit == null) {
        SheetItem(Icons.Filled.Notifications, stringResource(R.string.limit_add), stringResource(R.string.limit_add_sub), onClick = onChoose)
    } else {
        SheetItem(Icons.Filled.Notifications, stringResource(R.string.limit_change), stringResource(R.string.limit_now, limit), onClick = onChoose)
        SheetItem(Icons.Filled.Clear, stringResource(R.string.limit_remove), onClick = { close { Guard.removeLimit(pkg) } })
    }
}

/** アプリごとの「使えない時間」の項目。決めてあれば変更とやめる */
@Composable
fun BlockItems(pkg: String, onChoose: () -> Unit, close: (() -> Unit) -> Unit) {
    val now = Guard.appBlockText(pkg)
    if (now == null) {
        SheetItem(Icons.Filled.Lock, stringResource(R.string.ab_add), stringResource(R.string.ab_add_sub), onClick = onChoose)
    } else {
        SheetItem(Icons.Filled.Lock, stringResource(R.string.ab_change), stringResource(R.string.ab_now, now), onClick = onChoose)
        SheetItem(Icons.Filled.Clear, stringResource(R.string.ab_remove), onClick = { close { Guard.removeAppBlock(pkg) } })
    }
}

/** 使えない時間（何時から何時まで）を選ぶ。毎日同じ時間。日付をまたいでもよい（例 23:00〜7:00） */
@Composable
fun BlockChooser(pkg: String, onBack: () -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val current = Guard.appBlocks[pkg]
    // 最初は朝（5:00〜10:00）
    var start by remember { mutableStateOf(current?.first ?: (5 * 60)) }
    var end by remember { mutableStateOf(current?.second ?: (10 * 60)) }
    fun pick(initial: Int, set: (Int) -> Unit) {
        android.app.TimePickerDialog(context, { _, h, m -> set(h * 60 + m) }, initial / 60, initial % 60,
            android.text.format.DateFormat.is24HourFormat(context)).show()
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onBack).padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.ArrowBack, stringResource(R.string.back), tint = SubInk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.ab_title), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
    Text(stringResource(R.string.ab_desc), color = SubInk, fontSize = 13.sp, lineHeight = 19.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
    Spacer(Modifier.height(10.dp))
    Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(R.string.ab_from to start, R.string.ab_to to end).forEachIndexed { i, (label, value) ->
            if (i == 1) Text(stringResource(R.string.ab_sep), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(CardBg)
                    .clickable { pick(value) { if (i == 0) start = it else end = it } }
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(label), color = SubInk, fontSize = 12.sp)
                Text(HabitRemind.timeText(value), color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = { Guard.setAppBlock(pkg, start, end); onDone() },
        enabled = start != end,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = OnAccent),
    ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Black, fontSize = 16.sp) }
}

/** メニューの上の小さな説明（使いすぎ防止・使えない時間） */
@Composable
fun guardSubtitle(pkg: String): String? = listOfNotNull(
    Guard.limits[pkg]?.let { stringResource(R.string.limit_badge, it) },
    Guard.appBlockText(pkg)?.let { stringResource(R.string.ab_now, it) },
).joinToString("　").ifEmpty { null }

/** アプリ一覧で長押ししたときのメニュー */
@Composable
fun AppActionSheet(app: AppInfo, store: Store, onDismiss: () -> Unit) {
    // null＝メニュー、"limit"＝1日の時間を選ぶ、"block"＝使えない時間を選ぶ
    var choosing by remember { mutableStateOf<String?>(null) }
    ActionSheet(app.label, guardSubtitle(app.pkg), onDismiss) { close ->
        if (choosing == "limit") {
            LimitChooser(app.pkg, onBack = { choosing = null }, onDone = { close {} })
        } else if (choosing == "block") {
            BlockChooser(app.pkg, onBack = { choosing = null }, onDone = { close {} })
        } else {
            val fav = store.isFavorite(app.key)
            SheetItem(Icons.Filled.Star, stringResource(if (fav) R.string.fav_remove else R.string.fav_add), stringResource(R.string.fav_sub)) {
                close { if (fav) store.removeFavorite(app.key) else store.addFavorite(app.key, app.label) }
            }
            ReminderItems(app.pkg, onChoose = { choosing = "limit" }, close = close)
            BlockItems(app.pkg, onChoose = { choosing = "block" }, close = close)
            SheetItem(Icons.Filled.Close, stringResource(R.string.hide), stringResource(R.string.hide_sub), danger = true) {
                close { store.hide(app.pkg) }
            }
        }
    }
}
