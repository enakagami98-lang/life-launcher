package com.enakagami.lifelauncher

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
        Icon(Icons.Filled.ArrowBack, "戻る", tint = SubInk, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("1日に使ってよい時間", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
    Text(
        "開く前に少し待ってから、今回使う時間（5・10・15分）を決めるようになります。",
        color = SubInk, fontSize = 13.sp, lineHeight = 19.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    )
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
                    Text("分", color = if (selected) OnAccent else SubInk, fontSize = 12.sp)
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
        SheetItem(Icons.Filled.Notifications, "使いすぎ防止に追加", "開く前に少し待ち、使う時間を決めるようにします", onClick = onChoose)
    } else {
        SheetItem(Icons.Filled.Notifications, "1日の上限を変更", "今は1日 ${limit}分", onClick = onChoose)
        SheetItem(Icons.Filled.Clear, "使いすぎ防止から外す", onClick = { close { Guard.removeLimit(pkg) } })
    }
}

/** アプリ一覧で長押ししたときのメニュー */
@Composable
fun AppActionSheet(app: AppInfo, store: Store, onDismiss: () -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    val limit = Guard.limits[app.pkg]
    ActionSheet(app.label, if (limit != null) "使いすぎ防止：1日 ${limit}分" else null, onDismiss) { close ->
        if (choosing) {
            LimitChooser(app.pkg, onBack = { choosing = false }, onDone = { close {} })
        } else {
            val fav = store.isFavorite(app.key)
            SheetItem(Icons.Filled.Star, if (fav) "お気に入りから外す" else "お気に入りに追加", "寿命画面の下に表示されます") {
                close { if (fav) store.removeFavorite(app.key) else store.addFavorite(app.key, app.label) }
            }
            ReminderItems(app.pkg, onChoose = { choosing = true }, close = close)
            SheetItem(Icons.Filled.Close, "非表示にする", "一覧と検索に出なくなります（設定から戻せます）", danger = true) {
                close { store.hide(app.pkg) }
            }
        }
    }
}
