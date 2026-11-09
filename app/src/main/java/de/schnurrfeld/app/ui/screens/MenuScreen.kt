package de.schnurrfeld.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.schnurrfeld.app.App
import de.schnurrfeld.app.Screen
import de.schnurrfeld.app.game.Daily
import de.schnurrfeld.app.game.GameMode
import de.schnurrfeld.app.game.Levels
import de.schnurrfeld.app.ui.components.AnimatedCat
import de.schnurrfeld.app.ui.components.CatAnim
import de.schnurrfeld.app.ui.components.Gap
import de.schnurrfeld.app.ui.components.HGap
import de.schnurrfeld.app.ui.theme.Furs
import de.schnurrfeld.app.ui.theme.Gold
import java.time.LocalDate

@Composable
fun MenuScreen(app: App, navigate: (Screen) -> Unit) {
    val progress = app.store.progress
    val today = LocalDate.now()
    val dailyDone = today.toString() in progress.dailySolved
    val current = progress.unlocked.coerceAtMost(Levels.count)
    var showRules by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
            RoundIcon(Icons.AutoMirrored.Rounded.HelpOutline, "Anleitung") { showRules = true }
            HGap(8.dp)
            RoundIcon(Icons.Rounded.Settings, "Einstellungen") { navigate(Screen.Settings) }
        }

        Row(verticalAlignment = Alignment.Bottom) {
            AnimatedCat(Furs[4], CatAnim.LICK, 76.dp, phase = 1.3f)
            AnimatedCat(Furs[0], CatAnim.DANCE, 112.dp)
            AnimatedCat(Furs[3], CatAnim.SLEEP, 76.dp, phase = 0.6f)
        }
        Gap(8.dp)
        Text("Schnurrfeld", style = MaterialTheme.typography.displaySmall)
        Text(
            "Ein Katzen-Logikrätsel",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Gap(10.dp)
        de.schnurrfeld.app.ui.components.DailyTalerButton(app)
        Gap(20.dp)


        Button(
            onClick = { navigate(Screen.Game(GameMode.Level(current))) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp),
        ) {
            Icon(Icons.Rounded.PlayArrow, null)
            HGap(8.dp)
            Column {
                Text("Spielen", style = MaterialTheme.typography.titleMedium)
                val c = Levels.config(current)
                Text("Level $current · ${c.size}×${c.size}", style = MaterialTheme.typography.bodySmall)
            }
            Box(Modifier.weight(1f))
        }
        Gap(12.dp)

        MenuRow(Icons.Rounded.GridView, "Levels", "${progress.stars.size} von ${Levels.count} gelöst") {
            navigate(Screen.Levels)
        }
        Gap(8.dp)
        MenuRow(
            Icons.Rounded.CalendarToday, "Tagesrätsel",
            if (dailyDone) "Heute gelöst – bis morgen!" else "Neu · ${Daily.size(today)}×${Daily.size(today)}",
            trailing = if (dailyDone) Icons.Rounded.CheckCircle else null,
        ) { navigate(Screen.Game(GameMode.Daily(today))) }
        Gap(8.dp)
        MenuRow(Icons.Rounded.AllInclusive, "Endlos", "Rekord: ${progress.endlessBest} Rätsel am Stück") {
            navigate(Screen.Game(GameMode.Endless))
        }
        Gap(8.dp)
        val pet = app.store.pet
        val pending = app.store.petEvolutionPending
        val wobble = if (pending) {
            val f = de.schnurrfeld.app.ui.components.rememberFrameMillis().value
            val c = f % 2200
            if (c < 600) kotlin.math.sin(c / 600f * 6 * Math.PI.toFloat()) * 3f * (1 - c / 600f) else 0f
        } else 0f
        Box(Modifier.graphicsLayer { rotationZ = wobble }) {
            MenuRow(
                null, "Katzenkörbchen",
                when {
                    pending -> "Da tut sich was im Körbchen – schau rein!"
                    pet.breed != null -> "${pet.breed!!.title} wartet auf dich"
                    else -> de.schnurrfeld.app.data.PetRules.stageNames[pet.stage]
                },
                iconContent = { de.schnurrfeld.app.ui.components.BasketIcon(36.dp) },
            ) { navigate(Screen.Basket) }
        }
        Gap(16.dp)

        Gap(24.dp)
    }

    if (showRules) RulesDialog(onTutorial = { showRules = false; navigate(Screen.Tutorial) }) { showRules = false }
}

@Composable
fun RulesDialog(onTutorial: (() -> Unit)? = null, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Los geht's") } },
        dismissButton = { if (onTutorial != null) TextButton(onClick = onTutorial) { Text("Tutorial starten") } },
        title = { Text("So wird gespielt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Setze in jede Zeile, jede Spalte und jeden Farbbereich genau eine Katze.")
                Text("Katzen mögen ihre Ruhe: Sie dürfen sich nicht berühren – auch nicht diagonal.")
                Text("• Einmal tippen: ✕ setzen\n• ✕ nochmal antippen: ✕ entfernen\n• Schnell doppeltippen: Katze setzen\n• Wischen: viele ✕ auf einmal", fontWeight = FontWeight.Medium)
                Text("✕ setzen und entfernen ist immer kostenlos. Nur falsche Katzen kosten ein Leben (3 pro Rätsel) und beenden deine Kombo.")
                Text("Milchkännchen: 3 Katzen in Folge ohne Fehler +1 · Kombo (zwei Katzen ohne ✕ dazwischen) +1 · Quick Kombo (unter 20 s) +2 · Super fast (unter 10 s) +3 · Purrfect (Runde ohne verlorenes Leben) +3 · Tagesrätsel +1. Bei 9 Punkten gewinnst du abwechselnd einen Tipp oder Joker – ein Fehler leert das Kännchen. Ein Tipp erklärt dir den nächsten logischen Schritt, ein Joker setzt eine Katze für dich.")
            }
        }
    )
}

@Composable
private fun RoundIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, desc, modifier = Modifier.size(22.dp)) }
    }
}

@Composable
private fun StatChip(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector?, title: String, subtitle: String, trailing: ImageVector? = null,
    iconContent: (@Composable () -> Unit)? = null, onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {}
                if (iconContent != null) iconContent() else if (icon != null) Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            HGap(14.dp)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(trailing ?: Icons.Rounded.ChevronRight, null,
                tint = if (trailing != null) Color(0xFF3FA36B) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
