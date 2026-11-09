package de.schnurrfeld.app

import de.schnurrfeld.app.game.Levels
import de.schnurrfeld.app.game.PuzzleCodec
import de.schnurrfeld.app.game.PuzzleGenerator
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.File

class DumpLevels {
    /** Run with DUMP_LEVELS=1 to regenerate app/src/main/assets/levels.txt */
    @Test
    fun dump() {
        if (System.getenv("DUMP_LEVELS") != "1") return
        val out = (1..Levels.count).joinToString("\n") { lvl ->
            val c = Levels.config(lvl)
            val p = PuzzleGenerator.generate(c.size, Levels.seed(lvl), c.difficulty, c.maxRating)
            val enc = PuzzleCodec.encode(p)
            assertArrayEquals(p.regions, PuzzleCodec.decode(enc).regions)
            enc
        }
        File("src/main/assets").mkdirs()
        File("src/main/assets/levels.txt").writeText(out + "\n")
    }
}
