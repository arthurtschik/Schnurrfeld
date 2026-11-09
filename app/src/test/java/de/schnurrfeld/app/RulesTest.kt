package de.schnurrfeld.app

import androidx.test.core.app.ApplicationProvider
import de.schnurrfeld.app.audio.SoundManager
import de.schnurrfeld.app.audio.VoiceManager
import de.schnurrfeld.app.data.Store
import de.schnurrfeld.app.game.Cell
import de.schnurrfeld.app.game.GameController
import de.schnurrfeld.app.game.GameMode
import de.schnurrfeld.app.game.LevelBank
import de.schnurrfeld.app.game.Status
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Duration
import org.robolectric.shadows.ShadowSystemClock
import de.schnurrfeld.app.audio.Praise

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RulesTest {
    @Test
    fun crossesAreFreeOnlyWrongCatsCostLives() = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = Store(ctx).apply { updateSettings { it.copy(sound = false, voice = false, vibration = false) } }
        val ctrl = GameController(
            GameMode.Level(10), store, SoundManager(ctx), VoiceManager(ctx),
            LevelBank { File("src/main/assets/levels.txt").inputStream() }, this, animate = false
        )
        ctrl.start()
        while (ctrl.status != Status.PLAYING) delay(10)
        val p = ctrl.puzzle!!
        val n = p.size
        val sol = p.solution[0]                     // solution cell of row 0
        val wrong = (0 until n).first { kotlin.math.abs(it - sol) > 0 } // a wrong cell in row 0
        fun pause() = ShadowSystemClock.advanceBy(Duration.ofMillis(450))   // slower than a double tap
        fun quick() = ShadowSystemClock.advanceBy(Duration.ofMillis(120))   // inside the double-tap window

        // single taps toggle crosses – as often as you like, even on the solution cell, no lives lost
        repeat(25) {
            for (cell in listOf(sol, wrong, n + 1, 2 * n)) {
                pause(); ctrl.tap(cell); assertEquals(Cell.X, ctrl.cells[cell])
                pause(); ctrl.tap(cell); assertEquals(Cell.EMPTY, ctrl.cells[cell])
            }
        }
        // long press and swipe also clear crosses for free
        pause(); ctrl.tap(n + 2); ctrl.longPress(n + 2); assertEquals(Cell.EMPTY, ctrl.cells[n + 2])
        ctrl.dragStart(n * 3); for (c in n * 3 + 1 until n * 4) ctrl.dragOver(c)
        ctrl.dragStart(n * 3); for (c in n * 3 + 1 until n * 4) ctrl.dragOver(c)
        assertEquals(3, ctrl.lives)

        // a quick double tap on a wrong cell: one life lost, the cell becomes a cross
        pause(); ctrl.tap(wrong); quick(); ctrl.tap(wrong)
        assertEquals(2, ctrl.lives)
        assertEquals(Cell.X, ctrl.cells[wrong])
        // ...and that cross can be tapped away for free again
        pause(); ctrl.tap(wrong)
        assertEquals(Cell.EMPTY, ctrl.cells[wrong])
        assertEquals(2, ctrl.lives)

        // a quick double tap on the right cell places the cat, no cost
        pause(); ctrl.tap(sol); quick(); ctrl.tap(sol)
        assertEquals(Cell.CAT, ctrl.cells[sol])
        assertEquals(2, ctrl.lives)
        assertEquals(1, ctrl.streak)
        // tapping a placed cat does nothing
        pause(); ctrl.tap(sol); assertEquals(Cell.CAT, ctrl.cells[sol])
    }

    @Test
    fun praiseEscalatesAndVaries() {
        // tiers never go down while the streak grows, and reach the top before the last cat
        for (size in 5..12) {
            val tiers = (1 until size).map { Praise.tierFor(it, size) }
            assertEquals(tiers, tiers.sorted())
            assertEquals(0, tiers.first())
            assertEquals(Praise.TOP_TIER, tiers.last())
        }
        // every word except the finale is reachable and words of a tier alternate
        val rnd = kotlin.random.Random(1)
        val seen = mutableSetOf<Praise>()
        var last: Praise? = null
        repeat(200) {
            val tier = it % (Praise.TOP_TIER + 1)
            val p = Praise.pick(tier, last, rnd)
            assertEquals(tier, p.tier)
            if (p.tier == last?.tier && tier != Praise.TOP_TIER) assert(p != last)
            seen += p; last = p
        }
        assertEquals(Praise.entries.filter { it.kind == de.schnurrfeld.app.audio.VoiceKind.PRAISE }.toSet(), seen)
    }

    private fun controller(level: Int, mode: GameMode = GameMode.Level(level)): Pair<GameController, Store> = runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = Store(ctx).apply {
            updateSettings { it.copy(sound = false, voice = false, vibration = false) }
            resetProgress()
        }
        val ctrl = GameController(
            mode, store, SoundManager(ctx), VoiceManager(ctx),
            LevelBank { File("src/main/assets/levels.txt").inputStream() }, this, animate = false
        )
        ctrl.start()
        while (ctrl.status != Status.PLAYING) delay(10)
        ctrl to store
    }

    private fun waitMs(ms: Long) = ShadowSystemClock.advanceBy(Duration.ofMillis(ms))

    /** waits [afterMs] (optionally setting a cross somewhere first), then double-taps [cell] */
    private fun GameController.catAfter(cell: Int, afterMs: Long, crossFirst: Boolean = false) {
        if (crossFirst) {
            waitMs(400)
            val n = puzzle!!.size
            val x = (0 until n * n).first { cells[it] == Cell.EMPTY && !puzzle!!.isSolution(it) }
            tap(x)
        }
        waitMs(afterMs)
        tap(cell); waitMs(100); tap(cell)
    }

    // backwards compatible helper for the hint test
    private fun GameController.cat(cell: Int) = catAfter(cell, 450)

    @Test
    fun comboBarFollowsTheFixedRules() {
        val (ctrl, store) = controller(40)
        val p = ctrl.puzzle!!; val n = p.size
        val sol = (0 until n).map { it * n + p.solution[it] }
        val hintsBefore = store.progress.hints

        ctrl.catAfter(sol[0], 1000)
        assertEquals(0, store.progress.bar); assertEquals(null, ctrl.banner)       // first cat: nothing
        ctrl.catAfter(sol[1], 30_000, crossFirst = true)
        assertEquals(0, store.progress.bar)                                        // slow, cross in between: 0
        ctrl.catAfter(sol[2], 30_000)
        assertEquals(2, store.progress.bar)                                        // Kombo +1, 3 in a row +1
        assertEquals("Kombo!", ctrl.banner?.text)
        ctrl.catAfter(sol[3], 11_000, crossFirst = true)
        assertEquals(4, store.progress.bar); assertEquals("Quick Kombo!", ctrl.banner?.text)   // 11 s: Quick +2, not Super fast
        ctrl.catAfter(sol[4], 5_000)
        assertEquals(7, store.progress.bar); assertEquals("Super fast!", ctrl.banner?.text)    // +3 (best type wins)
        ctrl.catAfter(sol[5], 9_000, crossFirst = true)
        assertEquals("Super fast!", ctrl.banner?.text)                             // always announced, also twice in a row
        assertEquals(2, store.progress.bar)                                        // 7 + 3 + 1 (6th in a row) = 11 -> prize
        assertEquals(hintsBefore + 1, store.progress.hints)                          // first prize: a tip
        // a mistake empties the bar
        val wrong = (0 until n * n).first { !p.isSolution(it) && ctrl.cells[it] == Cell.EMPTY }
        ctrl.catAfter(wrong, 1000)
        assertEquals(0, store.progress.bar)
    }

    @Test
    fun perfectDailyGivesPurrfectAndDailyPoints() {
        val (ctrl, store) = controller(0, GameMode.Daily(java.time.LocalDate.of(2026, 11, 4)))
        val p = ctrl.puzzle!!; val n = p.size
        for (r in 0 until n) ctrl.catAfter(r * n + p.solution[r], 30_000, crossFirst = r > 0)
        assertEquals(Status.WON, ctrl.status)
        assert(ctrl.perfect)
        val total = store.progress.bar + 9 * store.progress.prizes
        assertEquals(n / 3 + 3 + 1, total)    // 1 per 3 in a row, +3 Purrfect, +1 daily
        assertEquals(1, ctrl.talersEarned)     // +1 Taler for the daily puzzle
    }

    @Test
    fun helperCatShowsUpForBeginnersWhenStuck() {
        val (ctrl, store) = controller(3)
        val hints = store.progress.hints
        waitMs(20_000); ctrl.checkIdle()
        assertEquals(null, ctrl.nudge)                       // 20 s of thinking: nothing yet
        waitMs(11_000); ctrl.checkIdle()
        assert(ctrl.nudge != null && ctrl.helper != null)    // after 30 s a cell wiggles, the helper talks
        assertEquals(false, ctrl.freeTip)
        waitMs(31_000); ctrl.checkIdle()
        assertEquals(true, ctrl.freeTip)                     // still no new cat 30 s later: free tip
        ctrl.useFreeTip()
        assert(ctrl.hint != null)
        assertEquals(hints, store.progress.hints)            // didn't cost a tip
        assertEquals(false, ctrl.freeTip)
    }

    @Test
    fun helperIsOnlyForTheFirstTenLevels() {
        val (ctrl, _) = controller(11)
        waitMs(70_000); ctrl.checkIdle(); waitMs(40_000); ctrl.checkIdle()
        assertEquals(null, ctrl.nudge); assertEquals(false, ctrl.freeTip)
    }

    @Test
    fun anyInputResetsTheHelperTimer() {
        val (ctrl, _) = controller(5)
        val p = ctrl.puzzle!!
        val x = (0 until p.size * p.size).first { !p.isSolution(it) }
        repeat(4) { waitMs(20_000); ctrl.tap(x); ctrl.checkIdle() }     // always busy -> never nudged
        assertEquals(null, ctrl.nudge)
    }

    @Test
    fun hintsAreAlwaysCorrect() {
        for (level in listOf(3, 12, 25, 40, 55, 70)) {
            val (ctrl, store) = controller(level)
            val p = ctrl.puzzle!!; val n = p.size
            var steps = 0
            while (ctrl.status == Status.PLAYING && steps < 200) {
                store.updateProgress { it.copy(hints = 9) }
                assert(ctrl.useHint())
                val h = ctrl.hint!!.first
                for (c in h.crosses) assert(!p.isSolution(c)) { "level $level: wrong cross suggested: ${h.title}" }
                h.catCell?.let { assert(p.isSolution(it)) { "level $level: wrong cat suggested" } }
                when {
                    h.crosses.isNotEmpty() || h.removeCross != null -> ctrl.applyHint()
                    h.catCell != null -> ctrl.cat(h.catCell!!)
                    else -> error("empty hint")
                }
                steps++
            }
            assertEquals("level $level solved purely by hints", Status.WON, ctrl.status)
            println("level $level (${n}x$n) solved with $steps hints")
        }
    }
}
