package de.schnurrfeld.app

import de.schnurrfeld.app.data.Care
import de.schnurrfeld.app.ui.components.CatAnim
import de.schnurrfeld.app.ui.screens.Act
import de.schnurrfeld.app.ui.screens.PetSim
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PetSimTest {
    private fun run(sim: PetSim, from: Long, ms: Long, log: Boolean = false): Long {
        var t = from
        while (t < from + ms) {
            t += 16; sim.step(t, 0.6f)
            if (log && t % 400 < 16) println("t=${t - from} act=${sim.act} phase=${sim.actPhase()} x=${"%.2f".format(sim.x)} item=${"%.2f".format(sim.itemX)} anim=${sim.anim} level=${"%.2f".format(sim.level)}")
        }
        return t
    }

    @Test fun catWalksToTheBowlAndEatsItEmpty() {
        val sim = PetSim().apply { stage = 5 }
        var t = run(sim, 1000, 500)
        sim.feed(Care.FOOD)
        t = run(sim, t, 2500, log = true)
        assertEquals(Act.FOOD, sim.act)
        assertTrue("cat stands at the bowl", abs(sim.x - sim.itemX) < 0.03f)
        assertEquals(CatAnim.EAT, sim.anim)
        t = run(sim, t, 4500)
        assertEquals(0f, sim.level, 0.01f)
        run(sim, t, 3000)
        assertEquals(Act.NONE, sim.act)
    }

    @Test fun brushingMakesTheCatPurr() {
        val sim = PetSim().apply { stage = 6 }
        var t = run(sim, 1000, 300)
        sim.feed(Care.BRUSH)
        t = run(sim, t, 1500)
        assertEquals(Act.BRUSH, sim.act)
        assertTrue(sim.wantsPurr)
        run(sim, t, 7000)
        assertEquals(Act.NONE, sim.act)
    }

    @Test fun yarnPlayDoesNotFlicker() {
        val sim = PetSim().apply { stage = 6 }
        var t = run(sim, 1000, 300)
        sim.feed(Care.YARN)
        var changes = 0; var last = sim.anim; var swats = 0
        val end = t + 9000
        while (t < end) {
            t += 16; sim.step(t, 0.6f)
            if (sim.anim != last) { changes++; if (sim.anim == CatAnim.SWAT) swats++; last = sim.anim }
        }
        println("yarn: $changes pose changes, $swats swats")
        assertTrue("cat plays with the ball", swats >= 2)
        assertTrue("no flickering between poses ($changes changes)", changes < 30)
    }
}
