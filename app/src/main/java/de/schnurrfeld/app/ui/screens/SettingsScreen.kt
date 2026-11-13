package de.schnurrfeld.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import de.schnurrfeld.app.App
import de.schnurrfeld.app.BuildConfig
import de.schnurrfeld.app.data.Changelog
import de.schnurrfeld.app.data.Updater
import kotlinx.coroutines.launch
import de.schnurrfeld.app.data.ThemeMode
import de.schnurrfeld.app.ui.components.Gap
import de.schnurrfeld.app.ui.components.HGap
import de.schnurrfeld.app.ui.components.SectionCard
import de.schnurrfeld.app.ui.components.SectionTitle
import de.schnurrfeld.app.ui.components.TopBar

@Composable
fun SettingsScreen(app: App, onBack: () -> Unit) {
    val s = app.store.settings
    val update = app.store::updateSettings
    var confirmReset by remember { mutableStateOf(false) }
    var check by remember { mutableStateOf<UpdateCheck>(UpdateCheck.Idle) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        TopBar("Einstellungen", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            SectionTitle("Darstellung")
            SectionCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text("Design", style = MaterialTheme.typography.bodyLarge)
                    Gap(8.dp)
                    val options = listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Hell", ThemeMode.DARK to "Dunkel")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { i, (mode, label) ->
                            SegmentedButton(
                                selected = s.theme == mode,
                                onClick = { update { it.copy(theme = mode) } },
                                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            ) { Text(label) }
                        }
                    }
                }
                Divider()
                SwitchRow(Icons.Rounded.Contrast, "Farbhilfe", "Kräftigere Muster auf den Farbflächen", s.colorAssist) {
                    update { st -> st.copy(colorAssist = it) }
                }
            }

            SectionTitle("Spiel")
            SectionCard {
                SwitchRow(Icons.Rounded.Timer, "Timer anzeigen", null, s.showTimer) { update { st -> st.copy(showTimer = it) } }
                Divider()
                SwitchRow(Icons.Rounded.Close, "Auto-✕", "Blockierte Felder nach jeder Katze automatisch markieren", s.autoX) {
                    update { st -> st.copy(autoX = it) }
                }
            }

            SectionTitle("Ton & Haptik")
            SectionCard {
                SwitchRow(Icons.Rounded.VolumeUp, "Soundeffekte", "Plop, Erfolg und Fanfare", s.sound) { update { st -> st.copy(sound = it) } }
                Divider()
                SwitchRow(Icons.Rounded.Vibration, "Vibration", null, s.vibration) { update { st -> st.copy(vibration = it) } }
                Divider()
                SwitchRow(Icons.Rounded.RecordVoiceOver, "Lob-Stimme", "Nice! → Great! → Incredible! – steigert sich mit jeder fehlerfreien Katze", s.voice) {
                    update { st -> st.copy(voice = it) }
                    if (it) app.voice.warmUp(s.ttsFallback)
                }
                Divider()
                SwitchRow(Icons.Rounded.GraphicEq, "Computerstimme als Ersatz", "Spricht Wörter, für die es noch keine Aufnahme gibt", s.ttsFallback, enabled = s.voice) {
                    update { st -> st.copy(ttsFallback = it) }
                    if (it) app.voice.warmUp(true)
                }
            }

            ChangelogSection()

            SectionTitle("App")
            SectionCard {
                SwitchRow(Icons.Rounded.SystemUpdate, "Updates automatisch suchen", "Beim Start auf GitHub nachsehen. GitHub sieht dabei deine IP-Adresse, sonst wird nichts gesendet.", s.autoUpdate) {
                    update { st -> st.copy(autoUpdate = it) }
                }
                Divider()
                val subtitle = when (val c = check) {
                    UpdateCheck.Idle -> "Installiert: ${BuildConfig.VERSION_NAME}"
                    UpdateCheck.Busy -> "Suche läuft …"
                    UpdateCheck.Latest -> "Du hast die neueste Version (${BuildConfig.VERSION_NAME})"
                    is UpdateCheck.Failed -> "Suche fehlgeschlagen: ${c.msg}"
                }
                NavRow(Icons.Rounded.Refresh, "Nach Updates suchen", subtitle) {
                    if (check == UpdateCheck.Busy) return@NavRow
                    check = UpdateCheck.Busy
                    scope.launch {
                        check = runCatching { Updater.check(BuildConfig.VERSION_NAME, BuildConfig.DEV) }.fold(
                            onSuccess = { found -> if (found != null) { app.update = found; UpdateCheck.Idle } else UpdateCheck.Latest },
                            onFailure = { UpdateCheck.Failed(it.message ?: it.javaClass.simpleName) },
                        )
                    }
                }
            }

            SectionTitle("Fortschritt")
            SectionCard {
                NavRow(Icons.Rounded.DeleteOutline, "Fortschritt zurücksetzen", "Levels, Sterne, Katzen und Joker") { confirmReset = true }
            }
            if (de.schnurrfeld.app.BuildConfig.DEV) DevSection(app)
            Gap(16.dp)
            Text(
                "Schnurrfeld ${de.schnurrfeld.app.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Alles zurücksetzen?") },
            text = { Text("Dein gesamter Spielfortschritt wird gelöscht. Einstellungen bleiben erhalten.") },
            confirmButton = {
                TextButton(onClick = { app.store.resetProgress(); confirmReset = false }) {
                    Text("Zurücksetzen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Abbrechen") } }
        )
    }
}

/** "Was ist neu": every version with its changes, the newest one open */
@Composable
private fun ChangelogSection() {
    var open by remember { mutableStateOf(setOf(Changelog.entries.first().version)) }
    SectionTitle("Was ist neu")
    SectionCard {
        Changelog.entries.forEachIndexed { i, e ->
            if (i > 0) Divider()
            val expanded = e.version in open
            Column(
                Modifier.fillMaxWidth().clickable { open = if (expanded) open - e.version else open + e.version }
                    .padding(horizontal = 16.dp, vertical = 12.dp).testTag("changelog_${e.version}")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Version ${e.version}" + if (e.version == BuildConfig.VERSION_NAME.removeSuffix("-dev")) "  ·  installiert" else "",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(e.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AnimatedVisibility(expanded) {
                    Column(Modifier.padding(top = 8.dp)) {
                        for (sec in e.sections) {
                            Text(sec.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                            for (item in sec.items) Row(Modifier.padding(vertical = 2.dp)) {
                                Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
                                Text(item, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class UpdateCheck {
    data object Idle : UpdateCheck()
    data object Busy : UpdateCheck()
    data object Latest : UpdateCheck()
    data class Failed(val msg: String) : UpdateCheck()
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun SwitchRow(
    icon: ImageVector, title: String, subtitle: String?, checked: Boolean,
    enabled: Boolean = true, onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        HGap(16.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HGap(12.dp)
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        HGap(16.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


// ======================================================================= developer menu (dev builds only)

@Composable
private fun DevSection(app: App) {
    val store = app.store
    var level by remember { mutableStateOf(store.levelReached.toFloat()) }
    var info by remember { mutableStateOf("") }
    SectionTitle("Entwickler")
    SectionCard {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text("Gelöstes Level: ${level.toInt()} (aktuell ${store.levelReached})", style = MaterialTheme.typography.bodyLarge)
            androidx.compose.material3.Slider(
                value = level, onValueChange = { level = it }, valueRange = 0f..de.schnurrfeld.app.game.Levels.count.toFloat(),
                steps = 0, modifier = Modifier.fillMaxWidth()
            )
            DevButtons(
                "Level übernehmen" to {
                    val l = level.toInt()
                    store.updateProgress { it.copy(unlocked = (l + 1).coerceAtMost(de.schnurrfeld.app.game.Levels.count)) }
                    info = "Level $l gesetzt"
                },
            )
            Gap(6.dp)
            DevButtons(
                "+5 Taler" to { store.updatePet { it.copy(talers = it.talers + 5) }; info = "+5 Taler" },
                "+20 Tipps" to { store.updateProgress { it.copy(hints = it.hints + 20) }; info = "+20 Tipps" },
                "+10 Joker" to { store.updateProgress { it.copy(jokers = it.jokers + 10) }; info = "+10 Joker" },
            )
            DevButtons(
                "+5 jedes Item" to {
                    store.updatePet { p -> p.copy(stock = de.schnurrfeld.app.data.Care.entries.associateWith { p.stockOf(it) + 5 }) }
                    info = "+5 von jedem Item"
                },
                "Milchkännchen 8/9" to { store.updateProgress { it.copy(bar = 8) }; info = "Kännchen auf 8" },
            )
            Gap(6.dp)
            Text("Katzenkörbchen: Stufe ${store.pet.stage} · ${store.pet.given} Items · ${store.pet.breed?.title ?: store.pet.tail?.name ?: "–"}",
                style = MaterialTheme.typography.bodyMedium)
            DevButtons(
                "Entwicklung prüfen" to { info = if (store.petCheckEvolution()) "Entwickelt auf Stufe ${store.pet.stage}!" else "Noch nicht bereit (Items/Level fehlen)" },
            )
            DevButtons(
                "Stufe −" to { store.updatePet { it.copy(stage = (it.stage - 1).coerceAtLeast(0), seenStage = (it.stage - 1).coerceAtLeast(0)) } },
                "Stufe +" to {
                    store.updatePet { it.copy(stage = (it.stage + 1).coerceAtMost(de.schnurrfeld.app.data.PetRules.MAX_STAGE)) }
                    info = "Stufe ${store.pet.stage} – Entwicklung wird im Körbchen gezeigt"
                },
            )
            Text("Katzenart erzwingen:", style = MaterialTheme.typography.bodyMedium)
            DevButtons(
                *(listOf<Pair<String, () -> Unit>>("Automatisch" to { store.updatePet { it.copy(breedOverride = null) } }) +
                    de.schnurrfeld.app.data.Breed.entries.map { b -> b.title to { store.updatePet { it.copy(breedOverride = b) } } }).toTypedArray()
            )
            Gap(6.dp)
            DevButtons(
                "Zeit +12 h (Bedürfnisse)" to { store.devAgeNeeds(12); info = "Bedürfnisse um 12 Stunden gealtert" },
                "Tages-Taler zurücksetzen" to { store.devResetDaily(); info = "Tages-Taler kann wieder abgeholt werden" },
            )
            DevButtons(
                "Tutorial zurücksetzen" to { store.updateSettings { it.copy(tutorialDone = false) }; info = "Tutorial startet beim nächsten App-Start" },
                "Körbchen zurücksetzen" to { store.resetPet(); info = "Katzenkörbchen zurückgesetzt" },
            )
            if (info.isNotEmpty()) Text(info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DevButtons(vararg buttons: Pair<String, () -> Unit>) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        for ((label, action) in buttons) {
            androidx.compose.material3.OutlinedButton(onClick = action, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
