package com.enakagami.lifelauncher

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * 画面の文字はスマホの言語設定に合わせて自動で切り替わる（res/values-xx/strings.xml）。
 * 訳文の元は i18n/ フォルダ。直したら `python i18n/build.py` で strings.xml を作り直す。
 */

/** 192 → 「3時間12分」/「3h 12m」 */
fun durationText(ctx: Context, minutes: Int): String =
    if (minutes < 60) ctx.getString(R.string.dur_m, minutes)
    else ctx.getString(R.string.dur_hm, minutes / 60, minutes % 60)

@Composable
fun durationText(minutes: Int) = durationText(LocalContext.current, minutes)

/** 「10月19日（月）」/「Mon, Oct 19」のように、その言語で自然な形にする */
fun dateWithWeekday(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMdEEE"), locale))

fun shortDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMd"), locale))

fun longDate(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "yMMMd"), locale))

/** 「今何してる？」の最初から入っている選択肢（"@キー"）の表示名 */
private val OptionNames = mapOf(
    "@study" to R.string.opt_study, "@work" to R.string.opt_work, "@class" to R.string.opt_class,
    "@commute" to R.string.opt_commute, "@meal" to R.string.opt_meal, "@break" to R.string.opt_break,
    "@relax" to R.string.opt_relax, "@shopping" to R.string.opt_shopping, "@music" to R.string.opt_music,
    "@gym" to R.string.opt_gym, "@reading" to R.string.opt_reading, "@bedtime" to R.string.opt_bedtime,
    "@wakeup" to R.string.opt_wakeup,
    "@contact" to R.string.opt_contact, "@sns" to R.string.opt_sns, "@video" to R.string.opt_video,
    "@search" to R.string.opt_search, "@pay" to R.string.opt_pay, "@schedule" to R.string.opt_schedule,
    "@time" to R.string.opt_time, "@notice" to R.string.opt_notice, "@game" to R.string.opt_game,
    "@idle" to R.string.opt_idle,
)

fun optionLabel(ctx: Context, entry: String): String = OptionNames[entry]?.let { ctx.getString(it) } ?: entry

@Composable
fun optionLabel(entry: String) = optionLabel(LocalContext.current, entry)
