package de.schnurrfeld.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.sin

/**
 * The little helper ("Mietze Schlau"): an upright standing, extra-chibi cat with a cheeky, sly look,
 * lilac-grey fur and a red neckerchief. She points with her left paw (towards a speech bubble).
 * [c] is the centre of a box of size s × 1.3s.
 */
fun DrawScope.drawHelperCat(c: Offset, s: Float, t: Float) {
    val u = s / 100f
    fun p(x: Float, y: Float) = Offset(c.x + x * u, c.y + y * u)
    val line = Color(0xFF3B3340)
    fun st(w: Float) = Stroke(w * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun oval(cx: Float, cy: Float, w: Float, h: Float, col: Color, alpha: Float = 1f) = drawOval(col, p(cx - w / 2, cy - h / 2), Size(w * u, h * u), alpha = alpha)
    fun ovalS(cx: Float, cy: Float, w: Float, h: Float, sw: Float) = drawOval(line, p(cx - w / 2, cy - h / 2), Size(w * u, h * u), style = st(sw))
    val fur = Color(0xFFBDB4D2); val shade = Color(0xFF9188AA); val cream = Color(0xFFF5F0FA); val pink = Color(0xFFF28CA0)

    val bob = sin(t * 3f) * 1.5f
    val wink = (t % 3.4f) < 0.35f

    // shadow
    oval(0f, 63f, 46f, 7f, Color.Black, 0.12f)
    // tail, curling up behind on the right
    val sway = sin(t * 2.4f) * 4f
    val tail = Path().apply {
        moveTo(p(12f, 44f).x, p(12f, 44f).y)
        cubicTo(p(36f, 46f).x, p(36f, 46f).y, p(40f, 20f + sway).x, p(40f, 20f + sway).y, p(30f, 12f + sway).x, p(30f, 12f + sway).y)
    }
    drawPath(tail, shade, style = st(10f)); drawPath(tail, fur, style = st(7f))

    // feet + body (small, chibi)
    oval(-9f, 60f, 15f, 8f, cream); ovalS(-9f, 60f, 15f, 8f, 1.8f)
    oval(9f, 60f, 15f, 8f, cream); ovalS(9f, 60f, 15f, 8f, 1.8f)
    oval(0f, 42f + bob * 0.3f, 38f, 38f, fur); ovalS(0f, 42f + bob * 0.3f, 38f, 38f, 2.2f)
    oval(0f, 46f + bob * 0.3f, 20f, 22f, cream)
    // right arm on the hip
    val hip = Path().apply { moveTo(p(14f, 32f).x, p(14f, 32f).y); quadraticTo(p(24f, 38f).x, p(24f, 38f).y, p(16f, 46f).x, p(16f, 46f).y) }
    drawPath(hip, shade, style = st(9f)); drawPath(hip, fur, style = st(6.5f))

    // big head
    val hy = -12f + bob
    // ears (big)
    for (sx in listOf(-1f, 1f)) {
        val ear = Path().apply {
            moveTo(p(sx * 36f, hy - 2f).x, p(sx * 36f, hy - 2f).y); lineTo(p(sx * 34f, hy - 40f).x, p(sx * 34f, hy - 40f).y); lineTo(p(sx * 10f, hy - 26f).x, p(sx * 10f, hy - 26f).y); close()
        }
        drawPath(ear, fur); drawPath(ear, line, style = st(2.2f))
        val inner = Path().apply {
            moveTo(p(sx * 31f, hy - 8f).x, p(sx * 31f, hy - 8f).y); lineTo(p(sx * 31f, hy - 32f).x, p(sx * 31f, hy - 32f).y); lineTo(p(sx * 16f, hy - 24f).x, p(sx * 16f, hy - 24f).y); close()
        }
        drawPath(inner, pink, alpha = 0.7f)
    }
    oval(0f, hy, 84f, 68f, fur)
    // a little tuft on top
    val tuft = Path().apply { moveTo(p(-6f, hy - 32f).x, p(-6f, hy - 32f).y); quadraticTo(p(0f, hy - 44f).x, p(0f, hy - 44f).y, p(4f, hy - 33f).x, p(4f, hy - 33f).y); quadraticTo(p(8f, hy - 40f).x, p(8f, hy - 40f).y, p(10f, hy - 31f).x, p(10f, hy - 31f).y) }
    drawPath(tuft, fur); drawPath(tuft, line, style = st(1.8f))
    oval(0f, hy + 16f, 36f, 22f, cream)                         // muzzle
    ovalS(0f, hy, 84f, 68f, 2.4f)

    // sly, half-lidded eyes (right one winks now and then) + one raised brow
    for (sx in listOf(-1f, 1f)) {
        val ex = sx * 17f; val ey = hy + 2f
        if (sx > 0 && wink) {
            val w = Path().apply { moveTo(p(ex - 7f, ey + 1f).x, p(ex - 7f, ey + 1f).y); quadraticTo(p(ex, ey - 5f).x, p(ex, ey - 5f).y, p(ex + 7f, ey + 1f).x, p(ex + 7f, ey + 1f).y) }
            drawPath(w, line, style = st(3f))
        } else {
            oval(ex, ey, 15f, 17f, line)
            oval(ex + 3f, ey - 2f, 5f, 5f, Color.White)
            // heavy upper lid -> cheeky look
            val lid = Path().apply { moveTo(p(ex - 9f, ey - 10f).x, p(ex - 9f, ey - 10f).y); lineTo(p(ex + 9f, ey - 10f).x, p(ex + 9f, ey - 10f).y); lineTo(p(ex + 9f, ey - 1f).x, p(ex + 9f, ey - 1f).y); lineTo(p(ex - 9f, ey - 3f).x, p(ex - 9f, ey - 3f).y); close() }
            drawPath(lid, fur)
            drawLine(line, p(ex - 8f, ey - 2.5f), p(ex + 8f, ey - 1f), 2.6f * u, StrokeCap.Round)
        }
    }
    val brow = Path().apply { moveTo(p(-24f, hy - 12f).x, p(-24f, hy - 12f).y); quadraticTo(p(-17f, hy - 18f).x, p(-17f, hy - 18f).y, p(-10f, hy - 14f).x, p(-10f, hy - 14f).y) }
    drawPath(brow, line, style = st(2.2f))
    // blush, nose, smirk with a tiny fang
    oval(-27f, hy + 13f, 12f, 6f, Color(0xFFFF9EB0), 0.6f); oval(27f, hy + 13f, 12f, 6f, Color(0xFFFF9EB0), 0.6f)
    val nose = Path().apply { moveTo(p(-3f, hy + 10f).x, p(-3f, hy + 10f).y); lineTo(p(3f, hy + 10f).x, p(3f, hy + 10f).y); lineTo(p(0f, hy + 13.5f).x, p(0f, hy + 13.5f).y); close() }
    drawPath(nose, pink)
    val smirk = Path().apply { moveTo(p(-7f, hy + 17f).x, p(-7f, hy + 17f).y); quadraticTo(p(1f, hy + 21f).x, p(1f, hy + 21f).y, p(9f, hy + 14f).x, p(9f, hy + 14f).y) }
    drawPath(smirk, line, style = st(2f))
    val fang = Path().apply { moveTo(p(3f, hy + 18.6f).x, p(3f, hy + 18.6f).y); lineTo(p(5.5f, hy + 22f).x, p(5.5f, hy + 22f).y); lineTo(p(6.5f, hy + 17.6f).x, p(6.5f, hy + 17.6f).y); close() }
    drawPath(fang, Color.White); drawPath(fang, line, style = st(1f))
    for (sx in listOf(-1f, 1f)) for (dy in listOf(12f, 17f)) drawLine(line, p(sx * 30f, hy + dy), p(sx * 44f, hy + dy - 3f + (dy - 12f) * 0.6f), 1.2f * u, alpha = 0.45f)

    // red neckerchief
    val scarf = Path().apply {
        moveTo(p(-18f, 20f + bob).x, p(-18f, 20f + bob).y); quadraticTo(p(0f, 28f + bob).x, p(0f, 28f + bob).y, p(18f, 20f + bob).x, p(18f, 20f + bob).y)
        lineTo(p(4f, 36f + bob).x, p(4f, 36f + bob).y); lineTo(p(-4f, 36f + bob).x, p(-4f, 36f + bob).y); close()
    }
    drawPath(scarf, Color(0xFFE5484D)); drawPath(scarf, line, style = st(1.8f))
    for ((dx, dy) in listOf(-6f to 26f, 6f to 26f, 0f to 31f)) drawCircle(Color.White, 1.3f * u, p(dx, dy + bob), alpha = 0.85f)

    // left arm raised, pointing (wiggles a little)
    val wig = sin(t * 7f) * 3f
    rotate(wig, p(-14f, 34f)) {
        val arm = Path().apply { moveTo(p(-14f, 34f + bob).x, p(-14f, 34f + bob).y); quadraticTo(p(-28f, 30f).x, p(-28f, 30f).y, p(-38f, 20f).x, p(-38f, 20f).y) }
        drawPath(arm, shade, style = st(9f)); drawPath(arm, fur, style = st(6.5f))
        oval(-40f, 18f, 11f, 10f, cream); ovalS(-40f, 18f, 11f, 10f, 1.8f)
    }
}
