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
class HintShot {
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

    @Test fun hintCard() {
        val a = app()
        a.store.updateProgress { it.copy(bar = 6, hints = 3) }
        val p = a.levels.level(30)
        setContent(false) { GameScreen(a, GameMode.Level(30), {}, {}) }
        settle(800)
        val n = p.size
        val board = rule.onNodeWithTag("board")
        val cs = board.fetchSemanticsNode().size.width.toFloat() / n
        fun tap(c: Int) { board.performTouchInput { click(Offset((c % n + 0.5f) * cs, (c / n + 0.5f) * cs)) } }
        val c = 2 * n + p.solution[2]
        tap(c); settle(100); tap(c); settle(600)
        rule.onNodeWithText("Tipp").performClick()
        settle(900)
        shot("hint_card")
        rule.onNodeWithText("✕ setzen").performClick()
        settle(400)
        rule.onNodeWithText("Tipp").performClick()
        settle(900)
        shot("hint_card2")
    }

}
