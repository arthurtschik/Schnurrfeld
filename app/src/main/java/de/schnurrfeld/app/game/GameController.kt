package de.schnurrfeld.app.game

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.schnurrfeld.app.audio.Haptic
import de.schnurrfeld.app.audio.Praise
import de.schnurrfeld.app.audio.Sfx
import de.schnurrfeld.app.audio.SoundManager
import de.schnurrfeld.app.audio.successSfx
import de.schnurrfeld.app.audio.plopSfx
import de.schnurrfeld.app.audio.VoiceManager
import de.schnurrfeld.app.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

object Cell {
    const val EMPTY = 0
    const val X = 1
    const val AUTO_X = 2
    const val CAT = 3
}

enum class Status { LOADING, PLAYING, WON, LOST }

/** Text shown under the combo bar. [points] > 0 means it liquefies and flows into the bar. */
data class Banner(val text: String, val points: Int, val tier: Int, val kind: de.schnurrfeld.app.audio.VoiceKind?, val id: Long)
/** Milk droplets flying into the bar: from the banner, or from the screen centre (PURRFECT). */
data class FlowEvent(val points: Int, val fromCenter: Boolean, val id: Long)
data class RewardEvent(val text: String, val id: Long)
/** what the little helper cat says */
data class HelperMsg(val text: String, val id: Long)

/** the helper cat only shows up in the first levels */
const val HELPER_UNTIL_LEVEL = 10
const val HELPER_IDLE_MS = 30_000L

const val MAX_LIVES = 3
const val TUTORIAL_SEED = 4242L
const val DOUBLE_TAP_MS = 300L
/** points needed to fill the combo bar */
const val BAR_MAX = 9
// fixed point rules for the combo bar
const val POINTS_STREAK3 = 1      // every 3 cats in a row without a mistake
const val POINTS_KOMBO = 1        // two cats directly after each other (no cross in between)
const val POINTS_QUICK = 2        // next cat within 20 s
const val POINTS_SUPER_FAST = 3   // next cat within 10 s
const val POINTS_PERFECT = 3      // round without losing a life
const val POINTS_DAILY = 1        // daily puzzle solved
const val QUICK_MS = 20_000L
const val SUPER_FAST_MS = 10_000L
/** time the words need to melt and flow into the bar before the points count */
const val FLOW_MS = 950L

class GameController(
    val mode: GameMode,
    private val store: Store,
    private val sound: SoundManager,
    private val voice: VoiceManager,
    private val levels: LevelBank,
    private val scope: CoroutineScope,
    /** 0 in tests: no animation delays */
    private val animate: Boolean = true,
) {
    var puzzle by mutableStateOf<Puzzle?>(null); private set
    val cells = mutableStateListOf<Int>()
    var lives by mutableIntStateOf(MAX_LIVES); private set
    var found by mutableIntStateOf(0); private set
    /** error-free cats in a row within the current puzzle (drives the spoken praise) */
    var streak by mutableIntStateOf(0); private set
    private var lastPraise: Praise? = null
    private var lastAnnounce: Praise? = null
    /** cats to stay quiet for before the next spoken praise */
    private var quietFor = 0
    /** the previous action was a correct cat -> the next correct cat is a real combo */
    private var lastActionWasCat = false
    private var lastCatAt = 0L
    private var playerCats = 0
    var elapsed by mutableIntStateOf(0)
    var status by mutableStateOf(Status.LOADING); private set
    val placedAt = mutableStateMapOf<Int, Long>()
    val errorAt = mutableStateMapOf<Int, Long>()
    /** the explained hint currently shown, with the time it appeared */
    var hint by mutableStateOf<Pair<Hint, Long>?>(null); private set
    /** time of the last bar gain – the liquid sloshes a bit after it */
    var barGainAt by mutableStateOf(0L); private set
    /** won without losing a life */
    var perfect by mutableStateOf(false); private set
    // ---- helper for beginners (levels 1–10)
    /** cells that wiggle to draw attention, with the time it started */
    var nudge by mutableStateOf<Pair<Set<Int>, Long>?>(null); private set
    var helper by mutableStateOf<HelperMsg?>(null); private set
    /** the Tipp button offers one free tip */
    var freeTip by mutableStateOf(false); private set
    private var lastInputAt = 0L
    private var nudgeAt = 0L
    private var nudgeFound = -1
    val helperEnabled get() = mode is GameMode.Level && mode.number <= HELPER_UNTIL_LEVEL

    /** Taler earned with this round (newly solved level / daily puzzle) */
    var talersEarned by mutableIntStateOf(0); private set
    var banner by mutableStateOf<Banner?>(null); private set
    var flow by mutableStateOf<FlowEvent?>(null); private set
    private var mistakes = 0
    private var eventSeq = 0L
    var reward by mutableStateOf<RewardEvent?>(null); private set
    var shake by mutableIntStateOf(0); private set
    /** Endless: puzzles solved in this run */
    var endlessSolved by mutableIntStateOf(0); private set
    var wonAt by mutableStateOf(0L); private set
    var catAnims: IntArray = IntArray(0); private set
    var catFurs: IntArray = IntArray(0); private set

    private var loadJob: Job? = null
    private var endlessSeed = Random.nextLong()
    private var prefetch: Pair<Int, Deferred<Puzzle>>? = null

    val title: String
        get() = when (mode) {
            is GameMode.Level -> "Level ${mode.number}"
            is GameMode.Daily -> "Tagesrätsel"
            GameMode.Endless -> "Endlos · Rätsel ${endlessSolved + 1}"
            GameMode.Tutorial -> "Tutorial"
        }

    /**
     * Starts loading the puzzle. Called from a LaunchedEffect, i.e. after the screen's composition is applied –
     * writing state from a coroutine started *during* composition can get lost.
     */
    fun start() {
        if (loadJob == null) load()
    }

    private fun now() = SystemClock.uptimeMillis()
    private val settings get() = store.settings

    private fun load() {
        status = Status.LOADING
        loadJob?.cancel()
        loadJob = scope.launch {
            val p = withContext(Dispatchers.Default) {
                when (mode) {
                    is GameMode.Level -> levels.level(mode.number)
                    is GameMode.Daily -> PuzzleGenerator.generate(Daily.size(mode.date), Daily.seed(mode.date), 0.6f, maxRating = 3)
                    GameMode.Tutorial -> PuzzleGenerator.generate(4, TUTORIAL_SEED, 0f, maxRating = 1)
                    GameMode.Endless -> {
                        val pre = prefetch?.takeIf { it.first == endlessSolved }?.second
                        pre?.await() ?: endlessPuzzle(endlessSolved)
                    }
                }
            }
            setup(p)
            if (mode == GameMode.Endless) {
                // generate the next puzzle in the background while this one is played
                val next = endlessSolved + 1
                prefetch = next to scope.async(Dispatchers.Default) { endlessPuzzle(next) }
            }
        }
    }

    private fun endlessPuzzle(index: Int) =
        PuzzleGenerator.generate(
            Endless.size(index), endlessSeed + index * 31L, Endless.difficulty(index), maxRating = if (index < 3) 2 else 4
        )

    private fun setup(p: Puzzle) {
        puzzle = p
        cells.clear()
        cells.addAll(List(p.size * p.size) { Cell.EMPTY })
        found = 0
        elapsed = 0
        placedAt.clear(); errorAt.clear()
        streak = 0; lastTapCell = -1
        hint = null; perfect = false; talersEarned = 0
        nudge = null; helper = null; freeTip = false; lastInputAt = now(); nudgeAt = 0L; nudgeFound = -1
        lastActionWasCat = false; lastCatAt = 0L; playerCats = 0
        lastAnnounce = null; quietFor = 0
        banner = null; flow = null
        val rnd = Random(p.regions.sum() * 31 + p.size)
        catAnims = IntArray(p.size * p.size) { rnd.nextInt(7) }
        catFurs = IntArray(p.size * p.size) { rnd.nextInt(7) }
        status = Status.PLAYING
    }

    fun restart() {
        lives = MAX_LIVES
        if (mode == GameMode.Endless) {
            endlessSolved = 0; endlessSeed = Random.nextLong()
            prefetch?.second?.cancel(); prefetch = null
        }
        load()
    }

    fun nextEndless() {
        if (mode == GameMode.Endless) load()
    }

    // ---------- input ----------

    private var lastTapCell = -1
    private var lastTapAt = 0L
    private var chainBeforeTap = false

    /**
     * One tap toggles a cross (always free). A quick double tap places a cat;
     * only a wrong cat costs a life.
     */
    private fun activity() { lastInputAt = now(); nudge = null }

    fun tap(cell: Int) {
        if (status != Status.PLAYING) return
        activity()
        val t = now()
        if (cell == lastTapCell && t - lastTapAt <= DOUBLE_TAP_MS && cells[cell] != Cell.CAT) {
            lastTapCell = -1
            // the first tap of a double tap doesn't count as "something in between"
            lastActionWasCat = chainBeforeTap
            tryCat(cell)
            return
        }
        lastTapCell = cell
        lastTapAt = t
        chainBeforeTap = lastActionWasCat
        if (cells[cell] != Cell.CAT) lastActionWasCat = false
        when (cells[cell]) {
            Cell.EMPTY -> {
                cells[cell] = Cell.X
                sfx(plopSfx(), 0.9f, 0.96f + Random.nextFloat() * 0.08f)
                haptic(Haptic.TICK)
            }
            Cell.X, Cell.AUTO_X -> {
                cells[cell] = Cell.EMPTY
                sfx(Sfx.ERASE, 0.8f, 0.95f + Random.nextFloat() * 0.1f)
            }
            else -> {}
        }
    }

    fun longPress(cell: Int) {
        if (status != Status.PLAYING) return
        activity()
        if (cells[cell] == Cell.X || cells[cell] == Cell.AUTO_X) {
            cells[cell] = Cell.EMPTY
            lastActionWasCat = false
            sfx(Sfx.ERASE, 0.8f)
        }
    }

    private var paintMode = 0 // 1 = set X, -1 = erase X
    fun dragStart(cell: Int) {
        if (status != Status.PLAYING) { paintMode = 0; return }
        activity()
        paintMode = when (cells[cell]) {
            Cell.EMPTY -> 1
            Cell.X, Cell.AUTO_X -> -1
            else -> 0
        }
        dragOver(cell)
    }

    fun dragOver(cell: Int) {
        if (status != Status.PLAYING) return
        if (paintMode == 1 && cells[cell] == Cell.EMPTY) {
            lastActionWasCat = false
            cells[cell] = Cell.X
            sfx(plopSfx(), 0.6f, 1.04f + Random.nextFloat() * 0.08f)
            haptic(Haptic.TICK)
        } else if (paintMode == -1 && (cells[cell] == Cell.X || cells[cell] == Cell.AUTO_X)) {
            lastActionWasCat = false
            cells[cell] = Cell.EMPTY
            sfx(Sfx.ERASE, 0.55f, 1.05f + Random.nextFloat() * 0.1f)
        }
    }

    private fun tryCat(cell: Int) {
        val p = puzzle ?: return
        if (!p.isSolution(cell)) { mistake(cell); return }

        val t = now()
        val direct = lastActionWasCat
        val sinceLast = if (lastCatAt > 0) t - lastCatAt else Long.MAX_VALUE
        lastActionWasCat = true
        lastCatAt = t
        playerCats++
        streak++
        if (mode != GameMode.Tutorial) store.updateProgress {
            val combo = it.combo + 1
            it.copy(combo = combo, bestCombo = maxOf(it.bestCombo, combo), totalCats = it.totalCats + 1)
        }
        placeCat(cell)

        // fixed point rules: the best combo type counts, plus 1 for every 3rd cat in a row
        val combo: Praise? = when {
            playerCats <= 1 -> null
            sinceLast < SUPER_FAST_MS -> Praise.SUPER_FAST
            sinceLast < QUICK_MS -> Praise.QUICK_KOMBO
            direct -> Praise.KOMBO
            else -> null
        }
        val comboPoints = when (combo) {
            Praise.SUPER_FAST -> POINTS_SUPER_FAST
            Praise.QUICK_KOMBO -> POINTS_QUICK
            Praise.KOMBO -> POINTS_KOMBO
            else -> 0
        }
        val streakPoints = if (streak % 3 == 0) POINTS_STREAK3 else 0
        val points = comboPoints + streakPoints

        if (status == Status.WON) {               // last cat: the finale takes the stage
            if (points > 0) addBarPoints(points)
            return
        }
        sfx(successSfx(Praise.tierFor(streak, p.size, 4)), 0.85f)
        haptic(Haptic.SUCCESS)

        when {
            mode == GameMode.Tutorial -> {}        // the tutorial stays quiet: no combo calls, no praise
            combo != null -> {                     // combo calls are ALWAYS announced
                say(combo)
                showBanner(combo.text, points, combo.tier, combo.kind)
                quietFor = Random.nextInt(1, 3)
            }
            playerCats > 1 && quietFor <= 0 -> {   // regular praise, not on every cat, never on the first
                val pr = Praise.pick(Praise.tierFor(streak, p.size), lastPraise)
                lastPraise = pr
                quietFor = Random.nextInt(1, 3)
                say(pr)
                showBanner(pr.text, points, pr.tier, pr.kind)
            }
            else -> {
                if (playerCats > 1) quietFor--
                if (points > 0) showBanner("3 in Folge!", points, 1, null)
            }
        }
        if (points > 0) grant(points, fromCenter = false)
    }

    private fun say(word: Praise, delayMs: Long = 120) {
        if (!settings.voice) return
        if (!animate) { voice.speak(word, settings.ttsFallback); return }
        scope.launch { delay(delayMs); voice.speak(word, settings.ttsFallback) }
    }

    private fun showBanner(text: String, points: Int, tier: Int, kind: de.schnurrfeld.app.audio.VoiceKind?) {
        banner = Banner(text, points, tier, kind, now() * 16 + (eventSeq++ % 16))
    }

    /** Points flow into the bar as milk; they count once they arrive (unless a mistake happened meanwhile). */
    private fun grant(points: Int, fromCenter: Boolean) {
        if (!animate) { addBarPoints(points); return }
        flow = FlowEvent(points, fromCenter, now() * 16 + (eventSeq++ % 16))
        val m = mistakes
        scope.launch { delay(FLOW_MS); if (m == mistakes) addBarPoints(points) }
    }

    private fun later(ms: Long, block: () -> Unit) {
        if (!animate) block() else scope.launch { delay(ms); block() }
    }

    private fun addBarPoints(points: Int) {
        if (points <= 0 || mode == GameMode.Tutorial) return
        barGainAt = now()
        var prizeText: String? = null
        store.updateProgress {
            var bar = it.bar + points
            var prizes = it.prizes
            var hints = it.hints; var jokers = it.jokers
            val won = mutableListOf<String>()
            while (bar >= BAR_MAX) {
                bar -= BAR_MAX
                if (prizes % 2 == 0) { hints++; won += "+1 Tipp" }
                else { jokers++; won += "+1 Joker" }
                prizes++
            }
            if (won.isNotEmpty()) prizeText = "Milchkännchen voll: " + won.joinToString(" · ") + "!"
            it.copy(bar = bar, prizes = prizes, hints = hints, jokers = jokers)
        }
        val level = store.progress.bar
        sfx(Sfx.FILL, 0.75f, 0.9f + level * 0.04f)
        prizeText?.let { txt ->
            reward = RewardEvent(txt, now())
            scope.launch { delay(280); sfx(Sfx.PRIZE, 0.9f); haptic(Haptic.SUCCESS) }
        }
    }

    private fun placeCat(cell: Int) {
        val p = puzzle ?: return
        nudgeAt = 0L; nudgeFound = -1
        cells[cell] = Cell.CAT
        placedAt[cell] = now()
        found++
        if (hint?.first?.catCell == cell) hint = null
        if (settings.autoX) autoCross(p, cell)
        if (found == p.size) win()
    }

    private fun autoCross(p: Puzzle, cell: Int) {
        val n = p.size
        val r = cell / n; val c = cell % n; val g = p.regions[cell]
        for (i in 0 until n * n) {
            if (cells[i] != Cell.EMPTY) continue
            val rr = i / n; val cc = i % n
            val adjacent = kotlin.math.abs(rr - r) <= 1 && kotlin.math.abs(cc - c) <= 1
            if (rr == r || cc == c || p.regions[i] == g || adjacent) cells[i] = Cell.AUTO_X
        }
    }

    private fun mistake(cell: Int) {
        cells[cell] = Cell.X
        errorAt[cell] = now()
        if (mode != GameMode.Tutorial) lives--
        mistakes++
        streak = 0
        lastActionWasCat = false
        shake++
        // a mistake empties the Milchkännchen
        if (mode != GameMode.Tutorial) store.updateProgress { it.copy(combo = 0, bar = 0) }
        sfx(Sfx.ERROR)
        haptic(Haptic.ERROR)
        if (lives <= 0) {
            status = Status.LOST
            if (mode == GameMode.Endless) {
                store.updateProgress { it.copy(endlessBest = maxOf(it.endlessBest, endlessSolved)) }
            }
        }
    }

    private fun win() {
        status = Status.WON
        wonAt = now()
        hint = null
        perfect = lives == MAX_LIVES
        sfx(Sfx.WIN)
        haptic(Haptic.SUCCESS)
        if (perfect) {
            later(650) { sfx(Sfx.SLAM, 0.9f); haptic(Haptic.ERROR) }
            say(Praise.PERFECT, delayMs = 900)
            later(2300) { grant(POINTS_PERFECT, fromCenter = true) }   // the big PURRFECT melts into the bar
        } else {
            val finish = Praise.entries.filter { it.kind == de.schnurrfeld.app.audio.VoiceKind.FINISH }
            val pool = finish.filter { voice.isAvailable(it) }.ifEmpty { finish }
            val word = pool.random()
            say(word, delayMs = 450)
            showBanner(word.text, 0, word.tier, word.kind)
        }
        if (mode is GameMode.Daily) {
            later(if (perfect) 3400 else 2300) {
                showBanner("Tagesrätsel!", POINTS_DAILY, 2, null)
                grant(POINTS_DAILY, fromCenter = false)
            }
        }
        when (mode) {
            is GameMode.Level -> {
                // +1 Taler only the first time a level is solved
                if (mode.number !in store.progress.stars) { talersEarned = de.schnurrfeld.app.data.PetRules.TALER_PER_NEW_LEVEL; store.addTalers(talersEarned) }
                store.updateProgress {
                val n = mode.number
                val stars = maxOf(it.stars[n] ?: 0, lives)
                val best = it.bestTimes[n]?.let { b -> minOf(b, elapsed) } ?: elapsed
                it.copy(
                    unlocked = maxOf(it.unlocked, (n + 1).coerceAtMost(Levels.count)),
                    stars = it.stars + (n to stars),
                    bestTimes = it.bestTimes + (n to best),
                )
                }
            }
            is GameMode.Daily -> {
                if (mode.date.toString() !in store.progress.dailySolved) { talersEarned = de.schnurrfeld.app.data.PetRules.TALER_PER_DAILY; store.addTalers(talersEarned) }
                store.updateProgress { it.copy(dailySolved = it.dailySolved + mode.date.toString()) }
            }
            GameMode.Tutorial -> {}
            GameMode.Endless -> {
                endlessSolved++
                store.updateProgress { it.copy(endlessBest = maxOf(it.endlessBest, endlessSolved)) }
            }
        }
    }

    // ---------- helpers ----------

    fun useHint(): Boolean {
        val p = puzzle ?: return false
        if (status != Status.PLAYING || store.progress.hints <= 0 || hint != null) return false
        activity()
        hint = HintEngine.find(p, cells.toList()) to now()
        store.updateProgress { it.copy(hints = it.hints - 1) }
        sfx(Sfx.JOKER, 0.6f, 1.2f)
        haptic(Haptic.CLICK)
        return true
    }

    /** Applies what the current hint suggests (crosses / removing a wrong cross) and closes it. */
    fun applyHint() {
        val h = hint?.first ?: return
        activity()
        lastActionWasCat = false
        for (c in h.crosses) if (cells[c] == Cell.EMPTY) cells[c] = Cell.X
        h.removeCross?.let { if (cells[it] == Cell.X || cells[it] == Cell.AUTO_X) cells[it] = Cell.EMPTY }
        if (h.crosses.isNotEmpty()) sfx(Sfx.PLOP, 0.8f)
        if (h.removeCross != null) sfx(Sfx.ERASE, 0.8f)
        hint = if (h.catCell != null && h.removeCross != null) h.copy(removeCross = null, crosses = emptySet()) to now() else null
    }

    fun closeHint() { hint = null }

    /**
     * Beginner help, called about once a second: after 30 s without any input a cell wiggles and the helper
     * cat points at it; if there is still no new cat 30 s later, the Tipp button offers a free tip.
     */
    fun checkIdle() {
        val p = puzzle ?: return
        if (!helperEnabled || status != Status.PLAYING || hint != null || freeTip) return
        val t = now()
        if (nudgeAt == 0L && t - lastInputAt > HELPER_IDLE_MS) {
            val h = HintEngine.find(p, cells.toList())
            val (target, text) = when {
                h.removeCross != null -> setOf(h.removeCross) to "Hm … stimmt dieses ✕ wirklich?"
                h.catCell != null -> setOf(h.catCell) to "Psst … schau dir dieses Feld mal genauer an!"
                h.crosses.isNotEmpty() -> setOf(h.crosses.first()) to "Psst … kann hier wirklich eine Katze hin?"
                else -> h.focus to "Psst … schau mal hier hin!"
            }
            nudge = target to t
            nudgeAt = t; nudgeFound = found
            helper = HelperMsg(text, t)
            sfx(Sfx.TAP, 0.7f)
        } else if (nudgeAt > 0L && t - nudgeAt > HELPER_IDLE_MS && found == nudgeFound) {
            freeTip = true
            helper = HelperMsg("Immer noch knifflig? Ein Tipp geht aufs Haus!", t)
            sfx(Sfx.TAP, 0.7f)
        }
    }

    /** The free tip offered by the helper – doesn't use up a tip. */
    fun useFreeTip() {
        if (!freeTip) return
        freeTip = false; helper = null; nudgeAt = 0L
        activity()
        freeHint()
    }

    /** Tutorial: show an explanation overlay on the board without using up a tip. */
    fun showHint(h: Hint?) { hint = h?.let { it to now() } }

    /** Tutorial: a free, explained tip. */
    fun freeHint() { puzzle?.let { hint = HintEngine.find(it, cells.toList()) to now() } }

    fun useJoker(): Boolean {
        val p = puzzle ?: return false
        if (status != Status.PLAYING || store.progress.jokers <= 0) return false
        val n = p.size
        val candidates = (0 until n).map { r -> r * n + p.solution[r] }.filter { cells[it] != Cell.CAT }
        val cell = candidates.randomOrNull() ?: return false
        store.updateProgress { it.copy(jokers = it.jokers - 1, totalCats = it.totalCats + 1) }
        lastActionWasCat = false
        sfx(Sfx.JOKER)
        haptic(Haptic.SUCCESS)
        placeCat(cell)
        return true
    }

    private fun sfx(s: Sfx, volume: Float = 1f, rate: Float = 1f) {
        if (settings.sound) sound.play(s, volume, rate)
    }

    private fun haptic(h: Haptic) {
        if (settings.vibration) sound.vibrate(h)
    }
}
