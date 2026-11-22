package com.enakagami.lifelauncher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * おやすみ時間・集中タイムの間に書くメモ（その間に思いついたことを忘れないように）。
 * 書いたメモは、その時間が終わったときに MemoScreen で出す。「閉じる」を押すまで消えない。
 */
@Composable
fun ModeMemoField(kind: String, text: Color) {
    OutlinedTextField(
        textStyle = userTextStyle().copy(color = text, fontSize = 16.sp),
        value = Guard.memo(kind),
        onValueChange = { Guard.setMemo(kind, it.take(1000)) },
        placeholder = { Text(stringResource(R.string.memo_hint), color = text.copy(alpha = 0.55f), fontSize = 15.sp) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = text.copy(alpha = 0.8f), unfocusedBorderColor = text.copy(alpha = 0.35f),
            focusedContainerColor = text.copy(alpha = 0.06f), unfocusedContainerColor = text.copy(alpha = 0.06f),
            cursorColor = text,
        ),
    )
}

/**
 * 終わったあとにメモを見せる画面。ホーム画面の代わりに出し、「閉じる」でしか消えない
 * （画面の端を押す・戻るボタン・ホームボタンでは消えない。読む前に消えてしまわないように）。
 */
@Composable
fun MemoScreen(kind: String) {
    BackHandler(enabled = true) { }
    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Text(
                stringResource(if (kind == "night") R.string.memo_title_night else R.string.memo_title_focus),
                color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                Guard.memo(kind), color = Ink, fontSize = 18.sp, lineHeight = 28.sp, style = userTextStyle(),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(CardBg).padding(20.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.memo_note), color = SubInk, fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(14.dp)).background(Blue)
                    .clickable { Guard.clearMemo(kind) },
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.close), color = OnAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(64.dp))
        }
    }
}
