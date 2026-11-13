package de.schnurrfeld.app.game

import java.time.LocalDate

/**
 * A running game, saved so it survives a phone call, another app, or Android clearing the app from memory.
 * Stored as one text line; [decode] checks every field, so a damaged line is simply ignored.
 * The puzzle itself is not stored: it is rebuilt from the level number, the date or the endless seed.
 */
data class GameSnapshot(
    /** 'L' level, 'D' daily puzzle, 'E' endless */
    val kind: Char,
    /** level number, ISO date, or empty for endless */
    val arg: String,
    /** one digit per cell (see [Cell]) */
    val cells: String,
    val lives: Int,
    val elapsed: Int,
    val mistakes: Int,
    val streak: Int,
    val playerCats: Int,
    val endlessSolved: Int,
    val endlessSeed: Long,
    val savedAt: Long,
) {
    fun encode(): String =
        listOf(VERSION, kind, arg, cells, lives, elapsed, mistakes, streak, playerCats, endlessSolved, endlessSeed, savedAt).joinToString("|")

    /** the game this snapshot belongs to, or null if it is no longer valid (e.g. a daily puzzle of another day) */
    fun mode(today: LocalDate = LocalDate.now()): GameMode? = when (kind) {
        'L' -> arg.toIntOrNull()?.takeIf { it in 1..Levels.count }?.let { GameMode.Level(it) }
        'D' -> runCatching { LocalDate.parse(arg) }.getOrNull()?.takeIf { it == today }?.let { GameMode.Daily(it) }
        'E' -> GameMode.Endless
        else -> null
    }

    companion object {
        private const val VERSION = "1"
        /** a game older than this is not offered any more */
        const val MAX_AGE_MS = 7L * 24 * 3600 * 1000

        fun decode(s: String?): GameSnapshot? {
            val p = s?.split("|") ?: return null
            if (p.size != 12 || p[0] != VERSION || p[1].length != 1) return null
            val cells = p[3]
            if (cells.isEmpty() || cells.length > 144 || !cells.all { it in '0'..'3' }) return null
            val lives = p[4].toIntOrNull() ?: return null
            val elapsed = p[5].toIntOrNull() ?: return null
            val mistakes = p[6].toIntOrNull() ?: return null
            val streak = p[7].toIntOrNull() ?: return null
            val cats = p[8].toIntOrNull() ?: return null
            val solved = p[9].toIntOrNull() ?: return null
            val seed = p[10].toLongOrNull() ?: return null
            val at = p[11].toLongOrNull() ?: return null
            if (lives !in 1..MAX_LIVES || elapsed < 0 || mistakes < 0 || streak < 0 || cats < 0 || solved < 0) return null
            return GameSnapshot(p[1][0], p[2], cells, lives, elapsed, mistakes, streak, cats, solved, seed, at)
        }
    }
}
