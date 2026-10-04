package com.nuvio.tv.ui.screens.player.smartsource

internal enum class SmartEngine { EXO, MPV }
internal enum class FallbackDecision { RETRY_OTHER_ENGINE, NEXT_SOURCE, EXHAUSTED, IGNORE }

/** A session owns one click. Only an explicit new click/content may reset its budgets. */
internal class SourceFallbackPolicy(val maxSources: Int = 5, val maxAttempts: Int = 8) {
    private val sources = linkedSetOf<String>()
    private val engines = mutableMapOf<String, MutableSet<SmartEngine>>()
    private var attempts = 0
    private var generation = 0L
    private var activeIdentity: String? = null
    private var activeEngine: SmartEngine? = null
    private var handled = false
    private var cancelled = false

    val attemptedSources: Set<String> get() = sources.toSet()
    val attemptCount: Int get() = attempts
    val token: Long get() = generation

    fun begin(identity: String, engine: SmartEngine): Long? {
        if (cancelled || attempts >= maxAttempts || (identity !in sources && sources.size >= maxSources)) return null
        val used = engines.getOrPut(identity) { mutableSetOf() }
        if (!used.add(engine)) return null
        sources.add(identity)
        attempts++
        generation++
        activeIdentity = identity
        activeEngine = engine
        handled = false
        return generation
    }

    fun failure(token: Long, health: PlaybackHealth): FallbackDecision {
        if (cancelled || token != generation || handled) return FallbackDecision.IGNORE
        handled = true
        val identity = activeIdentity ?: return FallbackDecision.IGNORE
        if (health == PlaybackHealth.CODEC_ERROR && attempts < maxAttempts && engines[identity]?.size == 1) {
            return FallbackDecision.RETRY_OTHER_ENGINE
        }
        return if (sources.size >= maxSources || attempts >= maxAttempts) FallbackDecision.EXHAUSTED
        else FallbackDecision.NEXT_SOURCE
    }

    fun isCurrent(token: Long): Boolean = !cancelled && !handled && token == generation
    fun cancel() { cancelled = true; handled = true; generation++ }
}
