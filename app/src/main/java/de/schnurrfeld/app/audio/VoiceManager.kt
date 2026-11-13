package de.schnurrfeld.app.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.speech.tts.TextToSpeech
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.mutableIntStateOf
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

/** upper limit for imported voice files */
private const val MAX_IMPORT_BYTES = 10L * 1024 * 1024

class VoiceManager(private val context: Context) {
    private val dir = File(context.filesDir, "voice").apply { mkdirs() }
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var player: MediaPlayer? = null
    private var recorder: MediaRecorder? = null
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

    /** bumps whenever recordings change, so the UI can refresh */
    val revision = mutableIntStateOf(0)

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

    fun fileFor(p: Praise) = File(dir, "${p.name.lowercase()}.audio")
    fun hasRecording(p: Praise) = fileFor(p).let { it.exists() && it.length() > 0 }

    fun hasBundled(p: Praise) = bundledIds.containsKey(p)

    /** true if this word will actually be heard (own recording or bundled file) */
    fun isAvailable(p: Praise) = hasRecording(p) || hasBundled(p)

    fun speak(p: Praise, ttsFallback: Boolean) {
        if (hasRecording(p)) {
            playFile(fileFor(p))
        } else if (bundledIds[p] != null) {
            pool.stop(lastStream)
            lastStream = pool.play(bundledIds[p]!!, 0.95f, 0.95f, 2, 0, 1f)
        } else if (ttsFallback) {
            ensureTts()
            if (ttsReady) tts?.speak(p.text, TextToSpeech.QUEUE_FLUSH, null, p.name)
        }
    }

    fun playFile(f: File) {
        runCatching {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(f.absolutePath)
                setOnCompletionListener { it.release(); if (player === it) player = null }
                prepare()
                start()
            }
        }
    }

    fun startRecording(p: Praise): Boolean = runCatching {
        stopRecording()
        val tmp = File(dir, "${p.name.lowercase()}.tmp")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(128_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(tmp.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        recordingFor = p
        true
    }.getOrElse { recorder = null; recordingFor = null; false }

    var recordingFor: Praise? = null
        private set

    fun stopRecording() {
        val r = recorder ?: return
        val p = recordingFor
        runCatching { r.stop() }
        r.release()
        recorder = null
        recordingFor = null
        if (p != null) {
            val tmp = File(dir, "${p.name.lowercase()}.tmp")
            if (tmp.exists() && tmp.length() > 0) tmp.renameTo(fileFor(p).also { it.delete() })
        }
        revision.intValue++
    }

    /** Copies a picked audio file into the app. Capped (a spoken word is a few seconds) and written atomically. */
    fun import(p: Praise, uri: Uri): Boolean {
        val tmp = File(dir, "${p.name.lowercase()}.tmp")
        return runCatching {
            var size = 0L
            context.contentResolver.openInputStream(uri)!!.use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(32 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        size += n
                        require(size <= MAX_IMPORT_BYTES) { "file too large" }
                        out.write(buf, 0, n)
                    }
                }
            }
            require(size > 0) { "empty file" }
            val target = fileFor(p)
            target.delete()
            require(tmp.renameTo(target)) { "rename failed" }
            revision.intValue++
            true
        }.getOrElse { tmp.delete(); false }
    }

    fun delete(p: Praise) {
        fileFor(p).delete()
        revision.intValue++
    }

    fun release() {
        stopRecording()
        player?.release(); player = null
        tts?.shutdown(); tts = null; ttsReady = false
        pool.release()
    }
}
