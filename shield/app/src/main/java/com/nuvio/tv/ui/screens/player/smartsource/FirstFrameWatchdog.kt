package com.nuvio.tv.ui.screens.player.smartsource

/** All deadlines use monotonic time. Pauses and background time do not count. */
internal class FirstFrameWatchdog(requestedMs: Long?, kids: Boolean = false, maxTimeoutMs: Long = if (kids) 8_000L else 12_000L) {
    val timeoutMs = (requestedMs ?: 7_000L).coerceIn(4_000L, maxTimeoutMs.coerceIn(4_000L, 75_000L))
    private var elapsedMs = 0L
    val waitingMs: Long get() = elapsedMs
    val remainingMs: Long get() = (timeoutMs - elapsedMs).coerceAtLeast(0L)
    private var previousMs: Long? = null
    private var previousSuspended = false
    private var completed = false

    fun start(nowMs: Long) {
        elapsedMs = 0L
        previousMs = nowMs
        previousSuspended = false
        completed = false
    }

    fun tick(nowMs: Long, suspended: Boolean): Boolean {
        val previous = previousMs ?: nowMs
        if (!suspended && !previousSuspended && !completed) elapsedMs += (nowMs - previous).coerceAtLeast(0L)
        previousMs = nowMs
        previousSuspended = suspended
        return !completed && elapsedMs >= timeoutMs
    }

    fun firstFrame(nowMs: Long): Long? {
        if (completed) return null
        tick(nowMs, suspended = false)
        completed = true
        return elapsedMs
    }
}

internal class PlaybackProgressWatchdog {
    data class Observation(val stalledForMs: Long, val stableProgressMs: Long)
    private var previousMs: Long? = null
    private var previousPositionMs: Long? = null
    private var previousSuspended = false
    private var lastAdvanceMs = 0L
    private var stableMs = 0L

    fun observe(nowMs: Long, positionMs: Long, suspended: Boolean, expectingPlayback: Boolean,
                healthyProgress: Boolean, ended: Boolean): Observation {
        val previous = previousMs
        val oldPosition = previousPositionMs
        val delta = if (previous == null) 0L else (nowMs - previous).coerceAtLeast(0L)
        if (previous == null || suspended || previousSuspended || ended || positionMs != oldPosition) {
            lastAdvanceMs = nowMs
            if (oldPosition != null && positionMs > oldPosition && !suspended && !previousSuspended &&
                healthyProgress && !ended) stableMs += delta else stableMs = 0L
        }
        if (!healthyProgress || (oldPosition != null && positionMs <= oldPosition)) stableMs = 0L
        previousMs = nowMs
        previousPositionMs = positionMs
        previousSuspended = suspended
        val stalled = if (suspended || ended || !expectingPlayback) 0L else (nowMs - lastAdvanceMs).coerceAtLeast(0L)
        return Observation(stalled, stableMs)
    }
}
