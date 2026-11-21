package com.enakagami.lifelauncher

import kotlin.random.Random

/**
 * ロック解除のときに出す算数の問題。
 * 足し算・引き算・掛け算・割り算のうち2種類以上を組み合わせた、ギリギリ暗算できるくらいの1問。
 * 答えはいつも 1〜999 の整数（割り算は必ず割り切れる）。
 */
data class MathProblem(val text: String, val answer: Int)

object MathQuiz {
    private const val TIMES = "×"
    private const val DIV = "÷"
    private const val MINUS = "−"

    fun make(rnd: Random = Random.Default): MathProblem {
        while (true) {
            val p = when (rnd.nextInt(8)) {
                0 -> { val a = rnd.nextInt(12, 20); val b = rnd.nextInt(3, 10); val c = rnd.nextInt(12, 90)
                    MathProblem("$a $TIMES $b + $c", a * b + c) }
                1 -> { val a = rnd.nextInt(12, 20); val b = rnd.nextInt(4, 10); val c = rnd.nextInt(11, a * b - 4)
                    MathProblem("$a $TIMES $b $MINUS $c", a * b - c) }
                2 -> { val b = rnd.nextInt(3, 10); val q = rnd.nextInt(7, 20); val c = rnd.nextInt(12, 90)
                    MathProblem("${q * b} $DIV $b + $c", q + c) }
                3 -> { val b = rnd.nextInt(3, 10); val q = rnd.nextInt(4, 13); val c = rnd.nextInt(3, 10)
                    MathProblem("${q * b} $DIV $b $TIMES $c", q * c) }
                4 -> { val a = rnd.nextInt(6, 20); val b = rnd.nextInt(6, 20); val c = rnd.nextInt(3, 9)
                    MathProblem("($a + $b) $TIMES $c", (a + b) * c) }
                5 -> { val a = rnd.nextInt(21, 100); val b = rnd.nextInt(6, 15); val c = rnd.nextInt(3, 10)
                    MathProblem("$a + $b $TIMES $c", a + b * c) }
                6 -> { val b = rnd.nextInt(6, 13); val c = rnd.nextInt(3, 9); val a = b * c + rnd.nextInt(5, 61)
                    MathProblem("$a $MINUS $b $TIMES $c", a - b * c) }
                else -> { val a = rnd.nextInt(11, 17); val b = rnd.nextInt(3, 8); val d = rnd.nextInt(3, 10); val q = rnd.nextInt(3, 13)
                    MathProblem("$a $TIMES $b $MINUS ${q * d} $DIV $d", a * b - q) }
            }
            if (p.answer in 1..999) return p
        }
    }
}
