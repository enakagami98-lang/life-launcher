package com.enakagami.lifelauncher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

/**
 * 習慣のリマインド。習慣に「何時にやるか」を決めておくと、その時刻を過ぎてもまだチェックが無いときに1回だけお知らせする。
 * 見守りサービス（GuardService）が2秒ごとに呼ぶ（中では20秒に1回だけ調べる）。データは Store と同じ prefs "life" を読む
 */
object HabitRemind {
    private const val CHANNEL = "habit"
    /** その日にもうお知らせした習慣（"日付|id,id"） */
    private const val KEY = "habitReminded"
    private var lastCheck = 0L

    fun timeText(m: Int) = "%d:%02d".format(m / 60, m % 60)

    private fun notifId(id: Long) = 2000 + (id % 1_000_000L).toInt()

    private fun reminded(ctx: Context, today: String): MutableSet<Long> {
        val v = prefs(ctx).getString(KEY, "") ?: ""
        if (v.substringBefore('|') != today) return mutableSetOf()
        return v.substringAfter('|', "").split(',').mapNotNull { it.toLongOrNull() }.toMutableSet()
    }

    private fun saveReminded(ctx: Context, today: String, ids: Set<Long>) {
        prefs(ctx).edit().putString(KEY, today + "|" + ids.joinToString(",")).apply()
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("life", Context.MODE_PRIVATE)

    /**
     * 時刻を決めた・変えたとき。今日もう過ぎた時刻なら、今日はお知らせしない（登録した瞬間に鳴らないように）。
     * まだ先の時刻なら、今日もお知らせする
     */
    fun onTimeSet(ctx: Context, id: Long, time: Int?) {
        val today = LocalDate.now().toString()
        val set = reminded(ctx, today)
        val now = LocalTime.now().let { it.hour * 60 + it.minute }
        if (time != null && time <= now) set += id else set -= id
        saveReminded(ctx, today, set)
        cancel(ctx, id)
    }

    fun cancel(ctx: Context, id: Long) {
        runCatching { ctx.getSystemService(NotificationManager::class.java).cancel(notifId(id)) }
    }

    fun check(ctx: Context) {
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastCheck < 20_000) return
        lastCheck = nowMs
        val p = prefs(ctx)
        val today = LocalDate.now().toString()
        val now = LocalTime.now().let { it.hour * 60 + it.minute }
        val habits = runCatching { JSONArray(p.getString("habits", "[]")) }.getOrNull() ?: return
        val done = runCatching { JSONObject(p.getString("habitDone", "{}") ?: "{}") }.getOrNull() ?: JSONObject()
        val sent = reminded(ctx, today)
        var changed = false
        for (i in 0 until habits.length()) {
            val o = habits.getJSONObject(i)
            if (!o.has("time")) continue
            val id = o.getLong("id")
            val time = o.getInt("time")
            if (now < time || id in sent) continue
            val days = done.optJSONArray(id.toString())
            val isDone = days != null && (0 until days.length()).any { days.optString(it) == today }
            if (isDone) continue
            notify(ctx, id, o.getString("text"), time)
            sent += id
            changed = true
        }
        if (changed) saveReminded(ctx, today, sent)
    }

    private fun notify(ctx: Context, id: Long, text: String, time: Int) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.hr_channel), NotificationManager.IMPORTANCE_DEFAULT))
        // 押すと習慣のページを開く
        val open = Intent(ctx, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("open_page", "habits")
        val pi = PendingIntent.getActivity(ctx, notifId(id), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(text)
            .setContentText(ctx.getString(R.string.hr_body, timeText(time)))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        runCatching { nm.notify(notifId(id), n) }
    }
}
