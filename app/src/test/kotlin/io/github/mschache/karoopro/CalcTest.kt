package io.github.mschache.karoopro

import io.github.mschache.karoopro.calc.BestEfforts
import io.github.mschache.karoopro.calc.Gearing
import io.github.mschache.karoopro.calc.LapCompare
import io.github.mschache.karoopro.calc.Polyline
import io.github.mschache.karoopro.calc.SampleHistory
import io.github.mschache.karoopro.calc.StepSummary
import io.github.mschache.karoopro.calc.TargetBand
import io.github.mschache.karoopro.calc.TargetKind
import io.github.mschache.karoopro.calc.TargetStatus
import io.github.mschache.karoopro.calc.TimeInZone
import io.github.mschache.karoopro.calc.Trend
import io.github.mschache.karoopro.calc.WPrimeBalance
import io.github.mschache.karoopro.calc.Wind
import io.github.mschache.karoopro.calc.WindKind
import io.github.mschache.karoopro.calc.WorkoutEvent
import io.github.mschache.karoopro.calc.WorkoutSample
import io.github.mschache.karoopro.calc.WorkoutTracker
import io.github.mschache.karoopro.calc.ZoneColors
import io.github.mschache.karoopro.calc.ZoneKind
import io.github.mschache.karoopro.calc.ZoneMath
import io.github.mschache.karoopro.calc.ZoneRange
import io.github.mschache.karoopro.calc.bucketAverages
import io.github.mschache.karoopro.weather.OpenMeteo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoneMathTest {
    private val power = listOf(ZoneRange(0, 109), ZoneRange(110, 149), ZoneRange(150, 179), ZoneRange(180, 209), ZoneRange(210, 239), ZoneRange(240, 299), ZoneRange(300, 2000))

    @Test fun boundaries() {
        assertEquals(1, ZoneMath.zoneOf(0.0, power))
        assertEquals(1, ZoneMath.zoneOf(109.0, power))
        assertEquals(1, ZoneMath.zoneOf(109.6, power)) // gap between zones belongs to the lower one
        assertEquals(2, ZoneMath.zoneOf(110.0, power))
        assertEquals(7, ZoneMath.zoneOf(300.0, power))
        assertEquals(7, ZoneMath.zoneOf(5000.0, power))
        assertNull(ZoneMath.zoneOf(null, power))
        assertNull(ZoneMath.zoneOf(150.0, emptyList()))
    }

    @Test fun colorsCoverAnyZoneCount() {
        assertEquals(ZoneColors.color(ZoneKind.POWER, 1, 7), ZoneColors.color(ZoneKind.POWER, 1, 3))
        assertEquals(ZoneColors.color(ZoneKind.POWER, 7, 7), ZoneColors.color(ZoneKind.POWER, 3, 3))
        assertEquals(ZoneColors.color(ZoneKind.HR, 5, 5), ZoneColors.color(ZoneKind.HR, 9, 5)) // clamped
    }

    @Test fun contrast() {
        assertEquals(0xFF000000.toInt(), ZoneColors.onColor(0xFFFFF500.toInt())) // yellow -> black text
        assertEquals(0xFFFFFFFF.toInt(), ZoneColors.onColor(0xFFB700A2.toInt())) // purple -> white text
    }
}

class BestEffortsTest {
    @Test fun rollingWindows() {
        val b = BestEfforts(intArrayOf(5, 60))
        repeat(10) { b.add(100.0) }
        repeat(5) { b.add(400.0) }
        repeat(60) { b.add(200.0) }
        assertEquals(400.0, b.bestFor(5)!!, 1e-9)
        // Best 60 s window: the 5 x 400 W plus 55 x 200 W.
        assertEquals((5 * 400.0 + 55 * 200.0) / 60, b.bestFor(60)!!, 1e-9)
    }

    @Test fun shortRideHasNoLongEffort() {
        val b = BestEfforts(intArrayOf(5, 1200))
        repeat(30) { b.add(250.0) }
        assertEquals(250.0, b.bestFor(5)!!, 1e-9)
        assertNull(b.bestFor(1200))
    }

    @Test fun missingPowerCountsAsZero() {
        val b = BestEfforts(intArrayOf(5))
        b.add(500.0)
        repeat(4) { b.add(null) }
        assertEquals(100.0, b.bestFor(5)!!, 1e-9)
    }
}

class WPrimeTest {
    @Test fun drainsAboveCpAndRecoversBelow() {
        val w = WPrimeBalance(cp = 250.0, wPrime = 20_000.0)
        repeat(60) { w.update(350.0) } // 100 W over CP for 60 s = 6 kJ
        assertEquals(14_000.0, w.balance, 1e-6)
        val drained = w.balance
        repeat(120) { w.update(150.0) }
        assertTrue(w.balance > drained)
        assertTrue(w.balance < 20_000.0)
    }

    @Test fun neverAboveWPrime() {
        val w = WPrimeBalance(cp = 250.0, wPrime = 20_000.0)
        repeat(600) { w.update(0.0) }
        assertEquals(20_000.0, w.balance, 1e-6)
    }

    @Test fun recoveryIsFasterWhenMoreDepleted() {
        val deep = WPrimeBalance(250.0, 20_000.0)
        repeat(150) { deep.update(350.0) } // 15 kJ down
        val shallow = WPrimeBalance(250.0, 20_000.0)
        repeat(30) { shallow.update(350.0) } // 3 kJ down
        val d0 = deep.balance
        val s0 = shallow.balance
        deep.update(100.0)
        shallow.update(100.0)
        assertTrue(deep.balance - d0 > shallow.balance - s0)
    }
}

class HistoryTest {
    @Test fun ringBufferKeepsNewest() {
        val h = SampleHistory(3)
        listOf(1.0, 2.0, null, 4.0).forEach { h.add(it) }
        val a = h.toArray()
        assertEquals(3, a.size)
        assertEquals(2.0, a[0], 0.0)
        assertTrue(a[1].isNaN())
        assertEquals(4.0, a[2], 0.0)
    }

    @Test fun bucketsPadOlderSideWithGaps() {
        val samples = DoubleArray(40) { 100.0 }
        val bars = bucketAverages(samples, 60, 3)
        assertNull(bars[0])
        assertEquals(100.0, bars[1]!!, 0.0)
        assertEquals(100.0, bars[2]!!, 0.0)
    }

    @Test fun timeInZoneGrows() {
        val t = TimeInZone(5)
        t.add(3)
        t.add(3)
        t.add(7) // beyond the array: grows
        t.add(null)
        assertEquals(2L, t.seconds[2])
        assertEquals(1L, t.seconds[6])
    }
}

class WindTest {
    @Test fun components() {
        // Riding north (0°): wind from the north is a pure headwind.
        Wind.components(5.0, 0.0, 0.0).let {
            assertEquals(5.0, it.head, 1e-9)
            assertEquals(0.0, it.cross, 1e-9)
            assertEquals(WindKind.HEAD, it.kind)
        }
        // Wind from the south while riding north pushes you.
        Wind.components(5.0, 180.0, 0.0).let {
            assertEquals(-5.0, it.head, 1e-9)
            assertEquals(WindKind.TAIL, it.kind)
        }
        // Wind from the east while riding north comes from the right.
        Wind.components(5.0, 90.0, 0.0).let {
            assertEquals(0.0, it.head, 1e-9)
            assertEquals(5.0, it.cross, 1e-9)
            assertEquals(WindKind.CROSS, it.kind)
        }
        // Riding west, wind from the south: from the left.
        Wind.components(5.0, 180.0, 270.0).let { assertEquals(-5.0, it.cross, 1e-9) }
    }

    @Test fun wrapAround() {
        assertEquals(2.0, Wind.normalize(359.0 - 357.0), 1e-9)
        assertEquals(-2.0, Wind.normalize(1.0 - 3.0 + 360.0 - 360.0), 1e-9)
        assertEquals(2.0, Wind.components(1.0, 1.0, 359.0).relativeFromDeg, 1e-9)
        assertEquals(-2.0, Wind.components(1.0, 359.0, 1.0).relativeFromDeg, 1e-9)
        assertEquals(180.0, Wind.normalize(-180.0), 1e-9)
    }
}

class LapCompareTest {
    @Test fun trends() {
        assertEquals(Trend.UP, LapCompare.trend(250.0, 230.0))
        assertEquals(Trend.DOWN, LapCompare.trend(220.0, 230.0))
        assertEquals(Trend.SAME, LapCompare.trend(231.0, 230.0))
        assertEquals(Trend.NONE, LapCompare.trend(231.0, null))
        assertEquals(Trend.NONE, LapCompare.trend(231.0, 0.0))
    }
}

class DrivetrainTest {
    private val cassette = listOf(11, 12, 13, 14, 15, 17, 19, 21, 24, 27, 30, 34)

    private fun twoBy(front: Int, rearTeeth: Int) = Gearing(
        frontGear = front, frontMax = 2, rearGear = null, rearMax = 12,
        frontTeeth = if (front == 2) 50 else 34, rearTeeth = rearTeeth,
        frontTeethList = listOf(34, 50), rearTeethList = cassette,
    )

    @Test fun crossChainOnTwoByTwelve() {
        assertTrue(twoBy(2, 34).crossChained) // big-big
        assertTrue(twoBy(2, 27).crossChained) // third-largest cog still counts on 12-speed
        assertFalse(twoBy(2, 24).crossChained)
        assertTrue(twoBy(1, 11).crossChained) // small-small
        assertFalse(twoBy(1, 15).crossChained)
        assertEquals(50.0 / 21, twoBy(2, 21).ratio!!, 1e-9)
    }

    @Test fun oneByNeverCrossChains() {
        val g = Gearing(frontGear = 1, frontMax = 1, rearGear = 1, rearMax = 12, frontTeeth = 40, rearTeeth = 52)
        assertFalse(g.crossChained)
        assertTrue(g.isOneBy)
    }

    @Test fun indexFallbackWithoutTeeth() {
        // Rear gear 1 is the largest cog by convention.
        val g = Gearing(frontGear = 2, frontMax = 2, rearGear = 1, rearMax = 11, frontTeeth = null, rearTeeth = null)
        assertTrue(g.crossChained)
        assertEquals(0f, g.rearPosition!!, 0f)
    }
}

class WorkoutTrackerTest {
    private fun sample(step: Int, remainingMs: Long?, output: Double? = 270.0, count: Int = 8) = WorkoutSample(
        stepIndex = step, stepCount = count, stepRemainingMs = remainingMs, totalRemainingMs = 1_000_000,
        kind = TargetKind.POWER, targetValue = null, targetMin = 265.0, targetMax = 290.0, output = output,
    )

    @Test fun statusAndBands() {
        assertEquals(TargetStatus.PUSH, TargetBand.status(250, 265 to 290))
        assertEquals(TargetStatus.IN_RANGE, TargetBand.status(265, 265 to 290))
        assertEquals(TargetStatus.EASE, TargetBand.status(291, 265 to 290))
        assertEquals(TargetStatus.NO_DATA, TargetBand.status(null, 265 to 290))
        // A single target value gets the implied ±5 % power band.
        assertEquals(190 to 210, TargetBand.bounds(TargetKind.POWER, 200.0, null, null))
        assertEquals(185 to 215, TargetBand.bounds(TargetKind.HEART_RATE, 200.0, 200.0, 200.0))
    }

    @Test fun countdownAndStepChange() {
        val t = WorkoutTracker()
        val events = mutableListOf<WorkoutEvent>()
        var now = 0L
        // 12 s left, sampled 4x per second down to the step change.
        var remaining = 12_000L
        while (remaining > 0) {
            events += t.update(sample(1, remaining), now)
            now += 250
            remaining -= 250
        }
        events += t.update(sample(2, 300_000), now)
        val countdown = events.filterIsInstance<WorkoutEvent.Countdown>().map { it.secondsLeft }
        assertEquals(listOf(10, 5, 4, 3, 2, 1), countdown)
        val started = events.filterIsInstance<WorkoutEvent.StepStarted>()
        assertEquals(1, started.size)
        assertEquals(3, started.single().state.stepNumber)
        assertEquals(3, events.filterIsInstance<WorkoutEvent.Countdown>().first().nextStepNumber)
    }

    @Test fun noCountdownOnLastStepAndFinishEvent() {
        val t = WorkoutTracker()
        val events = mutableListOf<WorkoutEvent>()
        events += t.update(sample(7, 10_000), 0)
        events += t.update(sample(7, 5_000), 5_000)
        assertTrue(events.none { it is WorkoutEvent.Countdown })
        events += t.update(null, 6_000)
        assertTrue(events.last() is WorkoutEvent.Finished)
        assertNull(t.state)
    }

    @Test fun stepStatsSampleOncePerSecond() {
        val t = WorkoutTracker()
        t.update(sample(0, 60_000, 270.0), 0)
        t.update(sample(0, 59_750, 300.0), 250) // same second: not counted
        t.update(sample(0, 59_000, 250.0), 1_000)
        val s = t.state!!
        assertEquals(260, s.stepAverage)
        assertEquals(50, s.inTargetPercent)
        assertEquals(TargetStatus.PUSH, s.status)
    }

    @Test fun summarizesEachFinishedStep() {
        val t = WorkoutTracker()
        t.update(sample(2, 3_000, 270.0), 10_000)
        t.update(sample(2, 2_000, 300.0), 11_000)
        t.update(sample(2, 1_000, 280.0), 12_000)
        assertNull("no step has ended yet", t.lastFinished)
        t.update(sample(3, 60_000, 200.0), 13_000)
        assertEquals(StepSummary(index = 2, kind = TargetKind.POWER, low = 265, high = 290, seconds = 3, average = 283, inTargetPercent = 67), t.lastFinished)
        // The final step is summarized when the workout ends, and kept until the next one starts.
        t.update(sample(3, 59_000, 210.0), 14_000)
        t.update(null, 15_500)
        assertEquals(StepSummary(3, TargetKind.POWER, 265, 290, 2, 205, 0), t.lastFinished)
        t.update(null, 16_000)
        assertEquals(3, t.lastFinished!!.index)
        t.update(sample(0, 60_000), 20_000)
        assertNull(t.lastFinished)
        t.update(sample(1, 60_000), 21_000)
        t.reset()
        assertNull(t.lastFinished)
    }
}

class PolylineTest {
    @Test fun decodesGoogleExample() {
        // Google's documented example: (38.5,-120.2), (40.7,-120.95), (43.252,-126.453)
        val pts = Polyline.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@", 5)
        assertEquals(3, pts.size)
        assertEquals(38.5, pts[0].first, 1e-9)
        assertEquals(-120.2, pts[0].second, 1e-9)
        assertEquals(43.252, pts[2].first, 1e-9)
        assertEquals(-126.453, pts[2].second, 1e-9)
    }
}

class OpenMeteoTest {
    private val body = """
        {"latitude":47.37,"longitude":8.54,"utc_offset_seconds":7200,
         "current":{"time":1760000400,"temperature_2m":14.2,"wind_speed_10m":4.1,"wind_direction_10m":225,"wind_gusts_10m":9.3},
         "hourly":{"time":[1759996800,1760000400,1760004000,1760007600],
           "temperature_2m":[13.0,14.0,15.0,16.0],
           "wind_speed_10m":[3.0,4.0,5.0,6.0],
           "wind_direction_10m":[200,210,220,230],
           "wind_gusts_10m":[7.0,8.0,9.0,10.0],
           "precipitation_probability":[10,20,30,null]}}
    """.trimIndent()

    @Test fun parses() {
        val w = OpenMeteo.parse(body, nowMs = 1_760_001_000_000L)
        assertEquals(4.1, w.windSpeed!!, 1e-9)
        assertEquals(225.0, w.windFromDeg!!, 1e-9)
        assertEquals(9.3, w.gust!!, 1e-9)
        assertEquals(20, w.rainChance) // the hour containing "now"
        assertEquals(2, w.hours.size) // only future hours
        assertEquals(1_760_004_000L, w.hours.first().epochSeconds)
        assertNull(w.hours.last().rainChance)
    }

    @Test fun urlAsksForSmallPayload() {
        val url = OpenMeteo.url(47.3769, 8.5417)
        assertTrue(url.contains("latitude=47.377"))
        assertTrue(url.contains("forecast_hours=7"))
        assertTrue(url.contains("wind_speed_unit=ms"))
    }
}
