package com.enakagami.lifelauncher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/** 全画面の「今日の言葉」（"quote"）や日記の1日分（journalPanelKey）を開く */
val LocalOpenPanel = compositionLocalOf<(String) -> Unit> { {} }

/** 日記の欄（JournalEntry.fields() と同じ順） */
private val JOURNAL_LABELS = listOf(
    R.string.j_story, R.string.j_good, R.string.j_goals, R.string.j_noticed,
    R.string.j_music, R.string.j_waste, R.string.j_happy, R.string.j_challenge,
)

/**
 * 全画面の入れ物。キーボードが出たら、その分だけ中身の高さを縮める（imePadding）。
 * 中身は縦にスクロールでき、入力中の欄は自動でキーボードの上に来る。
 * ボタンは上の見出しに置く（下に置くとキーボードに隠れるため）
 */
@Composable
private fun FullPanel(title: String, onClose: () -> Unit, action: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onClose)
    Column(
        Modifier.fillMaxSize().background(Bg)
            // 下にあるホーム画面に指が届かないように
            .clickable(remember { MutableInteractionSource() }, null) {}
            .systemBarsPadding().imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(R.string.close), tint = Ink) }
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
            action?.invoke()
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun HeaderButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = OnAccent),
    ) { Text(text, fontWeight = FontWeight.Black) }
}

@Composable
private fun BigButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = OnAccent),
    ) { Text(text, fontWeight = FontWeight.Black, fontSize = 16.sp) }
}

/** 見出しつきの入力欄（見出しは欄の外に出す。欄の中に入れると、書き始めたときに小さくなって読みにくいため） */
@Composable
private fun LabeledField(label: String, value: String, hint: String? = null, minLines: Int = 2, onChange: (String) -> Unit) {
    Column {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            placeholder = hint?.let { { Text(it, color = SubInk) } },
            minLines = minLines,
            textStyle = userTextStyle(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardBg, unfocusedContainerColor = CardBg),
        )
    }
}

// ---- トップの部品 ----

@Composable
fun QuoteCard(store: Store, today: LocalDate) {
    val open = LocalOpenPanel.current
    val q = store.quoteOf(today)
    Column(
        Modifier.fillMaxWidth().card().clickable { open("quote") }.padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.w_quote), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SubInk, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(4.dp))
        if (q == null) {
            Text(stringResource(R.string.q_empty), fontSize = 15.sp, color = SubInk, textAlign = TextAlign.Center)
        } else {
            Text(
                "“${q.text}”", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
                color = Ink, textAlign = TextAlign.Center, lineHeight = 26.sp, style = userTextStyle(),
                maxLines = 5, overflow = TextOverflow.Ellipsis,
            )
            quoteCredit(q)?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, fontSize = 13.sp, color = SubInk, textAlign = TextAlign.Center)
            }
        }
    }
}

/** 「— 誰　出どころ」の1行。どちらも無ければ null */
private fun quoteCredit(q: Quote): String? =
    listOf(q.by, q.source).filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }?.joinToString("　", prefix = "— ")

// ---- 振り返り日記 ----

/** 日記の1日分を全画面で開くときの合図（LocalOpenPanel に渡す） */
private fun journalPanelKey(date: LocalDate, edit: Boolean) = "${if (edit) "edit" else "view"}:$date"

/** 習慣とタスクの間のページ。今日の日記を開くボタンと、これまでの日記の一覧 */
@Composable
fun JournalPage(store: Store, today: LocalDate) {
    val open = LocalOpenPanel.current
    val yesterday = today.minusDays(1)
    val writtenToday = store.journal.containsKey(today)
    val dates = store.journal.keys.sortedDescending()
    Column(Modifier.fillMaxSize()) {
        PageHeader(stringResource(R.string.w_journal))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "top") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(if (writtenToday) R.string.j_card_done else R.string.j_intro),
                        fontSize = 14.sp, lineHeight = 21.sp,
                        color = if (writtenToday) Blue else SubInk,
                        fontWeight = if (writtenToday) FontWeight.Bold else FontWeight.Normal,
                    )
                    BigButton(stringResource(if (writtenToday) R.string.j_open else R.string.j_write)) { open(journalPanelKey(today, true)) }
                    if (!store.journal.containsKey(yesterday)) {
                        TextButton(onClick = { open(journalPanelKey(yesterday, true)) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text(stringResource(R.string.j_yesterday), fontWeight = FontWeight.Bold)
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(stringResource(R.string.j_past), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                        if (dates.isNotEmpty()) Text(stringResource(R.string.j_count, dates.size), fontSize = 13.sp, color = SubInk)
                    }
                    if (dates.isEmpty()) Text(stringResource(R.string.j_none), fontSize = 14.sp, color = SubInk)
                }
            }
            items(dates, key = { it.toString() }) { d ->
                val e = store.journal[d] ?: return@items
                Column(Modifier.fillMaxWidth().card().clickable { open(journalPanelKey(d, false)) }.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text(longDateWithWeekday(d), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SubInk)
                    Spacer(Modifier.height(2.dp))
                    Text(e.fields().first { it.isNotBlank() }, fontSize = 15.sp, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 21.sp, style = userTextStyle())
                }
            }
        }
    }
}

/** 日記の1日分を全画面で読む・書く。書き終えたら読む画面へ、読む画面を閉じたらページに戻る */
@Composable
fun JournalPanel(store: Store, panelKey: String, onClose: () -> Unit) {
    val date = remember(panelKey) { LocalDate.parse(panelKey.substringAfter(':')) }
    var editing by remember(panelKey) { mutableStateOf(panelKey.startsWith("edit:")) }
    if (editing) {
        JournalEditor(store, date) { if (store.journal.containsKey(date)) editing = false else onClose() }
    } else {
        JournalView(store, date, onBack = onClose, onEdit = { editing = true })
    }
}

@Composable
private fun JournalView(store: Store, date: LocalDate, onBack: () -> Unit, onEdit: () -> Unit) {
    var askDelete by remember { mutableStateOf(false) }
    val e = store.journal[date] ?: run { LaunchedEffect(Unit) { onBack() }; return }
    FullPanel(dateWithWeekday(date), onBack, action = { HeaderButton(stringResource(R.string.j_edit), onClick = onEdit) }) {
        Text(longDateWithWeekday(date), fontSize = 13.sp, color = SubInk)
        e.fields().zip(JOURNAL_LABELS).filter { it.first.isNotBlank() }.forEach { (text, label) ->
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text(stringResource(label), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Blue)
                Spacer(Modifier.height(4.dp))
                Text(text, fontSize = 16.sp, color = Ink, lineHeight = 24.sp, style = userTextStyle())
            }
        }
        TextButton(onClick = { askDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(R.string.j_delete), color = Danger)
        }
    }
    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            containerColor = Bg,
            text = { Text(stringResource(R.string.j_delete_q), color = Ink, fontSize = 16.sp) },
            confirmButton = {
                TextButton(onClick = { askDelete = false; store.saveJournal(date, JournalEntry()); onBack() }) {
                    Text(stringResource(R.string.delete), color = Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun JournalEditor(store: Store, date: LocalDate, onDone: () -> Unit) {
    val values = remember(date) { mutableStateListOf<String>().apply { addAll((store.journal[date] ?: JournalEntry()).fields()) } }
    FullPanel(dateWithWeekday(date), onDone, action = { HeaderButton(stringResource(R.string.j_done), onClick = onDone) }) {
        Text(stringResource(R.string.j_autosave), fontSize = 13.sp, color = SubInk, lineHeight = 19.sp)
        JOURNAL_LABELS.forEachIndexed { i, label ->
            LabeledField(
                stringResource(label), values[i],
                hint = if (i == 0) stringResource(R.string.j_story_hint) else null,
                minLines = if (i == 0) 5 else 2,
            ) {
                values[i] = it
                store.saveJournal(date, JournalEntry.of(values))
            }
        }
    }
}

// ---- 今日の言葉 ----

@Composable
fun QuoteScreen(store: Store, today: LocalDate, onClose: () -> Unit) {
    // 書いている言葉（新しく追加するときは id が今の時刻の空の言葉）
    var editing by remember { mutableStateOf<Quote?>(null) }
    editing?.let { q -> QuoteEditor(store, q) { editing = null }; return }
    val newQuote = { editing = Quote(System.currentTimeMillis(), "") }

    FullPanel(stringResource(R.string.w_quote), onClose, action = {
        IconButton(onClick = newQuote) { Icon(Icons.Filled.Add, stringResource(R.string.q_add), tint = Blue) }
    }) {
        val q = store.quoteOf(today)
        if (q == null) {
            Text(stringResource(R.string.q_empty), fontSize = 15.sp, color = SubInk, lineHeight = 22.sp)
        } else {
            // 今日の言葉と、その詳しいこと（誰の言葉・出どころ・前後の文脈）
            Column(Modifier.fillMaxWidth().card().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("“${q.text}”", fontSize = 21.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
                    color = Ink, textAlign = TextAlign.Center, lineHeight = 31.sp, style = userTextStyle())
            }
            listOf(R.string.q_by to q.by, R.string.q_source to q.source, R.string.q_note to q.note)
                .filter { it.second.isNotBlank() }
                .forEach { (label, text) ->
                    Column(Modifier.fillMaxWidth().card().padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Text(stringResource(label), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Blue)
                        Spacer(Modifier.height(4.dp))
                        Text(text, fontSize = 16.sp, color = Ink, lineHeight = 24.sp, style = userTextStyle())
                    }
                }
            TextButton(onClick = { editing = q }, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.q_edit), fontWeight = FontWeight.Bold)
            }
        }
        BigButton("＋ " + stringResource(R.string.q_add), onClick = newQuote)
        if (store.quotes.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.q_all, store.quotes.size), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Ink)
            store.quotes.sortedByDescending { it.id }.forEach { item ->
                Row(Modifier.fillMaxWidth().card().clickable { editing = item }.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(item.text, fontSize = 15.sp, color = Ink, maxLines = 3, overflow = TextOverflow.Ellipsis, lineHeight = 21.sp, style = userTextStyle())
                        quoteCredit(item)?.let { Text(it, fontSize = 12.sp, color = SubInk, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    if (item.fav) Text("★", fontSize = 18.sp, color = Blue, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun QuoteEditor(store: Store, initial: Quote, onDone: () -> Unit) {
    var text by remember { mutableStateOf(initial.text) }
    var by by remember { mutableStateOf(initial.by) }
    var source by remember { mutableStateOf(initial.source) }
    var note by remember { mutableStateOf(initial.note) }
    var fav by remember { mutableStateOf(initial.fav) }
    val exists = store.quotes.any { it.id == initial.id }
    val current = { Quote(initial.id, text.trim(), by.trim(), source.trim(), note.trim(), fav) }
    val today = LocalDate.now()
    val isToday = exists && store.quoteOf(today)?.id == initial.id
    FullPanel(stringResource(if (exists) R.string.q_edit else R.string.q_add), onDone, action = {
        HeaderButton(stringResource(R.string.save), enabled = text.isNotBlank()) { store.saveQuote(current()); onDone() }
    }) {
        // 「今日表示する」：書いた内容を保存して、今日の言葉をこれにする（次の日は元のくじに戻る）
        if (isToday) {
            Text("✓ " + stringResource(R.string.q_showing_today), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Blue,
                modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            BigButton(stringResource(R.string.q_show_today), enabled = text.isNotBlank()) {
                store.saveQuote(current()); store.showQuoteToday(initial.id, today); onDone()
            }
        }
        // お気に入り：登録済みの言葉はその場で保存する（保存を押し忘れても残るように）
        Row(
            Modifier.fillMaxWidth().card().clickable {
                fav = !fav
                if (exists) store.quotes.firstOrNull { it.id == initial.id }?.let { store.saveQuote(it.copy(fav = fav)) }
            }.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (fav) "★" else "☆", fontSize = 24.sp, color = if (fav) Blue else SubInk)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.q_fav), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(stringResource(R.string.q_fav_note, FAV_WEIGHT), fontSize = 13.sp, color = SubInk, lineHeight = 19.sp)
            }
        }
        LabeledField(stringResource(R.string.q_text), text, minLines = 3) { text = it }
        LabeledField(stringResource(R.string.q_by), by, hint = stringResource(R.string.q_by_hint), minLines = 1) { by = it }
        LabeledField(stringResource(R.string.q_source), source, hint = stringResource(R.string.q_source_hint), minLines = 1) { source = it }
        LabeledField(stringResource(R.string.q_note), note, hint = stringResource(R.string.q_note_hint), minLines = 3) { note = it }
        if (exists) {
            TextButton(onClick = { store.deleteQuote(initial.id); onDone() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.q_delete), color = Danger)
            }
        }
    }
}
