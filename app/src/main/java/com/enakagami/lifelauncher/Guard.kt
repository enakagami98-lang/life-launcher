package com.enakagami.lifelauncher

import android.app.AppOpsManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

    fun init(context: Context) {
        if (ready) return
        ready = true
        prefs = context.applicationContext.getSharedPreferences("guard", Context.MODE_PRIVATE)
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
    fun activeLock(now: LocalDateTime = LocalDateTime.now()): Pair<String, String>? = when {
        morning.activeAt(now) -> "朝の集中時間" to minutesText(morning.end)
        night.activeAt(now) -> "夜のおやすみ時間" to minutesText(night.end)
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

    fun usedMs(pkg: String): Long { rollDay(); return usage[pkg] ?: 0 }

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
