package com.nuvio.tv.ui.screens.player.smartsource

import kotlin.math.pow
import kotlin.math.roundToInt

internal data class SourceReputationEvent(
    val addonId: String,
    val mirrorKey: String,
    val health: PlaybackHealth,
    val atEpochMs: Long,
    val category: String = "",
    val mediaType: String = "",
    val firstFrameMs: Long? = null,
    val seeds: Int? = null,
)

internal data class LocalSourceReputation(
    val score: Double,
    val samples: Double,
    val medianFirstFrameMs: Long?,
    val circuitOpen: Boolean,
)

/**
 * Offline-first learning for a household. A single failure never bans a source:
 * the Bayesian prior keeps new/temporarily failing mirrors eligible, while two
 * recent consecutive failures temporarily push that mirror behind healthier ones.
 */
internal object LocalSourceReputationPolicy {
    private const val DAY_MS = 86_400_000.0
    private const val CIRCUIT_WINDOW_MS = 6 * 60 * 60 * 1_000L

    fun evaluate(
        events: List<SourceReputationEvent>,
        source: SmartSource<*>,
        category: String = "",
        mediaType: String = "",
        nowMs: Long = System.currentTimeMillis(),
    ): LocalSourceReputation {
        val matching = events.filter {
            it.addonId == source.addonId &&
                (source.mirrorKey.isBlank() || it.mirrorKey.isBlank() || it.mirrorKey == source.mirrorKey)
        }
        var good = 2.8
        var total = 4.0
        val firstFrames = mutableListOf<Long>()
        for (event in matching) {
            if (event.health == PlaybackHealth.PLAYBACK_OK || event.health == PlaybackHealth.USER_SKIP) continue
            val age = (nowMs - event.atEpochMs).coerceAtLeast(0L)
            var weight = 0.5.pow(age / (30.0 * DAY_MS))
            if (category.isNotBlank() && event.category == category) weight *= 1.35
            if (mediaType.isNotBlank() && event.mediaType == mediaType) weight *= 1.20
            var outcome = when (event.health) {
                PlaybackHealth.FIRST_FRAME_FAST -> 1.0
                PlaybackHealth.FIRST_FRAME_SLOW -> 0.68
                PlaybackHealth.CODEC_ERROR -> 0.34
                PlaybackHealth.STALL -> 0.22
                PlaybackHealth.HTTP_FATAL, PlaybackHealth.NETWORK_TIMEOUT -> 0.0
                PlaybackHealth.PLAYBACK_OK, PlaybackHealth.USER_SKIP -> 0.0
            }
            event.firstFrameMs?.takeIf { it > 0L }?.let {
                firstFrames += it
                outcome *= when {
                    it <= 2_500L -> 1.0
                    it <= 5_000L -> 0.90
                    it <= 8_000L -> 0.72
                    it <= 12_000L -> 0.48
                    else -> 0.20
                }
            }
            if (event.seeds == 0) outcome = minOf(outcome, 0.05)
            good += outcome * weight
            total += weight
        }
        firstFrames.sort()
        val median = firstFrames.takeIf { it.isNotEmpty() }?.get((firstFrames.size - 1) / 2)
        var score = ((good / total) * 100.0).coerceIn(0.0, 100.0)
        if (source.seeds == 0) score = minOf(score, 12.0)

        val recent = matching.asSequence()
            .filter { nowMs - it.atEpochMs in 0..CIRCUIT_WINDOW_MS }
            .sortedByDescending { it.atEpochMs }
            .filter { it.health != PlaybackHealth.PLAYBACK_OK && it.health != PlaybackHealth.USER_SKIP }
            .take(3)
            .toList()
        val circuitOpen = recent.take(2).size == 2 && recent.take(2).all {
            it.health in setOf(PlaybackHealth.HTTP_FATAL, PlaybackHealth.NETWORK_TIMEOUT, PlaybackHealth.STALL)
        }
        if (circuitOpen) score = minOf(score, 18.0)
        return LocalSourceReputation(
            score = score.roundToInt().toDouble(),
            samples = ((total - 4.0) * 100.0).roundToInt() / 100.0,
            medianFirstFrameMs = median,
            circuitOpen = circuitOpen,
        )
    }

    fun <T> apply(
        sources: List<SmartSource<T>>,
        events: List<SourceReputationEvent>,
        category: String,
        mediaType: String?,
        nowMs: Long = System.currentTimeMillis(),
    ): List<SmartSource<T>> = sources.map { source ->
        val local = evaluate(events, source, category, mediaType.orEmpty(), nowMs)
        if (local.samples <= 0.0) source else source.copy(score = source.score * 0.35 + local.score * 0.65)
    }
}
