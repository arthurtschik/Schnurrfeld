package de.schnurrfeld.app

import de.schnurrfeld.app.ui.components.Sky
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SkyTest {
    @Test fun nightAndDay() {
        for (h in listOf(0f, 2f, 4.9f, 21f, 23.5f)) assertEquals("night at $h", 1f, Sky.night(h), 0.001f)
        for (h in listOf(7.5f, 12f, 17.9f)) assertEquals("day at $h", 0f, Sky.night(h), 0.001f)
    }

    @Test fun twilightIsInBetween() {
        for (h in listOf(5.5f, 6.5f, 18.5f, 19.5f, 20f)) assertTrue("twilight at $h", Sky.night(h) in 0.05f..0.95f)
        // gets darker in the evening, lighter in the morning
        assertTrue(Sky.night(19f) < Sky.night(20f)); assertTrue(Sky.night(6.5f) < Sky.night(5.5f))
    }

    @Test fun warmLightAtSunriseAndSunsetOnly() {
        assertTrue(Sky.warm(6.3f) > 0.9f); assertTrue(Sky.warm(19.2f) > 0.9f)
        assertEquals(0f, Sky.warm(12f), 0.001f); assertEquals(0f, Sky.warm(2f), 0.001f)
    }

    @Test fun theHourCanBePinnedForTests() {
        Sky.hourOverride = 3.25f
        assertEquals(3.25f, Sky.hour(), 0.001f)
        Sky.hourOverride = null
        assertTrue(Sky.hour() in 0f..24f)
    }
}
