package de.schnurrfeld.app

import de.schnurrfeld.app.game.Levels
import de.schnurrfeld.app.game.LogicRater
import de.schnurrfeld.app.game.PuzzleCodec
import de.schnurrfeld.app.game.PuzzleGenerator
import de.schnurrfeld.app.game.Solver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class GeneratorTest {
    private val shipped = File("src/main/assets/levels.txt").readLines().filter { it.isNotBlank() }

    /** every level that ships in the app: right size, valid solution, exactly one solution */
    @Test
    fun allShippedLevelsAreUniqueAndValid() {
        assertEquals("levels.txt has one line per level", Levels.count, shipped.size)
        for (lvl in 1..Levels.count) {
            val p = PuzzleCodec.decode(shipped[lvl - 1])
            val n = p.size
            assertEquals("level $lvl size", Levels.config(lvl).size, n)
            assertEquals(n, p.solution.toSet().size)
            for (r in 1 until n) assertTrue("level $lvl: cats touch", abs(p.solution[r] - p.solution[r - 1]) > 1)
            assertEquals(n, (0 until n).map { p.regions[it * n + p.solution[it]] }.toSet().size)
            assertEquals("level $lvl has exactly one solution", 1, Solver(n, p.regions).countSolutions(2))
        }
    }

    @Test
    fun levelTagsCoverEveryLevel() {
        val tags = File("src/main/assets/level_tags.txt").readText().trim()
        assertEquals(Levels.count, tags.length)
        assertTrue(tags.all { it in '0'..'2' })
    }

    /** the second wave (201-500) can be reproduced from its seeds (a sample of it) */
    @Test
    fun secondWaveIsReproducible() {
        for (lvl in (Levels.FIRST_WAVE + 1)..Levels.count step 37) {
            val c = Levels.config(lvl)
            val p = PuzzleGenerator.generate(c.size, Levels.seed(lvl), c.difficulty, c.maxRating)
            assertEquals("level $lvl", shipped[lvl - 1], PuzzleCodec.encode(p))
        }
    }

    /** the generator still works for every band (a few levels each) */
    @Test
    fun generatorWorksForEveryBand() {
        for ((size, range) in Levels.groups()) {
            for (lvl in listOf(range.first, (range.first + range.last) / 2, range.last)) {
                val c = Levels.config(lvl)
                val p = PuzzleGenerator.generate(c.size, Levels.seed(lvl), c.difficulty, c.maxRating)
                assertEquals(size, p.size)
                assertEquals(1, Solver(p.size, p.regions).countSolutions(2))
                assertTrue(LogicRater.rate(p) <= c.maxRating)
            }
        }
    }

    @Test
    fun secondWaveIsHarderThanTheStart() {
        // levels 201-500 never use the "easy" shapes of the first levels
        for (lvl in (Levels.FIRST_WAVE + 1)..Levels.count) assertTrue(Levels.config(lvl).difficulty >= 0.5f)
        assertEquals(500, Levels.count)
    }
}
