package com.nuvio.tv.ui.screens.player.smartsource

import org.junit.Assert.*
import org.junit.Test

class DanexusSourceOptionsTest {
    private fun source(id: String, score: Double = 70.0) = SmartSource(id, "addon", "mirror-$id", id, score = score)

    @Test fun allElevenManifestSourcesRemainAvailableAfterFifthFailure() {
        val rows = (1..11).map { source("$it") }
        val queue = SmartSourceQueue(rows.first(), rows, manifestOrder = true, learning = false)
        val policy = SourceFallbackPolicy(Int.MAX_VALUE, Int.MAX_VALUE)
        for (i in 1..11) {
            val next = queue.next(policy.attemptedSources)!!
            assertEquals("$i", next.identity)
            val token = policy.begin(next.identity, SmartEngine.EXO)!!
            assertEquals(FallbackDecision.NEXT_SOURCE, policy.failure(token, PlaybackHealth.HTTP_FATAL))
        }
        assertEquals(11, policy.attemptedSources.size)
        assertNull(queue.next(policy.attemptedSources))
    }

    @Test fun unlearnedSourcesKeepManifestOrderRegardlessOfOrigin() {
        val a = source("chosen")
        val b = source("second", 99.0).copy(originKey = "other")
        val c = source("third", 100.0)
        assertEquals(listOf("chosen", "second", "third"), SmartSourceQueue(a, listOf(b,c), true, false).snapshot().map { it.identity })
    }

    @Test fun learningOnlyChangesAlternativesAndNeverRepeatsARequest() {
        val rows = listOf(source("clicked",10.0),source("slow",30.0),source("fast",90.0),source("fast",90.0))
        val queue = SmartSourceQueue(rows.first(), rows, true, true)
        assertEquals(listOf("clicked", "fast", "slow"), queue.snapshot().map { it.identity })
        assertEquals("slow", queue.next(setOf("clicked", "fast"))?.identity)
    }

    @Test fun configuredWaitIsFollowedBySixFullDecisionSeconds() {
        val watchdog = FirstFrameWatchdog(18_000L, maxTimeoutMs = 18_000L)
        val gate = SmartSourcePromptGate(12_000L)
        watchdog.start(0L)
        assertFalse(watchdog.tick(11_999L,false))
        assertFalse(gate.offerIfDue(watchdog.waitingMs,true))
        assertFalse(watchdog.tick(12_000L,false))
        assertTrue(gate.offerIfDue(watchdog.waitingMs,true))
        assertEquals(6_000L,watchdog.remainingMs)
        assertFalse(watchdog.tick(17_999L,false))
        assertTrue(watchdog.tick(18_000L,false))
    }

    @Test fun codecRetryDoesNotConsumeAnotherSourceSlot() {
        val policy = SourceFallbackPolicy(11,22)
        val token = policy.begin("one",SmartEngine.EXO)!!
        assertEquals(FallbackDecision.RETRY_OTHER_ENGINE,policy.failure(token,PlaybackHealth.CODEC_ERROR))
        assertNotNull(policy.begin("one",SmartEngine.MPV))
        assertEquals(1,policy.attemptedSources.size)
        assertEquals(2,policy.attemptCount)
    }
}
