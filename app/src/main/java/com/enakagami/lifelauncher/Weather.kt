package com.enakagami.lifelauncher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

/** 今日の天気。code は世界気象機関（WMO）の天気番号 */
data class WeatherData(val code: Int, val temp: Double, val max: Double, val min: Double, val at: Long)

/**
 * 天気の部品。だいたいの位置（約1km単位に丸める）を無料の Open-Meteo（登録・キー不要）に送って天気を受け取る。
 * 1時間以内に取ったものは保存してあるものを使う。
 */
object Weather {
    private lateinit var prefs: SharedPreferences
    private var ready = false
    var data by mutableStateOf<WeatherData?>(null)
        private set
    /** うまく取れなかった理由（文字のリソース番号） */
    var error by mutableStateOf<Int?>(null)
        private set

    fun init(context: Context) {
        if (ready) return
        ready = true
        prefs = context.applicationContext.getSharedPreferences("weather", Context.MODE_PRIVATE)
        prefs.getString("data", null)?.let { s ->
            runCatching {
                val o = JSONObject(s)
                data = WeatherData(o.getInt("code"), o.getDouble("temp"), o.getDouble("max"), o.getDouble("min"), o.getLong("at"))
            }
        }
    }

    fun hasPermission(context: Context) =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    suspend fun refresh(context: Context, force: Boolean = false) {
        init(context)
        val d = data
        if (!force && d != null && System.currentTimeMillis() - d.at < 3600_000L) return
        if (!hasPermission(context)) { error = R.string.weather_need_permission; return }
        val loc = location(context) ?: run { error = R.string.weather_no_location; return }
        val lat = String.format(Locale.US, "%.2f", loc.latitude)
        val lon = String.format(Locale.US, "%.2f", loc.longitude)
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val url = URL("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                    "&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1")
                val c = url.openConnection() as HttpURLConnection
                c.connectTimeout = 10_000; c.readTimeout = 10_000
                val o = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                val cur = o.getJSONObject("current")
                val day = o.getJSONObject("daily")
                WeatherData(
                    cur.getInt("weather_code"), cur.getDouble("temperature_2m"),
                    day.getJSONArray("temperature_2m_max").getDouble(0), day.getJSONArray("temperature_2m_min").getDouble(0),
                    System.currentTimeMillis(),
                )
            }.getOrNull()
        }
        if (result == null) { error = R.string.weather_failed; return }
        data = result; error = null
        prefs.edit().putString("data", JSONObject().put("code", result.code).put("temp", result.temp)
            .put("max", result.max).put("min", result.min).put("at", result.at).toString()).apply()
    }

    @SuppressLint("MissingPermission")
    private suspend fun location(context: Context): Location? {
        val lm = context.getSystemService(LocationManager::class.java)
        val known = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        // 6時間以内の位置があればそれで十分（天気は街の単位で決まるため）
        if (known != null && System.currentTimeMillis() - known.time < 6 * 3600_000L) return known
        if (Build.VERSION.SDK_INT < 30) return known
        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) } ?: return known
        return withTimeoutOrNull(15_000) {
            suspendCancellableCoroutine { cont ->
                lm.getCurrentLocation(provider, null, context.mainExecutor) { cont.resume(it) }
            }
        } ?: known
    }

    /** 天気番号 → 天気の名前（文字のリソース番号） */
    fun nameOf(code: Int) = when (code) {
        0 -> R.string.wx_clear
        1, 2 -> R.string.wx_partly
        3 -> R.string.wx_cloudy
        45, 48 -> R.string.wx_fog
        in 51..57 -> R.string.wx_drizzle
        in 61..67, in 80..82 -> R.string.wx_rain
        in 71..77, 85, 86 -> R.string.wx_snow
        in 95..99 -> R.string.wx_thunder
        else -> R.string.wx_cloudy
    }
}
