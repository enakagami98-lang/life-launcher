package com.enakagami.lifelauncher

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt
import java.time.format.TextStyle
import java.util.Locale

/** 1日分の「今日の調子」。点数は 0〜10、未入力は null */
data class DayLog(
    val overall: Int? = null,
    val sleep: Int? = null,
    val focus: Int? = null,
    val mood: Int? = null,
    val sleepHours: Float? = null,
    val breakfast: String = "",
    val lunch: String = "",
    val dinner: String = "",
    val memo: String = "",
) {
    fun isEmpty() = this == DayLog()

    fun toJson(): JSONObject = JSONObject().apply {
        overall?.let { put("overall", it) }
        sleep?.let { put("sleep", it) }
        focus?.let { put("focus", it) }
        mood?.let { put("mood", it) }
        sleepHours?.let { put("sleepHours", it.toDouble()) }
        put("breakfast", breakfast).put("lunch", lunch).put("dinner", dinner).put("memo", memo)
    }

    companion object {
        fun fromJson(o: JSONObject) = DayLog(
            overall = o.optInt("overall", -1).takeIf { it >= 0 },
            sleep = o.optInt("sleep", -1).takeIf { it >= 0 },
            focus = o.optInt("focus", -1).takeIf { it >= 0 },
            mood = o.optInt("mood", -1).takeIf { it >= 0 },
            sleepHours = o.optDouble("sleepHours", -1.0).takeIf { it >= 0 }?.toFloat(),
            breakfast = o.optString("breakfast"),
            lunch = o.optString("lunch"),
            dinner = o.optString("dinner"),
            memo = o.optString("memo"),
        )
    }
}

/** 寿命の画面から右へ（指を左→右に）スワイプした先の画面 */
@Composable
fun ConditionPage(store: Store, today: LocalDate) {
    var date by remember { mutableStateOf(today) }
    val log = store.day(date)
    val save: (DayLog) -> Unit = { store.saveDay(date, it) }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column(Modifier.padding(start = 4.dp, top = 12.dp)) {
                Text("今日の調子", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
                DateSwitcher(date, today, onChange = { date = it })
            }
        }
        item { ScreenTimeCard(date, today) }
        item {
            ScoreCard("今日の調子", "1日を振り返って総合的に", log.overall) { save(log.copy(overall = it)) }
        }
        item {
            Column(Modifier.fillMaxWidth().card().padding(20.dp)) {
                ScoreRow("睡眠の質", "ぐっすり眠れたか", log.sleep) { save(log.copy(sleep = it)) }
                Spacer(Modifier.height(16.dp))
                SleepHoursRow(log.sleepHours) { save(log.copy(sleepHours = it)) }
            }
        }
        item {
            ScoreCard("集中度", "やるべきことに集中できたか", log.focus) { save(log.copy(focus = it)) }
        }
        item {
            ScoreCard("気分", "前向きな気持ちだったか", log.mood) { save(log.copy(mood = it)) }
        }
        item {
            Column(Modifier.fillMaxWidth().card().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("食べたもの・メモ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                MemoField("朝ごはん", log.breakfast) { save(log.copy(breakfast = it)) }
                MemoField("昼ごはん", log.lunch) { save(log.copy(lunch = it)) }
                MemoField("夜ごはん", log.dinner) { save(log.copy(dinner = it)) }
                MemoField("メモ（運動・出来事・飲んだものなど）", log.memo) { save(log.copy(memo = it)) }
            }
        }
        item { UnlockLogCard(date) }
        item { TrendCard(store.days) }
        item { AiPromptCard(store) }
    }
}

@Composable
private fun DateSwitcher(date: LocalDate, today: LocalDate, onChange: (LocalDate) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(date.minusDays(1)) }) {
            Icon(Icons.Filled.KeyboardArrowLeft, "前の日", tint = Ink)
        }
        val label = when (date) {
            today -> "今日"
            today.minusDays(1) -> "昨日"
            else -> null
        }
        Text(
            "${date.monthValue}月${date.dayOfMonth}日（${weekday(date.dayOfWeek)}）" + (label?.let { "・$it" } ?: ""),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (date == today) Blue else Ink,
        )
        IconButton(onClick = { onChange(date.plusDays(1)) }, enabled = date < today) {
            Icon(Icons.Filled.KeyboardArrowRight, "次の日", tint = if (date < today) Ink else SubInk.copy(alpha = 0.25f))
        }
    }
}

private fun weekday(d: DayOfWeek) = d.getDisplayName(TextStyle.SHORT, Locale.JAPANESE)

/** 192 → 「3時間12分」 */
fun durationText(minutes: Int) = if (minutes < 60) "${minutes}分" else "${minutes / 60}時間${minutes % 60}分"

// ---- スマホの使用時間 ----

@Composable
private fun ScreenTimeCard(date: LocalDate, today: LocalDate) {
    val minutes = Guard.screenMinutes(date)
    val unlocks = Guard.unlockCount(date)
    Column(Modifier.fillMaxWidth().card().padding(20.dp)) {
        Text(
            if (date == today) "今日のスマホ使用時間" else "この日のスマホ使用時間",
            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            if (minutes >= 60) {
                Text("${minutes / 60}", fontSize = 64.sp, fontWeight = FontWeight.Black, color = Blue, lineHeight = 64.sp)
                Text("時間", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Blue, modifier = Modifier.padding(bottom = 10.dp, end = 8.dp))
            }
            Text("${minutes % 60}", fontSize = 64.sp, fontWeight = FontWeight.Black, color = Blue, lineHeight = 64.sp)
            Text("分", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Blue, modifier = Modifier.padding(bottom = 10.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text("ロック解除 ${unlocks}回", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SubInk)
        Spacer(Modifier.height(10.dp))
        MonthCompare(date, today)
        Spacer(Modifier.height(6.dp))
        Text(
            "画面をつけてロックを解除していた時間です（このアプリの見守りが動いている間だけ数えます）。",
            fontSize = 11.sp, color = SubInk, lineHeight = 16.sp,
        )
    }
}

/**
 * 先月との比較（1日あたりの平均）。記録のある日だけで平均する（入れた日より前や、見守りが止まっていた日で薄まらないように）。
 * 今日はまだ途中なので、今月のほかの日に記録があれば今日は平均に入れない。
 */
@Composable
private fun MonthCompare(date: LocalDate, today: LocalDate) {
    val month = YearMonth.from(date)
    fun avg(m: YearMonth, skipToday: Boolean): Int? {
        val xs = (1..m.lengthOfMonth()).map { m.atDay(it) }
            .filter { !(skipToday && it == today) }
            .map { Guard.screenMinutes(it) }.filter { it > 0 }
        return if (xs.isEmpty()) null else xs.average().roundToInt()
    }
    val now = avg(month, skipToday = true) ?: avg(month, skipToday = false)
    val last = avg(month.minusMonths(1), skipToday = false)
    val label = if (month == YearMonth.from(today)) "今月" else "${month.monthValue}月"

    Column(Modifier.fillMaxWidth().background(Bg, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
        when {
            now == null -> Text("この月の記録はまだありません。", fontSize = 13.sp, color = SubInk)
            last == null -> {
                Text("先月の記録がたまると、ここに先月との比較が出ます。", fontSize = 13.sp, color = SubInk, lineHeight = 19.sp)
                Text("${label}の1日平均 ${durationText(now)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            else -> {
                val pct = ((now - last) * 100.0 / last).roundToInt()
                Text(
                    when {
                        pct < 0 -> "先月より使用時間が平均 ${-pct}% 減っています"
                        pct > 0 -> "先月より使用時間が平均 ${pct}% 増えています"
                        else -> "先月と同じくらいです"
                    },
                    fontSize = 16.sp, fontWeight = FontWeight.Black, color = if (pct > 0) Danger else Blue, lineHeight = 22.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text("1日平均　${label} ${durationText(now)} ／ 先月 ${durationText(last)}", fontSize = 13.sp, color = SubInk)
            }
        }
    }
}

// ---- ロック解除時の質問の記録 ----

@Composable
private fun UnlockLogCard(date: LocalDate) {
    val context = LocalContext.current
    val resume = LocalResumeSignal.current
    val overlayOk = remember(resume) { Guard.hasOverlay(context) }
    val logs = Guard.logsOn(date)

    Column(Modifier.fillMaxWidth().card().padding(20.dp)) {
        Text("スマホを開いた記録", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "ロック解除のたびに「今何してる？」を聞く",
                fontSize = 14.sp, color = Ink, lineHeight = 20.sp, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = Guard.askEnabled,
                onCheckedChange = { Guard.updateAskEnabled(it) },
                modifier = Modifier.semantics { contentDescription = "今何してるを聞く" },
                colors = SwitchDefaults.colors(checkedTrackColor = Blue, checkedThumbColor = OnAccent),
            )
        }
        Text(
            "オンの間は、答えるまでスマホを使えません（電話は使えます）。前の答えから5分以内は聞きません。原因が分かったらオフにしてOK。",
            fontSize = 12.sp, color = SubInk, lineHeight = 18.sp,
        )
        if (Guard.askEnabled && !overlayOk) {
            Spacer(Modifier.height(6.dp))
            Text(
                "※ 他のアプリを開いたときに質問へ戻すには、設定の「他のアプリの上に重ねて表示」の許可が必要です",
                fontSize = 12.sp, color = Danger, lineHeight = 18.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (logs.isEmpty()) {
            Text("この日の記録はまだありません。", fontSize = 13.sp, color = SubInk)
            return@Column
        }
        // 多かった理由（無駄に気づくため）
        val top = logs.groupingBy { it.purpose }.eachCount().entries.sortedByDescending { it.value }.take(3)
        Text(
            "多かった理由：" + top.joinToString("　") { "${it.key} ${it.value}回" },
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Blue, lineHeight = 19.sp,
        )
        Spacer(Modifier.height(8.dp))
        val fmt = java.time.format.DateTimeFormatter.ofPattern("H:mm")
        val zone = java.time.ZoneId.systemDefault()
        logs.forEach { l ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Text(
                    java.time.Instant.ofEpochMilli(l.time).atZone(zone).format(fmt),
                    fontSize = 14.sp, fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.width(52.dp),
                )
                Text("${l.doing} → ${l.purpose}", fontSize = 14.sp, color = Ink, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun ScoreCard(title: String, hint: String, value: Int?, onChange: (Int?) -> Unit) {
    Column(Modifier.fillMaxWidth().card().padding(20.dp)) {
        ScoreRow(title, hint, value, onChange)
    }
}

/** 0〜10 の丸ボタン。選んである数字をもう一度押すと未入力に戻る */
@Composable
private fun ScoreRow(title: String, hint: String, value: Int?, onChange: (Int?) -> Unit) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.width(8.dp))
        Text(hint, fontSize = 12.sp, color = SubInk, modifier = Modifier.weight(1f))
        Text(value?.toString() ?: "未入力", fontSize = if (value == null) 13.sp else 22.sp, fontWeight = FontWeight.Black, color = if (value == null) SubInk else Blue)
    }
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (n in 0..10) {
            val selected = n == value
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if (selected) Blue else Bg)
                    .clickable { onChange(if (selected) null else n) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$n",
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
                    color = if (selected) OnAccent else SubInk,
                )
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("最悪", fontSize = 11.sp, color = SubInk)
        Text("最高", fontSize = 11.sp, color = SubInk)
    }
}

/** 睡眠時間を 0.5 時間きざみで入力する */
@Composable
private fun SleepHoursRow(value: Float?, onChange: (Float?) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("睡眠時間", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
        StepButton("−") { onChange(((value ?: 7f) - 0.5f).coerceAtLeast(0f)) }
        Text(
            value?.let { String.format(Locale.US, "%.1f時間", it) } ?: "未入力",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = if (value == null) SubInk else Blue,
            modifier = Modifier.width(96.dp),
            textAlign = TextAlign.Center,
        )
        StepButton("＋") { onChange(((value ?: 6.5f) + 0.5f).coerceAtMost(16f)) }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink) }
}

@Composable
private fun MemoField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        maxLines = 3,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Bg, unfocusedContainerColor = Bg),
    )
}

// ---- 傾向（どういう時に調子がいいか） ----

private fun avg(xs: List<Float>) = if (xs.isEmpty()) null else xs.average().toFloat()
private fun fmt(x: Float?, unit: String = "") = x?.let { String.format(Locale.US, "%.1f", it) + unit } ?: "–"

@Composable
private fun TrendCard(days: Map<LocalDate, DayLog>) {
    Column(Modifier.fillMaxWidth().card().padding(20.dp)) {
        Text("調子がいいときの傾向", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(10.dp))

        // 直近14日の「今日の調子」の棒グラフ
        val today = LocalDate.now()
        Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
            for (i in 13 downTo 0) {
                val v = days[today.minusDays(i.toLong())]?.overall
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(if (v == null) 0.04f else (v.coerceAtLeast(0) / 10f).coerceAtLeast(0.06f))
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (v == null) SubInk.copy(alpha = 0.15f) else Blue),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("2週間前", fontSize = 11.sp, color = SubInk)
            Text("今日", fontSize = 11.sp, color = SubInk)
        }
        Spacer(Modifier.height(14.dp))

        val rated = days.values.filter { it.overall != null }
        if (rated.size < 5) {
            Text(
                "「今日の調子」を5日分つけると、調子がいい日とそうでない日の違いがここに出ます（いま ${rated.size}日）。",
                fontSize = 13.sp, color = SubInk, lineHeight = 19.sp,
            )
            return@Column
        }
        val good = rated.filter { it.overall!! >= 7 }
        val other = rated.filter { it.overall!! < 7 }
        Row(Modifier.fillMaxWidth()) {
            Text("", Modifier.weight(1.2f))
            Text("調子 7以上\n(${good.size}日)", Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Blue, lineHeight = 16.sp)
            Text("それ以外\n(${other.size}日)", Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SubInk, lineHeight = 16.sp)
        }
        Spacer(Modifier.height(6.dp))
        TrendRow("睡眠時間", fmt(avg(good.mapNotNull { it.sleepHours }), "h"), fmt(avg(other.mapNotNull { it.sleepHours }), "h"))
        TrendRow("睡眠の質", fmt(avg(good.mapNotNull { it.sleep?.toFloat() })), fmt(avg(other.mapNotNull { it.sleep?.toFloat() })))
        TrendRow("集中度", fmt(avg(good.mapNotNull { it.focus?.toFloat() })), fmt(avg(other.mapNotNull { it.focus?.toFloat() })))
        TrendRow("気分", fmt(avg(good.mapNotNull { it.mood?.toFloat() })), fmt(avg(other.mapNotNull { it.mood?.toFloat() })))
        TrendRow("朝ごはんあり", rate(good) { it.breakfast.isNotBlank() }, rate(other) { it.breakfast.isNotBlank() })
        Spacer(Modifier.height(8.dp))
        Text("※平均です。食事の中身まで含めた分析は、下の「AIに分析してもらう」がおすすめ。", fontSize = 11.sp, color = SubInk, lineHeight = 16.sp)
    }
}

private fun rate(xs: List<DayLog>, f: (DayLog) -> Boolean) =
    if (xs.isEmpty()) "–" else "${xs.count(f) * 100 / xs.size}%"

@Composable
private fun TrendRow(label: String, good: String, other: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, Modifier.weight(1.2f), fontSize = 14.sp, color = Ink)
        Text(good, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Black, color = Blue)
        Text(other, Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SubInk)
    }
}

// ---- AIへの質問文 ----

@Composable
private fun AiPromptCard(store: Store) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(5000); copied = false } }
    val count = store.days.size

    Column(Modifier.fillMaxWidth().card().padding(20.dp)) {
        Text("AIに分析してもらう", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "これまでの記録（${count}日分）を、AIへの質問文にまとめます。ChatGPT や Claude に貼り付けると「どんな時に調子がいいか」を分析してくれます。",
            fontSize = 13.sp, color = SubInk, lineHeight = 19.sp,
        )
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                clipboard.setText(AnnotatedString(buildAiPrompt(store)))
                copied = true
            },
            enabled = count > 0,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue, contentColor = OnAccent),
        ) {
            if (copied) {
                Icon(Icons.Filled.Check, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("コピーしました", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            } else {
                Text("質問文をコピー", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, buildAiPrompt(store))
                runCatching { context.startActivity(Intent.createChooser(send, "AIアプリを選ぶ")) }
            },
            enabled = count > 0,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.Share, null, tint = Blue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("AIアプリに直接送る", color = Blue, fontWeight = FontWeight.Bold)
        }
    }
}

/** 記録を、そのままAIに貼れる質問文にする（直近120日分まで） */
fun buildAiPrompt(store: Store): String {
    // 調子を記録した日と、スマホの使用時間がある日をあわせる
    val dates = (store.days.keys + Guard.screenMs.keys.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() })
        .toSortedSet().toList().takeLast(120)
    fun cell(s: String) = s.replace(",", "、").replace("\n", " ").trim()
    fun n(x: Int?) = x?.toString() ?: ""
    val csv = dates.joinToString("\n") { d ->
        val l = store.day(d)
        val screen = Guard.screenMinutes(d)
        listOf(
            d.toString(), weekday(d.dayOfWeek), n(l.overall),
            l.sleepHours?.let { String.format(Locale.US, "%.1f", it) } ?: "",
            n(l.sleep), n(l.focus), n(l.mood),
            if (screen > 0) "$screen" else "", Guard.unlockCount(d).takeIf { it > 0 }?.toString() ?: "",
            cell(l.breakfast), cell(l.lunch), cell(l.dinner), cell(l.memo),
        ).joinToString(",")
    }
    // ロック解除時の質問の答え（直近14日分）
    val since = System.currentTimeMillis() - 14 * 86_400_000L
    val logFmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd,HH:mm")
    val zone = java.time.ZoneId.systemDefault()
    val unlockCsv = Guard.unlockLogs.filter { it.time >= since }.sortedBy { it.time }.joinToString("\n") {
        java.time.Instant.ofEpochMilli(it.time).atZone(zone).format(logFmt) + "," + cell(it.doing) + "," + cell(it.purpose)
    }
    val unlockSection = if (unlockCsv.isBlank()) "" else """

# スマホを開いたときの記録（直近14日。ロック解除のたびに答えたもの）
日付,時刻,そのとき何をしていたか,スマホを開いた理由
$unlockCsv"""
    val goals = store.goals.joinToString("、") { it.text }.ifBlank { "（未登録）" }
    val habits = store.habits.joinToString("、") { it.text }.ifBlank { "（未登録）" }
    val from = dates.firstOrNull()
    val to = dates.lastOrNull()

    return """
あなたは生活習慣とコンディション管理にくわしいコーチです。
以下は、私が毎日スマホで記録している「今日の調子」のデータです（${dates.size}日分、$from 〜 $to）。
このデータをもとに、私が「どういう時に調子がいいのか」を分析してください。

# 記録の項目
- 調子：その日の総合的な調子（0〜10の自己評価。10が最高）
- 睡眠時間：前の晩に眠った時間（時間）
- 睡眠の質・集中度・気分：0〜10の自己評価
- スマホ(分)：その日にスマホの画面をつけて使っていた時間。解除回数：ロックを解除した回数
- 朝・昼・夜：食べたもの
- メモ：運動・出来事・飲んだものなど
- 空欄は「記録していない」という意味です（0点ではありません）

# 参考：私の目標と習慣
- 目標：$goals
- 習慣：$habits

# データ（CSV）
日付,曜日,調子,睡眠時間,睡眠の質,集中度,気分,スマホ(分),解除回数,朝,昼,夜,メモ
$csv$unlockSection

# お願いしたいこと
1. 調子がいい日と悪い日で、何が違うか（睡眠・食事・スマホの使用時間・曜日・メモの出来事など）。できるだけ具体的な数字で示してください
2. 「こういう時に調子がいい」というパターンを3つまで
3. 調子を下げていそうな習慣や組み合わせ（スマホを開いた記録があれば、無駄になっていそうな時間帯・理由も）
4. 明日から試せる、小さく始められる行動を3つ
5. データが少ない・偏っているなど注意すべき点と、今後追加で記録するとよい項目

# 答え方
- 最初に結論を3行でまとめ、そのあと詳しく説明してください
- 日本語で、専門用語を使わずにわかりやすく
- データから言えることと推測は分けて書いてください
- 医学的な診断はしないでください
""".trim()
}
