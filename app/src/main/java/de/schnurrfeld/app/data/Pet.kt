package de.schnurrfeld.app.data

/** The four care items. [code] is used in the compact "given" log. */
enum class Care(val code: Char, val title: String, val plural: String) {
    FOOD('F', "Leckerli", "Leckerlis"),
    MILK('M', "Katzenmilch", "Katzenmilch"),
    BRUSH('B', "Streichelbürste", "Bürstenstriche"),
    YARN('Y', "Wollknäuel", "Wollknäuel");

    companion object {
        fun of(c: Char) = entries.first { it.code == c }
    }
}

enum class TailColor { RED, BLACK, WHITE }

/** The six cats that can hatch. Which one depends on the care items (see [PetRules]). */
enum class Breed(val title: String, val blurb: String, val tail: TailColor) {
    MOPPEL("Moppel", "Rote Tigerkatze mit Bäuchlein", TailColor.RED),
    KIRSCHE("Kirsche", "Weiß, mit rotem Schwanz und roten Ohren", TailColor.RED),
    MITTERNACHT("Mitternacht", "Pechschwarz mit goldenen Augen", TailColor.BLACK),
    DOMINO("Domino", "Weiß, mit schwarzem Schwanz und Augenfleck", TailColor.BLACK),
    KARAMELL("Karamell", "Karamellfarben mit weißem Schwanz", TailColor.WHITE),
    WOLKE("Wolke", "Ganz weiß und extra flauschig", TailColor.WHITE),
}

/** Everything about the Katzenkörbchen that is saved. */
data class Pet(
    val introSeen: Boolean = false,
    val talers: Int = PetRules.START_TALERS,
    /** 0 = single items only, 1 = Silber-Rabattmarke (5er-Packs), 2 = Gold-Rabattmarke (10er-Packs) */
    val discount: Int = 0,
    val stock: Map<Care, Int> = emptyMap(),
    /** every item ever given, in order (codes of [Care]) – decides tail colour and breed */
    val log: String = "",
    /** current, visible stage */
    val stage: Int = 0,
    /** last stage the player has seen in the Katzenkörbchen (to play reveal animations once) */
    val seenStage: Int = 0,
    /** dev builds can force a breed */
    val breedOverride: Breed? = null,
    /** needs as stored at [needsAt] (epoch ms); they sink in real time, also while the app is closed */
    val needs: Needs = Needs(),
    val needsAt: Long = 0L,
    /** last time the Katzenkörbchen was opened (epoch ms) – for the "missed you" reunion */
    val lastVisit: Long = 0L,
    /** the player has been told about the scratching post (so older cats get a one-time notice) */
    val treeSeen: Boolean = false,
) {
    val given get() = log.length
    fun stockOf(c: Care) = stock[c] ?: 0
    val tail: TailColor? get() = if (stage >= 1) breedOverride?.tail ?: PetRules.tailFor(log) else null
    val breed: Breed? get() = if (stage >= PetRules.HATCH_STAGE) breedOverride ?: PetRules.breedFor(log) else null
}

/** All numbers of the Katzenkörbchen in one place. */
object PetRules {
    /** stage names for the UI */
    val stageNames = listOf(
        "Ein geheimnisvolles Bündel",
        "Da wackelt etwas …",
        "Hallo, Kätzchen!",
        "Babykatze",
        "Jungkatze",
        "Teenie-Katze",
        "Ausgewachsen",
        "Prachtkatze im Palast",
    )
    /** items given in total to reach a stage */
    val itemsFor = intArrayOf(0, 8, 18, 28, 42, 60, 85, 115)
    /** Schnurrfeld level needed for a stage */
    val levelFor = intArrayOf(0, 5, 15, 25, 35, 50, 75, 100)

    /** stage at which the cat jumps out of the basket – the breed is fixed then */
    const val HATCH_STAGE = 2
    /** stage from which there is the bigger basket */
    const val BIG_BASKET_STAGE = 4
    /** stage from which the scratching post stands in the room */
    const val KRATZ_STAGE = 5
    /** stage at which the cat is fully grown */
    const val ADULT_STAGE = 6
    /** highest stage available in this version (the palace follows with 1.4) */
    const val MAX_STAGE = 6
    /** level from which a second cat can be discovered */
    const val SECOND_CAT_LEVEL = 150

    // shop
    const val PRICE_SINGLE = 1
    const val PRICE_SILVER = 4        // Silber-Rabattmarke, unlocks 5er-Packs
    const val PRICE_PACK5 = 3
    const val SILVER_FROM_STAGE = 1
    const val PRICE_GOLD = 10         // Gold-Rabattmarke, unlocks 10er-Packs
    const val PRICE_PACK10 = 5
    const val GOLD_FROM_STAGE = 3

    // Taler income
    const val START_TALERS = 5
    const val TALER_PER_NEW_LEVEL = 1
    const val TALER_PER_DAILY = 1
    const val TALER_PER_LOGIN = 1

    // Taler-Automat
    const val HINTS_PER_TALER = 10
    const val JOKERS_PER_TALER = 5

    /** The stage the cat *could* be at with the given care and level (capped to what exists). */
    fun reachable(given: Int, level: Int): Int {
        var s = 0
        for (i in 1 until itemsFor.size) if (given >= itemsFor[i] && level >= levelFor[i]) s = i else break
        return s.coerceAtMost(MAX_STAGE)
    }

    /** the item given most often in [part]; ties go to the one given last */
    private fun dominant(part: String, among: Set<Care> = Care.entries.toSet()): Care? {
        if (part.isEmpty()) return null
        val counts = among.associateWith { c -> part.count { it == c.code } }
        val max = counts.values.max()
        if (max == 0) return null
        val tied = counts.filter { it.value == max }.keys
        return part.reversed().map { Care.of(it) }.first { it in tied }
    }

    /** Tail colour: decided by the first [itemsFor] stage-1 items. */
    fun tailFor(log: String): TailColor = when (dominant(log.take(itemsFor[1]))) {
        Care.FOOD -> TailColor.RED
        Care.YARN -> TailColor.BLACK
        else -> TailColor.WHITE            // milk or brush
    }

    /**
     * Breed: tail colour + what was given most from the tail stage until hatching.
     * Each tail colour has two possible cats, decided by a pair of items.
     */
    fun breedFor(log: String): Breed {
        val tail = tailFor(log)
        val mid = log.drop(itemsFor[1]).take(itemsFor[HATCH_STAGE] - itemsFor[1])
        fun groupWins(a: Set<Care>, b: Set<Care>): Boolean {
            val ca = mid.count { Care.of(it) in a }; val cb = mid.count { Care.of(it) in b }
            if (ca != cb) return ca > cb
            val last = mid.lastOrNull()?.let { Care.of(it) } ?: return true
            return last in a
        }
        return when (tail) {
            TailColor.RED -> if (groupWins(setOf(Care.FOOD, Care.MILK), setOf(Care.BRUSH, Care.YARN))) Breed.MOPPEL else Breed.KIRSCHE
            TailColor.BLACK -> if (groupWins(setOf(Care.YARN, Care.FOOD), setOf(Care.BRUSH, Care.MILK))) Breed.MITTERNACHT else Breed.DOMINO
            TailColor.WHITE -> if (groupWins(setOf(Care.MILK, Care.FOOD), setOf(Care.BRUSH, Care.YARN))) Breed.KARAMELL else Breed.WOLKE
        }
    }
}

// ---------------------------------------------------------------- needs (from hatching on)

/** The four needs, 0..1 (1 = completely happy). */
data class Needs(val hunger: Float = 1f, val thirst: Float = 1f, val play: Float = 1f, val cuddle: Float = 1f) {
    val min get() = minOf(hunger, thirst, play, cuddle)
    fun of(c: Care) = when (c) { Care.FOOD -> hunger; Care.MILK -> thirst; Care.YARN -> play; Care.BRUSH -> cuddle }
    fun plus(c: Care, v: Float): Needs = when (c) {
        Care.FOOD -> copy(hunger = (hunger + v).coerceAtMost(1f))
        Care.MILK -> copy(thirst = (thirst + v).coerceAtMost(1f))
        Care.YARN -> copy(play = (play + v).coerceAtMost(1f))
        Care.BRUSH -> copy(cuddle = (cuddle + v).coerceAtMost(1f))
    }
    fun encode() = listOf(hunger, thirst, play, cuddle).joinToString(",")
    companion object {
        fun decode(s: String?): Needs? = s?.split(",")?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 4 }
            ?.let { Needs(it[0], it[1], it[2], it[3]) }
    }
}

enum class Mood(val title: String, val emoji: String) { HAPPY("Glücklich", "😸"), OK("Zufrieden", "🙂"), SAD("Vermisst dich", "🥺") }

object NeedRules {
    /** a full need takes this long to sink to "vermisst dich" */
    const val HOURS_TO_SAD = 18f
    const val SAD_BELOW = 0.3f
    const val HAPPY_FROM = 0.5f
    /** never lower than this – the kitten only gets a bit sad, nothing worse */
    const val FLOOR = 0.15f
    const val ITEM_FILL = 0.5f
    /** one petting stroke (a heart) */
    const val PET_FILL = 0.03f
    const val HAPPY_BONUS_TALER = 1

    private val perHour = (1f - SAD_BELOW) / HOURS_TO_SAD

    fun decayed(n: Needs, sinceMs: Long, nowMs: Long): Needs {
        val drop = ((nowMs - sinceMs).coerceAtLeast(0) / 3_600_000f) * perHour
        fun d(v: Float) = if (v <= FLOOR) v else maxOf(FLOOR, v - drop)
        return Needs(d(n.hunger), d(n.thirst), d(n.play), d(n.cuddle))
    }

    fun mood(n: Needs): Mood = when {
        n.min < SAD_BELOW -> Mood.SAD
        n.min >= HAPPY_FROM -> Mood.HAPPY
        else -> Mood.OK
    }
}
