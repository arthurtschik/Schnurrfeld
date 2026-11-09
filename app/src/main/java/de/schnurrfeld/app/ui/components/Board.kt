package de.schnurrfeld.app.ui.components

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import de.schnurrfeld.app.game.Cell
import de.schnurrfeld.app.game.GameController
import de.schnurrfeld.app.game.Puzzle
import de.schnurrfeld.app.game.Status
import de.schnurrfeld.app.ui.theme.Furs
import de.schnurrfeld.app.ui.theme.LocalDark
import de.schnurrfeld.app.ui.theme.RegionPalette
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun rememberFrameMillis(): State<Long> {
    val s = remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { }
            s.longValue = SystemClock.uptimeMillis()
        }
    }
    return s
}

@Composable
fun BoardView(ctrl: GameController, colorAssist: Boolean, modifier: Modifier = Modifier) {
    val puzzle = ctrl.puzzle ?: return
    val dark = LocalDark.current
    val frame = rememberFrameMillis()
    val n = puzzle.size

    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (dark) Color(0xFF1C1C20) else Color(0xFFFFFDF9))
            .padding(4.dp)
            .pointerInput(puzzle) {
                fun cellAt(o: Offset): Int? {
                    val cs = size.width.toFloat() / n
                    val c = floor(o.x / cs).toInt(); val r = floor(o.y / cs).toInt()
                    return if (r in 0 until n && c in 0 until n) r * n + c else null
                }
                detectTapGestures(
                    onTap = { o -> cellAt(o)?.let(ctrl::tap) },
                    onLongPress = { o -> cellAt(o)?.let(ctrl::longPress) },
                )
            }
            .pointerInput(puzzle) {
                fun cellAt(o: Offset): Int? {
                    val cs = size.width.toFloat() / n
                    val c = floor(o.x / cs).toInt(); val r = floor(o.y / cs).toInt()
                    return if (r in 0 until n && c in 0 until n) r * n + c else null
                }
                var last = -1
                detectDragGestures(
                    onDragStart = { o -> cellAt(o)?.let { last = it; ctrl.dragStart(it) } },
                    onDrag = { change, _ ->
                        val c = cellAt(change.position)
                        if (c != null && c != last) { last = c; ctrl.dragOver(c) }
                        change.consume()
                    },
                    onDragEnd = { last = -1 },
                )
            }
    ) {
        // static layer: only redraws when cells change
        Canvas(Modifier.fillMaxSize()) {
            drawBoard(puzzle, ctrl.cells, dark, colorAssist)
        }
        // animated layer
        Canvas(Modifier.fillMaxSize()) {
            drawOverlay(puzzle, ctrl, frame.value, dark)
        }
    }
}

/** Geometry of one cell: rounded tile inset by half the gap. */
private class CellGeo(val l: Float, val t: Float, val s: Float, val radius: Float)

private fun DrawScope.cellGeo(cell: Int, n: Int): CellGeo {
    val pitch = size.width / n
    val gap = (if (n >= 10) 2.5f else 3.5f) * density
    val s = pitch - gap
    return CellGeo((cell % n) * pitch + gap / 2, (cell / n) * pitch + gap / 2, s, s * 0.18f)
}

private fun DrawScope.drawBoard(p: Puzzle, cells: List<Int>, dark: Boolean, assist: Boolean) {
    val n = p.size
    val patternColor = if (dark) Color.White else Color.Black
    val patternAlpha = when {
        assist -> if (dark) 0.32f else 0.28f
        dark -> 0.14f
        else -> 0.11f
    }

    for (cell in 0 until n * n) {
        val g = cellGeo(cell, n)
        val slot = p.colorOf[p.regions[cell]]
        drawRoundRect(
            RegionPalette.color(slot, dark), Offset(g.l, g.t), Size(g.s, g.s), CornerRadius(g.radius)
        )
        // pattern sits inside the tile with a margin all around
        val m = g.s * 0.16f
        val inner = g.s - 2 * m
        val clip = Path().apply {
            addRoundRect(RoundRect(g.l + m, g.t + m, g.l + m + inner, g.t + m + inner, CornerRadius(inner * 0.14f)))
        }
        clipPath(clip) {
            val pw = (if (assist) 1.6f else 1.1f) * density
            drawPattern(RegionPalette.pattern(slot), g.l + m, g.t + m, inner, n, patternColor, patternAlpha, pw)
        }
    }

    // X marks: thick, softly rounded white strokes with a contour in a darker shade of the tile colour
    val pitch = size.width / n
    for (cell in 0 until n * n) {
        val st = cells.getOrNull(cell) ?: continue
        if (st != Cell.X && st != Cell.AUTO_X) continue
        val tile = RegionPalette.color(p.colorOf[p.regions[cell]], dark)
        drawCross(
            Offset((cell % n + 0.5f) * pitch, (cell / n + 0.5f) * pitch), pitch,
            if (st == Cell.AUTO_X) 0.7f else 1f, contour = crossContour(tile)
        )
    }
}

/** Darker shade of the tile colour – frames the white cross without a harsh black edge. */
private fun crossContour(tile: Color) = androidx.compose.ui.graphics.lerp(tile, Color(0xFF1E1A24), 0.55f)

/** A thick rounded white cross with a soft contour. [strength] < 1 draws it lighter (auto crosses, hint previews). */
private fun DrawScope.drawCross(c: Offset, pitch: Float, strength: Float, color: Color = Color.White, contour: Color? = null) {
    val a = pitch * 0.17f
    val w = pitch * 0.11f
    val edge = (pitch * 0.028f).coerceAtLeast(1.2f * density)
    val lines = listOf(Offset(-a, -a) to Offset(a, a), Offset(-a, a) to Offset(a, -a))
    // contour: a slightly wider stroke underneath, so the white never blends into light tiles
    for ((p1, p2) in lines) {
        drawLine(contour ?: Color(0xFF2B2A33), c + p1, c + p2, w + 2 * edge, StrokeCap.Round, alpha = 0.55f * strength)
    }
    for ((p1, p2) in lines) {
        drawLine(color, c + p1, c + p2, w, StrokeCap.Round, alpha = 0.97f * strength)
    }
}

/**
 * Motif patterns in tile-local coordinates. Each colour gets its own small symbol,
 * arranged either in a grid or in a staggered "dice five" layout.
 */
private fun DrawScope.drawPattern(kind: Int, x0: Float, y0: Float, s: Float, n: Int, col: Color, a: Float, w: Float) {
    val big = s >= 30f * density
    val fill = a * 1.25f

    // positions: grid k×k, or quincunx (4 corners + centre)
    fun grid(k: Int) = buildList {
        val st = s / k
        for (j in 0 until k) for (i in 0 until k) add(Offset(x0 + (i + 0.5f) * st, y0 + (j + 0.5f) * st))
    }
    fun quincunx() = listOf(
        Offset(x0 + s * 0.22f, y0 + s * 0.22f), Offset(x0 + s * 0.78f, y0 + s * 0.22f),
        Offset(x0 + s * 0.5f, y0 + s * 0.5f),
        Offset(x0 + s * 0.22f, y0 + s * 0.78f), Offset(x0 + s * 0.78f, y0 + s * 0.78f),
    )
    fun stagger(k: Int) = buildList {  // rows shifted by half a step
        val st = s / k
        for (j in 0 until k) for (i in 0..k) {
            val x = x0 + (i + if (j % 2 == 0) 0.5f else 0f) * st
            if (x > x0 + st * 0.2f && x < x0 + s - st * 0.2f) add(Offset(x, y0 + (j + 0.5f) * st))
        }
    }
    val dense = if (big) 4 else 3
    val sparse = if (big) 3 else 2

    fun star(c: Offset, r: Float, points: Int, inner: Float) = Path().apply {
        for (i in 0 until points * 2) {
            val ang = -PI.toFloat() / 2 + i * PI.toFloat() / points
            val rr = if (i % 2 == 0) r else r * inner
            val p = Offset(c.x + cos(ang) * rr, c.y + sin(ang) * rr)
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }

    when (kind) {
        0 -> grid(dense).forEach { drawCircle(col, s / dense * 0.13f, it, alpha = fill) }               // dots
        1 -> quincunx().forEach { c ->                                                                 // hearts
            val r = s * 0.11f
            val h = Path().apply {
                moveTo(c.x, c.y + r * 0.85f)
                cubicTo(c.x - r * 1.35f, c.y - r * 0.1f, c.x - r * 0.65f, c.y - r * 1.15f, c.x, c.y - r * 0.35f)
                cubicTo(c.x + r * 0.65f, c.y - r * 1.15f, c.x + r * 1.35f, c.y - r * 0.1f, c.x, c.y + r * 0.85f)
                close()
            }
            drawPath(h, col, alpha = fill)
        }
        2 -> quincunx().forEach { drawPath(star(it, s * 0.12f, 5, 0.45f), col, alpha = fill) }        // stars
        3 -> grid(sparse).forEach { drawCircle(col, s / sparse * 0.2f, it, alpha = a * 1.2f, style = Stroke(w * 1.1f)) } // rings
        4 -> grid(sparse).forEach { c ->                                                               // paw prints
            val r = s / sparse * 0.26f
            drawOval(col, Offset(c.x - r * 0.55f, c.y - r * 0.05f), Size(r * 1.1f, r * 0.85f), alpha = fill)
            for ((dx, dy) in listOf(-0.62f to -0.3f, -0.22f to -0.72f, 0.22f to -0.72f, 0.62f to -0.3f))
                drawCircle(col, r * 0.2f, Offset(c.x + dx * r, c.y + dy * r), alpha = fill)
        }
        5 -> grid(sparse).forEach { c ->                                                               // little flowers
            val r = s / sparse * 0.15f
            for ((dx, dy) in listOf(0f to -1f, 1f to 0f, 0f to 1f, -1f to 0f))
                drawCircle(col, r * 0.62f, Offset(c.x + dx * r * 0.8f, c.y + dy * r * 0.8f), alpha = a)
            drawCircle(col, r * 0.42f, c, alpha = fill * 1.2f)
        }
        6 -> stagger(dense).forEach { c ->                                                             // triangles
            val r = s / dense * 0.2f
            val t = Path().apply { moveTo(c.x, c.y - r); lineTo(c.x + r * 0.95f, c.y + r * 0.7f); lineTo(c.x - r * 0.95f, c.y + r * 0.7f); close() }
            drawPath(t, col, alpha = fill)
        }
        7 -> grid(sparse).forEach { c ->                                                               // crescent moons
            val r = s / sparse * 0.22f
            val moon = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(c, r))
                val cut = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(c.x + r * 0.45f, c.y - r * 0.3f), r * 0.85f)) }
                op(this, cut, androidx.compose.ui.graphics.PathOperation.Difference)
            }
            drawPath(moon, col, alpha = fill)
        }
        8 -> { val k = sparse; val st = s / k; val amp = st * 0.16f                                     // soft waves
            for (j in 0 until k) {
                val y = y0 + (j + 0.5f) * st
                val path = Path(); var x = x0; path.moveTo(x, y)
                while (x < x0 + s) { x += 1.5f; path.lineTo(x, y + sin((x - x0) / st * 2 * PI.toFloat()) * amp) }
                drawPath(path, col, alpha = a * 1.2f, style = Stroke(w * 1.2f, cap = StrokeCap.Round))
            }
        }
        9 -> stagger(dense).forEach { c ->                                                             // diamonds
            val d = s / dense * 0.2f
            val path = Path().apply { moveTo(c.x, c.y - d); lineTo(c.x + d * 0.75f, c.y); lineTo(c.x, c.y + d); lineTo(c.x - d * 0.75f, c.y); close() }
            drawPath(path, col, alpha = fill)
        }
        10 -> quincunx().forEach { drawPath(star(it, s * 0.12f, 4, 0.3f), col, alpha = fill) }        // sparkles
        else -> grid(sparse).forEach { c ->                                                            // little crosses
            val arm = s / sparse * 0.15f
            drawLine(col, Offset(c.x - arm, c.y), Offset(c.x + arm, c.y), w * 1.4f, StrokeCap.Round, alpha = a * 1.3f)
            drawLine(col, Offset(c.x, c.y - arm), Offset(c.x, c.y + arm), w * 1.4f, StrokeCap.Round, alpha = a * 1.3f)
        }
    }
}

private fun DrawScope.drawOverlay(p: Puzzle, ctrl: GameController, now: Long, dark: Boolean) {
    val n = p.size
    val cs = size.width / n

    ctrl.hint?.let { (h, at) ->
        val age = now - at
        val pulse = 0.5f + 0.5f * sin(age / 220f)
        val gold = Color(0xFFF5B83D)
        // the cells the explanation is about: glowing outline
        for (cell in h.focus) {
            val g = cellGeo(cell, n)
            drawRoundRect(Color.White, Offset(g.l, g.t), Size(g.s, g.s), CornerRadius(g.radius), alpha = 0.18f + 0.12f * pulse)
            drawRoundRect(
                gold, Offset(g.l + g.s * 0.04f, g.t + g.s * 0.04f), Size(g.s * 0.92f, g.s * 0.92f), CornerRadius(g.radius),
                alpha = 0.75f + 0.25f * pulse, style = Stroke(cs * 0.055f)
            )
        }
        // cells that may get a cross: pulsing ghost crosses
        for (cell in h.crosses) if (ctrl.cells.getOrNull(cell) == Cell.EMPTY) {
            drawCross(
                Offset((cell % n + 0.5f) * cs, (cell / n + 0.5f) * cs), cs, 0.35f + 0.35f * pulse,
                contour = crossContour(RegionPalette.color(p.colorOf[p.regions[cell]], dark))
            )
        }
        h.removeCross?.let { cell ->
            val c = Offset((cell % n + 0.5f) * cs, (cell / n + 0.5f) * cs)
            drawCircle(Color(0xFFE5484D), cs * (0.36f + 0.04f * pulse), c, alpha = 0.9f, style = Stroke(cs * 0.06f))
        }
        h.catCell?.let { cell ->
            if (ctrl.cells.getOrNull(cell) != Cell.CAT) {
                val c = Offset((cell % n + 0.5f) * cs, (cell / n + 0.5f) * cs)
                drawCircle(Color.White, cs * (0.3f + 0.07f * pulse), c, alpha = 0.95f, style = Stroke(cs * 0.08f))
                drawCircle(gold, cs * (0.3f + 0.07f * pulse), c, alpha = 0.95f, style = Stroke(cs * 0.045f))
            }
        }
    }

    // beginner help: the cell the helper cat points at wiggles (in short bursts)
    ctrl.nudge?.let { (cellsToWiggle, at) ->
        val age = now - at
        val burst = (age % 1800) < 700
        val wob = if (burst) sin(age / 45f) * cs * 0.06f else 0f
        val glow = 0.55f + 0.45f * sin(age / 260f)
        for (cell in cellsToWiggle) {
            val g = cellGeo(cell, n)
            rotate(if (burst) sin(age / 45f) * 6f else 0f, Offset(g.l + g.s / 2, g.t + g.s / 2)) {
                drawRoundRect(Color.White, Offset(g.l + wob, g.t), Size(g.s, g.s), CornerRadius(g.radius), alpha = 0.35f * glow)
                drawRoundRect(Color(0xFFF5B83D), Offset(g.l + wob + g.s * 0.04f, g.t + g.s * 0.04f), Size(g.s * 0.92f, g.s * 0.92f), CornerRadius(g.radius),
                    alpha = 0.9f, style = Stroke(cs * 0.07f))
            }
        }
    }

    for ((cell, at) in ctrl.errorAt) {
        val age = now - at
        if (age > 900) continue
        val k = 1f - age / 900f
        val l = cell % n * cs; val t = cell / n * cs
        val g = cellGeo(cell, n)
        drawRoundRect(Color(0xFFE5484D), Offset(g.l, g.t), Size(g.s, g.s), CornerRadius(g.radius), alpha = 0.6f * k)
        val cx = l + cs / 2; val cy = t + cs / 2; val a = cs * 0.24f
        drawLine(Color.White, Offset(cx - a, cy - a), Offset(cx + a, cy + a), cs * 0.09f, StrokeCap.Round, alpha = k)
        drawLine(Color.White, Offset(cx - a, cy + a), Offset(cx + a, cy - a), cs * 0.09f, StrokeCap.Round, alpha = k)
    }

    val won = ctrl.status == Status.WON
    val anims = CatAnim.entries
    for (cell in 0 until n * n) {
        if (ctrl.cells.getOrNull(cell) != Cell.CAT) continue
        val placed = ctrl.placedAt[cell] ?: 0L
        var pop = popScale(now - placed)
        val anim = if (won && now - ctrl.wonAt > 250) CatAnim.DANCE else anims[ctrl.catAnims[cell] % anims.size]
        if (won) {
            // little wave across the board when solved
            val age = now - ctrl.wonAt - (cell % n + cell / n) * 40
            if (age in 0..400) pop *= 1f + 0.18f * sin(age / 400f * PI.toFloat())
        }
        val center = Offset((cell % n + 0.5f) * cs, (cell / n + 0.5f) * cs + cs * 0.02f)
        val t = now / 1000f + cell * 0.37f
        drawCat(center, cs * 0.9f, Furs[ctrl.catFurs[cell] % Furs.size], anim, t, pop)
    }
}
