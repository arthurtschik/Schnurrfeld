package de.schnurrfeld.app.game

import kotlin.math.abs
import kotlin.random.Random

/**
 * A puzzle: an N×N grid split into N coloured regions.
 * Exactly one cat per row, column and region; cats never touch (not even diagonally).
 */
class Puzzle(
    val size: Int,
    /** region index per cell (row * size + col) */
    val regions: IntArray,
    /** solution column per row */
    val solution: IntArray,
    /** colour slot per region (index into the palette) */
    val colorOf: IntArray,
) {
    fun cell(r: Int, c: Int) = r * size + c
    fun isSolution(cell: Int) = solution[cell / size] == cell % size
    fun regionOf(cell: Int) = regions[cell]
    fun solutionCellOfRegion(region: Int): Int {
        for (r in 0 until size) {
            val cell = cell(r, solution[r])
            if (regions[cell] == region) return cell
        }
        error("region without cat")
    }
}

object PuzzleGenerator {

    /** difficulty 0..1 – higher means more irregular, unbalanced regions */
    /**
     * [maxRating]: hardest logic technique the player may need (see [LogicRater]).
     * 1 = only "last free cell", 2 = + line confinement, 3 = + one-step lookahead, 4 = anything.
     */
    fun generate(size: Int, seed: Long, difficulty: Float, maxRating: Int = 4): Puzzle {
        val rnd = Random(seed)
        var best: Puzzle? = null
        var bestRating = Int.MAX_VALUE
        var attempts = 0
        while (true) {
            val sol = randomSolution(size, rnd) ?: continue
            val regions = growRegions(size, sol, rnd, difficulty)
            if (makeUnique(size, sol, regions, rnd)) {
                val p = Puzzle(size, regions, sol, colorSlots(size, rnd))
                val rating = if (maxRating >= 4) 4 else LogicRater.rate(p)
                if (rating <= maxRating) return p
                if (rating < bestRating) { best = p; bestRating = rating }
                if (++attempts >= 600) return best!!
            }
        }
    }

    // Colour order: the first entries are the most distinct, so small boards get the best contrast.
    private fun colorSlots(size: Int, rnd: Random): IntArray {
        val slots = (0 until size).toMutableList()
        slots.shuffle(rnd)
        return slots.toIntArray()
    }

    private fun randomSolution(n: Int, rnd: Random): IntArray? {
        val sol = IntArray(n) { -1 }
        val used = BooleanArray(n)
        fun place(r: Int): Boolean {
            if (r == n) return true
            val cols = (0 until n).shuffled(rnd)
            for (c in cols) {
                if (used[c]) continue
                if (r > 0 && abs(sol[r - 1] - c) <= 1) continue
                sol[r] = c; used[c] = true
                if (place(r + 1)) return true
                used[c] = false
            }
            sol[r] = -1
            return false
        }
        return if (place(0)) sol else null
    }

    private fun growRegions(n: Int, sol: IntArray, rnd: Random, difficulty: Float): IntArray {
        val reg = IntArray(n * n) { -1 }
        val sizes = IntArray(n)
        for (r in 0 until n) { reg[r * n + sol[r]] = r; sizes[r] = 1 }
        var left = n * n - n
        // Easy: sample several candidates and grow the smallest region -> compact, balanced shapes.
        // Hard: grow random regions -> snaky, unbalanced shapes.
        val samples = when {
            difficulty < 0.25f -> 4
            difficulty < 0.5f -> 3
            else -> 2
        }
        val candCell = IntArray(n * n * 4)
        val candReg = IntArray(n * n * 4)
        while (left > 0) {
            var k = 0
            for (cell in 0 until n * n) {
                if (reg[cell] != -1) continue
                val r = cell / n; val c = cell % n
                if (r > 0 && reg[cell - n] >= 0) { candCell[k] = cell; candReg[k] = reg[cell - n]; k++ }
                if (r < n - 1 && reg[cell + n] >= 0) { candCell[k] = cell; candReg[k] = reg[cell + n]; k++ }
                if (c > 0 && reg[cell - 1] >= 0) { candCell[k] = cell; candReg[k] = reg[cell - 1]; k++ }
                if (c < n - 1 && reg[cell + 1] >= 0) { candCell[k] = cell; candReg[k] = reg[cell + 1]; k++ }
            }
            var best = rnd.nextInt(k)
            repeat(samples - 1) {
                val o = rnd.nextInt(k)
                if (sizes[candReg[o]] < sizes[candReg[best]]) best = o
            }
            reg[candCell[best]] = candReg[best]
            sizes[candReg[best]]++
            left--
        }
        return reg
    }

    /** Mutates regions until the puzzle has exactly one solution. */
    private fun makeUnique(n: Int, sol: IntArray, reg: IntArray, rnd: Random): Boolean {
        val solCells = HashSet<Int>()
        for (r in 0 until n) solCells.add(r * n + sol[r])
        repeat(n * 12) {
            val alt = Solver(n, reg).findOtherSolution(sol) ?: return true
            // Cells where the alternative solution puts a cat but ours doesn't.
            val cands = (0 until n).map { it * n + alt[it] }.filter { it !in solCells }.shuffled(rnd)
            var changed = false
            for (cell in cands) {
                val from = reg[cell]
                val r = cell / n; val c = cell % n
                val neigh = buildList {
                    if (r > 0) add(reg[cell - n]); if (r < n - 1) add(reg[cell + n])
                    if (c > 0) add(reg[cell - 1]); if (c < n - 1) add(reg[cell + 1])
                }.filter { it != from }.distinct().shuffled(rnd)
                if (neigh.isEmpty()) continue
                if (!staysConnected(n, reg, from, cell)) continue
                reg[cell] = neigh.first()
                changed = true
                break
            }
            if (!changed) return false
        }
        return false
    }

    private fun staysConnected(n: Int, reg: IntArray, region: Int, removed: Int): Boolean {
        val cells = (0 until n * n).filter { reg[it] == region && it != removed }
        if (cells.isEmpty()) return false
        val seen = HashSet<Int>()
        val stack = ArrayDeque<Int>()
        stack.add(cells[0]); seen.add(cells[0])
        while (stack.isNotEmpty()) {
            val cell = stack.removeLast()
            val r = cell / n; val c = cell % n
            val nb = intArrayOf(
                if (r > 0) cell - n else -1, if (r < n - 1) cell + n else -1,
                if (c > 0) cell - 1 else -1, if (c < n - 1) cell + 1 else -1
            )
            for (x in nb) if (x >= 0 && x != removed && reg[x] == region && seen.add(x)) stack.add(x)
        }
        return seen.size == cells.size
    }
}

/** Constraint solver using "most constrained unit first" (rows, columns, regions). */
class Solver(private val n: Int, private val reg: IntArray) {
    private val units: Array<IntArray>
    private val unitsOfCell: Array<IntArray>
    private val blockedBy: Array<IntArray>

    init {
        val list = ArrayList<IntArray>()
        for (r in 0 until n) list.add(IntArray(n) { r * n + it })
        for (c in 0 until n) list.add(IntArray(n) { it * n + c })
        for (g in 0 until n) list.add((0 until n * n).filter { reg[it] == g }.toIntArray())
        units = list.toTypedArray()
        unitsOfCell = Array(n * n) { cell -> intArrayOf(cell / n, n + cell % n, 2 * n + reg[cell]) }
        blockedBy = Array(n * n) { cell ->
            val r = cell / n; val c = cell % n
            val s = LinkedHashSet<Int>()
            for (u in unitsOfCell[cell]) for (x in units[u]) s.add(x)
            for (dr in -1..1) for (dc in -1..1) {
                val rr = r + dr; val cc = c + dc
                if (rr in 0 until n && cc in 0 until n) s.add(rr * n + cc)
            }
            s.toIntArray()
        }
    }

    fun findOtherSolution(sol: IntArray): IntArray? {
        val found = ArrayList<IntArray>()
        solve(limit = 2, out = found)
        return found.firstOrNull { !it.contentEquals(sol) }
    }

    fun countSolutions(limit: Int = 2): Int {
        val found = ArrayList<IntArray>()
        solve(limit, found)
        return found.size
    }

    private fun solve(limit: Int, out: MutableList<IntArray>) {
        val block = IntArray(n * n)
        val done = BooleanArray(3 * n)
        val placed = IntArray(n) { -1 }

        fun rec(count: Int): Boolean {
            if (count == n) { out.add(placed.copyOf()); return out.size >= limit }
            var bestU = -1; var bestCnt = Int.MAX_VALUE
            for (u in units.indices) {
                if (done[u]) continue
                var k = 0
                for (x in units[u]) if (block[x] == 0) k++
                if (k == 0) return false
                if (k < bestCnt) { bestCnt = k; bestU = u }
            }
            for (cell in units[bestU]) {
                if (block[cell] != 0) continue
                for (x in blockedBy[cell]) block[x]++
                for (u in unitsOfCell[cell]) done[u] = true
                placed[cell / n] = cell % n
                val stop = rec(count + 1)
                placed[cell / n] = -1
                for (u in unitsOfCell[cell]) done[u] = false
                for (x in blockedBy[cell]) block[x]--
                if (stop) return true
            }
            return false
        }
        rec(0)
    }
}

/** Compact text form: "size|regions(base36)|solution(base36)|colours(base36)" */
object PuzzleCodec {
    fun encode(p: Puzzle): String = listOf(
        p.size.toString(),
        p.regions.joinToString("") { it.toString(36) },
        p.solution.joinToString("") { it.toString(36) },
        p.colorOf.joinToString("") { it.toString(36) },
    ).joinToString("|")

    fun decode(s: String): Puzzle {
        val (n, reg, sol, col) = s.trim().split("|")
        fun ints(x: String) = x.map { it.digitToInt(36) }.toIntArray()
        return Puzzle(n.toInt(), ints(reg), ints(sol), ints(col))
    }
}

/** Pre-generated campaign levels (assets/levels.txt), falls back to live generation. */
class LevelBank(private val openTags: () -> java.io.InputStream? = { null }, private val open: () -> java.io.InputStream?) {
    /** 0 normal, 1 "schwer", 2 "super schwer" per level (assets/level_tags.txt) */
    private val tags: String by lazy {
        runCatching { openTags()?.bufferedReader()?.readText()?.trim() }.getOrNull() ?: ""
    }

    fun tag(n: Int): Int = tags.getOrNull(n - 1)?.digitToIntOrNull() ?: 0

    private val lines: List<String> by lazy {
        runCatching { open()?.bufferedReader()?.readLines()?.filter { it.isNotBlank() } }.getOrNull() ?: emptyList()
    }

    fun level(n: Int): Puzzle {
        lines.getOrNull(n - 1)?.let { line -> runCatching { return PuzzleCodec.decode(line) } }
        val c = Levels.config(n)
        return PuzzleGenerator.generate(c.size, Levels.seed(n), c.difficulty, c.maxRating)
    }
}

/** Rates how hard a puzzle is to solve by human-style logic. */
object LogicRater {
    /** How a puzzle can be solved: hardest technique needed + how often the harder ones were needed. */
    data class Analysis(val rating: Int, val confinements: Int, val lookaheads: Int) {
        /** a single number for "how hard does it feel" */
        val score get() = confinements + lookaheads * 3 + if (rating >= 4) 25 else 0
    }

    fun rate(p: Puzzle): Int = analyze(p).rating

    fun analyze(p: Puzzle): Analysis {
        var confinements = 0
        var lookaheads = 0
        val n = p.size
        val cand = BooleanArray(n * n) { true }
        val placed = BooleanArray(n * n)
        val units = ArrayList<IntArray>()
        for (r in 0 until n) units.add(IntArray(n) { r * n + it })
        for (c in 0 until n) units.add(IntArray(n) { it * n + c })
        for (g in 0 until n) units.add((0 until n * n).filter { p.regions[it] == g }.toIntArray())
        val unitDone = BooleanArray(units.size)
        fun blocked(a: Int, b: Int): Boolean {
            val ra = a / n; val ca = a % n; val rb = b / n; val cb = b % n
            return ra == rb || ca == cb || p.regions[a] == p.regions[b] ||
                (kotlin.math.abs(ra - rb) <= 1 && kotlin.math.abs(ca - cb) <= 1)
        }
        fun place(cell: Int) {
            placed[cell] = true
            for (x in 0 until n * n) if (x != cell && blocked(cell, x)) cand[x] = false
            cand[cell] = false
            for ((i, u) in units.withIndex()) if (cell in u) unitDone[i] = true
        }
        var rating = 1
        var count = 0
        while (count < n) {
            // 1) a unit with exactly one candidate
            var progress = false
            for ((i, u) in units.withIndex()) {
                if (unitDone[i]) continue
                val c = u.filter { cand[it] }
                if (c.isEmpty()) return Analysis(5, confinements, lookaheads)
                if (c.size == 1) { place(c[0]); count++; progress = true; break }
            }
            if (progress) continue
            // 2) confinement: all candidates of a unit lie in one other unit
            for ((i, u) in units.withIndex()) {
                if (unitDone[i]) continue
                val c = u.filter { cand[it] }
                for ((j, v) in units.withIndex()) {
                    if (j == i || unitDone[j]) continue
                    if (c.all { it in v }) {
                        for (x in v) if (cand[x] && x !in c) { cand[x] = false; progress = true }
                    }
                }
                if (progress) break
            }
            if (progress) { rating = maxOf(rating, 2); confinements++; continue }
            // 3) lookahead: a candidate that would wipe out another unit
            for (x in 0 until n * n) {
                if (!cand[x]) continue
                val kills = units.withIndex().any { (i, u) ->
                    !unitDone[i] && x !in u && u.none { cand[it] && !blocked(x, it) }
                }
                if (kills) { cand[x] = false; progress = true; break }
            }
            if (progress) { rating = maxOf(rating, 3); lookaheads++; continue }
            return Analysis(4, confinements, lookaheads)
        }
        return Analysis(rating, confinements, lookaheads)
    }
}
