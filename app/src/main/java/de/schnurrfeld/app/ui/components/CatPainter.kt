package de.schnurrfeld.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import de.schnurrfeld.app.ui.theme.Fur
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// the first 7 are used on the board; PURR and WALK are for the pet in the Katzenkörbchen
enum class CatAnim { DANCE, LICK, SLEEP, WAG, TILT, BOUNCE, LOOK, PURR, WALK, EAT, SWAT }

/**
 * Optional breed details for the Katzenkörbchen cats.
 * [chubby] and [fluffy] (0..1) grow with the cat's stage; [facing] = -1 mirrors the cat.
 */
data class CatLook(
    val tail: Color? = null,
    val tailShade: Color? = null,
    val ears: Color? = null,
    val patch: Color? = null,
    /** a coloured patch on top of the head, between the ears (Kirsche) */
    val cap: Color? = null,
    val chubby: Float = 0f,
    val fluffy: Float = 0f,
    val facing: Float = 1f,
    /** 0..1 – "misses you": droopy ears, big watery eyes with a little tear, worried brows */
    val sad: Float = 0f,
)

private val Ink = Color(0xFF2B2A33)
private val Pink = Color(0xFFF28CA0)
private val Blush = Color(0xFFFF9EB0)

private fun easeOutBack(p: Float): Float {
    val c1 = 1.70158f; val c3 = c1 + 1f
    val x = p - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}

fun popScale(ageMs: Long): Float {
    if (ageMs >= 450) return 1f
    return easeOutBack((ageMs / 450f).coerceIn(0f, 1f)).coerceAtLeast(0f)
}

private fun smooth(e0: Float, e1: Float, x: Float): Float {
    val t = ((x - e0) / (e1 - e0)).coerceIn(0f, 1f)
    return t * t * (3 - 2 * t)
}

/**
 * Draws a chibi cat centred at [center], fitting into a square of [size] px.
 * [t] is time in seconds (already phase-shifted per cat).
 */
fun DrawScope.drawCat(center: Offset, size: Float, fur: Fur, anim: CatAnim, t: Float, pop: Float = 1f, look: CatLook? = null) {
    val u = size / 100f
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun oval(cx: Float, cy: Float, w: Float, h: Float, color: Color, alpha: Float = 1f) =
        drawOval(color, topLeft = p(cx - w / 2, cy - h / 2), size = Size(w * u, h * u), alpha = alpha)
    fun ovalStroke(cx: Float, cy: Float, w: Float, h: Float, color: Color, width: Float) =
        drawOval(color, topLeft = p(cx - w / 2, cy - h / 2), size = Size(w * u, h * u), style = Stroke(width * u))
    val line = { w: Float -> Stroke(width = w * u, cap = StrokeCap.Round, join = StrokeJoin.Round) }

    // ---- animation parameters ----
    var bodyRot = 0f; var bodyY = 0f; var sx = 1f; var sy = 1f
    var headRot = 0f; var tailSway = sin(t * 2.2f) * 0.5f
    var eyes = 0 // 0 open, 1 happy ^^, 2 sleepy, 3 blink line
    var pupilX = 0f
    var leftPaw: Offset? = null; var rightPaw: Offset? = null
    var tongue = false; var mouthOpen = false
    var earTwitch = 0f
    var zzz = false
    var headDrop = 0f

    when (anim) {
        CatAnim.DANCE -> {
            val a = sin(t * 5.5f)
            bodyRot = a * 11f
            bodyY = -abs(cos(t * 5.5f)) * 6f
            leftPaw = Offset(-31f, 6f + a * 7f)
            rightPaw = Offset(31f, 6f - a * 7f)
            eyes = 1; mouthOpen = true
            tailSway = sin(t * 11f)
        }
        CatAnim.LICK -> {
            val c = (t % 3.4f)
            val k = smooth(0.4f, 0.9f, c) * (1f - smooth(2.4f, 2.9f, c))
            val bob = if (k > 0.9f) sin(t * 18f) * 1.8f else 0f
            rightPaw = Offset(13f + (5f - 13f) * k, 38f + (14f - 38f) * k + bob)
            headRot = -9f * k
            if (k > 0.5f) { eyes = 1; tongue = sin(t * 18f) > -0.2f }
            tailSway = sin(t * 1.6f) * 0.6f
        }
        CatAnim.SLEEP -> {
            sy = 1f + 0.035f * sin(t * 2f); sx = 1f - 0.015f * sin(t * 2f)
            headRot = 7f + sin(t * 1f) * 1.5f
            eyes = 2; zzz = true
            tailSway = 0.2f
        }
        CatAnim.WAG -> {
            tailSway = sin(t * 4.5f) * 1.2f
            if ((t % 3.6f) < 0.14f) eyes = 3
        }
        CatAnim.TILT -> {
            headRot = sin(t * 1.4f) * 13f
            val c = t % 2.8f
            if (c < 0.25f) earTwitch = sin(c / 0.25f * PI.toFloat()) * -18f
            if ((t % 4.1f) < 0.14f) eyes = 3
        }
        CatAnim.BOUNCE -> {
            val ph = (t * 1.4f) % 1f
            if (ph < 0.6f) {
                bodyY = -13f * sin(ph / 0.6f * PI.toFloat()); eyes = 1; mouthOpen = true
            } else if (ph < 0.78f) {
                val s = sin((ph - 0.6f) / 0.18f * PI.toFloat())
                sy = 1f - 0.12f * s; sx = 1f + 0.09f * s
            }
            tailSway = sin(t * 8f) * 0.8f
        }
        CatAnim.SWAT -> {                         // playful swipe with one paw (towards where the cat faces)
            val k = sin(t * 10f).coerceAtLeast(0f)
            rightPaw = Offset(16f + 14f * k, 30f - 12f * k)
            bodyRot = 4f * k; headRot = 5f * k
            pupilX = 2.5f
            tailSway = sin(t * 7f)
        }
        CatAnim.EAT -> {                          // head down in the bowl, munching
            headDrop = 15f + sin(t * 11f) * 1.6f
            headRot = sin(t * 5.5f) * 2f
            eyes = 1; mouthOpen = sin(t * 11f) > 0.2f
            tailSway = sin(t * 2f) * 0.6f
        }
        CatAnim.PURR -> {
            eyes = 1
            headRot = sin(t * 1.3f) * 5f
            sy = 1f + 0.02f * sin(t * 9f)        // tiny purr vibration
            tailSway = sin(t * 1.1f) * 0.5f
        }
        CatAnim.WALK -> {
            bodyY = -abs(sin(t * 9f)) * 2.5f
            headRot = sin(t * 9f) * 2f
            tailSway = 0.3f + sin(t * 4.5f) * 0.5f
            if ((t % 3.9f) < 0.13f) eyes = 3
        }
        CatAnim.LOOK -> {
            val c = t % 4f
            pupilX = when {
                c < 1.2f -> 0f
                c < 1.4f -> -3.5f * smooth(1.2f, 1.4f, c)
                c < 2.4f -> -3.5f
                c < 2.6f -> -3.5f + 7f * smooth(2.4f, 2.6f, c)
                c < 3.6f -> 3.5f
                else -> 3.5f * (1f - smooth(3.6f, 3.8f, c))
            }
            headRot = pupilX * 1.4f
            if ((t % 5.3f) < 0.13f) eyes = 3
        }
    }

    val chubby = look?.chubby ?: 0f
    val fluffy = look?.fluffy ?: 0f
    val tailBase = look?.tail ?: fur.base
    val tailShade = look?.tailShade ?: fur.shade
    translate(center.x, center.y) {
        scale(pop * (look?.facing ?: 1f), pop, pivot = Offset.Zero) {
            // soft shadow (stays on the ground)
            val shadowK = 1f + bodyY / 40f
            oval(0f, 45f, 52f * shadowK, 8f * shadowK, Color.Black, alpha = 0.13f)

            translate(0f, bodyY * u) {
                scale(sx, sy, pivot = p(0f, 46f)) {
                    rotate(bodyRot, pivot = p(0f, 44f)) {
                        // tail
                        val tail = Path().apply {
                            moveTo(20f * u, 34f * u)
                            quadraticTo(46f * u, 32f * u, (38f + tailSway * 8f) * u, (6f + abs(tailSway) * 3f) * u)
                        }
                        drawPath(tail, tailShade, style = line(11f + 5f * fluffy))
                        drawPath(tail, tailBase, style = line(7.5f + 5f * fluffy))

                        // body (a chubby cat gets wider)
                        val bw = 56f * (1f + 0.22f * chubby); val bh = 38f * (1f + 0.06f * chubby)
                        oval(0f, 25f, bw, bh, fur.base)
                        ovalStroke(0f, 25f, bw, bh, fur.shade, 2.2f)
                        oval(0f, 30f, 28f * (1f + 0.3f * chubby), 22f, fur.belly)
                        if (fluffy > 0.05f) {                    // fluffy chest ruff
                            for (i in -2..2) oval(i * 6f, 12f + abs(i) * 2f, 11f * fluffy + 4f, 10f * fluffy + 3f, fur.belly)
                        }
                        // feet
                        oval(-13f, 41f, 15f, 9f, fur.belly); ovalStroke(-13f, 41f, 15f, 9f, fur.shade, 1.6f)
                        if (rightPaw == null || anim == CatAnim.DANCE) {
                            oval(13f, 41f, 15f, 9f, fur.belly); ovalStroke(13f, 41f, 15f, 9f, fur.shade, 1.6f)
                        }

                        // head
                        translate(0f, headDrop * u) {
                            rotate(headRot, pivot = p(0f, 18f)) {
                                drawHead(u, fur, eyes, pupilX, tongue, mouthOpen, earTwitch, look)
                            }
                        }

                        // raised paws (drawn over the face)
                        for (paw in listOfNotNull(leftPaw, rightPaw)) {
                            oval(paw.x, paw.y, 14f, 13f, fur.base)
                            ovalStroke(paw.x, paw.y, 14f, 13f, fur.shade, 1.8f)
                            oval(paw.x, paw.y + 1.5f, 7f, 5f, Pink, alpha = 0.55f)
                        }
                    }
                }
            }

            if (zzz) {
                for (i in 0..1) {
                    val ph = ((t * 0.45f) + i * 0.5f) % 1f
                    val zx = 26f + ph * 10f; val zy = -34f - ph * 22f
                    val zs = 4f + ph * 4f
                    val z = Path().apply {
                        moveTo((zx - zs) * u, (zy - zs) * u); lineTo((zx + zs) * u, (zy - zs) * u)
                        lineTo((zx - zs) * u, (zy + zs) * u); lineTo((zx + zs) * u, (zy + zs) * u)
                    }
                    drawPath(z, Ink, alpha = (1f - ph) * 0.75f, style = line(2.4f))
                }
            }
        }
    }
}

private fun DrawScope.drawHead(
    u: Float, fur: Fur, eyes: Int, pupilX: Float, tongue: Boolean, mouthOpen: Boolean, earTwitch: Float,
    look: CatLook? = null,
) {
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun oval(cx: Float, cy: Float, w: Float, h: Float, color: Color, alpha: Float = 1f) =
        drawOval(color, topLeft = p(cx - w / 2, cy - h / 2), size = Size(w * u, h * u), alpha = alpha)
    val line = { w: Float -> Stroke(width = w * u, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val dark = fur.base.luminance() < 0.25f

    fun ear(sign: Float, rot: Float) {
        rotate(rot, pivot = p(sign * 20f, -30f)) {
            val outer = Path().apply {
                moveTo(sign * 35f * u, -16f * u); lineTo(sign * 28f * u, -49f * u); lineTo(sign * 7f * u, -34f * u); close()
            }
            drawPath(outer, look?.ears ?: fur.base)
            drawPath(outer, fur.shade, style = line(2.2f))
            val inner = Path().apply {
                moveTo(sign * 29f * u, -24f * u); lineTo(sign * 26.5f * u, -41f * u); lineTo(sign * 14f * u, -32f * u); close()
            }
            drawPath(inner, Pink, alpha = 0.7f)
        }
    }
    val sad = look?.sad ?: 0f
    ear(-1f, -26f * sad)
    ear(1f, earTwitch + 26f * sad)

    // head shape
    val headPath = Path().apply { addOval(androidx.compose.ui.geometry.Rect(p(-37f, -37f), Size(74f * u, 60f * u))) }
    drawPath(headPath, fur.base)
    clipPath(headPath) {
        fur.patches?.let { oval(-22f, -24f, 34f, 28f, it) }
        look?.patch?.let { oval(17f, -22f, 30f, 26f, it) }   // Domino's patch over one eye
        look?.cap?.let { oval(0f, -36f, 46f, 22f, it) }      // Kirsche's red cap
        if (fur.stripes) {
            for (x in listOf(-8f, 0f, 8f)) {
                drawLine(fur.shade, p(x * 0.9f, -36f), p(x * 0.7f, -26f), strokeWidth = 3.2f * u, cap = StrokeCap.Round)
            }
            drawLine(fur.shade, p(-37f, -6f), p(-29f, -5f), strokeWidth = 3f * u, cap = StrokeCap.Round)
            drawLine(fur.shade, p(37f, -6f), p(29f, -5f), strokeWidth = 3f * u, cap = StrokeCap.Round)
        }
        oval(0f, 10f, 30f, 18f, fur.belly, alpha = 0.9f) // muzzle
    }
    drawPath(headPath, fur.shade, style = line(2.2f))
    // fluffy cheeks
    val fl = look?.fluffy ?: 0f
    if (fl > 0.05f) for (sign in listOf(-1f, 1f)) {
        val tuft = Path().apply {
            moveTo(sign * 33f * u, -8f * u); lineTo(sign * (37f + 7f * fl) * u, 0f); lineTo(sign * 33f * u, 4f * u)
            lineTo(sign * (36f + 6f * fl) * u, 10f * u); lineTo(sign * 30f * u, 12f * u); close()
        }
        drawPath(tuft, fur.base)
        drawPath(tuft, fur.shade, style = line(1.6f))
    }

    // eyes
    val eyeInk = if (dark) Color(0xFFF2E27A) else Ink
    for (sign in listOf(-1f, 1f)) {
        val ex = sign * 14f; val ey = -5f
        when (eyes) {
            0 -> {
                if (dark) {
                    oval(ex, ey, 14f, 17f, eyeInk)
                    oval(ex + pupilX, ey, 7f, 13f, Ink)
                } else {
                    oval(ex + pupilX * 0.3f, ey, 12.5f, 16f, Ink)
                }
                oval(ex + 3f + pupilX, ey - 4f, 5f, 5f, Color.White)
                oval(ex - 2.5f + pupilX, ey + 3.5f, 2.4f, 2.4f, Color.White, 0.85f)
                if (sad > 0.05f) {
                    // big, glossy, watery eyes and a little tear
                    oval(ex + pupilX * 0.3f, ey + 4.5f, 11f, 4f, Color(0xFFBFE3FF), 0.75f * sad)
                    oval(ex + 1.5f + pupilX, ey + 1f, 3f, 3f, Color.White, sad)
                    val tear = Path().apply {
                        val tx = ex + sign * 6f; val ty = ey + 8f
                        moveTo(tx * u, (ty - 3f) * u)
                        quadraticTo((tx + 3f) * u, (ty + 2f) * u, tx * u, (ty + 3.5f) * u)
                        quadraticTo((tx - 3f) * u, (ty + 2f) * u, tx * u, (ty - 3f) * u)
                    }
                    drawPath(tear, Color(0xFF8FCBF5), alpha = sad)
                }
            }
            1 -> { // happy ^ ^
                val path = Path().apply {
                    moveTo((ex - 6f) * u, (ey + 2f) * u); lineTo(ex * u, (ey - 4f) * u); lineTo((ex + 6f) * u, (ey + 2f) * u)
                }
                drawPath(path, eyeInk, style = line(3.2f))
            }
            2 -> { // sleepy ‿
                val path = Path().apply {
                    moveTo((ex - 6.5f) * u, ey * u); quadraticTo(ex * u, (ey + 5f) * u, (ex + 6.5f) * u, ey * u)
                }
                drawPath(path, eyeInk, style = line(3f))
            }
            else -> drawLine(eyeInk, p(ex - 6f, ey + 1f), p(ex + 6f, ey + 1f), strokeWidth = 3f * u, cap = StrokeCap.Round)
        }
    }

    // worried brows when sad
    if (sad > 0.05f) for (sign in listOf(-1f, 1f)) {
        drawLine(if (dark) Color(0xFFD8D8E0) else Ink, p(sign * 21f, -15f), p(sign * 8f, -19.5f), strokeWidth = 2f * u, alpha = sad, cap = StrokeCap.Round)
    }
    // blush
    oval(-24f, 8f, 12f, 6.5f, Blush, alpha = 0.5f)
    oval(24f, 8f, 12f, 6.5f, Blush, alpha = 0.5f)

    // nose + mouth
    val nose = Path().apply { moveTo(-3.2f * u, 4f * u); lineTo(3.2f * u, 4f * u); lineTo(0f, 7.8f * u); close() }
    drawPath(nose, Pink)
    val mouthInk = if (dark) Color(0xFFD8D8E0) else Ink
    if (mouthOpen) {
        oval(0f, 12.5f, 7f, 6.5f, Color(0xFF8E3B4A))
        oval(0f, 14f, 4.5f, 3f, Pink)
    } else if (sad > 0.4f) {
        // a small pout
        val m = Path().apply { moveTo(-4.5f * u, 12f * u); quadraticTo(0f, 8.5f * u, 4.5f * u, 12f * u) }
        drawPath(m, mouthInk, style = line(1.8f))
    } else {
        val m = Path().apply {
            moveTo(0f, 7.8f * u); quadraticTo(-1.5f * u, 12f * u, -5.5f * u, 10f * u)
            moveTo(0f, 7.8f * u); quadraticTo(1.5f * u, 12f * u, 5.5f * u, 10f * u)
        }
        drawPath(m, mouthInk, style = line(1.8f))
    }
    if (tongue) oval(1.5f, 13.5f, 5.5f, 6f, Color(0xFFF26D85))

    // whiskers
    val wc = if (dark) Color(0xFFBFBFCB) else Ink
    for (sign in listOf(-1f, 1f)) {
        drawLine(wc, p(sign * 27f, 3f), p(sign * 41f, 0f), strokeWidth = 1.3f * u, alpha = 0.45f, cap = StrokeCap.Round)
        drawLine(wc, p(sign * 27f, 7f), p(sign * 40f, 9f), strokeWidth = 1.3f * u, alpha = 0.45f, cap = StrokeCap.Round)
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
