package com.nuvio.tv.ui.screens.player.smartsource

internal enum class PlaybackHealth {
    FIRST_FRAME_FAST, FIRST_FRAME_SLOW, HTTP_FATAL, NETWORK_TIMEOUT, CODEC_ERROR, STALL, PLAYBACK_OK, USER_SKIP
}

internal object PlaybackHealthClassifier {
    fun mpvFirstFrameEvidence(surfaceValid: Boolean, voConfigured: Boolean, outputWidth: Long): Boolean =
        surfaceValid && voConfigured && outputWidth > 0L

    fun firstFrame(firstFrameMs: Long): PlaybackHealth =
        if (firstFrameMs <= 5_000L) PlaybackHealth.FIRST_FRAME_FAST else PlaybackHealth.FIRST_FRAME_SLOW

    fun failure(httpStatus: Int? = null, codec: Boolean = false, network: Boolean = false,
                stallMs: Long = 0L): PlaybackHealth? = when {
        httpStatus in setOf(401, 403, 404, 410) -> PlaybackHealth.HTTP_FATAL
        codec -> PlaybackHealth.CODEC_ERROR
        stallMs >= 10_000L -> PlaybackHealth.STALL
        network || httpStatus != null -> PlaybackHealth.NETWORK_TIMEOUT
        else -> null
    }
}
