package com.enakagami.lifelauncher

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime

/**
 * ロック解除時の確認。答えるまでホーム画面の代わりに出し続ける。
 * - 「理由を書く」（Guard.askMode = "write"）：① 今していること ② なぜ開くか を自分の言葉で書く。
 *   「なんとなく」は受け付けない（無意識に触るのを減らすためのアプリなので）。答えは記録に残り、AI にも渡す
 * - 「算数を解く」（"math"）：ちょっと頭を使う計算を1問解く（MathQuiz）
 * どちらも、スマホの使用時間を減らすのではなく、無意識にスマホを触るのを減らして自由な時間を取り戻すためのもの。
 */
@Composable
fun AskScreen(now: LocalTime) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        val today = LocalDate.now()
        Text("%d:%02d".format(now.hour, now.minute), color = SubInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(
            stringResource(if (Guard.askMode == "math") R.string.math_title else R.string.ask_title),
            color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.ask_today, durationText(Guard.screenMinutes(today)), Guard.unlockCount(today)),
            color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        )
        if (Guard.askMode == "math") MathAsk() else WriteAsk()
        // 画面の一番下（ジェスチャーの場所）に重ならないよう余白を取る
        Spacer(Modifier.height(64.dp))
    }
}

/** 「なんとなく」のような、理由になっていない答えか */
private fun isVague(context: Context, text: String): Boolean {
    val t = text.lowercase().replace(" ", "").replace("　", "")
    if (t.isEmpty()) return false
    // スマホの言語の言い方に加えて、日本語と英語の言い方はいつも見る
    val words = (context.getString(R.string.ask_vague_words) + ",なんとなく,何となく,ナントナク,noreason,justbecause")
        .split(",").map { it.lowercase().replace(" ", "").trim() }.filter { it.isNotEmpty() }
    return words.any { t.contains(it) }
}

@Composable
private fun WriteAsk() {
    val context = LocalContext.current
    var doing by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    val vague = isVague(context, purpose)
    Text(stringResource(R.string.ask_sub), color = SubInk, fontSize = 13.sp)

    Spacer(Modifier.height(24.dp))
    Text(stringResource(R.string.ask_q1), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    AnswerField(stringResource(R.string.ask_q1_hint), doing) { doing = it }

    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.ask_q2), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    AnswerField(stringResource(R.string.ask_q2_hint), purpose) { purpose = it }
    if (vague) {
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.ask_vague), color = Danger, fontSize = 13.sp, lineHeight = 19.sp)
    }

    Spacer(Modifier.height(24.dp))
    Button(
        onClick = { Guard.answer(doing.trim(), purpose.trim()) },
        enabled = doing.isNotBlank() && purpose.isNotBlank() && !vague,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = OnAccent),
    ) { Text(stringResource(R.string.ask_submit), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
    Spacer(Modifier.height(8.dp))
    MoreInfo(stringResource(R.string.ask_hint))
}

@Composable
private fun AnswerField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        textStyle = userTextStyle(),
        value = value,
        onValueChange = { onChange(it.replace("\n", "").take(100)) },
        placeholder = { Text(label, color = SubInk) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardBg, unfocusedContainerColor = CardBg),
    )
}

/** 算数を1問解く。数字は画面のボタンで入れる（キーボードを出さない）。3回まちがえたら別の問題にする */
@Composable
private fun MathAsk() {
    var problem by remember { mutableStateOf(MathQuiz.make()) }
    var input by remember { mutableStateOf("") }
    var misses by remember { mutableIntStateOf(0) }
    var wrong by remember { mutableStateOf(false) }
    Text(stringResource(R.string.math_sub), color = SubInk, fontSize = 13.sp)

    Spacer(Modifier.height(28.dp))
    Text(
        problem.text + " = ?",
        color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(16.dp))
    Box(
        Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(14.dp)).background(CardBg),
        contentAlignment = Alignment.Center,
    ) {
        Text(input.ifEmpty { " " }, color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(6.dp))
    Text(
        if (wrong) stringResource(R.string.math_wrong) else " ",
        color = Danger, fontSize = 14.sp, fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(10.dp))
    val del = stringResource(R.string.math_delete)
    val okText = stringResource(R.string.math_ok)
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("⌫", "0", "OK"))
    rows.forEach { row ->
        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { k ->
                val ok = k == "OK"
                Box(
                    Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (ok) Blue else CardBg)
                        .semantics { contentDescription = when (k) { "⌫" -> del; "OK" -> okText; else -> k } }
                        .clickable {
                            when (k) {
                                "⌫" -> { input = input.dropLast(1); wrong = false }
                                "OK" -> if (input.isNotEmpty()) {
                                    if (input.toIntOrNull() == problem.answer) Guard.solvedMath()
                                    else {
                                        wrong = true; input = ""; misses++
                                        if (misses >= 3) { problem = MathQuiz.make(); misses = 0 }
                                    }
                                }
                                else -> if (input.length < 4) { input += k; wrong = false }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (ok) okText else k,
                        color = if (ok) OnAccent else Ink, fontSize = if (ok) 18.sp else 24.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
