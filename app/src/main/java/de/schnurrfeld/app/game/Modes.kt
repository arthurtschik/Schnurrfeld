package de.schnurrfeld.app.game

import java.time.LocalDate

sealed class GameMode {
    data class Level(val number: Int) : GameMode()
    data class Daily(val date: LocalDate) : GameMode()
    data object Endless : GameMode()
    /** the interactive first-start tutorial (fixed 4×4 puzzle, no lives lost, no stats) */
    data object Tutorial : GameMode()
}

object Levels {
    /** (size, number of levels with that size) */
    // first wave: levels 1-200, 10×10 at most (bigger boards make the cells too small)
    private val bands = listOf(5 to 8, 6 to 9, 7 to 13, 8 to 20, 9 to 40, 10 to 110)
    /** second wave: levels 201-500, starts at 8×8 again and is hard from the start */
    private val bands2 = listOf(8 to 30, 9 to 90, 10 to 180)
    /** number of levels in the first wave (their puzzles and tags never change) */
    const val FIRST_WAVE = 200
    val count = bands.sumOf { it.second } + bands2.sumOf { it.second }

    data class Config(val size: Int, val difficulty: Float, val indexInBand: Int, val bandLength: Int, val maxRating: Int)

    /** Gentle start: early levels only need the simplest logic. */
    private fun maxRating(level: Int) = when {
        level <= 12 -> 1
        level <= 30 -> 2
        level <= 70 -> 3
        else -> 4
    }

    fun config(level: Int): Config {
        var start = 1
        for ((size, len) in bands) {
            if (level < start + len) {
                val idx = level - start
                // difficulty ramps up inside each band
                val ramp = if (len <= 1) 0f else idx.toFloat() / (len - 1)
                // compact shapes early on, more irregular later
                val d = if (level <= 12) 0f else ramp
                return Config(size, d, idx, len, maxRating(level))
            }
            start += len
        }
        for ((size, len) in bands2) {
            if (level < start + len) {
                val idx = level - start
                val ramp = if (len <= 1) 0f else idx.toFloat() / (len - 1)
                return Config(size, 0.5f + 0.5f * ramp, idx, len, 4)
            }
            start += len
        }
        return Config(10, 1f, 0, 1, 4)
    }

    fun groups(): List<Pair<Int, IntRange>> {
        var start = 1
        return (bands + bands2).map { (size, len) ->
            val range = start until start + len
            start += len
            size to range
        }
    }

    fun seed(level: Int): Long = 0x5C4A_77L * 1_000_003L + level * 7919L
}

object Endless {
    fun size(solved: Int) = (6 + solved / 2).coerceAtMost(10)
    fun difficulty(solved: Int) = (0.3f + solved * 0.05f).coerceAtMost(1f)
}

object Daily {
    fun size(date: LocalDate) = when (date.dayOfWeek.value) { 1, 2 -> 7; 3, 4 -> 8; 5, 6 -> 9; else -> 10 }
    fun seed(date: LocalDate): Long = date.toEpochDay() * 2654435761L + 17
}
