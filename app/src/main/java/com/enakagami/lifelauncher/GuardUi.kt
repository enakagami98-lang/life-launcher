package com.enakagami.lifelauncher

import android.content.Intent
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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
                    Text(stringResource(R.string.guard_used_up, limit), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                    Spacer(Modifier.height(24.dp))
                }
                !waited -> {
                    Text(
                        if (expired) stringResource(R.string.guard_extend) else stringResource(R.string.guard_really, label),
                        color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp,
                    )
                    Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
                        LooseningKnot(progress, Modifier.size(150.dp))
                    }
                }
                else -> {
                    Text(stringResource(R.string.guard_how_long, label), color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.guard_remain, remainMin, limit), color = SubInk, fontSize = 13.sp)
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
                                Text(stringResource(R.string.unit_min), color = SubInk, fontSize = 13.sp)
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
            ) { Text(stringResource(R.string.close), color = Ink, fontSize = 17.sp) }
        }
    }
}

/**
 * 待ち時間のアニメーション：固く結ばれた結び目（三つ葉結び）が、待ち時間いっぱいをかけて、ゆるんだ輪になる。
 * わざと退屈にしている。数字は出さず、動きは一定の速さで小さく、色も薄い。
 * 見ていて面白いと、待ち時間があっという間に過ぎてしまうため（「待つのが面倒だから開くのをやめよう」と思ってもらう）
 */
@Composable
private fun LooseningKnot(progress: Float, modifier: Modifier) {
    val color = Ink.copy(alpha = 0.55f)
    Canvas(modifier) {
        val scale = size.minDimension / 10f
        val path = Path()
        val n = 360
        for (i in 0..n) {
            val a = 2 * Math.PI * i / n
            // 三つ葉結び → 円
            val kx = Math.sin(a) + 2 * Math.sin(2 * a)
            val ky = Math.cos(a) - 2 * Math.cos(2 * a)
            val x = kx + (Math.sin(a) * 2.4 - kx) * progress
            val y = ky + (Math.cos(a) * 2.4 - ky) * progress
            val px = center.x + (x * scale).toFloat()
            val py = center.y - (y * scale).toFloat()
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** 使えない時間帯に、ホーム画面の代わりに出す画面。夜のおやすみ時間は、設定の色に関係なく夜空のデザインに統一する */
@Composable
fun LockScreen(name: String, until: String, now: LocalTime, night: Boolean) {
    if (night) { NightLockScreen(name, until); return }
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
        Text(stringResource(R.string.lock_until, until), color = SubInk, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(48.dp))
        CallButton(Ink) { Guard.allowUntil = System.currentTimeMillis() + 10 * 60_000; dial(context) }
    }
}

private val NightTop = Color(0xFF0F1730)
private val NightBottom = Color(0xFF1C2E5C)
private val NightText = Color(0xFFFFF7DA)
private val Moon = Color(0xFFF2B84B)

@Composable
private fun NightLockScreen(name: String, until: String) {
    val context = LocalContext.current
    // 夜空の上では、時計や電池のアイコンを白にする（この画面を閉じたら元に戻す）
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val bars = window?.let { WindowCompat.getInsetsController(it, view) }
        val before = bars?.isAppearanceLightStatusBars
        bars?.isAppearanceLightStatusBars = false
        onDispose { before?.let { bars.isAppearanceLightStatusBars = it } }
    }
    // 「%1$s まではスマホを使えません」を、時刻の前と後ろに分けて、時刻だけ大きく出す（どの言語でも同じ文字を使う）
    val parts = stringResource(R.string.lock_until, "\u0000").split("\u0000")
    val time = until.removePrefix("0")
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NightTop, NightBottom))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            // 三日月：黄色い円から、少しずらした円をくり抜く
            Canvas(Modifier.size(72.dp).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
                val r = size.minDimension / 2.2f
                drawCircle(Moon, r, center)
                drawCircle(Color.Black, r * 0.92f, center + Offset(r * 0.55f, -r * 0.35f), blendMode = BlendMode.Clear)
            }
            Spacer(Modifier.height(20.dp))
            Text(name, color = NightText.copy(alpha = 0.8f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (parts.first().isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(parts.first().trim(), color = NightText.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            Text(time, color = NightText, fontSize = 76.sp, fontWeight = FontWeight.Black)
            if (parts.size > 1 && parts[1].isNotBlank()) {
                Text(parts[1].trim(), color = NightText.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp)) {
            CallButton(NightText.copy(alpha = 0.85f)) { Guard.allowUntil = System.currentTimeMillis() + 10 * 60_000; dial(context) }
        }
    }
}

@Composable
private fun CallButton(color: Color, onClick: () -> Unit) {
    Text(
        stringResource(R.string.lock_call),
        color = color,
        fontSize = 16.sp,
        modifier = Modifier
            .border(1.5.dp, color, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 12.dp),
    )
}

private fun dial(context: android.content.Context) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
