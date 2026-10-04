package com.nuvio.tv.ui.screens.player.smartsource

import org.junit.Assert.*
import org.junit.Test

class SmartSourceArchitectureTest {
    private fun source(id: String, addon: String = "akashi", score: Double = 70.0,
                       seeds: Int? = null) = SmartSource(id, addon, "mirror-$id", id, score = score, seeds = seeds)

    @Test fun clickedThenRealMirrorsThenOtherAllowedOrigins() {
        val clicked = source("chosen", score = 10.0)
        val queue = SmartSourceQueue(clicked, listOf(source("best", "bestcine", 100.0), source("mirror", score = 90.0), clicked))
        assertEquals(listOf("chosen", "mirror", "best"), queue.snapshot().map { it.identity })
    }
    @Test fun preservesSameAddonAndSameMirrorWithDifferentRequests() {
        val queue = SmartSourceQueue(source("a"), listOf(source("b"), source("c")))
        assertEquals(3, queue.snapshot().size)
    }
    @Test fun lateAlternativesDoNotResetChosenOrVisited() {
        val queue = SmartSourceQueue(source("a"), emptyList())
        queue.update(listOf(source("a"), source("b")))
        queue.update(listOf(source("c")))
        assertEquals("b", queue.next(setOf("a"))?.identity)
        assertEquals("c", queue.next(setOf("a", "b"))?.identity)
    }
    @Test fun knownZeroSeedsPenalizedButUnknownNotInvented() {
        val queue = SmartSourceQueue(source("a"), listOf(source("zero", score = 100.0, seeds = 0), source("unknown", score = 60.0)))
        assertEquals(listOf("a", "unknown", "zero"), queue.snapshot().map { it.identity })
    }
    @Test fun firstFrameIsTheOnlyStartupSuccess() {
        val watchdog = FirstFrameWatchdog(5_000L)
        watchdog.start(0L)
        assertFalse(watchdog.tick(4_999L, false))
        assertTrue(watchdog.tick(5_000L, false))
        assertEquals(5_001L, watchdog.firstFrame(5_001L))
        assertFalse(watchdog.tick(50_000L, false))
        assertNull(watchdog.firstFrame(50_001L))
    }
    @Test fun safeLocalTimeoutBoundsApplyToBackendValues() {
        assertEquals(4_000L, FirstFrameWatchdog(-1L).timeoutMs)
        assertEquals(12_000L, FirstFrameWatchdog(Long.MAX_VALUE).timeoutMs)
        assertEquals(8_000L, FirstFrameWatchdog(99_000L, kids = true).timeoutMs)
        assertEquals(7_000L, FirstFrameWatchdog(null).timeoutMs)
    }
    @Test fun pauseAndBackgroundDoNotConsumeStartupDeadline() {
        val watchdog = FirstFrameWatchdog(4_000L)
        watchdog.start(0L)
        assertFalse(watchdog.tick(1_000L, false))
        assertFalse(watchdog.tick(10_000L, true))
        assertFalse(watchdog.tick(30_000L, true))
        assertFalse(watchdog.tick(31_000L, false))
        assertFalse(watchdog.tick(33_999L, false))
        assertTrue(watchdog.tick(34_000L, false))
    }
    @Test fun codecGetsExactlyOneAlternateEngineThenNextSource() {
        val policy = SourceFallbackPolicy()
        val first = policy.begin("a", SmartEngine.EXO)!!
        assertEquals(FallbackDecision.RETRY_OTHER_ENGINE, policy.failure(first, PlaybackHealth.CODEC_ERROR))
        val second = policy.begin("a", SmartEngine.MPV)!!
        assertEquals(FallbackDecision.NEXT_SOURCE, policy.failure(second, PlaybackHealth.CODEC_ERROR))
        assertNull(policy.begin("a", SmartEngine.EXO))
    }
    @Test fun httpFatalSkipsWithoutChangingEngine() {
        for (status in listOf(401, 403, 404, 410)) {
            val health = PlaybackHealthClassifier.failure(status, codec = true)!!
            assertEquals(PlaybackHealth.HTTP_FATAL, health)
            val policy = SourceFallbackPolicy()
            assertEquals(FallbackDecision.NEXT_SOURCE, policy.failure(policy.begin("a", SmartEngine.EXO)!!, health))
        }
    }
    @Test fun staleAndDuplicateCallbacksCannotConsumeAnotherAttempt() {
        val policy = SourceFallbackPolicy()
        val a = policy.begin("a", SmartEngine.EXO)!!
        policy.failure(a, PlaybackHealth.NETWORK_TIMEOUT)
        assertEquals(FallbackDecision.IGNORE, policy.failure(a, PlaybackHealth.CODEC_ERROR))
        val b = policy.begin("b", SmartEngine.EXO)!!
        assertEquals(FallbackDecision.IGNORE, policy.failure(a, PlaybackHealth.HTTP_FATAL))
        assertTrue(policy.isCurrent(b))
        assertEquals(2, policy.attemptCount)
    }
    @Test fun sourceAndEngineBudgetsPreventInfiniteLoops() {
        val policy = SourceFallbackPolicy(maxSources = 2, maxAttempts = 3)
        val a = policy.begin("a", SmartEngine.EXO)!!
        policy.failure(a, PlaybackHealth.CODEC_ERROR)
        val alt = policy.begin("a", SmartEngine.MPV)!!
        policy.failure(alt, PlaybackHealth.CODEC_ERROR)
        val b = policy.begin("b", SmartEngine.EXO)!!
        assertEquals(FallbackDecision.EXHAUSTED, policy.failure(b, PlaybackHealth.NETWORK_TIMEOUT))
        assertNull(policy.begin("c", SmartEngine.EXO))
    }
    @Test fun exitingInvalidatesDelayedCallbacksAndPreventsFurtherPlayback() {
        val policy = SourceFallbackPolicy()
        val token = policy.begin("a", SmartEngine.EXO)!!
        policy.cancel()
        assertFalse(policy.isCurrent(token))
        assertEquals(FallbackDecision.IGNORE, policy.failure(token, PlaybackHealth.STALL))
        assertNull(policy.begin("b", SmartEngine.EXO))
    }
    @Test fun prolongedNoProgressTriggersStallEvenWhenBufferCounterGrows() {
        val watchdog = PlaybackProgressWatchdog()
        watchdog.observe(0L, 100L, false, true, false, false)
        assertEquals(9_999L, watchdog.observe(9_999L, 100L, false, true, false, false).stalledForMs)
        assertEquals(10_000L, watchdog.observe(10_000L, 100L, false, true, false, false).stalledForMs)
        assertEquals(PlaybackHealth.STALL, PlaybackHealthClassifier.failure(stallMs = 10_000L))
    }
    @Test fun pauseSeekAndNaturalEndDoNotCauseFallback() {
        val watchdog = PlaybackProgressWatchdog()
        watchdog.observe(0L, 100L, false, true, false, false)
        assertEquals(0L, watchdog.observe(100_000L, 100L, true, true, false, false).stalledForMs)
        assertEquals(0L, watchdog.observe(100_001L, 100L, false, true, false, false).stalledForMs)
        assertEquals(0L, watchdog.observe(300_000L, 100L, false, true, false, true).stalledForMs)
    }
    @Test fun stablePlaybackRequiresContinuousRealProgress() {
        val watchdog = PlaybackProgressWatchdog()
        watchdog.observe(0L, 0L, false, true, true, false)
        assertEquals(5_000L, watchdog.observe(5_000L, 5_000L, false, true, true, false).stableProgressMs)
        assertEquals(10_000L, watchdog.observe(10_000L, 10_000L, false, true, true, false).stableProgressMs)
        assertEquals(0L, watchdog.observe(11_000L, 12_000L, true, true, true, false).stableProgressMs)
    }
    @Test fun mpvAudioProgressAndMetadataAloneDoNotProveFirstFrame() {
        assertFalse(PlaybackHealthClassifier.mpvFirstFrameEvidence(true, false, 1920L))
        assertFalse(PlaybackHealthClassifier.mpvFirstFrameEvidence(false, true, 1920L))
        assertFalse(PlaybackHealthClassifier.mpvFirstFrameEvidence(true, true, 0L))
        assertTrue(PlaybackHealthClassifier.mpvFirstFrameEvidence(true, true, 1920L))
    }
    @Test fun controlledBadSourceThenGoodSourceStartsWithOneUserClick() {
        val queue = SmartSourceQueue(source("http404"), listOf(source("neverFirstFrame"), source("good")))
        val policy = SourceFallbackPolicy()
        val trace = mutableListOf<String>()
        var userClicks = 1
        while (true) {
            val candidate = queue.next(policy.attemptedSources) ?: break
            trace += candidate.identity
            val token = policy.begin(candidate.identity, SmartEngine.EXO)!!
            when (candidate.identity) {
                "http404" -> assertEquals(FallbackDecision.NEXT_SOURCE, policy.failure(token, PlaybackHealth.HTTP_FATAL))
                "neverFirstFrame" -> {
                    val watchdog = FirstFrameWatchdog(4_000L).apply { start(0L) }
                    assertTrue(watchdog.tick(4_000L, false))
                    assertEquals(FallbackDecision.NEXT_SOURCE, policy.failure(token, PlaybackHealth.NETWORK_TIMEOUT))
                }
                "good" -> { assertEquals(PlaybackHealth.FIRST_FRAME_FAST, PlaybackHealthClassifier.firstFrame(1_200L)); break }
            }
        }
        assertEquals(listOf("http404", "neverFirstFrame", "good"), trace)
        assertEquals(1, userClicks)
    }
    @Test fun bufferingBreaksContinuousHealthyProgressBeforePlaybackOk() {
        val watchdog = PlaybackProgressWatchdog()
        watchdog.observe(0L, 0L, false, true, true, false)
        assertEquals(9_000L, watchdog.observe(9_000L, 9_000L, false, true, true, false).stableProgressMs)
        assertEquals(0L, watchdog.observe(10_000L, 9_000L, false, true, false, false).stableProgressMs)
        assertEquals(1_000L, watchdog.observe(11_000L, 10_000L, false, true, true, false).stableProgressMs)
    }

}
