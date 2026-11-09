package de.schnurrfeld.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.schnurrfeld.app.App
import de.schnurrfeld.app.audio.Praise
import de.schnurrfeld.app.ui.components.SectionCard
import de.schnurrfeld.app.ui.components.SectionTitle
import de.schnurrfeld.app.ui.components.TopBar
import de.schnurrfeld.app.ui.theme.Heart

@Composable
fun VoiceScreen(app: App, onBack: () -> Unit) {
    val context = LocalContext.current
    val voice = app.voice
    @Suppress("UNUSED_VARIABLE") val rev = voice.revision.intValue // re-read on change
    var recording by remember { mutableStateOf<Praise?>(null) }
    var pendingRecord by remember { mutableStateOf<Praise?>(null) }
    var importFor by remember { mutableStateOf<Praise?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { voice.stopRecording() } }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        val p = pendingRecord
        pendingRecord = null
        if (ok && p != null) {
            if (voice.startRecording(p)) recording = p else message = "Aufnahme konnte nicht gestartet werden."
        } else if (!ok) message = "Ohne Mikrofon-Berechtigung kannst du stattdessen Dateien importieren."
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val p = importFor
        importFor = null
        if (uri != null && p != null) {
            message = if (voice.import(p, uri)) "\"${p.text}\" importiert." else "Import fehlgeschlagen."
        }
    }

    fun toggleRecord(p: Praise) {
        if (recording == p) { voice.stopRecording(); recording = null; return }
        if (recording != null) { voice.stopRecording(); recording = null }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (voice.startRecording(p)) recording = p else message = "Aufnahme konnte nicht gestartet werden."
        } else {
            pendingRecord = p
            permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar("Eigene Lob-Stimme", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(
                "Das Lob steigert sich in 5 Stufen, je mehr Katzen du in einem Rätsel ohne Fehler setzt; Wörter einer Stufe wechseln sich zufällig ab. Wörter ohne Aufnahme bleiben stumm. " +
                    "Mitgeliefert sind deine Aufnahmen. Du kannst jedes Wort neu aufnehmen (Mikrofon antippen, sprechen, nochmal antippen) oder eine Audiodatei importieren.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            message?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp))
            }
            val groups = listOf(
                "Lob (steigert sich im Rätsel)" to de.schnurrfeld.app.audio.VoiceKind.PRAISE,
                "Kombo-Ansagen" to de.schnurrfeld.app.audio.VoiceKind.COMBO,
                "Rätsel gelöst (mit Fehlern)" to de.schnurrfeld.app.audio.VoiceKind.FINISH,
                "Perfekte Runde" to de.schnurrfeld.app.audio.VoiceKind.PERFECT,
            )
            for ((title, kind) in groups) {
                SectionTitle(title)
                SectionCard {
                    Praise.entries.filter { it.kind == kind }.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val sub = when (p) {
                            Praise.KOMBO -> "keine ✕ zwischen zwei Katzen · +1"
                            Praise.QUICK_KOMBO -> "nächste Katze in unter 20 s · +2"
                            Praise.SUPER_FAST -> "nächste Katze in unter 10 s · +3"
                            Praise.PERFECT -> "ohne verlorenes Leben · +3"
                            else -> if (kind == de.schnurrfeld.app.audio.VoiceKind.PRAISE) "Stufe ${p.tier + 1} von ${Praise.TOP_TIER + 1}" else "zufällig"
                        }
                        PraiseRow(app, p, sub, recording == p,
                            onRecord = { toggleRecord(p) },
                            onImport = { importFor = p; picker.launch("audio/*") })
                    }
                }
            }
        }
    }
}

@Composable
private fun PraiseRow(app: App, p: Praise, sub: String, isRecording: Boolean, onRecord: () -> Unit, onImport: () -> Unit) {
    val voice = app.voice
    val has = voice.hasRecording(p)
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(p.text, style = MaterialTheme.typography.titleMedium)
            Text(
                when {
                    isRecording -> "● Aufnahme läuft …"
                    has -> "$sub · eigene Aufnahme"
                    voice.hasBundled(p) -> "$sub · mitgeliefert"
                    else -> "$sub · noch keine Aufnahme"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isRecording) Heart else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledTonalIconButton(
            onClick = onRecord,
            colors = if (isRecording) IconButtonDefaults.filledTonalIconButtonColors(containerColor = Heart, contentColor = androidx.compose.ui.graphics.Color.White)
            else IconButtonDefaults.filledTonalIconButtonColors()
        ) { Icon(if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic, "Aufnehmen") }
        IconButton(onClick = {
            if (has) voice.playFile(voice.fileFor(p)) else voice.speak(p, true)
        }) { Icon(Icons.Rounded.PlayArrow, "Anhören") }
        IconButton(onClick = onImport) { Icon(Icons.Rounded.FileOpen, "Datei importieren") }
        IconButton(onClick = { voice.delete(p) }, enabled = has) { Icon(Icons.Rounded.DeleteOutline, "Löschen") }
    }
}
