package de.schnurrfeld.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.schnurrfeld.app.App
import de.schnurrfeld.app.audio.Haptic
import de.schnurrfeld.app.audio.Sfx
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The daily Taler: a golden button that wiggles until it's tapped; then a coin bursts out
 * with sparkles and a coin sound. Disappears once collected for today.
 */
@Composable
fun DailyTalerButton(app: App, modifier: Modifier = Modifier) {
    val store = app.store
    val today = LocalDate.now()
    var result by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var claimedAt by remember { mutableLongStateOf(0L) }
    val waiting = store.dailyTalerWaiting(today)
    if (!waiting && result == null) return
    val frame = rememberFrameMillis()
    val now = frame.value

    LaunchedEffect(claimedAt) {
        if (claimedAt == 0L) return@LaunchedEffect
        if (store.settings.sound) app.sound.play(Sfx.COIN)
        if (store.settings.vibration) app.sound.vibrate(Haptic.SUCCESS)
        delay(260)
        if (store.settings.sound) app.sound.play(Sfx.PRIZE, 0.8f)
        delay(2600)
        result = null
    }

    Box(modifier.width(320.dp).height(if (result == null) 64.dp else 120.dp), contentAlignment = Alignment.Center) {
        val res = result
        if (res == null) {
            // wiggles in short bursts and pulses gently – "tap me!"
            val c = now % 2400
            val wob = if (c < 650) sin(c / 650f * 6f * PI.toFloat()) * 6f * (1f - c / 650f) else 0f
            val pulse = 1f + 0.035f * sin(now / 320f)
            Surface(
                onClick = {
                    store.claimDaily(today)?.let { result = it; claimedAt = android.os.SystemClock.uptimeMillis() }
                },
                shape = RoundedCornerShape(50),
                color = Color.Transparent,
                border = BorderStroke(2.dp, Color(0xFFE0A21F)),
                shadowElevation = 8.dp,
                modifier = Modifier.graphicsLayer { rotationZ = wob; scaleX = pulse; scaleY = pulse }.testTag("daily_taler"),
            ) {
                Box {
                    Canvas(Modifier.matchParentSize()) {
                        drawRect(Brush.horizontalGradient(listOf(Color(0xFFFFE58A), Color(0xFFFFC94A), Color(0xFFFFE58A))))
                        // a shine sweeping across
                        val x = ((now % 2400) / 2400f) * size.width * 2f - size.width * 0.5f
                        drawRect(Brush.linearGradient(listOf(Color(0x00FFFFFF), Color(0x99FFFFFF), Color(0x00FFFFFF)),
                            Offset(x - 40f, 0f), Offset(x + 40f, size.height)))
                    }
                    Row(Modifier.padding(start = 10.dp, end = 18.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Canvas(Modifier.size(38.dp)) {
                            val spin = cos(now / 420f)
                            drawTaler(Offset(size.width / 2, size.height / 2), size.width * 0.46f, abs(spin).coerceAtLeast(0.15f))
                        }
                        Column(Modifier.padding(start = 8.dp)) {
                            Text("Dein Tages-Taler wartet!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4A3305))
                            Text("Antippen zum Einsammeln", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6E5214))
                        }
                    }
                }
            }
        } else {
            // the burst
            val age = (android.os.SystemClock.uptimeMillis() - claimedAt).coerceAtLeast(0)
            val k = (age / 2600f).coerceIn(0f, 1f)
            Canvas(Modifier.size(320.dp, 120.dp)) {
                val center = Offset(size.width / 2, size.height * 0.45f)
                // flying little coins
                for (i in 0 until 14) {
                    val ang = i * 2f * PI.toFloat() / 14 + 0.3f
                    val t = (age / 1000f).coerceAtMost(1.6f)
                    val speed = size.height * (0.9f + (i % 3) * 0.25f)
                    val p = Offset(center.x + cos(ang) * speed * t, center.y + sin(ang) * speed * t * 0.6f + 140f * t * t)
                    val a = (1f - t / 1.6f).coerceIn(0f, 1f)
                    if (a > 0f) drawTaler(p, size.height * 0.07f, abs(cos(t * 9f + i)).coerceAtLeast(0.2f))
                }
                // sparkle ring
                val ring = (age / 600f).coerceAtMost(1f)
                drawCircle(Color(0xFFFFE08A), size.height * (0.2f + 0.5f * ring), center, alpha = (1f - ring) * 0.8f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(size.height * 0.04f))
                // the big coin pops up and spins
                val pop = if (age < 300) age / 300f * 1.25f else 1.25f - 0.25f * ((age - 300) / 300f).coerceAtMost(1f)
                val fade = if (k > 0.8f) (1f - k) / 0.2f else 1f
                if (fade > 0f) drawTaler(center, size.height * 0.3f * pop, abs(cos(age / 160f)).coerceAtLeast(0.15f))
            }
            val fade = if (k > 0.8f) (1f - k) / 0.2f else 1f
            Column(Modifier.align(Alignment.BottomCenter).graphicsLayer { alpha = fade; translationY = -k * 20f }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("+${res.first + res.second} Taler!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color(0xFFD2901A))
                if (res.second > 0) Text("inkl. +${res.second} Glückliche-Katze-Bonus 😸", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

