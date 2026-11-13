package de.schnurrfeld.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.schnurrfeld.app.App
import de.schnurrfeld.app.audio.Sfx
import de.schnurrfeld.app.audio.meowFor
import de.schnurrfeld.app.audio.meowRate
import de.schnurrfeld.app.data.Breed
import de.schnurrfeld.app.data.Pet
import de.schnurrfeld.app.ui.components.CatAnim
import de.schnurrfeld.app.ui.components.Sky
import de.schnurrfeld.app.ui.components.TopBar
import de.schnurrfeld.app.ui.components.drawCat
import de.schnurrfeld.app.ui.components.drawSky
import de.schnurrfeld.app.ui.components.furOf
import de.schnurrfeld.app.ui.components.lookOf
import de.schnurrfeld.app.ui.components.noise
import de.schnurrfeld.app.ui.components.rememberFrameMillis
import de.schnurrfeld.app.ui.theme.Fur
import de.schnurrfeld.app.ui.theme.LocalDark
import androidx.compose.ui.geometry.lerp as lerpOffset
import androidx.compose.ui.graphics.lerp as lerpColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * The room behind one of the doorways of the climbing tree. It is far bigger inside than outside:
 * the cat has its own little home. [home] 1 = living room (lower left cave), 2 = bedroom (upper right cave).
 * Lots of things react when you tap them. [startFx] (key -> age in ms) starts effects right away (for screenshots).
 */
@Composable
fun RoomOverlay(app: App, pet: Pet, sim: PetSim, home: Int, startFx: Map<String, Long> = emptyMap(), onClose: () -> Unit) {
    val frame = rememberFrameMillis()
    val dark = LocalDark.current
    val breed = pet.breed ?: Breed.WOLKE
    val fur = furOf(breed)
    BackHandler(onBack = onClose)
    val catHere by remember { derivedStateOf { frame.value; sim.inside == home } }
    val name = breed.title
    val starts = remember { HashMap<String, Long>() }
    val lampOn = remember { mutableStateOf(true) }
    val taps = remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { startFx.forEach { (k, age) -> starts[k] = frame.value - age } }

    fun trigger(key: String) {
        starts[key] = frame.value
        taps.intValue++
        if (key == "lamp") lampOn.value = !lampOn.value
        if (!app.store.settings.sound) return
        when (key) {
            "fire" -> app.sound.play(Sfx.SCRATCH, 0.35f, 1.6f)
            "fish" -> app.sound.play(Sfx.PLOP, 0.7f, 1.1f)
            "clock" -> app.sound.play(Sfx.COIN, 0.5f)
            "candle" -> app.sound.play(Sfx.ERASE, 0.6f)
            "window" -> app.sound.play(Sfx.SUCCESS_1, 0.4f)
            "diffuser" -> app.sound.play(Sfx.SUCCESS_3, 0.35f, 1.2f)
            "blanket" -> app.sound.play(Sfx.CRANK, 0.4f, 1.3f)
            "cat" -> { val m = meowFor(6, false); app.sound.play(m, 0.28f, meowRate(m, 6)) }
            else -> app.sound.play(Sfx.TAP, 0.6f, 0.9f + noise(taps.intValue) * 0.3f)
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            TopBar(if (home == 1) "Wohnzimmer" else "Schlafzimmer", onClose)
            Canvas(
                Modifier.weight(1f).fillMaxWidth().testTag("roomCanvas").pointerInput(home) {
                    detectTapGestures { pos ->
                        val g = Geo(size.width.toFloat(), size.height.toFloat())
                        val spots = if (home == 1) livingSpots(g) else bedroomSpots(g)
                        val hit = (if (catHere) listOf(Hot("cat", catRect(g, home, ((frame.value / 14_000L) % 2L).toInt()))) else emptyList()) + spots
                        hit.firstOrNull { it.rect.contains(pos) }?.let { trigger(it.key) }
                    }
                }
            ) {
                val t = frame.value / 1000f
                val g = Geo(size.width, size.height)
                val fx = Fx(starts, frame.value)
                val night = Sky.night(Sky.hour())
                val resident = if (catHere) Resident(fur, breed, ((frame.value / 14_000L) % 2L).toInt()) else null
                if (home == 1) drawLivingRoom(g, t, dark, fx, lampOn.value, night, resident, taps.intValue)
                else drawBedroom(g, t, dark, fx, lampOn.value, night, resident)
            }
            Text(
                if (catHere) "$name ist gerade zu Hause." else "Gerade ist niemand zu Hause.",
                style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
            Text(
                "Tippe auf Dinge im Zimmer – manches reagiert.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            )
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth().padding(12.dp).height(48.dp), shape = RoundedCornerShape(24.dp)) {
                Text("Zurück zum Kratzbaum")
            }
        }
    }
}

// ============================================================================ helpers

private class Resident(val fur: Fur, val breed: Breed, val phase: Int)
private class Hot(val key: String, val rect: Rect)

/** running tap effects: progress 0..1 while an effect is active */
private class Fx(private val starts: HashMap<String, Long>, private val now: Long) {
    fun p(key: String, dur: Long): Float? {
        val s = starts[key] ?: return null
        val a = now - s
        return if (a in 0..dur) a / dur.toFloat() else null
    }
}

/** The room is a stage seen from the front: the wall at the back, the floor in front. Floor-standing things have their base at or below [floorY]. */
private class Geo(val w: Float, val h: Float) {
    val floorY = h * 0.62f
    /** u: 0 left .. 1 right, v: 0 at the wall .. 1 at the viewer (the floor gets slightly wider towards the viewer) */
    fun floor(u: Float, v: Float): Offset {
        val sc = 0.8f + 0.2f * v
        return Offset(w * (0.5f + (u - 0.5f) * sc), floorY + (h - floorY) * v)
    }
    fun rect(x0: Float, y0: Float, x1: Float, y1: Float) = Rect(w * x0, h * y0, w * x1, h * y1)
}

private fun mixc(a: Color, b: Color, k: Float) = lerpColor(a, b, k.coerceIn(0f, 1f))

private fun DrawScope.poly(color: Color, a: Offset, b: Offset, c: Offset, d: Offset, alpha: Float = 1f) {
    drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close() }, color, alpha = alpha)
}

private fun DrawScope.heart(c: Offset, r: Float, a: Float) {
    if (a <= 0f) return
    val p = Path().apply {
        moveTo(c.x, c.y + r * 0.85f)
        cubicTo(c.x - r * 1.35f, c.y - r * 0.1f, c.x - r * 0.65f, c.y - r * 1.15f, c.x, c.y - r * 0.35f)
        cubicTo(c.x + r * 0.65f, c.y - r * 1.15f, c.x + r * 1.35f, c.y - r * 0.1f, c.x, c.y + r * 0.85f); close()
    }
    drawPath(p, Color(0xFFFF6F91), alpha = a.coerceIn(0f, 1f))
}

/** a bump 0 -> 1 -> 0 over p in 0..1 */
private fun bell(p: Float) = sin(p * PI.toFloat()).coerceAtLeast(0f)

/** damped swing: amplitude decays with p */
private fun swing(p: Float?, amp: Float, freq: Float = 18f) = if (p == null) 0f else amp * (1f - p).pow(2) * sin(p * freq)

private fun catRect(g: Geo, home: Int, phase: Int): Rect = when {
    home == 1 && phase == 0 -> g.rect(0.55f, 0.52f, 0.78f, 0.78f)       // on the rocking chair
    home == 1 -> g.rect(0.24f, 0.7f, 0.46f, 0.96f)                      // on the rug
    phase == 0 -> g.rect(0.22f, 0.3f, 0.5f, 0.52f)                      // on the bed
    else -> g.rect(0.28f, 0.7f, 0.5f, 0.96f)                            // on the rug
}

private fun DrawScope.drawCatAt(feet: Offset, size: Float, r: Resident, anim: CatAnim, t: Float, facing: Float) {
    drawCat(Offset(feet.x, feet.y - size * 0.43f), size, r.fur, anim, t, look = lookOf(r.breed, 1f, facing))
}

// ============================================================================ shell: wall, floor

private class Palette(val wall: Color, val wallDeep: Color, val floorA: Color, val floorB: Color, val trim: Color, val stripe: Color, val wood: Color, val woodDark: Color)

private fun DrawScope.shell(g: Geo, p: Palette, dark: Boolean) {
    val w = g.w; val h = g.h; val fy = g.floorY
    drawRect(Brush.verticalGradient(listOf(p.wall, mixc(p.wall, p.wallDeep, 0.5f)), 0f, fy), Offset.Zero, Size(w, fy))
    var x = 0f
    while (x < w) { drawRect(p.stripe, Offset(x, 0f), Size(w * 0.022f, fy), alpha = if (dark) 0.07f else 0.16f); x += w * 0.06f }
    // wainscot
    val wy = fy - fy * 0.24f
    drawRect(mixc(p.wall, p.trim, 0.55f), Offset(0f, wy), Size(w, fy - wy))
    drawRect(p.trim, Offset(0f, wy - h * 0.006f), Size(w, h * 0.01f))
    for (i in 0..7) drawRect(Color.Black, Offset(w * (0.015f + i * 0.123f), wy + h * 0.014f), Size(w * 0.1f, fy - wy - h * 0.04f), alpha = 0.07f, style = Stroke(1.5f))
    // crown molding and baseboard
    drawRect(p.trim, Offset.Zero, Size(w, h * 0.03f))
    drawRect(Color.Black, Offset(0f, h * 0.03f), Size(w, h * 0.004f), alpha = 0.12f)
    drawRect(p.trim, Offset(0f, fy - h * 0.026f), Size(w, h * 0.026f))
    drawRect(Color.Black, Offset(0f, fy - h * 0.004f), Size(w, h * 0.004f), alpha = 0.12f)
    // floor: boards run slightly towards each other in the distance
    drawRect(Brush.verticalGradient(listOf(p.floorB, p.floorA), fy, h), Offset(0f, fy), Size(w, h - fy))
    for (i in 0..16) drawLine(Color.Black, g.floor(i / 16f, 0f), g.floor(i / 16f, 1f), 1.4f, alpha = 0.12f)
    for (k in 1..8) {
        val v = (k / 9f).pow(1.4f)
        for (seg in 0 until 16 step 2) {
            val a = ((seg + (k % 2)) / 16f).coerceAtMost(1f); val b = ((seg + 1 + (k % 2)) / 16f).coerceAtMost(1f)
            drawLine(Color.Black, g.floor(a, v), g.floor(b, v), 1.2f, alpha = 0.1f)
        }
    }
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = if (dark) 0.26f else 0.12f)), Offset(w / 2, h * 0.5f), maxOf(w, h) * 0.8f), Offset.Zero, Size(w, h))
}

/** A rug lying on the floor in perspective: border, field, diamonds and fringe. */
private fun DrawScope.rug(g: Geo, u0: Float, u1: Float, v0: Float, v1: Float, border: Color, field: Color, motif: Color, cream: Color) {
    val tl = g.floor(u0, v0); val tr = g.floor(u1, v0); val bl = g.floor(u0, v1); val br = g.floor(u1, v1)
    fun P(a: Float, b: Float) = lerpOffset(lerpOffset(tl, tr, a), lerpOffset(bl, br, a), b)
    poly(Color.Black, P(0f, 0f), P(1f, 0f), P(1f, 1f), P(0f, 1f), alpha = 0.12f)
    poly(border, P(0f, 0f), P(1f, 0f), P(1f, 1f), P(0f, 1f))
    poly(field, P(0.06f, 0.07f), P(0.94f, 0.07f), P(0.94f, 0.93f), P(0.06f, 0.93f))
    poly(cream, P(0.13f, 0.15f), P(0.87f, 0.15f), P(0.87f, 0.85f), P(0.13f, 0.85f), alpha = 0.55f)
    for (k in 0..3) {
        val b = 0.25f + k * 0.17f
        poly(motif, P(0.5f, b - 0.075f), P(0.5f + 0.2f, b), P(0.5f, b + 0.075f), P(0.5f - 0.2f, b))
        poly(cream, P(0.5f, b - 0.035f), P(0.5f + 0.09f, b), P(0.5f, b + 0.035f), P(0.5f - 0.09f, b))
    }
    for (i in 0..11) for (b in listOf(0.035f, 0.965f)) {
        val a = (i + 0.5f) / 12f
        poly(motif, P(a - 0.02f, b - 0.02f), P(a + 0.02f, b - 0.02f), P(a + 0.02f, b + 0.02f), P(a - 0.02f, b + 0.02f), alpha = 0.75f)
    }
    for (i in 0..14) {
        val a = (i + 0.5f) / 15f
        drawLine(cream, P(a, 0f), P(a, -0.035f), 2f, StrokeCap.Round)
        drawLine(cream, P(a, 1f), P(a, 1.04f), 2f, StrokeCap.Round)
    }
}

private fun DrawScope.dust(g: Geo, p: Float) {
    for (i in 0..6) {
        val k = (p * 1.3f - noise(i) * 0.3f).coerceIn(0f, 1f)
        if (k <= 0f || k >= 1f) continue
        val c = g.floor(0.3f + 0.4f * noise(i, 2), 0.5f + 0.3f * noise(i, 3))
        drawCircle(Color(0xFFD9CFC2), g.w * (0.014f + 0.03f * k), Offset(c.x, c.y - k * g.h * 0.05f), alpha = 0.5f * (1f - k))
    }
}

private fun DrawScope.pictureFrame(r: Rect, wood: Color, angle: Float, art: DrawScope.(Rect) -> Unit) {
    rotate(angle, pivot = Offset(r.center.x, r.top)) {
        drawRect(Color.Black, Offset(r.left + r.width * 0.04f, r.top + r.width * 0.05f), r.size, alpha = 0.14f)
        drawRoundRect(wood, r.topLeft, r.size, CornerRadius(r.width * 0.05f))
        val m = r.width * 0.09f
        val inner = Rect(r.left + m, r.top + m, r.right - m, r.bottom - m)
        drawRect(Color(0xFFFAF3E3), inner.topLeft, inner.size)
        clipPath(Path().apply { addRect(inner) }) { art(inner) }
        drawRect(Color.Black, inner.topLeft, inner.size, alpha = 0.22f, style = Stroke(r.width * 0.015f))
        drawLine(Color.White, Offset(r.left + 4f, r.top + 3f), Offset(r.right - 4f, r.top + 3f), 2f, alpha = 0.3f)
    }
}

private fun DrawScope.artHills(r: Rect, sky: Color, hill1: Color, hill2: Color, sun: Color) {
    drawRect(sky, r.topLeft, r.size)
    drawCircle(sun, r.width * 0.12f, Offset(r.left + r.width * 0.7f, r.top + r.height * 0.32f))
    drawOval(hill1, Offset(r.left - r.width * 0.2f, r.top + r.height * 0.55f), Size(r.width * 1.0f, r.height * 0.9f))
    drawOval(hill2, Offset(r.left + r.width * 0.3f, r.top + r.height * 0.62f), Size(r.width * 1.0f, r.height * 0.8f))
}

/** a fish picture: [fishX] 0.5 = resting in the middle, <0 / >1 = out of the frame; [bubbles] 0..1 while it blubbers */
private fun DrawScope.artFish(r: Rect, fishX: Float, bubbles: Float?, t: Float) {
    drawRect(Brush.verticalGradient(listOf(Color(0xFFB4E1F2), Color(0xFF7FC0DE)), r.top, r.bottom), r.topLeft, r.size)
    for (i in 0..3) drawLine(Color.White, Offset(r.left + r.width * (0.15f + 0.22f * i), r.top), Offset(r.left + r.width * (0.1f + 0.22f * i), r.bottom), 1.5f, alpha = 0.18f)
    drawRect(Color(0xFFE8D4A4), Offset(r.left, r.bottom - r.height * 0.16f), Size(r.width, r.height * 0.16f))
    for (i in 0..2) drawOval(Color(0xFF6DBB83), Offset(r.left + r.width * (0.2f + 0.3f * i), r.bottom - r.height * 0.38f), Size(r.width * 0.07f, r.height * 0.3f))
    val c = Offset(r.left + r.width * fishX, r.center.y + sin(t * 2.2f) * r.height * 0.02f)
    val fw = r.width * 0.46f; val fh = r.height * 0.3f
    drawOval(Color(0xFFFF8F5A), Offset(c.x - fw * 0.55f, c.y - fh / 2), Size(fw, fh))
    drawPath(Path().apply { moveTo(c.x + fw * 0.35f, c.y); lineTo(c.x + fw * 0.7f, c.y - fh * 0.5f); lineTo(c.x + fw * 0.7f, c.y + fh * 0.5f); close() }, Color(0xFFFF6F3C))
    drawOval(Color(0xFFFFD36B), Offset(c.x - fw * 0.05f, c.y - fh * 0.15f), Size(fw * 0.3f, fh * 0.5f), alpha = 0.7f)
    drawCircle(Color.White, fh * 0.14f, Offset(c.x - fw * 0.32f, c.y - fh * 0.12f))
    drawCircle(Color(0xFF2B2A33), fh * 0.07f, Offset(c.x - fw * 0.33f, c.y - fh * 0.12f))
    bubbles?.let { bp ->
        for (i in 0..5) {
            val k = ((bp * 1.6f) - i * 0.12f).coerceIn(0f, 1f)
            if (k > 0f && k < 1f) drawCircle(Color.White, r.width * (0.018f + 0.012f * noise(i)), Offset(c.x - fw * 0.5f + noise(i, 2) * fw * 0.4f + sin(k * 9f + i) * 3f, c.y - k * r.height * 0.55f), alpha = 0.85f * (1f - k))
        }
    }
}

private fun DrawScope.books(x: Float, shelfY: Float, bw: Float, bh: Float, count: Int, seed: Int, slideIdx: Int = -1, slide: Float = 0f) {
    val cols = listOf(Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFFFFD54F), Color(0xFF81C784), Color(0xFFBA68C8), Color(0xFFFF8A65), Color(0xFF4DB6AC))
    var cx = x
    for (i in 0 until count) {
        val bwid = bw * (0.7f + 0.5f * (((i * 7 + seed * 3) % 5) / 4f))
        val bhei = bh * (0.72f + 0.28f * (((i * 5 + seed) % 4) / 3f))
        val sl = if (i == slideIdx) slide else 0f
        rotate(-16f * sl, pivot = Offset(cx + bwid, shelfY)) {
            drawRect(cols[(i + seed) % cols.size], Offset(cx, shelfY - bhei - bh * 0.1f * sl), Size(bwid, bhei))
            drawRect(Color.White, Offset(cx + bwid * 0.15f, shelfY - bhei * 0.7f - bh * 0.1f * sl), Size(bwid * 0.7f, bhei * 0.05f), alpha = 0.5f)
        }
        cx += bwid + bw * 0.06f
    }
}

private fun DrawScope.glow(c: Offset, r: Float, a: Float, col: Color = Color(0xFFFFE9A8)) =
    drawCircle(Brush.radialGradient(listOf(col.copy(alpha = a), Color.Transparent), c, r), r, c)

private fun DrawScope.plantPot(base: Offset, s: Float, sway: Float, leafA: Color, leafB: Color, pot: Color) {
    for ((i, ang) in listOf(-55f, -30f, -8f, 14f, 36f, 58f, -42f, 46f).withIndex()) {
        rotate(ang + sway * (if (i % 2 == 0) 1 else -1), Offset(base.x, base.y - s * 0.5f)) {
            drawOval(if (i % 2 == 0) leafA else leafB, Offset(base.x - s * 0.1f, base.y - s * (1.1f + 0.12f * (i % 3))), Size(s * 0.2f, s * 0.7f))
        }
    }
    poly(pot, Offset(base.x - s * 0.3f, base.y - s * 0.5f), Offset(base.x + s * 0.3f, base.y - s * 0.5f), Offset(base.x + s * 0.22f, base.y), Offset(base.x - s * 0.22f, base.y))
    drawRect(mixc(pot, Color.Black, 0.12f), Offset(base.x - s * 0.33f, base.y - s * 0.56f), Size(s * 0.66f, s * 0.12f))
}

// ============================================================================ living room

private fun livingSpots(g: Geo): List<Hot> = listOf(
    Hot("clock", g.rect(0.65f, 0.085f, 0.72f, 0.15f)),
    Hot("candle", g.rect(0.31f, 0.19f, 0.36f, 0.30f)),
    Hot("plant", g.rect(0.49f, 0.2f, 0.565f, 0.30f)),
    Hot("fish", g.rect(0.355f, 0.095f, 0.505f, 0.245f)),
    Hot("hills", g.rect(0.05f, 0.08f, 0.17f, 0.17f)),
    Hot("window", g.rect(0.605f, 0.17f, 0.765f, 0.42f)),
    Hot("fire", g.rect(0.33f, 0.40f, 0.53f, 0.63f)),
    Hot("chair", g.rect(0.54f, 0.5f, 0.86f, 0.9f)),
    Hot("lamp", g.rect(0.86f, 0.27f, 0.95f, 0.76f)),
    Hot("book", g.rect(0.025f, 0.2f, 0.205f, 0.64f)),
    Hot("fern", g.rect(0.0f, 0.5f, 0.17f, 0.84f)),
    Hot("rug", g.rect(0.18f, 0.72f, 0.6f, 0.97f)),
)

private fun DrawScope.drawLivingRoom(g: Geo, t: Float, dark: Boolean, fx: Fx, lampOn: Boolean, night: Float, cat: Resident?, tapCount: Int) {
    val w = g.w; val h = g.h; val fy = g.floorY
    val pal = Palette(
        wall = if (dark) Color(0xFF55433F) else Color(0xFFF7E1C3), wallDeep = if (dark) Color(0xFF3B2E2C) else Color(0xFFE2BB88),
        floorA = if (dark) Color(0xFF4F3C2D) else Color(0xFFC99A66), floorB = if (dark) Color(0xFF3A2D22) else Color(0xFFA9774A),
        trim = if (dark) Color(0xFF6B5A52) else Color(0xFFFFF6E6), stripe = Color.White,
        wood = if (dark) Color(0xFF6B4A33) else Color(0xFFA9774A), woodDark = if (dark) Color(0xFF55392A) else Color(0xFF7E5535),
    )
    val wood = pal.wood; val woodDark = pal.woodDark
    val base = fy + h * 0.012f      // furniture that stands against the wall rests on the floor right in front of the baseboard
    shell(g, pal, dark)

    // ---- window with curtains (the sky follows the real time)
    val win = g.rect(0.605f, 0.17f, 0.765f, 0.42f)
    drawRect(Color.Black, Offset(win.left - 3f, win.top - 3f), Size(win.width + 6f, win.height + 6f), alpha = 0.15f)
    drawSky(win, t, 1, fx.p("window", 2200))
    drawLine(Color.White, Offset(win.center.x, win.top), Offset(win.center.x, win.bottom), w * 0.008f, alpha = 0.9f)
    drawLine(Color.White, Offset(win.left, win.center.y), Offset(win.right, win.center.y), w * 0.008f, alpha = 0.9f)
    drawRect(wood, win.topLeft, win.size, style = Stroke(w * 0.014f))
    drawRoundRect(wood, Offset(win.left - w * 0.012f, win.bottom), Size(win.width + w * 0.024f, h * 0.012f), CornerRadius(3f))
    val curtain = if (dark) Color(0xFF8A4F5E) else Color(0xFFE79DB3)
    for (side in listOf(-1f, 1f)) {
        val ex = if (side < 0) win.left else win.right
        val c = Path().apply {
            moveTo(ex - side * w * 0.02f, win.top - h * 0.012f); lineTo(ex + side * w * 0.03f, win.top - h * 0.012f)
            cubicTo(ex + side * w * 0.035f, win.center.y, ex + side * w * 0.01f, win.center.y + h * 0.03f, ex + side * w * 0.025f, win.bottom + h * 0.04f)
            lineTo(ex - side * w * 0.02f, win.bottom + h * 0.04f); close()
        }
        drawPath(c, Brush.horizontalGradient(listOf(mixc(curtain, Color.Black, 0.18f), curtain, mixc(curtain, Color.White, 0.2f)), ex - w * 0.02f, ex + w * 0.03f))
    }
    drawLine(woodDark, Offset(win.left - w * 0.04f, win.top - h * 0.014f), Offset(win.right + w * 0.04f, win.top - h * 0.014f), h * 0.007f, StrokeCap.Round)
    // a patch of light from the window falls on the floor
    val day = 1f - night
    val beam = listOf(Offset(w * 0.62f, fy + h * 0.015f), Offset(w * 0.77f, fy + h * 0.015f), Offset(w * 0.84f, fy + h * 0.17f), Offset(w * 0.6f, fy + h * 0.17f))
    poly(Color(0xFFFFF0B0), beam[0], beam[1], beam[2], beam[3], alpha = 0.15f * day)
    poly(Color(0xFFBFD0FF), beam[0], beam[1], beam[2], beam[3], alpha = 0.1f * night)

    // ---- wall clock with a cuckoo (above the window)
    val clock = Offset(w * 0.685f, h * 0.115f); val cr = w * 0.027f
    val cp = fx.p("clock", 2400)
    drawCircle(woodDark, cr * 1.15f, clock); drawCircle(Color(0xFFFFF8E8), cr, clock)
    for (i in 0 until 12) { val a = i * PI.toFloat() / 6f; drawLine(woodDark, Offset(clock.x + sin(a) * cr * 0.82f, clock.y - cos(a) * cr * 0.82f), Offset(clock.x + sin(a) * cr * 0.95f, clock.y - cos(a) * cr * 0.95f), 1.5f) }
    val spin = if (cp != null) cp * 40f else 0f
    val minute = t * 0.2f + spin; val hourA = t * 0.2f / 12f + 1f + spin / 12f
    drawLine(Color(0xFF3B3340), clock, Offset(clock.x + sin(minute) * cr * 0.8f, clock.y - cos(minute) * cr * 0.8f), 2f, StrokeCap.Round)
    drawLine(Color(0xFF3B3340), clock, Offset(clock.x + sin(hourA) * cr * 0.55f, clock.y - cos(hourA) * cr * 0.55f), 2.6f, StrokeCap.Round)
    if (cp != null) {
        val by = clock.y - cr * 1.15f - cr * 0.9f * bell(cp)
        drawOval(Color(0xFFFFD36B), Offset(clock.x - cr * 0.35f, by - cr * 0.3f), Size(cr * 0.7f, cr * 0.6f))
        drawPath(Path().apply { moveTo(clock.x + cr * 0.3f, by); lineTo(clock.x + cr * 0.6f, by - cr * 0.08f); lineTo(clock.x + cr * 0.3f, by + cr * 0.12f); close() }, Color(0xFFFF8F5A))
        drawCircle(Color(0xFF2B2A33), cr * 0.05f, Offset(clock.x + cr * 0.12f, by - cr * 0.08f))
    }

    // ---- bookshelf against the left wall; one book slides out and back when tapped
    val sx = w * 0.025f; val sw = w * 0.18f; val st = h * 0.2f
    drawRect(woodDark, Offset(sx, st), Size(sw, base - st))
    val bp = fx.p("book", 2200)
    val rows = 4
    for (i in 0 until rows) {
        val y = st + (base - st) * (i + 1) / rows.toFloat()
        drawRect(wood, Offset(sx, y - h * 0.008f), Size(sw, h * 0.011f))
        if (i < rows - 1) books(sx + sw * 0.07f, y - h * 0.008f, sw * 0.062f, h * 0.075f, 9 - i, i + 1, slideIdx = if (i == tapCount % (rows - 1)) 2 else -1, slide = if (bp != null) bell(bp) else 0f)
    }
    drawCircle(Color(0xFF6DBB83), w * 0.022f, Offset(sx + sw * 0.78f, st - h * 0.012f)); drawRect(Color(0xFFE08A64), Offset(sx + sw * 0.7f, st - h * 0.003f), Size(sw * 0.16f, h * 0.012f))
    pictureFrame(g.rect(0.05f, 0.08f, 0.17f, 0.17f), wood, swing(fx.p("hills", 2600), 9f)) { artHills(it, Color(0xFFBFE3F5), Color(0xFF7FC48B), Color(0xFF5EA872), Color(0xFFFFD36B)) }

    // ---- chimney breast, fireplace and mantel
    val fL = w * 0.30f; val fR = w * 0.56f; val fT = h * 0.30f; val fB = base
    val plaster = if (dark) Color(0xFF6A5249) else Color(0xFFF2D3AE)
    drawRect(Brush.horizontalGradient(listOf(mixc(plaster, Color.Black, 0.1f), plaster, mixc(plaster, Color.Black, 0.1f)), w * 0.335f, w * 0.525f), Offset(w * 0.335f, h * 0.034f), Size(w * 0.19f, fT - h * 0.034f))
    val brick = if (dark) Color(0xFF7A4A3E) else Color(0xFFC77D63); val brickDark = if (dark) Color(0xFF5E382F) else Color(0xFFA9604A)
    drawRoundRect(brick, Offset(fL, fT), Size(fR - fL, fB - fT), CornerRadius(w * 0.008f))
    clipPath(Path().apply { addRect(Rect(Offset(fL, fT), Size(fR - fL, fB - fT))) }) {
        val rws = 10; val rh = (fB - fT) / rws
        for (r in 0..rws) {
            drawLine(brickDark, Offset(fL, fT + r * rh), Offset(fR, fT + r * rh), 2f, alpha = 0.8f)
            var bx = fL + (if (r % 2 == 0) 0f else (fR - fL) * 0.06f)
            while (bx < fR) { drawLine(brickDark, Offset(bx, fT + r * rh), Offset(bx, fT + (r + 1) * rh), 2f, alpha = 0.8f); bx += (fR - fL) * 0.12f }
        }
    }
    drawRoundRect(wood, Offset(fL - w * 0.025f, fT - h * 0.022f), Size(fR - fL + w * 0.05f, h * 0.026f), CornerRadius(4f))
    drawRect(woodDark, Offset(fL - w * 0.025f, fT + h * 0.002f), Size(fR - fL + w * 0.05f, h * 0.004f))
    // hearth stone in front of the fireplace, on the floor
    poly(if (dark) Color(0xFF5D5560) else Color(0xFFD5CDC6), Offset(fL - w * 0.02f, fB), Offset(fR + w * 0.02f, fB), Offset(fR + w * 0.045f, fB + h * 0.04f), Offset(fL - w * 0.045f, fB + h * 0.04f))
    // the opening with the fire
    val ox = fL + (fR - fL) * 0.2f; val ow = (fR - fL) * 0.6f; val ot = fT + (fB - fT) * 0.36f; val ob = fB - h * 0.004f
    val open = Path().apply { moveTo(ox, ob); lineTo(ox, ot + ow * 0.3f); arcTo(Rect(ox, ot, ox + ow, ot + ow * 0.6f), 180f, 180f, false); lineTo(ox + ow, ob); close() }
    drawPath(open, Color(0xFF2A1A1A))
    val fp = fx.p("fire", 2600)
    val boost = if (fp != null) 1f + 0.8f * bell(fp) else 1f
    clipPath(open) {
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFFFF8A3C).copy(alpha = 0.6f)), ot, ob), Offset(ox, ot), Size(ow, ob - ot))
        for (i in 0..4) {
            val cx = ox + ow * (0.15f + 0.175f * i)
            val fh = ((ob - ot) * (0.42f + 0.16f * sin(t * (4.2f + i * 1.3f) + i * 2f)) * (if (i == 2) 1.25f else 1f) * boost).coerceAtMost((ob - ot) * 0.95f)
            val fwid = ow * 0.11f; val sway = sin(t * (3f + i * 0.7f) + i) * fwid * 0.4f
            val flame = Path().apply { moveTo(cx - fwid, ob - h * 0.016f); quadraticTo(cx - fwid * 0.3f, ob - fh * 0.5f, cx + sway, ob - fh); quadraticTo(cx + fwid * 0.5f, ob - fh * 0.4f, cx + fwid, ob - h * 0.016f); close() }
            drawPath(flame, Color(0xFFFF7A2F))
            val inner = Path().apply { moveTo(cx - fwid * 0.5f, ob - h * 0.016f); quadraticTo(cx, ob - fh * 0.35f, cx + sway * 0.6f, ob - fh * 0.62f); quadraticTo(cx + fwid * 0.3f, ob - fh * 0.25f, cx + fwid * 0.5f, ob - h * 0.016f); close() }
            drawPath(inner, Color(0xFFFFD66B))
        }
        drawRoundRect(Color(0xFF6B4A33), Offset(ox + ow * 0.1f, ob - h * 0.026f), Size(ow * 0.8f, h * 0.022f), CornerRadius(6f))
    }
    drawPath(open, if (dark) Color(0xFF3B2A25) else Color(0xFF6B4A3A), style = Stroke(w * 0.01f))
    fp?.let { p ->                                  // sparks fly up when the fire is poked
        for (i in 0..21) {
            val k = ((p - noise(i) * 0.3f) / 0.65f).coerceIn(0f, 1f)
            if (k <= 0f || k >= 1f) continue
            val sx0 = ox + ow * (0.2f + 0.6f * noise(i, 1)); val sy0 = ob - (ob - ot) * 0.4f
            drawCircle(mixc(Color(0xFFFFE27A), Color(0xFFFF7A2F), k), w * 0.006f * (1.4f - k), Offset(sx0 + (noise(i, 2) - 0.5f) * w * 0.2f * k, sy0 - (0.14f + 0.22f * noise(i, 3)) * h * k + k * k * h * 0.04f), alpha = 1f - k)
        }
    }
    // the fish picture over the mantel: the fish swims away when tapped and a new one swims in
    val fishP = fx.p("fish", 2800)
    val fishX = when { fishP == null -> 0.5f; fishP < 0.35f -> 0.5f + fishP / 0.35f * 0.7f; fishP < 0.6f -> 1.4f; else -> -0.4f + (fishP - 0.6f) / 0.4f * 0.9f }
    pictureFrame(g.rect(0.355f, 0.095f, 0.505f, 0.245f), wood, 0f) { artFish(it, fishX, fishP?.takeIf { p -> p < 0.55f }, t) }
    // candle and plant on the mantel
    val cx0 = w * 0.335f; val cy0 = fT - h * 0.022f
    drawRoundRect(Color(0xFFFFF8E8), Offset(cx0 - w * 0.008f, cy0 - h * 0.05f), Size(w * 0.016f, h * 0.05f), CornerRadius(3f))
    val candle = fx.p("candle", 6000)
    val flameA = when { candle == null -> 1f; candle < 0.08f -> 1f - candle / 0.08f; candle < 0.55f -> 0f; candle < 0.65f -> (candle - 0.55f) / 0.1f; else -> 1f }
    if (flameA > 0f) {
        glow(Offset(cx0, cy0 - h * 0.065f), w * 0.07f, 0.3f * flameA)
        drawOval(Color(0xFFFFC94A), Offset(cx0 - w * 0.007f, cy0 - h * 0.084f + sin(t * 9f) * 1.2f), Size(w * 0.014f, h * 0.03f), alpha = flameA)
    } else if (candle != null && candle in 0.08f..0.5f) {
        for (i in 0..2) { val k = ((candle - 0.08f) * 2.4f - i * 0.25f).coerceIn(0f, 1f); drawCircle(Color.Gray, w * 0.007f * (1f + k), Offset(cx0 + sin(k * 8f + i) * w * 0.008f, cy0 - h * 0.055f - k * h * 0.07f), alpha = 0.5f * (1f - k)) }
    }
    val pp = fx.p("plant", 2400)
    plantPot(Offset(w * 0.525f, fT - h * 0.022f), w * 0.06f, swing(pp, 16f, 22f), Color(0xFF6DBB83), Color(0xFF4E9A66), Color(0xFFE08A64))
    pp?.let { p -> drawOval(Color(0xFF6DBB83), Offset(w * 0.545f + sin(p * 9f) * w * 0.02f, fT - h * 0.06f + p * h * 0.09f), Size(w * 0.016f, w * 0.03f), alpha = 1f - p) }

    // ---- rug with a pattern, floor plant, standing lamp behind the chair, rocking chair
    rug(g, 0.14f, 0.66f, 0.3f, 0.92f, if (dark) Color(0xFF7A3F58) else Color(0xFFD9708F), if (dark) Color(0xFF8E5470) else Color(0xFFF0A2B8), if (dark) Color(0xFFB07A94) else Color(0xFFC85B7D), if (dark) Color(0xFFB8A8A8) else Color(0xFFFFF0E4))
    fx.p("rug", 1500)?.let { dust(g, it) }
    plantPot(Offset(w * 0.075f, fy + h * 0.2f), w * 0.16f, swing(fx.p("fern", 2400), 12f, 20f) + sin(t * 0.8f) * 2f, Color(0xFF6DBB83), Color(0xFF4E9A66), Color(0xFFD98A64))

    // standing lamp: its base stands on the floor behind the chair
    val lampX = w * 0.885f; val lampBase = fy + h * 0.115f; val shadeTop = h * 0.29f
    if (lampOn) glow(Offset(lampX, shadeTop + h * 0.03f), w * 0.2f, if (dark) 0.5f else 0.32f)
    drawLine(Color(0xFF5B4A3B), Offset(lampX, shadeTop + h * 0.055f), Offset(lampX, lampBase), w * 0.009f, StrokeCap.Round)
    drawOval(Color(0xFF5B4A3B), Offset(lampX - w * 0.04f, lampBase - h * 0.01f), Size(w * 0.08f, h * 0.02f))
    val shade = Path().apply { moveTo(lampX - w * 0.045f, shadeTop + h * 0.055f); lineTo(lampX - w * 0.028f, shadeTop); lineTo(lampX + w * 0.028f, shadeTop); lineTo(lampX + w * 0.045f, shadeTop + h * 0.055f); close() }
    drawPath(shade, if (lampOn) Color(0xFFFFE9B8) else Color(0xFFCDBF9E))
    drawPath(shade, Color(0xFFE0B96B), style = Stroke(2f))

    // rocking chair in profile: fully on the floor, in front of the wall
    val chairBase = Offset(w * 0.695f, fy + h * 0.215f)
    val u = h * 0.0026f
    val sleeping = cat != null && cat.phase == 0
    val swingA = swing(fx.p("chair", 3400), 13f, 20f) + sin(t * 1.4f) * (if (sleeping) 3.5f else 1.8f)
    val onChair: (DrawScope.(Offset) -> Unit)? = if (sleeping) { seat -> drawCatAt(seat, minOf(w, h * 0.8f) * 0.29f, cat!!, CatAnim.SLEEP, t, -1f) } else null
    rockingChair(chairBase, u, swingA, wood, woodDark, if (dark) Color(0xFF8E5D75) else Color(0xFFF2A9BC), onChair)

    if (cat != null && cat.phase == 1) {
        val anim = when { fx.p("fire", 3000) != null || fx.p("fish", 3000) != null -> CatAnim.LOOK; fx.p("cat", 2200) != null -> CatAnim.PURR; (t / 4f).toInt() % 2 == 0 -> CatAnim.LOOK; else -> CatAnim.WAG }
        drawCatAt(g.floor(0.36f, 0.74f), minOf(w, h * 0.8f) * 0.3f, cat, anim, t, 1f)
    }
    fx.p("cat", 2200)?.let { p -> for (i in 0..2) heart(Offset(w * (0.3f + 0.07f * i) + sin(p * 8f + i) * 6f, h * 0.7f - p * h * 0.12f - i * 14f), w * 0.02f, 1f - p) }

    // darkness at night / lamp off, warm light from the fire on top
    drawRect(Color(0xFF0B1030), Offset.Zero, Size(w, h), alpha = (0.2f * night + (if (lampOn) 0f else 0.22f)).coerceIn(0f, 0.5f))
    drawRect(Brush.radialGradient(listOf(Color(0xFFFF9B4A).copy(alpha = 0.16f + 0.04f * sin(t * 5f)), Color.Transparent), Offset(w * 0.43f, fB), w * 0.6f), Offset.Zero, Size(w, h))
}

/** A rocking chair seen from the side: runners, turned legs, slatted back, armrest, cushions. [origin] = where the runner touches the floor. */
private fun DrawScope.rockingChair(origin: Offset, u: Float, swingDeg: Float, wood: Color, woodDark: Color, cushion: Color, occupant: (DrawScope.(Offset) -> Unit)?) {
    drawOval(Color.Black, Offset(origin.x - 46 * u, origin.y - 3 * u), Size(92 * u, 9 * u), alpha = 0.14f)
    rotate(swingDeg, pivot = origin) {
        fun p(x: Float, y: Float) = Offset(origin.x + x * u, origin.y + y * u)
        // runners
        val r0 = p(-48f, -7f); val rc = p(0f, 8f); val r1 = p(48f, -7f)
        drawPath(Path().apply { moveTo(r0.x, r0.y); quadraticTo(rc.x, rc.y, r1.x, r1.y) }, woodDark, style = Stroke(4.2f * u, cap = StrokeCap.Round))
        // turned legs
        for ((x0, x1) in listOf(-27f to -30f, 26f to 29f)) {
            drawLine(woodDark, p(x0, -52f), p(x1, -3f), 3.6f * u, StrokeCap.Round)
            for (k in 1..3) drawCircle(wood, 2.6f * u, p(x0 + (x1 - x0) * k / 4f, -52f + 49f * k / 4f))
        }
        drawLine(woodDark, p(-28f, -22f), p(27f, -22f), 2f * u, StrokeCap.Round)
        // back: two slanted posts with slats and a curved top rail
        for (off in listOf(0f, 7f)) drawLine(wood, p(-30f + off, -54f), p(-42f + off, -126f), 3.6f * u, StrokeCap.Round)
        for (k in 1..5) { val y = -54f - 12.5f * k; val x = -30f - 12f * (12.5f * k / 72f); drawLine(woodDark, p(x, y), p(x + 7f, y), 1.6f * u, StrokeCap.Round) }
        val t0 = p(-48f, -121f); val tc = p(-38f, -134f); val t1 = p(-30f, -121f)
        drawPath(Path().apply { moveTo(t0.x, t0.y); quadraticTo(tc.x, tc.y, t1.x, t1.y) }, wood, style = Stroke(5f * u, cap = StrokeCap.Round))
        // seat and cushion
        drawRoundRect(wood, p(-36f, -56f), Size(68f * u, 6f * u), CornerRadius(3f * u))
        drawRoundRect(cushion, p(-33f, -65f), Size(62f * u, 10f * u), CornerRadius(5f * u))
        for (k in 0..6) drawCircle(mixc(cushion, Color.Black, 0.25f), 0.9f * u, p(-28f + k * 9f, -61f))
        drawRoundRect(mixc(cushion, Color.White, 0.25f), p(-44f, -112f), Size(9f * u, 34f * u), CornerRadius(4.5f * u))   // head / back pillow
        drawOval(Color(0xFF7FC48B), p(-35f, -98f), Size(15f * u, 24f * u))                                                   // little green pillow
        // armrest with a scroll end and a front post
        drawLine(wood, p(-38f, -92f), p(26f, -88f), 3.4f * u, StrokeCap.Round)
        drawLine(woodDark, p(24f, -88f), p(24f, -56f), 3f * u, StrokeCap.Round)
        drawCircle(wood, 4f * u, p(28f, -88f)); drawCircle(woodDark, 1.8f * u, p(28f, -88f))
        occupant?.invoke(this, p(-3f, -64f))
        // a blanket draped over the front of the seat
        val a = p(10f, -66f); val b = p(34f, -66f); val c = p(36f, -44f); val c1 = p(28f, -38f); val c2 = p(18f, -46f); val d = p(10f, -40f)
        drawPath(Path().apply { moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); cubicTo(c1.x, c1.y, c2.x, c2.y, d.x, d.y); close() }, Color(0xFFFFD36B))
        for (k in 0..3) drawLine(Color.White, p(14f + k * 5.5f, -66f), p(14f + k * 5.5f, -42f), 1f * u, alpha = 0.4f)
    }
}

// ============================================================================ bedroom

private fun bedroomSpots(g: Geo): List<Hot> = listOf(
    Hot("lamp", g.rect(0.625f, 0.43f, 0.68f, 0.54f)),
    Hot("diffuser", g.rect(0.68f, 0.45f, 0.715f, 0.54f)),
    Hot("mug", g.rect(0.715f, 0.49f, 0.76f, 0.54f)),
    Hot("window", g.rect(0.615f, 0.185f, 0.765f, 0.335f)),
    Hot("pictures", g.rect(0.07f, 0.08f, 0.5f, 0.21f)),
    Hot("pillow", g.rect(0.09f, 0.45f, 0.215f, 0.54f)),
    Hot("blanket", g.rect(0.2f, 0.5f, 0.58f, 0.68f)),
    Hot("wardrobe", g.rect(0.78f, 0.24f, 0.97f, 0.68f)),
    Hot("slippers", g.rect(0.1f, 0.78f, 0.24f, 0.9f)),
    Hot("fern", g.rect(0.84f, 0.5f, 1.0f, 0.86f)),
    Hot("rug", g.rect(0.22f, 0.72f, 0.66f, 0.97f)),
)

private fun DrawScope.drawBedroom(g: Geo, t: Float, dark: Boolean, fx: Fx, lampOn: Boolean, night: Float, cat: Resident?) {
    val w = g.w; val h = g.h; val fy = g.floorY
    val pal = Palette(
        wall = if (dark) Color(0xFF3B4268) else Color(0xFFE3E5F8), wallDeep = if (dark) Color(0xFF2B314C) else Color(0xFFBFC5E6),
        floorA = if (dark) Color(0xFF45392F) else Color(0xFFD2A878), floorB = if (dark) Color(0xFF332B25) else Color(0xFFB08357),
        trim = if (dark) Color(0xFF5E5870) else Color(0xFFFFFBF4), stripe = Color.White,
        wood = if (dark) Color(0xFF6B4A33) else Color(0xFFA9774A), woodDark = if (dark) Color(0xFF55392A) else Color(0xFF7E5535),
    )
    val wood = pal.wood; val woodDark = pal.woodDark
    shell(g, pal, dark)

    // ---- round window with the real sky (above the nightstand)
    val wc = Offset(w * 0.69f, h * 0.26f); val wr = w * 0.075f
    val win = Rect(wc.x - wr, wc.y - wr, wc.x + wr, wc.y + wr)
    drawCircle(woodDark, wr + w * 0.012f, wc)
    clipPath(Path().apply { addOval(win) }) { drawSky(win, t, 2, fx.p("window", 2200)) }
    drawLine(woodDark, Offset(wc.x - wr, wc.y), Offset(wc.x + wr, wc.y), w * 0.007f); drawLine(woodDark, Offset(wc.x, wc.y - wr), Offset(wc.x, wc.y + wr), w * 0.007f)
    drawOval(Color.White, Offset(wc.x - wr * 0.7f, wc.y - wr * 0.8f), Size(wr * 0.5f, wr * 0.25f), alpha = 0.18f)
    drawRoundRect(wood, Offset(wc.x - wr * 1.15f, wc.y + wr + w * 0.006f), Size(wr * 2.3f, h * 0.012f), CornerRadius(3f))
    val day = 1f - night
    val beam = listOf(Offset(w * 0.63f, fy + h * 0.015f), Offset(w * 0.76f, fy + h * 0.015f), Offset(w * 0.8f, fy + h * 0.17f), Offset(w * 0.6f, fy + h * 0.17f))
    poly(Color(0xFFFFF0B0), beam[0], beam[1], beam[2], beam[3], alpha = 0.12f * day)
    poly(Color(0xFFBFD0FF), beam[0], beam[1], beam[2], beam[3], alpha = 0.1f * night)

    // ---- pictures above the bed (they swing when tapped)
    val sw = fx.p("pictures", 2600)
    pictureFrame(g.rect(0.08f, 0.09f, 0.17f, 0.2f), wood, swing(sw, 10f, 17f)) { artHills(it, Color(0xFFF7D9E4), Color(0xFFA6D8A1), Color(0xFF86C48C), Color(0xFFFFE08A)) }
    pictureFrame(g.rect(0.2f, 0.08f, 0.33f, 0.18f), wood, swing(sw, -9f, 15f)) { artFish(it, 0.5f, null, t) }
    pictureFrame(g.rect(0.36f, 0.1f, 0.45f, 0.21f), wood, swing(sw, 11f, 19f)) { artHills(it, Color(0xFFC7E6F7), Color(0xFF8BCB93), Color(0xFF68B27B), Color(0xFFFF9E7A)) }

    // ---- wardrobe against the right wall: tap and the left door swings open, then closes again
    val wx0 = w * 0.79f; val wx1 = w * 0.965f; val wt = h * 0.25f; val wb = fy + h * 0.05f
    drawRect(Color.Black, Offset(wx0 + 4f, wt + 4f), Size(wx1 - wx0, wb - wt), alpha = 0.12f)
    drawRoundRect(woodDark, Offset(wx0 - w * 0.01f, wt - h * 0.012f), Size(wx1 - wx0 + w * 0.02f, wb - wt + h * 0.012f), CornerRadius(5f))
    val wp = fx.p("wardrobe", 3400)
    val openK = if (wp == null) 0f else when { wp < 0.15f -> wp / 0.15f; wp < 0.8f -> 1f; else -> 1f - (wp - 0.8f) / 0.2f }
    val dw = (wx1 - wx0) / 2f
    // inside: a shelf and hanging clothes
    drawRect(Color(0xFF2E2230), Offset(wx0, wt), Size(dw, wb - wt - h * 0.02f))
    drawLine(Color(0xFFC9A57A), Offset(wx0 + 4f, wt + h * 0.04f), Offset(wx0 + dw - 4f, wt + h * 0.04f), 2f)
    for ((i, col) in listOf(Color(0xFFE79DB3), Color(0xFF9CD3E8), Color(0xFFFFD36B)).withIndex()) drawRoundRect(col, Offset(wx0 + 6f + i * dw * 0.3f, wt + h * 0.045f), Size(dw * 0.22f, h * 0.2f), CornerRadius(4f), alpha = 0.9f)
    // doors
    val doorCol = wood
    val leftScale = 1f - 0.78f * openK
    scale(leftScale, 1f, pivot = Offset(wx0, wt)) {
        drawRoundRect(doorCol, Offset(wx0, wt), Size(dw - 1.5f, wb - wt - h * 0.02f), CornerRadius(4f))
        drawRoundRect(woodDark, Offset(wx0 + dw * 0.12f, wt + h * 0.025f), Size(dw * 0.74f, wb - wt - h * 0.07f), CornerRadius(3f), style = Stroke(2f))
        drawCircle(Color(0xFFF1D79B), w * 0.007f, Offset(wx0 + dw * 0.86f, (wt + wb) / 2))
    }
    drawRoundRect(doorCol, Offset(wx0 + dw + 1.5f, wt), Size(dw - 1.5f, wb - wt - h * 0.02f), CornerRadius(4f))
    drawRoundRect(woodDark, Offset(wx0 + dw + dw * 0.14f, wt + h * 0.025f), Size(dw * 0.74f, wb - wt - h * 0.07f), CornerRadius(3f), style = Stroke(2f))
    drawCircle(Color(0xFFF1D79B), w * 0.007f, Offset(wx0 + dw + dw * 0.14f, (wt + wb) / 2))
    for (lx in listOf(wx0 + 4f, wx1 - w * 0.02f)) drawRoundRect(woodDark, Offset(lx, wb - h * 0.025f), Size(w * 0.016f, h * 0.03f), CornerRadius(3f))
    drawRoundRect(wood, Offset(wx0 - w * 0.015f, wt - h * 0.02f), Size(wx1 - wx0 + w * 0.03f, h * 0.014f), CornerRadius(4f))

    // ---- rug, slippers, floor plant
    rug(g, 0.2f, 0.7f, 0.34f, 0.94f, if (dark) Color(0xFF4F4378) else Color(0xFF9A86D6), if (dark) Color(0xFF6A5C91) else Color(0xFFC4B6F0), if (dark) Color(0xFF8E7FC0) else Color(0xFF7F69C4), if (dark) Color(0xFFB8B0D0) else Color(0xFFF4EFFF))
    fx.p("rug", 1500)?.let { dust(g, it) }
    val slip = swing(fx.p("slippers", 1800), 18f, 24f)
    for (i in 0..1) {
        val c = g.floor(0.12f + 0.06f * i, 0.62f)
        rotate(slip * (if (i == 0) 1 else -1), pivot = c) {
            drawOval(Color(0xFFFFB3C7), Offset(c.x - w * 0.022f, c.y - h * 0.012f), Size(w * 0.05f, h * 0.026f))
            drawOval(Color(0xFFFF8FB0), Offset(c.x - w * 0.002f, c.y - h * 0.014f), Size(w * 0.026f, h * 0.022f))
        }
    }
    plantPot(Offset(w * 0.925f, fy + h * 0.2f), w * 0.15f, swing(fx.p("fern", 2400), 12f, 20f) + sin(t * 0.8f) * 2f, Color(0xFF6DBB83), Color(0xFF4E9A66), Color(0xFF8E7FC0))

    // ---- nightstand with lamp, scent diffuser and a steaming mug (stands on the floor, against the wall)
    val nx0 = w * 0.62f; val nx1 = w * 0.76f; val ny0 = h * 0.54f; val ny1 = fy + h * 0.05f
    drawRect(Color.Black, Offset(nx0 + 4f, ny0 + 4f), Size(nx1 - nx0, ny1 - ny0), alpha = 0.12f)
    drawRoundRect(wood, Offset(nx0, ny0), Size(nx1 - nx0, ny1 - ny0), CornerRadius(5f))
    drawRoundRect(woodDark, Offset(nx0 - 4f, ny0 - h * 0.008f), Size(nx1 - nx0 + 8f, h * 0.012f), CornerRadius(4f))
    drawRect(woodDark, Offset(nx0 + 6f, ny0 + (ny1 - ny0) * 0.5f), Size(nx1 - nx0 - 12f, 2f))
    drawCircle(Color(0xFF5B4A3B), w * 0.007f, Offset((nx0 + nx1) / 2, ny0 + (ny1 - ny0) * 0.28f)); drawCircle(Color(0xFF5B4A3B), w * 0.007f, Offset((nx0 + nx1) / 2, ny0 + (ny1 - ny0) * 0.75f))
    val top = ny0 - h * 0.008f
    val lc = Offset(w * 0.65f, top)
    if (lampOn) glow(Offset(lc.x, lc.y - h * 0.06f), w * 0.22f, if (dark) 0.55f else 0.4f)
    drawLine(Color(0xFF5B4A3B), Offset(lc.x, lc.y), Offset(lc.x, lc.y - h * 0.045f), w * 0.007f, StrokeCap.Round)
    drawOval(Color(0xFF5B4A3B), Offset(lc.x - w * 0.016f, lc.y - h * 0.004f), Size(w * 0.032f, h * 0.008f))
    val shade = Path().apply { moveTo(lc.x - w * 0.03f, lc.y - h * 0.045f); lineTo(lc.x - w * 0.018f, lc.y - h * 0.09f); lineTo(lc.x + w * 0.018f, lc.y - h * 0.09f); lineTo(lc.x + w * 0.03f, lc.y - h * 0.045f); close() }
    drawPath(shade, if (lampOn) Color(0xFFFFE9B8) else Color(0xFFCDBF9E)); drawPath(shade, Color(0xFFE0B96B), style = Stroke(2f))
    // reed diffuser: a little bottle with sticks; scent curls rise when tapped
    val dc = Offset(w * 0.693f, top)
    drawRoundRect(Color(0xFFBFE3F5), Offset(dc.x - w * 0.014f, dc.y - h * 0.036f), Size(w * 0.028f, h * 0.036f), CornerRadius(5f), alpha = 0.95f)
    drawRoundRect(Color(0xFFE79DB3), Offset(dc.x - w * 0.012f, dc.y - h * 0.024f), Size(w * 0.024f, h * 0.022f), CornerRadius(4f), alpha = 0.7f)
    for (k in -1..1) drawLine(Color(0xFFC9A57A), Offset(dc.x + k * w * 0.002f, dc.y - h * 0.036f), Offset(dc.x + k * w * 0.01f, dc.y - h * 0.07f), 1.4f)
    val dp = fx.p("diffuser", 7000)
    if (dp != null) {
        val a = (if (dp < 0.12f) dp / 0.12f else if (dp > 0.85f) (1f - dp) / 0.15f else 1f)
        val cols = listOf(Color(0xFFE79DB3), Color(0xFFB59BE0), Color(0xFFFFD36B), Color(0xFF9CD3E8), Color(0xFFFF9EC4))
        for (i in 0..4) {
            val path = Path()
            val x0 = dc.x + (i - 2) * w * 0.012f
            for (s in 0..24) {
                val k = s / 24f
                val yy = dc.y - h * 0.075f - k * h * 0.2f
                val xx = x0 + sin(k * 9f + t * 2f + i) * w * 0.02f * (0.4f + k)
                if (s == 0) path.moveTo(xx, yy) else path.lineTo(xx, yy)
            }
            drawPath(path, cols[i], alpha = 0.55f * a, style = Stroke(w * 0.007f, cap = StrokeCap.Round))
        }
    }
    // mug
    val mc = Offset(w * 0.735f, top)
    drawRoundRect(Color(0xFFFFFFFF), Offset(mc.x - w * 0.014f, mc.y - h * 0.028f), Size(w * 0.028f, h * 0.028f), CornerRadius(4f))
    drawRoundRect(Color(0xFFE79DB3), Offset(mc.x - w * 0.014f, mc.y - h * 0.02f), Size(w * 0.028f, h * 0.006f), CornerRadius(2f))
    drawArc(Color.White, -90f, 180f, false, Offset(mc.x + w * 0.008f, mc.y - h * 0.024f), Size(w * 0.016f, h * 0.018f), style = Stroke(2f))
    val big = if (fx.p("mug", 3000) != null) 1.8f else 0.8f
    for (i in 0..2) {
        val k = ((t * 0.5f + i * 0.33f) % 1f)
        drawCircle(Color.White, w * 0.004f * (1f + k) * big, Offset(mc.x + (i - 1) * w * 0.006f + sin(k * 6f + i) * w * 0.006f * big, mc.y - h * 0.03f - k * h * 0.05f * big), alpha = 0.5f * (1f - k))
    }

    // ---- the bed in side view: headboard against the wall, frame, mattress, blanket you can pull back, pillow
    val bb = fy + h * 0.075f                                 // the bed's feet stand on the floor, a bit in front of the wall
    val bx0 = w * 0.035f; val bx1 = w * 0.575f
    val mTop = bb - h * 0.165f; val mBot = bb - h * 0.085f
    drawOval(Color.Black, Offset(bx0, bb - h * 0.012f), Size(bx1 - bx0 + w * 0.02f, h * 0.03f), alpha = 0.13f)
    // headboard with slats and an arched top, footboard
    drawRoundRect(woodDark, Offset(bx0, h * 0.33f), Size(w * 0.055f, bb - h * 0.33f), CornerRadius(w * 0.02f))
    drawRoundRect(wood, Offset(bx0 + w * 0.006f, h * 0.345f), Size(w * 0.043f, bb - h * 0.37f), CornerRadius(w * 0.015f))
    for (i in 0..4) drawRect(woodDark, Offset(bx0 + w * 0.012f, h * 0.38f + i * h * 0.045f), Size(w * 0.031f, h * 0.008f), alpha = 0.7f)
    drawCircle(woodDark, w * 0.01f, Offset(bx0 + w * 0.0275f, h * 0.33f))
    drawRoundRect(woodDark, Offset(bx1 - w * 0.01f, bb - h * 0.2f), Size(w * 0.03f, h * 0.2f), CornerRadius(w * 0.012f))
    drawCircle(wood, w * 0.011f, Offset(bx1 + w * 0.005f, bb - h * 0.205f))
    // frame rail + legs
    drawRoundRect(woodDark, Offset(bx0 + w * 0.03f, mBot), Size(bx1 - bx0 - w * 0.03f, h * 0.045f), CornerRadius(6f))
    drawRoundRect(wood, Offset(bx0 + w * 0.03f, mBot), Size(bx1 - bx0 - w * 0.03f, h * 0.014f), CornerRadius(6f))
    for (lx in listOf(bx0 + w * 0.045f, bx1 - w * 0.035f)) drawRoundRect(woodDark, Offset(lx, mBot + h * 0.04f), Size(w * 0.018f, bb - mBot - h * 0.04f), CornerRadius(4f))
    // mattress with a bit of sheet
    drawRoundRect(Color(0xFFFFFBF2), Offset(bx0 + w * 0.05f, mTop), Size(bx1 - bx0 - w * 0.055f, mBot - mTop + h * 0.006f), CornerRadius(w * 0.012f))
    drawRoundRect(Color(0xFFE9E2D2), Offset(bx0 + w * 0.05f, mBot - h * 0.016f), Size(bx1 - bx0 - w * 0.055f, h * 0.022f), CornerRadius(w * 0.01f))
    // blanket: its head end slides along the bed when you tap it, then slides back by itself
    val bp = fx.p("blanket", 5200)
    val pull = if (bp == null) 0f else when {
        bp < 0.18f -> 1f - (1f - bp / 0.18f).pow(3)
        bp < 0.7f -> 1f
        else -> { val k = (bp - 0.7f) / 0.3f; 1f - k * k * (3f - 2f * k) }
    }
    val bl0 = w * 0.215f + w * 0.24f * pull; val bl1 = bx1 - w * 0.005f
    val blTop = mTop - h * 0.012f
    val blanketCol = if (dark) Color(0xFF7C5A96) else Color(0xFFB59BE0)
    val drapeBottom = mBot + h * 0.055f
    drawRoundRect(blanketCol, Offset(bl0, blTop), Size(bl1 - bl0, drapeBottom - blTop), CornerRadius(w * 0.012f))
    for (k in 1..9) drawLine(Color.White, Offset(bl0 + (bl1 - bl0) * k / 10f, blTop + 3f), Offset(bl0 + (bl1 - bl0) * k / 10f, drapeBottom - 3f), w * 0.011f, alpha = 0.22f)
    drawRoundRect(mixc(blanketCol, Color.White, 0.55f), Offset(bl0, blTop), Size(w * 0.04f, mBot - blTop), CornerRadius(w * 0.012f))        // the turned-down sheet at the head end
    for (k in 0..8) drawCircle(blanketCol, (bl1 - bl0) / 20f, Offset(bl0 + (bl1 - bl0) * (k + 0.5f) / 9f, drapeBottom - (bl1 - bl0) / 20f * 0.3f))   // scalloped hem
    // pillow (squishes when tapped)
    val pil = fx.p("pillow", 1700)
    val sq = if (pil == null) 0f else bell((pil * 1.5f).coerceAtMost(1f))
    val pc = Offset(bx0 + w * 0.115f, mTop - h * 0.02f)
    scale(1f + 0.2f * sq, 1f - 0.45f * sq, pivot = Offset(pc.x, mTop)) {
        drawRoundRect(Color.White, Offset(pc.x - w * 0.065f, pc.y - h * 0.026f), Size(w * 0.13f, h * 0.06f), CornerRadius(w * 0.022f))
        drawRoundRect(Color(0xFFDDD6EE), Offset(pc.x - w * 0.065f, pc.y - h * 0.026f), Size(w * 0.13f, h * 0.06f), CornerRadius(w * 0.022f), style = Stroke(1.6f))
        drawLine(Color(0xFFDDD6EE), Offset(pc.x - w * 0.04f, pc.y + h * 0.004f), Offset(pc.x + w * 0.04f, pc.y + h * 0.004f), 1.2f, alpha = 0.7f)
    }
    if (pil != null) for (f in 0..3) { val k = (pil - 0.05f * f).coerceIn(0f, 1f); drawOval(Color.White, Offset(pc.x + (f - 1.5f) * w * 0.03f + sin(k * 7f + f) * 6f, pc.y - k * h * 0.12f), Size(w * 0.012f, w * 0.022f), alpha = 1f - k) }

    // ---- the cat sleeps on the blanket – or sits on the rug; the diffuser lures it to the nightstand
    if (cat != null) {
        val s = minOf(w, h * 0.8f) * 0.3f
        val dpv = fx.p("diffuser", 7000)
        val base = if (cat.phase == 0) Offset(w * 0.4f, blTop + h * 0.004f) else g.floor(0.44f, 0.72f)
        val target = Offset(w * 0.68f, fy + h * 0.115f)
        if (dpv != null) {
            val k = when { dpv < 0.12f -> dpv / 0.12f; dpv > 0.88f -> (1f - dpv) / 0.12f; else -> 1f }
            val pos = lerpOffset(base, target, k * k * (3f - 2f * k))
            val moving = dpv < 0.12f || dpv > 0.88f
            drawCatAt(pos, s, cat, if (moving) CatAnim.WALK else if ((t * 2f).toInt() % 2 == 0) CatAnim.TILT else CatAnim.LOOK, t, if (target.x > base.x) 1f else -1f)
            if (!moving) for (i in 0..1) heart(Offset(pos.x + w * 0.05f + i * w * 0.03f, pos.y - s * 1.0f - ((t * 0.6f + i * 0.5f) % 1f) * h * 0.06f), w * 0.016f, 0.9f)
        } else {
            val anim = when { fx.p("cat", 2200) != null -> CatAnim.PURR; fx.p("blanket", 5200) != null && cat.phase == 0 -> CatAnim.LOOK; cat.phase == 0 -> CatAnim.SLEEP; else -> CatAnim.LOOK }
            drawCatAt(base, s * (if (cat.phase == 0) 0.95f else 1f), cat, anim, t, if (cat.phase == 0) 1f else -1f)
        }
    }
    fx.p("cat", 2200)?.let { p -> for (i in 0..2) heart(Offset(w * (0.3f + 0.07f * i) + sin(p * 8f + i) * 6f, h * 0.5f - p * h * 0.12f - i * 14f), w * 0.02f, 1f - p) }

    // darkness at night / lamp off
    drawRect(Color(0xFF0B1030), Offset.Zero, Size(w, h), alpha = (0.22f * night + (if (lampOn) 0f else 0.22f)).coerceIn(0f, 0.5f))
    if (lampOn) drawRect(Brush.radialGradient(listOf(Color(0xFFFFD58A).copy(alpha = 0.14f), Color.Transparent), Offset(w * 0.64f, h * 0.45f), w * 0.5f), Offset.Zero, Size(w, h))
}
