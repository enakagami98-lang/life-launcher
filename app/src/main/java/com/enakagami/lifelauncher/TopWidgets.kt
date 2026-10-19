package com.enakagami.lifelauncher

import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun yenText(n: Int) = "¥%,d".format(n)

/** 残り日数の下に並べる部品（並び順・表示する部品は「表示する項目を編集」で選ぶ） */
@Composable
fun TopWidgets(store: Store, today: LocalDate) {
    var editingMotto by remember { mutableStateOf(false) }
    var showSpend by remember { mutableStateOf(false) }
    var editingLayout by remember { mutableStateOf(false) }

    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        store.widgets.forEach { id ->
            when (id) {
                "motto" -> MottoCard(store.motto) { editingMotto = true }
                "screen" -> ScreenCard(today)
                "tasks" -> TasksCard(store, today)
                "spend" -> SpendCard { showSpend = true }
            }
        }
        Text(
            "表示する項目を編集",
            color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally).clip(CircleShape)
                .clickable { editingLayout = true }.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    if (editingMotto) {
        TextInputDialog(
            title = "人生のモットー",
            initial = store.motto,
            onSave = { store.saveMotto(it); editingMotto = false },
            onDismiss = { editingMotto = false },
            onDelete = if (store.motto.isNotEmpty()) ({ store.saveMotto(""); editingMotto = false }) else null,
        )
    }
    if (showSpend) SpendDialog { showSpend = false }
    if (editingLayout) WidgetEditDialog(store) { editingLayout = false }
}

@Composable
private fun SmallLabel(text: String) {
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SubInk)
}

@Composable
private fun MottoCard(motto: String, onEdit: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().card().clickable(onClick = onEdit).padding(horizontal = 22.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (motto.isBlank()) {
            Text("タップして、人生のモットーを入力", fontSize = 15.sp, color = SubInk)
        } else {
            Text(
                "“$motto”",
                fontSize = 19.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
                color = Ink, textAlign = TextAlign.Center, lineHeight = 28.sp,
            )
        }
    }
}

@Composable
private fun ScreenCard(today: LocalDate) {
    Row(Modifier.fillMaxWidth().card().padding(horizontal = 22.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            SmallLabel("今日のスマホ")
            Text(durationText(Guard.screenMinutes(today)), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Blue)
        }
        Text("解除 ${Guard.unlockCount(today)}回", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SubInk)
    }
}

@Composable
private fun TasksCard(store: Store, today: LocalDate) {
    val open = store.tasks.filter { it.doneDate == null }
    Column(Modifier.fillMaxWidth().card().padding(horizontal = 22.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallLabel("今日のタスク")
            Spacer(Modifier.weight(1f))
            if (open.isNotEmpty()) Text("残り${open.size}件", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Blue)
        }
        Spacer(Modifier.height(6.dp))
        when {
            store.tasks.isEmpty() -> Text("まだありません（習慣の右の画面で追加）", fontSize = 14.sp, color = SubInk)
            open.isEmpty() -> Text("全部おわり！", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Blue)
            else -> open.take(3).forEach { t ->
                Row(
                    Modifier.fillMaxWidth().clickable { store.toggleTask(t.id, today) }.padding(vertical = 5.dp)
                        .semantics { contentDescription = "${t.text}を完了にする" },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(20.dp).clip(CircleShape).border(2.dp, SubInk, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(t.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
                }
            }
        }
        if (open.size > 3) Text("ほか${open.size - 3}件", fontSize = 12.sp, color = SubInk)
    }
}

@Composable
private fun SpendCard(onOpen: () -> Unit) {
    val context = LocalContext.current
    val resume = LocalResumeSignal.current
    val access = remember(resume) { Spend.hasAccess(context) }
    val list = Spend.inMonth()
    Column(Modifier.fillMaxWidth().card().clickable(onClick = onOpen).padding(horizontal = 22.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SmallLabel("今月の支払額")
                Text(yenText(list.sumOf { it.amount }), fontSize = 26.sp, fontWeight = FontWeight.Black, color = Blue)
            }
            Text("${list.size}件 ›", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SubInk)
        }
        if (!access) {
            Text(
                "支払いの通知から自動で記録するには、タップして「通知へのアクセス」を許可してください",
                fontSize = 12.sp, color = Danger, lineHeight = 17.sp,
            )
        }
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
    val fmt = DateTimeFormatter.ofPattern("M/d H:mm")
    val zone = ZoneId.systemDefault()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text("今月の支払額  ${yenText(list.sumOf { it.amount })}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (!access) {
                    Text(
                        "PayPay・カード・Gmailなどの「○○円お支払い」の通知から自動で記録します。\n" +
                            "許可できない場合：アプリ情報 → 右上の「︙」→「制限付き設定を許可」→ もう一度「許可する」",
                        fontSize = 13.sp, color = SubInk, lineHeight = 19.sp,
                    )
                    Row {
                        TextButton(onClick = {
                            runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        }) { Text("通知へのアクセスを許可する", fontWeight = FontWeight.Bold) }
                    }
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) { Text("アプリ情報を開く") }
                    Spacer(Modifier.height(8.dp))
                }
                if (list.isEmpty()) Text("今月の記録はまだありません。", fontSize = 14.sp, color = SubInk)
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(list, key = { it.id }) { p ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(yenText(p.amount), fontSize = 17.sp, fontWeight = FontWeight.Black, color = Ink)
                                Text(
                                    Instant.ofEpochMilli(p.time).atZone(zone).format(fmt) + "・" + p.source +
                                        (if (p.auto) "（自動）" else "") + (if (p.note.isNotBlank() && !p.auto) "・${p.note}" else ""),
                                    fontSize = 12.sp, color = SubInk, maxLines = 1,
                                )
                            }
                            IconButton(onClick = { Spend.remove(p.id) }) {
                                Icon(Icons.Filled.Close, "${yenText(p.amount)}を消す", tint = SubInk)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("閉じる", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = { adding = true }) { Text("＋ 手で追加") } },
    )
    if (adding) AddPaymentDialog { adding = false }
}

@Composable
private fun AddPaymentDialog(onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    val n = amount.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text("支払いを追加", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amount, onValueChange = { v -> amount = v.filter { it.isDigit() }.take(8) },
                    label = { Text("金額（円）") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = memo, onValueChange = { memo = it.replace("\n", "") },
                    label = { Text("メモ（任意）") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { Spend.add(n!!, "手入力", memo.trim(), auto = false); onDismiss() }, enabled = n != null && n > 0) {
                Text("保存", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
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
        title = { Text("表示する項目", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("「残りの日数」の下に、上から順に並びます。", fontSize = 13.sp, color = SubInk)
                Spacer(Modifier.height(8.dp))
                order.forEachIndexed { i, id ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = id in on,
                            onCheckedChange = { if (it) on.add(id) else on.remove(id) },
                            modifier = Modifier.semantics { contentDescription = "${ALL_WIDGETS[id]}を表示" },
                            colors = SwitchDefaults.colors(checkedTrackColor = Blue, checkedThumbColor = OnAccent),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(ALL_WIDGETS[id] ?: id, fontSize = 15.sp, color = Ink, modifier = Modifier.weight(1f))
                        IconButton(onClick = { order.add(i - 1, order.removeAt(i)) }, enabled = i > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, "${ALL_WIDGETS[id]}を上へ", tint = if (i > 0) Ink else SubInk.copy(alpha = 0.25f))
                        }
                        IconButton(onClick = { order.add(i + 1, order.removeAt(i)) }, enabled = i < order.size - 1) {
                            Icon(Icons.Filled.KeyboardArrowDown, "${ALL_WIDGETS[id]}を下へ", tint = if (i < order.size - 1) Ink else SubInk.copy(alpha = 0.25f))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { store.saveWidgets(order.filter { it in on }); onDismiss() }) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("保存", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } },
    )
}
