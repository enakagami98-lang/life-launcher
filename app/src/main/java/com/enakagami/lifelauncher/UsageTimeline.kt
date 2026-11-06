package com.enakagami.lifelauncher

/** 使用時間の計算に使う、スマホ本体の記録1件（UsageEvents.Event の必要な分だけ） */
data class UsageEv(val type: Int, val time: Long, val pkg: String = "", val cls: String = "")

/**
 * スマホ本体の記録を時刻順に読み、アプリごとの使用時間を出す。
 *
 * 考え方：「今、画面の一番前にあるアプリは1つだけ」。使用時間は次の両方を満たす間だけ増やす。
 *  - 画面がついていて、ロックが解除されている
 *  - そのアプリが一番前にある
 * なので、全アプリの合計は実際の時間を絶対に超えない。
 *
 * 前の数え方はアプリごとに別々に開始・終了を見ていたので、終了の記録が抜けたアプリ（LINE など）が
 * 他のアプリを使っている間も増え続け、画面を消した後に開始の記録が出ると一晩中増えていた（1日21時間など）。
 *
 * 画面（Activity）単位でも追うのは、ゲームのように起動画面→本編と切り替わるアプリのため。
 * 「起動画面が裏に回った」記録を「アプリが閉じた」と読み違えないようにする。
 */
object UsageTimeline {
    const val RESUMED = 1
    const val PAUSED = 2
    const val STOPPED = 23
    const val DESTROYED = 24
    const val SCREEN_ON = 15
    const val SCREEN_OFF = 16
    const val KEYGUARD_SHOWN = 17
    const val KEYGUARD_HIDDEN = 18
    const val SHUTDOWN = 26
    const val STARTUP = 27

    /** 読む必要のある記録の種類 */
    val TYPES = setOf(RESUMED, PAUSED, STOPPED, DESTROYED, SCREEN_ON, SCREEN_OFF, KEYGUARD_SHOWN, KEYGUARD_HIDDEN, SHUTDOWN, STARTUP)

    /** events は時刻順で、from より少し前から入れておく（開始時点の状態を知るため） */
    fun compute(events: List<UsageEv>, from: Long, to: Long): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        // 読み始めの状態は、最初に出てくる記録から逆算する（最初が「消えた」なら、それまではついていた）
        var screenOn = events.firstOrNull { it.type == SCREEN_ON || it.type == SCREEN_OFF }?.type != SCREEN_ON
        var locked = events.firstOrNull { it.type == KEYGUARD_SHOWN || it.type == KEYGUARD_HIDDEN }?.type == KEYGUARD_HIDDEN
        var top: String? = null                    // 一番前のアプリ
        val topScreens = mutableSetOf<String>()    // そのアプリの、前面にある画面
        var since = from                           // 今の状態がいつからか

        // 今の状態が続いていた分を、一番前のアプリに足す
        fun flush(t: Long) {
            val pkg = top
            if (pkg != null && screenOn && !locked) {
                val a = maxOf(since, from)
                val b = minOf(t, to)
                if (b > a) result[pkg] = (result[pkg] ?: 0) + (b - a)
            }
            since = t
        }
        fun clearTop() { top = null; topScreens.clear() }

        for (e in events) {
            flush(e.time)
            when (e.type) {
                RESUMED -> {
                    // 別のアプリが前に来たら、それまでのアプリは自動的に終わり
                    if (top != e.pkg) { top = e.pkg; topScreens.clear() }
                    topScreens += e.cls
                }
                PAUSED, STOPPED, DESTROYED -> if (top == e.pkg) {
                    // 画面名の無い記録は、アプリ全体が閉じたものとして扱う
                    if (e.cls.isEmpty()) topScreens.clear() else topScreens -= e.cls
                    if (topScreens.isEmpty()) clearTop()
                }
                SCREEN_ON -> screenOn = true
                SCREEN_OFF -> { screenOn = false; clearTop() }
                KEYGUARD_SHOWN -> locked = true
                KEYGUARD_HIDDEN -> { locked = false; screenOn = true }
                SHUTDOWN, STARTUP -> { screenOn = e.type == STARTUP; locked = false; clearTop() }
            }
        }
        flush(to)
        return result
    }
}
