package de.schnurrfeld.app

import de.schnurrfeld.app.game.Levels
import de.schnurrfeld.app.game.PuzzleGenerator
import de.schnurrfeld.app.game.Solver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GeneratorTest {
    @Test
    fun allLevelsAreUniqueAndValid() {
        var total = 0L
        for (lvl in 1..Levels.count) {
            val c = Levels.config(lvl)
            val t0 = System.currentTimeMillis()
            val p = PuzzleGenerator.generate(c.size, Levels.seed(lvl), c.difficulty, c.maxRating)
            val dt = System.currentTimeMillis() - t0
            total += dt
            val n = p.size
            assertEquals(n, c.size)
            // solution valid
            assertEquals(n, p.solution.toSet().size)
            for (r in 1 until n) assertTrue(abs(p.solution[r] - p.solution[r - 1]) > 1)
            assertEquals(n, (0 until n).map { p.regions[it * n + p.solution[it]] }.toSet().size)
            assertEquals(1, Solver(n, p.regions).countSolutions(2))
            println("level $lvl size $n rating ${de.schnurrfeld.app.game.LogicRater.rate(p)} max ${c.maxRating} ${dt}ms")
        }
        println("total ${total}ms")
    }
}
