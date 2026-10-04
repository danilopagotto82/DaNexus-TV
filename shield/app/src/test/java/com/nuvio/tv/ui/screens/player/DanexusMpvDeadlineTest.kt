package com.nuvio.tv.ui.screens.player

import org.junit.Assert.*
import org.junit.Test

class DanexusMpvDeadlineTest {
    @Test fun upstreamWatchdogDoesNotShortenConfiguredSmartSourceDecisionTime() {
        for (surface in listOf(false, true)) {
            val step = MpvStartupWatchdogPolicy.step(MpvStartupWatchdogPolicy.Input(
                enabled = true, waitingForSurface = surface, idleActive = true,
                cacheProgressing = false, counters = MpvStartupWatchdogPolicy.Counters(100,100,100,100),
                smartSourceOwnsDeadline = true,
            ))
            assertEquals(MpvStartupWatchdogPolicy.Action.Continue, step.action)
            assertEquals(MpvStartupWatchdogPolicy.Counters(), step.counters)
        }
    }

    @Test fun nativeWatchdogStillWorksWhenSmartSourceIsDisabled() {
        val step = MpvStartupWatchdogPolicy.step(MpvStartupWatchdogPolicy.Input(
            enabled = true, waitingForSurface = true, idleActive = false,
            cacheProgressing = false,
            counters = MpvStartupWatchdogPolicy.Counters(surfaceWaitTicks = MpvStartupWatchdogPolicy.surfaceWaitTicks - 1),
        ))
        assertEquals(MpvStartupWatchdogPolicy.Action.SurfaceTimeout, step.action)
    }
}
