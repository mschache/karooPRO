package io.github.mschache.karoopro

import io.github.mschache.karoopro.coach.CoachContract
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
