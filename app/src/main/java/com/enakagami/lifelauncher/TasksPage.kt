package com.enakagami.lifelauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDate

/** 今日のタスク（チェックボックス式）。習慣の右隣の画面 */
@Composable
fun TasksPage(store: Store, today: LocalDate) {
    LaunchedEffect(today) { store.pruneTasks(today) }
    val list = store.tasks
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }

    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        // 先頭の「◯/◯ 完了」の行の分だけずらす
        store.moveTask(from.index - 1, to.index - 1)
    }
    val done = list.count { it.doneDate != null }

    Column(Modifier.fillMaxSize()) {
        PageHeader("今日のタスク") {
            RoundButton(Icons.Filled.Add, "追加", Blue, OnAccent) { adding = true }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "summary") {
                if (list.isEmpty()) {
                    Text(
                        "右上の＋から今日やることを追加できます。\n終わったら左の丸をタップ。終わったタスクは次の日に消え、終わらなかったタスクは次の日に持ち越されます。\n長押ししたまま上下に動かすと並べ替えできます。",
                        color = SubInk, fontSize = 15.sp, lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                } else {
                    Column(Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            if (done == list.size) "全部おわり！" else "$done / ${list.size} 完了",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Blue,
                        )
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { done.toFloat() / list.size },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Blue,
                            trackColor = CardBg,
                        )
                    }
                }
            }
            items(list, key = { it.id }) { task ->
                ReorderableItem(reorder, key = task.id) { dragging ->
                    val isDone = task.doneDate != null
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .alpha(if (dragging) 0.85f else 1f)
                            .card()
                            .longPressDraggableHandle()
                            .clickable { editing = task }
                            .padding(start = 14.dp, end = 22.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // チェックボックス（丸）
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { store.toggleTask(task.id, today) }
                                .semantics { contentDescription = if (isDone) "${task.text}を未完了に戻す" else "${task.text}を完了にする" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isDone) Blue else CardBg)
                                    .border(2.dp, if (isDone) Blue else SubInk, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isDone) Icon(Icons.Filled.Check, null, tint = OnAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            task.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) SubInk else Ink,
                            textDecoration = if (isDone) TextDecoration.LineThrough else null,
                        )
                    }
                }
            }
        }
    }

    if (adding) {
        TextInputDialog(
            title = "今日のタスクを追加",
            initial = "",
            onSave = { store.addTask(it); adding = false },
            onDismiss = { adding = false },
        )
    }
    editing?.let { task ->
        TextInputDialog(
            title = "タスクを編集",
            initial = task.text,
            onSave = { store.updateTask(task.id, it); editing = null },
            onDismiss = { editing = null },
            onDelete = { store.deleteTask(task.id); editing = null },
        )
    }
}
