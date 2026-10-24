package io.github.mschache.karoopro

import io.github.mschache.karoopro.calc.BikePhysics
import io.github.mschache.karoopro.calc.Climb
import io.github.mschache.karoopro.calc.ClimbProgress
import io.github.mschache.karoopro.calc.ClimbResultTracker
import io.github.mschache.karoopro.calc.ElevationProfile
import io.github.mschache.karoopro.calc.FuelDue
import io.github.mschache.karoopro.calc.FuelItem
import io.github.mschache.karoopro.calc.FuelSizes
import io.github.mschache.karoopro.calc.FuelTracker
import io.github.mschache.karoopro.calc.GearLimit
import io.github.mschache.karoopro.calc.GearLimits
import io.github.mschache.karoopro.calc.Gearing
import io.github.mschache.karoopro.calc.PacePlan
import io.github.mschache.karoopro.calc.Rig
import io.github.mschache.karoopro.calc.SegmentPhase
import io.github.mschache.karoopro.calc.SegmentReading
import io.github.mschache.karoopro.calc.SegmentResultTracker
import io.github.mschache.karoopro.calc.SegmentState
import io.github.mschache.karoopro.calc.StaminaModel
import io.github.mschache.karoopro.calc.TargetBand
import io.github.mschache.karoopro.data.RouteTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GearLimitTest {
    private fun rear(g: Int) = Gearing(frontGear = 2, frontMax = 2, rearGear = g, rearMax = 12, frontTeeth = null, rearTeeth = null)

    @Test fun beepsOnlyWhenShiftingOntoAnEnd() {
        assertEquals(GearLimit.TOP, GearLimits.reached(rear(11), rear(12)))
        assertEquals(GearLimit.BOTTOM, GearLimits.reached(rear(2), rear(1)))
        assertNull(GearLimits.reached(rear(12), rear(12))) // no shift
        assertNull(GearLimits.reached(rear(5), rear(6)))
        assertNull(GearLimits.reached(null, rear(12))) // first reading
    }
}

class WorkoutDeviationTest {
    @Test fun scalesOutsideTheBand() {
        val band = 250 to 270
        assertEquals(0.0, TargetBand.deviation(260, band)!!, 1e-9)
        assertEquals(-10 / 37.5, TargetBand.deviation(240, band)!!, 1e-9)
        assertEquals(-1.0, TargetBand.deviation(150, band)!!, 1e-9)
        assertEquals(1.0, TargetBand.deviation(400, band)!!, 1e-9)
        assertNull(TargetBand.deviation(null, band))
    }
}

class ResultTrackerTest {
    private val climb = Climb(start = 1_000.0, end = 2_000.0, gain = 80.0, avgGrade = 8.0, maxGrade = 10.0)
    private fun progress(number: Int, onClimb: Boolean) =
        ClimbProgress(number, 3, climb, toStart = if (onClimb) -10.0 else 300.0, toTop = 500.0, elevationToTop = 40.0, avgGradeLeft = 8.0)

    @Test fun climbResultWhenToppingOut() {
        val t = ClimbResultTracker()
        assertNull(t.update(progress(1, onClimb = false), 200.0, 20_000.0, 0))
        for (s in 0..10) assertNull(t.update(progress(1, onClimb = true), 300.0, 20_000.0 - s * 500, s * 1000L))
        val r = t.update(progress(2, onClimb = false), 150.0, 15_000.0, 11_000)!!
        assertEquals(1, r.number)
        assertEquals(11, r.seconds)
        assertEquals(300.0, r.avgPower!!, 1e-9)
        assertEquals(15_000.0, r.wPrimeLow!!, 1e-9)
        assertEquals(80.0 / 11 * 3600, r.vam!!, 1e-6)
        // Rolling back before the start of the same climb (a reroute) isn't a result.
        t.update(progress(2, onClimb = true), 300.0, null, 20_000)
        assertNull(t.update(progress(2, onClimb = false), 300.0, null, 21_000))
    }

    @Test fun segmentResultWhenDone() {
        val t = SegmentResultTracker()
        val on = SegmentState(SegmentPhase.ON, SegmentReading(elapsed = 5_000.0, distanceLeft = 900.0, totalDistance = 1_500.0))
        repeat(5) { assertNull(t.update(on, 350.0, it * 1000L)) }
        val done = SegmentState(SegmentPhase.DONE, SegmentReading(elapsed = 131_000.0, prDelta = -7_000.0, komDelta = 41_000.0, totalDistance = 1_500.0))
        val r = t.update(done, 100.0, 6_000)!!
        assertEquals(131, r.seconds)
        assertEquals(350.0, r.avgPower!!, 1e-9)
        assertEquals(-7_000.0, r.prDelta!!, 1e-9)
        assertNull(t.update(done, 100.0, 7_000)) // once
    }
}

class StaminaTest {
    @Test fun ftpForAnHourEmptiesTheLongTermBudget() {
        assertEquals(3600.0, StaminaModel.limitSeconds(250.0, 250.0), 1e-6)
        assertTrue(StaminaModel.limitSeconds(300.0, 250.0) < 600)
        val s = StaminaModel()
        repeat(1800) { s.update(250.0, 250.0) }
        assertEquals(0.5, s.potential, 1e-6)
        repeat(3600) { s.update(100.0, 250.0) } // easy riding doesn't drain it
        assertEquals(0.5, s.potential, 1e-6)
    }

    @Test fun timeLeftUsesWPrimeAboveCp() {
        val s = StaminaModel()
        assertEquals(3600.0, s.secondsLeft(250.0, 250.0, 260.0, null)!!, 1e-6) // fresh: an hour at FTP
        s.restore(0.5)
        assertEquals(1800.0, s.secondsLeft(250.0, 250.0, 260.0, null)!!, 1e-6)
        assertEquals(120.0, s.secondsLeft(320.0, 250.0, 260.0, 120.0)!!, 1e-6)
        assertNull(s.secondsLeft(120.0, 250.0, 260.0, null)) // all day
        assertEquals(0.4, s.now(0.4), 1e-9)
        assertEquals(0.5, s.now(0.9), 1e-9) // capped by the long-term potential
    }
}

class FuelTest {
    private val sizes = FuelSizes(gelCarbs = 25, barCarbs = 40, bottleCarbs = 30, bottleMl = 500)

    @Test fun burnGrowsWithPowerAndIntensity() {
        val f = FuelTracker()
        repeat(3600) { f.addSecond(200.0, 250.0, null, null) }
        // 720 kJ ≈ 717 kcal, about 74 % from carbohydrate at 80 % FTP.
        assertEquals(133.0, f.state.burnedG, 3.0)
        assertEquals(0.95, FuelTracker.carbShare(1.2), 1e-9)
        assertEquals(0.35, FuelTracker.carbShare(0.2), 1e-9)
    }

    @Test fun eatWhenAServingBehindAndNotAgainForTenMinutes() {
        val f = FuelTracker()
        var due: FuelDue? = null
        var at = -1L
        for (s in 1..3600L) {
            f.addSecond(150.0, 250.0, null, null)
            if (f.due(60, sizes, 0) == FuelDue.EAT && due == null) {
                due = FuelDue.EAT
                at = s
            }
        }
        assertEquals(FuelDue.EAT, due)
        assertEquals(1_200 + 25 * 60L, at) // 20 min grace + 25 g at 60 g/h
        f.log(FuelItem.BOTTLE, sizes)
        assertEquals(30.0, f.state.eatenG, 1e-9)
        assertEquals(500.0, f.state.drankMl, 1e-9)
    }
}

class PacingTest {
    private val rig = Rig(massKg = 80.0)

    @Test fun speedFromPowerGradeAndWind() {
        assertEquals(9.3, BikePhysics.speed(200.0, 0.0, rig), 0.4)
        assertEquals(4.6, BikePhysics.speed(250.0, 0.06, rig), 0.5)
        assertTrue(BikePhysics.speed(200.0, 0.0, rig, headwind = 5.0) < BikePhysics.speed(200.0, 0.0, rig))
        assertEquals(15.0, BikePhysics.speed(0.0, -0.06, rig), 1.0)
        assertEquals(BikePhysics.MIN_SPEED, BikePhysics.speed(10.0, 0.15, rig), 1e-9)
    }

    @Test fun planPushesClimbsAndEasesDescents() {
        val plan = PacePlan.build(ElevationProfile(RouteTracker.DEMO_PROFILE), basePower = 200.0, maxPower = 240.0, rig = rig)
        val flat = plan.sectionAt(500.0)!!
        val climb = plan.sectionAt(2_500.0)!!
        val descent = plan.sectionAt(10_500.0)!!
        assertEquals(200, flat.power)
        assertEquals(240, climb.power) // 8 % would be 248 W, capped at CP
        assertTrue(descent.power < 200)
        assertTrue(plan.timeAt(2_500.0) > plan.timeAt(500.0))
        assertEquals(plan.totalSeconds, plan.timeAt(plan.length), 1e-6)
        assertEquals(2_000.0, plan.nextChange(500.0)!!.from, 1e-9)
        assertNotNull(plan.indexAt(0.0))
    }
}

class StripTest {
    private val profile = io.github.mschache.karoopro.data.Profile.DEFAULT

    @Test fun contextValuesTakeTheFirstPlaces() {
        val s = io.github.mschache.karoopro.data.Settings()
        val strip = io.github.mschache.karoopro.ui.field.FieldModelBuilder.strip(io.github.mschache.karoopro.ui.field.DemoSource(s, profile), systemDark = true)
        // The demo rides a segment, a workout and a climb at once: those three, then the first chosen value.
        assertEquals(listOf("VS PR", "TARGET", "TO TOP", "W' BAL"), strip.cells.map { it.label })
    }

    @Test fun onlyTheChosenValuesWithoutContext() {
        val s = io.github.mschache.karoopro.data.Settings(stripContext = false)
        val strip = io.github.mschache.karoopro.ui.field.FieldModelBuilder.strip(io.github.mschache.karoopro.ui.field.DemoSource(s, profile), systemDark = true)
        assertEquals(s.stripMetrics.map { it.label }, strip.cells.map { it.label })
    }
}
