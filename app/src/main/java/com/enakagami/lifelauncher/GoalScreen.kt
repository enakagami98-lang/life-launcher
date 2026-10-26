package com.enakagami.lifelauncher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/**
 * 夜のおやすみ時間が終わって最初にスマホを開いたときの「今日の目標」。
 * 決めるまではホーム画面の代わりにこの画面を出し続け、ほかのアプリを開いても戻す（電話は使える）。
 */
@Composable
fun GoalScreen() {
    val today = LocalDate.now()
    var text by remember { mutableStateOf("") }
    val yesterday = Guard.dailyGoal(today.minusDays(1))
    val submit = { if (text.isNotBlank()) Guard.setDailyGoal(today, text) }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text(dateWithWeekday(today), color = SubInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.goal_title), color = Ink, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.goal_sub), color = SubInk, fontSize = 15.sp, lineHeight = 22.sp)
        if (yesterday.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.goal_yesterday, yesterday), color = Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it.replace("\n", "").take(60) },
            placeholder = { Text(stringResource(R.string.goal_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = CardBg, unfocusedContainerColor = CardBg),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { submit() },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = OnAccent),
        ) { Text(stringResource(R.string.goal_start), fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Spacer(Modifier.height(10.dp))
        MoreInfo(stringResource(R.string.goal_note))
        Spacer(Modifier.height(64.dp))
    }
}
