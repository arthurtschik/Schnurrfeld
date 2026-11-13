package de.schnurrfeld.app

import android.content.Intent
import android.content.pm.PackageInstaller
import androidx.test.core.app.ApplicationProvider
import de.schnurrfeld.app.data.InstallReceiver
import de.schnurrfeld.app.data.InstallStatus
import de.schnurrfeld.app.data.Updater
import org.robolectric.Shadows.shadowOf
import de.schnurrfeld.app.ui.components.cleanNotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)   // org.json needs the Android runtime
class UpdaterTest {
    private fun release(tag: String, vararg assets: String, pre: Boolean = false) = """
        {"tag_name":"$tag","name":"Schnurrfeld $tag","draft":false,"prerelease":$pre,"body":"## Neu\r\n- Kratzbaum",
         "assets":[${assets.joinToString(",") { """{"name":"$it","size":1234,"browser_download_url":"https://github.com/arthurtschik/Schnurrfeld/releases/download/$tag/$it"}""" }}]}
    """.trimIndent()

    @Test fun versions() {
        assertEquals(listOf(1, 2, 1), Updater.parts("v1.2.1"))
        assertEquals(listOf(1, 2), Updater.parts("1.2-dev"))
        assertTrue(Updater.isNewer("v1.3", "1.2"))
        assertTrue(Updater.isNewer("1.2.1", "1.2-dev"))
        assertTrue(Updater.isNewer("1.10", "1.9"))
        assertFalse(Updater.isNewer("1.2", "1.2"))
        assertFalse(Updater.isNewer("1.2", "1.2.0"))
        assertFalse(Updater.isNewer("1.1", "1.2"))
        assertFalse(Updater.isNewer("latest", "1.2"))
    }

    @Test fun picksTheRightApk() {
        val json = release("v1.3", "Schnurrfeld-1.3-Dev.apk", "Schnurrfeld-1.3.apk", "notes.txt")
        val play = Updater.parseRelease(json, dev = false)!!
        assertEquals("1.3", play.version)
        assertEquals("Schnurrfeld-1.3.apk", play.apkName)
        assertEquals("https://github.com/arthurtschik/Schnurrfeld/releases/download/v1.3/Schnurrfeld-1.3.apk", play.apkUrl)
        assertEquals(1234L, play.size)
        assertEquals("Schnurrfeld-1.3-Dev.apk", Updater.parseRelease(json, dev = true)!!.apkName)
        // the apps never take each other's APK (different package names)
        assertNull(Updater.parseRelease(release("1.3", "Schnurrfeld-1.3.apk"), dev = true))
        assertNull(Updater.parseRelease(release("1.3", "Schnurrfeld-1.3-Dev.apk"), dev = false))
    }

    @Test fun ignoresUnusableReleases() {
        assertNull(Updater.parseRelease(release("v1.3", "readme.md"), dev = false))
        assertNull(Updater.parseRelease(release("v1.3", "Schnurrfeld-1.3.apk", pre = true), dev = false))
    }

    @Test fun notesAreCleaned() {
        assertEquals("Neu\n• Kratzbaum\n• Mehr Leckerlis", cleanNotes("## Neu\r\n- **Kratzbaum**\r\n* Mehr Leckerlis\r\n"))
        assertEquals("Titel\n\nDatei – Für\nSchnurrfeld-1.3.apk – die App\n\nHinweis: siehe Anleitung",
            cleanNotes("# Titel\n\n---\n\n| Datei | Für |\n|---|:---:|\n| `Schnurrfeld-1.3.apk` | die App |\n\n> **Hinweis:** siehe [Anleitung](https://x.y)"))
    }

    @Test fun onlyGithubOverHttps() {
        assertTrue(Updater.isTrustedHost("https://github.com/arthurtschik/Schnurrfeld/releases/download/v1.3/a.apk"))
        assertTrue(Updater.isTrustedHost("https://objects.githubusercontent.com/github-production-release-asset/1?x=y"))
        assertTrue(Updater.isTrustedHost("https://release-assets.githubusercontent.com/a"))
        assertFalse(Updater.isTrustedHost("http://github.com/a.apk"))                        // no plain http
        assertFalse(Updater.isTrustedHost("https://evilgithub.com/a.apk"))                   // look-alike
        assertFalse(Updater.isTrustedHost("https://github.com.evil.example/a.apk"))
        assertFalse(Updater.isTrustedHost("https://github.com@evil.example/a.apk"))          // credentials trick
        assertFalse(Updater.isTrustedHost("https://github.com:8443/a.apk"))
        assertFalse(Updater.isTrustedHost("file:///sdcard/a.apk"))
        assertFalse(Updater.isTrustedHost("not a url"))
    }

    @Test fun assetMustBeFromThisRepository() {
        assertTrue(Updater.isTrustedAsset("https://github.com/arthurtschik/Schnurrfeld/releases/download/v1.3/Schnurrfeld-1.3.apk"))
        assertFalse(Updater.isTrustedAsset("https://github.com/someone-else/Schnurrfeld/releases/download/v1.3/Schnurrfeld-1.3.apk"))
        assertFalse(Updater.isTrustedAsset("https://github.com/arthurtschik/Schnurrfeld-fake/releases/download/v1.3/a.apk"))
        // a release whose asset points elsewhere is ignored
        val evil = """{"tag_name":"v9.9","assets":[{"name":"Schnurrfeld-9.9.apk","size":1,"browser_download_url":"https://evil.example/Schnurrfeld-9.9.apk"}]}"""
        assertNull(Updater.parseRelease(evil, dev = false))
        // odd asset names do no harm: the file is always saved as update.apk
        val odd = """{"tag_name":"v1.3","assets":[{"name":"../../x.apk","size":1,"browser_download_url":"https://github.com/arthurtschik/Schnurrfeld/releases/download/v1.3/x.apk"}]}"""
        assertEquals("v1.3".removePrefix("v"), Updater.parseRelease(odd, dev = false)!!.version)
    }

    @Test fun garbageFileIsRejected() {
        val f = java.io.File(ApplicationProvider.getApplicationContext<android.app.Application>().cacheDir, "garbage.apk").apply { writeText("not an apk") }
        assertEquals("Die Datei ist keine gültige App", Updater.verify(ApplicationProvider.getApplicationContext(), f))
    }

    private val ctx get() = ApplicationProvider.getApplicationContext<android.app.Application>()
    private fun receive(status: Int, extra: Intent.() -> Unit = {}) =
        InstallReceiver().onReceive(ctx, Intent().putExtra(PackageInstaller.EXTRA_STATUS, status).apply(extra))

    @Test fun installResults() {
        val confirm = Intent("android.content.pm.action.CONFIRM_INSTALL")
        receive(PackageInstaller.STATUS_PENDING_USER_ACTION) { putExtra(Intent.EXTRA_INTENT, confirm) }
        assertEquals(InstallStatus.Confirm, Updater.installStatus.value)
        assertEquals("android.content.pm.action.CONFIRM_INSTALL", shadowOf(ctx).nextStartedActivity.action)

        receive(PackageInstaller.STATUS_FAILURE_ABORTED)
        assertEquals(InstallStatus.Cancelled, Updater.installStatus.value)

        receive(PackageInstaller.STATUS_FAILURE_CONFLICT)
        assertTrue((Updater.installStatus.value as InstallStatus.Failed).msg.contains("Signatur"))

        receive(PackageInstaller.STATUS_SUCCESS)
        assertNull(Updater.installStatus.value)
    }

    @Test fun installOpensASession() {
        val apk = java.io.File(ctx.cacheDir, "test.apk").apply { writeBytes(ByteArray(4096) { it.toByte() }) }
        Updater.install(ctx, apk)
        val sessions = ctx.packageManager.packageInstaller.allSessions
        assertEquals(1, sessions.size)
        assertEquals(ctx.packageName, sessions[0].appPackageName)
    }
}
