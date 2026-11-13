package de.schnurrfeld.app

import de.schnurrfeld.app.game.Levels
import de.schnurrfeld.app.game.PuzzleCodec
import de.schnurrfeld.app.game.PuzzleGenerator
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.File

class DumpLevels {
    /**
     * Run with DUMP_LEVELS=1 to regenerate app/src/main/assets/levels.txt.
     * With DUMP_FROM=201 the first 200 lines are kept exactly as they are and only the new levels are generated
     * (in parallel; every level depends only on its own seed).
     */
    @Test
    fun dump() {
        if (System.getenv("DUMP_LEVELS") != "1") return
        val file = File("src/main/assets/levels.txt")
        val from = System.getenv("DUMP_FROM")?.toIntOrNull() ?: 1
        val kept = if (from > 1) file.readLines().filter { it.isNotBlank() }.take(from - 1) else emptyList()
        check(kept.size == from - 1) { "levels.txt has only ${kept.size} levels" }
        val fresh = (from..Levels.count).toList().parallelStream().map { lvl ->
            val c = Levels.config(lvl)
            val p = PuzzleGenerator.generate(c.size, Levels.seed(lvl), c.difficulty, c.maxRating)
            val enc = PuzzleCodec.encode(p)
            assertArrayEquals(p.regions, PuzzleCodec.decode(enc).regions)
            println("level $lvl ${c.size}x${c.size} ok")
            enc
        }.toList()
        File("src/main/assets").mkdirs()
        file.writeText((kept + fresh).joinToString("\n") + "\n")
    }
}
