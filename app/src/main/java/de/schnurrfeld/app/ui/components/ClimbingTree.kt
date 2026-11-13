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
    val tx = fx(Tree.POST_X) - u * 5.5f
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
        if (isA) previewLiving(ax, ay, aw, ah, b, u, t, dark) else previewBedroom(ax, ay, aw, ah, b, u, t, dark)
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


/** What you see of the living room through the lower doorway: the fireplace in the distance, a bit of rug and chair. */
private fun DrawScope.previewLiving(ax: Float, ay: Float, aw: Float, ah: Float, b: Float, u: Float, t: Float, dark: Boolean) {
    val cx = ax + aw / 2
    drawRect(Brush.verticalGradient(listOf(Color(0xFFF4D8AE), Color(0xFFE3B67F)), ay, b), Offset(ax, ay), Size(aw, ah))
    // floor
    drawRect(Brush.verticalGradient(listOf(Color(0xFFC99A66), Color(0xFFB07F4E)), b - ah * 0.2f, b), Offset(ax, b - ah * 0.2f), Size(aw, ah * 0.2f))
    drawRect(Color(0xFFF7EBD9), Offset(ax, b - ah * 0.215f), Size(aw, ah * 0.02f))
    // fireplace
    val fw = aw * 0.46f; val ft = b - ah * 0.60f; val fb = b - ah * 0.2f
    drawRect(Color(0xFFC77D63), Offset(cx - fw / 2, ft), Size(fw, fb - ft))
    for (r in 1..5) drawLine(Color(0xFFA9604A), Offset(cx - fw / 2, ft + (fb - ft) * r / 6f), Offset(cx + fw / 2, ft + (fb - ft) * r / 6f), u * 0.25f, alpha = 0.8f)
    drawRect(Color(0xFFA9774A), Offset(cx - fw * 0.62f, ft - ah * 0.03f), Size(fw * 1.24f, ah * 0.045f))
    val ow = fw * 0.5f; val ot = ft + (fb - ft) * 0.38f
    val open = Path().apply { moveTo(cx - ow / 2, fb); lineTo(cx - ow / 2, ot + ow * 0.3f); arcTo(Rect(cx - ow / 2, ot, cx + ow / 2, ot + ow * 0.6f), 180f, 180f, false); lineTo(cx + ow / 2, fb); close() }
    drawPath(open, Color(0xFF2A1A1A))
    clipPath(open) {
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFFF8A3C).copy(alpha = 0.6f)), ot, fb), Offset(cx - ow / 2, ot), Size(ow, fb - ot))
        for (i in 0..2) {
            val fx = cx - ow * 0.28f + i * ow * 0.28f
            val fh = (fb - ot) * (0.55f + 0.1f * sin(t * 2.2f + i * 2f))
            val fl = Path().apply { moveTo(fx - ow * 0.12f, fb); quadraticTo(fx, fb - fh * 0.55f, fx + sin(t * 1.6f + i) * ow * 0.04f, fb - fh); quadraticTo(fx + ow * 0.06f, fb - fh * 0.4f, fx + ow * 0.12f, fb); close() }
            drawPath(fl, Color(0xFFFF7A2F)); drawCircle(Color(0xFFFFD66B), ow * 0.05f, Offset(fx, fb - fh * 0.25f))
        }
    }
    // a little picture over the mantel
    drawRect(Color(0xFF8A5A44), Offset(cx - aw * 0.1f, ft - ah * 0.2f), Size(aw * 0.2f, ah * 0.14f))
    drawRect(Color(0xFF9CD3E8), Offset(cx - aw * 0.085f, ft - ah * 0.185f), Size(aw * 0.17f, ah * 0.11f))
    drawOval(Color(0xFFFF8F5A), Offset(cx - aw * 0.045f, ft - ah * 0.15f), Size(aw * 0.08f, ah * 0.05f))
    // rug and the runner of the rocking chair
    drawOval(Color(0xFFE8A2B6), Offset(ax + aw * 0.05f, b - ah * 0.11f), Size(aw * 0.9f, ah * 0.2f))
    drawPath(Path().apply { moveTo(ax + aw * 0.72f, b - ah * 0.08f); quadraticTo(ax + aw * 0.85f, b - ah * 0.015f, ax + aw * 1.0f, b - ah * 0.08f) }, Color(0xFF8A5E3A), style = Stroke(u * 0.9f, cap = StrokeCap.Round))
    if (dark) drawRect(Color.Black, Offset(ax, ay), Size(aw, ah), alpha = 0.22f)
    drawRect(Brush.radialGradient(listOf(Color(0xFFFF9B4A).copy(alpha = 0.2f), Color.Transparent), Offset(cx, fb), aw * 0.8f), Offset(ax, ay), Size(aw, ah))
}

/** What you see of the bedroom through the upper doorway: the foot of the bed, the nightstand with its lamp, a window. */
private fun DrawScope.previewBedroom(ax: Float, ay: Float, aw: Float, ah: Float, b: Float, u: Float, t: Float, dark: Boolean) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFFDDE0F5), Color(0xFFC7CCEA)), ay, b), Offset(ax, ay), Size(aw, ah))
    drawRect(Brush.verticalGradient(listOf(Color(0xFFD2A878), Color(0xFFB98B5C)), b - ah * 0.2f, b), Offset(ax, b - ah * 0.2f), Size(aw, ah * 0.2f))
    drawRect(Color(0xFFF7EBD9), Offset(ax, b - ah * 0.215f), Size(aw, ah * 0.02f))
    // round window with the real sky
    val wc = Offset(ax + aw * 0.33f, ay + ah * 0.42f); val wr = aw * 0.12f
    drawCircle(Color(0xFF8A5E3A), wr + u * 0.5f, wc)
    clipPath(Path().apply { addOval(Rect(wc.x - wr, wc.y - wr, wc.x + wr, wc.y + wr)) }) { drawSky(Rect(wc.x - wr, wc.y - wr, wc.x + wr, wc.y + wr), t, 2) }
    // bed: footboard, blanket and a pillow
    val bl = ax + aw * 0.03f; val bw = aw * 0.56f
    drawRect(Color(0xFFB59BE0), Offset(bl + bw * 0.04f, b - ah * 0.46f), Size(bw * 0.92f, ah * 0.2f))
    for (i in 0..4) drawRect(Color.White, Offset(bl + bw * (0.1f + i * 0.18f), b - ah * 0.46f), Size(bw * 0.07f, ah * 0.2f), alpha = 0.25f)
    drawRoundRect(Color.White, Offset(bl + bw * 0.1f, b - ah * 0.52f), Size(bw * 0.4f, ah * 0.08f), CornerRadius(u * 1.2f))
    drawRoundRect(Color(0xFF8A5E3A), Offset(bl, b - ah * 0.3f), Size(bw, ah * 0.2f), CornerRadius(u * 0.8f))
    drawRect(Color(0xFFA9774A), Offset(bl + bw * 0.04f, b - ah * 0.27f), Size(bw * 0.92f, ah * 0.05f), alpha = 0.6f)
    // nightstand with a warm lamp
    val nx = ax + aw * 0.68f; val nw = aw * 0.27f
    drawRect(Color(0xFFA9774A), Offset(nx, b - ah * 0.34f), Size(nw, ah * 0.28f))
    drawRect(Color(0xFF8A5E3A), Offset(nx + nw * 0.1f, b - ah * 0.2f), Size(nw * 0.8f, u * 0.4f))
    val lamp = Offset(nx + nw * 0.5f, b - ah * 0.5f)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE9A8).copy(alpha = 0.55f), Color.Transparent), lamp, aw * 0.5f), aw * 0.5f, lamp)
    drawLine(Color(0xFF5B4A3B), Offset(lamp.x, lamp.y + ah * 0.04f), Offset(lamp.x, b - ah * 0.34f), u * 0.6f)
    drawPath(Path().apply { moveTo(lamp.x - nw * 0.3f, lamp.y + ah * 0.04f); lineTo(lamp.x - nw * 0.17f, lamp.y - ah * 0.06f); lineTo(lamp.x + nw * 0.17f, lamp.y - ah * 0.06f); lineTo(lamp.x + nw * 0.3f, lamp.y + ah * 0.04f); close() }, Color(0xFFFFE9B8))
    if (dark) drawRect(Color.Black, Offset(ax, ay), Size(aw, ah), alpha = 0.2f)
}
