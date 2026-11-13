package de.schnurrfeld.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.schnurrfeld.app.App
import de.schnurrfeld.app.audio.Haptic
import de.schnurrfeld.app.audio.Sfx
import de.schnurrfeld.app.audio.MEOW_VOLUME
import de.schnurrfeld.app.audio.meowFor
import de.schnurrfeld.app.audio.meowRate
import de.schnurrfeld.app.data.Breed
import de.schnurrfeld.app.data.Care
import de.schnurrfeld.app.data.Pet
import de.schnurrfeld.app.data.PetRules
import de.schnurrfeld.app.data.TailColor
import de.schnurrfeld.app.ui.components.BreedPortrait
import de.schnurrfeld.app.ui.components.CareIcon
import de.schnurrfeld.app.ui.components.CatAnim
import de.schnurrfeld.app.ui.components.Gap
import de.schnurrfeld.app.ui.components.HGap
import de.schnurrfeld.app.ui.components.JokerIcon
import de.schnurrfeld.app.ui.components.MachineIcon
import de.schnurrfeld.app.ui.components.TalerIcon
import de.schnurrfeld.app.ui.components.TopBar
import de.schnurrfeld.app.ui.components.drawBedBack
import de.schnurrfeld.app.ui.components.Spot
import de.schnurrfeld.app.ui.components.Tree
import de.schnurrfeld.app.ui.components.drawClimbingTree
import de.schnurrfeld.app.ui.components.drawSky
import de.schnurrfeld.app.ui.components.drawBedBlanket
import de.schnurrfeld.app.ui.components.drawBedFront
import de.schnurrfeld.app.ui.components.drawTailOverRim
import de.schnurrfeld.app.ui.components.drawFoodBowl
import de.schnurrfeld.app.ui.components.drawMilkBowl
import de.schnurrfeld.app.ui.components.drawYarnBall
import de.schnurrfeld.app.ui.components.drawCatSide
import de.schnurrfeld.app.ui.components.drawSlickerBrush
import de.schnurrfeld.app.ui.components.drawCareIcon
import de.schnurrfeld.app.ui.components.drawCat
import de.schnurrfeld.app.ui.components.drawMachine
import de.schnurrfeld.app.ui.components.drawTaler
import de.schnurrfeld.app.ui.components.furOf
import de.schnurrfeld.app.ui.components.lookOf
import de.schnurrfeld.app.ui.components.rememberFrameMillis
import de.schnurrfeld.app.ui.components.tailColorOf
import de.schnurrfeld.app.ui.theme.Fur
import de.schnurrfeld.app.ui.theme.Gold
import de.schnurrfeld.app.ui.theme.LocalDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

// ======================================================================= screen

@Composable
fun BasketScreen(app: App, sim: PetSim = remember { PetSim() }, onBack: () -> Unit) {
    val store = app.store
    val pet = store.pet
    var showMachine by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf<Care?>(null) }
    var shopOpen by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf<Int?>(null) }
    var treeNote by remember { mutableStateOf(false) }
    /** the cave whose room is open (1 / 2), 0 = none */
    var room by remember { mutableStateOf(0) }
    val settings = store.settings
    fun sfx(s: Sfx, v: Float = 1f, r: Float = 1f) { if (settings.sound) app.sound.play(s, v, r) }

    // back after a long time? the cat is overjoyed
    LaunchedEffect(Unit) {
        val away = store.visitBasket()
        if (pet.stage >= PetRules.HATCH_STAGE && away > 20 * 3_600_000L) sim.reunion(android.os.SystemClock.uptimeMillis() + 600)
    }
    // the cat grows as soon as care + level allow it – one stage at a time, each with its own moment
    LaunchedEffect(pet.given, store.levelReached, reveal, pet.introSeen) {
        if (pet.introSeen && reveal == null) { delay(500); store.petCheckEvolution() }
    }
    // a stage the player hasn't seen yet: play its moment once
    LaunchedEffect(pet.stage, pet.introSeen) {
        if (pet.introSeen && pet.seenStage < pet.stage) {
            if (pet.stage >= PetRules.HATCH_STAGE && pet.seenStage < PetRules.HATCH_STAGE) sim.startHatch()
            treeNote = pet.stage >= PetRules.KRATZ_STAGE && !pet.treeSeen
            reveal = pet.stage
            store.updatePet { it.copy(seenStage = it.stage, treeSeen = it.treeSeen || it.stage >= PetRules.KRATZ_STAGE) }
        } else if (pet.introSeen && pet.stage >= PetRules.KRATZ_STAGE && !pet.treeSeen) {
            reveal = TREE_NEWS
            store.updatePet { it.copy(treeSeen = true) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopBar("Katzenkörbchen", onBack) {
                Surface(
                    shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TalerIcon(20.dp); HGap(4.dp)
                        Text("${pet.talers}", style = MaterialTheme.typography.titleMedium)
                    }
                }
                HGap(6.dp)
                Surface(
                    onClick = { showMachine = true }, shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(44.dp).testTag("machine"),
                ) { Box(contentAlignment = Alignment.Center) { MachineIcon(36.dp) } }
                HGap(8.dp)
            }

            if (pet.stage >= PetRules.HATCH_STAGE) NeedsRow(app, sim)
            PetScene(app, pet, sim, Modifier.weight(1f).fillMaxWidth(), onRoom = { room = it })

            ProgressPanel(pet, store.levelReached)

            // care bar
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (care in Care.entries) {
                    val n = pet.stockOf(care)
                    Surface(
                        onClick = {
                            if (store.give(care)) {
                                sim.feed(care)
                                if (settings.vibration) app.sound.vibrate(Haptic.TICK)
                            } else showShop = care
                        },
                        modifier = Modifier.weight(1f).height(92.dp).testTag("care_${care.name}"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            CareIcon(care, 54.dp)
                            Text(if (n > 0) "×$n" else "+", style = MaterialTheme.typography.labelLarge,
                                color = if (n > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
                Button(onClick = { shopOpen = true }, modifier = Modifier.fillMaxWidth().height(48.dp).testTag("shop")) {
                    Text("Kaufen")
                }
            }
        }

        reveal?.let { st ->
            RevealCard(st, pet.breed, treeNote, Modifier.align(Alignment.Center)) { reveal = null }
        }
        if (room != 0) RoomOverlay(app, pet, sim, room) { room = 0 }
        if (!pet.introSeen) IntroOverlay { store.updatePet { it.copy(introSeen = true, seenStage = it.stage) } }
    }

    if (showMachine) MachineDialog(app) { showMachine = false }
    if (shopOpen || showShop != null) ShopDialog(app, showShop) { shopOpen = false; showShop = null }
}

// ======================================================================= simulation of the cat

private enum class Mode { IDLE, WALK, PLAY, DANCE, NAP_WALK, NAP_IN, NAP, NAP_OUT, SCRATCH_WALK, SCRATCH, GO, TREE_JUMP, REST, ENTER, INSIDE, LEAVE }
enum class Act { NONE, YARN, MILK, FOOD, BRUSH }

internal data class Heart(val x: Float, val y: Float, val born: Long)
internal data class Flight(val care: Care, val born: Long)

/**
 * Behaviour of the cat – lives outside of composition and is advanced every frame.
 * x positions are fractions of the scene width; the cat stands in front of the basket.
 */
class PetSim(seed: Long = System.nanoTime()) {
    var x = 0.32f; var facing = 1f
    var anim = CatAnim.WAG; private set
    private var mode = Mode.IDLE; private var until = 0L
    private var walkTo = 0.32f
    val inBasket get() = mode == Mode.NAP
    // jumps (hatching, into / out of the basket)
    var jumpStart = -1L; var jumpDur = 700L; var jumpFromX = 0f; var jumpToX = 0f; var jumpIn = false
    /** 0 = basket, 1 = a jump between the floor and a spot of the climbing tree */
    var jumpKind = 0
    var jumpFromSpot: Spot? = null; var jumpToSpot: Spot? = null
    /** where the cat stands on the climbing tree (null = on the floor) */
    var spot: Spot? = null; private set
    /** 0 = outside, 1 / 2 = at home in that cave (not visible) */
    var inside = 0; private set
    /** 0 = fully visible .. 1 = gone through the doorway (while entering / leaving) */
    var doorK = 0f; private set
    private var doorStart = 0L; private var doorHome = 0
    private var goSpot: Spot? = null; private var goHome = 0; private var goSleep = false
    /** a toy or the scent diffuser was tapped: the cat should come to this room as soon as it is free */
    private var lureHome = 0; private var lureGo = false; private var luredIn = false
    /** the room (1 / 2) the cat is on its way to, 0 = none */
    val headingHome: Int get() = when {
        lureHome != 0 -> lureHome
        mode == Mode.GO || mode == Mode.ENTER -> goHome
        mode == Mode.TREE_JUMP && jumpToSpot == Spot.DOOR_B && goHome == 2 -> 2
        else -> 0
    }
    private var restAnim = CatAnim.LOOK; private var restAnimUntil = 0L
    val scratching get() = mode == Mode.SCRATCH
    var scratchStart = 0L; private set
    private var lastMeow = 0L
    /** the cat is somewhere the care items cannot reach it (up in the tree / at home) */
    private val away get() = spot != null || inside != 0 || mode == Mode.ENTER || mode == Mode.LEAVE
    var hatching = false
    var petting = false; var petEnd = 0L
    // what the cat is doing with an item
    var act = Act.NONE; private set
    var actStart = 0L; private set
    var itemX = 0.5f; private set
    var level = 1f; private set            // bowls: how full
    var itemFade = 1f; private set
    var yarnX = 0.5f; private var yarnV = 0f; var yarnSpin = 0f; private set
    private var actPhase = 0               // 0 = walking there, 1 = doing it, 2 = done/fading
    fun actPhase() = actPhase
    private var pending: Care? = null
    private var swatUntil = 0L
    var wiggleUntil = 0L
    internal val hearts = ArrayList<Heart>()
    internal val flights = ArrayList<Flight>()
    /** sounds the screen should play (drained every frame) */
    val sounds = ArrayList<Sfx>()
    private var last = 0L
    private val rnd = Random(seed)
    private var lastHeart = 0L
    var stage = 0

    val wantsPurr get() = petting || (act == Act.BRUSH && actPhase == 1)

    fun startHatch() { hatching = true; jumpStart = -2L }
    fun feed(care: Care) {
        flights += Flight(care, -1L); pending = care
        // something to eat or play with: the cat drops its plan to climb or scratch and turns to the new item
        if (mode == Mode.GO || mode == Mode.SCRATCH_WALK) { mode = Mode.IDLE; until = 0L }
    }

    /** spawns a heart (at most every 220 ms); returns true if one was spawned */
    fun heart(x: Float, y: Float, now: Long): Boolean {
        if (now - lastHeart < 220) return false
        lastHeart = now; hearts += Heart(x + (rnd.nextFloat() - 0.5f) * 0.05f, y, now)
        return true
    }

    /** how the cat feels (set by the screen from the needs) */
    var mood: de.schnurrfeld.app.data.Mood = de.schnurrfeld.app.data.Mood.OK
    /** when the little "mew …" bubble appeared */
    var mewAt = 0L; private set
    private var reunionUntil = 0L
    /** back after a long time: the cat is overjoyed */
    fun reunion(now: Long) { reunionUntil = now + 3200; meow(now, long = true, minGap = 0) }

    /** a meow that fits the cat's age – but never too often */
    private fun meow(now: Long, long: Boolean = false, minGap: Long = 20_000) {
        if (stage < PetRules.HATCH_STAGE || now - lastMeow < minGap) return
        lastMeow = now; sounds += meowFor(stage, long, rnd)
    }
    /** the cat was touched: sometimes it answers */
    fun touched(now: Long) { if (rnd.nextFloat() < 0.3f) meow(now, minGap = 8_000) }

    private fun jump(now: Long, from: Float, to: Float, into: Boolean, dur: Long = 750, kind: Int = 0) {
        jumpStart = now; jumpFromX = from; jumpToX = to; jumpIn = into; jumpDur = dur; jumpKind = kind
    }
    /** jumps between the floor and a spot of the tree; the floor point is right below the spot */
    private fun spotJump(now: Long, from: Spot?, to: Spot?, dur: Long = 900) {
        val fx = from?.x ?: x
        val tx = to?.x ?: (from?.x ?: x)
        jump(now, fx, tx, false, dur, 1)
        jumpFromSpot = from; jumpToSpot = to; mode = Mode.TREE_JUMP
    }

    private fun enterDoor(now: Long, home: Int) { doorHome = home; doorStart = now; mode = Mode.ENTER; anim = CatAnim.WALK; luredIn = lureGo; lureGo = false }

    /** the cat is called into a room (1 = living room, 2 = bedroom): it finishes what it is doing and comes */
    fun lure(home: Int, now: Long) {
        if (stage < PetRules.KRATZ_STAGE || inside == home || headingHome == home) return
        lureHome = home
        when {
            mode == Mode.GO || mode == Mode.SCRATCH || mode == Mode.SCRATCH_WALK || mode == Mode.PLAY || mode == Mode.DANCE || mode == Mode.WALK -> mode = Mode.IDLE
            mode == Mode.INSIDE -> { mode = Mode.LEAVE; doorStart = now }
            mode == Mode.REST && spot != null -> spotJump(now, spot, null, 800)
        }
    }

    val jumping get() = jumpStart > 0 && last - jumpStart < jumpDur

    fun step(now: Long, basketX: Float) {
        val dt = if (last == 0L) 0f else ((now - last) / 1000f).coerceAtMost(0.1f)
        last = now
        for (i in flights.indices) if (flights[i].born < 0) flights[i] = flights[i].copy(born = now)
        flights.removeAll { it.born > 0 && now - it.born > 700 }
        hearts.removeAll { now - it.born > 1300 }

        if (jumpStart == -2L) { jump(now, basketX, 0.34f, false, 1500); mode = Mode.IDLE; until = now + 2200 }
        if (jumpStart > 0 && now - jumpStart >= jumpDur) {
            x = jumpToX; jumpStart = -1L
            if (hatching) meow(now, minGap = 0)            // the first "hello"
            hatching = false
            when (jumpKind) {
                1 -> {
                    spot = jumpToSpot
                    val s = spot
                    if (mode == Mode.TREE_JUMP) {
                        if (s == null) { mode = Mode.IDLE; until = now + 1500 }
                        else if (goHome == 2 && s == Spot.DOOR_B) enterDoor(now, 2)
                        else { mode = Mode.REST; until = now + (if (goSleep) 25_000L else 12_000L) + rnd.nextLong(14_000); restAnimUntil = 0L }
                    }
                    if (act != Act.NONE && actPhase == 0) actStart = now   // restart the walk to the item
                }
                else -> if (jumpIn) { mode = Mode.NAP; until = now + 25_000 + rnd.nextLong(30_000) }
            }
            jumpKind = 0
        }

        // a new item arrived: start the matching activity (wake up first if napping)
        pending?.let { care ->
            if (flights.any { it.care == care && now - it.born < 600 }) return@let
            pending = null
            if (stage < PetRules.HATCH_STAGE) { startAct(care, basketX, now); return@let }
            if (mode == Mode.NAP) { jump(now, basketX, basketX - 0.2f, false); mode = Mode.IDLE }
            // bowls and yarn go next to the cat; for brushing the cat moves to a comfy spot in the middle
            startAct(care, if (care == Care.BRUSH) x.coerceIn(0.3f, 0.75f)
                else if (x < 0.55f) (x + 0.17f).coerceAtMost(0.86f) else (x - 0.17f).coerceAtLeast(0.14f), now)
        }
        // the doorway animation runs on its own
        if (mode == Mode.ENTER) {
            doorK = ((now - doorStart) / 700f).coerceIn(0f, 1f); anim = CatAnim.WALK
            if (doorK >= 1f) { inside = doorHome; mode = Mode.INSIDE; until = now + 20_000 + rnd.nextLong(40_000) }
        } else if (mode == Mode.LEAVE) {
            doorK = 1f - ((now - doorStart) / 700f).coerceIn(0f, 1f); anim = CatAnim.WALK
            if (doorK <= 0f) {
                inside = 0; goSleep = false; goHome = 0; luredIn = false
                if (spot != null) { mode = Mode.REST; until = now + 2500; restAnimUntil = 0L } else { mode = Mode.IDLE; until = now + 1500 }
            }
        }
        // an item arrived while the cat is up in the tree or at home: come down / come out first, the activity waits
        if (act != Act.NONE && !jumping) {
            when {
                inside != 0 && mode != Mode.LEAVE -> { mode = Mode.LEAVE; doorStart = now }
                spot != null && inside == 0 && mode != Mode.ENTER && mode != Mode.LEAVE && mode != Mode.TREE_JUMP -> spotJump(now, spot, null, 800)
            }
        }
        stepAct(now, dt, basketX)

        if (jumping) { anim = CatAnim.BOUNCE; return }
        if (stage < PetRules.HATCH_STAGE) return
        if (act != Act.NONE) return
        if (petting || now < petEnd) { anim = CatAnim.PURR; return }
        if (now < reunionUntil) {
            anim = CatAnim.DANCE
            if ((now / 300) % 2 == 0L) heart(x, 0.55f, now)
            return
        }
        // a sad kitten mews quietly now and then
        if (mood == de.schnurrfeld.app.data.Mood.SAD && mode != Mode.NAP && now - mewAt > 7000 + rnd.nextLong(3000)) {
            mewAt = now; meow(now, long = rnd.nextBoolean(), minGap = 5_000)
        }

        when (mode) {
            Mode.WALK, Mode.NAP_WALK -> {
                val d = walkTo - x
                if (abs(d) < 0.01f) {
                    if (mode == Mode.NAP_WALK) { jump(now, x, basketX, true, 800); mode = Mode.NAP_IN }
                    else { mode = Mode.IDLE; anim = CatAnim.WAG; until = now + 1500 + rnd.nextLong(2500) }
                } else {
                    facing = if (d > 0) 1f else -1f
                    x += (if (d > 0) 1 else -1) * minOf(abs(d), 0.11f * dt)
                    anim = CatAnim.WALK
                }
                return
            }
            Mode.SCRATCH_WALK -> {
                val d = walkTo - x
                if (abs(d) < 0.01f) {
                    mode = Mode.SCRATCH; facing = -1f; scratchStart = now
                    until = now + 4600 + rnd.nextLong(2600)
                } else {
                    facing = if (d > 0) 1f else -1f
                    x += (if (d > 0) 1 else -1) * minOf(abs(d), 0.12f * dt)
                    anim = CatAnim.WALK
                }
                return
            }
            Mode.SCRATCH -> {
                anim = CatAnim.SCRATCH; facing = -1f
                if (mood == de.schnurrfeld.app.data.Mood.SAD || now > until) { mode = Mode.IDLE; anim = CatAnim.WAG; until = now + 1600 + rnd.nextLong(1500) }
                return
            }
            Mode.GO -> {
                if (mood == de.schnurrfeld.app.data.Mood.SAD && !lureGo) { mode = Mode.IDLE; until = now + 500; return }
                val tx = goSpot?.x ?: Tree.DOOR_A_X
                val d = tx - x
                if (abs(d) < 0.01f) {
                    if (goHome == 1) enterDoor(now, 1) else spotJump(now, null, goSpot)
                } else {
                    facing = if (d > 0) 1f else -1f
                    x += (if (d > 0) 1 else -1) * minOf(abs(d), 0.12f * dt)
                    anim = CatAnim.WALK
                }
                return
            }
            Mode.TREE_JUMP, Mode.ENTER, Mode.LEAVE -> return
            Mode.REST -> {
                if (now >= restAnimUntil) {
                    restAnim = if (goSleep) CatAnim.SLEEP else listOf(CatAnim.SLEEP, CatAnim.LOOK, CatAnim.TILT, CatAnim.WAG, CatAnim.LICK).random(rnd)
                    restAnimUntil = now + (if (restAnim == CatAnim.SLEEP) 9_000L else 3_500L) + rnd.nextLong(3_000)
                }
                anim = restAnim
                if (mood == de.schnurrfeld.app.data.Mood.SAD || now > until) spotJump(now, spot, null)
                return
            }
            Mode.INSIDE -> {
                if ((mood == de.schnurrfeld.app.data.Mood.SAD && !luredIn) || now > until) { mode = Mode.LEAVE; doorStart = now }
                return
            }
            Mode.NAP_IN -> return
            Mode.NAP -> {
                anim = CatAnim.SLEEP
                if (now > until) { jump(now, basketX, basketX - 0.2f, false); mode = Mode.IDLE; until = now + 1500 }
                return
            }
            else -> {}
        }
        // called into a room by a toy: go there now (the sad ones come too)
        if (lureHome != 0 && act == Act.NONE && pending == null && spot == null && inside == 0 && mode == Mode.IDLE) {
            goHome = lureHome; goSpot = Tree.spotAt(goHome); goSleep = false; lureGo = true; mode = Mode.GO; lureHome = 0; return
        }
        if (now < until || pending != null) return          // (an item is on its way: no new plans)
        val r = rnd.nextFloat()
        if (mood == de.schnurrfeld.app.data.Mood.SAD) {
            // sad: mostly sits there, looking around for you; no dancing
            when {
                r < 0.2f -> { mode = Mode.WALK; walkTo = 0.25f + rnd.nextFloat() * 0.5f }
                r < 0.9f -> { mode = Mode.IDLE; anim = listOf(CatAnim.LOOK, CatAnim.WAG).random(rnd); until = now + 4000 + rnd.nextLong(3000) }
                else -> if (stage >= PetRules.KRATZ_STAGE) { mode = Mode.IDLE; anim = CatAnim.LOOK; until = now + 4000 } else { mode = Mode.NAP_WALK; walkTo = basketX - 0.18f }
            }
            return
        }
        val happy = mood == de.schnurrfeld.app.data.Mood.HAPPY
        if (happy && r < 0.12f) { mode = Mode.DANCE; anim = CatAnim.DANCE; until = now + 2200; return }
        if (stage >= PetRules.KRATZ_STAGE) {
            val q = rnd.nextFloat()
            if (q < 0.12f) { mode = Mode.SCRATCH_WALK; walkTo = Tree.SCRATCH_X; return }
            if (q < 0.34f) { goSpot = listOf(Spot.ROOF_A, Spot.ROOF_B, Spot.STEP).random(rnd); goHome = 0; goSleep = rnd.nextFloat() < 0.45f; mode = Mode.GO; return }
            if (q < 0.50f) { goHome = if (rnd.nextBoolean()) 1 else 2; goSpot = Tree.spotAt(goHome); goSleep = true; mode = Mode.GO; return }
        }
        if (rnd.nextFloat() < 0.1f) meow(now, minGap = 25_000)
        when {
            r < 0.36f -> { mode = Mode.WALK; walkTo = 0.12f + rnd.nextFloat() * 0.76f }
            r < 0.62f -> { mode = Mode.IDLE; anim = listOf(CatAnim.WAG, CatAnim.LOOK, CatAnim.TILT, CatAnim.LICK).random(rnd); until = now + 3000 + rnd.nextLong(3000) }
            r < 0.74f -> { mode = Mode.PLAY; anim = CatAnim.BOUNCE; until = now + 1800 + rnd.nextLong(1500) }
            r < 0.80f -> { mode = Mode.DANCE; anim = CatAnim.DANCE; until = now + 2000 }
            else -> if (stage >= PetRules.KRATZ_STAGE) {
                goSpot = listOf(Spot.ROOF_A, Spot.ROOF_B, Spot.STEP).random(rnd); goHome = 0; goSleep = true; mode = Mode.GO   // a proper nap up in the tree
            } else { mode = Mode.NAP_WALK; walkTo = basketX - 0.18f }   // time for a proper nap in the basket
        }
    }

    /** for tests and screenshots: put the cat straight into a pose ("scratch", "roofA", "roofB", "step", "homeA", "homeB") */
    internal fun debugPose(pose: String, now: Long) {
        val far = now + 600_000
        when (pose) {
            "scratch" -> { x = Tree.SCRATCH_X; facing = -1f; mode = Mode.SCRATCH; scratchStart = now; until = far }
            "roofA", "roofB", "step", "doorB" -> {
                val s = when (pose) { "roofA" -> Spot.ROOF_A; "roofB" -> Spot.ROOF_B; "doorB" -> Spot.DOOR_B; else -> Spot.STEP }
                spot = s; x = s.x; mode = Mode.REST; goSleep = false; restAnim = CatAnim.LOOK; restAnimUntil = far; until = far
            }
            "homeA", "homeB" -> {
                val home = if (pose.endsWith("A")) 1 else 2
                spot = Tree.spotAt(home); x = Tree.doorX(home); inside = home; doorK = 1f; mode = Mode.INSIDE; until = far
            }
        }
    }

    private fun startAct(care: Care, at: Float, now: Long) {
        act = when (care) { Care.YARN -> Act.YARN; Care.MILK -> Act.MILK; Care.FOOD -> Act.FOOD; Care.BRUSH -> Act.BRUSH }
        actStart = now; itemX = at; level = 1f; itemFade = 1f; actPhase = 0
        yarnX = at; yarnV = if (at > 0.5f) -0.12f else 0.12f; yarnSpin = 0f
        mode = Mode.IDLE
    }

    private fun stepAct(now: Long, dt: Float, basketX: Float) {
        if (act == Act.NONE) return
        if (away || (jumping && jumpKind != 0)) return          // wait until the cat is back on the floor
        val hidden = stage < PetRules.HATCH_STAGE
        val age = now - actStart
        // walk to the item first (not for brushing – the brush comes to the cat)
        if (actPhase == 0) {
            if (hidden) { actPhase = 1; actStart = now; if (act != Act.BRUSH) sounds += Sfx.NOM; return }
            val target = if (act == Act.YARN) yarnX else itemX
            val d = target - x
            if (abs(d) < 0.015f || age > 4000) { actPhase = 1; actStart = now; if (act == Act.MILK || act == Act.FOOD) sounds += Sfx.NOM }
            else { facing = if (d > 0) 1f else -1f; x += (if (d > 0) 1 else -1) * minOf(abs(d), 0.16f * dt); anim = CatAnim.WALK }
            return
        }
        if (actPhase == 1) {
            when (act) {
                Act.MILK, Act.FOOD -> {
                    level = (1f - age / 4500f).coerceAtLeast(0f)
                    if (!hidden) anim = CatAnim.EAT
                    if (hidden) wiggleUntil = now + 200
                    if (age > 4500) { actPhase = 2; actStart = now }
                }
                Act.YARN -> {
                    // the ball rolls; the cat chases and swats it
                    yarnX += yarnV * dt
                    yarnV *= (1f - 0.9f * dt)
                    yarnSpin += yarnV * 900f * dt
                    if (yarnX < 0.1f) { yarnX = 0.1f; yarnV = abs(yarnV) }
                    if (yarnX > 0.9f) { yarnX = 0.9f; yarnV = -abs(yarnV) }
                    if (hidden) {
                        wiggleUntil = now + 200
                        yarnX += (basketX - yarnX) * dt * 1.5f
                    } else if (now < swatUntil) {
                        anim = CatAnim.SWAT                        // paw swipe in progress
                    } else {
                        val d = yarnX - x
                        if (abs(d) > 0.06f) {
                            facing = if (d > 0) 1f else -1f
                            x += (if (d > 0) 1 else -1) * minOf(abs(d) - 0.05f, 0.2f * dt)
                            anim = CatAnim.WALK
                        } else {
                            // close enough: swipe and send the ball rolling away
                            facing = if (d >= 0) 1f else -1f
                            swatUntil = now + 650
                            val dir = if (x < 0.25f) 1 else if (x > 0.75f) -1 else if (d >= 0) 1 else -1
                            yarnV = dir * (0.28f + rnd.nextFloat() * 0.18f)
                            anim = CatAnim.SWAT
                        }
                    }
                    if (age > 8000) { actPhase = 2; actStart = now }
                }
                Act.BRUSH -> {
                    if (hidden) wiggleUntil = now + 200
                    if (age > 5200) { actPhase = 2; actStart = now }
                }
                Act.NONE -> {}
            }
            return
        }
        // fade the item away, then a little lick
        itemFade = (1f - age / 700f).coerceAtLeast(0f)
        if (!hidden) anim = if (act == Act.YARN) CatAnim.WAG else CatAnim.LICK
        if (age > (if (act == Act.MILK || act == Act.FOOD) 1800 else 800)) {
            act = Act.NONE; until = now + 1200; mode = Mode.IDLE
        }
    }
}

// ======================================================================= scene

/** Fur for the hidden kitten: only the tail colour is known yet. */
private fun hiddenFur(tail: TailColor?): Fur = when (tail) {
    TailColor.RED -> furOf(Breed.MOPPEL)
    TailColor.BLACK -> furOf(Breed.MITTERNACHT)
    else -> furOf(Breed.WOLKE)
}

private const val HORIZON = 0.56f     // where the wall meets the floor
private const val GROUND = 0.9f       // the line the cat stands on (in front of the basket)
private const val BASKET_X = 0.6f

// the climbing tree (stage KRATZ_STAGE and up) is described in ClimbingTree.kt
private const val TREE_NEWS = 100         // "reveal" code for the one-time notice about the post

@Composable
private fun PetScene(app: App, pet: Pet, sim: PetSim, modifier: Modifier, onRoom: (Int) -> Unit) {
    val frame = rememberFrameMillis()
    val dark = LocalDark.current
    val settings = app.store.settings
    val stage = pet.stage
    sim.stage = stage

    // the simulation runs on the frame clock (independent of drawing)
    LaunchedEffect(sim) {
        while (true) {
            androidx.compose.runtime.withFrameMillis { }
            sim.step(android.os.SystemClock.uptimeMillis(), BASKET_X)
        }
    }
    // sounds + purring follow the simulation
    LaunchedEffect(Unit) {
        var purr = 0
        try {
            while (true) {
                delay(80)
                while (sim.sounds.isNotEmpty()) {
                    val sfx = sim.sounds.removeAt(0)
                    if (!settings.sound) continue
                    if (sfx.isMeow) app.sound.play(sfx, MEOW_VOLUME, meowRate(sfx, sim.stage)) else app.sound.play(sfx, 0.8f)
                }
                if (sim.wantsPurr && purr == 0 && settings.sound) purr = app.sound.loop(Sfx.PURR, if (stage < PetRules.HATCH_STAGE) 0.5f else 0.85f)
                if (!sim.wantsPurr && purr != 0) { app.sound.stop(purr); purr = 0 }
            }
        } finally { app.sound.stop(purr) }
    }

    Canvas(
        modifier
            .testTag("scene")
            .pointerInput(stage) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val w = size.width.toFloat(); val h = size.height.toFloat()
                    val catSize = catSizeFor(stage, w, h)
                    val bw = basketWidth(stage, w)
                    val bc = Offset(w * BASKET_X, h * (HORIZON + 0.06f))
                    val treeOn = stage >= PetRules.KRATZ_STAGE
                    val hit = if (stage >= PetRules.HATCH_STAGE && !sim.inBasket) {
                        // the cat can only be petted while it is in view
                        if (sim.inside != 0 || sim.doorK > 0.5f) false else {
                            val feet = sim.spot?.let { it.feetY * h } ?: (h * GROUND)
                            val cx = sim.x * w; val cy = feet - catSize * 0.43f
                            abs(down.position.x - cx) < catSize * 0.55f && abs(down.position.y - cy) < catSize * 0.6f
                        }
                    } else if (!treeOn) {
                        abs(down.position.x - bc.x) < bw * 0.5f && abs(down.position.y - bc.y) < bw * 0.25f
                    } else false
                    if (!hit) {
                        // a tap on one of the caves opens the room behind its door
                        if (treeOn) {
                            val home = Tree.homeAt(down.position.x / w, down.position.y / h)
                            if (home != 0 && waitForUpOrCancellation() != null) onRoom(home)
                        }
                        return@awaitEachGesture
                    }
                    sim.petting = true
                    sim.touched(frame.value)
                    if (settings.vibration) app.sound.vibrate(Haptic.TICK)
                    var pos = down.position
                    do {
                        if (sim.heart(pos.x / w, pos.y / h, frame.value)) app.store.petStroke()
                        val ev = awaitPointerEvent()
                        ev.changes.firstOrNull()?.let { pos = it.position }
                    } while (ev.changes.any { it.pressed })
                    sim.petting = false
                    sim.petEnd = frame.value + 700
                }
            }
    ) {
        val now = frame.value
        val t = now / 1000f
        val w = size.width; val h = size.height
        val horizon = h * HORIZON
        val ground = h * GROUND

        // ---- room
        drawRect(Brush.verticalGradient(if (dark) listOf(Color(0xFF2B2631), Color(0xFF241F29)) else listOf(Color(0xFFF7ECDD), Color(0xFFEFDDC6)), 0f, horizon), size = Size(w, horizon))
        val win = Offset(w * 0.22f, h * 0.2f); val ww = w * 0.22f
        // the sky outside follows the real time of day
        clipPath(androidx.compose.ui.graphics.Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(androidx.compose.ui.geometry.Rect(Offset(win.x - ww / 2, win.y - ww / 2), Size(ww, ww)), androidx.compose.ui.geometry.CornerRadius(ww * 0.12f)))
        }) { drawSky(androidx.compose.ui.geometry.Rect(Offset(win.x - ww / 2, win.y - ww / 2), Size(ww, ww)), t, 0) }
        val bar = if (dark) Color(0xFF4A4458) else Color.White
        drawLine(bar, Offset(win.x, win.y - ww / 2), Offset(win.x, win.y + ww / 2), ww * 0.05f)
        drawLine(bar, Offset(win.x - ww / 2, win.y), Offset(win.x + ww / 2, win.y), ww * 0.05f)
        drawRoundRect(if (dark) Color(0xFF5A4C44) else Color(0xFFB98B5E), Offset(win.x - ww / 2, win.y - ww / 2), Size(ww, ww), androidx.compose.ui.geometry.CornerRadius(ww * 0.12f), style = Stroke(ww * 0.06f))
        drawRect(Brush.verticalGradient(if (dark) listOf(Color(0xFF3B3129), Color(0xFF30281F)) else listOf(Color(0xFFDDBB90), Color(0xFFC9A273)), horizon, h), Offset(0f, horizon), Size(w, h - horizon))
        for (i in 1..7) {
            val y = horizon + (h - horizon) * (i / 7f) * (i / 7f)
            drawLine(if (dark) Color(0xFF2E2620) else Color(0xFFBF9668), Offset(0f, y), Offset(w, y), 1.5f)
        }
        drawLine(if (dark) Color(0xFF221C17) else Color(0xFFB48A5E), Offset(0f, horizon), Offset(w, horizon), 4f)
        val treeOn = stage >= PetRules.KRATZ_STAGE
        if (!treeOn) drawPlant(Offset(w * 0.09f, horizon + h * 0.05f), w * 0.15f, t, dark)
        // rug in front of the basket
        drawOval(if (dark) Color(0xFF5B3F55) else Color(0xFFF2B8C6), Offset(w * 0.08f, ground - h * 0.06f), Size(w * 0.84f, h * 0.13f))
        drawOval(if (dark) Color(0xFF6E4E67) else Color(0xFFF7CFD9), Offset(w * 0.15f, ground - h * 0.045f), Size(w * 0.7f, h * 0.1f))

        // ---- the climbing tree replaces the basket from the teenager stage on
        if (treeOn) drawClimbingTree(w, h, t, dark, rake = if (sim.scratching) (now - sim.scratchStart) / 1000f else -1f)

        // ---- basket
        val bw = basketWidth(stage, w)
        val bc = Offset(w * BASKET_X, horizon + h * 0.06f)
        val catSize = catSizeFor(stage, w, h)
        val growth = ((stage - PetRules.HATCH_STAGE).toFloat() / (PetRules.ADULT_STAGE - PetRules.HATCH_STAGE)).coerceIn(0f, 1f)
        val wiggle = if (sim.petting) 0.35f else if (now < sim.wiggleUntil) 1f else 0f
        if (!treeOn) drawBedBack(bc, bw)

        val hidden = stage < PetRules.HATCH_STAGE
        val breed = pet.breed ?: Breed.WOLKE
        val fur = if (hidden) hiddenFur(pet.tail) else furOf(breed)
        val sadness = if (sim.mood == de.schnurrfeld.app.data.Mood.SAD && !sim.petting && now > sim.petEnd) 1f else 0f
        val look = if (hidden) null else lookOf(breed, growth, sim.facing).copy(sad = sadness)
        val catOnCushion = Offset(bc.x, bc.y + bw * 0.02f - catSize * 0.25f)

        if (hidden) {
            drawHidden(pet, stage, bc, bw, t, wiggle)
        } else if (!treeOn) {
            if (sim.inBasket) drawCat(catOnCushion, catSize, fur, CatAnim.SLEEP, t, look = look)
            drawBedFront(bc, bw)
        }

        // ---- items on the floor (behind the cat when it eats from them)
        val itemGroundY = if (hidden) bc.y + bw * 0.33f else ground
        fun floorX(f: Float) = if (hidden) bc.x + bw * 0.42f else f * w
        val fade = sim.itemFade

        // ---- the cat
        if (!hidden && !sim.inBasket) {
            val cx = sim.x * w
            if (sim.jumping) {
                val k = ((now - sim.jumpStart) / sim.jumpDur.toFloat()).coerceIn(0f, 1f)
                val tx = (sim.jumpFromX + (sim.jumpToX - sim.jumpFromX) * k) * w
                val facing = if (sim.jumpToX > sim.jumpFromX) 1f else -1f
                if (sim.jumpKind == 0) {
                    val fromIn = !sim.jumpIn   // jumping out of the basket
                    val fromY = if (fromIn) catOnCushion.y else ground - catSize * 0.43f
                    val toY = if (sim.jumpIn) catOnCushion.y else ground - catSize * 0.43f
                    val y = fromY + (toY - fromY) * k - sin(k * PI.toFloat()) * h * 0.2f
                    drawCat(Offset(tx, y), catSize, fur, CatAnim.BOUNCE, t, look = lookOf(breed, growth, facing))
                    if (sim.hatching) sparkle(Offset(tx, y), catSize, now - sim.jumpStart)
                } else {
                    // between the floor and a spot of the tree
                    val fromFeet = sim.jumpFromSpot?.let { it.feetY * h } ?: ground
                    val toFeet = sim.jumpToSpot?.let { it.feetY * h } ?: ground
                    val feet = fromFeet + (toFeet - fromFeet) * k - sin(k * PI.toFloat()) * h * 0.12f
                    drawCat(Offset(tx, feet - catSize * 0.43f), catSize, fur, CatAnim.BOUNCE, t, look = lookOf(breed, growth, facing))
                }
            } else if (sim.act == Act.BRUSH && sim.actPhase() == 1) {
                val center = Offset(cx, ground - catSize * 0.43f)
                drawCatSide(center, catSize, fur, look, t, 1f)
                // the brush glides along the back, from the neck to the tail and back
                val u = catSize / 100f
                val k = (now - sim.actStart) / 900f
                val along = (sin(k * PI.toFloat()) * 0.5f + 0.5f)
                val back = Offset(center.x + (14f + 16f * along) * u, center.y + (-3f + 20f * along) * u)
                drawSlickerBrush(Offset(back.x + 10f * u, back.y - 12f * u), catSize * 0.34f, 235f)
                val bx = back.x; val by = back.y
                for (i in 0..2) {
                    val a = ((k * 1.3f + i * 0.33f) % 1f)
                    drawCircle(Color.White, catSize * 0.012f, Offset(bx + (i - 1) * catSize * 0.06f, by - a * catSize * 0.15f), alpha = 1f - a)
                }
                if ((now / 400) % 2 == 0L) sim.heart(cx / w, (ground - catSize * 0.95f) / h, now)
            } else {
                val feet = sim.spot?.let { it.feetY * h } ?: ground
                if (sim.inside != 0) {
                    // at home: not in view
                } else if (sim.doorK > 0f) {
                    // walking through the doorway: fades and shrinks a little
                    val sc = 1f - 0.2f * sim.doorK
                    withAlpha(1f - sim.doorK) { drawCat(Offset(cx, feet - catSize * sc * 0.43f - h * 0.012f * sim.doorK), catSize * sc, fur, sim.anim, t, look = look) }
                } else drawCat(Offset(cx, feet - catSize * 0.43f), catSize, fur, sim.anim, t, look = look)
            }
        }
        // the yarn ball rolls in front of the cat
        if (sim.act == Act.YARN) {
            val r = if (hidden) w * 0.035f else catSize * 0.13f
            withAlpha(fade) { drawYarnBall(Offset(sim.yarnX * w, (if (hidden) itemGroundY else ground + catSize * 0.06f) - r), r, sim.yarnSpin, 0f) }
        }
        // bowls in front of the cat
        if (sim.act == Act.MILK || sim.act == Act.FOOD) {
            val bx = floorX(sim.itemX)
            val by = if (hidden) itemGroundY else ground + catSize * 0.02f
            withAlpha(fade) {
                if (sim.act == Act.MILK) drawMilkBowl(Offset(bx, by), (if (hidden) bw * 0.24f else catSize * 0.5f), sim.level)
                else drawFoodBowl(Offset(bx, by), (if (hidden) bw * 0.24f else catSize * 0.5f), sim.level)
            }
        }
        if (hidden && sim.act == Act.BRUSH) {
            val k = (now - sim.actStart) / 700f
            drawSlickerBrush(Offset(bc.x + sin(k * PI.toFloat()) * bw * 0.15f, bc.y - bw * 0.1f), bw * 0.2f, 100f)
        }

        // ---- flying items (from the buttons into the scene)
        for (f in sim.flights) {
            if (f.born <= 0) continue
            val k = ((now - f.born) / 600f).coerceIn(0f, 1f)
            val start = Offset(w * (0.125f + 0.25f * f.care.ordinal), h * 1.05f)
            val end = when {
                hidden -> Offset(bc.x, bc.y - bw * 0.05f)
                f.care == Care.BRUSH -> Offset(sim.x * w, ground - catSize * 0.75f)
                else -> Offset(if (sim.x < 0.55f) (sim.x + 0.17f) * w else (sim.x - 0.17f) * w, ground)
            }
            val e = 1 - (1 - k) * (1 - k)
            val pos = Offset(start.x + (end.x - start.x) * e, start.y + (end.y - start.y) * e - sin(k * PI.toFloat()) * h * 0.25f)
            drawCareIcon(f.care, pos, w * 0.13f * (1f - 0.3f * k))
        }
        // ---- the sad little "mew …"
        if (!hidden && !sim.inBasket && now - sim.mewAt < 2200 && sim.mood == de.schnurrfeld.app.data.Mood.SAD) {
            val a = (1f - (now - sim.mewAt) / 2200f).coerceIn(0f, 1f)
            val bx = sim.x * w + catSize * 0.35f; val by = ground - catSize * 1.05f
            drawRoundRect(Color.White, Offset(bx, by), Size(catSize * 0.62f, catSize * 0.26f), androidx.compose.ui.geometry.CornerRadius(catSize * 0.13f), alpha = 0.95f * a)
            drawRoundRect(Color(0xFF8A7F99), Offset(bx, by), Size(catSize * 0.62f, catSize * 0.26f), androidx.compose.ui.geometry.CornerRadius(catSize * 0.13f), alpha = a, style = Stroke(catSize * 0.012f))
            drawContext.canvas.nativeCanvas.drawText("mew …", bx + catSize * 0.31f, by + catSize * 0.175f,
                android.graphics.Paint().apply { color = android.graphics.Color.argb((a * 255).toInt(), 90, 80, 110); textSize = catSize * 0.13f; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true; typeface = android.graphics.Typeface.DEFAULT_BOLD })
        }
        // ---- hearts
        for (hh in sim.hearts) {
            val a = (now - hh.born) / 1300f
            drawHeart(Offset(hh.x * w + sin(a * 9f) * 6f, hh.y * h - a * h * 0.18f), w * 0.035f * (0.7f + a), 1f - a)
        }
    }
}

/** draws [block] with reduced opacity */
private fun DrawScope.withAlpha(a: Float, block: DrawScope.() -> Unit) {
    if (a <= 0f) return
    if (a >= 0.999f) { block(); return }
    drawContext.canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), androidx.compose.ui.graphics.Paint().apply { alpha = a })
    block()
    drawContext.canvas.restore()
}

private fun basketWidth(stage: Int, w: Float) = w * (if (stage >= PetRules.BIG_BASKET_STAGE) 0.66f else 0.56f)

private fun DrawScope.drawPlant(base: Offset, s: Float, t: Float, dark: Boolean) {
    val leaf = if (dark) Color(0xFF4F8F64) else Color(0xFF6DBB83)
    val leafDark = if (dark) Color(0xFF3B6E4C) else Color(0xFF4E9A66)
    val sway = sin(t * 0.8f) * 3f
    for ((i, ang) in listOf(-40f, -15f, 10f, 35f, -60f, 55f).withIndex()) {
        rotate(ang + sway * (if (i % 2 == 0) 1 else -1), Offset(base.x, base.y - s * 0.55f)) {
            drawOval(if (i % 2 == 0) leaf else leafDark, Offset(base.x - s * 0.12f, base.y - s * 1.35f), Size(s * 0.24f, s * 0.8f))
        }
    }
    val pot = Path().apply {
        moveTo(base.x - s * 0.32f, base.y - s * 0.6f); lineTo(base.x + s * 0.32f, base.y - s * 0.6f)
        lineTo(base.x + s * 0.24f, base.y); lineTo(base.x - s * 0.24f, base.y); close()
    }
    drawPath(pot, if (dark) Color(0xFFA0583F) else Color(0xFFE08A64))
    drawRect(if (dark) Color(0xFF8A4A35) else Color(0xFFC9714E), Offset(base.x - s * 0.35f, base.y - s * 0.66f), Size(s * 0.7f, s * 0.12f))
}

private fun catSizeFor(stage: Int, w: Float, h: Float): Float {
    val base = minOf(w, h * 1.3f)
    return base * when {
        stage <= 2 -> 0.27f      // just hatched
        stage == 3 -> 0.32f      // baby
        stage == 4 -> 0.37f      // young cat
        stage == 5 -> 0.33f      // teenager (the climbing tree room is bigger, the cat looks smaller next to it)
        else -> 0.35f            // grown up
    }
}

/** Before hatching the kitten stays under the knitted blanket; from stage 1 its tail hangs over the rim. */
private fun DrawScope.drawHidden(pet: Pet, stage: Int, bc: Offset, bw: Float, t: Float, extraWiggle: Float) {
    val playing = stage >= 1 && (t % 7f) < 1.4f
    val wiggle = maxOf(extraWiggle, if (playing) 0.8f else 0f)
    val lump = 0.7f + if (playing) sin(t * 9f) * 0.2f else 0f
    drawBedBlanket(bc, bw, t, lump, wiggle)
    drawBedFront(bc, bw)
    if (stage >= 1) pet.tail?.let { val (b, sh) = tailColorOf(it); drawTailOverRim(bc, bw, t, b, sh, if (playing) 1.6f else 1f) }
}

private fun DrawScope.drawHeart(c: Offset, r: Float, alpha: Float) {
    if (alpha <= 0f) return
    val p = Path().apply {
        moveTo(c.x, c.y + r * 0.85f)
        cubicTo(c.x - r * 1.35f, c.y - r * 0.1f, c.x - r * 0.65f, c.y - r * 1.15f, c.x, c.y - r * 0.35f)
        cubicTo(c.x + r * 0.65f, c.y - r * 1.15f, c.x + r * 1.35f, c.y - r * 0.1f, c.x, c.y + r * 0.85f)
        close()
    }
    drawPath(p, Color(0xFFFF6F91), alpha = alpha.coerceIn(0f, 1f))
}

private fun DrawScope.sparkle(c: Offset, s: Float, age: Long) {
    val a = (age / 1500f).coerceIn(0f, 1f)
    for (i in 0 until 8) {
        val ang = i * PI.toFloat() / 4 + a * 2f
        val d = s * (0.4f + a * 0.5f)
        drawCircle(Color(0xFFFFE08A), s * 0.03f * (1 - a), Offset(c.x + kotlin.math.cos(ang) * d, c.y + sin(ang) * d), alpha = 1 - a)
    }
}

// ======================================================================= info panel

@Composable
private fun ProgressPanel(pet: Pet, level: Int) {
    val next = pet.stage + 1
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).testTag("progress"),
        shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pet.breed?.let { "${it.title} · ${PetRules.stageNames[pet.stage]}" } ?: PetRules.stageNames[pet.stage],
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)
                )
                Text("Stufe ${pet.stage}/${PetRules.MAX_STAGE}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (next > PetRules.MAX_STAGE) {
                Gap(4.dp)
                Text("Voll ausgewachsen! 🎉 Kleidung und der Pappkarton-Palast kommen mit den nächsten Updates.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            Text("Nächste Stufe: ${PetRules.stageNames[next]}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Gap(8.dp)
            val from = PetRules.itemsFor[pet.stage]; val to = PetRules.itemsFor[next]
            val done = (pet.given - from).coerceIn(0, to - from); val need = to - from
            GoalRow("Pflege", done, need, if (done >= need) "geschafft" else if (need - done == 1) "noch 1 Item" else "noch ${need - done} Items", Color(0xFFF08BB0))
            Gap(6.dp)
            val needLevel = PetRules.levelFor[next]
            if (needLevel > 0) GoalRow("Level", level.coerceAtMost(needLevel), needLevel,
                if (level >= needLevel) "geschafft" else "noch ${needLevel - level} Level", Color(0xFF64B5F6))
            else Text("Level: keine Voraussetzung", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoalRow(label: String, value: Int, max: Int, hint: String, color: Color) {
    val ok = value >= max
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.widthIn(min = 52.dp))
        LinearProgressIndicator(
            progress = { if (max == 0) 1f else value.toFloat() / max },
            modifier = Modifier.weight(1f).height(10.dp),
            color = if (ok) Color(0xFF3FA36B) else color, trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, drawStopIndicator = {}, gapSize = 0.dp,
        )
        HGap(8.dp)
        Text("$value/$max", style = MaterialTheme.typography.labelLarge, modifier = Modifier.widthIn(min = 44.dp))
    }
    Text(if (ok) "✓ $hint" else hint, style = MaterialTheme.typography.labelSmall,
        color = if (ok) Color(0xFF3FA36B) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 52.dp))
}

@Composable
private fun RevealCard(stage: Int, breed: Breed?, treeNote: Boolean, modifier: Modifier, onClose: () -> Unit) {
    val name = breed?.title ?: "Dein Kätzchen"
    val news = stage == TREE_NEWS
    val tree = "\n\nNeu im Zimmer: ein großer Kratzbaum mit zwei Wohnhöhlen. Tippe auf eine Höhle, um nachzusehen, ob $name zu Hause ist."
    val text = when (stage) {
        TREE_NEWS -> "Das Körbchen ist zu klein geworden: Jetzt steht ein großer Kratzbaum im Zimmer. $name kratzt, klettert, schläft oben auf den Kissen oder zieht sich in eine Wohnhöhle zurück. Tippe auf eine Höhle, um nachzusehen!"
        1 -> "Da wackelt etwas! Ein Schwänzchen schaut aus der Decke."
        2 -> "Hallo, $name! Dein Kätzchen ist aus dem Körbchen gesprungen!"
        3 -> "$name ist jetzt eine Babykatze und wächst und wächst."
        4 -> "$name ist eine Jungkatze! Und es gibt ein größeres Körbchen."
        5 -> "$name ist jetzt eine Teenie-Katze – fast schon groß! Das Körbchen ist zu klein geworden."
        6 -> "$name ist ausgewachsen! Eine wunderschöne Katze."
        else -> "$name hat sich weiterentwickelt!"
    } + (if (treeNote && !news) tree else "")
    LaunchedEffect(stage) { delay(if (stage == PetRules.HATCH_STAGE) 1700 else 400) }
    Surface(
        modifier = modifier.widthIn(max = 420.dp).padding(24.dp), shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (news) "✨ Neu im Zimmer ✨" else "✨ Neue Stufe ✨", style = MaterialTheme.typography.labelLarge, color = Gold)
            Gap(6.dp)
            Text(if (news) "Ein Kratzbaum!" else PetRules.stageNames[stage], style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Gap(8.dp)
            Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Gap(14.dp)
            Button(onClick = onClose) { Text("Wie süß!") }
        }
    }
}

@Composable
private fun IntroOverlay(onDone: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.97f)).testTag("intro")) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Gap(12.dp)
            Text("Wer steckt wohl im Körbchen?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Gap(6.dp)
            Text(
                "In deinem Körbchen schlummert ein kleines Bündel. Kümmere dich darum – und eines Tages springt eine dieser Katzen heraus. Welche? Das liegt ganz bei dir …",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Gap(14.dp)
            for (row in Breed.entries.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for ((i, b) in row.withIndex()) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            BreedPortrait(b, 110.dp, listOf(CatAnim.WAG, CatAnim.LOOK, CatAnim.TILT, CatAnim.LICK)[(b.ordinal + i) % 4], phase = b.ordinal * 0.7f)
                            Text(b.title, style = MaterialTheme.typography.titleMedium)
                            Text(b.blurb, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Gap(10.dp)
            }
            Gap(8.dp)
            Text(
                "Taler bekommst du am Taler-Automaten oben rechts: Tausche dort übrige Tipps und Joker ein.",
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Gap(14.dp)
            Button(onClick = onDone, modifier = Modifier.testTag("intro_done")) { Text("Zum Körbchen") }
            Gap(20.dp)
        }
    }
}

// ======================================================================= Taler-Automat

@Composable
private fun MachineDialog(app: App, onClose: () -> Unit) {
    val store = app.store
    val scope = rememberCoroutineScope()
    val crank = remember { Animatable(0f) }
    val inflow = remember { Animatable(0f) }
    val coin = remember { Animatable(0f) }
    var paying by remember { mutableStateOf<Boolean?>(null) } // true = hints, false = jokers
    var busy by remember { mutableStateOf(false) }
    val frame = rememberFrameMillis()
    fun sfx(s: Sfx, v: Float = 1f) { if (store.settings.sound) app.sound.play(s, v) }

    fun run(hints: Boolean) {
        if (busy) return
        busy = true; paying = hints
        scope.launch {
            coin.snapTo(0f); crank.snapTo(0f); inflow.snapTo(0f)
            inflow.animateTo(1f, tween(900, easing = LinearEasing))
            if (if (hints) store.exchangeHints() else store.exchangeJokers()) {
                sfx(Sfx.CRANK, 0.9f)
                crank.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
                coin.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
                sfx(Sfx.COIN)
                if (store.settings.vibration) app.sound.vibrate(Haptic.SUCCESS)
                delay(700)
            }
            paying = null; busy = false
            coin.snapTo(0f); inflow.snapTo(0f); crank.snapTo(0f)
        }
    }

    Dialog(onDismissRequest = { if (!busy) onClose() }) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Taler-Automat", style = MaterialTheme.typography.headlineSmall)
                Text("Tausche übrige Tipps und Joker gegen Katzentaler.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Gap(8.dp)
                Box(Modifier.size(260.dp).testTag("machine_canvas")) {
                    Canvas(Modifier.fillMaxSize()) {
                        val t = frame.value / 1000f
                        val c = Offset(size.width / 2, size.height * 0.58f)
                        val s = size.width * 0.68f
                        drawMachine(c, s, crank.value, if (crank.value in 0.01f..0.99f) 1f else 0f, t)
                        val u = s / 100f
                        // tokens flowing into the slot
                        val k = inflow.value
                        if (paying != null && k in 0.001f..0.999f) {
                            for (i in 0 until 6) {
                                val kk = ((k * 1.6f) - i * 0.12f).coerceIn(0f, 1f)
                                if (kk <= 0f || kk >= 1f) continue
                                val start = Offset(if (paying == true) size.width * 0.12f else size.width * 0.88f, size.height * 0.95f)
                                val end = Offset(c.x - 8f * u, c.y + 18f * u)
                                val pos = Offset(start.x + (end.x - start.x) * kk, start.y + (end.y - start.y) * kk - sin(kk * PI.toFloat()) * 60f)
                                if (paying == true) drawTipToken(pos, 7.5f * u * (1 - kk * 0.45f)) else drawJokerToken(pos, 8.5f * u * (1 - kk * 0.45f))
                            }
                        }
                        // the Taler drops out of the chute
                        val ck = coin.value
                        if (ck > 0f) {
                            // rolls out of the chute and bounces once in front of the machine
                            val drop = if (ck < 0.6f) ck / 0.6f else 1f
                            val bounce = if (ck >= 0.6f) sin((ck - 0.6f) / 0.4f * PI.toFloat()) * 6f * u else 0f
                            val y = c.y + 35f * u + drop * 14f * u - bounce
                            drawTaler(Offset(c.x, y), 8f * u * (0.7f + 0.4f * drop), if (ck < 0.6f) abs(kotlin.math.cos(ck * 14f)) else 1f)
                        }
                    }
                }
                Text("Deine Taler: ${store.pet.talers}", style = MaterialTheme.typography.titleMedium)
                Gap(10.dp)
                ExchangeRow(
                    icon = { androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.TipsAndUpdates, null, tint = Gold, modifier = Modifier.size(26.dp)) },
                    have = store.progress.hints, need = PetRules.HINTS_PER_TALER, label = "Tipps", enabled = !busy, tag = "swap_hints"
                ) { run(true) }
                Gap(8.dp)
                ExchangeRow(
                    icon = { JokerIcon(30.dp) },
                    have = store.progress.jokers, need = PetRules.JOKERS_PER_TALER, label = "Joker", enabled = !busy, tag = "swap_jokers"
                ) { run(false) }
                Gap(6.dp)
                TextButton(onClick = onClose, enabled = !busy) { Text("Fertig") }
            }
        }
    }
}

private fun DrawScope.drawTipToken(c: Offset, r: Float) {
    drawCircle(Gold, r, c); drawCircle(Color(0xFFB7821A), r, c, style = Stroke(r * 0.18f))
    drawCircle(Color.White, r * 0.3f, Offset(c.x - r * 0.15f, c.y - r * 0.2f), alpha = 0.9f)
}

private fun DrawScope.drawJokerToken(c: Offset, r: Float) {
    drawCircle(Color(0xFF8E5BD8), r, c); drawCircle(Color(0xFF5E3A9A), r, c, style = Stroke(r * 0.18f))
    drawCircle(Color(0xFFF5C23D), r * 0.35f, c)
}

@Composable
private fun ExchangeRow(icon: @Composable () -> Unit, have: Int, need: Int, label: String, enabled: Boolean, tag: String, onSwap: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) { icon() }
            HGap(10.dp)
            Column(Modifier.weight(1f)) {
                Text("$need $label", style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("= ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TalerIcon(14.dp); HGap(3.dp)
                    Text("1 Taler", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                Gap(5.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { (have.toFloat() / need).coerceAtMost(1f) },
                        modifier = Modifier.weight(1f).height(6.dp),
                        color = if (have >= need) Color(0xFF3FA36B) else Gold, trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, drawStopIndicator = {}, gapSize = 0.dp,
                    )
                    HGap(6.dp)
                    Text("$have / $need", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            HGap(10.dp)
            Button(
                onClick = onSwap, enabled = enabled && have >= need, modifier = Modifier.testTag(tag),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) { Text("Tauschen", maxLines = 1) }
        }
    }
}

// ======================================================================= shop

@Composable
private fun ShopDialog(app: App, focus: Care?, onClose: () -> Unit) {
    val store = app.store
    val pet = store.pet
    var msg by remember { mutableStateOf<String?>(null) }
    fun buy(ok: Boolean, text: String) {
        msg = if (ok) text else "Dafür reichen deine Taler noch nicht."
        if (ok && store.settings.sound) app.sound.play(Sfx.COIN, 0.7f)
    }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(18.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Kaufen", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    TalerIcon(22.dp); HGap(4.dp); Text("${pet.talers}", style = MaterialTheme.typography.titleMedium)
                }
                if (focus != null) Text("${focus.title} ist leer – hol Nachschub!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                msg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                Gap(10.dp)
                for (care in Care.entries) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        CareIcon(care, 40.dp); HGap(10.dp)
                        Column(Modifier.weight(1f)) {
                            Text(care.title, style = MaterialTheme.typography.titleSmall)
                            Text("Vorrat: ${pet.stockOf(care)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            PriceButton("1×", PetRules.PRICE_SINGLE, "buy_${care.name}_1") { buy(store.buy(care, 1, PetRules.PRICE_SINGLE), "1 ${care.title} gekauft!") }
                            if (pet.discount >= 1) PriceButton("5×", PetRules.PRICE_PACK5, "buy_${care.name}_5") { buy(store.buy(care, 5, PetRules.PRICE_PACK5), "5er-Pack ${care.plural} gekauft!") }
                            if (pet.discount >= 2) PriceButton("10×", PetRules.PRICE_PACK10, "buy_${care.name}_10") { buy(store.buy(care, 10, PetRules.PRICE_PACK10), "10er-Pack ${care.plural} gekauft!") }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Gap(10.dp)
                Text("Rabattmarken", style = MaterialTheme.typography.titleMedium)
                Gap(4.dp)
                DiscountRow(
                    "Silber-Marke", "5er-Packs: 5 Stück für ${PetRules.PRICE_PACK5} Taler",
                    PetRules.PRICE_SILVER, owned = pet.discount >= 1, available = pet.stage >= PetRules.SILVER_FROM_STAGE,
                    lockText = "Ab Stufe ${PetRules.SILVER_FROM_STAGE} (${PetRules.stageNames[PetRules.SILVER_FROM_STAGE]})", silver = true,
                ) { buy(store.buyDiscount(1, PetRules.PRICE_SILVER), "Silber-Marke gekauft – 5er-Packs sind da!") }
                DiscountRow(
                    "Gold-Marke", "10er-Packs: 10 Stück für ${PetRules.PRICE_PACK10} Taler",
                    PetRules.PRICE_GOLD, owned = pet.discount >= 2, available = pet.stage >= PetRules.GOLD_FROM_STAGE && pet.discount >= 1,
                    lockText = if (pet.discount < 1) "Erst Silber-Marke kaufen" else "Ab Stufe ${PetRules.GOLD_FROM_STAGE} (${PetRules.stageNames[PetRules.GOLD_FROM_STAGE]})", silver = false,
                ) { buy(store.buyDiscount(2, PetRules.PRICE_GOLD), "Gold-Marke gekauft – 10er-Packs sind da!") }
                Gap(8.dp)
                Text("Spielzeug, Deko und Kleidung kommen mit den nächsten Updates 🐾", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Gap(6.dp)
                OutlinedButton(onClick = onClose, modifier = Modifier.align(Alignment.End)) { Text("Schließen") }
            }
        }
    }
}

@Composable
private fun PriceButton(amount: String, price: Int, tag: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(vertical = 2.dp).testTag(tag)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(amount, style = MaterialTheme.typography.labelLarge); HGap(6.dp)
            TalerIcon(16.dp); HGap(2.dp)
            Text("$price", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DiscountRow(title: String, desc: String, price: Int, owned: Boolean, available: Boolean, lockText: String, silver: Boolean, onBuy: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
            Canvas(Modifier.padding(top = 2.dp).size(34.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                drawCircle(if (silver) Color(0xFFC9CED6) else Color(0xFFF5C23D), size.width * 0.44f, c)
                drawCircle(if (silver) Color(0xFF8C93A0) else Color(0xFFB7821A), size.width * 0.44f, c, style = Stroke(size.width * 0.07f))
                drawCircle(Color.White, size.width * 0.12f, Offset(c.x - size.width * 0.12f, c.y - size.width * 0.12f), alpha = 0.8f)
            }
            HGap(12.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Gap(8.dp)
                when {
                    owned -> Text("✓ Gekauft", style = MaterialTheme.typography.labelLarge, color = Color(0xFF3FA36B))
                    !available -> Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text("🔒 $lockText", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    else -> PriceButton("Kaufen", price, "buy_discount_${if (silver) 1 else 2}", onBuy)
                }
            }
        }
    }
}


// ======================================================================= needs

/** The four needs as little bars, plus the cat's mood. Updates every couple of seconds. */
@Composable
private fun NeedsRow(app: App, sim: PetSim) {
    val store = app.store
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(2000); tick++ } }
    @Suppress("UNUSED_EXPRESSION") tick
    val pet = store.pet   // re-read on changes (feeding, petting)
    val needs = store.needsNow()
    val mood = de.schnurrfeld.app.data.NeedRules.mood(needs)
    sim.mood = mood
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).testTag("needs"),
        shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            for (care in Care.entries) {
                val v = needs.of(care)
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    CareIcon(care, 22.dp)
                    HGap(3.dp)
                    LinearProgressIndicator(
                        progress = { v }, modifier = Modifier.weight(1f).height(7.dp),
                        color = when {
                            v >= de.schnurrfeld.app.data.NeedRules.HAPPY_FROM -> Color(0xFF3FA36B)
                            v >= de.schnurrfeld.app.data.NeedRules.SAD_BELOW -> Color(0xFFF2A93B)
                            else -> Color(0xFFE77F86)
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round, drawStopIndicator = {}, gapSize = 0.dp,
                    )
                    HGap(6.dp)
                }
            }
            Text(mood.emoji, style = MaterialTheme.typography.titleMedium)
        }
    }
    if (pet.stage < 0) Unit
}
