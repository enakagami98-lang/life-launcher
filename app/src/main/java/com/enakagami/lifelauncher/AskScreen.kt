package com.enakagami.lifelauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime

/**
 * ロック解除時の「今何してる？」。答えるまでホーム画面の代わりに出し続ける。
 * ① 今していること ② スマホを開いた理由 をボタンで選ぶ（無いときは文字で書く）。
 * ①を選んでから②のボタンを押すと、それだけで記録して閉じる（最短2タップ）。
 * 選択肢は「選択肢を編集」で登録・削除できる（Guard.doingOptions / purposeOptions）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AskScreen(now: LocalTime) {
    var doing by remember { mutableStateOf("") }
    var doingOther by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var purposeOther by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        Text("%d:%02d".format(now.hour, now.minute), color = SubInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.ask_title), color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.ask_today, durationText(Guard.screenMinutes(today)), Guard.unlockCount(today)),
            color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        )
        Text(stringResource(R.string.ask_sub), color = SubInk, fontSize = 13.sp)

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.ask_q1), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Guard.doingOptions.forEach { o ->
                val label = optionLabel(o)
                AnswerChip(label, doing == label && doingOther.isBlank()) { doing = label; doingOther = "" }
            }
        }
        Spacer(Modifier.height(8.dp))
        OtherField(stringResource(R.string.ask_other), doingOther) { doingOther = it; doing = it.trim() }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.ask_q2), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Guard.purposeOptions.forEach { o ->
                val label = optionLabel(o)
                AnswerChip(label, purpose == label && purposeOther.isBlank()) {
                    purpose = label; purposeOther = ""
                    // ①が決まっていれば、ここで記録して終わり
                    if (doing.isNotBlank()) Guard.answer(doing, label)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OtherField(stringResource(R.string.ask_other), purposeOther) { purposeOther = it; purpose = it.trim() }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { Guard.answer(doing, purpose) },
            enabled = doing.isNotBlank() && purpose.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = OnAccent),
        ) { Text(stringResource(R.string.ask_submit), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.ask_hint), color = SubInk, fontSize = 12.sp, lineHeight = 18.sp)
        TextButton(onClick = { editing = true }) { Text(stringResource(R.string.opt_edit), fontWeight = FontWeight.Bold) }
        // 画面の一番下（ジェスチャーの場所）に重ならないよう余白を取る
        Spacer(Modifier.height(64.dp))
    }
    if (editing) OptionsEditDialog { editing = false }
}

/** 「今何してる？」の選択肢を登録・削除する */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OptionsEditDialog(onDismiss: () -> Unit) {
    var newDoing by remember { mutableStateOf("") }
    var newPurpose by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Bg,
        title = { Text(stringResource(R.string.opt_edit), fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.opt_edit_hint), fontSize = 13.sp, color = SubInk, lineHeight = 19.sp)
                Spacer(Modifier.height(12.dp))
                OptionSection(stringResource(R.string.ask_q1), doing = true, newDoing, { newDoing = it })
                Spacer(Modifier.height(16.dp))
                OptionSection(stringResource(R.string.ask_q2), doing = false, newPurpose, { newPurpose = it })
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { Guard.resetOptions() }) { Text(stringResource(R.string.opt_reset), color = Danger) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), fontWeight = FontWeight.Bold) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionSection(title: String, doing: Boolean, input: String, onInput: (String) -> Unit) {
    Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Guard.optionList(doing).toList().forEach { o ->
            val label = optionLabel(o)
            val remove = stringResource(R.string.opt_remove, label)
            Row(
                Modifier.clip(RoundedCornerShape(18.dp)).border(1.dp, SubInk, RoundedCornerShape(18.dp))
                    .clickable { Guard.removeOption(doing, o) }
                    .semantics { contentDescription = remove }
                    .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, fontSize = 14.sp, color = Ink)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.Close, null, tint = SubInk, modifier = Modifier.size(14.dp))
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = input,
            onValueChange = { onInput(it.replace("\n", "").take(20)) },
            placeholder = { Text(stringResource(R.string.opt_new)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = { Guard.addOption(doing, input); onInput("") },
            enabled = input.isNotBlank(),
        ) { Text(stringResource(R.string.add), fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun AnswerChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) OnAccent else Ink,
        fontSize = 15.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Blue else Color.Transparent)
            .border(1.dp, if (selected) Blue else SubInk, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun OtherField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.replace("\n", "")) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardBg, unfocusedContainerColor = CardBg),
    )
}
