package de.schnurrfeld.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.schnurrfeld.app.App
import de.schnurrfeld.app.data.Breed
import de.schnurrfeld.app.data.Pet
import de.schnurrfeld.app.ui.components.CatAnim
import de.schnurrfeld.app.ui.components.TopBar
import de.schnurrfeld.app.ui.components.drawCat
import de.schnurrfeld.app.ui.components.furOf
import de.schnurrfeld.app.ui.components.lookOf
import de.schnurrfeld.app.ui.components.rememberFrameMillis
import de.schnurrfeld.app.ui.theme.LocalDark
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * The room behind one of the doorways of the climbing tree. It is far bigger inside than outside:
 * the cat has its own little home with a fireplace, a rocking chair, pictures on the walls …
 * [home] 1 = living room (lower left cave), 2 = bedroom (upper right cave).
 */
@Composable
fun RoomOverlay(app: App, pet: Pet, sim: PetSim, home: Int, onClose: () -> Unit) {
    val frame = rememberFrameMillis()
    val dark = LocalDark.current
    val breed = pet.breed ?: Breed.WOLKE
    val fur = furOf(breed)
    BackHandler(onBack = onClose)
    val catHere by remember { derivedStateOf { frame.value; sim.inside == home } }
    val name = breed.title

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            TopBar(if (home == 1) "Wohnzimmer" else "Schlafzimmer", onClose)
            Canvas(Modifier.weight(1f).fillMaxWidth()) {
                val t = frame.value / 1000f
                val w = size.width; val h = size.height
                val catSize = minOf(w, h * 0.8f) * 0.34f
                // the resident changes its spot every now and then
                val phase = ((frame.value / 14_000L) % 2L).toInt()
                if (home == 1) {
                    val onChair: (DrawScope.(Offset) -> Unit)? = if (catHere && phase == 0) { seat ->
                        drawCat(Offset(seat.x, seat.y - catSize * 0.43f), catSize * 0.92f, fur, CatAnim.SLEEP, t, look = lookOf(breed, 1f, -1f))
                    } else null
                    drawLivingRoom(w, h, t, dark, onChair)
                    if (catHere && phase == 1) {
                        val anim = if ((frame.value / 4_000L) % 2L == 0L) CatAnim.LOOK else CatAnim.WAG
                        drawCat(Offset(w * 0.44f, h * 0.84f - catSize * 0.43f), catSize, fur, anim, t, look = lookOf(breed, 1f, 1f))
                    }
                } else {
                    drawBedroom(w, h, t, dark)
                    if (catHere) {
                        if (phase == 0) drawCat(Offset(w * 0.33f, h * 0.575f - catSize * 0.43f), catSize * 0.95f, fur, CatAnim.SLEEP, t, look = lookOf(breed, 1f, 1f))
                        else drawCat(Offset(w * 0.5f, h * 0.84f - catSize * 0.43f), catSize, fur, CatAnim.LOOK, t, look = lookOf(breed, 1f, -1f))
                    }
                }
            }
            Text(
                if (catHere) "$name ist gerade zu Hause." else "Gerade ist niemand zu Hause.",
                style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            Text(
                "Drinnen ist es viel größer als draußen – eben ein richtiges Zuhause.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            )
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth().padding(12.dp).height(48.dp), shape = RoundedCornerShape(24.dp)) {
                Text("Zurück zum Kratzbaum")
            }
        }
    }
}

// ----------------------------------------------------------------------------- shared pieces

private fun DrawScope.pictureFrame(x: Float, y: Float, fw: Float, fh: Float, wood: Color, art: DrawScope.(Rect) -> Unit) {
    val r = Rect(Offset(x, y), Size(fw, fh))
    drawRect(Color.Black, Offset(x + fw * 0.03f, y + fw * 0.04f), Size(fw, fh), alpha = 0.12f)
    drawRoundRect(wood, r.topLeft, r.size, CornerRadius(fw * 0.05f))
    val m = fw * 0.09f
    val inner = Rect(Offset(x + m, y + m), Size(fw - 2 * m, fh - 2 * m))
    drawRect(Color(0xFFFAF3E3), inner.topLeft, inner.size)
    clipPath(Path().apply { addRect(inner) }) { art(inner) }
    drawRect(Color.Black, inner.topLeft, inner.size, alpha = 0.22f, style = Stroke(fw * 0.015f))
}

/** simple painting: sun over hills */
private fun DrawScope.artHills(r: Rect, sky: Color, hill1: Color, hill2: Color, sun: Color) {
    drawRect(sky, r.topLeft, r.size)
    drawCircle(sun, r.width * 0.12f, Offset(r.left + r.width * 0.7f, r.top + r.height * 0.32f))
    drawOval(hill1, Offset(r.left - r.width * 0.2f, r.top + r.height * 0.55f), Size(r.width * 1.0f, r.height * 0.9f))
    drawOval(hill2, Offset(r.left + r.width * 0.3f, r.top + r.height * 0.62f), Size(r.width * 1.0f, r.height * 0.8f))
}

/** simple painting: a fish in a bowl of blue */
private fun DrawScope.artFish(r: Rect) {
    drawRect(Color(0xFF9CD3E8), r.topLeft, r.size)
    val c = Offset(r.center.x, r.center.y)
    drawOval(Color(0xFFFF8F5A), Offset(c.x - r.width * 0.26f, c.y - r.height * 0.14f), Size(r.width * 0.46f, r.height * 0.28f))
    val tail = Path().apply { moveTo(c.x + r.width * 0.18f, c.y); lineTo(c.x + r.width * 0.38f, c.y - r.height * 0.16f); lineTo(c.x + r.width * 0.38f, c.y + r.height * 0.16f); close() }
    drawPath(tail, Color(0xFFFF6F3C))
    drawCircle(Color.White, r.width * 0.03f, Offset(c.x - r.width * 0.14f, c.y - r.height * 0.03f))
    drawCircle(Color(0xFF2B2A33), r.width * 0.015f, Offset(c.x - r.width * 0.14f, c.y - r.height * 0.03f))
    for (i in 0..2) drawCircle(Color.White, r.width * (0.018f + 0.006f * i), Offset(r.left + r.width * (0.2f + 0.08f * i), r.top + r.height * (0.7f - 0.15f * i)), alpha = 0.6f)
}

private fun DrawScope.books(x: Float, shelfY: Float, bw: Float, bh: Float, count: Int, seed: Int) {
    val cols = listOf(Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFFFFD54F), Color(0xFF81C784), Color(0xFFBA68C8), Color(0xFFFF8A65), Color(0xFF4DB6AC))
    var cx = x
    for (i in 0 until count) {
        val bwid = bw * (0.7f + 0.5f * (((i * 7 + seed * 3) % 5) / 4f))
        val bhei = bh * (0.72f + 0.28f * (((i * 5 + seed) % 4) / 3f))
        drawRect(cols[(i + seed) % cols.size], Offset(cx, shelfY - bhei), Size(bwid, bhei))
        drawRect(Color.White, Offset(cx + bwid * 0.15f, shelfY - bhei * 0.7f), Size(bwid * 0.7f, bhei * 0.05f), alpha = 0.5f)
        cx += bwid + bw * 0.06f
    }
}

private fun DrawScope.lampGlow(c: Offset, r: Float, a: Float) =
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE9A8).copy(alpha = a), Color.Transparent), c, r), r, c)

private fun DrawScope.roomShell(w: Float, h: Float, dark: Boolean, wallTop: Color, wallBottom: Color, floorTop: Color, floorBottom: Color, stripe: Color) {
    val floorY = h * 0.64f
    drawRect(Brush.verticalGradient(listOf(wallTop, wallBottom), 0f, floorY), Offset.Zero, Size(w, floorY))
    var x = 0f
    while (x < w) { drawRect(stripe, Offset(x, 0f), Size(w * 0.03f, floorY), alpha = if (dark) 0.10f else 0.18f); x += w * 0.09f }
    // crown molding + baseboard
    drawRect(if (dark) Color(0xFF4A3E38) else Color(0xFFF7EBD9), Offset.Zero, Size(w, h * 0.028f))
    drawRect(Color.Black, Offset(0f, h * 0.028f), Size(w, h * 0.004f), alpha = 0.14f)
    drawRect(if (dark) Color(0xFF4A3E38) else Color(0xFFF7EBD9), Offset(0f, floorY - h * 0.025f), Size(w, h * 0.025f))
    // floor boards
    drawRect(Brush.verticalGradient(listOf(floorTop, floorBottom), floorY, h), Offset(0f, floorY), Size(w, h - floorY))
    for (i in 1..8) {
        val y = floorY + (h - floorY) * (i / 8f) * (i / 8f)
        drawLine(Color.Black, Offset(0f, y), Offset(w, y), h * 0.0025f, alpha = 0.12f)
    }
}

// ----------------------------------------------------------------------------- living room

private fun DrawScope.drawLivingRoom(w: Float, h: Float, t: Float, dark: Boolean, onChair: (DrawScope.(Offset) -> Unit)?) {
    roomShell(
        w, h, dark,
        wallTop = if (dark) Color(0xFF4B3C3A) else Color(0xFFF6DFC0), wallBottom = if (dark) Color(0xFF3F322F) else Color(0xFFEDC99A),
        floorTop = if (dark) Color(0xFF4A3A2C) else Color(0xFFC99A66), floorBottom = if (dark) Color(0xFF3A2D22) else Color(0xFFB07F4E),
        stripe = if (dark) Color(0xFFFFFFFF) else Color(0xFFFFFFFF),
    )
    val floorY = h * 0.64f
    val wood = if (dark) Color(0xFF6B4A33) else Color(0xFFA9774A)
    val woodDark = if (dark) Color(0xFF55392A) else Color(0xFF8A5E3A)

    // bookshelf on the left
    val sx = w * 0.03f; val sw = w * 0.22f; val st = h * 0.2f
    drawRect(woodDark, Offset(sx, st), Size(sw, floorY - st - h * 0.01f))
    for (i in 0..3) {
        val y = st + (floorY - st - h * 0.01f) * (i + 1) / 4f
        drawRect(wood, Offset(sx, y - h * 0.008f), Size(sw, h * 0.012f))
        if (i < 3) books(sx + sw * 0.06f, y - h * 0.008f, sw * 0.06f, h * 0.075f, 8 - i, i + 1)
    }
    drawCircle(Color(0xFF6DBB83), w * 0.03f, Offset(sx + sw * 0.8f, st + h * 0.04f))   // a small plant on top row
    drawRect(Color(0xFFE08A64), Offset(sx + sw * 0.74f, st + h * 0.06f), Size(sw * 0.12f, h * 0.022f))

    // pictures
    pictureFrame(w * 0.30f, h * 0.07f, w * 0.12f, h * 0.15f, wood) { artHills(it, Color(0xFFBFE3F5), Color(0xFF7FC48B), Color(0xFF5EA872), Color(0xFFFFD36B)) }
    pictureFrame(w * 0.62f, h * 0.06f, w * 0.16f, h * 0.11f, wood) { artFish(it) }
    // wall clock
    val clock = Offset(w * 0.88f, h * 0.12f)
    drawCircle(woodDark, w * 0.065f, clock); drawCircle(Color(0xFFFFF8E8), w * 0.055f, clock)
    for (i in 0 until 12) { val a = i * PI.toFloat() / 6f; drawLine(woodDark, Offset(clock.x + sin(a) * w * 0.045f, clock.y - kotlin.math.cos(a) * w * 0.045f), Offset(clock.x + sin(a) * w * 0.05f, clock.y - kotlin.math.cos(a) * w * 0.05f), 2f) }
    val sec = t * 0.2f
    drawLine(Color(0xFF3B3340), clock, Offset(clock.x + sin(sec) * w * 0.04f, clock.y - kotlin.math.cos(sec) * w * 0.04f), 3f, StrokeCap.Round)
    drawLine(Color(0xFF3B3340), clock, Offset(clock.x + sin(sec / 12f + 1f) * w * 0.028f, clock.y - kotlin.math.cos(sec / 12f + 1f) * w * 0.028f), 4f, StrokeCap.Round)

    // fireplace in the middle
    val fx = w * 0.30f; val fw = w * 0.40f; val ft = h * 0.30f
    val brick = if (dark) Color(0xFF7A4A3E) else Color(0xFFC77D63)
    val brickDark = if (dark) Color(0xFF5E382F) else Color(0xFFA9604A)
    drawRoundRect(brick, Offset(fx, ft), Size(fw, floorY - ft - h * 0.012f), CornerRadius(w * 0.01f))
    clipPath(Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(Offset(fx, ft), Size(fw, floorY - ft)), CornerRadius(w * 0.01f))) }) {
        val rows = 9; val rh = (floorY - ft) / rows
        for (r in 0..rows) {
            drawLine(brickDark, Offset(fx, ft + r * rh), Offset(fx + fw, ft + r * rh), 2f, alpha = 0.8f)
            var bx = fx + (if (r % 2 == 0) 0f else fw * 0.075f)
            while (bx < fx + fw) { drawLine(brickDark, Offset(bx, ft + r * rh), Offset(bx, ft + (r + 1) * rh), 2f, alpha = 0.8f); bx += fw * 0.15f }
        }
    }
    // mantel shelf with a few things on it
    drawRoundRect(wood, Offset(fx - w * 0.03f, ft - h * 0.02f), Size(fw + w * 0.06f, h * 0.026f), CornerRadius(4f))
    drawRect(Color(0xFFFFF8E8), Offset(fx + fw * 0.12f, ft - h * 0.062f), Size(w * 0.012f, h * 0.042f))
    drawOval(Color(0xFFFFC94A), Offset(fx + fw * 0.12f - w * 0.004f, ft - h * 0.085f + sin(t * 9f) * 1.5f), Size(w * 0.02f, h * 0.026f))
    drawRoundRect(Color(0xFFE08A64), Offset(fx + fw * 0.7f, ft - h * 0.05f), Size(w * 0.05f, h * 0.03f), CornerRadius(3f))
    drawCircle(Color(0xFF6DBB83), w * 0.032f, Offset(fx + fw * 0.7f + w * 0.025f, ft - h * 0.062f))
    // the opening with a fire
    val ox = fx + fw * 0.2f; val ow = fw * 0.6f; val ot = ft + (floorY - ft) * 0.36f; val ob = floorY - h * 0.012f
    val open = Path().apply {
        moveTo(ox, ob); lineTo(ox, ot + ow * 0.3f)
        arcTo(Rect(ox, ot, ox + ow, ot + ow * 0.6f), 180f, 180f, false)
        lineTo(ox + ow, ob); close()
    }
    drawPath(open, Color(0xFF2A1A1A))
    clipPath(open) {
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFFF8A3C).copy(alpha = 0.55f)), ot, ob), Offset(ox, ot), Size(ow, ob - ot))
        for (i in 0..4) {
            val cx = ox + ow * (0.15f + 0.175f * i)
            val fh = (ob - ot) * (0.42f + 0.18f * sin(t * (6f + i * 1.7f) + i * 2f)) * (if (i == 2) 1.25f else 1f)
            val fwid = ow * 0.11f
            val sway = sin(t * (4f + i) + i) * fwid * 0.4f
            val flame = Path().apply {
                moveTo(cx - fwid, ob - h * 0.02f)
                quadraticTo(cx - fwid * 0.3f, ob - fh * 0.5f, cx + sway, ob - fh)
                quadraticTo(cx + fwid * 0.5f, ob - fh * 0.4f, cx + fwid, ob - h * 0.02f); close()
            }
            drawPath(flame, Color(0xFFFF7A2F))
            drawPath(flame, Color(0xFFFFC94A), alpha = 0.0f)
            val inner = Path().apply {
                moveTo(cx - fwid * 0.5f, ob - h * 0.02f)
                quadraticTo(cx, ob - fh * 0.35f, cx + sway * 0.6f, ob - fh * 0.62f)
                quadraticTo(cx + fwid * 0.3f, ob - fh * 0.25f, cx + fwid * 0.5f, ob - h * 0.02f); close()
            }
            drawPath(inner, Color(0xFFFFD66B))
        }
        // logs
        drawRoundRect(Color(0xFF6B4A33), Offset(ox + ow * 0.12f, ob - h * 0.03f), Size(ow * 0.76f, h * 0.026f), CornerRadius(8f))
    }
    drawPath(open, if (dark) Color(0xFF3B2A25) else Color(0xFF6B4A3A), style = Stroke(w * 0.012f))
    // hearth stone
    drawRoundRect(if (dark) Color(0xFF5D5560) else Color(0xFFD5CDC6), Offset(fx - w * 0.02f, floorY - h * 0.012f), Size(fw + w * 0.04f, h * 0.022f), CornerRadius(4f))

    // standing lamp
    val lx = w * 0.29f
    lampGlow(Offset(lx, h * 0.27f), w * 0.18f, if (dark) 0.45f else 0.35f)
    drawLine(Color(0xFF5B4A3B), Offset(lx, h * 0.31f), Offset(lx, floorY + h * 0.06f), w * 0.01f, StrokeCap.Round)
    drawOval(Color(0xFF5B4A3B), Offset(lx - w * 0.04f, floorY + h * 0.05f), Size(w * 0.08f, h * 0.018f))
    val shade = Path().apply { moveTo(lx - w * 0.035f, h * 0.31f); lineTo(lx - w * 0.055f, h * 0.22f); lineTo(lx + w * 0.055f, h * 0.22f); lineTo(lx + w * 0.035f, h * 0.31f); close() }
    drawPath(shade, Color(0xFFFFE9B8))
    drawPath(shade, Color(0xFFE0B96B), style = Stroke(2f))

    // rug
    drawOval(if (dark) Color(0xFF6E4E67) else Color(0xFFE8A2B6), Offset(w * 0.14f, h * 0.74f), Size(w * 0.72f, h * 0.2f))
    drawOval(if (dark) Color(0xFF805C79) else Color(0xFFF4C4D2), Offset(w * 0.19f, h * 0.765f), Size(w * 0.62f, h * 0.15f))
    drawOval(if (dark) Color(0xFF6E4E67) else Color(0xFFE8A2B6), Offset(w * 0.27f, h * 0.795f), Size(w * 0.46f, h * 0.09f), style = Stroke(w * 0.008f))

    // rocking chair on the right (rocks gently, more with the cat in it)
    val cx = w * 0.80f
    val seatY = floorY + h * 0.095f
    val legsEnd = seatY + h * 0.105f
    val swing = sin(t * 1.5f) * (if (onChair != null) 5f else 2.5f)
    rotate(swing, pivot = Offset(cx, legsEnd + h * 0.03f)) {
        // back with spindles
        val back = Path().apply { moveTo(cx - w * 0.085f, seatY); cubicTo(cx - w * 0.095f, seatY - h * 0.25f, cx + w * 0.095f, seatY - h * 0.25f, cx + w * 0.085f, seatY); lineTo(cx + w * 0.066f, seatY); cubicTo(cx + w * 0.066f, seatY - h * 0.19f, cx - w * 0.066f, seatY - h * 0.19f, cx - w * 0.066f, seatY); close() }
        drawPath(back, wood)
        for (i in -2..2) drawLine(wood, Offset(cx + i * w * 0.027f, seatY), Offset(cx + i * w * 0.027f, seatY - h * 0.17f + abs(i) * h * 0.016f), w * 0.008f, StrokeCap.Round)
        // legs and runners
        drawLine(woodDark, Offset(cx - w * 0.095f, seatY + h * 0.01f), Offset(cx - w * 0.105f, legsEnd), w * 0.014f, StrokeCap.Round)
        drawLine(woodDark, Offset(cx + w * 0.095f, seatY + h * 0.01f), Offset(cx + w * 0.105f, legsEnd), w * 0.014f, StrokeCap.Round)
        val runner = Path().apply { moveTo(cx - w * 0.17f, legsEnd + h * 0.005f); quadraticTo(cx, legsEnd + h * 0.075f, cx + w * 0.17f, legsEnd + h * 0.005f) }
        drawPath(runner, woodDark, style = Stroke(w * 0.015f, cap = StrokeCap.Round))
        // armrests
        drawLine(wood, Offset(cx - w * 0.115f, seatY - h * 0.07f), Offset(cx - w * 0.095f, seatY), w * 0.012f, StrokeCap.Round)
        drawLine(wood, Offset(cx + w * 0.115f, seatY - h * 0.07f), Offset(cx + w * 0.095f, seatY), w * 0.012f, StrokeCap.Round)
        drawRoundRect(wood, Offset(cx - w * 0.13f, seatY - h * 0.078f), Size(w * 0.05f, h * 0.014f), CornerRadius(5f))
        drawRoundRect(wood, Offset(cx + w * 0.08f, seatY - h * 0.078f), Size(w * 0.05f, h * 0.014f), CornerRadius(5f))
        // seat and cushion
        drawRoundRect(wood, Offset(cx - w * 0.115f, seatY), Size(w * 0.23f, h * 0.02f), CornerRadius(6f))
        drawRoundRect(if (dark) Color(0xFF8E5D75) else Color(0xFFF2A9BC), Offset(cx - w * 0.095f, seatY - h * 0.016f), Size(w * 0.19f, h * 0.022f), CornerRadius(9f))
        onChair?.invoke(this, Offset(cx, seatY - h * 0.014f))
    }

    // warm light from the fire over everything
    drawRect(Brush.radialGradient(listOf(Color(0xFFFF9B4A).copy(alpha = 0.16f + 0.03f * sin(t * 7f)), Color.Transparent), Offset(w * 0.5f, floorY), w * 0.7f), Offset.Zero, Size(w, h))
}

// ----------------------------------------------------------------------------- bedroom

private fun DrawScope.drawBedroom(w: Float, h: Float, t: Float, dark: Boolean) {
    roomShell(
        w, h, dark,
        wallTop = if (dark) Color(0xFF323A5A) else Color(0xFFDDE0F5), wallBottom = if (dark) Color(0xFF2A314C) else Color(0xFFC7CCEA),
        floorTop = if (dark) Color(0xFF41372F) else Color(0xFFD2A878), floorBottom = if (dark) Color(0xFF332B25) else Color(0xFFB98B5C),
        stripe = Color.White,
    )
    val floorY = h * 0.64f
    val wood = if (dark) Color(0xFF6B4A33) else Color(0xFFA9774A)
    val woodDark = if (dark) Color(0xFF55392A) else Color(0xFF8A5E3A)

    // round night window with moon and stars
    val wc = Offset(w * 0.74f, h * 0.27f); val wr = w * 0.15f
    drawCircle(woodDark, wr + w * 0.012f, wc)
    drawCircle(Brush.verticalGradient(listOf(Color(0xFF1C2447), Color(0xFF3A4C8C)), wc.y - wr, wc.y + wr), wr, wc)
    clipPath(Path().apply { addOval(Rect(Offset(wc.x - wr, wc.y - wr), Size(wr * 2, wr * 2))) }) {
        for (i in 0..9) {
            val sx = wc.x + (((i * 37) % 19) - 9) / 9f * wr * 0.85f; val sy = wc.y + (((i * 53) % 17) - 8) / 8f * wr * 0.85f
            drawCircle(Color.White, w * 0.004f * (1f + 0.6f * sin(t * 2f + i)), Offset(sx, sy), alpha = 0.6f + 0.4f * sin(t * 1.5f + i * 2f))
        }
        drawCircle(Color(0xFFFFF3C4), wr * 0.28f, Offset(wc.x - wr * 0.25f, wc.y - wr * 0.2f))
        drawCircle(Color(0xFF2E3870), wr * 0.24f, Offset(wc.x - wr * 0.12f, wc.y - wr * 0.26f))
    }
    drawLine(woodDark, Offset(wc.x - wr, wc.y), Offset(wc.x + wr, wc.y), w * 0.008f)
    drawLine(woodDark, Offset(wc.x, wc.y - wr), Offset(wc.x, wc.y + wr), w * 0.008f)
    // moonlight on the floor
    val beam = Path().apply { moveTo(w * 0.62f, floorY); lineTo(w * 0.88f, floorY); lineTo(w * 0.98f, h * 0.9f); lineTo(w * 0.7f, h * 0.9f); close() }
    drawPath(beam, Color(0xFFBFD0FF), alpha = 0.14f)

    // pictures above the bed
    pictureFrame(w * 0.07f, h * 0.08f, w * 0.13f, h * 0.15f, wood) { artHills(it, Color(0xFFF7D9E4), Color(0xFFA6D8A1), Color(0xFF86C48C), Color(0xFFFFE08A)) }
    pictureFrame(w * 0.23f, h * 0.06f, w * 0.18f, h * 0.12f, wood) { artFish(it) }
    pictureFrame(w * 0.43f, h * 0.10f, w * 0.1f, h * 0.12f, wood) { artHills(it, Color(0xFFC7E6F7), Color(0xFF8BCB93), Color(0xFF68B27B), Color(0xFFFF9E7A)) }

    // bed
    val bx = w * 0.03f; val bw = w * 0.56f; val by = floorY - h * 0.10f
    drawRoundRect(woodDark, Offset(bx, floorY - h * 0.30f), Size(w * 0.05f, h * 0.36f), CornerRadius(8f))           // headboard
    drawRoundRect(woodDark, Offset(bx + bw - w * 0.02f, by - h * 0.02f), Size(w * 0.03f, h * 0.14f), CornerRadius(6f))  // footboard
    drawRoundRect(woodDark, Offset(bx, by + h * 0.05f), Size(bw, h * 0.05f), CornerRadius(8f))
    drawRoundRect(Color(0xFFFFF8EE), Offset(bx + w * 0.03f, by - h * 0.02f), Size(bw - w * 0.04f, h * 0.075f), CornerRadius(12f))      // mattress
    val blanket = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(Offset(bx + w * 0.18f, by - h * 0.035f), Size(bw - w * 0.2f, h * 0.1f)), CornerRadius(16f))) }
    drawPath(blanket, if (dark) Color(0xFF7C5A96) else Color(0xFFB59BE0))
    clipPath(blanket) { for (i in 0..9) drawRect(Color.White, Offset(bx + w * 0.2f + i * w * 0.04f, by - h * 0.04f), Size(w * 0.018f, h * 0.12f), alpha = 0.22f) }
    drawRoundRect(Color.White, Offset(bx + w * 0.055f, by - h * 0.055f), Size(w * 0.13f, h * 0.045f), CornerRadius(14f))                  // pillow
    drawRoundRect(Color(0xFFE6E0F5), Offset(bx + w * 0.055f, by - h * 0.055f), Size(w * 0.13f, h * 0.045f), CornerRadius(14f), style = Stroke(2f))

    // nightstand with a lamp and a mug
    val nx = w * 0.64f
    drawRoundRect(wood, Offset(nx, floorY - h * 0.12f), Size(w * 0.14f, h * 0.15f), CornerRadius(6f))
    drawRect(woodDark, Offset(nx + w * 0.01f, floorY - h * 0.065f), Size(w * 0.12f, h * 0.006f))
    drawCircle(Color(0xFF5B4A3B), w * 0.008f, Offset(nx + w * 0.07f, floorY - h * 0.09f))
    lampGlow(Offset(nx + w * 0.035f, floorY - h * 0.21f), w * 0.2f, if (dark) 0.5f else 0.38f)
    drawRect(Color(0xFF5B4A3B), Offset(nx + w * 0.032f, floorY - h * 0.17f), Size(w * 0.006f, h * 0.05f))
    val shade = Path().apply { moveTo(nx + w * 0.01f, floorY - h * 0.165f); lineTo(nx + w * 0.0f, floorY - h * 0.22f); lineTo(nx + w * 0.07f, floorY - h * 0.22f); lineTo(nx + w * 0.06f, floorY - h * 0.165f); close() }
    drawPath(shade, Color(0xFFFFE9B8))
    drawRoundRect(Color(0xFFFFFFFF), Offset(nx + w * 0.09f, floorY - h * 0.15f), Size(w * 0.03f, h * 0.03f), CornerRadius(4f))
    for (i in 0..1) drawLine(Color.White, Offset(nx + w * 0.1f + i * w * 0.012f, floorY - h * 0.16f), Offset(nx + w * 0.1f + i * w * 0.012f + sin(t * 2f + i) * 3f, floorY - h * 0.19f), 2f, StrokeCap.Round, alpha = 0.5f)

    // little bookshelf on the right wall
    val sx = w * 0.83f
    drawRect(woodDark, Offset(sx, h * 0.46f), Size(w * 0.15f, h * 0.014f))
    books(sx + w * 0.012f, h * 0.46f, w * 0.018f, h * 0.07f, 5, 2)
    drawRect(woodDark, Offset(sx, h * 0.56f), Size(w * 0.15f, h * 0.014f))
    books(sx + w * 0.012f, h * 0.56f, w * 0.018f, h * 0.07f, 4, 4)
    drawCircle(Color(0xFF6DBB83), w * 0.03f, Offset(sx + w * 0.115f, h * 0.425f))
    drawRoundRect(Color(0xFFE08A64), Offset(sx + w * 0.1f, h * 0.44f), Size(w * 0.03f, h * 0.02f), CornerRadius(3f))

    // rug
    drawOval(if (dark) Color(0xFF574A7A) else Color(0xFFC2B2EA), Offset(w * 0.2f, h * 0.76f), Size(w * 0.62f, h * 0.18f))
    drawOval(if (dark) Color(0xFF6A5C91) else Color(0xFFDDD2F7), Offset(w * 0.25f, h * 0.785f), Size(w * 0.52f, h * 0.13f))
}
