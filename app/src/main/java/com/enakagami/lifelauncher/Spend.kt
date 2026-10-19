package com.enakagami.lifelauncher

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** 支払い1件。auto=通知から自動で読んだもの */
data class Payment(val id: Long, val time: Long, val amount: Int, val source: String, val note: String, val auto: Boolean)

/**
 * 今月の支払額。PayPay・カード・Gmail などの「〇〇円お支払い」の通知から金額を読む。
 * 読み間違いは一覧から消せる。通知が来ないものは手で追加する。データはスマホ内だけ。
 */
object Spend {
    private lateinit var prefs: SharedPreferences
    private var ready = false
    val payments = mutableStateListOf<Payment>()

    fun init(context: Context) {
        if (ready) return
        ready = true
        prefs = context.applicationContext.getSharedPreferences("spend", Context.MODE_PRIVATE)
        val a = JSONArray(prefs.getString("payments", "[]"))
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            payments.add(Payment(o.getLong("id"), o.getLong("t"), o.getInt("yen"), o.optString("src"), o.optString("note"), o.optBoolean("auto")))
        }
    }

    fun monthOf(p: Payment): YearMonth = YearMonth.from(Instant.ofEpochMilli(p.time).atZone(ZoneId.systemDefault()))
    fun inMonth(m: YearMonth = YearMonth.now()) = payments.filter { monthOf(it) == m }.sortedByDescending { it.time }
    fun total(m: YearMonth = YearMonth.now()) = inMonth(m).sumOf { it.amount }

    fun add(amount: Int, source: String, note: String, auto: Boolean, time: Long = System.currentTimeMillis()) {
        payments.add(Payment(System.nanoTime(), time, amount, source, note, auto))
        save()
    }

    fun remove(id: Long) {
        payments.removeAll { it.id == id }
        save()
    }

    private fun save() {
        // 古い記録は2年分だけ残す
        val keep = System.currentTimeMillis() - 730 * 86_400_000L
        payments.removeAll { it.time < keep }
        val a = JSONArray()
        payments.forEach {
            a.put(JSONObject().put("id", it.id).put("t", it.time).put("yen", it.amount).put("src", it.source).put("note", it.note).put("auto", it.auto))
        }
        prefs.edit().putString("payments", a.toString()).apply()
    }

    fun hasAccess(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        val me = ComponentName(context, PaymentListener::class.java).flattenToString()
        return flat.split(':').any { it == me }
    }

    // ---- 通知の文章から金額を読む ----
    private val payWords = Regex("支払|決済|利用|購入|引き落とし|引落")
    // 受け取り・チャージ・広告などは支払いではない
    private val notPayWords = Regex("受け取|受取|入金|チャージ|返金|キャンセル|取消|OFF|オフ|割引|クーポン|還元|もらえる|プレゼント|キャンペーン|当選|限度|可能額|残高|請求予定|予定額|以上の")
    private val yen = Regex("(?:[¥￥]\\s*([0-9][0-9,，]*)|([0-9][0-9,，]*)\\s*円)")

    /** 支払いの通知なら金額を返す。違えば null */
    fun parse(text: String): Int? {
        if (!payWords.containsMatchIn(text) || notPayWords.containsMatchIn(text)) return null
        val m = yen.find(text) ?: return null
        val n = (m.groupValues[1].ifEmpty { m.groupValues[2] }).replace(",", "").replace("，", "").toIntOrNull() ?: return null
        return n.takeIf { it in 1..10_000_000 }
    }

    /** 同じ支払いが「アプリの通知」と「メール」の両方で来ることがあるので、10分以内の同じ金額は1回と数える */
    fun onNotification(amount: Int, source: String, note: String) {
        val now = System.currentTimeMillis()
        if (payments.any { it.auto && it.amount == amount && now - it.time < 10 * 60_000L }) return
        add(amount, source, note.take(80), auto = true, time = now)
    }
}

/** スマホに届いた通知を見て、支払いの通知なら金額を記録する */
class PaymentListener : NotificationListenerService() {
    /** 友だちとのやりとりで「支払い1000円」と書かれただけのものは数えない */
    private val chatApps = setOf(
        "jp.naver.line.android", "com.google.android.apps.messaging", "com.discord", "com.Slack",
        "com.twitter.android", "com.instagram.android", "com.facebook.orca", "org.telegram.messenger",
        "com.whatsapp", "com.kakao.talk", "com.microsoft.teams",
    )

    override fun onCreate() {
        super.onCreate()
        Spend.init(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        runCatching {
            if (sbn.packageName == packageName || sbn.isOngoing || sbn.packageName in chatApps) return
            val ex = sbn.notification.extras
            val title = ex.getCharSequence("android.title")?.toString().orEmpty()
            val text = ex.getCharSequence("android.bigText")?.toString()
                ?: ex.getCharSequence("android.text")?.toString().orEmpty()
            val all = "$title $text"
            val amount = Spend.parse(all) ?: return
            val label = runCatching {
                packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
            }.getOrDefault(sbn.packageName)
            Spend.onNotification(amount, label, all.trim())
        }
    }
}
