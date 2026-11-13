package de.schnurrfeld.app.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.util.Locale

enum class VoiceKind { PRAISE, COMBO, FINISH, PERFECT }

/**
 * Everything the game can say. PRAISE words are grouped into enthusiasm tiers (0 = calm … 4 = ecstatic);
 * words in the same tier are interchangeable and picked at random for variety.
 * [resName] is looked up in res/raw at runtime – a missing file just means "not recorded yet".
 */
enum class Praise(val text: String, val resName: String, val tier: Int, val kind: VoiceKind = VoiceKind.PRAISE) {
    NICE("Nice!", "voice_nice", 0),
    GREAT("Great!", "voice_great", 1),
    INCREDIBLE("Incredible!", "voice_incredible", 2),

    KOMBO("Kombo!", "voice_kombo", 1, VoiceKind.COMBO),
    QUICK_KOMBO("Quick Kombo!", "voice_quick_kombo", 2, VoiceKind.COMBO),
    SUPER_FAST("Super fast!", "voice_super_fast", 3, VoiceKind.COMBO),

    FINISH("Finish!", "voice_finish", 1, VoiceKind.FINISH),
    NICE_JOB("Nice job!", "voice_nice_job", 1, VoiceKind.FINISH),
    YOU_DID_IT("You did it!", "voice_you_did_it", 1, VoiceKind.FINISH),

    PERFECT("Purrfect!", "voice_perfect", 4, VoiceKind.PERFECT);

    companion object {
        /** highest tier of the PRAISE words */
        const val TOP_TIER = 2

        /**
         * Tier for the n-th error-free cat in a row on a board of [size]:
         * spread over the whole puzzle so it keeps building up until the last cat.
         */
        fun tierFor(streak: Int, size: Int, top: Int = TOP_TIER): Int {
            val span = (size - 2).coerceAtLeast(1)
            return Math.round((streak - 1) * top.toFloat() / span).coerceIn(0, top)
        }

        /** Random praise word of the tier, avoiding an immediate repeat of [last]. */
        fun pick(tier: Int, last: Praise?, random: kotlin.random.Random = kotlin.random.Random): Praise {
            val options = entries.filter { it.kind == VoiceKind.PRAISE && it.tier == tier }
            val fresh = options.filter { it != last }.ifEmpty { options }
            return fresh.random(random)
        }
    }
}

class VoiceManager(private val context: Context) {
    init {
        // older versions could record own voices: that feature is gone, so the leftover files are removed
        runCatching { File(context.filesDir, "voice").takeIf { it.exists() }?.deleteRecursively() }
    }
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    ).build()
    @android.annotation.SuppressLint("DiscouragedApi")
    private val bundledIds = Praise.entries.mapNotNull { p ->
        val id = context.resources.getIdentifier(p.resName, "raw", context.packageName)
        if (id != 0) p to pool.load(context, id, 1) else null
    }.toMap()
    private var lastStream = 0

    private fun ensureTts() {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setPitch(1.35f)
                tts?.setSpeechRate(1.05f)
                ttsReady = true
            }
        }
    }

    fun warmUp(useTts: Boolean) { if (useTts) ensureTts() }

    fun hasBundled(p: Praise) = bundledIds.containsKey(p)

    /** true if this word will actually be heard from a bundled recording */
    fun isAvailable(p: Praise) = hasBundled(p)

    fun speak(p: Praise, ttsFallback: Boolean) {
        val id = bundledIds[p]
        if (id != null) {
            pool.stop(lastStream)
            lastStream = pool.play(id, 0.95f, 0.95f, 2, 0, 1f)
        } else if (ttsFallback) {
            ensureTts()
            if (ttsReady) tts?.speak(p.text, TextToSpeech.QUEUE_FLUSH, null, p.name)
        }
    }

    fun release() {
        tts?.shutdown(); tts = null; ttsReady = false
        pool.release()
    }
}
