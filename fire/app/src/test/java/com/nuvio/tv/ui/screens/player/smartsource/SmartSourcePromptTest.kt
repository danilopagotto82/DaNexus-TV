package com.nuvio.tv.ui.screens.player.smartsource

import org.junit.Assert.*
import org.junit.Test

class SmartSourcePromptTest {
    @Test fun noPromptBeforeFiveActiveSeconds() {
        val gate = SmartSourcePromptGate()
        assertFalse(gate.offerIfDue(4_999L, true))
        assertTrue(gate.offerIfDue(5_000L, true))
    }
    @Test fun promptIsOfferedOnlyOncePerAttempt() {
        val gate = SmartSourcePromptGate()
        assertTrue(gate.offerIfDue(5_000L, true))
        assertFalse(gate.offerIfDue(7_000L, true))
        assertFalse(gate.offerIfDue(12_000L, true))
    }
    @Test fun pausedOrStalePlaybackCannotOfferPrompt() {
        val gate = SmartSourcePromptGate()
        assertFalse(gate.offerIfDue(6_000L, false))
        assertTrue(gate.offerIfDue(6_000L, true))
    }
    @Test fun newAttemptCanOfferItsOwnPrompt() {
        assertTrue(SmartSourcePromptGate().offerIfDue(5_000L, true))
        assertTrue(SmartSourcePromptGate().offerIfDue(5_000L, true))
    }
    @Test fun pausedWallTimeDoesNotReachPromptThreshold() {
        val clock = FirstFrameWatchdog(12_000L)
        val gate = SmartSourcePromptGate()
        clock.start(0L)
        clock.tick(4_000L, false)
        clock.tick(8_000L, true)
        clock.tick(14_000L, false)
        assertFalse(gate.offerIfDue(clock.waitingMs, true))
        clock.tick(15_000L, false)
        assertTrue(gate.offerIfDue(clock.waitingMs, true))
    }
}
