package de.schnurrfeld.app.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.cos
import kotlin.math.sin

/** A place where the grown-up cat can stand or lie on the climbing tree. x / feetY are fractions of the scene. */
enum class Spot(val x: Float, val feetY: Float) {
    ROOF_A(0.25f, 0.485f),    // cushion on top of the lower-left cave
    ROOF_B(0.75f, 0.325f),    // cushion on top of the upper-right cave
    STEP(0.79f, 0.795f),      // low platform under the right cave
    DOOR_B(0.75f, 0.70f),     // ledge in front of the upper-right cave's doorway
}

/** Axis-aligned box in scene fractions. */
class FRect(val l: Float, val t: Float, val r: Float, val b: Float) {
    val cx get() = (l + r) / 2
    fun contains(x: Float, y: Float) = x in l..r && y in t..b
}

/**
 * The big climbing tree that replaces the basket from the teenager stage on.
 * Two caves big enough for the cat (with a doorway each), cushions on the roofs, a low step and a sisal post between.
 * Everything is described in fractions of the scene so the cat simulation and the drawing agree.
 */
object Tree {
    const val BASE_Y = 0.86f
    val boxA = FRect(0.03f, 0.50f, 0.47f, BASE_Y)
    val boxB = FRect(0.53f, 0.34f, 0.97f, 0.70f)
    const val ARCH_W = 0.31f          // of the scene width
    const val ARCH_H = 0.29f          // of the scene height
    const val DOOR_A_X = 0.25f
    const val DOOR_B_X = 0.75f
    /** where the cat stands to scratch the post (it faces the post) */
    const val SCRATCH_X = 0.36f
    /** the post between the caves */
    const val POST_X = 0.50f

    /** which cave (1 = A, 2 = B) is at this point of the scene, 0 = none */
    fun homeAt(x: Float, y: Float): Int = when {
        boxA.contains(x, y) -> 1
        boxB.contains(x, y) -> 2
        else -> 0
    }

    fun spotAt(home: Int) = if (home == 2) Spot.DOOR_B else null
    fun doorX(home: Int) = if (home == 2) DOOR_B_X else DOOR_A_X
    fun doorBottom(home: Int) = if (home == 2) boxB.b else boxA.b
}

private val SisalLight = Color(0xFFE0C28C); private val SisalDark = Color(0xFFB58C55)

/**
 * Draws the tree filling the scene (size [w] x [h]). [rake] >= 0 is the time of an ongoing scratching (sisal flecks fly).
 */
fun DrawScope.drawClimbingTree(w: Float, h: Float, t: Float, dark: Boolean, rake: Float = -1f) {
    fun fx(v: Float) = v * w
    fun fy(v: Float) = v * h
    val u = w / 100f
    val sisalLight = if (dark) Color(0xFFB59463) else SisalLight
    val sisalDark = if (dark) Color(0xFF8B6C41) else SisalDark
    val plate = if (dark) Color(0xFF6A5A48) else Color(0xFFEADAC0)
    val plateEdge = if (dark) Color(0xFF51443A) else Color(0xFFCDB48F)
    val cushion = if (dark) Color(0xFF9A5F73) else Color(0xFFF4C2CF)
    val cushionEdge = if (dark) Color(0xFF7C4A5C) else Color(0xFFE79DB3)

    // soft shadow + carpet plate
    drawOval(Color.Black, Offset(fx(0.0f), fy(0.835f)), Size(w, fy(0.085f)), alpha = if (dark) 0.22f else 0.13f)
    drawRoundRect(Brush.verticalGradient(listOf(plate, plateEdge), fy(0.85f), fy(0.885f)), Offset(fx(0.015f), fy(0.85f)), Size(fx(0.97f), fy(0.035f)), CornerRadius(u * 3f))
    drawRoundRect(plateEdge, Offset(fx(0.015f), fy(0.85f)), Size(fx(0.97f), fy(0.035f)), CornerRadius(u * 3f), style = Stroke(u * 0.5f))

    // the sisal post between the caves, up to a little perch with a toy
    val px = fx(Tree.POST_X) - u * 3.2f; val pw = u * 6.4f
    val postTop = fy(0.17f); val postBottom = fy(Tree.BASE_Y)
    drawRect(Brush.horizontalGradient(listOf(sisalDark, sisalLight, sisalLight, sisalDark), px, px + pw), Offset(px, postTop), Size(pw, postBottom - postTop))
    clipPath(Path().apply { addRect(Rect(Offset(px, postTop), Size(pw, postBottom - postTop))) }) {
        var y = postBottom + u
        while (y > postTop - u) {
            drawLine(sisalDark, Offset(px - u, y + u * 1.1f), Offset(px + pw + u, y - u * 1.1f), u * 0.7f, StrokeCap.Round, alpha = 0.75f)
            drawLine(Color.White, Offset(px - u, y + u * 0.3f), Offset(px + pw + u, y - u * 1.9f), u * 0.35f, StrokeCap.Round, alpha = 0.22f)
            y -= u * 2.3f
        }
    }
    drawRoundRect(cushion, Offset(fx(Tree.POST_X) - u * 8f, postTop - u * 2.2f), Size(u * 16f, u * 3.4f), CornerRadius(u * 1.7f))
    drawRoundRect(cushionEdge, Offset(fx(Tree.POST_X) - u * 8f, postTop - u * 2.2f), Size(u * 16f, u * 3.4f), CornerRadius(u * 1.7f), style = Stroke(u * 0.4f))
    // toy on a string
    val sway = sin(t * 1.9f) * u * 1.6f
    val tx = fx(Tree.POST_X) + u * 5.5f
    val tEnd = Offset(tx + sway, postTop + u * 14f)
    drawLine(if (dark) Color(0xFFBDB4C4) else Color(0xFF8A7F99), Offset(tx, postTop + u * 1f), tEnd, u * 0.4f, StrokeCap.Round)
    drawCircle(Color(0xFFF28CA0), u * 2.8f, Offset(tEnd.x, tEnd.y + u * 2.2f))
    drawCircle(Color.White, u * 0.8f, Offset(tEnd.x - u * 1f, tEnd.y + u * 1.2f), alpha = 0.7f)
    for (a in listOf(-35f, 0f, 35f)) {
        val r = Math.toRadians((a + 90).toDouble()).toFloat()
        drawLine(Color(0xFFFFD36B), Offset(tEnd.x, tEnd.y + u * 4.4f), Offset(tEnd.x + cos(r) * u * 3.4f, tEnd.y + u * 4.4f + sin(r) * u * 3.4f), u * 0.6f, StrokeCap.Round)
    }

    // low step under the right cave
    val stepY = fy(Spot.STEP.feetY)
    drawRoundRect(if (dark) Color(0xFF557F6E) else Color(0xFFBFE0CC), Offset(fx(0.54f), stepY), Size(fx(0.43f), fy(0.03f)), CornerRadius(u * 1.5f))
    drawRoundRect(if (dark) Color(0xFF3E6354) else Color(0xFF8FBEA5), Offset(fx(0.54f), stepY), Size(fx(0.43f), fy(0.03f)), CornerRadius(u * 1.5f), style = Stroke(u * 0.4f))
    drawRoundRect(cushion, Offset(fx(0.58f), stepY - fy(0.012f)), Size(fx(0.35f), fy(0.02f)), CornerRadius(u * 1.5f))
    drawRect(sisalLight, Offset(fx(0.915f), stepY + fy(0.03f)), Size(u * 3.4f, postBottom - stepY - fy(0.03f)))
    for (k in 0..6) drawLine(sisalDark, Offset(fx(0.915f), stepY + fy(0.04f) + k * fy(0.0125f)), Offset(fx(0.915f) + u * 3.4f, stepY + fy(0.035f) + k * fy(0.0125f)), u * 0.4f, alpha = 0.7f)

    // the two caves
    cave(Tree.boxA, w, h, u, dark, body = if (dark) Color(0xFF7A5E49) else Color(0xFFF6DDBB), edge = if (dark) Color(0xFF5E4636) else Color(0xFFD8A878), cushion, cushionEdge, true, t)
    cave(Tree.boxB, w, h, u, dark, body = if (dark) Color(0xFF4D7566) else Color(0xFFCFE7DA), edge = if (dark) Color(0xFF3A5C4F) else Color(0xFF92C3AA), cushion, cushionEdge, false, t)

    // sisal flecks flying while the cat scratches
    if (rake >= 0f) for (i in 0..4) {
        val ph = ((rake * 1.6f) + i * 0.21f) % 1f
        val fxp = fx(Tree.POST_X) - u * 3.2f - ph * u * 4f
        val fyp = fy(0.62f) - i * fy(0.025f) - ph * u * 1f + (ph * ph) * u * 5f
        drawLine(sisalDark, Offset(fxp, fyp), Offset(fxp - u * 1.2f, fyp + u * 0.5f), u * 0.5f, StrokeCap.Round, alpha = 1f - ph)
    }
}

/** One cave: box with a doorway the cat fits through, a lit room behind it and a cushion on the roof. */
private fun DrawScope.cave(box: FRect, w: Float, h: Float, u: Float, dark: Boolean, body: Color, edge: Color, cushion: Color, cushionEdge: Color, isA: Boolean, t: Float) {
    val l = box.l * w; val tp = box.t * h; val r = box.r * w; val b = box.b * h
    val corner = CornerRadius(u * 3f)
    drawRoundRect(Brush.verticalGradient(listOf(body, edge.copy(alpha = 0.35f).compositeOverSafe(body)), tp, b), Offset(l, tp), Size(r - l, b - tp), corner)
    drawRoundRect(edge, Offset(l, tp), Size(r - l, b - tp), corner, style = Stroke(u * 0.6f))
    // soft vertical panel lines
    for (k in 1..5) { val x = l + (r - l) * k / 6f; drawLine(edge, Offset(x, tp + u * 2f), Offset(x, b - u * 1.2f), u * 0.3f, alpha = 0.22f) }
    // the doorway
    val aw = Tree.ARCH_W * w; val ah = Tree.ARCH_H * h
    val cx = (l + r) / 2; val ax = cx - aw / 2; val ay = b - ah
    val arch = Path().apply {
        moveTo(ax, b); lineTo(ax, ay + aw / 2)
        arcTo(Rect(ax, ay, ax + aw, ay + aw), 180f, 180f, false)
        lineTo(ax + aw, b); close()
    }
    drawPath(arch, Brush.radialGradient(listOf(Color(0xFFFFD08A), Color(0xFFC98A52), Color(0xFF4A2F35)), Offset(cx, b - ah * 0.2f), aw * 0.95f))
    clipPath(arch) {
        // a hint of the home behind the door: back wall line, a tiny picture, a warm lamp
        drawRect(Color(0xFF3B2A2A), Offset(ax, b - ah * 0.17f), Size(aw, ah * 0.17f), alpha = 0.55f)
        drawRoundRect(Color(0xFFF5E6C8), Offset(cx - aw * 0.27f, ay + ah * 0.30f), Size(aw * 0.17f, aw * 0.14f), CornerRadius(u * 0.5f), alpha = 0.85f)
        drawRoundRect(Color(0xFF8A5A44), Offset(cx - aw * 0.27f, ay + ah * 0.30f), Size(aw * 0.17f, aw * 0.14f), CornerRadius(u * 0.5f), style = Stroke(u * 0.35f))
        drawCircle(Color(0xFFFFF3C4), aw * 0.07f, Offset(cx + aw * 0.22f, ay + ah * 0.42f), alpha = 0.9f)
        drawLine(Color(0xFF5A3A3A), Offset(cx + aw * 0.22f, ay + ah * 0.46f), Offset(cx + aw * 0.22f, b - ah * 0.12f), u * 0.5f)
        if (isA) drawOval(Color(0xFFFF9B4A), Offset(cx - aw * 0.12f, b - ah * 0.27f), Size(aw * 0.24f, ah * 0.16f), alpha = 0.55f + 0.2f * sin(t * 7f))   // fire glow
    }
    drawPath(arch, edge, style = Stroke(u * 0.9f))
    // the upper cave has a ledge in front of its door (the lower one stands on the floor)
    if (!isA) {
        drawRoundRect(body, Offset(cx - aw * 0.62f, b), Size(aw * 1.24f, h * 0.026f), CornerRadius(u * 1.4f))
        drawRoundRect(edge, Offset(cx - aw * 0.62f, b), Size(aw * 1.24f, h * 0.026f), CornerRadius(u * 1.4f), style = Stroke(u * 0.4f))
        drawRect(edge, Offset(cx - aw * 0.5f, b + h * 0.026f), Size(u * 1.1f, h * 0.012f))
        drawRect(edge, Offset(cx + aw * 0.5f - u * 1.1f, b + h * 0.026f), Size(u * 1.1f, h * 0.012f))
    }
    // door mat
    drawRoundRect(cushion, Offset(cx - aw * 0.42f, b - u * 1.2f), Size(aw * 0.84f, u * 2.4f), CornerRadius(u * 1.2f))
    // cushion on the roof (this is where the cat lies)
    val rt = tp - h * 0.03f
    drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = if (dark) 0.15f else 0.5f).compositeOverSafe(cushion), cushion), rt, tp + h * 0.005f), Offset(l - u * 1.4f, rt), Size(r - l + u * 2.8f, tp + h * 0.005f - rt), CornerRadius(u * 2.2f))
    drawRoundRect(cushionEdge, Offset(l - u * 1.4f, rt), Size(r - l + u * 2.8f, tp + h * 0.005f - rt), CornerRadius(u * 2.2f), style = Stroke(u * 0.4f))
    for (i in 0..8) drawLine(cushionEdge, Offset(l + (r - l) * (i + 0.5f) / 9f, rt + (tp - rt) * 0.55f), Offset(l + (r - l) * (i + 0.5f) / 9f + u * 1.3f, rt + (tp - rt) * 0.55f), u * 0.4f, StrokeCap.Round, alpha = 0.7f)
}

private fun Color.compositeOverSafe(bg: Color): Color {
    val a = alpha + bg.alpha * (1f - alpha)
    if (a <= 0f) return Color.Transparent
    return Color((red * alpha + bg.red * bg.alpha * (1f - alpha)) / a, (green * alpha + bg.green * bg.alpha * (1f - alpha)) / a, (blue * alpha + bg.blue * bg.alpha * (1f - alpha)) / a, a)
}

/** A tail sticking out of a doorway (the cat is at home and cannot resist), swaying a little. */
fun DrawScope.drawTailPeek(w: Float, h: Float, home: Int, base: Color, shade: Color, t: Float) {
    val u = w / 100f
    val bx = Tree.doorX(home) * w - Tree.ARCH_W * w * 0.12f
    val by = Tree.doorBottom(home) * h - u * 0.6f
    val sway = sin(t * 2.3f)
    val tail = Path().apply {
        moveTo(bx, by - u * 1f)
        cubicTo(bx - u * 5f, by - u * 3f, bx - u * 8f + sway * u * 2f, by - u * 9f, bx - u * 4f + sway * u * 3f, by - u * 15f - u * 2f * (1f + sway) / 2f)
    }
    drawPath(tail, shade, style = Stroke(u * 4.4f, cap = StrokeCap.Round))
    drawPath(tail, base, style = Stroke(u * 3.1f, cap = StrokeCap.Round))
}
