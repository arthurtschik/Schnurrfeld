package de.schnurrfeld.app

import de.schnurrfeld.app.game.LevelBank
import de.schnurrfeld.app.game.Levels
import de.schnurrfeld.app.game.LogicRater
import org.junit.Test
import java.io.File

/**
 * Rates every level and writes app/src/main/assets/level_tags.txt (run with DUMP_LEVELS=1):
 * 0 = normal, 1 = "schwer" (hardest 20 % of its board size), 2 = "super schwer" (hardest 5 %, needs look-ahead).
 * A level is compared with the others of the same size, so the label means "harder than you'd expect here".
 */
class DifficultyReport {
    @Test fun tags() {
        if (System.getenv("DUMP_LEVELS") != "1") return
        val bank = LevelBank { File("src/main/assets/levels.txt").inputStream() }
        // DUMP_FROM=201: keep the tags of the first wave as they are, rate only the new levels (against each other)
        val from = System.getenv("DUMP_FROM")?.toIntOrNull() ?: 1
        val oldTags = if (from > 1) File("src/main/assets/level_tags.txt").readText().trim().take(from - 1) else ""
        check(oldTags.length == from - 1)
        val rows = (from..Levels.count).toList().parallelStream().map { l -> val p = bank.level(l); Triple(l, p.size, LogicRater.analyze(p)) }.toList()
        val tags = IntArray(Levels.count + 1)
        oldTags.forEachIndexed { i, c -> tags[i + 1] = c.digitToInt() }
        for ((_, band) in rows.groupBy { it.second }) {
            val scores = band.map { it.third.score }.sorted()
            fun pct(q: Double) = scores[minOf(scores.size - 1, (scores.size * q).toInt())]
            val hard = pct(0.8); val superHard = pct(0.95)
            for ((l, _, a) in band) {
                tags[l] = when {
                    a.score >= superHard && a.score >= 20 && a.rating >= 3 -> 2
                    a.score >= hard && a.score >= 3 -> 1
                    else -> 0
                }
            }
        }
        File("src/main/assets/level_tags.txt").writeText((1..Levels.count).joinToString("") { tags[it].toString() } + "\n")
        println("schwer: ${tags.count { it == 1 }}, super schwer: ${tags.count { it == 2 }}")
    }
}
