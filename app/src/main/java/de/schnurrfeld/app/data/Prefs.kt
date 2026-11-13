package de.schnurrfeld.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val sound: Boolean = true,
    val voice: Boolean = true,
    val ttsFallback: Boolean = true,
    val vibration: Boolean = true,
    val showTimer: Boolean = true,
    val autoX: Boolean = false,
    val colorAssist: Boolean = false,
    /** the interactive tutorial was finished or skipped */
    val tutorialDone: Boolean = false,
    /** look for a new version on GitHub when the app starts */
    val autoUpdate: Boolean = true,
)

data class Progress(
    val unlocked: Int = 1,
    val stars: Map<Int, Int> = emptyMap(),
    val bestTimes: Map<Int, Int> = emptyMap(),
    val totalCats: Int = 0,
    val combo: Int = 0,
    val bestCombo: Int = 0,
    val hints: Int = 3,
    val jokers: Int = 1,
    val endlessBest: Int = 0,
    /** combo bar points (0 until BAR_MAX) and number of prizes won so far */
    val bar: Int = 0,
    val prizes: Int = 0,
    val dailySolved: Set<String> = emptySet(),
) {
    fun dailyStreak(today: LocalDate): Int {
        var d = if (today.toString() in dailySolved) today else today.minusDays(1)
        var n = 0
        while (d.toString() in dailySolved) { n++; d = d.minusDays(1) }
        return n
    }
}

/** Small persistent store backed by SharedPreferences, observable from Compose. */
class Store(context: Context) {
    private val sp = context.getSharedPreferences("schnurrfeld", Context.MODE_PRIVATE)

    var settings by mutableStateOf(loadSettings())
        private set
    var progress by mutableStateOf(loadProgress())
        private set
    var pet by mutableStateOf(loadPet())
        private set

    init {
        // Taler income was introduced later: pay out the start capital + 1 Taler per level already solved, once
        if (!sp.getBoolean("taler_v2", false)) {
            val back = if (sp.contains("pet_talers") || levelReached > 0) PetRules.START_TALERS + levelReached * PetRules.TALER_PER_NEW_LEVEL else 0
            if (back > 0) updatePet { it.copy(talers = it.talers + back) }
            sp.edit().putBoolean("taler_v2", true).apply()
        }
    }

    fun addTalers(n: Int) { if (n > 0) updatePet { it.copy(talers = it.talers + n) } }

    /** +1 Taler for the first opening of the app each day. Returns true if it was paid out. */
    /** the day the daily Taler was last collected */
    var dailyClaimedDay by mutableStateOf(sp.getString("login_day", "").orEmpty())
        private set

    fun dailyTalerWaiting(today: java.time.LocalDate) = dailyClaimedDay != today.toString()

    /**
     * Collects the daily Taler (once per day). A happy cat adds a bonus Taler.
     * Returns (base, bonus), or null if already collected today.
     */
    fun claimDaily(today: java.time.LocalDate, nowMs: Long = System.currentTimeMillis()): Pair<Int, Int>? {
        if (!dailyTalerWaiting(today)) return null
        dailyClaimedDay = today.toString()
        sp.edit().putString("login_day", dailyClaimedDay).apply()
        val bonus = if (pet.stage >= PetRules.HATCH_STAGE && NeedRules.mood(needsNow(nowMs)) == Mood.HAPPY) NeedRules.HAPPY_BONUS_TALER else 0
        addTalers(PetRules.TALER_PER_LOGIN + bonus)
        return PetRules.TALER_PER_LOGIN to bonus
    }

    /** dev: let the daily Taler be collected again */
    fun devResetDaily() { dailyClaimedDay = ""; sp.edit().putString("login_day", "").apply() }

    /** dev: pretend [hours] have passed for the needs */
    fun devAgeNeeds(hours: Int) {
        val now = System.currentTimeMillis()
        val n = needsNow(now)
        updatePet { it.copy(needs = n, needsAt = now - hours * 3_600_000L) }
    }

    // ---------- needs ----------

    /** the needs right now (they sink in real time, also while the app is closed) */
    fun needsNow(nowMs: Long = System.currentTimeMillis()): Needs =
        if (pet.needsAt == 0L) pet.needs else NeedRules.decayed(pet.needs, pet.needsAt, nowMs)

    fun moodNow(nowMs: Long = System.currentTimeMillis()): Mood? =
        if (pet.stage < PetRules.HATCH_STAGE) null else NeedRules.mood(needsNow(nowMs))

    private fun setNeeds(n: Needs, nowMs: Long) = updatePet { it.copy(needs = n, needsAt = nowMs) }

    /** one petting stroke fills the cuddle need a little – always free */
    fun petStroke(nowMs: Long = System.currentTimeMillis()) {
        if (pet.stage < PetRules.HATCH_STAGE) return
        setNeeds(needsNow(nowMs).plus(Care.BRUSH, NeedRules.PET_FILL), nowMs)
    }

    /** remembers the visit; returns how long ago the last one was (ms), 0 for the first */
    fun visitBasket(nowMs: Long = System.currentTimeMillis()): Long {
        val last = pet.lastVisit
        updatePet { it.copy(lastVisit = nowMs) }
        return if (last == 0L) 0L else nowMs - last
    }

    /** highest Schnurrfeld level solved so far */
    val levelReached get() = (progress.unlocked - 1).coerceAtLeast(0)

    fun updatePet(f: (Pet) -> Pet) {
        pet = f(pet)
        val p = pet
        sp.edit()
            .putBoolean("pet_intro", p.introSeen)
            .putInt("pet_talers", p.talers)
            .putInt("pet_discount", p.discount)
            .putString("pet_stock", p.stock.entries.joinToString(",") { "${it.key.code}:${it.value}" })
            .putString("pet_log", p.log)
            .putInt("pet_stage", p.stage)
            .putInt("pet_seen", p.seenStage)
            .putString("pet_breed", p.breedOverride?.name ?: "")
            .putString("pet_needs", p.needs.encode())
            .putLong("pet_needs_at", p.needsAt)
            .putLong("pet_last_visit", p.lastVisit)
            .putBoolean("pet_tree_seen", p.treeSeen)
            .apply()
    }

    /** Old saves used 9 stages (with "Pfötchen" and "Köpfchen"); map them to the new 7-stage scheme. */
    private fun migrateStage(old: Int) = intArrayOf(0, 1, 1, 1, 2, 3, 4, 5, 6, 7).getOrElse(old) { 6 }

    private fun loadPet(): Pet {
        val scheme = sp.getInt("pet_scheme", 1)
        val p = rawPet()
        if (scheme >= 2) return p
        val migrated = p.copy(stage = migrateStage(p.stage).coerceAtMost(PetRules.MAX_STAGE), seenStage = migrateStage(p.seenStage).coerceAtMost(PetRules.MAX_STAGE))
        sp.edit().putInt("pet_scheme", 2).putInt("pet_stage", migrated.stage).putInt("pet_seen", migrated.seenStage).apply()
        return migrated
    }

    private fun rawPet() = Pet(
        introSeen = sp.getBoolean("pet_intro", false),
        talers = sp.getInt("pet_talers", PetRules.START_TALERS),
        discount = sp.getInt("pet_discount", 0),
        stock = sp.getString("pet_stock", "").orEmpty().split(",").mapNotNull {
            val parts = it.split(":")
            if (parts.size == 2 && parts[0].length == 1) runCatching { Care.of(parts[0][0]) to parts[1].toInt() }.getOrNull() else null
        }.toMap(),
        log = sp.getString("pet_log", "").orEmpty().filter { c -> Care.entries.any { it.code == c } },
        stage = sp.getInt("pet_stage", 0),
        seenStage = sp.getInt("pet_seen", 0),
        breedOverride = sp.getString("pet_breed", "").orEmpty().let { n -> Breed.entries.firstOrNull { it.name == n } },
        needs = Needs.decode(sp.getString("pet_needs", null)) ?: Needs(),
        needsAt = sp.getLong("pet_needs_at", 0L),
        lastVisit = sp.getLong("pet_last_visit", 0L),
        treeSeen = sp.getBoolean("pet_tree_seen", false),
    )

    // ---------- Katzenkörbchen actions ----------

    /** the cat could grow right now (shown as a wobbling menu entry) */
    val petEvolutionPending get() = PetRules.reachable(pet.given, levelReached) > pet.stage

    /** Grows the cat by one stage if care and level allow it. Returns true if it grew. */
    fun petCheckEvolution(): Boolean {
        if (!petEvolutionPending) return false
        updatePet { it.copy(stage = it.stage + 1) }
        // the needs start when the kitten jumps out of the basket – all happy to begin with
        if (pet.stage == PetRules.HATCH_STAGE) setNeeds(Needs(), System.currentTimeMillis())
        return true
    }

    fun exchangeHints(): Boolean {
        if (progress.hints < PetRules.HINTS_PER_TALER) return false
        updateProgress { it.copy(hints = it.hints - PetRules.HINTS_PER_TALER) }
        updatePet { it.copy(talers = it.talers + 1) }
        return true
    }

    fun exchangeJokers(): Boolean {
        if (progress.jokers < PetRules.JOKERS_PER_TALER) return false
        updateProgress { it.copy(jokers = it.jokers - PetRules.JOKERS_PER_TALER) }
        updatePet { it.copy(talers = it.talers + 1) }
        return true
    }

    /** buys [amount] of [care] for [price] Taler */
    fun buy(care: Care, amount: Int, price: Int): Boolean {
        if (pet.talers < price) return false
        updatePet { it.copy(talers = it.talers - price, stock = it.stock + (care to it.stockOf(care) + amount)) }
        return true
    }

    fun buyDiscount(level: Int, price: Int): Boolean {
        if (pet.talers < price || pet.discount >= level) return false
        updatePet { it.copy(talers = it.talers - price, discount = level) }
        return true
    }

    /** gives one item from the stock to the cat */
    fun give(care: Care): Boolean {
        if (pet.stockOf(care) <= 0) return false
        updatePet { it.copy(stock = it.stock + (care to it.stockOf(care) - 1), log = it.log + care.code) }
        if (pet.stage >= PetRules.HATCH_STAGE) {
            val now = System.currentTimeMillis()
            setNeeds(needsNow(now).plus(care, NeedRules.ITEM_FILL), now)
        }
        return true
    }

    fun resetPet() = updatePet { Pet() }

    fun updateSettings(f: (Settings) -> Settings) {
        settings = f(settings)
        val s = settings
        sp.edit()
            .putString("theme", s.theme.name)
            .putBoolean("sound", s.sound)
            .putBoolean("voice", s.voice)
            .putBoolean("tts3", s.ttsFallback)
            .putBoolean("vibration", s.vibration)
            .putBoolean("timer", s.showTimer)
            .putBoolean("autoX", s.autoX)
            .putBoolean("colorAssist", s.colorAssist)
            .putBoolean("tutorialDone", s.tutorialDone)
            .putBoolean("autoUpdate", s.autoUpdate)
            .apply()
    }

    /** the running game (see GameSnapshot), null when nothing is to be resumed */
    val savedGame: String? get() = sp.getString("saved_game", null)
    fun saveGame(line: String?) { sp.edit().apply { if (line == null) remove("saved_game") else putString("saved_game", line) }.apply() }

    fun updateProgress(f: (Progress) -> Progress) {
        progress = f(progress)
        val p = progress
        sp.edit()
            .putInt("unlocked", p.unlocked)
            .putString("stars", p.stars.entries.joinToString(",") { "${it.key}:${it.value}" })
            .putString("times", p.bestTimes.entries.joinToString(",") { "${it.key}:${it.value}" })
            .putInt("totalCats", p.totalCats)
            .putInt("combo", p.combo)
            .putInt("bestCombo", p.bestCombo)
            .putInt("hints", p.hints)
            .putInt("jokers", p.jokers)
            .putInt("endlessBest", p.endlessBest)
            .putInt("bar", p.bar)
            .putInt("prizes", p.prizes)
            .putStringSet("daily", p.dailySolved)
            .apply()
    }

    fun resetProgress() {
        updateProgress { Progress() }
    }

    private fun loadSettings() = Settings(
        theme = runCatching { ThemeMode.valueOf(sp.getString("theme", "SYSTEM")!!) }.getOrDefault(ThemeMode.SYSTEM),
        sound = sp.getBoolean("sound", true),
        voice = sp.getBoolean("voice", true),
        ttsFallback = sp.getBoolean("tts3", true),
        vibration = sp.getBoolean("vibration", true),
        showTimer = sp.getBoolean("timer", true),
        autoX = sp.getBoolean("autoX", false),
        colorAssist = sp.getBoolean("colorAssist", false),
        tutorialDone = sp.getBoolean("tutorialDone", false),
        autoUpdate = sp.getBoolean("autoUpdate", true),
    )

    private fun parseMap(s: String?): Map<Int, Int> =
        s.orEmpty().split(",").mapNotNull {
            val p = it.split(":")
            if (p.size == 2) p[0].toIntOrNull()?.let { k -> p[1].toIntOrNull()?.let { v -> k to v } } else null
        }.toMap()

    private fun loadProgress() = Progress(
        unlocked = sp.getInt("unlocked", 1),
        stars = parseMap(sp.getString("stars", "")),
        bestTimes = parseMap(sp.getString("times", "")),
        totalCats = sp.getInt("totalCats", 0),
        combo = sp.getInt("combo", 0),
        bestCombo = sp.getInt("bestCombo", 0),
        hints = sp.getInt("hints", 3),
        jokers = sp.getInt("jokers", 1),
        endlessBest = sp.getInt("endlessBest", 0),
        bar = sp.getInt("bar", 0),
        prizes = sp.getInt("prizes", 0),
        dailySolved = sp.getStringSet("daily", emptySet())!!.toSet(),
    )
}
