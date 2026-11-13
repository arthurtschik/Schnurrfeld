package de.schnurrfeld.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import de.schnurrfeld.app.data.Breed
import de.schnurrfeld.app.data.Care
import de.schnurrfeld.app.data.TailColor
import de.schnurrfeld.app.ui.theme.Fur
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Outline = Color(0xFF3B3340)
private fun stroke(w: Float) = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round)

// ---------------------------------------------------------------- breeds

private val Ginger = Color(0xFFEE9A4D); private val GingerShade = Color(0xFFC0702E)
private val White = Color(0xFFFFFBF5); private val WhiteShade = Color(0xFFD9D2C7)
private val Black = Color(0xFF3B3B46); private val BlackShade = Color(0xFF22222A)

fun tailColorOf(t: TailColor) = when (t) {
    TailColor.RED -> Ginger to GingerShade
    TailColor.BLACK -> Black to BlackShade
    TailColor.WHITE -> White to WhiteShade
}

fun furOf(b: Breed): Fur = when (b) {
    Breed.MOPPEL -> Fur(Ginger, GingerShade, Color(0xFFFFE3C4), stripes = true)
    Breed.KIRSCHE -> Fur(White, WhiteShade, Color(0xFFFFFFFF))
    Breed.MITTERNACHT -> Fur(Black, BlackShade, Color(0xFF66667A))
    Breed.DOMINO -> Fur(White, WhiteShade, Color(0xFFFFFFFF))
    Breed.KARAMELL -> Fur(Color(0xFFD9A66C), Color(0xFFAE7B45), Color(0xFFFBE7CC))
    Breed.WOLKE -> Fur(White, Color(0xFFD6D3E0), Color(0xFFFFFFFF))
}

/**
 * Breed details. [growth] 0..1 says how far the characteristic traits are developed
 * (they grow with each stage until the cat is fully grown).
 */
fun lookOf(b: Breed, growth: Float, facing: Float = 1f): CatLook {
    val g = growth.coerceIn(0f, 1f)
    val (tail, tailShade) = tailColorOf(b.tail)
    return when (b) {
        Breed.MOPPEL -> CatLook(tail, tailShade, chubby = 0.25f + 0.75f * g, facing = facing)
        Breed.KIRSCHE -> CatLook(tail, tailShade, ears = androidx.compose.ui.graphics.lerp(White, Ginger, 0.45f + 0.55f * g),
            cap = Ginger.copy(alpha = 0.35f + 0.65f * g), facing = facing)
        Breed.MITTERNACHT -> CatLook(tail, tailShade, facing = facing)
        Breed.DOMINO -> CatLook(tail, tailShade, patch = Black.copy(alpha = 0.25f + 0.75f * g), facing = facing)
        Breed.KARAMELL -> CatLook(tail, tailShade, fluffy = 0.2f * g, facing = facing)
        Breed.WOLKE -> CatLook(tail, tailShade, fluffy = 0.3f + 0.7f * g, facing = facing)
    }
}

/** A breed portrait, e.g. for the "Wer steckt im Körbchen?" intro. */
@Composable
fun BreedPortrait(breed: Breed, size: Dp, anim: CatAnim = CatAnim.WAG, phase: Float = 0f, modifier: Modifier = Modifier) {
    val frame = rememberFrameMillis()
    Canvas(modifier.size(size)) {
        drawCat(Offset(this.size.width / 2, this.size.height / 2), this.size.width, furOf(breed), anim, frame.value / 1000f + phase, look = lookOf(breed, 1f))
    }
}

// ---------------------------------------------------------------- care item icons

fun DrawScope.drawCareIcon(care: Care, c: Offset, s: Float) {
    val u = s / 100f
    fun p(x: Float, y: Float) = Offset(c.x + x * u, c.y + y * u)
    val w = 4f * u
    when (care) {
        Care.FOOD -> drawFoodTin(c, s)
        Care.MILK -> { // a shallow bowl of milk
            val bowl = Path().apply {
                moveTo(p(-42f, -4f).x, p(-42f, -4f).y)
                quadraticTo(p(-38f, 34f).x, p(-38f, 34f).y, p(0f, 34f).x, p(0f, 34f).y)
                quadraticTo(p(38f, 34f).x, p(38f, 34f).y, p(42f, -4f).x, p(42f, -4f).y); close()
            }
            drawPath(bowl, Color(0xFF7FB4E8)); drawPath(bowl, Outline, style = stroke(w))
            drawOval(Color(0xFF5E97D1), p(-42f, -14f), Size(84f * u, 20f * u))
            drawOval(Color(0xFFFFFDF7), p(-36f, -11f), Size(72f * u, 14f * u))
            drawOval(Outline, p(-42f, -14f), Size(84f * u, 20f * u), style = stroke(w))
            for (x in listOf(-22f, 0f, 22f)) drawCircle(Color.White, 4f * u, p(x, 18f), alpha = 0.7f) // dots on the bowl
            drawOval(Color.White, p(-20f, -9f), Size(14f * u, 4f * u), alpha = 0.9f)
        }
        Care.BRUSH -> drawSlickerBrush(c, s, -32f)
        Care.YARN -> { // ball of yarn with a loose thread
            drawCircle(Color(0xFFF08BB0), 34f * u, p(0f, 2f))
            clipPath(Path().apply { addOval(Rect(p(0f, 2f), 34f * u)) }) {
                for (i in -3..3) drawArc(Color(0xFFD9638F), -40f, 260f, false, p(-60f + i * 14f, -30f), Size(70f * u, 70f * u), style = stroke(w * 0.8f))
                for (i in -2..2) drawLine(Color(0xFFFFB8D2), p(-40f, -20f + i * 14f), p(40f, 10f + i * 14f), w * 0.7f, StrokeCap.Round)
            }
            drawCircle(Outline, 34f * u, p(0f, 2f), style = stroke(w))
            val thread = Path().apply {
                moveTo(p(26f, 22f).x, p(26f, 22f).y)
                cubicTo(p(44f, 30f).x, p(44f, 30f).y, p(36f, 44f).x, p(36f, 44f).y, p(46f, 44f).x, p(46f, 44f).y)
            }
            drawPath(thread, Color(0xFFD9638F), style = stroke(w))
        }
    }
}

@Composable
fun CareIcon(care: Care, size: Dp, modifier: Modifier = Modifier) =
    Canvas(modifier.size(size)) { drawCareIcon(care, Offset(this.size.width / 2, this.size.height / 2), this.size.width) }

// ---------------------------------------------------------------- Taler

fun DrawScope.drawTaler(c: Offset, r: Float, spin: Float = 1f) {
    val w = r * spin.coerceIn(0.08f, 1f)
    drawOval(Color(0xFFB7821A), Offset(c.x - w, c.y - r + r * 0.08f), Size(2 * w, 2 * r))
    drawOval(Brush.radialGradient(listOf(Color(0xFFFFE68A), Color(0xFFF5B83D)), c, r), Offset(c.x - w, c.y - r), Size(2 * w, 2 * r))
    drawOval(Color(0xFFC98E1E), Offset(c.x - w * 0.78f, c.y - r * 0.78f), Size(1.56f * w, 1.56f * r), style = Stroke(r * 0.08f))
    if (spin > 0.5f) { // paw print
        val k = spin
        drawOval(Color(0xFFC98E1E), Offset(c.x - r * 0.26f * k, c.y - r * 0.02f), Size(r * 0.52f * k, r * 0.4f))
        for ((dx, dy) in listOf(-0.32f to -0.18f, -0.11f to -0.36f, 0.11f to -0.36f, 0.32f to -0.18f))
            drawOval(Color(0xFFC98E1E), Offset(c.x + dx * r * k - r * 0.09f * k, c.y + dy * r - r * 0.1f), Size(r * 0.18f * k, r * 0.2f))
    }
    drawOval(Color.White, Offset(c.x - w * 0.55f, c.y - r * 0.7f), Size(w * 0.35f, r * 0.22f), alpha = 0.7f)
}

@Composable
fun TalerIcon(size: Dp, modifier: Modifier = Modifier) =
    Canvas(modifier.size(size)) { drawTaler(Offset(this.size.width / 2, this.size.height / 2), this.size.width * 0.46f) }

// ---------------------------------------------------------------- Joker: cat head with a jester's cap

fun DrawScope.drawJokerCat(c: Offset, s: Float) {
    // a cute cat head wearing a jester's cap: one point stands up, the other one flops down with its bobble
    val u = s / 100f
    fun p(x: Float, y: Float) = Offset(c.x + x * u, c.y + y * u)
    val w = 3.6f * u
    val fur = Color(0xFFF7B06A); val furShade = Color(0xFFD08840); val cream = Color(0xFFFFEBD3)
    val purple = Color(0xFF8E5BD8); val purpleDark = Color(0xFF6B3FB0)
    val yellow = Color(0xFFF5C23D); val yellowDark = Color(0xFFD29A1A)

    // floppy point (right): goes up, then bends down beside the head – drawn behind the head
    val flop = Path().apply {
        moveTo(p(4f, -16f).x, p(4f, -16f).y)
        cubicTo(p(22f, -48f).x, p(22f, -48f).y, p(48f, -44f).x, p(48f, -44f).y, p(46f, -14f).x, p(46f, -14f).y)
        lineTo(p(38f, -16f).x, p(38f, -16f).y)
        cubicTo(p(38f, -34f).x, p(38f, -34f).y, p(26f, -34f).x, p(26f, -34f).y, p(22f, -14f).x, p(22f, -14f).y)
        close()
    }
    drawPath(flop, Brush.linearGradient(listOf(yellow, yellowDark), p(10f, -40f), p(46f, -10f)))
    drawPath(flop, Outline, style = stroke(w))
    // head
    drawOval(fur, p(-36f, -14f), Size(72f * u, 60f * u))
    drawOval(cream, p(-17f, 12f), Size(34f * u, 24f * u))           // muzzle
    drawOval(Outline, p(-36f, -14f), Size(72f * u, 60f * u), style = stroke(w))
    // standing point (left), curling to the left
    val up = Path().apply {
        moveTo(p(-30f, -10f).x, p(-30f, -10f).y)
        cubicTo(p(-38f, -36f).x, p(-38f, -36f).y, p(-30f, -52f).x, p(-30f, -52f).y, p(-42f, -56f).x, p(-42f, -56f).y)
        cubicTo(p(-18f, -56f).x, p(-18f, -56f).y, p(-6f, -34f).x, p(-6f, -34f).y, p(4f, -16f).x, p(4f, -16f).y)
        close()
    }
    drawPath(up, Brush.linearGradient(listOf(Color(0xFFAE85F0), purpleDark), p(-40f, -56f), p(0f, -12f)))
    drawPath(up, Outline, style = stroke(w))
    // cap band with little diamonds
    val band = Path().apply {
        moveTo(p(-34f, -6f).x, p(-34f, -6f).y)
        quadraticTo(p(0f, -20f).x, p(0f, -20f).y, p(34f, -6f).x, p(34f, -6f).y)
        lineTo(p(32f, 2f).x, p(32f, 2f).y)
        quadraticTo(p(0f, -11f).x, p(0f, -11f).y, p(-32f, 2f).x, p(-32f, 2f).y); close()
    }
    drawPath(band, Color(0xFFE5484D)); drawPath(band, Outline, style = stroke(w * 0.85f))
    for (x in listOf(-18f, 0f, 18f)) {
        val y = if (x == 0f) -10f else -6.5f
        val d = Path().apply { moveTo(p(x, y - 3.5f).x, p(x, y - 3.5f).y); lineTo(p(x + 3f, y).x, p(x + 3f, y).y); lineTo(p(x, y + 3.5f).x, p(x, y + 3.5f).y); lineTo(p(x - 3f, y).x, p(x - 3f, y).y); close() }
        drawPath(d, yellow)
    }
    // bobbles: one on top of the standing point, one dangling from the floppy one
    drawCircle(yellow, 6.5f * u, p(-43f, -56f)); drawCircle(Outline, 6.5f * u, p(-43f, -56f), style = stroke(w * 0.85f))
    drawLine(Outline, p(42f, -14f), p(42f, -6f), w * 0.8f, StrokeCap.Round)
    drawCircle(purple, 6.5f * u, p(42f, -1f)); drawCircle(Outline, 6.5f * u, p(42f, -1f), style = stroke(w * 0.85f))
    drawCircle(Color.White, 2f * u, p(40f, -3f), alpha = 0.7f)
    // big sparkly eyes
    for (sx in listOf(-1f, 1f)) {
        drawOval(Outline, p(sx * 13f - 6f, 4f), Size(12f * u, 15f * u))
        drawCircle(Color.White, 2.6f * u, p(sx * 13f + 1.8f, 7.5f))
        drawCircle(Color.White, 1.3f * u, p(sx * 13f - 2.5f, 14f), alpha = 0.85f)
    }
    // blush, nose, ω mouth
    drawOval(Color(0xFFFF9EB0), p(-31f, 17f), Size(11f * u, 6f * u), alpha = 0.7f)
    drawOval(Color(0xFFFF9EB0), p(20f, 17f), Size(11f * u, 6f * u), alpha = 0.7f)
    val nose = Path().apply { moveTo(p(-3f, 16f).x, p(-3f, 16f).y); lineTo(p(3f, 16f).x, p(3f, 16f).y); lineTo(p(0f, 19.5f).x, p(0f, 19.5f).y); close() }
    drawPath(nose, Color(0xFFF28CA0))
    val mouth = Path().apply {
        moveTo(p(0f, 19.5f).x, p(0f, 19.5f).y); quadraticTo(p(-2f, 24f).x, p(-2f, 24f).y, p(-6f, 22f).x, p(-6f, 22f).y)
        moveTo(p(0f, 19.5f).x, p(0f, 19.5f).y); quadraticTo(p(2f, 24f).x, p(2f, 24f).y, p(6f, 22f).x, p(6f, 22f).y)
    }
    drawPath(mouth, Outline, style = stroke(w * 0.6f))
    // whiskers
    for (sx in listOf(-1f, 1f)) for (dy in listOf(15f, 20f)) drawLine(furShade, p(sx * 24f, dy), p(sx * 38f, dy - 2f + (dy - 15f) * 0.5f), w * 0.45f, StrokeCap.Round)
}

@Composable
fun JokerIcon(size: Dp, modifier: Modifier = Modifier) =
    Canvas(modifier.size(size)) { drawJokerCat(Offset(this.size.width * 0.48f, this.size.height * 0.6f), this.size.width * 0.8f) }

@Composable
fun MachineIcon(size: Dp, modifier: Modifier = Modifier) =
    Canvas(modifier.size(size)) { drawMachine(Offset(this.size.width / 2, this.size.height * 0.56f), this.size.width * 0.82f, 0f, 0f, 0f) }

/** Small basket icon for the menu. */
@Composable
fun BasketIcon(size: Dp, modifier: Modifier = Modifier) = Canvas(modifier.size(size)) {
    val c = Offset(this.size.width / 2, this.size.height * 0.5f)
    val w = this.size.width * 0.95f
    drawBedBack(c, w)
    drawBedBlanket(c, w, 0f, 0.5f, 0f)
    drawBedFront(c, w)
    drawTailOverRim(c, w, 0f, Color(0xFFEE9A4D), Color(0xFFC0702E))
}

// ================================================================ new art (1.2.1)

private fun hGrad(cols: List<Color>, x0: Float, x1: Float) = Brush.horizontalGradient(cols, x0, x1)
private fun vGrad(cols: List<Color>, y0: Float, y1: Float) = Brush.verticalGradient(cols, y0, y1)

/** Cat food tin: metallic body, coral label with a fish, open pâté top and the peeled-back lid with ring pull. */
fun DrawScope.drawFoodTin(c: Offset, s: Float) {
    val u = s / 100f
    fun p(x: Float, y: Float) = Offset(c.x + x * u, c.y + y * u)
    val w = 3.2f * u
    val metal = listOf(Color(0xFF8F9AA7), Color(0xFFE9EEF3), Color(0xFFC5CDD6), Color(0xFF8A95A2))
    // peeled-back lid (behind the tin)
    rotate(-28f, p(24f, -14f)) {
        drawOval(hGrad(metal, p(-8f, 0f).x, p(56f, 0f).x), p(-8f, -34f), Size(64f * u, 18f * u))
        drawOval(Outline, p(-8f, -34f), Size(64f * u, 18f * u), style = stroke(w))
        drawOval(Color(0xFFB9C2CC), p(2f, -30f), Size(44f * u, 10f * u), style = stroke(w * 0.6f))
    }
    // body
    val body = Path().apply {
        moveTo(p(-36f, -10f).x, p(-36f, -10f).y); lineTo(p(-36f, 28f).x, p(-36f, 28f).y)
        arcTo(Rect(p(-36f, 20f), Size(72f * u, 18f * u)), 180f, -180f, false)
        lineTo(p(36f, -10f).x, p(36f, -10f).y); close()
    }
    drawPath(body, hGrad(metal, p(-36f, 0f).x, p(36f, 0f).x))
    // label
    clipPath(body) {
        drawRect(hGrad(listOf(Color(0xFFD95F4E), Color(0xFFFF8E77), Color(0xFFF2735E), Color(0xFFC9513F)), p(-36f, 0f).x, p(36f, 0f).x),
            p(-40f, -2f), Size(80f * u, 30f * u))
        // wavy white trims
        for (yy in listOf(-1f, 27f)) {
            val wave = Path().apply {
                moveTo(p(-40f, yy).x, p(-40f, yy).y)
                var x = -40f
                while (x < 40f) { quadraticTo(p(x + 4f, yy - 2.5f).x, p(x + 4f, yy - 2.5f).y, p(x + 8f, yy).x, p(x + 8f, yy).y); x += 8f }
            }
            drawPath(wave, Color.White, style = stroke(w * 0.7f))
        }
        // fish
        val fish = Path().apply {
            moveTo(p(-15f, 13f).x, p(-15f, 13f).y)
            quadraticTo(p(-1f, 1f).x, p(-1f, 1f).y, p(11f, 13f).x, p(11f, 13f).y)
            quadraticTo(p(-1f, 25f).x, p(-1f, 25f).y, p(-15f, 13f).x, p(-15f, 13f).y)
            moveTo(p(10f, 13f).x, p(10f, 13f).y); lineTo(p(19f, 6f).x, p(19f, 6f).y); lineTo(p(19f, 20f).x, p(19f, 20f).y); close()
        }
        drawPath(fish, Color.White)
        drawCircle(Outline, 1.9f * u, p(-8f, 11f))
        drawArc(Color(0xFFFFB3A6), 300f, 120f, false, p(-4f, 7f), Size(10f * u, 12f * u), style = stroke(w * 0.5f))
        for ((bx, by, br) in listOf(Triple(-27f, 8f, 2.2f), Triple(-24f, 17f, 1.6f), Triple(27f, 10f, 1.8f))) drawCircle(Color.White, br * u, p(bx, by), alpha = 0.8f)
    }
    drawPath(body, Outline, style = stroke(w))
    // open top with pâté
    drawOval(hGrad(metal, p(-36f, 0f).x, p(36f, 0f).x), p(-36f, -19f), Size(72f * u, 18f * u))
    drawOval(Brush.radialGradient(listOf(Color(0xFFC88655), Color(0xFF8E5430)), p(-4f, -12f), 34f * u), p(-31f, -17f), Size(62f * u, 13f * u))
    for ((x, y) in listOf(-14f to -12f, -2f to -14f, 10f to -11f, 0f to -9f, -22f to -10f, 18f to -13f)) drawCircle(Color(0xFF7A4526), 3f * u, p(x, y))
    drawOval(Color.White, p(-12f, -15.5f), Size(14f * u, 3f * u), alpha = 0.55f)
    drawOval(Outline, p(-36f, -19f), Size(72f * u, 18f * u), style = stroke(w))
    // ring pull on the lid
    drawCircle(Color(0xFFDCE3EA), 5f * u, p(38f, -40f)); drawCircle(Outline, 5f * u, p(38f, -40f), style = stroke(w * 0.7f))
    // shine on the tin
    drawLine(Color.White, p(-26f, -4f), p(-26f, 28f), 2.5f * u, StrokeCap.Round, alpha = 0.6f)
}

/** Pink slicker brush with pins and a wooden handle. */
fun DrawScope.drawSlickerBrush(c: Offset, s: Float, angle: Float) {
    val u = s / 100f
    fun p(x: Float, y: Float) = Offset(c.x + x * u, c.y + y * u)
    val w = 3.2f * u
    rotate(angle, c) {
        // handle
        val handle = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(p(-9f, 0f).x, p(-9f, 0f).y, p(9f, 50f).x, p(9f, 50f).y, CornerRadius(9f * u))) }
        drawPath(handle, hGrad(listOf(Color(0xFFB97A45), Color(0xFFF0C18D), Color(0xFFC88A52)), p(-9f, 0f).x, p(9f, 0f).x))
        drawPath(handle, Outline, style = stroke(w))
        drawCircle(Color(0xFF7A4E2A), 3f * u, p(0f, 42f))
        // heart sticker
        val heart = Path().apply {
            val hc = p(0f, 22f); val r = 5f * u
            moveTo(hc.x, hc.y + r * 0.85f)
            cubicTo(hc.x - r * 1.35f, hc.y - r * 0.1f, hc.x - r * 0.65f, hc.y - r * 1.15f, hc.x, hc.y - r * 0.35f)
            cubicTo(hc.x + r * 0.65f, hc.y - r * 1.15f, hc.x + r * 1.35f, hc.y - r * 0.1f, hc.x, hc.y + r * 0.85f); close()
        }
        drawPath(heart, Color(0xFFFF6F91))
        // head
        val head = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(p(-33f, -50f).x, p(-33f, -50f).y, p(33f, 4f).x, p(33f, 4f).y, CornerRadius(26f * u))) }
        drawPath(head, vGrad(listOf(Color(0xFFFFB5CB), Color(0xFFE77FA1)), p(0f, -50f).y, p(0f, 4f).y))
        drawPath(head, Outline, style = stroke(w))
        val pad = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(p(-25f, -42f).x, p(-25f, -42f).y, p(25f, -4f).x, p(25f, -4f).y, CornerRadius(18f * u))) }
        drawPath(pad, Color(0xFFFFE6EE))
        clipPath(pad) {
            for (row in 0..4) for (col in -3..3) {
                val pin = p(col * 7.4f + (row % 2) * 3.7f, -37f + row * 7.5f)
                drawCircle(Color(0xFFB8BEC8), 2.3f * u, pin)
                drawCircle(Color.White, 1.1f * u, Offset(pin.x - 0.6f * u, pin.y - 0.6f * u))
            }
        }
        drawPath(pad, Color(0xFFE77FA1), style = stroke(w * 0.6f))
        drawArc(Color.White, 200f, 70f, false, p(-30f, -48f), Size(30f * u, 30f * u), style = stroke(w), alpha = 0.7f)
    }
}

// ---------------------------------------------------------------- the cat bed (wicker basket)

private val Wicker = Color(0xFFD6A06A); private val WickerDark = Color(0xFFA36A3A); private val WickerDeep = Color(0xFF7E4F2A)

/** Point on the rim ellipse at angle [a] (radians, 0 = right, PI/2 = front). */
private fun rimPoint(c: Offset, rx: Float, ry: Float, a: Float) = Offset(c.x + kotlin.math.cos(a) * rx, c.y + sin(a) * ry)

/** A braided rim segment from angle a0 to a1. */
private fun DrawScope.braid(c: Offset, rx: Float, ry: Float, thick: Float, a0: Float, a1: Float) {
    val steps = 64
    val path = Path()
    for (i in 0..steps) {
        val a = a0 + (a1 - a0) * i / steps
        val pt = rimPoint(c, rx, ry, a)
        if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
    }
    drawPath(path, WickerDark, style = Stroke(thick * 1.15f, cap = StrokeCap.Round))
    drawPath(path, Wicker, style = Stroke(thick, cap = StrokeCap.Round))
    // braid: short slanted strands along the rim
    val n = 26
    for (i in 0 until n) {
        val a = a0 + (a1 - a0) * (i + 0.5f) / n
        val pt = rimPoint(c, rx, ry, a)
        val tang = Offset(-sin(a) * rx, kotlin.math.cos(a) * ry).let { val l = kotlin.math.sqrt(it.x * it.x + it.y * it.y); Offset(it.x / l, it.y / l) }
        val norm = Offset(-tang.y, tang.x)
        val d = thick * 0.42f
        val k = if (i % 2 == 0) 1f else -1f
        drawLine(WickerDark, pt + norm * d + tang * (d * 0.7f * k), pt - norm * d - tang * (d * 0.7f * k), thick * 0.18f, StrokeCap.Round, alpha = 0.7f)
    }
    drawPath(path, Color.White, style = Stroke(thick * 0.14f, cap = StrokeCap.Round), alpha = 0.25f)
}

/** Back of the basket: inner wall, plush cushion and the back half of the braided rim. */
fun DrawScope.drawBedBack(c: Offset, w: Float) {
    val rx = w * 0.46f; val ry = w * 0.15f
    // inner wall
    drawOval(vGrad(listOf(WickerDeep, WickerDark), c.y - ry, c.y + ry), Offset(c.x - rx, c.y - ry), Size(2 * rx, 2 * ry))
    // plush cushion
    val crx = rx * 0.84f; val cry = ry * 0.74f
    val cc = Offset(c.x, c.y + ry * 0.12f)
    drawOval(Brush.radialGradient(listOf(Color(0xFFFFF6EE), Color(0xFFF2D4C4)), Offset(cc.x - crx * 0.2f, cc.y - cry * 0.3f), crx), Offset(cc.x - crx, cc.y - cry), Size(2 * crx, 2 * cry))
    drawOval(Color(0xFFE3B9A6), Offset(cc.x - crx * 0.86f, cc.y - cry * 0.8f), Size(crx * 1.72f, cry * 1.6f), style = Stroke(w * 0.006f), alpha = 0.6f)
    braid(c, rx, ry, w * 0.075f, PI.toFloat(), 2 * PI.toFloat())
}

/**
 * Knitted blanket inside the basket. [lump] raises it where the bundle sleeps, [wiggle] makes it jiggle.
 */
fun DrawScope.drawBedBlanket(c: Offset, w: Float, t: Float, lump: Float, wiggle: Float) {
    val rx = w * 0.46f; val ry = w * 0.15f
    val breathe = sin(t * 2.2f) * w * 0.004f
    val jig = sin(t * 23f) * wiggle * w * 0.01f
    val mound = w * (0.02f + 0.075f * lump) + breathe
    val bl = Path().apply {
        moveTo(c.x - rx * 0.92f, c.y + ry * 0.2f)
        cubicTo(c.x - rx * 0.8f, c.y - ry * 0.7f, c.x - rx * 0.35f + jig, c.y - ry * 0.45f - mound, c.x + jig, c.y - ry * 0.4f - mound)
        cubicTo(c.x + rx * 0.4f - jig, c.y - ry * 0.35f - mound, c.x + rx * 0.82f, c.y - ry * 0.65f, c.x + rx * 0.92f, c.y + ry * 0.2f)
        lineTo(c.x + rx * 0.9f, c.y + ry * 0.7f); lineTo(c.x - rx * 0.9f, c.y + ry * 0.7f); close()
    }
    drawPath(bl, Brush.radialGradient(listOf(Color(0xFFFFE2D6), Color(0xFFF2B6A6), Color(0xFFDE9384)), Offset(c.x - rx * 0.15f, c.y - ry * 0.6f - mound), rx * 1.05f))
    clipPath(bl) {
        // knit: rows of little "v" stitches, plus a cable stripe
        val rows = 7; val cols = 22
        for (r in 0 until rows) for (k in 0 until cols) {
            val x = c.x - rx + (k + 0.5f) * 2 * rx / cols
            val y = c.y - ry * 0.9f - mound * 0.6f + r * (ry * 0.5f) + sin(k * 0.6f) * w * 0.002f
            val v = w * 0.009f
            drawLine(Color.White, Offset(x - v, y - v), Offset(x, y), w * 0.004f, StrokeCap.Round, alpha = 0.35f)
            drawLine(Color.White, Offset(x, y), Offset(x + v, y - v), w * 0.004f, StrokeCap.Round, alpha = 0.35f)
        }
        for (k in -1..1) drawLine(Color(0xFFFFF0E8), Offset(c.x + k * rx * 0.45f, c.y - ry * 2f), Offset(c.x + k * rx * 0.45f + w * 0.02f, c.y + ry * 2f), w * 0.012f, alpha = 0.35f)
    }
    drawPath(bl, Color(0xFFC77D6E), style = Stroke(w * 0.005f), alpha = 0.6f)
}

/** Front of the basket: woven body and the front half of the braided rim. */
fun DrawScope.drawBedFront(c: Offset, w: Float) {
    val rx = w * 0.46f; val ry = w * 0.15f
    val depth = w * 0.17f
    // soft shadow on the floor
    drawOval(Color.Black, Offset(c.x - rx * 1.02f, c.y + ry + depth * 0.55f), Size(rx * 2.04f, ry * 1.1f), alpha = 0.12f)
    val body = Path().apply {
        moveTo(c.x - rx, c.y)
        // the body goes down from the rim and bulges a little (basket shape)
        cubicTo(c.x - rx * 1.02f, c.y + ry + depth * 0.9f, c.x - rx * 0.55f, c.y + ry + depth * 1.08f, c.x, c.y + ry + depth * 1.08f)
        cubicTo(c.x + rx * 0.55f, c.y + ry + depth * 1.08f, c.x + rx * 1.02f, c.y + ry + depth * 0.9f, c.x + rx, c.y)
        // follow the front rim back
        val steps = 32
        for (i in 0..steps) { val a = 0f + PI.toFloat() * i / steps; val pt = rimPoint(c, rx, ry, a); lineTo(pt.x, pt.y) }
        close()
    }
    drawPath(body, vGrad(listOf(Wicker, Color(0xFFC08650), WickerDark), c.y, c.y + ry + depth * 1.1f))
    clipPath(body) {
        // basket weave: staggered rounded "bricks"
        val rowH = depth * 0.2f
        var row = 0
        var y = c.y + ry * 0.3f
        while (y < c.y + ry + depth * 1.2f) {
            val brickW = w * 0.075f
            var x = c.x - rx - (if (row % 2 == 0) 0f else brickW / 2)
            while (x < c.x + rx) {
                drawRoundRect(Color(0xFFE2B07C), Offset(x + w * 0.004f, y + rowH * 0.12f), Size(brickW - w * 0.008f, rowH * 0.76f), CornerRadius(rowH * 0.38f), alpha = 0.55f)
                x += brickW
            }
            drawLine(WickerDeep, Offset(c.x - rx, y), Offset(c.x + rx, y), w * 0.003f, alpha = 0.35f)
            y += rowH; row++
        }
        // soft shading on the sides
        drawRect(hGrad(listOf(Color(0x55000000), Color(0x00000000), Color(0x00000000), Color(0x55000000)), c.x - rx, c.x + rx), Offset(c.x - rx, c.y), Size(2 * rx, ry + depth * 1.3f))
    }
    drawPath(body, WickerDeep, style = Stroke(w * 0.006f))
    braid(c, rx, ry, w * 0.08f, 0f, PI.toFloat())
}

/** Stage 1+: the tail comes out from under the blanket at the side and hangs over the front rim. */
fun DrawScope.drawTailOverRim(c: Offset, w: Float, t: Float, base: Color, shade: Color, wag: Float = 1f) {
    val rx = w * 0.46f; val ry = w * 0.15f
    val start = rimPoint(c, rx * 0.86f, ry * 0.86f, 0.55f)
    val sway = sin(t * 3f) * wag
    val tail = Path().apply {
        moveTo(start.x - w * 0.04f, start.y - w * 0.015f)
        quadraticTo(start.x + w * 0.03f, start.y - w * 0.02f, start.x + w * 0.05f, start.y + w * 0.03f)
        quadraticTo(start.x + w * (0.07f + 0.03f * sway), start.y + w * 0.1f, start.x + w * (0.04f + 0.05f * sway), start.y + w * 0.13f)
    }
    drawPath(tail, shade, style = Stroke(w * 0.055f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(tail, base, style = Stroke(w * 0.038f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Stage 2+: a little paw reaches out from under the blanket's edge and rests on the front-left rim. */
fun DrawScope.drawPawOverRim(c: Offset, w: Float, out: Float, base: Color, shade: Color) {
    if (out <= 0f) return
    val rx = w * 0.46f; val ry = w * 0.15f
    val rim = rimPoint(c, rx, ry, 2.25f)
    val from = Offset(rim.x + w * 0.03f, rim.y - w * 0.04f)
    val to = Offset(rim.x - w * 0.005f, rim.y - w * 0.012f + (1 - out) * w * 0.03f)
    drawLine(shade, from, to, w * 0.06f, StrokeCap.Round)
    drawLine(base, from, to, w * 0.045f, StrokeCap.Round)
    val pw = w * 0.075f; val ph = w * 0.055f
    drawOval(base, Offset(to.x - pw / 2, to.y - ph / 2), Size(pw, ph))
    drawOval(shade, Offset(to.x - pw / 2, to.y - ph / 2), Size(pw, ph), style = Stroke(w * 0.004f))
    for (i in -1..1) drawCircle(Color(0xFFF28CA0), w * 0.007f, Offset(to.x + i * pw * 0.26f, to.y - ph * 0.12f), alpha = 0.9f)
}

// ---------------------------------------------------------------- bowls, yarn, side-view cat

/** Food bowl on the floor; [level] 1 = full, 0 = licked clean. */
fun DrawScope.drawFoodBowl(c: Offset, w: Float, level: Float) {
    val h = w * 0.36f
    val bowl = Path().apply {
        moveTo(c.x - w / 2, c.y - h * 0.4f)
        lineTo(c.x - w * 0.38f, c.y + h * 0.45f); lineTo(c.x + w * 0.38f, c.y + h * 0.45f); lineTo(c.x + w / 2, c.y - h * 0.4f); close()
    }
    drawPath(bowl, vGrad(listOf(Color(0xFFF2836B), Color(0xFFD25F49)), c.y - h * 0.4f, c.y + h * 0.45f))
    drawPath(bowl, Outline, style = stroke(w * 0.025f))
    // paw print on the bowl
    drawOval(Color.White, Offset(c.x - w * 0.06f, c.y + h * 0.02f), Size(w * 0.12f, w * 0.09f), alpha = 0.85f)
    for (dx in listOf(-0.07f, -0.025f, 0.025f, 0.07f)) drawCircle(Color.White, w * 0.022f, Offset(c.x + dx * w, c.y - h * 0.12f), alpha = 0.85f)
    drawOval(Color(0xFFB54A37), Offset(c.x - w / 2, c.y - h * 0.62f), Size(w, h * 0.44f))
    if (level > 0.02f) {
        val fw = w * 0.86f * (0.55f + 0.45f * level)
        drawOval(Color(0xFF9A5A33), Offset(c.x - fw / 2, c.y - h * (0.5f + 0.25f * level)), Size(fw, h * 0.32f))
        for (i in 0 until (12 * level).toInt()) {
            val a = i * 2.39f; val r = fw * 0.32f * ((i % 4) / 4f + 0.2f)
            drawCircle(Color(0xFF7A4526), w * 0.03f, Offset(c.x + kotlin.math.cos(a) * r, c.y - h * (0.36f + 0.25f * level) + sin(a) * r * 0.25f))
        }
    }
    drawOval(Outline, Offset(c.x - w / 2, c.y - h * 0.62f), Size(w, h * 0.44f), style = stroke(w * 0.025f))
}

/** Milk bowl on the floor; [level] 1 = full, 0 = empty. */
fun DrawScope.drawMilkBowl(c: Offset, w: Float, level: Float) {
    val h = w * 0.34f
    val bowl = Path().apply {
        moveTo(c.x - w / 2, c.y - h * 0.4f)
        quadraticTo(c.x - w * 0.46f, c.y + h * 0.5f, c.x, c.y + h * 0.5f)
        quadraticTo(c.x + w * 0.46f, c.y + h * 0.5f, c.x + w / 2, c.y - h * 0.4f); close()
    }
    drawPath(bowl, vGrad(listOf(Color(0xFF8CC0F0), Color(0xFF5E97D1)), c.y - h * 0.4f, c.y + h * 0.5f))
    drawPath(bowl, Outline, style = stroke(w * 0.025f))
    for (x in listOf(-0.22f, 0f, 0.22f)) drawCircle(Color.White, w * 0.03f, Offset(c.x + x * w, c.y + h * 0.15f), alpha = 0.7f)
    drawOval(Color(0xFF4F86C0), Offset(c.x - w / 2, c.y - h * 0.62f), Size(w, h * 0.44f))
    if (level > 0.02f) {
        val mw = w * 0.88f * (0.6f + 0.4f * level)
        drawOval(Color(0xFFFFFDF7), Offset(c.x - mw / 2, c.y - h * (0.52f + 0.1f * level)), Size(mw, h * 0.3f))
        drawOval(Color.White, Offset(c.x - mw * 0.25f, c.y - h * (0.48f + 0.1f * level)), Size(mw * 0.2f, h * 0.06f), alpha = 0.9f)
    }
    drawOval(Outline, Offset(c.x - w / 2, c.y - h * 0.62f), Size(w, h * 0.44f), style = stroke(w * 0.025f))
}

/** Yarn ball rolling on the floor ([spin] in degrees) with a thread trailing behind. */
fun DrawScope.drawYarnBall(c: Offset, r: Float, spin: Float, trail: Float) {
    val thread = Path().apply {
        moveTo(c.x - r * 0.6f, c.y + r * 0.85f)
        cubicTo(c.x - r * 1.6f, c.y + r * 1.1f, c.x - r * 2.2f, c.y + r * 0.6f, c.x - r * (2.5f + trail), c.y + r)
    }
    drawPath(thread, Color(0xFFD9638F), style = stroke(r * 0.12f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFB0CB), Color(0xFFE77BA0)), Offset(c.x - r * 0.3f, c.y - r * 0.3f), r * 1.2f), r, c)
    rotate(spin, c) {
        clipPath(Path().apply { addOval(Rect(c, r)) }) {
            for (i in -3..3) drawArc(Color(0xFFCB5584), -40f, 260f, false, Offset(c.x - r * 1.8f + i * r * 0.42f, c.y - r * 0.9f), Size(r * 2.1f, r * 2.1f), style = stroke(r * 0.07f))
            for (i in -2..2) drawLine(Color(0xFFFFCADB), Offset(c.x - r * 1.2f, c.y - r * 0.6f + i * r * 0.42f), Offset(c.x + r * 1.2f, c.y + r * 0.3f + i * r * 0.42f), r * 0.06f, StrokeCap.Round)
        }
    }
    drawCircle(Outline, r, c, style = stroke(r * 0.08f))
}

/**
 * The cat seen from the side, sitting with closed eyes (for brushing). Faces left; [facing] = -1 mirrors.
 * [purr] adds a tiny vibration.
 */
fun DrawScope.drawCatSide(center: Offset, size: Float, fur: Fur, look: CatLook?, t: Float, facing: Float = 1f) {
    val u = size / 100f
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    fun oval(cx: Float, cy: Float, w: Float, h: Float, col: Color) = drawOval(col, p(cx - w / 2, cy - h / 2), Size(w * u, h * u))
    fun ovalS(cx: Float, cy: Float, w: Float, h: Float, col: Color, sw: Float) = drawOval(col, p(cx - w / 2, cy - h / 2), Size(w * u, h * u), style = stroke(sw * u))
    val tailC = look?.tail ?: fur.base; val tailS = look?.tailShade ?: fur.shade
    val chubby = look?.chubby ?: 0f; val fluffy = look?.fluffy ?: 0f
    val vib = sin(t * 40f) * 0.25f
    translate(center.x, center.y + vib * u) {
        scale(facing, 1f, pivot = Offset.Zero) {
            drawOval(Color.Black, p(-30f, 41f), Size(64f * u, 8f * u), alpha = 0.13f)
            // tail curled around the feet
            val tail = Path().apply {
                moveTo(p(22f, 34f).x, p(22f, 34f).y)
                cubicTo(p(40f, 46f).x, p(40f, 46f).y, p(0f, 50f).x, p(0f, 50f).y, p(-24f, 43f).x, p(-24f, 43f).y)
            }
            drawPath(tail, tailS, style = stroke((10f + 5f * fluffy) * u)); drawPath(tail, tailC, style = stroke((7f + 5f * fluffy) * u))
            // body + haunch
            val bw = 48f * (1f + 0.2f * chubby)
            oval(6f, 18f, bw, 50f, fur.base); ovalS(6f, 18f, bw, 50f, fur.shade, 2.2f)
            oval(16f, 30f, 34f, 26f, fur.base); ovalS(16f, 30f, 34f, 26f, fur.shade, 1.8f)
            oval(-6f, 20f, 18f, 30f, fur.belly)
            if (fluffy > 0.05f) for (i in 0..3) oval(-10f + i * 2f, 4f + i * 6f, 10f * fluffy + 4f, 9f * fluffy + 4f, fur.belly)
            // front legs
            for (dx in listOf(-14f, -6f)) {
                drawLine(fur.shade, p(dx, 26f), p(dx, 41f), 9.5f * u, StrokeCap.Round)
                drawLine(fur.base, p(dx, 26f), p(dx, 41f), 7.5f * u, StrokeCap.Round)
            }
            oval(-15f, 42f, 11f, 6f, fur.belly); oval(-7f, 42f, 11f, 6f, fur.belly)
            // head (profile)
            val hx = -16f; val hy = -16f
            // back ear
            val earB = Path().apply { moveTo(p(hx + 6f, hy - 18f).x, p(hx + 6f, hy - 18f).y); lineTo(p(hx + 14f, hy - 38f).x, p(hx + 14f, hy - 38f).y); lineTo(p(hx + 20f, hy - 14f).x, p(hx + 20f, hy - 14f).y); close() }
            drawPath(earB, look?.ears ?: fur.shade)
            oval(hx, hy, 50f, 44f, fur.base); ovalS(hx, hy, 50f, 44f, fur.shade, 2.2f)
            look?.cap?.let { drawOval(it, p(hx - 14f, hy - 22f), Size(30f * u, 14f * u)) }
            look?.patch?.let { drawOval(it, p(hx - 14f, hy - 14f), Size(18f * u, 14f * u)) }
            oval(hx - 22f, hy + 6f, 16f, 13f, fur.belly)           // muzzle
            drawCircle(Color(0xFFF28CA0), 2.6f * u, p(hx - 29f, hy + 1f))
            // front ear
            val earF = Path().apply { moveTo(p(hx - 10f, hy - 17f).x, p(hx - 10f, hy - 17f).y); lineTo(p(hx - 4f, hy - 40f).x, p(hx - 4f, hy - 40f).y); lineTo(p(hx + 8f, hy - 19f).x, p(hx + 8f, hy - 19f).y); close() }
            drawPath(earF, look?.ears ?: fur.base); drawPath(earF, fur.shade, style = stroke(2f * u))
            val inner = Path().apply { moveTo(p(hx - 6f, hy - 20f).x, p(hx - 6f, hy - 20f).y); lineTo(p(hx - 3.5f, hy - 33f).x, p(hx - 3.5f, hy - 33f).y); lineTo(p(hx + 3f, hy - 21f).x, p(hx + 3f, hy - 21f).y); close() }
            drawPath(inner, Color(0xFFF28CA0), alpha = 0.7f)
            // closed, happy eye + blush + smile
            val eye = Path().apply { moveTo(p(hx - 16f, hy - 3f).x, p(hx - 16f, hy - 3f).y); quadraticTo(p(hx - 11f, hy + 2f).x, p(hx - 11f, hy + 2f).y, p(hx - 6f, hy - 3f).x, p(hx - 6f, hy - 3f).y) }
            drawPath(eye, if (fur.base.red + fur.base.green < 0.6f) Color(0xFFF2E27A) else Outline, style = stroke(2.8f * u))
            drawOval(Color(0xFFFF9EB0), p(hx - 14f, hy + 5f), Size(10f * u, 5.5f * u), alpha = 0.55f)
            val mouth = Path().apply { moveTo(p(hx - 27f, hy + 9f).x, p(hx - 27f, hy + 9f).y); quadraticTo(p(hx - 24f, hy + 12f).x, p(hx - 24f, hy + 12f).y, p(hx - 21f, hy + 9f).x, p(hx - 21f, hy + 9f).y) }
            drawPath(mouth, Outline, style = stroke(1.6f * u))
            for (dy in listOf(4f, 8f)) drawLine(Outline, p(hx - 26f, hy + dy), p(hx - 40f, hy + dy - 2f + (dy - 4f) * 0.6f), 1.2f * u, alpha = 0.45f)
        }
    }
}

// ---------------------------------------------------------------- Taler-Automat (gumball machine), the pretty version

/**
 * The coin machine. [crank] 0..1 = one full turn of the handle, [shake] wobbles the coins in the globe.
 */
fun DrawScope.drawMachine(c: Offset, s: Float, crank: Float, shake: Float, t: Float) {
    val u = s / 100f
    fun p(x: Float, y: Float) = Offset(c.x + x * u, c.y + y * u)
    val w = 2.6f * u
    val red = listOf(Color(0xFFFF8A73), Color(0xFFE8604F), Color(0xFFB83E31))
    val chrome = listOf(Color(0xFF8C96A3), Color(0xFFF4F7FA), Color(0xFFB5BEC9), Color(0xFF7D8794))
    val gold = listOf(Color(0xFFFFEA9A), Color(0xFFF5B83D), Color(0xFFC98E1E))

    // feet + plinth
    for (x in listOf(-30f, 22f)) drawRoundRect(Color(0xFF7A2E25), p(x, 50f), Size(8f * u, 6f * u), CornerRadius(2f * u))
    val plinth = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(p(-38f, 44f).x, p(-38f, 44f).y, p(38f, 52f).x, p(38f, 52f).y, CornerRadius(4f * u))) }
    drawPath(plinth, hGrad(chrome, p(-38f, 0f).x, p(38f, 0f).x)); drawPath(plinth, Outline, style = stroke(w))
    // body: rounded, slightly flared
    val body = Path().apply {
        moveTo(p(-24f, 2f).x, p(-24f, 2f).y); lineTo(p(24f, 2f).x, p(24f, 2f).y)
        quadraticTo(p(30f, 4f).x, p(30f, 4f).y, p(33f, 44f).x, p(33f, 44f).y)
        lineTo(p(-33f, 44f).x, p(-33f, 44f).y)
        quadraticTo(p(-30f, 4f).x, p(-30f, 4f).y, p(-24f, 2f).x, p(-24f, 2f).y); close()
    }
    drawPath(body, hGrad(red, p(-33f, 0f).x, p(33f, 0f).x))
    clipPath(body) { drawRect(Color.White, p(-22f, 0f), Size(5f * u, 44f * u), alpha = 0.22f) }  // gloss stripe
    drawPath(body, Outline, style = stroke(w))
    // gold trim + "TALER" plaque with a paw
    drawLine(Color(0xFFF5B83D), p(-31f, 38f), p(31f, 38f), 2.2f * u)
    val plaque = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(p(-14f, 5f).x, p(-14f, 5f).y, p(14f, 12f).x, p(14f, 12f).y, CornerRadius(3f * u))) }
    drawPath(plaque, hGrad(gold, p(-14f, 0f).x, p(14f, 0f).x)); drawPath(plaque, Outline, style = stroke(w * 0.6f))
    for ((i, x) in listOf(-9f, -5f, -1f, 3f, 7f).withIndex()) drawCircle(Color(0xFF9A6A12), (if (i == 2) 1.6f else 1.1f) * u, p(x + 1f, 8.5f))
    // coin slot plate (chrome) with slot
    val plate = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(p(-21f, 15f).x, p(-21f, 15f).y, p(-1f, 30f).x, p(-1f, 30f).y, CornerRadius(4f * u))) }
    drawPath(plate, hGrad(chrome, p(-21f, 0f).x, p(-1f, 0f).x)); drawPath(plate, Outline, style = stroke(w * 0.8f))
    drawRoundRect(Color(0xFF2B2A33), p(-14f, 20.5f), Size(6f * u, 4f * u), CornerRadius(1.5f * u))
    drawRoundRect(Color(0xFF2B2A33), p(-15f, 21.5f), Size(8f * u, 2f * u), CornerRadius(1f * u))
    // crank: golden disc with a lever and a round grip
    val hub = p(14f, 22f)
    drawCircle(hGrad(gold, hub.x - 9f * u, hub.x + 9f * u), 9f * u, hub); drawCircle(Outline, 9f * u, hub, style = stroke(w * 0.8f))
    drawCircle(Color(0xFFFFF4C2), 3f * u, Offset(hub.x - 3f * u, hub.y - 3f * u), alpha = 0.8f)
    val a = crank * 2f * PI.toFloat() - PI.toFloat() / 2
    val knob = Offset(hub.x + kotlin.math.cos(a) * 12f * u, hub.y + sin(a) * 12f * u)
    drawLine(Outline, hub, knob, 3.6f * u, StrokeCap.Round)
    drawLine(Color(0xFFDCE3EA), hub, knob, 2.2f * u, StrokeCap.Round)
    drawCircle(Color(0xFFE8604F), 3.8f * u, knob); drawCircle(Outline, 3.8f * u, knob, style = stroke(w * 0.6f))
    drawCircle(Color.White, 1.2f * u, Offset(knob.x - 1.2f * u, knob.y - 1.2f * u), alpha = 0.8f)
    // chute: dark arch with a chrome flap
    val chute = Path().apply {
        moveTo(p(-12f, 42f).x, p(-12f, 42f).y); lineTo(p(-12f, 35f).x, p(-12f, 35f).y)
        quadraticTo(p(0f, 26f).x, p(0f, 26f).y, p(12f, 35f).x, p(12f, 35f).y); lineTo(p(12f, 42f).x, p(12f, 42f).y); close()
    }
    drawPath(chute, Color(0xFF3A1611)); drawPath(chute, Outline, style = stroke(w * 0.8f))
    drawRoundRect(hGrad(chrome, p(-11f, 0f).x, p(11f, 0f).x), p(-11f, 31f), Size(22f * u, 4f * u), CornerRadius(2f * u), alpha = 0.95f)
    // chrome collar
    val collar = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(p(-19f, -4f).x, p(-19f, -4f).y, p(19f, 4f).x, p(19f, 4f).y, CornerRadius(3f * u))) }
    drawPath(collar, hGrad(chrome, p(-19f, 0f).x, p(19f, 0f).x)); drawPath(collar, Outline, style = stroke(w * 0.8f))
    // glass globe full of Taler
    val g = p(0f, -30f); val gr = 31f * u
    clipPath(Path().apply { addOval(Rect(g, gr)) }) {
        drawCircle(Brush.radialGradient(listOf(Color(0x22FFFFFF), Color(0x44A9D8F5)), g, gr), gr, g)
        var k = 0
        for (row in 0..5) for (col in -4..4) {
            val jx = sin(t * 30f + k) * shake * 2.5f * u
            val jy = kotlin.math.cos(t * 27f + k * 1.7f) * shake * 2.5f * u
            val x = g.x + col * 8.6f * u + (row % 2) * 4.3f * u + jx
            val y = g.y + gr - 5f * u - row * 7.2f * u + jy
            drawTaler(Offset(x, y), 4.9f * u, if (k % 3 == 0) 0.5f else 1f)
            k++
        }
        drawCircle(Color(0x33264B66), gr, g, style = Stroke(gr * 0.12f))   // inner glass shade
    }
    drawCircle(Color(0xFF7FA9C9), gr, g, style = stroke(w))
    drawArc(Color.White, 195f, 55f, false, Offset(g.x - gr * 0.78f, g.y - gr * 0.78f), Size(gr * 1.56f, gr * 1.56f), alpha = 0.85f, style = stroke(3.2f * u))
    drawCircle(Color.White, 2.4f * u, Offset(g.x + gr * 0.45f, g.y - gr * 0.5f), alpha = 0.7f)
    // cap: red dome with chrome knob
    val cap = Path().apply {
        moveTo(p(-13f, -59f).x, p(-13f, -59f).y)
        quadraticTo(p(0f, -71f).x, p(0f, -71f).y, p(13f, -59f).x, p(13f, -59f).y); close()
    }
    drawPath(cap, hGrad(red, p(-13f, 0f).x, p(13f, 0f).x)); drawPath(cap, Outline, style = stroke(w * 0.8f))
    drawCircle(hGrad(chrome, p(-3f, 0f).x, p(3f, 0f).x), 3.2f * u, p(0f, -69f)); drawCircle(Outline, 3.2f * u, p(0f, -69f), style = stroke(w * 0.6f))
}
