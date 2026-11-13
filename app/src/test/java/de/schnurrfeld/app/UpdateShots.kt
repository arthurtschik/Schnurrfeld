package de.schnurrfeld.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import de.schnurrfeld.app.audio.SoundManager
import de.schnurrfeld.app.audio.VoiceManager
import de.schnurrfeld.app.data.ReleaseInfo
import de.schnurrfeld.app.data.Store
import de.schnurrfeld.app.game.LevelBank
import de.schnurrfeld.app.ui.components.UpdateDialog
import de.schnurrfeld.app.ui.screens.SettingsScreen
import de.schnurrfeld.app.ui.theme.SchnurrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class UpdateShots {
    @get:Rule val rule = createComposeRule()
    private val out = System.getenv("SHOT_DIR") ?: "build/shots"

    @Test fun dialog() {
        val info = ReleaseInfo("1.3", "## Neu in 1.3\n- **Kratzbaum** für dein Kätzchen\n- Neue Spielzeuge\n- Kleine Fehler behoben",
            "https://example.org/a.apk", "Schnurrfeld-1.3.apk", 2_900_000)
        rule.setContent { SchnurrTheme(false) { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { UpdateDialog(info) {} } } }
        rule.waitForIdle()
        rule.onNode(isDialog()).captureRoboImage("$out/update_dialog.png")
    }

    @Test fun settings() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val app = App(Store(ctx), SoundManager(ctx), VoiceManager(ctx), LevelBank { File("src/main/assets/levels.txt").inputStream() })
        rule.setContent { SchnurrTheme(false) { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { SettingsScreen(app, {}) } } }
        rule.onNodeWithText("Fortschritt zurücksetzen").performScrollTo()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/update_settings.png")
    }

    @Test fun changelog() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val app = App(Store(ctx), SoundManager(ctx), VoiceManager(ctx), LevelBank { File("src/main/assets/levels.txt").inputStream() })
        rule.setContent { SchnurrTheme(false) { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { SettingsScreen(app, {}) } } }
        rule.onNodeWithTag("changelog_1.2.2").performScrollTo()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/changelog_a.png")
        rule.onNodeWithTag("changelog_1.2.2").performClick()
        rule.onNodeWithTag("changelog_1.4").performScrollTo()
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("$out/changelog_b.png")
    }
}
