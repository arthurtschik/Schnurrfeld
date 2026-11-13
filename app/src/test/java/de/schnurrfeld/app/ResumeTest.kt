package de.schnurrfeld.app

import androidx.test.core.app.ApplicationProvider
import de.schnurrfeld.app.audio.SoundManager
import de.schnurrfeld.app.audio.VoiceManager
import de.schnurrfeld.app.data.Store
import de.schnurrfeld.app.game.Cell
import de.schnurrfeld.app.game.GameController
import de.schnurrfeld.app.game.GameMode
import de.schnurrfeld.app.game.GameSnapshot
import de.schnurrfeld.app.game.LevelBank
import de.schnurrfeld.app.game.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ResumeTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()
    private fun store() = Store(ctx).apply { updateSettings { it.copy(sound = false, voice = false, vibration = false) } }
    private fun controller(mode: GameMode, store: Store, scope: CoroutineScope, resume: GameSnapshot? = null) =
        GameController(mode, store, SoundManager(ctx), VoiceManager(ctx), LevelBank { File("src/main/assets/levels.txt").inputStream() }, scope, animate = false, resume = resume)

    private suspend fun GameController.ready() { start(); while (status != Status.PLAYING) delay(10) }

    @Test fun snapshotSurvivesTheTextFormat() {
        val s = GameSnapshot('L', "42", "0123" + "0".repeat(21), 2, 77, 1, 3, 4, 0, 99L, 1_700_000_000_000L)
        assertEquals(s, GameSnapshot.decode(s.encode()))
    }

    @Test fun damagedSnapshotsAreIgnored() {
        val ok = GameSnapshot('E', "", "0".repeat(36), 3, 5, 0, 0, 0, 2, 5L, 1L).encode()
        assertNotNull(GameSnapshot.decode(ok))
        for (bad in listOf(null, "", "garbage", ok.replace("1|E", "2|E"), ok.replace("|3|5|", "|9|5|"), ok.replace("0".repeat(36), "0".repeat(35) + "7"),
            ok.replace("|3|5|", "|3|-5|"), ok.substringBeforeLast("|"), "$ok|extra")) {
            assertNull("should be rejected: $bad", GameSnapshot.decode(bad))
        }
    }

    @Test fun onlyValidGamesAreOffered() {
        val today = LocalDate.of(2026, 11, 13)
        fun snap(kind: Char, arg: String) = GameSnapshot(kind, arg, "0".repeat(25), 3, 0, 0, 0, 0, 0, 1L, 1L)
        assertEquals(GameMode.Level(7), snap('L', "7").mode(today))
        assertNull(snap('L', "9999").mode(today))
        assertNull(snap('L', "0").mode(today))
        assertEquals(GameMode.Daily(today), snap('D', "2026-11-13").mode(today))
        assertNull("yesterday's daily puzzle", snap('D', "2026-11-12").mode(today))
        assertEquals(GameMode.Endless, snap('E', "").mode(today))
        assertNull(snap('X', "").mode(today))
    }

    @Test fun aLevelContinuesWhereItWasLeft() = runBlocking {
        val a = controller(GameMode.Level(10), store(), this)
        a.ready()
        val p = a.puzzle!!
        val sol = p.solution[0]
        val wrong = (0 until p.size).first { it != sol }
        a.tap(wrong)                                            // a cross
        a.elapsed = 123
        // a wrong cat costs a life, the right one is placed
        kotlinx.coroutines.delay(1); org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(450))
        a.tap(sol); org.robolectric.shadows.ShadowSystemClock.advanceBy(java.time.Duration.ofMillis(120)); a.tap(sol)
        val saved = a.snapshot()!!
        assertEquals(1, saved.cells.count { it - '0' == Cell.CAT })

        // "the app was cleared from memory": a brand-new controller gets the saved line
        val b = controller(GameMode.Level(10), store(), this, GameSnapshot.decode(saved.encode()))
        b.ready()
        assertEquals(a.cells.toList(), b.cells.toList())
        assertEquals(123, b.elapsed)
        assertEquals(a.lives, b.lives)
        assertEquals(1, b.found)
        assertEquals(a.streak, b.streak)
    }

    @Test fun endlessContinuesWithTheSamePuzzle() = runBlocking {
        val a = controller(GameMode.Endless, store(), this)
        a.ready()
        val saved = a.snapshot()!!
        val b = controller(GameMode.Endless, store(), this, GameSnapshot.decode(saved.encode()))
        b.ready()
        assertEquals("same puzzle after the restart", a.puzzle!!.regions.toList(), b.puzzle!!.regions.toList())
        assertEquals(a.endlessSolved, b.endlessSolved)
    }

    @Test fun aSnapshotOfAnotherSizeIsIgnored() = runBlocking {
        val wrong = GameSnapshot('L', "10", "0".repeat(9), 1, 500, 2, 0, 0, 0, 1L, 1L)
        val c = controller(GameMode.Level(10), store(), this, wrong)
        c.ready()
        assertEquals(3, c.lives)
        assertEquals(0, c.elapsed)
        assertTrue(c.cells.all { it == Cell.EMPTY })
    }

    @Test fun finishedOrLoadingGamesHaveNothingToSave() = runBlocking {
        val c = controller(GameMode.Level(10), store(), this)
        assertNull("loading", c.snapshot())
        c.ready()
        assertNotNull(c.snapshot())
        val t = controller(GameMode.Tutorial, store(), this)
        t.ready()
        assertNull("the tutorial is never saved", t.snapshot())
    }

    @Test fun theStoreKeepsAndClearsTheLine() {
        val st = store()
        assertNull(st.savedGame)
        st.saveGame("x")
        assertEquals("x", Store(ctx).savedGame)
        st.saveGame(null)
        assertNull(Store(ctx).savedGame)
    }
}
