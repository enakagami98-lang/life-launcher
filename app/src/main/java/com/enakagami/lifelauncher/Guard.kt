package com.enakagami.lifelauncher

import android.app.AppOpsManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/** 使えない時間帯。start/end は 0:00 からの分。days は 1=月 … 7=日 */
data class LockRange(val enabled: Boolean, val start: Int, val end: Int, val days: Set<Int>) {
    fun activeAt(dt: LocalDateTime): Boolean {
        if (!enabled || start == end) return false
        val t = dt.hour * 60 + dt.minute
        val dow = dt.dayOfWeek.value
        val yesterday = (dow + 5) % 7 + 1
        return if (start < end) dow in days && t >= start && t < end
        // 夜22時〜朝6時のように日付をまたぐ場合、朝の部分は「前の日」の曜日で判定する
        else (t >= start && dow in days) || (t < end && yesterday in days)
    }
}

fun minutesText(m: Int) = "%02d:%02d".format(m / 60, m % 60)

/** ロック解除時の質問への答え1件。time はミリ秒 */
data class UnlockLog(val time: Long, val doing: String, val purpose: String)

/**
 * アプリリマインダーと使えない時間帯の設定・記録。
 * ホーム画面と見守りサービスは同じプロセスで動くので、このオブジェクトを共有する。
 */
object Guard {
    private lateinit var prefs: SharedPreferences
    private var ready = false

    /** パッケージ名 → 1日に使ってよい分数 */
    val limits = mutableStateMapOf<String, Int>()
    var waitSec by mutableIntStateOf(10)
    var floatingBar by mutableStateOf(true)
    var homeOnScreenOff by mutableStateOf(true)
    var morning by mutableStateOf(LockRange(false, 5 * 60, 7 * 60, (1..7).toSet()))
    var night by mutableStateOf(LockRange(false, 23 * 60, 6 * 60, (1..7).toSet()))

    // 今日の使用時間（ミリ秒）と、今回の利用の終了時刻
    private var usageDay = ""
    private val usage = mutableMapOf<String, Long>()
    private val sessionEnd = mutableMapOf<String, Long>()
    private val sessionLen = mutableMapOf<String, Long>()

    // スマホ全体の使用時間（日付 → ミリ秒）とロック解除の回数（日付 → 回）
    val screenMs = mutableStateMapOf<String, Long>()
    val unlocks = mutableStateMapOf<String, Int>()
    private var screenSavedAt = 0L

    // ロック解除時の質問
    var askEnabled by mutableStateOf(false)
        private set
    /** true の間は質問に答えるまでホーム画面（質問）に戻し続ける */
    var askPending by mutableStateOf(false)
        private set
    private var lastAnswerAt = 0L
    val unlockLogs = mutableStateListOf<UnlockLog>()

    fun init(context: Context) {
        if (ready) return
        ready = true
        prefs = context.applicationContext.getSharedPreferences("guard", Context.MODE_PRIVATE)
        appContext = context.applicationContext
        JSONObject(prefs.getString("limits", "{}")!!).let { o -> o.keys().forEach { limits[it] = o.getInt(it) } }
        waitSec = prefs.getInt("waitSec", 10)
        floatingBar = prefs.getBoolean("floatingBar", true)
        homeOnScreenOff = prefs.getBoolean("homeOnScreenOff", true)
        prefs.getString("morning", null)?.let { morning = parseRange(it) }
        prefs.getString("night", null)?.let { night = parseRange(it) }
        usageDay = prefs.getString("usageDay", "")!!
        JSONObject(prefs.getString("usage", "{}")!!).let { o -> o.keys().forEach { usage[it] = o.getLong(it) } }
        JSONObject(prefs.getString("sessions", "{}")!!).let { o ->
            o.keys().forEach { k -> val a = o.getJSONArray(k); sessionEnd[k] = a.getLong(0); sessionLen[k] = a.getLong(1) }
        }
        JSONObject(prefs.getString("screen", "{}")!!).let { o -> o.keys().forEach { screenMs[it] = o.getLong(it) } }
        JSONObject(prefs.getString("unlocks", "{}")!!).let { o -> o.keys().forEach { unlocks[it] = o.getInt(it) } }
        askEnabled = prefs.getBoolean("askEnabled", false)
        askPending = askEnabled && prefs.getBoolean("askPending", false)
        lastAnswerAt = prefs.getLong("lastAnswerAt", 0)
        JSONArray(prefs.getString("unlockLogs", "[]")).let { a ->
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                unlockLogs.add(UnlockLog(o.getLong("t"), o.optString("doing"), o.optString("purpose")))
            }
        }
        loadOptions()
        goalOn = prefs.getBoolean("goalOn", true)
        JSONObject(prefs.getString("dailyGoals", "{}")!!).let { o -> o.keys().forEach { dailyGoals[it] = o.getString(it) } }
    }

    // ---- 設定 ----
    fun setLimit(pkg: String, minutes: Int) { limits[pkg] = minutes; saveLimits() }
    fun removeLimit(pkg: String) { limits.remove(pkg); saveLimits() }
    private fun saveLimits() {
        val o = JSONObject(); limits.forEach { (k, v) -> o.put(k, v) }
        prefs.edit().putString("limits", o.toString()).apply()
    }
    fun updateWaitSec(v: Int) { waitSec = v; prefs.edit().putInt("waitSec", v).apply() }
    fun updateFloatingBar(v: Boolean) { floatingBar = v; prefs.edit().putBoolean("floatingBar", v).apply() }
    fun updateHomeOnScreenOff(v: Boolean) { homeOnScreenOff = v; prefs.edit().putBoolean("homeOnScreenOff", v).apply() }
    fun updateMorning(r: LockRange) { morning = r; prefs.edit().putString("morning", rangeJson(r)).apply() }
    fun updateNight(r: LockRange) { night = r; prefs.edit().putString("night", rangeJson(r)).apply() }

    private fun rangeJson(r: LockRange) = JSONObject().put("enabled", r.enabled).put("start", r.start)
        .put("end", r.end).put("days", JSONArray(r.days.sorted())).toString()

    private fun parseRange(s: String): LockRange {
        val o = JSONObject(s); val d = o.getJSONArray("days")
        return LockRange(o.getBoolean("enabled"), o.getInt("start"), o.getInt("end"), (0 until d.length()).map { d.getInt(it) }.toSet())
    }

    // ---- 使えない時間帯 ----
    /** 今が使えない時間なら (名前, 解除時刻) を返す */
    fun activeLock(now: LocalDateTime = LocalDateTime.now()): Pair<Int, String>? = when {
        morning.activeAt(now) -> R.string.lock_morning to minutesText(morning.end)
        night.activeAt(now) -> R.string.lock_night to minutesText(night.end)
        else -> null
    }

    // ---- 使用時間 ----
    fun isLimited(pkg: String) = limits.containsKey(pkg)

    private fun rollDay() {
        val today = LocalDate.now().toString()
        if (usageDay != today) {
            usageDay = today; usage.clear()
            prefs.edit().putString("usageDay", today).putString("usage", "{}").apply()
        }
    }

    /**
     * 今日の使用時間。「使用状況へのアクセス」が許可されていれば、スマホ本体の使用記録
     * （Digital Wellbeing と同じ元データ）から数える → 通知・履歴・ウィジェットから開いた分も入る。
     * 許可が無いときは、このホーム画面から開いて使っていた間だけ（自前の記録）。
     */
    fun usedMs(pkg: String): Long {
        rollDay()
        val own = usage[pkg] ?: 0
        val sys = systemUsage()?.get(pkg) ?: return own
        return maxOf(own, sys)
    }

    private var appContext: Context? = null
    private var sysCache = emptyMap<String, Long>()
    private var sysAt = 0L
    private var sysDay = ""

    /** 今日0時からの、アプリごとの前面にあった時間（15秒ごとに作り直す）。許可が無ければ null */
    fun systemUsage(): Map<String, Long>? {
        val ctx = appContext ?: return null
        if (!hasUsageAccess(ctx)) return null
        val now = System.currentTimeMillis()
        val today = LocalDate.now().toString()
        if (sysDay == today && now - sysAt < 15_000) return sysCache
        val start = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val result = mutableMapOf<String, Long>()
        var count = 0
        runCatching {
            val usm = ctx.getSystemService(android.app.usage.UsageStatsManager::class.java)
            val events = usm.queryEvents(start, now)
            val e = android.app.usage.UsageEvents.Event()
            val resumed = mutableMapOf<String, Long>()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                count++
                when (e.eventType) {
                    android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED ->
                        if (e.packageName !in resumed) resumed[e.packageName] = e.timeStamp
                    android.app.usage.UsageEvents.Event.ACTIVITY_PAUSED, 23 /* ACTIVITY_STOPPED */ ->
                        resumed.remove(e.packageName)?.let { t -> result[e.packageName] = (result[e.packageName] ?: 0) + (e.timeStamp - t) }
                }
            }
            // 今も前面にあるアプリ
            resumed.forEach { (pkg, t) -> result[pkg] = (result[pkg] ?: 0) + (now - t) }
        }.onFailure { android.util.Log.w("Loosen", "usage query failed", it) }
        // 動作確認用の記録（adb logcat -s Loosen で見られる）
        android.util.Log.d("Loosen", "usage events=$count since=$start limited=" +
            limits.keys.joinToString { "$it:${(result[it] ?: 0) / 1000}s" })
        sysCache = result; sysAt = now; sysDay = today
        return result
    }

    fun remainingMs(pkg: String): Long = (limits[pkg] ?: 0) * 60_000L - usedMs(pkg)

    fun addUsage(pkg: String, ms: Long) {
        rollDay()
        usage[pkg] = (usage[pkg] ?: 0) + ms
        val o = JSONObject(); usage.forEach { (k, v) -> o.put(k, v) }
        prefs.edit().putString("usage", o.toString()).apply()
    }

    fun sessionActive(pkg: String) = (sessionEnd[pkg] ?: 0) > System.currentTimeMillis()

    /** 直近（3時間以内）に使った時間が切れたばかりか。「延長しますか？」の表示に使う */
    fun hadSession(pkg: String): Boolean {
        val end = sessionEnd[pkg] ?: return false
        val now = System.currentTimeMillis()
        return end <= now && now - end < 3 * 3600_000L
    }

    /** 今回の残り時間の割合（フローティングバー用）。0〜1 */
    fun sessionFraction(pkg: String): Float {
        val end = sessionEnd[pkg] ?: return 0f
        val len = sessionLen[pkg] ?: return 0f
        return ((end - System.currentTimeMillis()).toFloat() / len).coerceIn(0f, 1f)
    }

    // ---- 簡易モード（使用状況データの許可がないとき）用 ----
    /** このアプリから最後に開いた、使用中のリマインダー対象アプリ */
    var currentPkg: String? = null
    /** ホーム画面（このアプリ）が今表示されているか */
    var launcherResumed = false
    /** ロック画面の「電話をかける」を押してから、この時刻までは閉じない */
    var allowUntil = 0L

    /** 簡易モードで、ホームに戻ってきたら今回の利用を終わりにする */
    fun endCurrentSession() {
        val pkg = currentPkg ?: return
        if (sessionActive(pkg)) {
            sessionEnd[pkg] = System.currentTimeMillis()
            saveSessions()
        }
        currentPkg = null
    }

    fun startSession(pkg: String, minutes: Int) {
        currentPkg = pkg
        val len = minOf(minutes * 60_000L, remainingMs(pkg)).coerceAtLeast(0)
        sessionEnd[pkg] = System.currentTimeMillis() + len
        sessionLen[pkg] = len.coerceAtLeast(1)
        saveSessions()
    }

    private fun saveSessions() {
        val o = JSONObject()
        sessionEnd.forEach { (k, v) -> o.put(k, JSONArray().put(v).put(sessionLen[k] ?: 1)) }
        prefs.edit().putString("sessions", o.toString()).apply()
    }

    // ---- スマホ全体の使用時間 ----
    /** 画面がついていて、ロックが解除されている間に呼ばれる */
    fun addScreenTime(ms: Long) {
        val today = LocalDate.now().toString()
        screenMs[today] = (screenMs[today] ?: 0) + ms
        // 書き込みは30秒に1回で十分（画面を消したときにも保存する）
        if (System.currentTimeMillis() - screenSavedAt > 30_000) saveScreen()
    }

    fun saveScreen() {
        screenSavedAt = System.currentTimeMillis()
        // 古い記録は400日分だけ残す
        val keep = LocalDate.now().minusDays(400).toString()
        screenMs.keys.filter { it < keep }.forEach { screenMs.remove(it) }
        unlocks.keys.filter { it < keep }.forEach { unlocks.remove(it) }
        val s = JSONObject(); screenMs.forEach { (k, v) -> s.put(k, v) }
        val u = JSONObject(); unlocks.forEach { (k, v) -> u.put(k, v) }
        prefs.edit().putString("screen", s.toString()).putString("unlocks", u.toString()).apply()
    }

    fun screenMinutes(date: LocalDate) = ((screenMs[date.toString()] ?: 0) / 60_000).toInt()
    fun unlockCount(date: LocalDate) = unlocks[date.toString()] ?: 0

    // ---- ロック解除時の質問 ----
    /** 前の答えからこの時間は聞かない */
    private const val ASK_GAP_MS = 5 * 60_000L

    fun updateAskEnabled(v: Boolean) {
        askEnabled = v
        if (!v) setPending(false)
        prefs.edit().putBoolean("askEnabled", v).apply()
    }

    /** ロックを解除したときに見守りサービスから呼ばれる。質問を出すなら true */
    fun onUnlock(): Boolean {
        val today = LocalDate.now().toString()
        unlocks[today] = (unlocks[today] ?: 0) + 1
        saveScreen()
        // 夜のおやすみ時間のあと最初の解除：まず今日の目標（このときは「今何してる？」は聞かない）
        if (needGoal()) return true
        if (!askEnabled || askPending) return askPending
        if (System.currentTimeMillis() - lastAnswerAt < ASK_GAP_MS) return false
        // 使えない時間帯は、そもそもスマホを使えないので聞かない
        if (activeLock() != null) return false
        setPending(true)
        return true
    }

    fun answer(doing: String, purpose: String) {
        val now = System.currentTimeMillis()
        lastAnswerAt = now
        unlockLogs.add(UnlockLog(now, doing, purpose))
        // 古い答えは120日分だけ残す
        val keep = now - 120 * 86_400_000L
        unlockLogs.removeAll { it.time < keep }
        val a = JSONArray()
        unlockLogs.forEach { a.put(JSONObject().put("t", it.time).put("doing", it.doing).put("purpose", it.purpose)) }
        prefs.edit().putString("unlockLogs", a.toString()).putLong("lastAnswerAt", now).apply()
        setPending(false)
    }

    private fun setPending(v: Boolean) {
        askPending = v
        prefs.edit().putBoolean("askPending", v).apply()
    }

    fun logsOn(date: LocalDate): List<UnlockLog> {
        val zone = java.time.ZoneId.systemDefault()
        val from = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return unlockLogs.filter { it.time in from until to }.sortedBy { it.time }
    }

    // ---- 今日の目標（夜のおやすみ時間が終わったら最初に決める） ----
    var goalOn by mutableStateOf(true)
        private set
    /** 日付 → その日の目標 */
    val dailyGoals = mutableStateMapOf<String, String>()

    fun updateGoalOn(v: Boolean) { goalOn = v; prefs.edit().putBoolean("goalOn", v).apply() }

    fun dailyGoal(date: LocalDate = LocalDate.now()) = dailyGoals[date.toString()] ?: ""

    fun setDailyGoal(date: LocalDate, text: String) {
        val t = text.trim()
        if (t.isEmpty()) dailyGoals.remove(date.toString()) else dailyGoals[date.toString()] = t
        // 古い目標は400日分だけ残す
        val keep = date.minusDays(400).toString()
        dailyGoals.keys.filter { it < keep }.forEach { dailyGoals.remove(it) }
        val o = JSONObject(); dailyGoals.forEach { (k, v) -> o.put(k, v) }
        prefs.edit().putString("dailyGoals", o.toString()).apply()
    }

    /**
     * 今日の「夜のおやすみ時間」が終わったあとで、まだ今日の目標を決めていないか。
     * true の間は、目標を決めるまでホーム画面（目標の入力）に戻し続ける。
     * 夜のおやすみ時間を使っていない人には出さない。
     */
    fun needGoal(now: LocalDateTime = LocalDateTime.now()): Boolean {
        val n = night
        if (!goalOn || !n.enabled || n.start == n.end) return false
        if (activeLock(now) != null) return false
        if (dailyGoal(now.toLocalDate()).isNotBlank()) return false
        val t = now.hour * 60 + now.minute
        val dow = now.dayOfWeek.value
        val yesterday = (dow + 5) % 7 + 1
        // 22時〜6時のように日付をまたぐなら「昨日の夜」、またがないなら「今日」の分が終わったか
        val endedToday = if (n.start > n.end) yesterday in n.days else dow in n.days
        return endedToday && t >= n.end
    }

    // ---- 「今何してる？」の選択肢（登録・削除できる） ----
    // 最初から入っている選択肢は "@キー" で保存し、表示するときにスマホの言語の文字にする
    val DEFAULT_DOING = listOf("@study", "@work", "@class", "@commute", "@meal", "@break", "@relax", "@shopping", "@music", "@gym", "@reading", "@bedtime", "@wakeup")
    val DEFAULT_PURPOSE = listOf("@contact", "@sns", "@video", "@search", "@pay", "@schedule", "@time", "@notice", "@game", "@idle")
    val doingOptions = mutableStateListOf<String>()
    val purposeOptions = mutableStateListOf<String>()

    private fun loadOptions() {
        fun load(key: String, def: List<String>) = prefs.getString(key, null)
            ?.let { s -> JSONArray(s).let { a -> (0 until a.length()).map { a.getString(it) } } } ?: def
        doingOptions.clear(); doingOptions.addAll(load("doingOpts", DEFAULT_DOING))
        purposeOptions.clear(); purposeOptions.addAll(load("purposeOpts", DEFAULT_PURPOSE))
    }

    private fun saveOptions() {
        prefs.edit()
            .putString("doingOpts", JSONArray(doingOptions.toList()).toString())
            .putString("purposeOpts", JSONArray(purposeOptions.toList()).toString())
            .apply()
    }

    fun optionList(doing: Boolean) = if (doing) doingOptions else purposeOptions

    fun addOption(doing: Boolean, text: String) {
        val t = text.trim()
        val list = optionList(doing)
        if (t.isEmpty() || t in list) return
        list.add(t); saveOptions()
    }

    fun removeOption(doing: Boolean, entry: String) { optionList(doing).remove(entry); saveOptions() }

    fun resetOptions() {
        doingOptions.clear(); doingOptions.addAll(DEFAULT_DOING)
        purposeOptions.clear(); purposeOptions.addAll(DEFAULT_PURPOSE)
        saveOptions()
    }

    // ---- 許可の確認 ----
    fun hasUsageAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= 29)
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        else @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlay(context: Context) = Settings.canDrawOverlays(context)
}
