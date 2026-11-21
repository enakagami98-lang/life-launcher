package com.enakagami.lifelauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 算数の問題：答えが正しいか、2種類以上の計算を使っているか、暗算できる大きさか */
class MathQuizTest {
    /** 表示する式を計算しなおす（× ÷ を先に、左から） */
    private fun eval(text: String): Int {
        val t = text.replace("×", "*").replace("÷", "/").replace("−", "-")
        val m = Regex("""\((\d+) \+ (\d+)\) \* (\d+)""").matchEntire(t)
        if (m != null) { val (a, b, c) = m.destructured; return (a.toInt() + b.toInt()) * c.toInt() }
        val tok = t.split(" ")
        // まず * と / を計算
        val terms = mutableListOf<Int>(); val ops = mutableListOf<String>()
        var cur = tok[0].toInt(); var i = 1
        while (i < tok.size) {
            val op = tok[i]; val n = tok[i + 1].toInt()
            when (op) {
                "*" -> cur *= n
                "/" -> { assertEquals("割り切れる: $text", 0, cur % n); cur /= n }
                else -> { terms.add(cur); ops.add(op); cur = n }
            }
            i += 2
        }
        terms.add(cur)
        var r = terms[0]
        ops.forEachIndexed { k, op -> r = if (op == "+") r + terms[k + 1] else r - terms[k + 1] }
        return r
    }

    @Test
    fun answersAreRightAndSmall() {
        val rnd = Random(1)
        repeat(5000) {
            val p = MathQuiz.make(rnd)
            assertEquals(p.text, eval(p.text), p.answer)
            assertTrue(p.text, p.answer in 1..999)
            val kinds = listOf("+", "−", "×", "÷").count { p.text.contains(it) }
            assertTrue("2種類以上: ${p.text}", kinds >= 2)
        }
    }
}
