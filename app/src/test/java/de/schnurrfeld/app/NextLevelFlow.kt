package de.schnurrfeld.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ApplicationProvider
import de.schnurrfeld.app.audio.SoundManager
import de.schnurrfeld.app.audio.VoiceManager
import de.schnurrfeld.app.data.Store
import de.schnurrfeld.app.game.GameMode
import de.schnurrfeld.app.game.LevelBank
import de.schnurrfeld.app.ui.screens.GameScreen
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

/** Perfect round -> tap "Nächstes Level" while the cheer is still running -> the next level must be fine. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class NextLevelFlow {
    @get:Rule val rule = createComposeRule()

    private fun settle(ms: Long) = repeat((ms / 100).toInt()) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(100)); rule.mainClock.advanceTimeBy(100)
    }

    @Test fun perfectThenNextLevel() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = Store(ctx).apply { updateSettings { it.copy(voice = false) } }
        val levels = LevelBank { File("src/main/assets/levels.txt").inputStream() }
        val a = App(store, SoundManager(ctx), VoiceManager(ctx), levels)
        rule.mainClock.autoAdvance = false
        rule.setContent {
            var mode by remember { mutableStateOf<GameMode>(GameMode.Level(6)) }
            SchnurrTheme(false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    androidx.compose.runtime.key(mode) { GameScreen(a, mode, {}, { mode = it.mode }) }
                }
            }
        }
        settle(800)
        repeat(3) { round ->
            val level = 6 + round
            val p = levels.level(level); val n = p.size
            val board = rule.onNodeWithTag("board")
            val cs = board.fetchSemanticsNode().size.width.toFloat() / n
            for (r in 0 until n) {
                val c = r * n + p.solution[r]
                repeat(2) { board.performTouchInput { click(Offset((c % n + 0.5f) * cs, (c / n + 0.5f) * cs)) }; settle(100) }
                settle(400)
            }
            settle(3700)                          // end panel visible, confetti still falling
            rule.onNodeWithText("Nächstes Level").performClick()
            settle(1000)
            val next = levels.level(level + 1).size
            // the next level loads on a background thread: give it real time, too
            var tries = 0
            while (rule.onAllNodesWithText("0/$next").fetchSemanticsNodes().isEmpty() && tries++ < 50) { Thread.sleep(20); settle(100) }
            rule.onNodeWithText("0/$next").fetchSemanticsNode()   // header of the next level is correct
            println("round $round ok: level ${level + 1} shows 0/$next")
        }
    }
}
