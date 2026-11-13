package de.schnurrfeld.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.graphics.graphicsLayer
import de.schnurrfeld.app.ui.components.drawHelperCat
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import de.schnurrfeld.app.App
import de.schnurrfeld.app.Screen
import de.schnurrfeld.app.audio.Praise
import de.schnurrfeld.app.game.GameController
import de.schnurrfeld.app.game.GameMode
import de.schnurrfeld.app.game.BAR_MAX
import de.schnurrfeld.app.game.Levels
import de.schnurrfeld.app.game.MAX_LIVES
import de.schnurrfeld.app.game.Status
import de.schnurrfeld.app.ui.components.BoardView
import de.schnurrfeld.app.ui.components.Gap
import de.schnurrfeld.app.ui.components.HGap
import de.schnurrfeld.app.ui.components.TopBar
import de.schnurrfeld.app.ui.theme.Gold
import de.schnurrfeld.app.ui.theme.Heart
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun GameScreen(app: App, mode: GameMode, onBack: () -> Unit, navigate: (Screen.Game) -> Unit, onBasket: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val ctrl = remember(mode) { GameController(mode, app.store, app.sound, app.voice, app.levels, scope, resume = app.takeResume(mode)) }
    LaunchedEffect(ctrl) { ctrl.start() }
    val settings = app.store.settings
    val progress = app.store.progress
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // timer – runs only while the app is in the foreground
    LaunchedEffect(ctrl, ctrl.status) {
        if (ctrl.status == Status.PLAYING) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) { delay(1000); ctrl.elapsed++ }
            }
        }
    }

    // keep the running game saved: whenever the board, the lives etc. change, and always when the app is paused
    // (phone call, another app) – so the game also survives Android clearing the app from memory
    LaunchedEffect(ctrl) {
        snapshotFlow { ctrl.snapshot()?.copy(elapsed = 0, savedAt = 0L) }.collect { snap ->
            if (snap != null) ctrl.snapshot()?.let { app.store.saveGame(it.encode()) }
        }
    }
    LaunchedEffect(ctrl.status) { if (ctrl.status == Status.WON || ctrl.status == Status.LOST) app.store.saveGame(null) }
    DisposableEffect(lifecycle, ctrl) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) ctrl.snapshot()?.let { app.store.saveGame(it.encode()) }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // beginner help: check once a second whether the player seems stuck
    LaunchedEffect(ctrl) {
        if (!ctrl.helperEnabled) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { while (true) { delay(1000); ctrl.checkIdle() } }
    }

    // board shake on mistakes
    val shake = remember { Animatable(0f) }
    LaunchedEffect(ctrl.shake) {
        if (ctrl.shake > 0) {
            for (i in 0..5) shake.animateTo(if (i % 2 == 0) 9f else -9f, tween(45))
            shake.animateTo(0f, spring())
        }
    }

    val won = ctrl.status == Status.WON
    val frame = if (won) de.schnurrfeld.app.ui.components.rememberFrameMillis().value else 0L
    val slam = de.schnurrfeld.app.ui.components.slamShake(ctrl.wonAt, won && ctrl.perfect, frame)

    // where things are on screen, for the milk flowing from the words into the bar
    val geo = remember { de.schnurrfeld.app.ui.components.FlowGeometry() }

    Box(Modifier.fillMaxSize().onGloballyPositioned { geo.origin = it.boundsInRoot().topLeft }) {
        Column(
            Modifier.fillMaxSize().offset { IntOffset((slam * 14).dp.roundToPx(), (slam * 6).dp.roundToPx()) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TopBar(ctrl.title, onBack) {
                IconButton(onClick = { ctrl.restart() }) { Icon(Icons.Rounded.Refresh, "Neu starten") }
            }

            StatusBar(ctrl, settings.showTimer)
            ComboBarRow(ctrl, progress.bar, progress.prizes, geo)

            PraiseArea(ctrl, geo)

            BoxWithConstraints(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                val n = ctrl.puzzle?.size ?: 8
                val side = minOf(maxWidth, maxHeight, 54.dp * n + 24.dp)
                if (ctrl.puzzle == null || ctrl.status == Status.LOADING) {
                    CircularProgressIndicator()
                } else {
                    BoardView(
                        ctrl, settings.colorAssist,
                        Modifier.size(side).testTag("board").offset { IntOffset(shake.value.dp.roundToPx(), 0) }
                    )
                }
                RewardBanner(ctrl, Modifier.align(Alignment.TopCenter))
            }

            ToolRow(ctrl, progress.hints, progress.jokers)
            Text(
                "Tippen: ✕ setzen/entfernen · Doppeltippen: Katze",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp, top = 2.dp)
            )
        }

        de.schnurrfeld.app.ui.components.MilkFlow(ctrl.flow, geo, progress.bar.toFloat() / BAR_MAX, Modifier.fillMaxSize())
        HelperOverlay(ctrl, Modifier.align(Alignment.TopEnd).padding(top = 160.dp, end = 8.dp))
        HintCard(ctrl, Modifier.align(Alignment.BottomCenter))
        if (won) de.schnurrfeld.app.ui.components.Celebration(ctrl.wonAt, ctrl.perfect)
        EndPanel(ctrl, onBack, navigate, Modifier.align(Alignment.BottomCenter),
            nextTag = (mode as? GameMode.Level)?.let { app.levels.tag(it.number + 1) } ?: 0,
            basketNews = app.store.petEvolutionPending, onBasket = onBasket)
    }
}

@Composable
private fun ComboBarRow(ctrl: GameController, bar: Int, prizes: Int, geo: de.schnurrfeld.app.ui.components.FlowGeometry) {
    val next = if (prizes % 2 == 0) "Tipp" else "Joker"
    var help by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MilkJugIcon(Modifier.size(22.dp))
        HGap(6.dp)
        de.schnurrfeld.app.ui.components.MilkBar(
            bar, BAR_MAX, ctrl.barGainAt,
            Modifier.weight(1f).height(18.dp).onGloballyPositioned { geo.bar = it.boundsInRoot() }
        )
        HGap(8.dp)
        Text("$bar/$BAR_MAX", style = MaterialTheme.typography.labelLarge)
        HGap(6.dp)
        if (next == "Tipp") Icon(Icons.Rounded.TipsAndUpdates, "Nächster Gewinn: Tipp", tint = Gold, modifier = Modifier.size(20.dp))
        else de.schnurrfeld.app.ui.components.JokerIcon(24.dp)
        IconButton(onClick = { help = true }, modifier = Modifier.size(36.dp).testTag("milk_help")) {
            Icon(Icons.AutoMirrored.Rounded.HelpOutline, "Was ist das Milchkännchen?", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
    if (help) MilkHelpDialog(next) { help = false }
}

@Composable
private fun MilkJugIcon(modifier: Modifier) = androidx.compose.foundation.Canvas(modifier) {
    val w = size.width; val h = size.height
    val line = Color(0xFF3B3340)
    val jug = androidx.compose.ui.graphics.Path().apply {
        moveTo(w * 0.22f, h * 0.18f); lineTo(w * 0.70f, h * 0.18f); lineTo(w * 0.78f, h * 0.88f)
        quadraticTo(w * 0.46f, h * 0.98f, w * 0.14f, h * 0.88f); close()
    }
    drawPath(jug, Color(0xFFFFFBF2))
    drawPath(jug, line, style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.08f, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    drawArc(line, -80f, 160f, false, androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.32f), androidx.compose.ui.geometry.Size(w * 0.36f, h * 0.36f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.08f))
    drawLine(line, androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.18f), androidx.compose.ui.geometry.Offset(w * 0.08f, h * 0.08f), w * 0.08f, androidx.compose.ui.graphics.StrokeCap.Round)
    drawCircle(Color(0xFF9CC7E8), w * 0.08f, androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.6f))
}

@Composable
private fun MilchkannchenRule(points: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(50), color = Gold.copy(alpha = 0.9f), contentColor = Color(0xFF3A2A05)) {
            Text(points, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp))
        }
        HGap(10.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MilkHelpDialog(next: String, onClose: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { androidx.compose.material3.TextButton(onClick = onClose) { Text("Alles klar") } },
        title = { Text("Das Milchkännchen") },
        text = {
            Column {
                Text("Mit jeder starken Aktion fließt Milch ins Kännchen. Bei 9 Punkten ist es voll und du gewinnst abwechselnd einen Tipp oder Joker – als Nächstes: $next.",
                    style = MaterialTheme.typography.bodyMedium)
                Gap(10.dp)
                MilchkannchenRule("+1", "3 Katzen in Folge ohne Fehler")
                MilchkannchenRule("+1", "Kombo: keine ✕ zwischen zwei Katzen")
                MilchkannchenRule("+2", "Quick Kombo: nächste Katze in unter 20 s")
                MilchkannchenRule("+3", "Super fast: nächste Katze in unter 10 s")
                MilchkannchenRule("+3", "Purrfect: Runde ohne verlorenes Leben")
                MilchkannchenRule("+1", "Tagesrätsel gelöst")
                Gap(10.dp)
                Text("Aber Achtung: Eine falsche Katze leert das Kännchen.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun HintCard(ctrl: GameController, modifier: Modifier) {
    val h = ctrl.hint?.first
    AnimatedVisibility(
        h != null && ctrl.status == Status.PLAYING, modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = 0.85f)) { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        val hint = h ?: return@AnimatedVisibility
        Surface(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 12.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TipsAndUpdates, null, tint = Gold, modifier = Modifier.size(22.dp))
                    HGap(8.dp)
                    Text(hint.title, style = MaterialTheme.typography.titleMedium)
                }
                Gap(8.dp)
                Text(hint.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Gap(10.dp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.TextButton(onClick = { ctrl.closeHint() }) {
                        Text(if (hint.crosses.isNotEmpty() || hint.removeCross != null) "Selbst machen" else "Ausblenden")
                    }
                    when {
                        hint.removeCross != null -> Button(onClick = { ctrl.applyHint() }) { Text("✕ entfernen") }
                        hint.crosses.isNotEmpty() -> Button(onClick = { ctrl.applyHint() }) { Text("✕ setzen") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBar(ctrl: GameController, showTimer: Boolean) {
    val n = ctrl.puzzle?.size ?: 0
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Pets, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            HGap(6.dp)
            Text("${ctrl.found}/$n", style = MaterialTheme.typography.titleMedium)
            HGap(14.dp)
            Row {
                repeat(MAX_LIVES) { i ->
                    val alive = i < ctrl.lives
                    Icon(
                        if (alive) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        null,
                        tint = if (alive) Heart else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Box(Modifier.weight(1f))
            if (showTimer) {
                Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                HGap(3.dp)
                Text(formatTime(ctrl.elapsed), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

fun formatTime(s: Int) = "%d:%02d".format(s / 60, s % 60)

@Composable
private fun PraiseArea(ctrl: GameController, geo: de.schnurrfeld.app.ui.components.FlowGeometry) {
    val ev = ctrl.banner
    val scale = remember { Animatable(0f) }
    val melt = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(ev?.id) {
        if (ev == null) return@LaunchedEffect
        melt.snapTo(0f)
        visible = true
        scale.snapTo(0.3f)
        kotlinx.coroutines.coroutineScope {
            launch { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) }
            if (ev.points > 0) {
                // the word liquefies and flows into the bar (droplets: MilkFlow)
                delay(de.schnurrfeld.app.ui.components.MELT_START_MS)
                melt.animateTo(1f, tween(380, easing = androidx.compose.animation.core.FastOutLinearInEasing))
                visible = false
            } else {
                delay(if (ev.kind == de.schnurrfeld.app.audio.VoiceKind.FINISH) 2200 else 1000)
                visible = false
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(58.dp), contentAlignment = Alignment.Center) {
        AnimatedVisibility(visible && ev != null, enter = fadeIn(), exit = fadeOut(tween(if (ev?.points ?: 0 > 0) 60 else 300))) {
            if (ev != null) {
                val tier = ev.tier
                val base = when {
                    ev.kind == de.schnurrfeld.app.audio.VoiceKind.COMBO && tier >= 3 -> Gold
                    ev.kind == de.schnurrfeld.app.audio.VoiceKind.COMBO -> Color(0xFFFF7043)
                    ev.kind == de.schnurrfeld.app.audio.VoiceKind.FINISH -> MaterialTheme.colorScheme.primary
                    tier >= 2 -> Gold
                    tier >= 1 -> Color(0xFFFF7043)
                    else -> MaterialTheme.colorScheme.primary
                }
                val m = melt.value
                val color = androidx.compose.ui.graphics.lerp(base, Color(0xFFFFF6E6), (m * 1.6f).coerceAtMost(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .onGloballyPositioned { if (ev.points > 0) geo.banner = it.boundsInRoot() }
                        .graphicsLayer {
                            val sc = scale.value
                            // melting: the letters lose their shape, squash and run together
                            scaleX = sc * (1f + 0.18f * m)
                            scaleY = sc * (1f - 0.75f * m)
                            translationY = -10.dp.toPx() * m
                            alpha = 1f - m * m
                        }
                ) {
                    Text(
                        ev.text,
                        fontSize = (24 + tier * 3).coerceAtMost(36).sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = color,
                    )
                    if (ev.points > 0) {
                        HGap(8.dp)
                        Surface(shape = RoundedCornerShape(50), color = Gold.copy(alpha = 0.9f), contentColor = Color(0xFF3A2A05)) {
                            Text("+${ev.points}", fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RewardBanner(ctrl: GameController, modifier: Modifier) {
    val ev = ctrl.reward
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(ev?.id) {
        if (ev == null) return@LaunchedEffect
        visible = true
        delay(2400)
        visible = false
    }
    AnimatedVisibility(
        visible && ev != null, modifier = modifier.padding(top = 8.dp),
        enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()
    ) {
        Surface(shape = RoundedCornerShape(50), color = Gold, contentColor = Color(0xFF3A2A05), shadowElevation = 6.dp) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                HGap(6.dp)
                Text(ev?.text ?: "", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ToolRow(ctrl: GameController, hints: Int, jokers: Int) {
    val playing = ctrl.status == Status.PLAYING
    val free = ctrl.freeTip && playing
    // a free tip makes the button wiggle every couple of seconds
    val wiggle = if (free) {
        val f = de.schnurrfeld.app.ui.components.rememberFrameMillis().value
        val c = f % 1600
        if (c < 500) kotlin.math.sin(c / 500f * 6 * Math.PI.toFloat()) * 4f * (1 - c / 500f) else 0f
    } else 0f
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ToolButton({ Icon(Icons.Rounded.TipsAndUpdates, null, tint = Gold, modifier = Modifier.size(22.dp)) },
                "Tipp", hints, playing && (free || (hints > 0 && ctrl.hint == null)),
                Modifier.weight(1f).graphicsLayer { rotationZ = wiggle }.testTag("tip_button"),
                badge = if (free) "Gratis" else null) { if (free) ctrl.useFreeTip() else ctrl.useHint() }
            ToolButton({ de.schnurrfeld.app.ui.components.JokerIcon(30.dp) },
                "Joker", jokers, playing && jokers > 0, Modifier.weight(1f)) { ctrl.useJoker() }
        }
    }
}

@Composable
private fun ToolButton(icon: @Composable () -> Unit, label: String, count: Int, enabled: Boolean, modifier: Modifier, badge: String? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.alpha(if (enabled) 1f else 0.45f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            HGap(8.dp)
            Text(label, style = MaterialTheme.typography.labelLarge)
            HGap(8.dp)
            Surface(shape = RoundedCornerShape(50), color = if (badge != null) Gold else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (badge != null) Color(0xFF3A2A05) else MaterialTheme.colorScheme.onSurface) {
                Text(badge ?: "$count", style = MaterialTheme.typography.labelMedium, fontWeight = if (badge != null) FontWeight.Bold else null,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun EndPanel(
    ctrl: GameController, onBack: () -> Unit, navigate: (Screen.Game) -> Unit, modifier: Modifier,
    nextTag: Int = 0, basketNews: Boolean = false, onBasket: () -> Unit = {},
) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(ctrl.status) {
        show = false
        if (ctrl.status == Status.WON) { delay(if (ctrl.perfect) 3400 else 2300); show = true }
        if (ctrl.status == Status.LOST) { delay(700); show = true }
    }
    AnimatedVisibility(
        show, modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = 0.8f)) { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(12.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                val won = ctrl.status == Status.WON
                val mode = ctrl.mode
                Text(
                    when {
                        won && mode is GameMode.Daily -> "Tagesrätsel gelöst!"
                        won -> "Geschafft!"
                        mode == GameMode.Endless -> "Lauf beendet"
                        else -> "Keine Leben mehr"
                    },
                    style = MaterialTheme.typography.headlineMedium
                )
                Gap(10.dp)
                if (won && mode is GameMode.Level) {
                    val frame = de.schnurrfeld.app.ui.components.rememberFrameMillis().value
                    Row {
                        repeat(3) { i ->
                            val bob = if (i < ctrl.lives) sin(frame / 300f + i) * 3f else 0f
                            Icon(
                                Icons.Rounded.Star, null,
                                tint = if (i < ctrl.lives) Gold else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(44.dp).offset(y = bob.dp)
                            )
                        }
                    }
                    Gap(6.dp)
                }
                Text(
                    when {
                        won && mode == GameMode.Endless -> "Rätsel ${ctrl.endlessSolved} in ${formatTime(ctrl.elapsed)} gelöst. Bereit für das nächste?"
                        won -> "Alle Katzen haben ihren Platz gefunden · ${formatTime(ctrl.elapsed)}"
                        mode == GameMode.Endless -> "Du hast ${ctrl.endlessSolved} Rätsel am Stück gelöst."
                        else -> "Die Katzen geben dir gern noch eine Chance."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (won && ctrl.talersEarned > 0) {
                    Gap(10.dp)
                    Surface(shape = RoundedCornerShape(50), color = Gold.copy(alpha = 0.25f)) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            de.schnurrfeld.app.ui.components.TalerIcon(22.dp)
                            HGap(6.dp)
                            Text("+${ctrl.talersEarned} Taler", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Gap(20.dp)
                // a heads-up above "Nächstes Level" when a hard one is coming
                // psst – something is happening in the Katzenkörbchen
                if (won && basketNews) {
                    BasketNews(onBasket)
                    Gap(14.dp)
                }
                val showTag = won && mode is GameMode.Level && mode.number < Levels.count && nextTag > 0
                if (showTag) Gap(18.dp)      // room for the flames above the button
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(50.dp)) { Text("Menü") }
                    val primary: Pair<String, () -> Unit>? = when {
                        won && mode is GameMode.Level && mode.number < Levels.count ->
                            "Nächstes Level" to { navigate(Screen.Game(GameMode.Level(mode.number + 1))) }
                        won && mode == GameMode.Endless -> "Weiter" to { ctrl.nextEndless() }
                        won -> null
                        mode == GameMode.Endless -> "Neuer Lauf" to { ctrl.restart() }
                        else -> "Nochmal" to { ctrl.restart() }
                    }
                    if (primary != null && showTag) {
                        // a hard level is next: the button is wrapped in flames
                        Box(Modifier.weight(1f).flameFrame(nextTag).testTag("difficulty_badge")) {
                            Button(
                                onClick = primary.second, modifier = Modifier.fillMaxWidth().height(54.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = if (nextTag >= 2) Color(0xFFD63C3C) else Color(0xFFF07B2E), contentColor = Color.White),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(primary.first, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text(if (nextTag >= 2) "Super schwer" else "Schwer", style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFE9A8), maxLines = 1)
                                }
                            }
                        }
                    } else if (primary != null) {
                        Button(onClick = primary.second, modifier = Modifier.weight(1f).height(50.dp)) { Text(primary.first) }
                    }
                }
            }
        }
    }
}


/** The little helper cat: hops in at the top, points at its speech bubble, and leaves again after a while. */
@Composable
private fun HelperOverlay(ctrl: GameController, modifier: Modifier) {
    val msg = ctrl.helper
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(msg?.id) {
        if (msg == null) return@LaunchedEffect
        visible = true
        delay(7000)
        visible = false
    }
    AnimatedVisibility(
        visible && msg != null && ctrl.status == Status.PLAYING, modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = 0.5f, stiffness = 300f)) { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("helper")) {
            // speech bubble with a little tail pointing to the cat
            Surface(
                shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline), shadowElevation = 6.dp,
                modifier = Modifier.widthIn(max = 210.dp),
            ) {
                Text(msg?.text ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
            }
            val frame = de.schnurrfeld.app.ui.components.rememberFrameMillis()
            androidx.compose.foundation.Canvas(Modifier.size(width = 86.dp, height = 112.dp)) {
                drawHelperCat(androidx.compose.ui.geometry.Offset(size.width * 0.55f, size.height * 0.52f), size.width * 0.95f, frame.value / 1000f)
            }
        }
    }
}


/**
 * Fire around the next-level button: a soft, layered crown of flames (deep red → orange → bright core),
 * rising embers and a warm glow. "Super schwer" burns taller and redder and licks up the sides.
 */
@Composable
private fun Modifier.flameFrame(tag: Int): Modifier {
    val frame = de.schnurrfeld.app.ui.components.rememberFrameMillis()
    val superHard = tag >= 2
    return this.drawBehind {
        val t = frame.value / 1000f
        val w = size.width; val h = size.height
        val dens = density

        // smooth "noise": a few sines at different speeds
        fun noise(x: Float, speed: Float, seed: Float) =
            0.5f + 0.22f * kotlin.math.sin(x * 9.1f + t * speed + seed) + 0.16f * kotlin.math.sin(x * 17.3f - t * speed * 1.7f + seed * 2f) +
                0.12f * kotlin.math.sin(x * 4.3f + t * speed * 0.6f + seed * 3f)

        /** a continuous crown of flame tongues along [x0, x1] on the line y = baseY, pointing up */
        fun crown(x0: Float, x1: Float, baseY: Float, height: Float, tongues: Int, speed: Float, seed: Float, brush: androidx.compose.ui.graphics.Brush) {
            val path = androidx.compose.ui.graphics.Path()
            val step = (x1 - x0) / tongues
            path.moveTo(x0, baseY + h * 0.3f)
            path.lineTo(x0, baseY)
            for (i in 0 until tongues) {
                val left = x0 + i * step
                val mid = left + step / 2
                val right = left + step
                val k = (mid - x0) / (x1 - x0)
                // taller in the middle, lower at the ends
                val envelope = 0.55f + 0.45f * kotlin.math.sin(k * Math.PI.toFloat())
                val th = height * envelope * (0.55f + 0.6f * noise(k, speed, seed + i))
                val lean = kotlin.math.sin(t * speed * 0.8f + i * 1.3f + seed) * step * 0.35f
                val tip = Offset(mid + lean, baseY - th)
                // left flank of the tongue, curving into a soft tip, then down the right flank into the next valley
                path.cubicTo(left - step * 0.05f, baseY - th * 0.55f, tip.x - step * 0.42f, tip.y + th * 0.3f, tip.x, tip.y)
                path.cubicTo(tip.x + step * 0.42f, tip.y + th * 0.3f, right + step * 0.05f, baseY - th * 0.55f, right, baseY - th * 0.12f * noise(k, speed, seed))
            }
            path.lineTo(x1, baseY + h * 0.3f)
            path.close()
            drawPath(path, brush)
        }

        val base = h * 0.18f
        val crownH = h * (if (superHard) 0.9f else 0.68f)
        val red = if (superHard) Color(0xFFB0121B) else Color(0xFFD9381E)
        val orange = if (superHard) Color(0xFFF2501F) else Color(0xFFFF7A1F)
        val yellow = Color(0xFFFFC23A)
        val core = Color(0xFFFFF4C8)

        // warm glow behind everything
        drawOval(
            androidx.compose.ui.graphics.Brush.radialGradient(
                listOf(orange.copy(alpha = 0.55f), orange.copy(alpha = 0.18f), Color.Transparent),
                Offset(w / 2, h * 0.35f), w * 0.7f
            ),
            Offset(-w * 0.2f, -h * 1.3f), androidx.compose.ui.geometry.Size(w * 1.4f, h * 3f)
        )

        // sides (super hard): small crowns licking up the left and right ends
        if (superHard) for (side in listOf(-1f, 1f)) {
            val pivot = Offset(if (side < 0) h * 0.2f else w - h * 0.2f, h * 0.3f)
            rotate(side * 38f, pivot) {
                crown(pivot.x - h * 0.28f, pivot.x + h * 0.28f, pivot.y, h * 0.6f, 2, 7f, 4f + side,
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(orange.copy(alpha = 0f), orange, red), pivot.y - h * 0.7f, pivot.y + h * 0.1f))
                crown(pivot.x - h * 0.16f, pivot.x + h * 0.16f, pivot.y, h * 0.35f, 1, 9f, 6f + side,
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(yellow.copy(alpha = 0f), yellow, orange), pivot.y - h * 0.4f, pivot.y))
            }
        }

        val x0 = h * 0.2f; val x1 = w - h * 0.2f
        val n = if (superHard) 6 else 5
        // three layers: deep outer flame, orange body, bright core
        crown(x0, x1, base, crownH, n, 6.5f, 0f,
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(orange.copy(alpha = 0.0f), orange, red), base - crownH * 1.1f, base + h * 0.1f))
        crown(x0 + h * 0.15f, x1 - h * 0.15f, base, crownH * 0.78f, n - 1, 8f, 2.1f,
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(yellow.copy(alpha = 0.0f), yellow, orange), base - crownH * 0.85f, base + h * 0.05f))
        crown(x0 + h * 0.4f, x1 - h * 0.4f, base, crownH * 0.45f, maxOf(2, n - 2), 10f, 4.3f,
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(core.copy(alpha = 0.0f), core, yellow), base - crownH * 0.5f, base))

        // rising embers
        val embers = if (superHard) 14 else 8
        for (i in 0 until embers) {
            val life = 1.4f + (i % 4) * 0.25f
            val a = ((t + i * 0.37f) % life) / life
            val ex = x0 + (x1 - x0) * ((i * 0.618f) % 1f) + kotlin.math.sin(t * 3f + i) * 4f * dens
            val ey = base - crownH * 0.4f - a * crownH * 1.2f
            val r = (1.2f + (i % 3) * 0.6f) * dens * (1f - a * 0.6f)
            drawCircle(
                androidx.compose.ui.graphics.Brush.radialGradient(listOf(core, yellow.copy(alpha = 0.8f), Color.Transparent), Offset(ex, ey), r * 2.2f),
                r * 2.2f, Offset(ex, ey), alpha = (1f - a) * 0.95f
            )
        }

        // soft hot rim hugging the button
        drawRoundRect(
            androidx.compose.ui.graphics.Brush.verticalGradient(listOf(yellow.copy(alpha = 0.9f), orange.copy(alpha = 0.6f)), 0f, h),
            Offset(-1.5f * dens, -1.5f * dens), androidx.compose.ui.geometry.Size(w + 3f * dens, h + 3f * dens),
            androidx.compose.ui.geometry.CornerRadius(h / 2 + 1.5f * dens), style = androidx.compose.ui.graphics.drawscope.Stroke(2f * dens)
        )
    }
}

/** "Psst … im Katzenkörbchen tut sich was!" – shown in the win panel when the cat can grow now. */
@Composable
private fun BasketNews(onBasket: () -> Unit) {
    val f = de.schnurrfeld.app.ui.components.rememberFrameMillis().value
    val c = f % 2600
    val wob = if (c < 600) kotlin.math.sin(c / 600f * 6 * Math.PI.toFloat()) * 2.5f * (1 - c / 600f) else 0f
    Surface(
        onClick = onBasket, shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth().graphicsLayer { rotationZ = wob }.testTag("basket_news"),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            de.schnurrfeld.app.ui.components.BasketIcon(34.dp)
            HGap(10.dp)
            Column(Modifier.weight(1f)) {
                Text("Psst … im Körbchen tut sich was!", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text("Schau doch mal vorbei 🐾", style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Rounded.ChevronRight, null)
        }
    }
}
