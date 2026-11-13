package de.schnurrfeld.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** What the sky outside the windows looks like: follows the real time of day. */
object Sky {
    /** tests and screenshots can pin the hour (0..24) */
    var hourOverride: Float? = null

    fun hour(): Float = hourOverride ?: Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) + it.get(Calendar.MINUTE) / 60f }

    /** 0 = full day, 1 = full night */
    fun night(h: Float): Float = when {
        h < 5f -> 1f
        h < 7f -> 1f - (h - 5f) / 2f
        h < 18f -> 0f
        h < 20.5f -> (h - 18f) / 2.5f
        else -> 1f
    }

    /** 0..1, strongest at sunrise and sunset */
    fun warm(h: Float): Float = maxOf(bump(h, 6.3f, 1.3f), bump(h, 19.2f, 1.5f))

    private fun bump(h: Float, c: Float, r: Float) = (1f - abs(h - c) / r).coerceAtLeast(0f)
}

private fun mix(a: Color, b: Color, k: Float) = lerp(a, b, k.coerceIn(0f, 1f))

/** deterministic pseudo random 0..1 */
fun noise(i: Int, k: Int = 0): Float {
    var x = i * 374761393 + k * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    return ((x xor (x ushr 16)) and 0xFFFF) / 65535f
}

/** which window: 0 = the room with the tree, 1 = living room, 2 = bedroom */
fun DrawScope.drawSky(r: Rect, t: Float, kind: Int, burst: Float? = null, hour: Float = Sky.hour()) {
    val n = Sky.night(hour); val warm = Sky.warm(hour)
    var top = mix(Color(0xFF6FB8EE), Color(0xFF0B1230), n)
    var bot = mix(Color(0xFFD5EEFF), Color(0xFF2B3A78), n)
    top = mix(top, Color(0xFF7A74C8), warm * 0.75f)
    bot = mix(bot, Color(0xFFFFB07A), warm * 0.85f)
    fun px(fx: Float) = r.left + r.width * fx
    fun py(fy: Float) = r.top + r.height * fy
    val s = minOf(r.width, r.height)

    clipPath(Path().apply { addRect(r) }) {
        drawRect(Brush.verticalGradient(listOf(top, bot), r.top, r.bottom), r.topLeft, r.size)

        // stars
        if (n > 0.3f) for (i in 0..19) {
            val a = (n - 0.3f) / 0.7f * (0.55f + 0.45f * sin(t * 1.7f + i * 2.1f))
            drawCircle(Color.White, s * (0.008f + 0.008f * noise(i, 3)), Offset(px(noise(i, 1)), py(noise(i, 2) * 0.75f)), alpha = a.coerceIn(0f, 1f))
        }
        // sun
        if (hour in 5.5f..19.5f) {
            val u = (hour - 5.5f) / 14f
            val c = Offset(px(0.12f + 0.76f * u), py(0.88f - 0.72f * sin(PI.toFloat() * u)))
            val col = mix(Color(0xFFFFE27A), Color(0xFFFF8F4A), warm)
            drawCircle(Brush.radialGradient(listOf(col.copy(alpha = 0.5f), Color.Transparent), c, s * 0.42f), s * 0.42f, c)
            drawCircle(col, s * 0.13f, c)
        }
        // moon (a crescent)
        if (n > 0.3f) {
            val u = ((hour - 19f + 24f) % 24f) / 11.5f
            if (u in 0f..1f) {
                val c = Offset(px(0.15f + 0.7f * u), py(0.8f - 0.6f * sin(PI.toFloat() * u)))
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3C4).copy(alpha = 0.35f), Color.Transparent), c, s * 0.4f), s * 0.4f, c)
                drawCircle(Color(0xFFFFF3C4), s * 0.14f, c)
                drawCircle(mix(top, bot, 0.4f), s * 0.12f, Offset(c.x + s * 0.07f, c.y - s * 0.03f))
            }
        }
        // clouds drift by
        for (i in 0..2) {
            val x = ((t * 0.011f * (1f + 0.3f * i) + i * 0.37f) % 1.5f) - 0.25f
            val y = 0.2f + 0.18f * i
            val col = mix(Color.White, Color(0xFF8E9ACB), n).copy(alpha = 0.92f - 0.55f * n)
            for ((dx, dy, rw) in listOf(Triple(0f, 0f, 0.22f), Triple(0.13f, 0.02f, 0.17f), Triple(-0.12f, 0.03f, 0.15f))) {
                drawOval(col, Offset(px(x + dx - rw / 2), py(y + dy) - s * 0.05f), androidx.compose.ui.geometry.Size(r.width * rw, s * 0.11f))
            }
        }

        // ---- things that happen out there
        val day = n < 0.5f
        fun cycle(period: Float, dur: Float, offset: Float): Float? {
            val a = (t + offset) % period
            return if (a < dur) a / dur else null
        }
        when (kind) {
            1 -> {                                                           // living room: birds by day, butterfly, fireflies and a shooting star at night
                if (day) {
                    cycle(17f, 5f, 3f)?.let { p -> birds(Offset(px(-0.1f + 1.2f * p), py(0.3f + 0.08f * sin(p * 6f))), s, t, 3, Color(0xFF3B4A6B)) }
                    cycle(29f, 8f, 11f)?.let { p -> butterfly(Offset(px(1.1f - 1.2f * p), py(0.62f + 0.1f * sin(p * 9f))), s, t) }
                } else {
                    for (i in 0..6) {
                        val a = 0.4f + 0.6f * (0.5f + 0.5f * sin(t * 1.3f + i * 1.9f))
                        val c = Offset(px(0.1f + 0.8f * noise(i, 5) + 0.05f * sin(t * 0.4f + i)), py(0.55f + 0.4f * noise(i, 6) + 0.04f * cos(t * 0.5f + i)))
                        drawCircle(Color(0xFFD7FF7A).copy(alpha = 0.25f * a), s * 0.05f, c)
                        drawCircle(Color(0xFFF4FFB0), s * 0.016f, c, alpha = a)
                    }
                    cycle(33f, 5f, 8f)?.let { p -> bat(Offset(px(-0.1f + 1.2f * p), py(0.3f + 0.12f * sin(p * 11f))), s, t) }
                }
            }
            2 -> {                                                           // bedroom: an owl and shooting stars at night, birds by day
                if (!day) cycle(37f, 7f, 5f)?.let { p -> owl(Offset(px(-0.15f + 1.3f * p), py(0.36f + 0.1f * sin(p * 5f))), s, t) }
                else cycle(21f, 5f, 4f)?.let { p -> birds(Offset(px(1.1f - 1.2f * p), py(0.35f + 0.07f * sin(p * 7f))), s, t, 2, Color(0xFF3B4A6B)) }
            }
            else -> if (day) cycle(25f, 5f, 6f)?.let { p -> birds(Offset(px(-0.1f + 1.2f * p), py(0.3f)), s, t, 2, Color(0xFF3B4A6B)) }
        }
        // shooting stars at night (a different rhythm in each room)
        if (!day && kind != 0) cycle(if (kind == 2) 23f else 41f, 1.2f, if (kind == 2) 0f else 14f)?.let { shootingStar(r, it) }
        // a tap on the window makes something happen right away
        burst?.let { p ->
            when {
                !day -> shootingStar(r, p)
                kind == 1 -> { butterfly(Offset(px(0.1f + 0.9f * p), py(0.6f - 0.25f * sin(p * PI.toFloat()))), s, t); birds(Offset(px(-0.1f + 1.2f * p), py(0.25f)), s, t, 3, Color(0xFF3B4A6B)) }
                else -> birds(Offset(px(-0.1f + 1.2f * p), py(0.3f + 0.05f * sin(p * 8f))), s, t, 3, Color(0xFF3B4A6B))
            }
        }
    }
}

private fun DrawScope.shootingStar(r: Rect, p: Float) {
    val head = Offset(r.left + r.width * (0.95f - 0.7f * p), r.top + r.height * (0.12f + 0.5f * p))
    val tail = Offset(head.x + r.width * 0.22f, head.y - r.height * 0.15f)
    val a = (1f - p).coerceIn(0f, 1f)
    drawLine(Brush.linearGradient(listOf(Color.White.copy(alpha = a), Color.Transparent), head, tail), head, tail, minOf(r.width, r.height) * 0.018f, StrokeCap.Round)
    drawCircle(Color.White, minOf(r.width, r.height) * 0.025f, head, alpha = a)
}

private fun DrawScope.birds(c: Offset, s: Float, t: Float, count: Int, col: Color) {
    for (i in 0 until count) {
        val o = Offset(c.x - i * s * 0.16f, c.y + (if (i % 2 == 0) 1 else -1) * i * s * 0.05f)
        val flap = sin(t * 11f + i * 1.7f)
        val w = s * 0.07f
        drawLine(col, o, Offset(o.x - w, o.y - w * 0.5f * flap - w * 0.15f), s * 0.014f, StrokeCap.Round)
        drawLine(col, o, Offset(o.x + w, o.y - w * 0.5f * flap - w * 0.15f), s * 0.014f, StrokeCap.Round)
    }
}

private fun DrawScope.butterfly(c: Offset, s: Float, t: Float) {
    val f = 0.35f + 0.65f * abs(sin(t * 9f))
    val w = s * 0.045f
    drawOval(Color(0xFFFF9EC4), Offset(c.x - w * f * 1.2f, c.y - w), androidx.compose.ui.geometry.Size(w * f * 1.2f, w * 1.3f))
    drawOval(Color(0xFFFF9EC4), Offset(c.x, c.y - w), androidx.compose.ui.geometry.Size(w * f * 1.2f, w * 1.3f))
    drawOval(Color(0xFFFFD36B), Offset(c.x - w * f * 0.8f, c.y), androidx.compose.ui.geometry.Size(w * f * 0.8f, w * 0.9f))
    drawOval(Color(0xFFFFD36B), Offset(c.x, c.y), androidx.compose.ui.geometry.Size(w * f * 0.8f, w * 0.9f))
    drawLine(Color(0xFF5B4A5E), Offset(c.x, c.y - w * 0.6f), Offset(c.x, c.y + w * 0.8f), s * 0.012f, StrokeCap.Round)
}

private fun DrawScope.bat(c: Offset, s: Float, t: Float) {
    val f = sin(t * 14f)
    val w = s * 0.09f
    val body = Color(0xFF1B1B2B)
    drawOval(body, Offset(c.x - w * 0.18f, c.y - w * 0.2f), androidx.compose.ui.geometry.Size(w * 0.36f, w * 0.5f))
    for (side in listOf(-1f, 1f)) {
        val p = Path().apply {
            moveTo(c.x, c.y); lineTo(c.x + side * w, c.y - w * 0.55f * f - w * 0.1f); lineTo(c.x + side * w * 0.65f, c.y + w * 0.1f)
            lineTo(c.x + side * w * 0.35f, c.y + w * 0.05f); close()
        }
        drawPath(p, body)
    }
}

private fun DrawScope.owl(c: Offset, s: Float, t: Float) {
    val f = sin(t * 9f)
    val w = s * 0.17f
    val body = Color(0xFF6B4F3A); val light = Color(0xFFC9A57A)
    for (side in listOf(-1f, 1f)) {
        rotate(side * (-18f + 30f * f), pivot = Offset(c.x + side * w * 0.2f, c.y)) {
            drawOval(Color(0xFF55402F), Offset(if (side < 0) c.x - w * 1.05f else c.x + w * 0.1f, c.y - w * 0.2f), androidx.compose.ui.geometry.Size(w * 0.95f, w * 0.38f))
        }
    }
    drawOval(body, Offset(c.x - w * 0.3f, c.y - w * 0.15f), androidx.compose.ui.geometry.Size(w * 0.6f, w * 0.75f))
    drawOval(light, Offset(c.x - w * 0.2f, c.y + w * 0.05f), androidx.compose.ui.geometry.Size(w * 0.4f, w * 0.45f))
    drawCircle(body, w * 0.27f, Offset(c.x, c.y - w * 0.2f))
    for (sx in listOf(-1f, 1f)) {
        drawCircle(Color(0xFFFFE27A), w * 0.1f, Offset(c.x + sx * w * 0.11f, c.y - w * 0.22f))
        drawCircle(Color(0xFF2B2A33), w * 0.045f, Offset(c.x + sx * w * 0.11f, c.y - w * 0.22f))
        val tuft = Path().apply { moveTo(c.x + sx * w * 0.2f, c.y - w * 0.42f); lineTo(c.x + sx * w * 0.3f, c.y - w * 0.62f); lineTo(c.x + sx * w * 0.07f, c.y - w * 0.46f); close() }
        drawPath(tuft, body)
    }
    val beak = Path().apply { moveTo(c.x - w * 0.04f, c.y - w * 0.14f); lineTo(c.x + w * 0.04f, c.y - w * 0.14f); lineTo(c.x, c.y - w * 0.05f); close() }
    drawPath(beak, Color(0xFFFFB347))
}
