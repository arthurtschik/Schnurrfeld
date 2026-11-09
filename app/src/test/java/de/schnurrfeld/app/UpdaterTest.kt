package de.schnurrfeld.app

import de.schnurrfeld.app.data.Updater
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
         "assets":[${assets.joinToString(",") { """{"name":"$it","size":1234,"browser_download_url":"https://github.com/x/$it"}""" }}]}
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
        assertEquals("https://github.com/x/Schnurrfeld-1.3.apk", play.apkUrl)
        assertEquals(1234L, play.size)
        assertEquals("Schnurrfeld-1.3-Dev.apk", Updater.parseRelease(json, dev = true)!!.apkName)
        // only one APK attached: both apps take it
        assertEquals("Schnurrfeld-1.3.apk", Updater.parseRelease(release("1.3", "Schnurrfeld-1.3.apk"), dev = true)!!.apkName)
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
}
