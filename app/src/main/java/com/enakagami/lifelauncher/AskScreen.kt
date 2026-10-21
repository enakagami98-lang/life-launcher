package com.enakagami.lifelauncher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime

private val DoingOptions = listOf("勉強", "仕事・作業", "授業・会議", "移動中", "食事中", "休憩中", "家でくつろぎ", "買い物", "音楽を聴く", "ジム", "読書", "寝る前", "起きた直後")
private val PurposeOptions = listOf("連絡（LINEなど）", "SNS", "動画", "調べもの", "支払い", "予定・メモ", "時間を見るだけ", "通知が来た", "ゲーム", "なんとなく")

/**
 * ロック解除時の「今何してる？」。答えるまでホーム画面の代わりに出し続ける。
 * ① 今していること ② スマホを開いた理由 をボタンで選ぶ（無いときは文字で書く）。
 * ①を選んでから②のボタンを押すと、それだけで記録して閉じる（最短2タップ）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AskScreen(now: LocalTime) {
    var doing by remember { mutableStateOf("") }
    var doingOther by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var purposeOther by remember { mutableStateOf("") }
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
        Text("今何してる？", color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text(
            "今日 ${durationText(Guard.screenMinutes(today))}・${Guard.unlockCount(today)}回目のロック解除",
            color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        )
        Text("答えるとスマホを使えます", color = SubInk, fontSize = 13.sp)

        Spacer(Modifier.height(24.dp))
        Text("① 今していること", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DoingOptions.forEach { o ->
                AnswerChip(o, doing == o && doingOther.isBlank()) { doing = o; doingOther = "" }
            }
        }
        Spacer(Modifier.height(8.dp))
        OtherField("ボタンに無いときはここに書く", doingOther) { doingOther = it; doing = it.trim() }

        Spacer(Modifier.height(24.dp))
        Text("② スマホを開いた理由", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PurposeOptions.forEach { o ->
                AnswerChip(o, purpose == o && purposeOther.isBlank()) {
                    purpose = o; purposeOther = ""
                    // ①が決まっていれば、ここで記録して終わり
                    if (doing.isNotBlank()) Guard.answer(doing, o)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OtherField("ボタンに無いときはここに書く", purposeOther) { purposeOther = it; purpose = it.trim() }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { Guard.answer(doing, purpose) },
            enabled = doing.isNotBlank() && purpose.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = OnAccent),
        ) { Text("記録してスマホを使う", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.height(8.dp))
        Text(
            "①を選んでから②のボタンを押すと、すぐに記録されます。この質問は「今日の調子」の画面でオフにできます。",
            color = SubInk, fontSize = 12.sp, lineHeight = 18.sp,
        )
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
