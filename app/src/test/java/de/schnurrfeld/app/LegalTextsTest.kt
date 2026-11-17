package de.schnurrfeld.app

import de.schnurrfeld.app.data.LegalTexts
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalTextsTest {
    private fun all(d: LegalTexts.Doc) = d.blocks.joinToString("\n") { it.heading + "\n" + it.text }

    @Test fun impressumHasProviderAddressAndMail() {
        val t = all(LegalTexts.impressum)
        for (needle in listOf("Arthurtschik", "c/o Block Services", "Stuttgarter Str. 106", "70736 Fellbach", "arthurtschik.business@gmail.com"))
            assertTrue("fehlt im Impressum: $needle", needle in t)
    }

    @Test fun privacyMatchesWhatTheAppDoes() {
        val t = all(LegalTexts.datenschutz)
        assertTrue("GitHub", "api.github.com" in t)
        assertTrue("Verantwortlicher", "Stuttgarter Str. 106" in t)
        assertTrue("Rechte", "Auskunft" in t && "Aufsichtsbehörde" in t)
        // the manifest must not ask for more than the text promises
        val manifest = java.io.File("src/main/AndroidManifest.xml").readText()
        val perms = Regex("uses-permission android:name=\"android.permission.([A-Z_]+)\"").findAll(manifest).map { it.groupValues[1] }.toSet()
        assertTrue("unerwartete Berechtigungen: $perms", perms == setOf("VIBRATE", "INTERNET", "REQUEST_INSTALL_PACKAGES"))
    }

    @Test fun termsExistAndHaveNoEmojis() {
        for (d in listOf(LegalTexts.impressum, LegalTexts.datenschutz, LegalTexts.nutzung)) {
            assertTrue(d.title, d.blocks.isNotEmpty())
            assertTrue("Emoji in ${d.title}", all(d).none { Character.isSurrogate(it) })
        }
    }
}
