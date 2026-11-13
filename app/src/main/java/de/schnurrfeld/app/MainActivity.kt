package de.schnurrfeld.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.schnurrfeld.app.audio.SoundManager
import de.schnurrfeld.app.audio.VoiceManager
import de.schnurrfeld.app.data.Store
import de.schnurrfeld.app.data.ThemeMode
import de.schnurrfeld.app.data.Updater
import de.schnurrfeld.app.ui.components.UpdateDialog
import de.schnurrfeld.app.game.GameMode
import de.schnurrfeld.app.game.LevelBank
import de.schnurrfeld.app.ui.screens.GameScreen
import de.schnurrfeld.app.ui.screens.LevelsScreen
import de.schnurrfeld.app.ui.screens.MenuScreen
import de.schnurrfeld.app.ui.screens.SettingsScreen
import de.schnurrfeld.app.ui.theme.SchnurrTheme

sealed class Screen {
    data object Menu : Screen()
    data object Levels : Screen()
    data object Settings : Screen()
    data object Tutorial : Screen()
    data object Basket : Screen()
    data class Game(val mode: GameMode, val from: Screen = Menu) : Screen()
}

class App(val store: Store, val sound: SoundManager, val voice: VoiceManager, val levels: LevelBank) {
    /** a newer release found on GitHub, shown as a dialog */
    var update by mutableStateOf<de.schnurrfeld.app.data.ReleaseInfo?>(null)
    /** the automatic check runs once per app start */
    var autoChecked = false
    /** a game that was running when the app was closed or cleared from memory: it is continued once */
    var resume: de.schnurrfeld.app.game.GameSnapshot? = null
    fun takeResume(mode: GameMode): de.schnurrfeld.app.game.GameSnapshot? {
        val r = resume ?: return null
        if (r.mode() != mode) return null
        resume = null
        return r
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = App(Store(this), SoundManager(this), VoiceManager(this), LevelBank(openTags = { runCatching { assets.open("level_tags.txt") }.getOrNull() }, open = { runCatching { assets.open("levels.txt") }.getOrNull() }))
        app.voice.warmUp(app.store.settings.voice && app.store.settings.ttsFallback)
        app.resume = de.schnurrfeld.app.game.GameSnapshot.decode(app.store.savedGame)?.takeIf {
            app.store.settings.tutorialDone && it.mode() != null && System.currentTimeMillis() - it.savedAt < de.schnurrfeld.app.game.GameSnapshot.MAX_AGE_MS
        }

        setContent {
            val s = app.store.settings
            val dark = when (s.theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            SchnurrTheme(dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNav(app)
                }
            }
        }
    }

    override fun onDestroy() {
        app.voice.release()
        super.onDestroy()
    }
}

@Composable
private fun AppNav(app: App) {
    var screen by remember {
        mutableStateOf<Screen>(
            if (!app.store.settings.tutorialDone) Screen.Tutorial
            else app.resume?.mode()?.let { m -> Screen.Game(m, if (m is GameMode.Level) Screen.Levels else Screen.Menu) } ?: Screen.Menu
        )
    }
    val back: () -> Unit = {
        if (screen == Screen.Tutorial) app.store.updateSettings { it.copy(tutorialDone = true) }
        if (screen is Screen.Game) app.store.saveGame(null)      // left on purpose: nothing to resume
        screen = when (val s = screen) {
            is Screen.Game -> s.from
            else -> Screen.Menu
        }
    }
    BackHandler(enabled = screen != Screen.Menu, onBack = back)

    // look for a new version once per start (quietly – errors are ignored)
    LaunchedEffect(Unit) {
        if (!app.autoChecked && app.store.settings.autoUpdate) {
            app.autoChecked = true
            runCatching { Updater.check(BuildConfig.VERSION_NAME, BuildConfig.DEV) }.getOrNull()?.let { app.update = it }
        }
    }
    // never interrupt a puzzle or the tutorial – offer it on the menu or in the settings
    app.update?.let { info ->
        if (screen == Screen.Menu || screen == Screen.Settings) UpdateDialog(info, onDismiss = { app.update = null })
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
        contentKey = { if (it is Screen.Game) "game-${it.mode}" else it.toString() },
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        label = "nav",
    ) { s ->
        when (s) {
            Screen.Menu -> MenuScreen(app, navigate = { screen = it })
            Screen.Levels -> LevelsScreen(app, onBack = back, onPlay = { screen = Screen.Game(GameMode.Level(it), Screen.Levels) })
            Screen.Settings -> SettingsScreen(app, onBack = back)
            Screen.Tutorial -> de.schnurrfeld.app.ui.screens.TutorialScreen(app) {
                app.store.updateSettings { it.copy(tutorialDone = true) }
                screen = Screen.Menu
            }
            Screen.Basket -> de.schnurrfeld.app.ui.screens.BasketScreen(app, onBack = back)
            is Screen.Game -> GameScreen(app, s.mode, onBack = back, navigate = { screen = it.copy(from = s.from) }, onBasket = { screen = Screen.Basket })
        }
    }
}
