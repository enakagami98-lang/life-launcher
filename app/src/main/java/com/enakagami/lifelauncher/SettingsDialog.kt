package com.enakagami.lifelauncher

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate

@Composable
fun SettingsDialog(store: Store, apps: List<AppInfo>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val b = store.birth
    var year by remember { mutableStateOf(b?.year?.toString() ?: "") }
    var month by remember { mutableStateOf(b?.monthValue?.toString() ?: "") }
    var day by remember { mutableStateOf(b?.dayOfMonth?.toString() ?: "") }
    var life by remember { mutableStateOf(store.lifespan.toString()) }

    val parsed = runCatching { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()
    val lifeNum = life.toIntOrNull()
    val birthOk = parsed != null && parsed.year >= 1900 && !parsed.isAfter(LocalDate.now())
    val lifeOk = lifeNum != null && lifeNum in 1..150 && (parsed == null || parsed.plusYears(lifeNum.toLong()).isAfter(LocalDate.now()))
    val valid = birthOk && lifeOk
    val error = when {
        year.isEmpty() || month.isEmpty() || day.isEmpty() -> null
        !birthOk -> "生年月日を正しく入力してください（例：1999年4月29日）"
        life.isNotEmpty() && !lifeOk -> "寿命は今の年齢より大きい数にしてください"
        else -> null
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState())) {
                PageHeader("設定") {
                    RoundButton(Icons.Filled.Close, "閉じる", CardBg, Ink, onDismiss)
                }

                Section("生年月日と寿命")
                Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().card().padding(20.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("年", year, { year = it }, Modifier.weight(1.4f))
                        NumberField("月", month, { month = it }, Modifier.weight(1f))
                        NumberField("日", day, { day = it }, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    NumberField("寿命（歳）", life, { life = it }, Modifier.fillMaxWidth())
                    if (error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(error, color = Danger, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { store.saveProfile(parsed!!, lifeNum!!); onDismiss() },
                        enabled = valid,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    ) { Text("保存", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                }

                Section("ホームアプリ")
                Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().card().padding(20.dp)) {
                    Text(
                        "このアプリを「ホームアプリ」に設定すると、ロック解除後やホームボタンを押したときに必ずこの画面が出ます。",
                        fontSize = 14.sp, color = SubInk,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            runCatching { context.startActivity(intent) }.onFailure {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue),
                    ) { Text("ホームアプリの設定を開く", fontWeight = FontWeight.Bold) }
                }

                Section("非表示にしたアプリ")
                Column(Modifier.padding(horizontal = 20.dp).fillMaxWidth().card().padding(vertical = 8.dp)) {
                    if (store.hidden.isEmpty()) {
                        Text(
                            "まだありません。アプリ一覧で長押しすると非表示にできます。",
                            fontSize = 14.sp, color = SubInk,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                    store.hidden.toList().forEach { pkg ->
                        val label = apps.firstOrNull { it.pkg == pkg }?.label ?: pkg
                        Row(
                            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label, fontSize = 16.sp, color = Ink, modifier = Modifier.weight(1f))
                            TextButton(onClick = { store.unhide(pkg) }) { Text("表示に戻す") }
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = SubInk,
        modifier = Modifier.padding(start = 28.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(4)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
