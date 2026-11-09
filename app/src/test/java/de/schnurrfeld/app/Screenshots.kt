package de.schnurrfeld.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import de.schnurrfeld.app.audio.SoundManager
import de.schnurrfeld.app.audio.VoiceManager
import de.schnurrfeld.app.data.Store
import de.schnurrfeld.app.data.ThemeMode
import de.schnurrfeld.app.game.GameMode
import de.schnurrfeld.app.game.LevelBank
import de.schnurrfeld.app.ui.screens.GameScreen
import de.schnurrfeld.app.ui.screens.MenuScreen
import de.schnurrfeld.app.ui.screens.SettingsScreen
import de.schnurrfeld.app.ui.theme.SchnurrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSystemClock
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class Screenshots {
    @get:Rule val rule = createComposeRule()

    private val out = System.getenv("SHOT_DIR") ?: "build/shots"

    private fun app(): App {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = Store(ctx)
        store.updateSettings { it.copy(voice = false) }
        return App(store, SoundManager(ctx), VoiceManager(ctx), LevelBank { File("src/main/assets/levels.txt").inputStream() })
    }

    private fun setContent(dark: Boolean, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            SchnurrTheme(dark) { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() } }
        }
    }

    private fun settle(ms: Long = 1500) {
        repeat((ms / 100).toInt()) {
            ShadowSystemClock.advanceBy(Duration.ofMillis(100))
            rule.mainClock.advanceTimeBy(100)
        }
    }

    private fun shot(name: String) = rule.onRoot().captureRoboImage("$out/$name.png")

    @Test fun menuLight() { val a = app(); setContent(false) { MenuScreen(a) {} }; settle(); shot("menu_light") }
    @Test fun menuDark() { val a = app(); setContent(true) { MenuScreen(a) {} }; settle(); shot("menu_dark") }
    @Test fun settings() { val a = app(); setContent(true) { SettingsScreen(a, {}, {}) }; settle(); shot("settings_dark") }

    private fun playLevel(level: Int, dark: Boolean, name: String, cats: Int, wrong: Boolean = true, assist: Boolean = false) {
        val a = app()
        a.store.updateSettings { it.copy(colorAssist = assist, theme = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) }
        val p = a.levels.level(level)
        setContent(dark) { GameScreen(a, GameMode.Level(level), {}, {}) }
        settle(800)
        val n = p.size
        val board = rule.onNodeWithTag("board")
        val w = board.fetchSemanticsNode().size.width.toFloat()
        val cs = w / n
        fun tap(cell: Int) {
            board.performTouchInput { click(Offset((cell % n + 0.5f) * cs, (cell / n + 0.5f) * cs)) }
            settle(250)
        }
        // place cats (tap twice: X then cat)
        for (r in 0 until cats) { val c = r * n + p.solution[r]; tap(c); tap(c) }
        // a few X marks
        var xs = 0
        for (cell in 0 until n * n) {
            if (xs >= 4) break
            if (!p.isSolution(cell) && cell / n >= cats + 1) { tap(cell); xs++ }
        }
        if (wrong) { val bad = (0 until n * n).first { !p.isSolution(it) && it / n == n - 1 }; tap(bad); tap(bad) }
        settle(1300)
        shot(name)
    }

    @Test fun gameLight() = playLevel(8, false, "game_light_6x6", cats = 3)
    @Test fun gameDark() = playLevel(40, true, "game_dark_9x9", cats = 5)
    @Test fun gameBig() = playLevel(75, false, "game_light_12x12", cats = 6, wrong = false)
    @Test fun gameAssist() = playLevel(30, false, "game_assist_8x8", cats = 2, wrong = false, assist = true)
    private fun finishLevel(level: Int, mistake: Boolean, dark: Boolean, shots: List<Pair<Long, String>>) {
        val a = app()
        a.store.updateProgress { it.copy(bar = 4) }
        val p = a.levels.level(level)
        setContent(dark) { GameScreen(a, GameMode.Level(level), {}, {}) }
        settle(800)
        val n = p.size
        val board = rule.onNodeWithTag("board")
        val cs = board.fetchSemanticsNode().size.width.toFloat() / n
        fun tap(c: Int) { board.performTouchInput { click(Offset((c % n + 0.5f) * cs, (c / n + 0.5f) * cs)) } }
        if (mistake) {
            val bad = (0 until n * n).first { !p.isSolution(it) }
            tap(bad); settle(100); tap(bad); settle(600)
        }
        for (r in 0 until n) {
            val c = r * n + p.solution[r]
            tap(c); settle(100); tap(c); settle(500)
        }
        var at = 0L
        for ((t, name) in shots) { settle(t - at); at = t; shot(name) }
        settle(6000) // let the cheer finish before the test tears down
    }

    @Test fun perfectRound() = finishLevel(6, false, false, listOf(1000L to "perfect_slam", 2300L to "perfect_rays"))
    @Test fun normalWin() = finishLevel(9, true, true, listOf(1100L to "win_tinsel_dark"))

    @Test fun meltIntoBar() {
        val a = app()
        a.store.updateProgress { it.copy(bar = 3) }
        val p = a.levels.level(20)
        setContent(false) { GameScreen(a, GameMode.Level(20), {}, {}) }
        settle(800)
        val n = p.size
        val board = rule.onNodeWithTag("board")
        val cs = board.fetchSemanticsNode().size.width.toFloat() / n
        fun tap(c: Int) { board.performTouchInput { click(Offset((c % n + 0.5f) * cs, (c / n + 0.5f) * cs)) } }
        for (r in 0..1) { val c = r * n + p.solution[r]; tap(c); settle(100); tap(c); settle(2000) }
        val c = 2 * n + p.solution[2]
        tap(c); settle(100); tap(c)
        settle(300); shot("melt_1_word")
        settle(300); shot("melt_2_melting")
        settle(200); shot("melt_3_flowing")
        settle(1500)
    }

    @Test fun gameWon() {
        val a = app()
        val p = a.levels.level(3)
        setContent(false) { GameScreen(a, GameMode.Level(3), {}, {}) }
        settle(800)
        val n = p.size
        val board = rule.onNodeWithTag("board")
        val cs = board.fetchSemanticsNode().size.width.toFloat() / n
        for (r in 0 until n) {
            val c = r * n + p.solution[r]
            repeat(2) { board.performTouchInput { click(Offset((c % n + 0.5f) * cs, (c / n + 0.5f) * cs)) }; settle(250) }
        }
        settle(2500)
        shot("game_won")
        settle(6000)
    }
}
