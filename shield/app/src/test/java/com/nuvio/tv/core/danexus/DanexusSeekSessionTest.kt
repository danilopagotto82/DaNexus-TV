package com.nuvio.tv.core.danexus

import org.junit.Assert.*
import org.junit.Test

class DanexusSeekSessionTest {
    @Test fun browsingDoesNotSeekOrResumeUntilConfirmation() {
        val session = DanexusSeekSession()
        val effects = mutableListOf<String>()
        var decoderPosition = 50_000L
        session.begin(decoderPosition) { effects += "pause" }
        session.begin(70_000L) { effects += "pause" }
        assertEquals(listOf("pause"), effects)
        assertEquals(50_000L, decoderPosition)
        assertTrue(session.active)
        session.confirm(90_000L, { decoderPosition = it; effects += "seek" }, { effects += "play" })
        assertEquals(90_000L, decoderPosition)
        assertEquals(listOf("pause", "seek", "play"), effects)
        assertFalse(session.active)
        assertFalse(session.confirm(100_000L, { fail("second confirmation sought again") }, { fail("resumed twice") }))
    }

    @Test fun cancellingKeepsTheOriginalPositionAndDoesNotResume() {
        val session = DanexusSeekSession()
        session.begin(12_000L) {}
        assertEquals(12_000L, session.cancel())
        assertFalse(session.active)
        assertFalse(session.confirm(45_000L, { fail("cancelled preview must not seek") }, { fail("cancel must not resume") }))
    }

    @Test fun changingMediaResetsThePreviousSelection() {
        val session = DanexusSeekSession()
        session.begin(12_000L) {}
        session.reset()
        assertFalse(session.active)
        assertNull(session.cancel())
        var pauses = 0
        session.begin(0L) { pauses++ }
        assertEquals(1, pauses)
    }
}
