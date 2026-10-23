package io.github.mschache.karoopro

import io.github.mschache.karoopro.calc.StepSummary
import io.github.mschache.karoopro.calc.TargetKind
import io.github.mschache.karoopro.calc.TargetStatus
import io.github.mschache.karoopro.calc.Wind
import io.github.mschache.karoopro.calc.WorkoutState
import io.github.mschache.karoopro.coach.CoachContract
import io.github.mschache.karoopro.coach.CoachEffort
import io.github.mschache.karoopro.coach.CoachInbox
import io.github.mschache.karoopro.coach.CoachInfo
import io.github.mschache.karoopro.coach.CoachMessage
import io.github.mschache.karoopro.coach.CoachStatus
import io.github.mschache.karoopro.data.Panel
import io.github.mschache.karoopro.data.PanelSetting
import io.github.mschache.karoopro.data.Profile
import io.github.mschache.karoopro.data.Settings
import io.github.mschache.karoopro.ui.field.CoachContent
import io.github.mschache.karoopro.ui.field.DemoSource
import io.github.mschache.karoopro.ui.field.FieldModelBuilder
import io.github.mschache.karoopro.ui.field.TextFit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachInfoTest {
    private val row = mapOf<String, Any?>(
        CoachContract.COL_VERSION to 1L,
        CoachContract.COL_CONFIGURED to 1L,
        CoachContract.COL_ACTIVE to 1L,
        CoachContract.COL_RIDING to 1L,
        CoachContract.COL_LAST_CHECK_IN_AT to 9_000L,
        CoachContract.COL_LAST_ERROR to null,
        CoachContract.COL_MESSAGE_ID to "m1",
        CoachContract.COL_MESSAGE_TITLE to "Coach",
        CoachContract.COL_MESSAGE_TEXT to "Ease to 200 W",
        CoachContract.COL_MESSAGE_AT to 8_000L,
        CoachContract.COL_ACKNOWLEDGED to 0L,
    )

    @Test fun parsesTheProviderRow() {
        val info = CoachInfo.from(row)!!
        assertTrue(info.configured && info.active && info.riding)
        assertEquals(9_000L, info.lastCheckInAt)
        assertEquals(CoachMessage("m1", "Coach", "Ease to 200 W", 8_000L), info.message)
        assertTrue(info.unread)
        assertFalse(CoachInfo.from(row + (CoachContract.COL_ACKNOWLEDGED to 1L))!!.unread)
    }

    @Test fun noMessageYet() {
        val info = CoachInfo.from(row + (CoachContract.COL_MESSAGE_ID to null) + (CoachContract.COL_MESSAGE_TEXT to null))!!
        assertNull(info.message)
        assertFalse(info.unread)
        // A blank title falls back to "Coach".
        assertEquals("Coach", CoachInfo.from(row + (CoachContract.COL_MESSAGE_TITLE to " "))!!.message!!.title)
    }

    @Test fun refusesUnknownContracts() {
        assertNull(CoachInfo.from(row + (CoachContract.COL_VERSION to 2L)))
        assertNull(CoachInfo.from(row - CoachContract.COL_VERSION))
    }
}

class CoachStatusTest {
    private val now = 1_000_000L
    private val live = CoachInfo(configured = true, active = true, riding = true, lastCheckInAt = now - 5_000, lastError = null, message = null, acknowledged = false)

    @Test fun followsTheCoachField() {
        assertEquals(CoachStatus.LIVE, CoachStatus.of(live, now))
        assertEquals(CoachStatus.NOT_SET_UP, CoachStatus.of(live.copy(configured = false), now))
        assertEquals(CoachStatus.WAITING, CoachStatus.of(live.copy(riding = false), now))
        assertEquals(CoachStatus.OFF, CoachStatus.of(live.copy(active = false), now))
        assertEquals(CoachStatus.CONNECTING, CoachStatus.of(live.copy(lastCheckInAt = null), now))
        // One failed check-in after a good one is still live; offline after 90 s without success.
        assertEquals(CoachStatus.LIVE, CoachStatus.of(live.copy(lastError = "HTTP 502"), now))
        assertEquals(CoachStatus.OFFLINE, CoachStatus.of(live.copy(lastError = "HTTP 502", lastCheckInAt = now - 120_000), now))
        assertTrue(CoachStatus.OFFLINE.warning)
    }
}

class CoachInboxTest {
    private fun info(id: String, at: Long, acked: Boolean = false) =
        CoachInfo(configured = true, active = true, riding = true, lastCheckInAt = at, lastError = null, message = CoachMessage(id, "Coach", "Drink", at), acknowledged = acked)

    @Test fun opensOncePerFreshUnreadMessage() {
        val inbox = CoachInbox()
        val now = 10_000_000L
        assertFalse(inbox.isNew(null, now))
        assertTrue(inbox.isNew(info("a", now - 1_000), now))
        assertFalse("same message, next refresh", inbox.isNew(info("a", now - 1_000), now + 1_000))
        assertFalse("read on the Coach field", inbox.isNew(info("b", now, acked = true), now))
        assertTrue(inbox.isNew(info("c", now), now))
    }

    @Test fun oldMessagesDontOpen() {
        val now = 10_000_000L
        assertFalse(CoachInbox().isNew(info("a", now - 6 * 60_000), now))
    }
}

class CoachEffortTest {
    private val step = WorkoutState(
        stepNumber = 4, stepCount = 8, kind = TargetKind.POWER, low = 265, high = 290, output = 280, status = TargetStatus.IN_RANGE,
        stepRemainingS = 120, totalRemainingS = 900, stepElapsedS = 60, stepAverage = 276, inTargetPercent = 91, progress = 0.33f,
    )

    @Test fun sendsEverythingKnownInSiUnits() {
        val values = CoachEffort.values(
            wPrimeJ = 4_200.0, wPrimeMaxJ = 20_000.0, cpW = 250.0,
            best = mapOf(5 to 810.0, 60 to 402.0, 300 to 281.0, 1200 to 255.0, 30 to 500.0),
            // 5 m/s from 30° left of straight ahead.
            wind = Wind.components(5.0, 330.0, 0.0),
            step = step,
            lastStep = StepSummary(index = 2, kind = TargetKind.HEART_RATE, low = 150, high = 160, seconds = 300, average = 156, inTargetPercent = 88),
        )
        assertEquals(
            mapOf(
                CoachContract.W_PRIME_J to 4_200.0, CoachContract.W_PRIME_MAX_J to 20_000.0, CoachContract.CP_W to 250.0,
                CoachContract.BEST_5 to 810.0, CoachContract.BEST_60 to 402.0, CoachContract.BEST_300 to 281.0, CoachContract.BEST_1200 to 255.0,
                CoachContract.STEP_AVG to 276.0, CoachContract.STEP_IN_TARGET to 91.0,
                CoachContract.LAST_STEP_INDEX to 2.0, CoachContract.LAST_STEP_KIND to 2.0,
                CoachContract.LAST_STEP_LOW to 150.0, CoachContract.LAST_STEP_HIGH to 160.0, CoachContract.LAST_STEP_SECONDS to 300.0,
                CoachContract.LAST_STEP_AVG to 156.0, CoachContract.LAST_STEP_IN_TARGET to 88.0,
            ),
            values - CoachContract.HEAD_MS - CoachContract.CROSS_MS,
        )
        assertEquals(4.33, values.getValue(CoachContract.HEAD_MS), 0.01)
        // The crosswind is sent as a size, whichever side it comes from.
        assertEquals(2.5, values.getValue(CoachContract.CROSS_MS), 0.01)
        assertEquals("best_300", CoachContract.BEST_300)
        assertEquals("last_step_in_target", CoachContract.LAST_STEP_IN_TARGET)
    }

    @Test fun leavesOutWhatIsUnknown() {
        val none = CoachEffort.values(null, 20_000.0, 250.0, mapOf(5 to null), wind = null, step = null, lastStep = null)
        assertTrue("CP and W′ come only with a W′ balance", none.isEmpty())
        val open = CoachEffort.values(
            null, 20_000.0, 250.0, emptyMap(), Wind.components(3.0, 180.0, 0.0),
            step.copy(kind = TargetKind.NONE, low = null, high = null, stepAverage = null, inTargetPercent = null),
            StepSummary(5, TargetKind.NONE, null, null, 42, null, null),
        )
        assertEquals(
            setOf(CoachContract.HEAD_MS, CoachContract.CROSS_MS, CoachContract.LAST_STEP_INDEX, CoachContract.LAST_STEP_KIND, CoachContract.LAST_STEP_SECONDS),
            open.keys,
        )
        assertEquals(-3.0, open.getValue(CoachContract.HEAD_MS), 1e-9)
        assertEquals(0.0, open.getValue(CoachContract.LAST_STEP_KIND), 0.0)
    }
}

class CoachPanelTest {
    private val now = 50_000_000L
    private val settings = Settings(panels = Panel.entries.map { PanelSetting(it) })

    private fun content(coach: CoachInfo?): CoachContent {
        val src = DemoSource(settings, Profile.DEFAULT, liveCoach = coach, nowMs = now)
        val model = FieldModelBuilder.build(src, Panel.COACH, Panel.entries, 255f, 342f, 1.875f, interactive = true, systemDark = true)
        return model.content as CoachContent
    }

    @Test fun showsMessageStatusAndAge() {
        val info = CoachInfo(true, true, true, now - 3_000, null, CoachMessage("m", "Coach", "Spin up", now - 120_000), false)
        val c = content(info)
        assertEquals("Spin up", c.text)
        assertEquals("LIVE", c.status)
        assertEquals("2 min ago", c.age)
        assertEquals("TAP TO CONFIRM", c.hint)
        assertNull("the default title isn't repeated", c.title)
        assertTrue(c.unread && !c.placeholder)
        val read = content(info.copy(acknowledged = true, message = info.message!!.copy(title = "Pacing")))
        assertEquals("READ", read.hint)
        assertEquals("PACING", read.title)
        assertFalse(read.unread)
    }

    @Test fun waitsForTheFirstMessage() {
        val c = content(CoachInfo(true, false, false, null, null, null, false))
        assertTrue(c.placeholder)
        assertEquals("WAITING FOR A RIDE", c.status)
        assertEquals("", c.hint)
    }
}

class TextFitTest {
    // Every character is half a cap height wide.
    private val measure: (String) -> Float = { it.length * 0.5f }

    @Test fun wrapsWordsAndSplitsLongOnes() {
        assertEquals(listOf("hold 220", "W to the", "top"), TextFit.wrap("hold 220 W to the top", 4f, measure))
        assertEquals(listOf("abcdefgh", "ij"), TextFit.wrap("abcdefghij", 4f, measure))
        assertEquals(listOf("one", "two"), TextFit.wrap("one\n\ntwo", 10f, measure))
    }

    @Test fun shrinksUntilItFits() {
        val short = TextFit.fit("Drink", 100f, 60f, maxCap = 20f, minCap = 5f, measure = measure)
        assertEquals(20f, short.capHeight, 0f)
        assertEquals(listOf("Drink"), short.lines)
        val long = TextFit.fit("Nice and steady. Hold 220 W to the top, then spin easy", 100f, 60f, maxCap = 20f, minCap = 5f, measure = measure)
        assertTrue(long.capHeight < 20f)
        assertTrue(TextFit.height(long.lines.size, long.capHeight) <= 60f)
        assertTrue(long.lines.all { measure(it) * long.capHeight <= 100f })
    }

    @Test fun cutsWithAnEllipsisAtTheSmallestSize() {
        val fitted = TextFit.fit("word ".repeat(200), 100f, 30f, maxCap = 10f, minCap = 10f, measure = measure)
        assertEquals(10f, fitted.capHeight, 0f)
        assertTrue(TextFit.height(fitted.lines.size, 10f) <= 30f)
        assertTrue(fitted.lines.last().endsWith("…"))
    }
}
