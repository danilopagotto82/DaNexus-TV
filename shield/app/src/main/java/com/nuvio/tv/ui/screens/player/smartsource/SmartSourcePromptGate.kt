package com.nuvio.tv.ui.screens.player.smartsource

/** One invitation per attempt; the playback deadline and attempt budget remain unchanged. */
internal class SmartSourcePromptGate(private val offerAfterMs: Long = 5_000L) {
    private var offered = false
    fun offerIfDue(waitingMs: Long, eligible: Boolean): Boolean {
        if (offered || !eligible || waitingMs < offerAfterMs) return false
        offered = true
        return true
    }
}
