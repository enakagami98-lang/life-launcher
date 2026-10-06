package com.enakagami.lifelauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import java.text.Collator
import java.util.Locale

/** key は「パッケージ名/画面名」。お気に入りの識別に使う */
data class AppInfo(val key: String, val pkg: String, val cls: String, val label: String)

/** インストール済みで、ホーム画面から開けるアプリをすべて取得する（自分自身は除く） */
fun loadApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val collator = Collator.getInstance(Locale.JAPANESE)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .map {
            val pkg = it.activityInfo.packageName
            val cls = it.activityInfo.name
            AppInfo("$pkg/$cls", pkg, cls, it.loadLabel(pm).toString())
        }
        .distinctBy { it.key }
        .sortedWith { a, b -> collator.compare(a.label, b.label) }
}

fun launchApp(context: Context, key: String) {
    val pkg = key.substringBefore('/')
    val cls = key.substringAfter('/')
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(ComponentName(pkg, cls))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    runCatching { context.startActivity(intent) }
}
