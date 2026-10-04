package com.nuvio.tv.ui.screens.player.smartsource

import org.junit.Assert.*
import org.junit.Test

class SourceReputationTest {
    private fun source(id: String = "a", score: Double = 70.0, seeds: Int? = null) =
        SmartSource(id, "addon", "mirror", id, score = score, seeds = seeds)

    @Test fun oneFailureNeverQuarantinesSource() {
        val now = 1_000_000L
        val rep = LocalSourceReputationPolicy.evaluate(
            listOf(SourceReputationEvent("addon", "mirror", PlaybackHealth.NETWORK_TIMEOUT, now - 1_000L)),
            source(), nowMs = now
        )
        assertFalse(rep.circuitOpen)
        assertTrue(rep.score > 18.0)
    }

    @Test fun twoRecentConsecutiveFailuresTemporarilyDemoteWithoutRemoving() {
        val now = 20_000_000L
        val events = listOf(
            SourceReputationEvent("addon", "mirror", PlaybackHealth.HTTP_FATAL, now - 2_000L),
            SourceReputationEvent("addon", "mirror", PlaybackHealth.STALL, now - 1_000L),
        )
        val rep = LocalSourceReputationPolicy.evaluate(events, source(), nowMs = now)
        assertTrue(rep.circuitOpen)
        assertTrue(rep.score <= 18.0)
    }

    @Test fun laterSuccessClosesCircuitAndRecoversRanking() {
        val now = 30_000_000L
        val events = listOf(
            SourceReputationEvent("addon", "mirror", PlaybackHealth.NETWORK_TIMEOUT, now - 3_000L),
            SourceReputationEvent("addon", "mirror", PlaybackHealth.STALL, now - 2_000L),
            SourceReputationEvent("addon", "mirror", PlaybackHealth.FIRST_FRAME_FAST, now - 1_000L, firstFrameMs = 900L),
        )
        val rep = LocalSourceReputationPolicy.evaluate(events, source(), nowMs = now)
        assertFalse(rep.circuitOpen)
        assertTrue(rep.score > 18.0)
    }

    @Test fun manualSkipIsNotCountedAsFailure() {
        val now = 40_000_000L
        val events = List(4) {
            SourceReputationEvent("addon", "mirror", PlaybackHealth.USER_SKIP, now - it * 1_000L)
        }
        val rep = LocalSourceReputationPolicy.evaluate(events, source(), nowMs = now)
        assertEquals(70.0, rep.score, 0.01)
        assertEquals(0.0, rep.samples, 0.01)
    }

    @Test fun localLearningReordersAlternativesButNeverTheClickedSource() {
        val clicked = source("clicked")
        val slow = SmartSource("slow", "addon", "slow-mirror", "slow")
        val fast = SmartSource("fast", "addon", "fast-mirror", "fast")
        val now = 50_000_000L
        val events = listOf(
            SourceReputationEvent("addon", "slow-mirror", PlaybackHealth.NETWORK_TIMEOUT, now - 2_000L),
            SourceReputationEvent("addon", "slow-mirror", PlaybackHealth.STALL, now - 1_000L),
            SourceReputationEvent("addon", "fast-mirror", PlaybackHealth.FIRST_FRAME_FAST, now - 1_000L, firstFrameMs = 700L),
        )
        val learned = LocalSourceReputationPolicy.apply(listOf(slow, fast), events, "", null, now)
        val queue = SmartSourceQueue(clicked, learned)
        assertEquals(listOf("clicked", "fast", "slow"), queue.snapshot().map { it.identity })
    }
}
