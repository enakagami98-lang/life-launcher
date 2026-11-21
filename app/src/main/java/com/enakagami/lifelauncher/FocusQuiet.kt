package com.enakagami.lifelauncher

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.notification.Condition
import android.service.notification.ZenPolicy

/**
 * 集中タイム中だけ、電話以外の通知を止める（Android のおやすみモード）。
 * - Loosen 専用の「自動ルール」を1つ作り、集中の始まりでオン・終わりでオフにする。
 *   ユーザー自身のおやすみモードの設定は書き換えない
 * - 通るのは電話（誰からでも・2回目の着信も）とアラームだけ。音・バイブ・画面の上から出る通知・ロック画面の表示は止める
 * - 「通知の不要な中断（おやすみモード）へのアクセス」の許可が要る。Android 10 未満では何もしない
 */
object FocusQuiet {
    private val COND: Uri = Uri.parse("condition://com.enakagami.loosen/focus")

    /** この機能が使える Android か（Android 10 以上） */
    val supported get() = Build.VERSION.SDK_INT >= 29

    fun granted(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    fun openPermission(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences("quiet", Context.MODE_PRIVATE)

    fun on(context: Context?) {
        val ctx = context ?: return
        if (!supported || !granted(ctx)) return
        runCatching {
            val id = ruleId(ctx) ?: return
            nm(ctx).setAutomaticZenRuleState(id, Condition(COND, ctx.getString(R.string.w_focus), Condition.STATE_TRUE))
            prefs(ctx).edit().putBoolean("on", true).apply()
        }
    }

    fun off(context: Context?) {
        val ctx = context ?: return
        if (!supported) return
        prefs(ctx).edit().putBoolean("on", false).apply()
        if (!granted(ctx)) return
        runCatching {
            val id = prefs(ctx).getString("rule", null) ?: return
            nm(ctx).setAutomaticZenRuleState(id, Condition(COND, ctx.getString(R.string.w_focus), Condition.STATE_FALSE))
        }
    }

    /** 見守りサービスから2秒ごとに呼ぶ：集中が時間どおりに終わったら（画面が消えていても）通知を元に戻す */
    fun sync(context: Context) {
        if (!supported) return
        val wasOn = prefs(context).getBoolean("on", false)
        if (wasOn && !Guard.focusActive()) off(context)
    }

    private fun nm(ctx: Context) = ctx.getSystemService(NotificationManager::class.java)

    /** Loosen の自動ルール。無ければ作る */
    private fun ruleId(ctx: Context): String? {
        val p = prefs(ctx)
        p.getString("rule", null)?.let { id -> if (nm(ctx).getAutomaticZenRule(id) != null) return id }
        if (Build.VERSION.SDK_INT < 29) return null
        val policy = ZenPolicy.Builder()
            .disallowAllSounds()
            .allowCalls(ZenPolicy.PEOPLE_TYPE_ANYONE)
            .allowRepeatCallers(true)
            .allowAlarms(true)
            .hideAllVisualEffects()
            .build()
        val rule = AutomaticZenRule(
            ctx.getString(R.string.focus_quiet_rule), null, ComponentName(ctx, MainActivity::class.java),
            COND, policy, NotificationManager.INTERRUPTION_FILTER_PRIORITY, true,
        )
        val id = nm(ctx).addAutomaticZenRule(rule) ?: return null
        p.edit().putString("rule", id).apply()
        return id
    }
}
