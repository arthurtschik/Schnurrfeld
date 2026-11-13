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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.schnurrfeld.app.BuildConfig
import de.schnurrfeld.app.data.InstallStatus
import de.schnurrfeld.app.data.ReleaseInfo
import de.schnurrfeld.app.data.Updater
import kotlinx.coroutines.launch
import java.io.File

private enum class Phase { OFFER, DOWNLOADING, PERMISSION, INSTALLING, ERROR }

/** Offers a new release, downloads it inside the app and installs it through Android's PackageInstaller. */
@Composable
fun UpdateDialog(info: ReleaseInfo, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(Phase.OFFER) }
    var progress by remember { mutableFloatStateOf(0f) }
    var apk by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf("") }
    val status by Updater.installStatus.collectAsState()

    fun installOrAsk(file: File) {
        Updater.verify(context, file)?.let { msg -> file.delete(); apk = null; error = msg; phase = Phase.ERROR; return }
        if (Updater.canInstall(context)) { phase = Phase.INSTALLING; Updater.install(context, file) }
        else phase = Phase.PERMISSION
    }

    fun start() {
        apk?.takeIf { it.exists() }?.let { installOrAsk(it); return }
        phase = Phase.DOWNLOADING; progress = 0f
        scope.launch {
            runCatching { Updater.download(context, info) { progress = it } }
                .onSuccess { apk = it; installOrAsk(it) }
                .onFailure { error = it.message ?: "Unbekannter Fehler"; phase = Phase.ERROR }
        }
    }

    // coming back from the "Unbekannte Apps installieren" setting: continue on our own
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME && phase == Phase.PERMISSION && Updater.canInstall(context)) apk?.let { installOrAsk(it) }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    val busy = phase == Phase.DOWNLOADING || (phase == Phase.INSTALLING && status == InstallStatus.Working)
    AlertDialog(
        onDismissRequest = { if (!busy) { Updater.installStatus.value = null; onDismiss() } },
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
                        "Android fragt beim ersten Update einmal nach: Tippe auf \"Erlauben\" und schalte " +
                            "\"Aus dieser Quelle zulassen\" ein. Danach geht es hier von selbst weiter."
                    )
                    Phase.INSTALLING -> when (val st = status) {
                        InstallStatus.Working, null -> {
                            Text("Wird installiert …")
                            Gap(12.dp)
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        InstallStatus.Confirm -> Text("Bestätige das Update im Android-Fenster. Schnurrfeld startet danach neu, dein Fortschritt bleibt erhalten.")
                        InstallStatus.Cancelled -> Text("Das Update wurde abgebrochen. Du kannst es jederzeit erneut starten.")
                        is InstallStatus.Failed -> Text("Die Installation hat nicht geklappt: ${st.msg}")
                    }
                    Phase.ERROR -> Text("Das Update ist nicht möglich: $error")
                }
            }
        },
        confirmButton = {
            when (phase) {
                Phase.OFFER -> TextButton(onClick = ::start) { Text("Jetzt aktualisieren") }
                Phase.PERMISSION -> TextButton(onClick = { Updater.openInstallPermission(context) }) { Text("Erlauben") }
                Phase.INSTALLING -> if (status is InstallStatus.Cancelled || status is InstallStatus.Failed || status == InstallStatus.Confirm)
                    TextButton(onClick = ::start) { Text(if (status == InstallStatus.Confirm) "Nochmal anzeigen" else "Nochmal versuchen") }
                Phase.ERROR -> TextButton(onClick = ::start) { Text("Nochmal versuchen") }
                Phase.DOWNLOADING -> {}
            }
        },
        dismissButton = {
            if (!busy) TextButton(onClick = { Updater.installStatus.value = null; onDismiss() }) {
                Text(if (phase == Phase.OFFER) "Später" else "Schließen")
            }
        },
    )
}

/** GitHub release notes are Markdown – strip the common markup (rules, tables, links, quotes) for plain display. */
fun cleanNotes(md: String): String = md.lines().mapNotNull { raw ->
    val line = raw.trimEnd()
    when {
        line.trim().matches(Regex("[-*_]{3,}")) -> ""                                  // --- rule
        line.trim().matches(Regex("\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?")) -> null  // table separator
        else -> line
            .replace(Regex("^>\\s?"), "")
            .replace(Regex("^#{1,6}\\s*"), "")
            .replace(Regex("^\\s*[-*]\\s+"), "• ")
            .let { if (it.trim().startsWith("|")) it.trim().trim('|').split('|').joinToString(" – ") { c -> c.trim() } else it }
            .replace(Regex("!?\\[([^\\]]*)]\\([^)]*\\)"), "$1")
            .replace("**", "").replace("__", "").replace("`", "")
    }
}.joinToString("\n").replace(Regex("\n{3,}"), "\n\n").trim()
