package io.github.mschache.karoopro

import io.github.mschache.karoopro.calc.BlockTotals
import io.github.mschache.karoopro.calc.TargetBand
import io.github.mschache.karoopro.calc.TargetStatus
import io.github.mschache.karoopro.calc.UnitPrefs
import io.github.mschache.karoopro.data.LapMetric
import io.github.mschache.karoopro.data.Metric
import io.github.mschache.karoopro.data.Panel
import io.github.mschache.karoopro.data.Profile
import io.github.mschache.karoopro.data.Settings
import io.github.mschache.karoopro.ui.field.DemoSource
import io.github.mschache.karoopro.ui.field.FieldModelBuilder
import io.github.mschache.karoopro.ui.field.LapContent
import io.github.mschache.karoopro.ui.field.Palette
import io.github.mschache.karoopro.ui.field.PathSink
import io.github.mschache.karoopro.ui.field.RideContent
import io.github.mschache.karoopro.ui.field.SvgPath
import io.github.mschache.karoopro.ui.field.TileFamily
import io.github.mschache.karoopro.ui.field.TileGraphic
import io.github.mschache.karoopro.ui.field.TileGraphics
import io.github.mschache.karoopro.ui.field.TileIcon
import io.github.mschache.karoopro.ui.field.TileStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TileGraphicsTest {
    @Test fun hoursFillOneSegmentPerHour() {
        assertEquals(listOf(0.5f, 0f, 0f, 0f), TileGraphics.hours(1_800_000.0))
        assertEquals(listOf(1f, 1f, 0.25f, 0f), TileGraphics.hours(2.25 * 3_600_000))
        // Past four hours a segment is added per hour, up to eight.
        assertEquals(6, TileGraphics.hours(5.5 * 3_600_000).size)
        assertEquals(8, TileGraphics.hours(11.0 * 3_600_000).size)
        assertEquals(listOf(0f, 0f, 0f, 0f), TileGraphics.hours(null))
    }

    @Test fun spreadSharesAFractionOverSegments() {
        assertEquals(listOf(1f, 1f, 0.5f, 0f, 0f), TileGraphics.spread(0.5f))
        assertEquals(listOf(1f, 1f, 1f, 1f, 1f), TileGraphics.spread(1f))
    }

    @Test fun routeAndTimeFractions() {
        assertEquals(0.25f, TileGraphics.routeFraction(10_000.0, 30_000.0)!!, 1e-6f)
        assertNull(TileGraphics.routeFraction(10_000.0, null))
        assertNull(TileGraphics.routeFraction(10_000.0, 0.0))
        // 1 h ridden, 11:00 now, arriving 14:00: a quarter of the way in time.
        assertEquals(0.25f, TileGraphics.timeFraction(3_600_000.0, 39_600_000.0, 50_400_000.0, 0L)!!, 1e-6f)
        // Arrival just after midnight wraps.
        assertEquals(0.5f, TileGraphics.timeFraction(3_600_000.0, 82_800_000.0, 0.0, 0L)!!, 1e-6f)
        // Epoch arrival: compared with now.
        assertEquals(0.5f, TileGraphics.timeFraction(3_600_000.0, null, 1_760_003_600_000.0, 1_760_000_000_000L)!!, 1e-6f)
        assertNull(TileGraphics.timeFraction(3_600_000.0, 39_600_000.0, null, 0L))
    }

    @Test fun cadenceScaleKeepsTheBandInTheMiddle() {
        assertEquals(55f to 125f, TileGraphics.cadenceScale(85, 95))
    }
}

class TileStyleTest {
    @Test fun iconsAndFamilies() {
        assertEquals(TileIcon.TIMER, TileStyle.look(Metric.ELAPSED_TIME).icon)
        assertTrue(TileStyle.look(Metric.ELAPSED_TIME).named)
        assertEquals(TileFamily.DISTANCE, TileStyle.look(Metric.DISTANCE).family)
        assertEquals(TileFamily.ENERGY, TileStyle.look(Metric.CALORIES).family)
        assertEquals(TileFamily.CADENCE, TileStyle.look(Metric.CADENCE).family)
        // Variants keep their label next to the icon.
        assertEquals(TileIcon.BOLT, TileStyle.look(Metric.AVG_POWER).icon)
        assertFalse(TileStyle.look(Metric.AVG_POWER).named)
        assertNull(TileStyle.look(Metric.TSS).icon)
    }

    @Test fun everySelectableMetricHasALook() {
        // Labels stay for anything the icon doesn't name; nothing is left without a label and without an icon.
        Metric.selectableEntries.forEach { m ->
            val look = TileStyle.look(m)
            assertTrue("$m", look.icon != null || !look.named)
        }
    }

    @Test fun zoneColorsDarkenOnTheLightTheme() {
        val zone = 0xFFFFC400.toInt()
        assertEquals(zone, TileStyle.color(TileFamily.POWER, Palette.DARK, zone, null))
        assertTrue(TileStyle.color(TileFamily.POWER, Palette.LIGHT, zone, null) != zone)
        assertEquals(Palette.DARK.dim, TileStyle.color(TileFamily.POWER, Palette.DARK, null, null))
    }
}

class SvgPathTest {
    private class Recorder : PathSink {
        val ops = ArrayList<String>()
        override fun moveTo(x: Float, y: Float) { ops += "M $x $y" }
        override fun lineTo(x: Float, y: Float) { ops += "L $x $y" }
        override fun quadTo(x1: Float, y1: Float, x: Float, y: Float) { ops += "Q $x1 $y1 $x $y" }
        override fun close() { ops += "Z" }
    }

    @Test fun absoluteAndRelativeCommands() {
        val r = Recorder()
        SvgPath.parse("M10-20h5v-5l-5 5H0V0Z", r)
        assertEquals(listOf("M 10.0 -20.0", "L 15.0 -20.0", "L 15.0 -25.0", "L 10.0 -20.0", "L 0.0 -20.0", "L 0.0 0.0", "Z"), r.ops)
    }

    @Test fun quadraticsAndTheirReflection() {
        val r = Recorder()
        SvgPath.parse("M0 0q10 0 10 10t10 10", r)
        assertEquals(listOf("M 0.0 0.0", "Q 10.0 0.0 10.0 10.0", "Q 10.0 20.0 20.0 20.0"), r.ops)
    }

    @Test fun compactNumbersSplit() {
        val r = Recorder()
        // "28.5.5" is 28.5 then .5; "-11.5-11.5" two negatives.
        SvgPath.parse("M28.5.5l-11.5-11.5", r)
        assertEquals(listOf("M 28.5 0.5", "L 17.0 -11.0"), r.ops)
    }

    @Test fun everyIconParses() {
        TileIcon.entries.forEach { icon ->
            val r = Recorder()
            SvgPath.parse(icon.path, r)
            assertTrue("$icon", r.ops.size > 3 && r.ops.last() == "Z")
        }
    }
}

class RideTilesTest {
    private val settings = Settings()

    @Test fun demoTilesGetTheirGraphics() {
        val tiles = (FieldModelBuilder.build(DemoSource(settings, Profile.DEFAULT, t = 30), Panel.RIDE, listOf(Panel.RIDE), 256f, 426f, 1.875f, true, true).content as RideContent).tiles
        // Default tiles: ride time, distance, cadence, calories. The demo is navigating.
        assertTrue(tiles[0].graphic is TileGraphic.Segments)
        assertTrue(tiles[1].graphic is TileGraphic.Route)
        assertTrue(tiles[2].graphic is TileGraphic.Band)
        assertTrue(tiles[3].graphic is TileGraphic.Bars)
        assertEquals(8, (tiles[3].graphic as TileGraphic.Bars).values.size)
        assertEquals(TileIcon.FIRE, tiles[3].icon)
        assertFalse(tiles[3].showLabel)
        // The unit moved to the corner, the value stands alone.
        assertEquals("rpm", tiles[2].unit)
        val band = tiles[2].graphic as TileGraphic.Band
        assertEquals(85f, band.low)
        assertEquals(95f, band.high)
    }

    @Test fun statusLineShowsSunsetAndTemperature() {
        val hero = FieldModelBuilder.build(DemoSource(settings, Profile.DEFAULT), Panel.RIDE, listOf(Panel.RIDE), 256f, 426f, 1.875f, true, true).hero
        // Demo clock 11:00, sunset in 4:18.
        assertEquals("15:18", hero.status!!.sunset)
        assertEquals("19°", hero.status!!.temperature)
        val off = FieldModelBuilder.build(DemoSource(settings.copy(statusLine = false), Profile.DEFAULT), Panel.RIDE, listOf(Panel.RIDE), 256f, 426f, 1.875f, true, true).hero
        assertNull(off.status)
    }

    @Test fun lapAveragesGetADelta() {
        val lap = FieldModelBuilder.build(DemoSource(settings, Profile.DEFAULT), Panel.LAP, listOf(Panel.LAP), 256f, 426f, 1.875f, true, true).content as LapContent
        // TIME, POWER, SPEED, HR by default: the lap time keeps growing, so it gets no delta.
        assertNull(lap.fields[0].delta)
        assertTrue(lap.fields[1].delta!!.startsWith("+"))
        assertEquals("+3.9", lap.fields[2].delta)
        assertEquals("−2", lap.fields[3].delta)
    }

    @Test fun cadenceBandIsNormalized() {
        val s = Settings(cadenceLow = 130, cadenceHigh = 120).normalized()
        assertEquals(130, s.cadenceLow)
        assertEquals(135, s.cadenceHigh)
        assertEquals(40, Settings(cadenceLow = 10).normalized().cadenceLow)
    }
}

class RideMathAdditionsTest {
    @Test fun blockTotalsSplitARunningTotal() {
        val b = BlockTotals(blockSeconds = 3, keep = 2)
        assertTrue(b.values().isEmpty())
        listOf(0.0, 5.0, 10.0).forEach { b.add(it) }
        assertEquals(listOf(10.0, 0.0), b.values())
        listOf(12.0, null, 20.0).forEach { b.add(it) }
        assertEquals(listOf(10.0, 10.0, 0.0), b.values())
        listOf(21.0, 22.0, 23.0, 30.0).forEach { b.add(it) }
        // Only the newest two finished blocks are kept; the running one has 30 - 23.
        assertEquals(listOf(10.0, 3.0, 7.0), b.values())
        val copy = BlockTotals(blockSeconds = 3, keep = 2).apply { restore(b.state) }
        assertEquals(b.values(), copy.values())
    }

    @Test fun cueSaysHowFarToTheBand() {
        assertEquals("PUSH +6 W", TargetBand.cue(TargetStatus.PUSH, 259, 265, 290, "W"))
        assertEquals("EASE −12 W", TargetBand.cue(TargetStatus.EASE, 302, 265, 290, "W"))
        assertEquals("IN RANGE", TargetBand.cue(TargetStatus.IN_RANGE, 270, 265, 290, "W"))
        assertEquals("PUSH ▲", TargetBand.cue(TargetStatus.PUSH, null, 265, 290, "W"))
        assertEquals("PUSH +4 bpm", TargetBand.cue(TargetStatus.PUSH, 136, 140, 150, "bpm"))
    }

    @Test fun lapDeltas() {
        val metric = UnitPrefs(metricDistance = true, metricElevation = true, metricTemperature = true)
        assertEquals("+20", LapMetric.POWER.delta(226.0, 206.0, metric))
        assertEquals("−1.1", LapMetric.SPEED.delta(9.0, 9.0 + 1.1 / 3.6, metric))
        assertEquals("±0", LapMetric.HR.delta(150.2, 150.0, metric))
        assertNull(LapMetric.TIME.delta(500_000.0, 600_000.0, metric))
        assertNull(LapMetric.POWER.delta(226.0, null, metric))
    }
}
