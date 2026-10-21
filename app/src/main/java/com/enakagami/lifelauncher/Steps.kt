package com.enakagami.lifelauncher

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

/**
 * 今日の歩数。スマホの歩数センサー（電源を入れてからの合計歩数）の増えた分を、日付ごとに足していく。
 * 見守りサービスの中でセンサーを聞き続ける。アプリを入れる前・許可する前の分は数えられない。
 */
object Steps : SensorEventListener {
    private lateinit var prefs: SharedPreferences
    private var ready = false
    private var listening = false
    var today by mutableIntStateOf(0)
        private set
    private var day = ""
    /** 前回センサーが示した合計（-1 は未取得） */
    private var last = -1f
    private var savedAt = 0L

    fun init(context: Context) {
        if (ready) return
        ready = true
        prefs = context.applicationContext.getSharedPreferences("steps", Context.MODE_PRIVATE)
        day = prefs.getString("day", "") ?: ""
        today = if (day == LocalDate.now().toString()) prefs.getInt("steps", 0) else 0
        last = prefs.getFloat("last", -1f)
    }

    fun hasPermission(context: Context) = Build.VERSION.SDK_INT < 29 ||
        context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    fun hasSensor(context: Context) =
        context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    /** 許可があればセンサーを聞き始める（何度呼んでもよい） */
    fun ensureListening(context: Context) {
        init(context)
        if (listening || !hasPermission(context)) return
        val sm = context.getSystemService(SensorManager::class.java) ?: return
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
        listening = sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    /** 日付が変わっていたら0に戻す（画面表示のとき用） */
    fun todaySteps(): Int {
        if (ready && day != LocalDate.now().toString()) today = 0
        return today
    }

    override fun onSensorChanged(event: SensorEvent) {
        val c = event.values.firstOrNull() ?: return
        val d = LocalDate.now().toString()
        if (day != d) { day = d; today = 0 }
        // 再起動すると合計が0から数え直しになるので、減ったときは足さない
        if (last >= 0 && c >= last) today += (c - last).toInt()
        last = c
        val now = System.currentTimeMillis()
        if (now - savedAt > 30_000) {
            savedAt = now
            prefs.edit().putString("day", day).putInt("steps", today).putFloat("last", last).apply()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
