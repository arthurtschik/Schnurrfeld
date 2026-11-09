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
    PURR(R.raw.purr), COIN(R.raw.coin), CRANK(R.raw.crank), NOM(R.raw.nom)
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
