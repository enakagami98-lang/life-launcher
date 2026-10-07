package com.enakagami.lifelauncher

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.time.LocalTime

/** reason: "open"=ホームから開こうとした / "session"=時間切れ・履歴などから開いた */
data class GuardRequest(val pkg: String, val key: String?, val reason: String, val id: Long = System.nanoTime())

/** 少し待ってから、何分使うかを選ぶ画面（マインドフル・スロー・ローンチ） */
@Composable
fun GuardDialog(req: GuardRequest, label: String, onLaunch: (Int) -> Unit, onClose: () -> Unit) {
    val limit = Guard.limits[req.pkg] ?: 0
    val remainMs = Guard.remainingMs(req.pkg)
    // 残り30秒未満は「使い切った」扱い（数秒だけ延長しても意味がないため）
    val remainMin = if (remainMs < 30_000) 0 else ((remainMs + 59_999) / 60_000).toInt()
    val expired = req.reason == "session" && Guard.hadSession(req.pkg)
    var progress by remember(req.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(req.id) {
        val total = Guard.waitSec * 1000L
        val start = System.currentTimeMillis()
        while (true) {
            progress = ((System.currentTimeMillis() - start).toFloat() / total).coerceAtMost(1f)
            if (progress >= 1f) break
            delay(50)
        }
    }
    val waited = progress >= 1f

    Dialog(onDismissRequest = onClose, properties = DialogProperties(dismissOnClickOutside = false)) {
        Column(
            Modifier.fillMaxWidth()
                .background(CardBg, RoundedCornerShape(32.dp))
                .border(1.dp, SubInk.copy(alpha = 0.4f), RoundedCornerShape(32.dp))
                .padding(26.dp),
        ) {
            Text(label, color = SubInk, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            when {
                remainMin <= 0 -> {
                    Text("今日の使用時間（${limit}分）を使い切りました。\nまた明日。", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                    Spacer(Modifier.height(24.dp))
                }
                !waited -> {
                    Text(
                        if (expired) "時間になりました。延長しますか？" else "${label}を本当に開きますか？",
                        color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp,
                    )
                    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(96.dp), color = Ink, strokeWidth = 4.dp, trackColor = CardBg)
                    }
                }
                else -> {
                    Text("${label}にどのくらいの時間を使いますか？", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("今日の残り ${remainMin}分（1日 ${limit}分）", color = SubInk, fontSize = 13.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(5, 10, 15).forEach { m ->
                            Column(
                                Modifier.weight(1f)
                                    .background(Bg.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                    .clickable { onLaunch(m) }
                                    .padding(vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("$m", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                                Text("分", color = SubInk, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                border = BorderStroke(1.5.dp, Ink),
            ) { Text("閉じる", color = Ink, fontSize = 17.sp) }
        }
    }
}

/** 使えない時間帯に、ホーム画面の代わりに出す画面 */
@Composable
fun LockScreen(name: String, until: String, now: LocalTime) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("%02d:%02d".format(now.hour, now.minute), color = Ink, fontSize = 72.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Text(name, color = Blue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("$until まではスマホを使えません", color = SubInk, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(48.dp))
        Text(
            "電話をかける",
            color = Ink,
            fontSize = 16.sp,
            modifier = Modifier
                .border(1.5.dp, Ink, RoundedCornerShape(24.dp))
                .clickable {
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
                .padding(horizontal = 28.dp, vertical = 12.dp),
        )
    }
}
