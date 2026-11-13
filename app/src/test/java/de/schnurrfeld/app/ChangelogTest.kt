package de.schnurrfeld.app

import de.schnurrfeld.app.data.Changelog
import de.schnurrfeld.app.data.Updater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogTest {
    @Test fun theInstalledVersionHasAnEntry() {
        // forgetting to write the changelog for a new version fails this test
        assertEquals(BuildConfig.VERSION_NAME.removeSuffix("-dev"), Changelog.entries.first().version)
    }

    @Test fun newestFirstAndNothingEmpty() {
        val v = Changelog.entries.map { it.version }
        assertEquals("no version twice", v.size, v.toSet().size)
        for (i in 1 until v.size) assertTrue("${v[i - 1]} is newer than ${v[i]}", Updater.isNewer(v[i - 1], v[i]))
        for (e in Changelog.entries) {
            assertTrue("${e.version} has a title", e.title.isNotBlank())
            assertTrue("${e.version} has sections", e.sections.isNotEmpty())
            for (s in e.sections) { assertTrue(s.title.isNotBlank()); assertTrue("${e.version}/${s.title} has items", s.items.isNotEmpty() && s.items.all { it.isNotBlank() }) }
        }
    }

    @Test fun noEmojis() {
        val text = Changelog.entries.flatMap { listOf(it.title) + it.sections.flatMap { s -> listOf(s.title) + s.items } }.joinToString("\n")
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            assertFalse("emoji U+${cp.toString(16)}", cp > 0xFFFF || Character.getType(cp) == Character.OTHER_SYMBOL.toInt())
            i += Character.charCount(cp)
        }
    }

    @Test fun carriesTheChangesOfVersion13() {
        // 1.3 was never published on its own: its changes live in 1.4
        val all = Changelog.entries.first { it.version == "1.4" }.sections.flatMap { listOf(it.title) + it.items }.joinToString(" ")
        for (word in listOf("Kratzbaum", "Wohnhöhlen", "Duftspender", "500", "Miaus", "Tageszeit", "gesichert")) assertTrue("1.4 mentions $word", all.contains(word))
    }
}
