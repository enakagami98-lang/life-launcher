package com.enakagami.lifelauncher

import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 期限の短い表示（例：10/28 18:00。並びはスマホの言語に合わせる） */
fun dueShort(ms: Long): String {
    val pattern = android.text.format.DateFormat.getBestDateTimePattern(Locale.getDefault(), "MdHm")
    return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

/** タスク（チェックボックス式）。期限がある場合は、期限が近い順に並ぶ。習慣の右隣の画面 */
@Composable
fun TasksPage(store: Store, today: LocalDate) {
    LaunchedEffect(today) { store.pruneTasks(today) }
    val list = store.sortedTasks()
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    val done = list.count { it.doneDate != null }
    val now = System.currentTimeMillis()

    Column(Modifier.fillMaxSize()) {
        PageHeader(stringResource(R.string.page_tasks)) {
            RoundButton(Icons.Filled.Add, stringResource(R.string.add), Blue, OnAccent) { adding = true }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "summary") {
                if (list.isEmpty()) {
                    Text(
                        stringResource(R.string.tasks_empty),
                        color = SubInk, fontSize = 15.sp, lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                } else {
                    Column(Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            if (done == list.size) stringResource(R.string.all_done) else stringResource(R.string.tasks_progress, done, list.size),
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
                val isDone = task.doneDate != null
                Row(
                    Modifier
                        .fillMaxWidth()
                        .card()
                        .clickable { editing = task }
                        .padding(start = 14.dp, end = 22.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // チェックボックス（丸）
                    val checkDesc = stringResource(if (isDone) R.string.task_undo else R.string.task_do, task.text)
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { store.toggleTask(task.id, today) }
                            .semantics { contentDescription = checkDesc },
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
                    Column(Modifier.weight(1f)) {
                        Text(
                            task.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone) SubInk else Ink,
                            textDecoration = if (isDone) TextDecoration.LineThrough else null,
                        )
                        task.due?.let { due ->
                            val late = !isDone && due < now
                            Text(
                                if (late) stringResource(R.string.task_overdue) + " · " + dueShort(due)
                                else stringResource(R.string.task_due_until, dueShort(due)),
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = if (late) Danger else SubInk,
                            )
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        TaskDialog(
            title = stringResource(R.string.task_add),
            initial = null,
            onSave = { text, due -> store.addTask(text, due); adding = false },
            onDismiss = { adding = false },
        )
    }
    editing?.let { task ->
        TaskDialog(
            title = stringResource(R.string.task_edit),
            initial = task,
            onSave = { text, due -> store.updateTask(task.id, text, due); editing = null },
            onDismiss = { editing = null },
            onDelete = { store.deleteTask(task.id); editing = null },
        )
    }
}

/** タスクの追加・編集。名前と「何日の何時までに」 */
@Composable
private fun TaskDialog(
    title: String,
    initial: Task?,
    onSave: (String, Long?) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var due by remember { mutableStateOf(initial?.due) }

    // 日付 → 時刻の順に選ぶ（最初は明日の18:00）
    fun pickDue() {
        val zone = ZoneId.systemDefault()
        val start = due?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), zone) }
            ?: LocalDate.now().plusDays(1).atTime(18, 0)
        android.app.DatePickerDialog(context, { _, y, m, d ->
            android.app.TimePickerDialog(context, { _, h, min ->
                due = LocalDateTime.of(y, m + 1, d, h, min).atZone(zone).toInstant().toEpochMilli()
            }, start.hour, start.minute, android.text.format.DateFormat.is24HourFormat(context)).show()
        }, start.year, start.monthValue - 1, start.dayOfMonth).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = text, onValueChange = { text = it }, maxLines = 3, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.task_due), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
                    Text(
                        due?.let { stringResource(R.string.task_due_until, dueShort(it)) } ?: stringResource(R.string.task_due_none),
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (due == null) SubInk else Blue,
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(CardBg).clickable { pickDue() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
                if (due != null) {
                    TextButton(onClick = { due = null }) { Text(stringResource(R.string.task_due_clear)) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text.trim(), due) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text(stringResource(R.string.delete), color = Danger) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}
