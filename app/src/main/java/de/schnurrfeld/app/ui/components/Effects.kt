package de.schnurrfeld.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import de.schnurrfeld.app.ui.theme.LocalDark
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * The combo bar: a glass tube with shimmering milk that sloshes when it gains points.
 * [gainAt] is the uptime of the last gain (drives the slosh).
 */
@Composable
fun MilkBar(points: Int, max: Int, gainAt: Long, modifier: Modifier = Modifier) {
    val frame = rememberFrameMillis()
    val dark = LocalDark.current
    val level by animateFloatAsState(
        points.toFloat() / max, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessVeryLow), label = "milk"
    )
    val bubbles = remember { List(9) { Triple(Random.nextFloat(), Random.nextFloat(), 0.6f + Random.nextFloat()) } }

    Canvas(modifier) {
        val now = frame.value
        val t = now / 1000f
        val w = size.width; val h = size.height
        val r = h / 2
        val tube = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(r))) }
        drawPath(tube, if (dark) Color(0xFF2B2A31) else Color(0xFFE6D8C2))

        // segment ticks so the 9 steps are readable
        for (i in 1 until max) {
            val x = w * i / max
            drawLine(if (dark) Color.White else Color(0xFF8C7B64), Offset(x, h * 0.3f), Offset(x, h * 0.7f), density, alpha = 0.18f)
        }

        if (level > 0.001f) clipPath(tube) {
            val age = (now - gainAt).coerceAtLeast(0)
            val slosh = 7f * density * exp(-age / 450f)
            val amp = 1.6f * density + slosh
            val front = w * level
            val milk = Path().apply {
                moveTo(-2f, -2f)
                var y = -2f
                while (y <= h + 2f) {
                    val k = y / h
                    val x = front + amp * sin(k * 2f * PI.toFloat() + t * 4.2f) + amp * 0.5f * sin(k * 5f * PI.toFloat() - t * 6.5f)
                    lineTo(x, y); y += 1.5f
                }
                lineTo(-2f, h + 2f); close()
            }
            // milky body
            drawPath(milk, Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF7EFE3), Color(0xFFEBDDCB)), 0f, h))
            clipPath(milk) {
                // pearly, slowly drifting iridescent sheen
                val shift = (t * 60f * density) % (w * 2)
                drawRect(
                    Brush.linearGradient(
                        listOf(
                            Color(0x00FFFFFF), Color(0x55FFC4DA), Color(0x55C8D8FF), Color(0x55C9F2E6),
                            Color(0x55FFE9B8), Color(0x00FFFFFF),
                        ),
                        start = Offset(shift - w, 0f), end = Offset(shift, h),
                    )
                )
                // glossy highlight along the top
                drawRoundRect(Color.White, Offset(r * 0.6f, h * 0.16f), Size(w, h * 0.16f), CornerRadius(h), alpha = 0.75f)
                // tiny bubbles drifting along
                for ((bx, by, sp) in bubbles) {
                    val x = ((bx * front + t * 14f * density * sp) % front.coerceAtLeast(1f))
                    val y = h * (0.35f + 0.45f * by) + sin(t * 3f * sp + bx * 9f) * h * 0.08f
                    drawCircle(Color.White, h * 0.09f * sp, Offset(x, y), alpha = 0.9f)
                    drawCircle(Color(0xFFD8C8B0), h * 0.09f * sp, Offset(x, y), alpha = 0.6f, style = Stroke(density * 0.8f))
                }
            }
        }
        // glass rim
        drawPath(tube, if (dark) Color.White else Color(0xFF8C7B64), alpha = 0.18f, style = Stroke(density * 1.2f))
    }
}

private class Particle(
    val x0: Float, val y0: Float, val vy: Float, val sway: Float, val swayF: Float, val phase: Float,
    val spin: Float, val len: Float, val color: Color, val tinsel: Boolean, val delay: Float,
)

private val TinselColors = listOf(
    Color(0xFFF5C451), Color(0xFFE8E8F0), Color(0xFFFF8FB1), Color(0xFF7FC8FF),
    Color(0xFF8EE3B5), Color(0xFFFF9A76), Color(0xFFC7A6FF),
)

/**
 * Full-screen cheer: tinsel and confetti rain; for a perfect round additionally golden light rays,
 * sparkles and a big "PURRFECT!" slamming onto the screen.
 */
@Composable
fun Celebration(startAt: Long, perfect: Boolean, modifier: Modifier = Modifier) {
    val frame = rememberFrameMillis()
    val total = if (perfect) 5200L else 4200L
    val particles = remember(startAt) {
        val rnd = Random(startAt)
        List(if (perfect) 190 else 140) {
            Particle(
                x0 = rnd.nextFloat(), y0 = -0.05f - rnd.nextFloat() * 0.5f,
                vy = 0.22f + rnd.nextFloat() * 0.22f, sway = 0.015f + rnd.nextFloat() * 0.03f,
                swayF = 1.5f + rnd.nextFloat() * 2.5f, phase = rnd.nextFloat() * 6.28f,
                spin = (2f + rnd.nextFloat() * 6f) * if (rnd.nextBoolean()) 1 else -1,
                len = 0.6f + rnd.nextFloat() * 0.8f,
                color = TinselColors[rnd.nextInt(TinselColors.size)], tinsel = rnd.nextFloat() < 0.6f,
                delay = rnd.nextFloat() * 0.6f,
            )
        }
    }
    val sparkles = remember(startAt) {
        val rnd = Random(startAt + 1)
        List(26) { Triple(rnd.nextFloat(), rnd.nextFloat() * 0.8f + 0.05f, rnd.nextFloat() * 6.28f) }
    }
    val now = frame.value
    val age = now - startAt
    if (startAt <= 0L || age < 0 || age > total) return
    val fadeOut = ((total - age) / 900f).coerceIn(0f, 1f)

    BoxWithConstraints(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val W = size.width; val H = size.height
            val t = age / 1000f
            val center = Offset(W / 2, H * 0.42f)

            if (perfect) {
                // golden glow + slowly turning rays
                val inK = ((age - 450) / 400f).coerceIn(0f, 1f) * fadeOut
                if (inK > 0f) {
                    drawCircle(
                        Brush.radialGradient(listOf(Color(0x88FFD36B), Color(0x00FFD36B)), center, W * 0.75f),
                        W * 0.75f, center, alpha = inK
                    )
                    rotate(t * 14f, center) {
                        val rays = 14
                        for (i in 0 until rays) {
                            val a0 = i * 2 * PI.toFloat() / rays
                            val a1 = a0 + PI.toFloat() / rays * 0.55f
                            val len = H
                            val ray = Path().apply {
                                moveTo(center.x, center.y)
                                lineTo(center.x + cos(a0) * len, center.y + sin(a0) * len)
                                lineTo(center.x + cos(a1) * len, center.y + sin(a1) * len)
                                close()
                            }
                            drawPath(
                                ray,
                                Brush.radialGradient(listOf(Color(0x66FFE08A), Color(0x00FFE08A)), center, H * 0.6f),
                                alpha = inK
                            )
                        }
                    }
                    // twinkling sparkles
                    for ((sx, sy, ph) in sparkles) {
                        val tw = (sin(t * 6f + ph) * 0.5f + 0.5f)
                        val rr = W * 0.018f * (0.4f + tw)
                        val c = Offset(sx * W, sy * H)
                        val star = Path().apply {
                            for (k in 0 until 8) {
                                val ang = -PI.toFloat() / 2 + k * PI.toFloat() / 4
                                val rad = if (k % 2 == 0) rr else rr * 0.28f
                                val pp = Offset(c.x + cos(ang) * rad, c.y + sin(ang) * rad)
                                if (k == 0) moveTo(pp.x, pp.y) else lineTo(pp.x, pp.y)
                            }
                            close()
                        }
                        drawPath(star, Color(0xFFFFF4C2), alpha = inK * (0.35f + 0.65f * tw))
                    }
                }
            }

            // tinsel & confetti rain
            for (p in particles) {
                val pt = t - p.delay
                if (pt < 0f) continue
                val y = (p.y0 + p.vy * pt + 0.05f * pt * pt) * H
                if (y > H + 40f) continue
                val x = (p.x0 + p.sway * sin(pt * p.swayF + p.phase)) * W
                val flip = cos(pt * p.spin + p.phase)           // fake 3D flip
                val shine = lerp(p.color, Color.White, (abs(flip) * 0.55f).coerceIn(0f, 1f))
                rotate(sin(pt * 1.7f + p.phase) * 40f + p.phase * 30f, Offset(x, y)) {
                    if (p.tinsel) {
                        val lw = 2.6f * density * (0.35f + 0.65f * abs(flip))
                        val lh = 16f * density * p.len
                        drawRoundRect(shine, Offset(x - lw / 2, y - lh / 2), Size(lw, lh), CornerRadius(lw / 2), alpha = fadeOut)
                    } else {
                        val s = 7f * density * p.len
                        drawRect(shine, Offset(x - s / 2, y - s * abs(flip) / 2), Size(s, s * abs(flip).coerceAtLeast(0.15f)), alpha = fadeOut)
                    }
                }
            }
        }

        if (perfect) {
            // "PURRFECT!" slams onto the screen
            val slamStart = 600L
            val a = age - slamStart
            if (a >= 0) {
                val k = (a / 260f).coerceIn(0f, 1f)
                val scale = if (k < 1f) 3.2f - 2.2f * (k * k) else 1f + 0.07f * exp(-(a - 260) / 140f) * sin((a - 260) / 40f)
                val melt = ((age - 2300) / 380f).coerceIn(0f, 1f)   // then it liquefies and flows into the bar
                val alpha = (if (k < 1f) k * 1.6f else 1f).coerceAtMost(1f) * (1f - melt * melt)
                Box(Modifier.fillMaxSize().graphicsLayer { translationY = -size.height * 0.08f }, contentAlignment = Alignment.Center) {
                    Box(Modifier.graphicsLayer {
                        scaleX = scale * (1f + 0.15f * melt); scaleY = scale * (1f - 0.75f * melt)
                        rotationZ = -6f * (1f - melt); this.alpha = alpha
                    }) {
                        // 3D look from two solid layers (no gradient brush: keeps text rendering simple and robust)
                        Text(
                            "PURRFECT!", color = Color(0xFFC0611A),
                            style = TextStyle(fontSize = 54.sp, fontWeight = FontWeight.Black),
                            modifier = Modifier.graphicsLayer { translationY = 5f * density; translationX = 2f * density }
                        )
                        Text(
                            "PURRFECT!", color = lerp(Color(0xFFFFC53D), Color(0xFFFFF6E6), (melt * 1.6f).coerceAtMost(1f)),
                            style = TextStyle(
                                fontSize = 54.sp, fontWeight = FontWeight.Black,
                                shadow = Shadow(Color(0x99FFF3B0), Offset.Zero, 24f),
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** Screen shake offset (px factor) right after the PURRFECT slam. */
fun slamShake(startAt: Long, perfect: Boolean, now: Long): Float {
    if (!perfect || startAt <= 0) return 0f
    val a = now - startAt - 860
    if (a < 0 || a > 420) return 0f
    return sin(a / 18f) * exp(-a / 120f)
}

/** When the banner starts melting after it popped in (ms). */
const val MELT_START_MS = 450L

/** Screen positions (in root coordinates) needed for the milk flow. */
class FlowGeometry {
    var origin by androidx.compose.runtime.mutableStateOf(Offset.Zero)
    var bar by androidx.compose.runtime.mutableStateOf<androidx.compose.ui.geometry.Rect?>(null)
    var banner by androidx.compose.runtime.mutableStateOf<androidx.compose.ui.geometry.Rect?>(null)
}

private class Drop(
    val sx: Float, val sy: Float, val delay: Float, val dur: Float, val side: Float, val size: Float, val tx: Float,
)

/**
 * Milk droplets that leave the melting words and stream into the combo bar.
 * [fill] is the bar's current fill (0..1) – the droplets aim at the milk's front.
 */
@Composable
fun MilkFlow(flow: de.schnurrfeld.app.game.FlowEvent?, geo: FlowGeometry, fill: Float, modifier: Modifier = Modifier) {
    if (flow == null) return
    val frame = rememberFrameMillis()
    val startedAt = remember(flow.id) { frame.value }
    val drops = remember(flow.id) {
        val rnd = Random(flow.id)
        List(10 + flow.points * 7) {
            Drop(
                sx = rnd.nextFloat(), sy = rnd.nextFloat(),
                delay = rnd.nextFloat() * 170f, dur = 360f + rnd.nextFloat() * 170f,
                side = (rnd.nextFloat() - 0.5f) * 2f, size = 0.6f + rnd.nextFloat() * 0.7f,
                tx = (rnd.nextFloat() - 0.5f),
            )
        }
    }
    val aimAt = remember(flow.id) { fill }
    val age = frame.value - startedAt - if (flow.fromCenter) 0 else MELT_START_MS
    if (age > 1100) return
    val bar = geo.bar ?: return

    Canvas(modifier) {
        val o = geo.origin
        val src = if (flow.fromCenter) {
            androidx.compose.ui.geometry.Rect(size.width * 0.12f, size.height * 0.36f, size.width * 0.88f, size.height * 0.46f)
        } else {
            (geo.banner ?: return@Canvas).translate(-o.x, -o.y)
        }
        val b = bar.translate(-o.x, -o.y)
        val front = (b.left + b.width * aimAt.coerceIn(0.03f, 0.97f))
        val target = Offset(front, b.center.y)
        val milk = Color(0xFFFFF8EC)
        val rim = Color(0xFFD9C7AA)

        for (d in drops) {
            val t = (age - d.delay) / d.dur
            if (t < 0f || t > 1.15f) continue
            val start = Offset(src.left + src.width * d.sx, src.top + src.height * (0.3f + 0.4f * d.sy))
            val end = Offset(target.x + d.tx * 18f * density, target.y)
            val ctrl = Offset(
                (start.x + end.x) / 2 + d.side * 60f * density,
                end.y + (start.y - end.y) * 0.15f - 26f * density,
            )
            fun at(p: Float): Offset {
                val q = p.coerceIn(0f, 1f); val e = q * q * (3 - 2 * q)
                val a = 1 - e
                return Offset(a * a * start.x + 2 * a * e * ctrl.x + e * e * end.x, a * a * start.y + 2 * a * e * ctrl.y + e * e * end.y)
            }
            if (t <= 1f) {
                val r = 4.2f * density * d.size * (1f - 0.45f * t)
                // a little trail so it reads as liquid, not as dots
                for (k in 3 downTo 1) {
                    val tp = t - k * 0.05f
                    if (tp < 0f) continue
                    drawCircle(milk, r * (1f - k * 0.2f), at(tp), alpha = 0.55f - k * 0.13f)
                }
                val pos = at(t)
                drawCircle(milk, r, pos)
                drawCircle(rim, r, pos, alpha = 0.55f, style = Stroke(density * 0.9f))
                drawCircle(Color.White, r * 0.35f, pos + Offset(-r * 0.3f, -r * 0.3f), alpha = 0.95f)
            } else {
                // tiny splash ring where the drop joins the milk
                val k = (t - 1f) / 0.15f
                drawCircle(milk, 6f * density * (0.5f + k), end, alpha = 0.6f * (1 - k), style = Stroke(density * 1.5f))
            }
        }
    }
}
