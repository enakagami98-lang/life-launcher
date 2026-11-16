package com.enakagami.lifelauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import java.time.LocalDate
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * 目標（kind="goals"）と習慣（kind="habits"）で共通の画面。
 * 習慣は左の丸でその日のチェック、右に続けた日数。チェックは日付ごとに残すので、0:00 になると自然に外れる
 */
@Composable
fun ItemsPage(store: Store, kind: String) {
    val goals = kind == "goals"
    // 開いたまま日付が変わっても、0:00 にチェックが外れて見えるよう、日付を読み直す
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) { while (true) { today = LocalDate.now(); delay(20_000) } }
    val title = stringResource(if (goals) R.string.page_goals else R.string.page_habits)
    val list = store.listFor(kind)
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Item?>(null) }

    val listState = rememberLazyListState()
    val reorder = rememberReorderableLazyListState(listState) { from, to ->
        store.moveItem(kind, from.index, to.index)
    }

    Column(Modifier.fillMaxSize()) {
        PageHeader(title) {
            RoundButton(Icons.Filled.Add, stringResource(R.string.add), Blue, OnAccent) { adding = true }
        }
        if (!goals && list.isNotEmpty()) {
            Text(
                stringResource(R.string.habits_reset),
                color = SubInk, fontSize = 13.sp, lineHeight = 19.sp,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )
        }
        if (list.isEmpty()) {
            Text(
                stringResource(R.string.items_empty),
                color = SubInk,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(list, key = { it.id }) { item ->
                ReorderableItem(reorder, key = item.id) { dragging ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .alpha(if (dragging) 0.85f else 1f)
                            .card()
                            .longPressDraggableHandle()
                            .clickable { editing = item }
                            .padding(horizontal = if (goals) 22.dp else 12.dp, vertical = if (goals) 20.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (goals) {
                            Icon(painterResource(R.drawable.ic_flag), null, tint = Blue, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(14.dp))
                            Text(item.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, style = userTextStyle())
                        } else {
                            val done = store.isHabitDone(item.id, today)
                            val streak = store.habitStreak(item.id, today)
                            val desc = stringResource(if (done) R.string.task_undo else R.string.task_do, item.text)
                            // チェックの丸（押すと今日の分をつける・外す）
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).clickable { store.toggleHabit(item.id, today) }
                                    .semantics { contentDescription = desc },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.size(26.dp).clip(CircleShape)
                                        .then(if (done) Modifier.background(Blue) else Modifier.border(2.dp, SubInk, CircleShape)),
                                    contentAlignment = Alignment.Center,
                                ) { if (done) Icon(Icons.Filled.Check, null, tint = OnAccent, modifier = Modifier.size(18.dp)) }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(item.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (done) SubInk else Ink,
                                style = userTextStyle(), modifier = Modifier.weight(1f))
                            if (streak > 0) {
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.streak, streak), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Blue)
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        TextInputDialog(
            title = stringResource(if (goals) R.string.goal_add else R.string.habit_add),
            initial = "",
            onSave = { store.addItem(kind, it); adding = false },
            onDismiss = { adding = false },
        )
    }
    editing?.let { item ->
        TextInputDialog(
            title = stringResource(if (goals) R.string.goal_edit else R.string.habit_edit),
            initial = item.text,
            onSave = { store.updateItem(kind, item.id, it); editing = null },
            onDismiss = { editing = null },
            onDelete = { store.deleteItem(kind, item.id); editing = null },
        )
    }
}
