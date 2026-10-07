package com.enakagami.lifelauncher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.telecom.TelecomManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast

/**
 * 見守りサービス。画面がついている間だけ2秒ごとに「今どのアプリが前面か」を調べ、
 * ・リマインダー対象アプリの使用時間を数える／時間切れならホームに戻して延長を聞く
 * ・使えない時間帯ならアプリを閉じてホームに戻す
 * ・画面を消したらホームに戻す（次に画面をつけたとき寿命画面から始まる）
 */
class GuardService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private var fg: String? = null
    private var lastQuery = 0L
    private var lastTick = 0L
    private var lastBounce = 0L
    private var anchor: View? = null
    private var bar: FrameLayout? = null
    private var barFill: View? = null
    private val warned = mutableSetOf<String>()
    private var warnedDay = ""
    private val launchable = mutableMapOf<String, Boolean>()

    private val tick = object : Runnable {
        override fun run() {
            runCatching { check() }
            handler.postDelayed(this, 2000)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                hideBar()
                if (Guard.homeOnScreenOff && !inCall() && fg != packageName) {
                    openLauncher(null, null)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Guard.init(this)
        wm = getSystemService(WindowManager::class.java)
        startAsForeground()
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        lastQuery = System.currentTimeMillis() - 60_000
        lastTick = System.currentTimeMillis()
        handler.post(tick)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { unregisterReceiver(screenReceiver) }
        hideBar()
        anchor?.let { runCatching { wm.removeView(it) } }
        super.onDestroy()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("guard", "見守り", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "guard")
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle("LifeLauncher が見守り中")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun check() {
        val now = System.currentTimeMillis()
        val dt = (now - lastTick).coerceIn(0, 5000)
        lastTick = now
        if (!getSystemService(PowerManager::class.java).isInteractive) return
        ensureAnchor()
        if (!Guard.hasUsageAccess(this)) return
        updateForeground(now)
        val pkg = fg
        if (pkg == null || pkg == packageName) { hideBar(); return }

        // 使えない時間帯
        if (Guard.activeLock() != null && isBlockable(pkg)) {
            hideBar(); openLauncher(pkg, "lock"); return
        }
        if (!Guard.isLimited(pkg)) { hideBar(); return }

        // リマインダー対象アプリ
        if (!Guard.sessionActive(pkg)) {
            hideBar(); openLauncher(pkg, "session"); return
        }
        Guard.addUsage(pkg, dt)
        val remain = Guard.remainingMs(pkg)
        if (remain < 30_000) { hideBar(); openLauncher(pkg, "session"); return }
        warnIfNeeded(pkg, remain)
        if (Guard.floatingBar) showBar(Guard.sessionFraction(pkg)) else hideBar()
    }

    /** 前面のアプリを、使用状況の記録（イベント）から調べる */
    private fun updateForeground(now: Long) {
        val usm = getSystemService(UsageStatsManager::class.java)
        val events = usm.queryEvents(lastQuery, now)
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED) fg = e.packageName
        }
        lastQuery = now
    }

    /** 電話・設定・このアプリ以外の「ホームから開けるアプリ」は、使えない時間帯に閉じる */
    private fun isBlockable(pkg: String): Boolean = launchable.getOrPut(pkg) {
        val dialer = runCatching { getSystemService(TelecomManager::class.java).defaultDialerPackage }.getOrNull()
        pkg != dialer && pkg != "com.android.settings" && pkg != packageName &&
            packageManager.getLaunchIntentForPackage(pkg) != null
    }

    private fun inCall(): Boolean {
        val mode = getSystemService(AudioManager::class.java).mode
        return mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION || mode == AudioManager.MODE_RINGTONE
    }

    private fun openLauncher(pkg: String?, reason: String?) {
        val now = System.currentTimeMillis()
        if (reason != null && now - lastBounce < 3000) return
        if (reason != null) lastBounce = now
        val i = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("guard_pkg", pkg)
            .putExtra("guard_reason", reason)
        runCatching { startActivity(i) }
        fg = packageName
    }

    private fun label(pkg: String) = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    // ---- 残り5分・1分のお知らせ ----
    private fun warnIfNeeded(pkg: String, remain: Long) {
        val today = java.time.LocalDate.now().toString()
        if (warnedDay != today) { warnedDay = today; warned.clear() }
        val minute = when {
            remain <= 60_000 -> 1
            remain <= 5 * 60_000 -> 5
            else -> return
        }
        if (warned.add("$pkg-$minute")) {
            if (minute == 1) warned.add("$pkg-5")
            showPill("${label(pkg)}：今日の残り${minute}分")
        }
    }

    private fun overlayParams(w: Int, h: Int, gravity: Int, y: Int = 0, name: String = "LifeLauncherOverlay") = WindowManager.LayoutParams(
        w, h,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply { this.gravity = gravity; this.y = y; title = name }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun accent(): Int = getSharedPreferences("life", MODE_PRIVATE).getInt("accent", 0xFF1043E5.toInt())

    private fun showPill(text: String) {
        if (!Guard.hasOverlay(this)) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); return }
        val tv = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(dp(18), dp(10), dp(18), dp(10))
            background = GradientDrawable().apply { cornerRadius = dp(24).toFloat(); setColor(0xE6202024.toInt()) }
        }
        runCatching {
            wm.addView(tv, overlayParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, dp(72), "LifeLauncherPill"))
            handler.postDelayed({ runCatching { wm.removeView(tv) } }, 5000)
        }
    }

    // ---- 画面最下部の細い進捗バー ----
    private fun showBar(fraction: Float) {
        if (!Guard.hasOverlay(this)) return
        if (bar == null) {
            val fill = View(this).apply { setBackgroundColor(accent()) }
            val frame = FrameLayout(this).apply {
                setBackgroundColor(0x33000000)
                addView(fill, FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT))
            }
            runCatching {
                wm.addView(frame, overlayParams(WindowManager.LayoutParams.MATCH_PARENT, dp(4), Gravity.BOTTOM, name = "LifeLauncherBar"))
                bar = frame; barFill = fill
            }
        }
        val width = resources.displayMetrics.widthPixels
        barFill?.layoutParams = FrameLayout.LayoutParams((width * fraction).toInt(), FrameLayout.LayoutParams.MATCH_PARENT)
    }

    private fun hideBar() {
        bar?.let { runCatching { wm.removeView(it) } }
        bar = null; barFill = null
    }

    /**
     * 1ピクセルの透明な目印。新しいAndroidでは「画面に何か表示しているアプリ」しか
     * 裏からホーム画面を呼び出せないため。
     */
    private fun ensureAnchor() {
        if (anchor != null || !Guard.hasOverlay(this)) return
        val v = View(this)
        runCatching {
            wm.addView(v, overlayParams(1, 1, Gravity.TOP or Gravity.START, name = "LifeLauncherAnchor"))
            anchor = v
        }
    }
}
