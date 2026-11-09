package de.schnurrfeld.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.schnurrfeld.app.App
import de.schnurrfeld.app.game.BAR_MAX
import de.schnurrfeld.app.game.Cell
import de.schnurrfeld.app.game.GameController
import de.schnurrfeld.app.game.GameMode
import de.schnurrfeld.app.game.Hint
import de.schnurrfeld.app.game.HintEngine
import de.schnurrfeld.app.game.Status
import de.schnurrfeld.app.ui.components.BoardView
import de.schnurrfeld.app.ui.components.Celebration
import de.schnurrfeld.app.ui.components.Gap
import de.schnurrfeld.app.ui.components.MilkBar
import de.schnurrfeld.app.ui.components.rememberFrameMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.sin

/**
 * Interactive first-start tutorial on a small 4×4 board:
 * set a cross, remove it, learn the rules, place a cat, cross out what it blocks, solve the rest,
 * then a short word on combos and the Milchkännchen. Can be skipped at any time.
 */
@Composable
fun TutorialScreen(app: App, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val ctrl = remember { GameController(GameMode.Tutorial, app.store, app.sound, app.voice, app.levels, scope) }
    LaunchedEffect(ctrl) { ctrl.start() }
    var step by remember { mutableIntStateOf(0) }
    val p = ctrl.puzzle

    // the cells the tutorial points at
    val n = p?.size ?: 4
    val catCell = p?.let { pz -> (0 until n).minBy { g -> pz.regions.count { it == g } }.let { pz.solutionCellOfRegion(it) } } ?: -1
    val xCell = p?.let { pz -> (0 until n * n).first { !pz.isSolution(it) && it / n != catCell / n && it % n != catCell % n } } ?: -1

    // advance automatically when the player did what was asked
    LaunchedEffect(step, p) {
        if (p == null) return@LaunchedEffect
        when (step) {
            1 -> { snapshotFlow { ctrl.cells.getOrNull(xCell) }.first { it == Cell.X }; step = 2 }
            2 -> { snapshotFlow { ctrl.cells.getOrNull(xCell) }.first { it == Cell.EMPTY }; step = 3 }
            3 -> { // rules: show a row, a column, a colour and the neighbours one after the other
                val r = catCell / n; val c = catCell % n
                val sets = listOf(
                    (0 until n).map { r * n + it }.toSet(),
                    (0 until n).map { it * n + c }.toSet(),
                    (0 until n * n).filter { p.regions[it] == p.regions[catCell] }.toSet(),
                    (0 until n * n).filter { kotlin.math.abs(it / n - r) <= 1 && kotlin.math.abs(it % n - c) <= 1 }.toSet(),
                )
                var i = 0
                while (step == 3) { ctrl.showHint(Hint("", "", focus = sets[i % sets.size])); i++; delay(1300) }
            }
            4 -> {
                ctrl.showHint(Hint("", "", catCell = catCell))
                snapshotFlow { ctrl.cells.getOrNull(catCell) }.first { it == Cell.CAT }
                step = 5
            }
            5 -> ctrl.showHint(HintEngine.find(p, ctrl.cells.toList()))
            6 -> { snapshotFlow { ctrl.status }.first { it == Status.WON }; delay(2600); step = 7 }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("So geht Schnurrfeld", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (step < 7) TextButton(onClick = onDone, modifier = Modifier.testTag("skip")) { Text("Überspringen") }
            }
            // progress dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(8) { i ->
                    Surface(shape = RoundedCornerShape(50), color = if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(if (i == step) 10.dp else 7.dp)) {}
                }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, maxHeight, 300.dp)
                if (p != null && step in 1..6) {
                    Box(Modifier.size(side)) {
                        BoardView(ctrl, app.store.settings.colorAssist, Modifier.size(side).testTag("board"))
                        val target = when (step) { 1, 2 -> xCell; 4 -> catCell; else -> -1 }
                        if (target >= 0) PointingPaw(target, n, Modifier.size(side))
                    }
                } else if (step == 0) {
                    de.schnurrfeld.app.ui.components.AnimatedCat(de.schnurrfeld.app.ui.theme.Furs[0], de.schnurrfeld.app.ui.components.CatAnim.DANCE, 180.dp)
                } else if (step == 7) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Das Milchkännchen", style = MaterialTheme.typography.titleMedium)
                        Gap(8.dp)
                        val demo = (rememberFrameMillis().value / 700 % (BAR_MAX + 1)).toInt()
                        MilkBar(demo, BAR_MAX, 0L, Modifier.widthIn(max = 280.dp).fillMaxWidth().height(20.dp))
                    }
                }
            }
            CoachCard(step, ctrl, onNext = { step++ }, onDone = onDone)
        }
        if (ctrl.status == Status.WON && step == 6) Celebration(ctrl.wonAt, true)
    }
}

@Composable
private fun CoachCard(step: Int, ctrl: GameController, onNext: () -> Unit, onDone: () -> Unit) {
    val (title, text) = when (step) {
        0 -> "Willkommen bei Schnurrfeld!" to "Hier dreht sich alles darum, Katzen auf die richtigen Felder zu setzen. Das Wichtigste lernst du in einer Minute – direkt zum Ausprobieren."
        1 -> "Ein ✕ setzen" to "Tippe einmal auf das Feld, auf das die Pfote zeigt. Ein ✕ heißt: Hier ist sicher keine Katze."
        2 -> "Ein ✕ wieder entfernen" to "Tippe nochmal drauf – schon ist das ✕ wieder weg. Kreuze setzen und entfernen kostet nie etwas."
        3 -> "Die Regeln" to "In jede Zeile, jede Spalte und jede Farbe gehört genau eine Katze. Und Katzen mögen ihre Ruhe: Sie berühren sich nie – auch nicht schräg."
        4 -> "Eine Katze setzen" to "Doppeltippe schnell auf das leuchtende Feld. Aber Vorsicht: Im echten Spiel kostet eine falsche Katze ein Leben."
        5 -> "Felder ausschließen" to "Super! Diese Katze belegt jetzt ihre Zeile, Spalte und Farbe – und ihre Nachbarfelder. Dort kann keine Katze mehr hin."
        6 -> "Jetzt du!" to "Löse den Rest des Rätsels. Kommst du nicht weiter, hilft dir ein Tipp und erklärt dir den nächsten Schritt."
        else -> "Kombos & Milchkännchen" to "Setzt du Katzen schnell oder direkt hintereinander, gibt's Kombos: „Kombo“, „Quick Kombo“ und „Super fast“. Sie füllen dein Milchkännchen – ist es voll, bekommst du einen Tipp oder Joker. Ein Fehler leert es wieder."
    }
    Surface(
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 8.dp,
    ) {
        AnimatedContent(step, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "coach") { _ ->
            Column(Modifier.padding(20.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Gap(6.dp)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Gap(14.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    when (step) {
                        0 -> Button(onClick = onNext, modifier = Modifier.testTag("next")) { Text("Los geht's") }
                        3 -> Button(onClick = onNext, modifier = Modifier.testTag("next")) { Text("Verstanden") }
                        5 -> Button(onClick = { ctrl.applyHint(); onNext() }, modifier = Modifier.testTag("next")) { Text("✕ setzen") }
                        6 -> OutlinedButton(onClick = { ctrl.freeHint() }, enabled = ctrl.hint == null, modifier = Modifier.testTag("tip")) { Text("Tipp") }
                        7 -> Button(onClick = onDone, modifier = Modifier.testTag("done")) { Text("Los geht's!") }
                        else -> Text("👆 Probier's aus", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

/** A bobbing cat paw pointing at a cell. */
@Composable
private fun PointingPaw(cell: Int, n: Int, modifier: Modifier) {
    val frame = rememberFrameMillis()
    Canvas(modifier) {
        val cs = size.width / n
        val bob = sin(frame.value / 160f) * cs * 0.06f
        val c = Offset((cell % n + 0.68f) * cs, (cell / n + 0.78f) * cs + bob)
        drawPaw(c, cs * 0.34f)
    }
}

private fun DrawScope.drawPaw(c: Offset, r: Float) {
    val fur = Color(0xFFFFFBF5); val line = Color(0xFF3B3340); val pink = Color(0xFFF28CA0)
    drawOval(fur, Offset(c.x - r * 0.55f, c.y - r * 0.2f), androidx.compose.ui.geometry.Size(r * 1.1f, r * 1.25f))
    drawOval(line, Offset(c.x - r * 0.55f, c.y - r * 0.2f), androidx.compose.ui.geometry.Size(r * 1.1f, r * 1.25f), style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.09f))
    drawOval(pink, Offset(c.x - r * 0.25f, c.y + r * 0.2f), androidx.compose.ui.geometry.Size(r * 0.5f, r * 0.38f))
    for ((dx, dy) in listOf(-0.32f to -0.02f, -0.1f to -0.12f, 0.12f to -0.12f, 0.33f to -0.02f)) {
        drawCircle(pink, r * 0.1f, Offset(c.x + dx * r, c.y + dy * r + r * 0.12f))
    }
}
