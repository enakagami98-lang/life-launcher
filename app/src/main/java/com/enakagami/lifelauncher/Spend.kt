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
    private val payWords = Regex("支払|決済|利用|購入|引き落とし|引落|お会計|使いました|payment|paid|purchase|spent|charged", RegexOption.IGNORE_CASE)
    /** 支払いのアプリ（アプリ名か通知の見出しで判断）。ここからの通知は「残高」「還元」などが入っていても支払いとして読む */
    private val payApps = Regex("PayPay|楽天ペイ|d払い|au ?PAY|メルペイ|メルカリ|Kyash|Suica|PASMO|Vpass|MyJCB|楽天カード|エポス|三井住友|イオン|セゾン|Revolut|ウォレット|Wallet|銀行|Bank|カード|Card|Pay", RegexOption.IGNORE_CASE)
    // どのアプリでも支払いではないもの（受け取り・返金・広告など）
    private val hardNot = Regex("受け取|受取|入金|返金|キャンセル|取消|送金|もらえる|当選|キャンペーン|最大|以上の|限度|可能額|請求予定|予定額|プレゼント")
    // 支払いのアプリ以外では広告のことが多いもの（支払いのアプリの「PayPay残高で支払いました」などは読む）
    private val softNot = Regex("チャージ|OFF|オフ|割引|クーポン|還元|残高|[%％]")
    private val yen = Regex("(?:[¥￥]\\s*([0-9][0-9,，]*)|([0-9][0-9,，]*)\\s*円)")
    // 「3円相当」「10ポイント」のようなポイントの数は金額ではない
    private val pointAfter = Regex("^\\s*(相当|分の?ポイント|ポイント|pt|P\\b)")

    /** 支払いの通知なら金額を返す。違えば null。fromPayApp は支払いのアプリからの通知か */
    fun parse(text: String, fromPayApp: Boolean = payApps.containsMatchIn(text)): Int? {
        if (!payWords.containsMatchIn(text) || hardNot.containsMatchIn(text)) return null
        if (!fromPayApp && softNot.containsMatchIn(text)) return null
        val m = yen.findAll(text).firstOrNull { !pointAfter.containsMatchIn(text.substring(it.range.last + 1)) } ?: return null
        val n = (m.groupValues[1].ifEmpty { m.groupValues[2] }).replace(",", "").replace("，", "").toIntOrNull() ?: return null
        return n.takeIf { it in 1..10_000_000 }
    }

    fun isPayApp(name: String) = payApps.containsMatchIn(name)

    // 同じ通知を2回数えないため、読んだ通知の目印（通知のキーと時刻）を残す
    private val seen = ArrayDeque<String>()

    /** まだ読んでいない通知なら true（読んだ印をつける） */
    fun firstTime(mark: String): Boolean {
        if (seen.isEmpty()) prefs.getString("seen", "")!!.split('\n').filter { it.isNotEmpty() }.forEach { seen.addLast(it) }
        if (mark in seen) return false
        seen.addLast(mark)
        while (seen.size > 300) seen.removeFirst()
        prefs.edit().putString("seen", seen.joinToString("\n")).apply()
        return true
    }

    /** 同じ支払いが「アプリの通知」と「メール」の両方で来ることがあるので、10分以内の同じ金額は1回と数える */
    fun onNotification(amount: Int, source: String, note: String, time: Long = System.currentTimeMillis()) {
        if (payments.any { it.auto && it.amount == amount && kotlin.math.abs(time - it.time) < 10 * 60_000L }) return
        add(amount, source, note.take(80), auto = true, time = time)
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

    /** つながったとき：つながっていない間に届いて、まだ残っている通知も読む */
    override fun onListenerConnected() {
        runCatching { activeNotifications?.forEach { read(it) } }
    }

    /** 切れたとき（アプリの更新・電池の節約など）は、つなぎ直しを頼む */
    override fun onListenerDisconnected() {
        runCatching { NotificationListenerService.requestRebind(ComponentName(this, PaymentListener::class.java)) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        runCatching { read(sbn) }
    }

    private fun read(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || sbn.packageName in chatApps) return
        val ex = sbn.notification.extras
        // 見出し・本文・長い本文・まとめ表示の各行・小見出しを全部つなげて読む（アプリによって入れる場所が違う）
        val parts = listOfNotNull(
            ex.getCharSequence("android.title"), ex.getCharSequence("android.title.big"),
            ex.getCharSequence("android.bigText") ?: ex.getCharSequence("android.text"),
            ex.getCharSequence("android.subText"), ex.getCharSequence("android.summaryText"),
            sbn.notification.tickerText,
        ).map { it.toString() } + (ex.getCharSequenceArray("android.textLines")?.map { it.toString() } ?: emptyList())
        val all = parts.filter { it.isNotBlank() }.distinct().joinToString(" ")
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)
        val amount = Spend.parse(all, Spend.isPayApp("$label $all")) ?: return
        if (!Spend.firstTime("${sbn.key}|${sbn.postTime}")) return
        Spend.onNotification(amount, label, all.trim(), sbn.postTime)
    }

    companion object {
        /** 許可されているのに切れていたら、つなぎ直す（ホーム画面を開いたときに呼ぶ） */
        fun rebind(context: android.content.Context) {
            if (!Spend.hasAccess(context)) return
            runCatching { NotificationListenerService.requestRebind(ComponentName(context, PaymentListener::class.java)) }
        }
    }
}
