package de.schnurrfeld.app

import androidx.test.core.app.ApplicationProvider
import de.schnurrfeld.app.data.Breed
import de.schnurrfeld.app.data.Care
import de.schnurrfeld.app.data.PetRules
import de.schnurrfeld.app.data.Store
import de.schnurrfeld.app.data.TailColor
import de.schnurrfeld.app.data.Needs
import de.schnurrfeld.app.data.Mood
import de.schnurrfeld.app.data.NeedRules
import de.schnurrfeld.app.game.PuzzleGenerator
import de.schnurrfeld.app.game.Solver
import de.schnurrfeld.app.game.TUTORIAL_SEED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PetTest {
    // first 8 items decide the tail, the next 10 the breed
    private fun log(first: String, mid: String) = first + mid

    @Test fun tailColourIsDecidedByTheFirstEightItems() {
        assertEquals(TailColor.RED, PetRules.tailFor("FFFFFMMM"))
        assertEquals(TailColor.BLACK, PetRules.tailFor("YYYYYBBB"))
        assertEquals(TailColor.WHITE, PetRules.tailFor("MMMMMFFF"))
        assertEquals(TailColor.WHITE, PetRules.tailFor("BBBBBYYY"))
        assertEquals(TailColor.BLACK, PetRules.tailFor("FFMMYYBY"))   // Y wins (3×)
    }

    @Test fun allSixBreedsAreReachable() {
        assertEquals(Breed.MOPPEL, PetRules.breedFor(log("FFFFFFFF", "FFFFMMMMBY")))
        assertEquals(Breed.KIRSCHE, PetRules.breedFor(log("FFFFFFFF", "BBBBYYYYFM")))
        assertEquals(Breed.MITTERNACHT, PetRules.breedFor(log("YYYYYYYY", "YYYYFFFFBM")))
        assertEquals(Breed.DOMINO, PetRules.breedFor(log("YYYYYYYY", "BBBBMMMMYF")))
        assertEquals(Breed.KARAMELL, PetRules.breedFor(log("MMMMMMMM", "MMMMFFFFBY")))
        assertEquals(Breed.WOLKE, PetRules.breedFor(log("BBBBBBBB", "BBBBYYYYMF")))
        // only items given before hatching count – later care can't change the breed
        assertEquals(Breed.MOPPEL, PetRules.breedFor(log("FFFFFFFF", "FFFFMMMMBY") + "B".repeat(40)))
    }

    @Test fun stagesNeedItemsAndLevels() {
        assertEquals(0, PetRules.reachable(7, 50))
        assertEquals(0, PetRules.reachable(8, 4))       // the tail needs level 5
        assertEquals(1, PetRules.reachable(8, 5))
        assertEquals(1, PetRules.reachable(17, 50))
        assertEquals(2, PetRules.reachable(18, 15))     // hatching
        assertEquals(PetRules.MAX_STAGE, PetRules.reachable(500, 200))
        assertEquals(6, PetRules.MAX_STAGE)
        assertEquals(PetRules.stageNames.size, PetRules.itemsFor.size)
    }

    @Test fun catGrowsOneStageAtATimeWhenCareAndLevelAllow() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = Store(ctx).apply { resetPet(); updatePet { it.copy(talers = 0) }; updateProgress { it.copy(unlocked = 51, hints = 20, jokers = 5) } }
        // exchange at the machine: 10 tips -> 1 Taler, 5 jokers -> 1 Taler
        assertTrue(store.exchangeHints()); assertTrue(store.exchangeHints()); assertFalse(store.exchangeHints())
        assertTrue(store.exchangeJokers()); assertFalse(store.exchangeJokers())
        assertEquals(3, store.pet.talers); assertEquals(0, store.progress.hints)
        assertTrue(store.buy(Care.FOOD, 1, 1)); assertTrue(store.buy(Care.FOOD, 1, 1)); assertTrue(store.buy(Care.MILK, 1, 1))
        assertFalse(store.buy(Care.MILK, 1, 1))     // out of Taler
        repeat(3) { assertTrue(store.give(if (it < 2) Care.FOOD else Care.MILK)) }
        assertFalse(store.give(Care.FOOD))
        assertFalse(store.petCheckEvolution())       // 3 of 8 items – not yet
        store.updatePet { it.copy(log = it.log + "FFFFF") }
        assertTrue(store.petCheckEvolution())
        assertEquals(1, store.pet.stage)
        assertEquals(TailColor.RED, store.pet.tail)
        assertFalse(store.petCheckEvolution())
        // lots of care at once: one stage per check (each gets its own moment)
        store.updatePet { it.copy(log = it.log + "F".repeat(40)) }
        assertTrue(store.petCheckEvolution()); assertEquals(2, store.pet.stage)
        assertEquals(Breed.MOPPEL, store.pet.breed)
        assertTrue(store.petCheckEvolution()); assertEquals(3, store.pet.stage)
        assertTrue(store.petCheckEvolution()); assertEquals(4, store.pet.stage)
    }

    @Test fun oldSavesAreMovedToTheNewStages() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        // an old save: stage 6 (Jungkatze) in the old 9-stage scheme, no scheme marker yet
        ctx.getSharedPreferences("schnurrfeld", android.content.Context.MODE_PRIVATE).edit()
            .clear().putInt("pet_stage", 6).putInt("pet_seen", 6).putString("pet_log", "F".repeat(45)).putBoolean("pet_intro", true).commit()
        val store = Store(ctx)
        assertEquals(4, store.pet.stage)                       // still a Jungkatze
        assertEquals("Jungkatze", PetRules.stageNames[store.pet.stage])
        assertEquals(4, store.pet.seenStage)                   // no reveal replayed
        assertEquals(4, Store(ctx).pet.stage)                  // migrated only once
    }

    @Test fun talerIncome() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ctx.getSharedPreferences("schnurrfeld", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        val store = Store(ctx)
        assertEquals(PetRules.START_TALERS, store.pet.talers)                 // new player: start capital
        val day = java.time.LocalDate.of(2026, 11, 5)
        assertTrue(store.dailyTalerWaiting(day))
        assertEquals(1 to 0, store.claimDaily(day))                            // no cat yet -> no bonus
        assertEquals(null, store.claimDaily(day))                              // once per day
        assertFalse(store.dailyTalerWaiting(day))
        assertTrue(store.dailyTalerWaiting(day.plusDays(1)))
        assertEquals(PetRules.START_TALERS + 1, store.pet.talers)
    }

    @Test fun needsSinkSlowlyButNeverBelowTheFloor() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = Store(ctx).apply { resetPet() }
        val h = 3_600_000L
        val t0 = 1_000_000_000_000L
        store.updatePet { it.copy(stage = PetRules.HATCH_STAGE, needs = Needs(), needsAt = t0) }
        assertEquals(Mood.HAPPY, store.moodNow(t0))
        assertEquals(Mood.HAPPY, store.moodNow(t0 + 10 * h))               // still fine after 10 h
        assertEquals(Mood.OK, store.moodNow(t0 + 15 * h))
        assertEquals(Mood.SAD, store.moodNow(t0 + 19 * h))                  // ~18 h: misses you
        assertEquals(NeedRules.FLOOR, store.needsNow(t0 + 1000 * h).hunger, 0.001f)   // never worse than a bit sad
        // a happy cat gives a bonus Taler on the daily visit
        store.updatePet { it.copy(needsAt = System.currentTimeMillis()) }
        assertEquals(1 to 1, store.claimDaily(java.time.LocalDate.of(2026, 12, 24)))
    }

    @Test fun itemsAndPettingFillTheNeeds() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = Store(ctx).apply { resetPet() }
        val now = System.currentTimeMillis()
        store.updatePet { it.copy(stage = PetRules.HATCH_STAGE, needs = Needs(0.2f, 0.2f, 0.2f, 0.2f), needsAt = now, stock = mapOf(Care.FOOD to 1)) }
        assertTrue(store.give(Care.FOOD))
        assertEquals(0.7f, store.needsNow().hunger, 0.02f)
        assertEquals(0.2f, store.needsNow().thirst, 0.02f)
        repeat(10) { store.petStroke() }                                     // petting is free and fills "cuddle"
        assertEquals(0.5f, store.needsNow().cuddle, 0.02f)
    }

    @Test fun existingPlayersGetTheirTalersBackPaid() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ctx.getSharedPreferences("schnurrfeld", android.content.Context.MODE_PRIVATE).edit()
            .clear().putInt("pet_talers", 2).putInt("unlocked", 11).commit()      // level 10 solved, 2 Taler
        assertEquals(2 + 5 + 10, Store(ctx).pet.talers)
        assertEquals(17, Store(ctx).pet.talers)                                  // only once
    }

    @Test fun discountsUnlockPacks() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = Store(ctx).apply { resetPet(); updatePet { it.copy(talers = 20) } }
        assertTrue(store.buyDiscount(1, PetRules.PRICE_SILVER))
        assertFalse(store.buyDiscount(1, PetRules.PRICE_SILVER))     // only once
        assertTrue(store.buy(Care.YARN, 5, PetRules.PRICE_PACK5))
        assertEquals(5, store.pet.stockOf(Care.YARN))
        assertEquals(20 - PetRules.PRICE_SILVER - PetRules.PRICE_PACK5, store.pet.talers)
    }

    @Test fun tutorialPuzzleIsUniqueAndEasy() {
        val p = PuzzleGenerator.generate(4, TUTORIAL_SEED, 0f, maxRating = 1)
        assertEquals(1, Solver(4, p.regions).countSolutions(2))
        assertEquals(1, de.schnurrfeld.app.game.LogicRater.rate(p))
    }

    @Test fun corruptCareLogIsCleanedInsteadOfCrashing() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ctx.getSharedPreferences("schnurrfeld", android.content.Context.MODE_PRIVATE).edit().putString("pet_log", "FFxMM??B\u0000Y").commit()
        val pet = Store(ctx).pet
        assertEquals("FFMMBY", pet.log)
        PetRules.breedFor(pet.log)   // must not throw
    }
}
