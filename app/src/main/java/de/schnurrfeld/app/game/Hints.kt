package de.schnurrfeld.app.game

import kotlin.math.abs

/**
 * An explained hint.
 * [focus]   cells the explanation talks about (outlined on the board)
 * [crosses] cells that can safely get a cross (shown as ghost crosses, applied on request)
 * [catCell] a cell where a cat must go
 * [removeCross] a cross the player set on a cell that actually needs a cat
 */
data class Hint(
    val title: String,
    val text: String,
    val focus: Set<Int> = emptySet(),
    val crosses: Set<Int> = emptySet(),
    val catCell: Int? = null,
    val removeCross: Int? = null,
)

object HintEngine {

    private val colorNames = listOf(
        "Rot", "Blau", "Gelb", "Grün", "Lila", "Türkis",
        "Orange", "Rosa", "Hellgrün", "Lavendel", "Braun", "Grau",
    )

    private class Unit(val kind: Int, val index: Int, val cells: IntArray) // kind 0 row, 1 column, 2 region

    /** Finds the simplest next logical step for the current board, with a human explanation. */
    fun find(p: Puzzle, cells: List<Int>): Hint {
        val n = p.size
        fun isCross(c: Int) = cells[c] == Cell.X || cells[c] == Cell.AUTO_X
        fun name(u: Unit) = when (u.kind) {
            0 -> "Zeile ${u.index + 1}"
            1 -> "Spalte ${u.index + 1}"
            else -> "Bereich ${colorNames[p.colorOf[u.index] % colorNames.size]}"
        }
        fun blocks(a: Int, b: Int): Boolean {
            val ra = a / n; val ca = a % n; val rb = b / n; val cb = b % n
            return ra == rb || ca == cb || p.regions[a] == p.regions[b] || (abs(ra - rb) <= 1 && abs(ca - cb) <= 1)
        }

        val units = buildList {
            for (r in 0 until n) add(Unit(0, r, IntArray(n) { r * n + it }))
            for (c in 0 until n) add(Unit(1, c, IntArray(n) { it * n + c }))
            for (g in 0 until n) add(Unit(2, g, (0 until n * n).filter { p.regions[it] == g }.toIntArray()))
        }
        val cats = (0 until n * n).filter { cells[it] == Cell.CAT }

        // 0) a cross on a cell that needs a cat would make the puzzle unsolvable
        (0 until n * n).firstOrNull { isCross(it) && p.isSolution(it) }?.let { c ->
            return Hint(
                "Ein ✕ ist falsch",
                "Auf diesem Feld steht ein ✕ – genau hier gehört aber eine Katze hin. " +
                    "Solange das ✕ dort steht, findet die Katze keinen Platz. Entferne es mit einem Tipp.",
                focus = setOf(c), removeCross = c,
            )
        }

        // 1) free cells that a placed cat already blocks
        cats.map { cat -> cat to (0 until n * n).filter { it != cat && cells[it] == Cell.EMPTY && blocks(cat, it) } }
            .filter { it.second.isNotEmpty() }
            .maxByOrNull { it.second.size }
            ?.let { (cat, free) ->
                val r = cat / n; val c = cat % n
                val area = units.first { it.kind == 2 && it.index == p.regions[cat] }
                return Hint(
                    "Die Katze blockiert Felder",
                    "Jede Zeile, jede Spalte und jeder Farbbereich bekommt genau eine Katze – und Katzen berühren sich nie. " +
                        "Diese Katze belegt also Zeile ${r + 1}, Spalte ${c + 1} und ${name(area)}, dazu alle Nachbarfelder. " +
                        "Auf die markierten Felder kann deshalb keine Katze mehr.",
                    focus = setOf(cat), crosses = free.toSet(),
                )
            }

        // what is logically known: cats, and cells excluded by crosses or by cats
        val excluded = BooleanArray(n * n) { c -> isCross(c) || cats.any { it != c && blocks(it, c) } }
        val done = units.map { u -> u.cells.any { cells[it] == Cell.CAT } }
        fun cand(u: Unit) = u.cells.filter { cells[it] != Cell.CAT && !excluded[it] }
        fun unitWord(u: Unit) = when (u.kind) { 0 -> "jede Zeile"; 1 -> "jede Spalte"; else -> "jeder Farbbereich" }

        // 2) a row, column or region with exactly one free cell left (regions first – easiest to see)
        for (u in units.sortedByDescending { it.kind }) {
            if (done[units.indexOf(u)]) continue
            val c = cand(u)
            if (c.size == 1) {
                return Hint(
                    "Nur noch ein Platz",
                    "In ${name(u)} ist nur noch ein einziges Feld frei – alle anderen sind schon ausgeschlossen. " +
                        "Da ${unitWord(u)} genau eine Katze braucht, muss sie dorthin. Doppeltippe das leuchtende Feld.",
                    focus = u.cells.toSet(), catCell = c[0],
                )
            }
        }

        // 3) all free cells of one unit lie inside another unit
        val order = units.sortedByDescending { it.kind }
        for (u in order) {
            if (done[units.indexOf(u)]) continue
            val cu = cand(u)
            if (cu.isEmpty()) continue
            for (v in units) {
                if (v === u || v.kind == u.kind || done[units.indexOf(v)]) continue
                if (!cu.all { it in v.cells }) continue
                val rest = cand(v).filter { it !in cu }
                if (rest.isEmpty()) continue
                return Hint(
                    "Eingesperrte Katze",
                    "Alle freien Felder von ${name(u)} liegen in ${name(v)}. Die Katze von ${name(u)} sitzt also auf jeden Fall " +
                        "irgendwo in ${name(v)} – und weil ${unitWord(v)} nur eine Katze hat, ist für die übrigen Felder von ${name(v)} kein Platz mehr.",
                    focus = cu.toSet(), crosses = rest.toSet(),
                )
            }
        }

        // 4) a cat on a cell would leave another unit without any free cell
        data class Kill(val cell: Int, val victim: Unit)
        val kills = (0 until n * n).filter { cells[it] != Cell.CAT && !excluded[it] }.mapNotNull { x ->
            units.withIndex().filter { (i, v) -> !done[i] && x !in v.cells }
                .map { it.value }
                .firstOrNull { v -> cand(v).all { blocks(x, it) } }
                ?.let { Kill(x, it) }
        }
        kills.groupBy { it.victim }.minByOrNull { (v, _) -> cand(v).size }?.let { (v, ks) ->
            val xs = ks.map { it.cell }.toSet()
            val one = xs.size == 1
            return Hint(
                "Was wäre, wenn …?",
                "Stell dir vor, auf ${if (one) "dem markierten Feld" else "einem der markierten Felder"} säße eine Katze: " +
                    "Sie würde alle freien Felder von ${name(v)} blockieren – durch ihre Zeile, Spalte, Farbe oder weil sie sie berührt. " +
                    "${name(v)} braucht aber eine Katze. Also ${if (one) "kommt dort ein ✕ hin" else "bekommen diese Felder ein ✕"}.",
                focus = cand(v).toSet(), crosses = xs,
            )
        }

        // 5) nothing simple left – reveal a safe cat
        val g = (0 until n).filter { !done[2 * n + it] }.minByOrNull { cand(units[2 * n + it]).size } ?: 0
        val cell = p.solutionCellOfRegion(g)
        return Hint(
            "Ein sicherer Platz",
            "Hier wird es knifflig – dafür bräuchte man mehrere Schritte im Voraus. " +
                "Ich verrate dir ein Feld in ${name(units[2 * n + g])}: Doppeltippe das leuchtende Feld.",
            focus = units[2 * n + g].cells.toSet(), catCell = cell,
        )
    }
}
