package com.enakagami.lifelauncher

import com.enakagami.lifelauncher.UsageTimeline.KEYGUARD_HIDDEN
import com.enakagami.lifelauncher.UsageTimeline.KEYGUARD_SHOWN
import com.enakagami.lifelauncher.UsageTimeline.PAUSED
import com.enakagami.lifelauncher.UsageTimeline.RESUMED
import com.enakagami.lifelauncher.UsageTimeline.SCREEN_OFF
import com.enakagami.lifelauncher.UsageTimeline.SCREEN_ON
import com.enakagami.lifelauncher.UsageTimeline.STOPPED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 使用時間の数え方の確認。実機で起きた不具合の順番をそのまま再現している（単位は分） */
class UsageTimelineTest {
    private fun at(min: Int) = min * 60_000L
    private fun r(min: Int, pkg: String, cls: String = "A") = UsageEv(RESUMED, at(min), pkg, cls)
    private fun p(min: Int, pkg: String, cls: String = "A") = UsageEv(PAUSED, at(min), pkg, cls)
    private fun s(min: Int, pkg: String, cls: String = "A") = UsageEv(STOPPED, at(min), pkg, cls)
    private fun ev(min: Int, type: Int) = UsageEv(type, at(min))
    private fun run(vararg e: UsageEv, to: Int = 24 * 60) =
        UsageTimeline.compute(e.toList(), 0, at(to)).mapValues { (it.value / 60_000).toInt() }

    @Test fun ふつうに開いて閉じた分だけ数える() {
        assertEquals(mapOf("line" to 10), run(r(5, "line"), p(15, "line")))
    }

    @Test fun ゲームの起動画面から本編への切り替えで抜けない() {
        val u = run(r(0, "game", "Splash"), p(1, "game", "Splash"), r(1, "game", "Main"), s(2, "game", "Splash"), p(180, "game", "Main"))
        assertEquals(mapOf("game" to 180), u)
    }

    @Test fun 閉じた記録が抜けても他のアプリの時間を足さない() {
        // LINE の PAUSED が無いままゲームを3時間。前の数え方では LINE も3時間増えていた
        val u = run(r(0, "line"), r(3, "game"), p(183, "game"))
        assertEquals(mapOf("line" to 3, "game" to 180), u)
    }

    @Test fun 画面が消えた後に開始の記録が出ても一晩中数えない() {
        // 前の数え方では、次に画面を消すまでの全部が LINE の時間になっていた（1日21時間など）
        val u = run(
            ev(0, SCREEN_ON), ev(0, KEYGUARD_HIDDEN), r(1, "game"), p(10, "game"), ev(10, SCREEN_OFF), ev(10, KEYGUARD_SHOWN),
            r(11, "line"),
            ev(600, SCREEN_ON), ev(600, KEYGUARD_HIDDEN), r(600, "home"), p(605, "home"),
        )
        assertEquals(mapOf("game" to 9, "home" to 5), u)
    }

    @Test fun ロック中は数えず解除してから数える() {
        val u = run(ev(0, SCREEN_OFF), ev(0, KEYGUARD_SHOWN), ev(60, SCREEN_ON), r(60, "game"), ev(61, KEYGUARD_HIDDEN), p(71, "game"))
        assertEquals(mapOf("game" to 10), u)
    }

    @Test fun 画面が消えたら終わりで次の開始記録まで数えない() {
        val u = run(ev(0, SCREEN_ON), r(0, "line"), ev(5, SCREEN_OFF), ev(60, SCREEN_ON), r(70, "line"), p(75, "line"))
        assertEquals(mapOf("line" to 10), u)
    }

    @Test fun 前の画面の閉じた記録で今の画面を止めない() {
        val u = run(r(0, "app", "A"), r(1, "app", "B"), p(2, "app", "A"), p(10, "app", "B"))
        assertEquals(mapOf("app" to 10), u)
    }

    @Test fun 後ろのアプリの記録で前のアプリを止めない() {
        val u = run(r(0, "game"), s(5, "line"), p(5, "line"), p(30, "game"))
        assertEquals(mapOf("game" to 30), u)
    }

    @Test fun 日をまたいで開いている分は期間の中だけ数える() {
        val e = listOf(r(-30, "game"), p(30, "game"))
        assertEquals(mapOf("game" to at(30)), UsageTimeline.compute(e, 0, at(60)))
    }

    @Test fun 開きっぱなしは今の時刻まで数える() {
        assertEquals(mapOf("game" to 20), run(r(40, "game"), to = 60))
    }

    @Test fun 画面が消えたままで始まる日は開始の記録があっても数えない() {
        // 読み始めで最初の画面の記録が「ついた」なら、それまでは消えていた
        assertEquals(mapOf("home" to 1), run(r(5, "line"), ev(60, SCREEN_ON), r(60, "home"), p(61, "home")))
    }

    @Test fun 合計は実際の時間を超えない() {
        val u = UsageTimeline.compute(
            listOf(r(0, "a"), r(1, "b"), r(2, "c"), r(3, "d"), s(4, "a"), r(5, "e")), 0, at(60),
        )
        assertTrue(u.values.sum() <= at(60))
    }
}
