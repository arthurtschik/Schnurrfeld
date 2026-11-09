package de.schnurrfeld.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.schnurrfeld.app.BuildConfig
import de.schnurrfeld.app.data.ReleaseInfo
import de.schnurrfeld.app.data.Updater
import kotlinx.coroutines.launch
import java.io.File

private enum class Phase { OFFER, DOWNLOADING, PERMISSION, READY, ERROR }

/** Offers a new release, downloads it and hands it to the Android installer. */
@Composable
fun UpdateDialog(info: ReleaseInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(Phase.OFFER) }
    var progress by remember { mutableFloatStateOf(0f) }
    var apk by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf("") }

    fun installOrAsk(file: File) {
        if (Updater.canInstall(context)) { phase = Phase.READY; Updater.install(context, file) }
        else phase = Phase.PERMISSION
    }

    fun start() {
        apk?.let { installOrAsk(it); return }
        phase = Phase.DOWNLOADING; progress = 0f
        scope.launch {
            runCatching { Updater.download(context, info) { progress = it } }
                .onSuccess { apk = it; installOrAsk(it) }
                .onFailure { error = it.message ?: "Unbekannter Fehler"; phase = Phase.ERROR }
        }
    }

    AlertDialog(
        onDismissRequest = { if (phase != Phase.DOWNLOADING) onDismiss() },
        title = { Text("Update auf ${info.version}") },
        text = {
            Column {
                when (phase) {
                    Phase.OFFER -> {
                        Text("Eine neue Version von Schnurrfeld ist da. Du hast gerade ${BuildConfig.VERSION_NAME.removeSuffix("-dev")}.",
                            style = MaterialTheme.typography.bodyMedium)
                        if (info.notes.isNotBlank()) {
                            Gap(12.dp)
                            Text("Neu:", style = MaterialTheme.typography.titleSmall)
                            Gap(4.dp)
                            Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                                Text(cleanNotes(info.notes), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (info.size > 0) {
                            Gap(8.dp)
                            Text("Download: %.1f MB".format(info.size / 1_048_576f), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Phase.DOWNLOADING -> {
                        Text("Wird heruntergeladen … ${(progress * 100).toInt()} %")
                        Gap(12.dp)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    }
                    Phase.PERMISSION -> Text(
                        "Android fragt beim ersten Update einmal nach: Erlaube Schnurrfeld unter \"Unbekannte Apps installieren\", " +
                            "komm dann zurück und tippe auf \"Installieren\"."
                    )
                    Phase.READY -> Text("Der Android-Installer ist geöffnet. Bestätige dort das Update – dein Fortschritt bleibt erhalten.")
                    Phase.ERROR -> Text("Das hat leider nicht geklappt: $error")
                }
            }
        },
        confirmButton = {
            when (phase) {
                Phase.OFFER -> TextButton(onClick = ::start) { Text("Jetzt aktualisieren") }
                Phase.DOWNLOADING -> {}
                Phase.PERMISSION -> Column {
                    TextButton(onClick = { Updater.openInstallPermission(context) }) { Text("Erlauben") }
                    TextButton(onClick = ::start) { Text("Installieren") }
                }
                Phase.READY -> TextButton(onClick = ::start) { Text("Nochmal öffnen") }
                Phase.ERROR -> TextButton(onClick = ::start) { Text("Nochmal versuchen") }
            }
        },
        dismissButton = {
            if (phase != Phase.DOWNLOADING) TextButton(onClick = onDismiss) { Text(if (phase == Phase.OFFER) "Später" else "Schließen") }
        },
    )
}

/** GitHub release notes are Markdown – strip the most common markup for plain display. */
fun cleanNotes(md: String): String = md.lines().joinToString("\n") { line ->
    line.trimEnd()
        .replace(Regex("^#{1,6}\\s*"), "")
        .replace(Regex("^\\s*[-*]\\s+"), "• ")
        .replace("**", "").replace("__", "").replace("`", "")
}.replace(Regex("\n{3,}"), "\n\n").trim()
