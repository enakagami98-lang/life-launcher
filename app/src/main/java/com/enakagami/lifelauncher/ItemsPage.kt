package com.enakagami.lifelauncher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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
    val context = LocalContext.current
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
                            Column(Modifier.weight(1f)) {
                                Text(item.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (done) SubInk else Ink,
                                    style = userTextStyle())
                                item.time?.let {
                                    Text(stringResource(R.string.hr_at, HabitRemind.timeText(it)), fontSize = 12.sp, color = SubInk)
                                }
                            }
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
        if (goals) TextInputDialog(
            title = stringResource(R.string.goal_add),
            initial = "",
            onSave = { store.addItem(kind, it); adding = false },
            onDismiss = { adding = false },
        ) else HabitDialog(
            title = stringResource(R.string.habit_add),
            initial = null,
            onSave = { text, time -> store.addItem(kind, text, time); adding = false },
            onDismiss = { adding = false },
        )
    }
    editing?.let { item ->
        if (goals) TextInputDialog(
            title = stringResource(R.string.goal_edit),
            initial = item.text,
            onSave = { store.updateItem(kind, item.id, it); editing = null },
            onDismiss = { editing = null },
            onDelete = { store.deleteItem(kind, item.id); editing = null },
        ) else HabitDialog(
            title = stringResource(R.string.habit_edit),
            initial = item,
            onSave = { text, time -> store.updateItem(kind, item.id, text, time); editing = null },
            onDismiss = { editing = null },
            onDelete = { store.deleteItem(kind, item.id); HabitRemind.cancel(context, item.id); editing = null },
        )
    }
}

/**
 * 習慣の追加・編集。名前と「何時にやるか」（決めなくてもよい）。
 * 時刻を決めると、その時刻を過ぎてもチェックが無いときにお知らせする（Android 13 以降は通知の許可を聞く）
 */
@Composable
private fun HabitDialog(
    title: String,
    initial: Item?,
    onSave: (String, Int?) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var time by remember { mutableStateOf(initial?.time) }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun pick() {
        val start = time ?: (7 * 60)
        android.app.TimePickerDialog(context, { _, h, m -> time = h * 60 + m }, start / 60, start % 60,
            android.text.format.DateFormat.is24HourFormat(context)).show()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(textStyle = userTextStyle(), value = text, onValueChange = { text = it }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.hr_time), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                    Text(
                        time?.let { HabitRemind.timeText(it) } ?: stringResource(R.string.hr_none),
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (time == null) SubInk else Blue,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { pick() }.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                    if (time != null) {
                        IconButton(onClick = { time = null }) { Icon(Icons.Filled.Clear, stringResource(R.string.hr_clear), tint = SubInk) }
                    }
                }
                Text(stringResource(R.string.hr_note), fontSize = 12.sp, color = SubInk, lineHeight = 17.sp)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (time != null && Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    ) askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
                    onSave(text.trim(), time)
                },
                enabled = text.isNotBlank(),
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.delete), color = Danger) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
