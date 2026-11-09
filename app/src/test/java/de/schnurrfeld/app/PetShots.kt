package de.schnurrfeld.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import de.schnurrfeld.app.audio.SoundManager
import de.schnurrfeld.app.audio.VoiceManager
import de.schnurrfeld.app.data.Breed
import de.schnurrfeld.app.data.Care
import de.schnurrfeld.app.data.Store
import de.schnurrfeld.app.game.LevelBank
import de.schnurrfeld.app.ui.screens.BasketScreen
import de.schnurrfeld.app.ui.screens.MenuScreen
import de.schnurrfeld.app.ui.screens.TutorialScreen
import de.schnurrfeld.app.ui.theme.SchnurrTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSystemClock
import java.io.File
import androidx.compose.ui.unit.dp
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class PetShots {
    @get:Rule val rule = createComposeRule()
    private val out = System.getenv("SHOT_DIR") ?: "build/shots"

    private fun app(): App {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = Store(ctx)
        store.updateSettings { it.copy(voice = false, sound = false) }
        return App(store, SoundManager(ctx), VoiceManager(ctx), LevelBank { File("src/main/assets/levels.txt").inputStream() })
    }

    private fun show(dark: Boolean = false, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { SchnurrTheme(dark) { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() } } }
    }

    private fun settle(ms: Long) = repeat((ms / 100).toInt()) {
        ShadowSystemClock.advanceBy(Duration.ofMillis(100)); rule.mainClock.advanceTimeBy(100)
    }

    private fun shot(name: String) = rule.onRoot().captureRoboImage("$out/$name.png")

    @Test fun tutorial() {
        val a = app()
        show { TutorialScreen(a) {} }
        settle(800); shot("tut_0_welcome")
        rule.onNodeWithTag("next").performClick(); settle(900); shot("tut_1_cross")
    }

    @Test fun tutorialEnd() {
        val a = app()
        show { TutorialScreen(a) {} }
        settle(500)
        rule.onNodeWithTag("next").performClick(); settle(500)
        // skip ahead through the steps the way a player would: via the skip button we'd leave, so just check the rules step
        rule.onNodeWithTag("board") // exists
    }

    private fun basket(name: String, stage: Int, log: String, breed: Breed? = null, intro: Boolean = true, dark: Boolean = false, ms: Long = 2500, setup: (App) -> Unit = {}) {
        val a = app()
        a.store.resetPet()
        a.store.updatePet {
            it.copy(introSeen = intro, stage = stage, seenStage = stage, log = log, breedOverride = breed, talers = 7,
                stock = mapOf(Care.FOOD to 3, Care.MILK to 2, Care.BRUSH to 5, Care.YARN to 0))
        }
        a.store.updateProgress { it.copy(unlocked = 30) }
        setup(a)
        show(dark) { BasketScreen(a) {} }
        settle(ms); shot(name)
    }

    @Test fun intro() = basket("pet_intro", 0, "", intro = false, ms = 1200)
    @Test fun stage0() = basket("pet_stage0", 0, "")
    @Test fun stage1() = basket("pet_stage1_tail_red", 1, "FFFFFFFF")
    @Test fun stage1Black() = basket("pet_stage1_tail_black", 1, "YYYYYYYY" + "Y".repeat(5))
    @Test fun stage2() = basket("pet_stage2_moppel", 2, "F".repeat(18))
    @Test fun stage6() = basket("pet_stage6_mitternacht", 6, "Y".repeat(90), setup = { it.store.updateProgress { p -> p.copy(unlocked = 80) } })
    @Test fun growsRightAway() = basket("pet_grows_now", 0, "FFFFFFF", ms = 400, setup = { it.store.updatePet { p -> p.copy(stock = mapOf(Care.FOOD to 3)) } }).also {
        // one more item completes stage 1 (8 items, level 5) – the cat should grow right away
        rule.onNodeWithTag("care_FOOD").performClick(); settle(1500); shot("pet_grows_now_after")
    }
    @Test fun stage4Dark() = basket("pet_stage4_wolke_dark", 4, "B".repeat(45), dark = true)
    @Test fun breeds() {
        for (b in Breed.entries) {
            val a = app()
            a.store.resetPet()
            a.store.updatePet { it.copy(introSeen = true, stage = 4, seenStage = 4, log = "F".repeat(42), breedOverride = b) }
        }
    }

    @Test fun machine() {
        val a = app()
        a.store.resetPet()
        a.store.updatePet { it.copy(introSeen = true) }
        a.store.updateProgress { it.copy(hints = 25, jokers = 4) }
        show { BasketScreen(a) {} }
        settle(800)
        rule.onNodeWithTag("machine").performClick(); settle(500); shot("machine_open")
        rule.onNodeWithTag("swap_hints").performClick(); settle(600); shot("machine_inflow")
        settle(700); shot("machine_crank")
        settle(800); shot("machine_coin")
    }

    @Test fun shop() = basket("shop", 1, "MMMMMMMMBBBBB", setup = { it.store.updatePet { p -> p.copy(discount = 1) } }, ms = 600).also {
        rule.onNodeWithTag("shop").performClick(); settle(600); shot("shop_open")
    }

    @Test fun menu() {
        val a = app()
        a.store.resetPet()
        a.store.updatePet { it.copy(log = "FFF") }   // a new stage is pending -> the basket row wobbles
        show { MenuScreen(a) {} }
        settle(1500); shot("menu_basket")
    }

    @Test fun icons() {
        show {
            androidx.compose.foundation.layout.Row {
                for (c in Care.entries) de.schnurrfeld.app.ui.components.CareIcon(c, 96.dp)
            }
        }
        settle(300); shot("icons")
    }

    private fun feed(name: String, care: Care, stage: Int, log: String, msList: List<Long>) {
        val a = app()
        a.store.resetPet()
        a.store.updatePet { it.copy(introSeen = true, stage = stage, seenStage = stage, log = log, stock = Care.entries.associateWith { 5 }) }
        a.store.updateProgress { it.copy(unlocked = 40) }
        show { BasketScreen(a) {} }
        settle(1500)
        rule.onNodeWithTag("care_${care.name}").performClick()
        var at = 0L
        for ((i, ms) in msList.withIndex()) { settle(ms - at); at = ms; shot("${name}_$i") }
    }

    @Test fun feedFood() = feed("act_food", Care.FOOD, 3, "F".repeat(28), listOf(400L, 2600L, 5000L))
    @Test fun feedMilk() = feed("act_milk", Care.MILK, 3, "M".repeat(28), listOf(2600L))
    @Test fun feedYarn() = feed("act_yarn", Care.YARN, 4, "Y".repeat(43), listOf(2500L, 4500L))
    @Test fun feedBrush() = feed("act_brush", Care.BRUSH, 4, "B".repeat(43), listOf(1800L))
    @Test fun feedHidden() = feed("act_hidden_milk", Care.MILK, 1, "FFFFFFFF", listOf(1800L))

    @Test @Config(qualifiers = "w360dp-h760dp-xxhdpi") fun bigFontMachineAndShop() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.3f)
        val a = app()
        a.store.resetPet()
        a.store.updatePet { it.copy(introSeen = true, stage = 1, seenStage = 1, log = "FFFFFFFF", talers = 9) }
        a.store.updateProgress { it.copy(hints = 7, jokers = 4, unlocked = 10) }
        show { BasketScreen(a) {} }
        settle(800)
        rule.onNodeWithTag("machine").performClick(); settle(600); shot("bigfont_machine")
    }

    @Test @Config(qualifiers = "w360dp-h760dp-xxhdpi") fun bigFontShop() {
        org.robolectric.RuntimeEnvironment.setFontScale(1.3f)
        val a = app()
        a.store.resetPet()
        a.store.updatePet { it.copy(introSeen = true, stage = 1, seenStage = 1, log = "FFFFFFFF", talers = 9) }
        show { BasketScreen(a) {} }
        settle(800)
        rule.onNodeWithTag("shop").performClick(); settle(600)
        rule.onNodeWithTag("buy_discount_1").performScrollTo(); settle(300); shot("bigfont_shop")
    }

    @Test fun helperCat() {
        val a = app()
        show { de.schnurrfeld.app.ui.screens.GameScreen(a, de.schnurrfeld.app.game.GameMode.Level(3), {}, {}) }
        settle(800)
        repeat(31) { ShadowSystemClock.advanceBy(Duration.ofMillis(1000)); rule.mainClock.advanceTimeBy(1000) }
        settle(900); shot("helper_nudge")
        repeat(31) { ShadowSystemClock.advanceBy(Duration.ofMillis(1000)); rule.mainClock.advanceTimeBy(1000) }
        settle(900); shot("helper_free_tip")
    }

    @Test fun jokerBig() {
        show { androidx.compose.foundation.layout.Row { de.schnurrfeld.app.ui.components.JokerIcon(140.dp); de.schnurrfeld.app.ui.components.JokerIcon(48.dp); de.schnurrfeld.app.ui.components.JokerIcon(30.dp) } }
        settle(200); shot("joker")
    }

    @Test fun sadAndHappy() {
        for ((name, hours) in listOf("pet_sad" to 60L, "pet_happy" to 0L)) {
            val a = app()
            a.store.resetPet()
            a.store.updatePet { it.copy(introSeen = true, stage = 4, seenStage = 4, log = "M".repeat(45), needs = de.schnurrfeld.app.data.Needs(),
                needsAt = System.currentTimeMillis() - hours * 3_600_000L, lastVisit = System.currentTimeMillis()) }
            a.store.updateProgress { it.copy(unlocked = 40) }
        }
    }

    @Test fun sadCat() {
        val a = app()
        a.store.resetPet()
        a.store.updatePet { it.copy(introSeen = true, stage = 4, seenStage = 4, log = "M".repeat(45), needs = de.schnurrfeld.app.data.Needs(),
            needsAt = System.currentTimeMillis() - 60 * 3_600_000L, lastVisit = System.currentTimeMillis()) }
        a.store.updateProgress { it.copy(unlocked = 40) }
        show { BasketScreen(a) {} }
        settle(9000); shot("pet_sad")
    }

    @Test fun dailyTaler() {
        val a = app()
        a.store.devResetDaily()
        show { MenuScreen(a) {} }
        settle(700); shot("daily_waiting")
        rule.onNodeWithTag("daily_taler").performClick()
        settle(500); shot("daily_burst")
        settle(900); shot("daily_burst2")
    }

    private fun winLevel(level: Int, name: String, basketNews: Boolean = false) {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = Store(ctx).apply { updateSettings { it.copy(voice = false, sound = false) } }
        store.resetPet()
        if (basketNews) store.updatePet { it.copy(introSeen = true, log = "FFFFFFFF") }   // enough care: the next level unlocks the tail
        val bank = LevelBank(openTags = { File("src/main/assets/level_tags.txt").inputStream() }, open = { File("src/main/assets/levels.txt").inputStream() })
        val a = App(store, SoundManager(ctx), VoiceManager(ctx), bank)
        show { de.schnurrfeld.app.ui.screens.GameScreen(a, de.schnurrfeld.app.game.GameMode.Level(level), {}, {}) }
        settle(800)
        val p = bank.level(level); val n = p.size
        val board = rule.onNodeWithTag("board")
        val cs = board.fetchSemanticsNode().size.width.toFloat() / n
        for (r in 0 until n) {
            val c = r * n + p.solution[r]
            repeat(2) { board.performTouchInput { click(androidx.compose.ui.geometry.Offset((c % n + 0.5f) * cs, (c / n + 0.5f) * cs)) }; settle(100) }
            settle(300)
        }
        settle(4200); shot(name)
        settle(3000)
    }

    @Test fun nextIsHard() = winLevel(13, "end_next_hard", basketNews = true)
    @Test fun nextIsSuperHard() = winLevel(38, "end_next_super_hard")
}
