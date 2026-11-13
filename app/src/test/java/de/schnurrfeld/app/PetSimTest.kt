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

    @Test fun grownCatsUseTheScratchingPost() {
        val sim = PetSim(seed = 11).apply { stage = 6 }
        var t = 1000L
        var scratched = false; var climbed = false
        val end = t + 15 * 60_000L          // 15 minutes of cat life
        while (t < end) {
            t += 16; sim.step(t, 0.6f)
            if (sim.scratching) scratched = true
            if (sim.spot != null) climbed = true
        }
        assertTrue("scratches the post", scratched)
        assertTrue("climbs up to the top cushion", climbed)
    }

    @Test fun youngCatsIgnoreThePost() {
        val sim = PetSim(seed = 5).apply { stage = 4 }
        var t = 1000L
        val end = t + 10 * 60_000L
        while (t < end) {
            t += 16; sim.step(t, 0.6f)
            assertTrue("no tree below stage 5", !sim.scratching && sim.spot == null && sim.inside == 0)
        }
    }

    @Test fun catComesDownFromThePostForItsFood() {
        val sim = PetSim(seed = 3).apply { stage = 6 }
        var t = run(sim, 1000, 300)
        sim.debugPose("roofA", t)
        t = run(sim, t, 500)
        assertTrue(sim.spot != null)
        sim.feed(Care.FOOD)
        t = run(sim, t, 6000)
        assertTrue("came down", sim.spot == null)
        assertEquals(Act.FOOD, sim.act)
    }

    @Test fun catMeowsNowAndThenAndSadOnesCallForYou() {
        val sim = PetSim(seed = 9).apply { stage = 4; mood = de.schnurrfeld.app.data.Mood.SAD }
        var t = 1000L; var meows = 0
        val end = t + 3 * 60_000L
        while (t < end) {
            t += 16; sim.step(t, 0.6f)
            while (sim.sounds.isNotEmpty()) if (sim.sounds.removeAt(0).isMeow) meows++
        }
        assertTrue("a sad kitten mews ($meows)", meows >= 5)
    }

    @Test fun foodThatArrivesWhileJumpingUpDoesNotStrandTheCat() {
        val sim = PetSim(seed = 21).apply { stage = 6 }
        var t = 1000L
        // wait until the cat takes off towards the post, then feed it in mid-air
        var guard = 0
        while (!(sim.jumping && sim.jumpKind == 1) && guard++ < 200_000) { t += 16; sim.step(t, 0.6f) }
        assertTrue("cat took off", sim.jumping && sim.jumpKind == 1)
        sim.feed(Care.FOOD)
        t = run(sim, t, 12_000)
        assertTrue("back on the floor", sim.spot == null && sim.inside == 0)
        assertTrue("cat is at the bowl or done", sim.act == Act.NONE || abs(sim.x - sim.itemX) < 0.05f)
    }

    @Test fun catsDisappearIntoTheCaves() {
        var seenA = false; var seenB = false
        for (seed in 1L..6L) {
            val sim = PetSim(seed).apply { stage = 6 }
            var t = 1000L
            val end = t + 20 * 60_000L
            while (t < end) {
                t += 16; sim.step(t, 0.6f)
                if (sim.inside == 1) seenA = true
                if (sim.inside == 2) seenB = true
            }
        }
        assertTrue("goes home to cave A", seenA)
        assertTrue("goes home to cave B", seenB)
    }

    @Test fun foodMakesACatComeOutOfItsCave() {
        for (pose in listOf("homeA", "homeB")) {
            val sim = PetSim(seed = 4).apply { stage = 6 }
            var t = run(sim, 1000, 300)
            sim.debugPose(pose, t)
            t = run(sim, t, 300)
            assertTrue("$pose: at home", sim.inside != 0)
            sim.feed(Care.MILK)
            var drank = false; var cameOut = false
            val end = t + 14_000
            while (t < end) {
                t += 16; sim.step(t, 0.6f)
                if (sim.act == Act.MILK && sim.actPhase() >= 1) drank = true
                if (sim.inside == 0 && sim.spot == null) cameOut = true
            }
            assertTrue("$pose: came out", cameOut)
            assertTrue("$pose: drank the milk", drank)
        }
    }

    @Test fun aCatAtHomeCannotBePetted() {
        val sim = PetSim(seed = 2).apply { stage = 6 }
        var t = run(sim, 1000, 300)
        sim.debugPose("homeA", t)
        t = run(sim, t, 300)
        assertEquals(1f, sim.doorK, 0.001f)
        assertEquals(1, sim.inside)
    }

    /** whatever the cat was about to do (climb, scratch, go home): a new item always gets its attention within a few seconds */
    @Test fun anItemAlwaysGetsTheCatsAttention() {
        for (care in listOf(Care.BRUSH, Care.FOOD, Care.YARN, Care.MILK)) {
            for (seed in 1L..120L) {
                val sim = PetSim(seed).apply { stage = 6 }
                var t = 1000L
                repeat(19) { t += 16; sim.step(t, 0.6f) }
                sim.feed(care)
                var started = false
                val end = t + 4000
                while (t < end) { t += 16; sim.step(t, 0.6f); if (sim.act != Act.NONE && sim.actPhase() >= 1) started = true }
                assertTrue("seed $seed, $care: the cat turned to the item", started)
            }
        }
    }

    /** a toy or the scent in a room calls the cat there – wherever it is and whatever it is doing */
    @Test fun toysCallTheCatIntoTheRoom() {
        for (home in 1..2) for (start in listOf("none", "scratch", "roofA", "roofB", "step", "homeA", "homeB")) for (seed in 1L..6L) {
            val sim = PetSim(seed).apply { stage = 6 }
            var t = run(sim, 1000, 300)
            if (start != "none") sim.debugPose(start, t)
            t = run(sim, t, 300)
            sim.lure(home, t)
            var there = sim.inside == home
            val end = t + 45_000
            while (t < end && !there) { t += 16; sim.step(t, 0.6f); there = sim.inside == home }
            assertTrue("start=$start seed=$seed home=$home: the cat came", there)
        }
    }

    @Test fun aSadCatAlsoComesForAToyAndStays() {
        val sim = PetSim(seed = 8).apply { stage = 6; mood = de.schnurrfeld.app.data.Mood.SAD }
        var t = run(sim, 1000, 300)
        sim.lure(2, t)
        val end = t + 45_000
        var there = false
        while (t < end && !there) { t += 16; sim.step(t, 0.6f); there = sim.inside == 2 }
        assertTrue("came although sad", there)
        t = run(sim, t, 3000)
        assertEquals("stays a while", 2, sim.inside)
    }

    @Test fun lureIsIgnoredForYoungCatsAndWhenAlreadyThere() {
        val young = PetSim(seed = 1).apply { stage = 4 }
        young.lure(1, 1000)
        assertEquals(0, young.headingHome)
        val sim = PetSim(seed = 1).apply { stage = 6 }
        val t = run(sim, 1000, 300)
        sim.debugPose("homeA", t)
        sim.lure(1, t)
        assertEquals(0, sim.headingHome)
    }
}
