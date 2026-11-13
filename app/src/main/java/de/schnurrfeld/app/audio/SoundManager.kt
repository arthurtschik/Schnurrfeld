package de.schnurrfeld.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import de.schnurrfeld.app.R

enum class Sfx(val res: Int) {
    PLOP(R.raw.plop), PLOP_2(R.raw.plop_2), PLOP_3(R.raw.plop_3), ERASE(R.raw.erase), ERROR(R.raw.error),
    SUCCESS_1(R.raw.success_1), SUCCESS_2(R.raw.success_2), SUCCESS_3(R.raw.success_3),
    SUCCESS_4(R.raw.success_4), SUCCESS_5(R.raw.success_5),
    WIN(R.raw.win), JOKER(R.raw.joker), TAP(R.raw.tap),
    FILL(R.raw.fill), PRIZE(R.raw.prize), SLAM(R.raw.slam),
    PURR(R.raw.purr), COIN(R.raw.coin), CRANK(R.raw.crank), NOM(R.raw.nom),
    // real recordings: kitten calls (high) and a bigger cat (lower)
    MEOW_KITTEN_1(R.raw.meow_kitten_1), MEOW_KITTEN_2(R.raw.meow_kitten_2), MEOW_KITTEN_3(R.raw.meow_kitten_3),
    MEOW_KITTEN_4(R.raw.meow_kitten_4), MEOW_KITTEN_5(R.raw.meow_kitten_5), MEOW_KITTEN_LONG(R.raw.meow_kitten_long),
    MEOW_CAT_1(R.raw.meow_cat_1), MEOW_CAT_2(R.raw.meow_cat_2), MEOW_CAT_3(R.raw.meow_cat_3),
    MEOW_CAT_4(R.raw.meow_cat_4), MEOW_CAT_5(R.raw.meow_cat_5), MEOW_CAT_TRILL(R.raw.meow_cat_trill),
    MEOW_CAT_LONG(R.raw.meow_cat_long);

    val isMeow get() = name.startsWith("MEOW_")
}

/** how loud the cat meows: the recordings are much louder than the synthetic sounds, so this is turned down a lot */
const val MEOW_VOLUME = 0.14f

/**
 * A meow that fits the cat's age: small cats squeak, grown ones sound deeper.
 * [long] is the drawn-out call used when the cat misses you or is overjoyed.
 */
fun meowFor(stage: Int, long: Boolean, rnd: kotlin.random.Random = kotlin.random.Random.Default): Sfx {
    val kitten = listOf(Sfx.MEOW_KITTEN_1, Sfx.MEOW_KITTEN_2, Sfx.MEOW_KITTEN_3, Sfx.MEOW_KITTEN_4, Sfx.MEOW_KITTEN_5)
    val cat = listOf(Sfx.MEOW_CAT_1, Sfx.MEOW_CAT_2, Sfx.MEOW_CAT_3, Sfx.MEOW_CAT_4, Sfx.MEOW_CAT_5)
    return when {
        stage <= 3 -> if (long) Sfx.MEOW_KITTEN_LONG else kitten.random(rnd)
        stage == 4 -> if (long) (if (rnd.nextBoolean()) Sfx.MEOW_KITTEN_LONG else Sfx.MEOW_CAT_LONG) else (kitten + Sfx.MEOW_CAT_3).random(rnd)
        stage == 5 -> if (long) Sfx.MEOW_CAT_LONG else (cat + kitten.take(2)).random(rnd)
        else -> if (long) Sfx.MEOW_CAT_LONG else (cat + Sfx.MEOW_CAT_TRILL).random(rnd)
    }
}

/** playback speed that makes the same recording sound a little different every time (and the youngest a bit squeakier) */
fun meowRate(sfx: Sfx, stage: Int, rnd: kotlin.random.Random = kotlin.random.Random.Default): Float {
    val base = if (sfx.name.startsWith("MEOW_KITTEN")) (if (stage <= 2) 1.1f else 1.0f) else 0.97f
    return base + (rnd.nextFloat() - 0.5f) * 0.1f
}

/** One of three slightly different bubble pops, so crosses never sound mechanical. */
fun plopSfx() = listOf(Sfx.PLOP, Sfx.PLOP_2, Sfx.PLOP_3).random()

/** Success chime for an enthusiasm tier 0..4 – each one a little bigger than the last. */
fun successSfx(tier: Int) = listOf(Sfx.SUCCESS_1, Sfx.SUCCESS_2, Sfx.SUCCESS_3, Sfx.SUCCESS_4, Sfx.SUCCESS_5)[tier.coerceIn(0, 4)]

enum class Haptic { TICK, CLICK, SUCCESS, ERROR }

class SoundManager(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
    private val ids = Sfx.entries.associateWith { pool.load(context, it.res, 1) }

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun play(sfx: Sfx, volume: Float = 1f, rate: Float = 1f) {
        val id = ids[sfx] ?: return
        pool.play(id, volume, volume, 1, 0, rate.coerceIn(0.5f, 2f))
    }

    /** Plays [sfx] in an endless loop; returns a stream id for [stop]. */
    fun loop(sfx: Sfx, volume: Float = 1f): Int {
        val id = ids[sfx] ?: return 0
        return pool.play(id, volume, volume, 2, -1, 1f)
    }

    fun stop(stream: Int) { if (stream != 0) pool.stop(stream) }

    fun vibrate(h: Haptic) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = when (h) {
            Haptic.TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            Haptic.CLICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            Haptic.SUCCESS -> VibrationEffect.createWaveform(longArrayOf(0, 18, 60, 28), intArrayOf(0, 120, 0, 200), -1)
            Haptic.ERROR -> VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 60), intArrayOf(0, 255, 0, 200), -1)
        }
        runCatching { v.vibrate(effect) }
    }
}
