package com.enakagami.lifelauncher

import android.content.Context

/**
 * 「今から使い続けてくれた人は初年度半額」のための、最初に使い始めた日の記録。
 * ログインが無いので、スマホの中に「いつから使っているか」を残しておく（Google の自動バックアップで機種変更後も残る）。
 * - 最初の値は、この記録と「このアプリを入れた日」の早いほう（記録を始める前から使っている人も含める）
 * - 有料にするときは、この日付が有料化の日より前なら、Play Console の「初年度半額」の特典（デベロッパー指定）を出す
 */
object Early {
    fun firstSeen(context: Context): Long {
        val p = context.getSharedPreferences("early", Context.MODE_PRIVATE)
        val installed = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime }.getOrNull()
            ?: System.currentTimeMillis()
        val saved = p.getLong("firstSeen", Long.MAX_VALUE)
        val first = minOf(saved, installed)
        if (first != saved) p.edit().putLong("firstSeen", first).apply()
        return first
    }
}
