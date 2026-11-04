package com.enakagami.lifelauncher

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** 上の端がギザギザ（ミシン目からちぎった跡）の紙 */
private val TornPaper = GenericShape { size, _ ->
    val teeth = 18
    val w = size.width / teeth
    val h = 7f * (size.width / 600f).coerceAtLeast(1f)
    moveTo(0f, h)
    for (i in 0 until teeth) {
        lineTo(w * i + w / 2, 0f)
        lineTo(w * (i + 1), h)
    }
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

/**
 * 日めくり：その日に初めて「今日の目標」かタスクを登録したとき、昨日の残り日数の紙を1枚ちぎって、
 * 今日の数字を見せる（数字が大きくても1日減ったのを目で感じられるように）。文字の説明は出さない。
 * 誕生日が未設定なら、カレンダーの日付（昨日→今日）でめくる
 */
@Composable
fun TearOverlay(store: Store, today: LocalDate, onEnd: () -> Unit) {
    val birth = store.birth
    val (label, before, after) = if (birth != null) {
        val days = ChronoUnit.DAYS.between(today, birth.plusYears(store.lifespan.toLong())).coerceAtLeast(0)
        Triple(stringResource(R.string.days_left), "%,d".format(days + 1), "%,d".format(days))
    } else {
        Triple(today.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()), today.minusDays(1).dayOfMonth.toString(), today.dayOfMonth.toString())
    }
    val haptic = LocalHapticFeedback.current
    val scrim = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }   // ちぎる前に少しめくれる
    val fall = remember { Animatable(0f) }   // ちぎれて落ちる
    LaunchedEffect(Unit) {
        scrim.animateTo(1f, tween(250))
        delay(450)
        lift.animateTo(1f, tween(380))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        launch { fall.animateTo(1f, tween(750, easing = FastOutLinearInEasing)) }
        delay(1700)
        scrim.animateTo(0f, tween(300))
        onEnd()
    }

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = scrim.value }
            .background(Bg.copy(alpha = 0.94f))
            .clickable(remember { MutableInteractionSource() }, null) { onEnd() },
        contentAlignment = Alignment.Center,
    ) {
        val paper = if (LocalPalette.current.dark) Color(0xFFF4F1EA) else Color.White
        val paperInk = Color(0xFF16213E)
        Column(Modifier.width(250.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // とじ具（紙の上に置く。ちぎった紙はここから離れていく）
            Box(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)).background(Blue)) {
                Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(120.dp)) {
                    repeat(2) { Box(Modifier.size(12.dp).clip(CircleShape).background(Bg)) }
                }
            }
            Box(Modifier.fillMaxWidth().height(250.dp)) {
                // 下の紙＝今日
                Sheet(label, after, paper, paperInk, Modifier.shadow(6.dp, RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                    .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)))
                // 上の紙＝昨日。右上を軸に少しめくれてから、回りながら下へ落ちる
                Sheet(
                    label, before, paper, paperInk,
                    Modifier.graphicsLayer {
                        transformOrigin = TransformOrigin(1f, 0f)
                        rotationZ = -6f * lift.value - 28f * fall.value
                        rotationX = 18f * lift.value
                        translationY = fall.value * 1400f
                        translationX = -fall.value * 120f
                        alpha = 1f - fall.value * 0.6f
                        shadowElevation = 12f + 24f * lift.value
                        shape = if (fall.value > 0f) TornPaper else RoundedCornerShape(0.dp)
                        clip = true
                    },
                )
            }
        }
    }
}

@Composable
private fun Sheet(label: String, number: String, paper: Color, ink: Color, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().background(paper).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ink.copy(alpha = 0.7f))
        Text(
            number,
            fontSize = if (number.length > 5) 58.sp else 80.sp,
            fontWeight = FontWeight.Black, color = ink, lineHeight = 90.sp,
            modifier = Modifier.offset(y = 2.dp),
        )
    }
}
