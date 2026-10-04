package com.nuvio.tv.ui.screens.player

import android.os.SystemClock
import androidx.media3.common.PlaybackException
import com.nuvio.tv.data.local.InternalPlayerEngine
import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamBehaviorHints
import com.nuvio.tv.domain.model.ProxyHeaders
import com.nuvio.tv.ui.screens.player.smartsource.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal class SmartSourceSession(
    val config: SmartSourceConfig,
    selected: SmartSource<Stream>,
    val contentKey: String,
) {
    val queue = SmartSourceQueue(selected, emptyList(), manifestOrder = true, learning = config.learning)
    val policy = SourceFallbackPolicy(maxSources = config.sourceLimit.takeIf { it > 0 } ?: Int.MAX_VALUE,
        maxAttempts = config.sourceLimit.takeIf { it > 0 }?.times(2) ?: Int.MAX_VALUE)
    val timeoutMs = (config.waitSeconds + if (config.showPrompt) config.promptSeconds else 0) * 1_000L
    var queueJob: Job? = null
    var queueFingerprint = ""
    var current = selected
    var token: Long? = null
    var engine: SmartEngine? = null
    var watchdog = FirstFrameWatchdog(timeoutMs, config.kids, maxTimeoutMs = timeoutMs)
    var firstFrameMs: Long? = null
    var promptGate = SmartSourcePromptGate(config.waitSeconds * 1_000L)
    var monitorJob: Job? = null
    var fallbackJob: Job? = null
    var remoteJob: Job? = null
    var lastRemoteHeartbeatMs = 0L
    var lastRemoteCommandId = 0L
    val remoteSessionId: String = java.util.UUID.randomUUID().toString()
    var progressWatchdog = PlaybackProgressWatchdog()
    var okReported = false
    var transitioning = false
    var ended = false
}

private fun PlayerRuntimeController.smartContentKey(): String =
    "$contentType|$contentId|$currentVideoId|$currentSeason|$currentEpisode|$profileId"

private fun PlayerRuntimeController.navigationSmartStream(): Stream = Stream(
    name = streamName, title = title, description = currentStreamDescription,
    url = currentStreamUrl, ytId = null, infoHash = navigationArgs.infoHash,
    fileIdx = navigationArgs.fileIdx, externalUrl = null,
    behaviorHints = StreamBehaviorHints(null, navigationArgs.bingeGroup, null,
        ProxyHeaders(currentHeaders, emptyMap()), filename = currentFilename),
    addonName = currentAddonName.orEmpty(), addonLogo = currentAddonLogo,
    subtitles = streamSubtitles,
)

internal fun smartSourceCandidate(stream: Stream): SmartSource<Stream> {
    val headers = PlayerMediaSourceFactory.sanitizeHeaders(stream.behaviorHints?.proxyHeaders?.request).entries
        .sortedBy { it.key.lowercase() }.joinToString("\u0000") { "${it.key.lowercase()}:${it.value}" }
    val request = stream.getStreamUrl() ?: "torrent:${stream.getEffectiveInfoHash()}:${stream.getEffectiveFileIdx()}"
    // Credentials affect request identity, but only the digest ever leaves memory.
    val identity = smartOpaqueKey("$request\u0000$headers\u0000${stream.ytId.orEmpty()}")
    val hints = stream.smartSource
    val safeAddon = hints?.addonId?.takeIf { it.matches(Regex("[a-zA-Z0-9._:-]{1,100}")) }
        ?: "addon-${smartOpaqueKey(stream.addonName).take(24)}"
    val mirror = hints?.mirrorKey?.takeIf { it.matches(Regex("[a-zA-Z0-9._:-]{1,100}")) }
        ?: "mirror-${smartOpaqueKey(request.substringBefore('?').substringBefore('#')).take(24)}"
    return SmartSource(identity, safeAddon, mirror, stream, hints?.timeoutMs,
        seeds = hints?.seeds?.takeIf { it >= 0 },
        originKey = hints?.addonId?.takeIf { it.matches(Regex("[a-zA-Z0-9._:-]{1,100}")) } ?: stream.addonName)
}

internal fun PlayerRuntimeController.startSmartSourceSession(stream: Stream = navigationSmartStream()) {
    stopSmartSourceSession()
    val config = SmartSourceConfig.load(context, stream.addonName, profileId)
    if (!config.enabled || stream.isExternal() || stream.isYouTube()) return
    val selected = smartSourceCandidate(stream)
    val session = SmartSourceSession(config, selected, smartContentKey())
    smartSourceSession = session
    session.watchdog.start(SystemClock.elapsedRealtime())
    _uiState.update { it.copy(smartSourceMessage = context.getString(com.nuvio.tv.R.string.danexus_searching_source)) }
    loadSourceStreams(forceRefresh = false)
    armSmartSourceMonitor(session)
}

internal fun PlayerRuntimeController.stopSmartSourceSession() {
    smartSourceSession?.let { session ->
        session.ended = true
        session.policy.cancel()
        session.queueJob?.cancel()
    session.monitorJob?.cancel()
        session.fallbackJob?.cancel()
        session.remoteJob?.cancel()
    }
    smartSourceSession = null
    smartSourceInitializationJob?.cancel()
    smartSourceInitializationJob = null
    _uiState.update { it.copy(smartSourceMessage = null, smartSourcePromptVisible = false, smartSourceSwitchSeconds = 0) }
}

internal fun PlayerRuntimeController.refreshSmartSourceContentIfNeeded() {
    if (smartSourceSession?.let { it.contentKey != smartContentKey() } == true) startSmartSourceSession()
}

/** Bind the actual engine selected by existing Nuvio settings, never guess it. */
internal fun PlayerRuntimeController.bindSmartSourceEngine(engine: InternalPlayerEngine) {
    val session = smartSourceSession ?: return
    if (session.ended || session.token != null) return
    val resolved = if (engine == InternalPlayerEngine.MVP_PLAYER) SmartEngine.MPV else SmartEngine.EXO
    session.engine = resolved
    session.token = session.policy.begin(session.current.identity, resolved)
    if (session.token == null) exhaustSmartSources(session) else updateSmartSourcePresentation(session)
}

private fun PlayerRuntimeController.armSmartSourceMonitor(session: SmartSourceSession) {
    session.queueJob?.cancel()
    session.monitorJob?.cancel()
    session.monitorJob = scope.launch {
        while (isActive && smartSourceSession === session && !session.ended) {
            delay(250L)
            if (session.transitioning) continue
            val fresh = smartAvailableSources(session)
            val fingerprint = fresh.joinToString("|") { it.identity }
            if (fingerprint != session.queueFingerprint && session.queueJob?.isActive != true) {
                session.queueFingerprint = fingerprint
                session.queueJob = scope.launch {
                    val ranked = if (session.config.learning) SmartSourceBridge(context, session.config)
                        .rank(fresh, session.current, contentType, currentVideoId ?: contentId) else fresh
                    if (smartSourceSession !== session || session.ended) return@launch
                    session.queue.update(ranked)
                    updateSmartSourcePresentation(session)
                }
            }
            val now = SystemClock.elapsedRealtime()
            val state = _uiState.value
            val suspended = isInBackground || userPausedManually || state.showSeekOverlay ||
                state.pendingPreviewSeekPosition != null || state.showSourcesPanel || state.showEpisodesPanel ||
                state.showAudioOverlay || state.showSubtitleOverlay || state.showMoreDialog || state.showSpeedDialog
            if (session.firstFrameMs == null && isUsingMpvEngine()) onSmartSourceMpvVideoStarted()
            if (session.firstFrameMs == null) {
                if (session.watchdog.tick(now, suspended)) {
                    handleSmartSourceFailure(PlaybackHealth.NETWORK_TIMEOUT)
                    continue
                }
                val eligible = !suspended && session.token?.let(session.policy::isCurrent) == true
                val offer = session.config.showPrompt && session.promptGate.offerIfDue(session.watchdog.waitingMs, eligible)
                if (offer || (eligible && _uiState.value.smartSourcePromptVisible)) {
                    val seconds = ((session.watchdog.remainingMs + 999L) / 1_000L).toInt()
                    _uiState.update { it.copy(smartSourcePromptVisible = true,
                        smartSourceSwitchSeconds = seconds,
                        smartSourceTimeoutSeconds = session.config.promptSeconds) }
                }
            } else {
                val position = currentPlaybackPositionMs() ?: playbackTimeline.value.currentPosition
                val observation = session.progressWatchdog.observe(now, position, suspended,
                    expectingPlayback = true, healthyProgress = !state.isBuffering && state.isPlaying,
                    ended = state.playbackEnded)
                if (observation.stalledForMs >= session.config.stallSeconds * 1_000L) {
                    handleSmartSourceFailure(PlaybackHealth.STALL, stallMs = observation.stalledForMs)
                    continue
                }
                if (observation.stableProgressMs >= 10_000L && !session.okReported) {
                    session.okReported = true
                    reportSmartSource(session, PlaybackHealth.PLAYBACK_OK)
                }
            }
            if (!suspended && state.error != null) handleSmartSourceFailure(PlaybackHealth.NETWORK_TIMEOUT)
            if (session.config.backendBaseUrls.isNotEmpty() && now - session.lastRemoteHeartbeatMs >= 1_500L &&
                session.remoteJob?.isActive != true) {
                session.lastRemoteHeartbeatMs = now
                val remoteState = when {
                    session.transitioning -> "switching"
                    session.firstFrameMs == null -> "starting"
                    userPausedManually -> "paused"
                    state.isBuffering -> "buffering"
                    state.isPlaying -> "playing"
                    else -> "idle"
                }
                session.remoteJob = scope.launch {
                    val command = SmartSourceBridge(context, session.config).heartbeat(
                        sessionId = session.remoteSessionId,
                        afterCommandId = session.lastRemoteCommandId,
                        title = contentName ?: title,
                        state = remoteState,
                        attempt = session.policy.attemptedSources.size.coerceAtLeast(1),
                        maxSources = session.queue.snapshot().size,
                        source = session.current,
                    ) ?: return@launch
                    if (smartSourceSession !== session || session.ended || command.id <= session.lastRemoteCommandId) return@launch
                    session.lastRemoteCommandId = command.id
                    when (command.action) {
                        "next_source" -> handleSmartSourceFailure(PlaybackHealth.USER_SKIP)
                        "toggle_play_pause" -> onEvent(PlayerEvent.OnPlayPause)
                    }
                }
            }
        }
    }
}

internal fun PlayerRuntimeController.onSmartSourceFirstFrame(evidence: String) {
    val session = smartSourceSession ?: return
    if (session.ended || session.transitioning || session.firstFrameMs != null ||
        session.token?.let { !session.policy.isCurrent(it) } != false) return
    val ms = session.watchdog.firstFrame(SystemClock.elapsedRealtime()) ?: return
    session.firstFrameMs = ms
    _uiState.update { it.copy(smartSourceMessage = null, smartSourcePromptVisible = false, smartSourceSwitchSeconds = 0) }
    reportSmartSource(session, PlaybackHealthClassifier.firstFrame(ms), evidence = evidence)
}

/** mpv's output-start event plus a configured video output; audio progress is never proof. */
internal fun PlayerRuntimeController.onSmartSourceMpvVideoStarted() {
    if (smartSourceSession == null || !isUsingMpvEngine() || isReleasingPlayer) return
    val view = mpvView ?: return
    val videoOutput = runCatching { view.mpv.getPropertyBoolean("vo-configured") }.getOrNull() == true
    val width = runCatching { view.mpv.getPropertyInt("video-out-params/w") }.getOrNull() ?: 0L
    if (PlaybackHealthClassifier.mpvFirstFrameEvidence(view.holder.surface?.isValid == true, videoOutput, width.toLong())) {
        onSmartSourceFirstFrame("MPV_VIDEO_OUTPUT_READY")
    }
}

internal fun PlayerRuntimeController.handleSmartSourceExoError(error: PlaybackException): Boolean {
    val http = error.findInvalidResponseCodeException()?.responseCode
    val codec = error.errorCode in 4001..4005 || error.errorCode in 5001..5004
    val network = error.errorCode in 2000..2008 || error.errorCode == PlaybackException.ERROR_CODE_TIMEOUT
    val health = PlaybackHealthClassifier.failure(httpStatus = http, codec = codec, network = network) ?: return false
    return handleSmartSourceFailure(health, httpStatus = http)
}

internal fun PlayerRuntimeController.handleSmartSourceFailure(health: PlaybackHealth,
    httpStatus: Int? = null, stallMs: Long? = null): Boolean {
    val session = smartSourceSession ?: return false
    if (session.ended) return true
    if (isInBackground || isReleasingPlayer || (userPausedManually && health != PlaybackHealth.USER_SKIP)) return false
    if (session.transitioning) return true
    if (session.token == null) bindSmartSourceEngine(currentInternalPlayerEngine)
    val token = session.token ?: return true
    val decision = session.policy.failure(token, health)
    if (decision == FallbackDecision.IGNORE) return true
    reportSmartSource(session, health, httpStatus = httpStatus, stallMs = stallMs)
    session.transitioning = true
    cancelFirstFrameWatchdog()
    cancelStallWatchdog()
    errorRetryJob?.cancel()
    _uiState.update { it.copy(error = null, showLoadingOverlay = true, showPlayerEngineSwitchInfo = false,
        smartSourcePromptVisible = false, smartSourceSwitchSeconds = 0,
        smartSourceMessage = context.getString(com.nuvio.tv.R.string.danexus_trying_source,
            (session.policy.attemptedSources.size + 1).coerceAtMost(session.queue.snapshot().size), session.queue.snapshot().size)) }
    if (decision == FallbackDecision.EXHAUSTED) {
        exhaustSmartSources(session)
    } else {
        session.fallbackJob = scope.launch {
            delay(1L)
            if (smartSourceSession !== session || session.ended) return@launch
            // Schedule engine/source mutation outside the originating player's callback.
            if (decision == FallbackDecision.RETRY_OTHER_ENGINE) {
                val target = if (session.engine == SmartEngine.MPV) SmartEngine.EXO else SmartEngine.MPV
                rememberCurrentTrackPreferenceForEngineSwitch()
                val position = currentPlaybackPositionMs()?.coerceAtLeast(0L) ?: 0L
                releasePlayer(flushPlaybackState = false)
                resetSmartSourceAttempt(session)
                session.engine = target
                session.token = session.policy.begin(session.current.identity, target)
                if (session.token == null) { exhaustSmartSources(session); return@launch }
                if (position > 0L) _uiState.update { it.copy(pendingSeekPosition = position) }
                val targetEngine = if (target == SmartEngine.MPV) InternalPlayerEngine.MVP_PLAYER else InternalPlayerEngine.EXOPLAYER
                pendingMpvHardRestartOnNextAttach = target == SmartEngine.MPV
                delayMpvResumeSeekUntilVideoTrack = target == SmartEngine.MPV
                initializePlayer(currentStreamUrl, currentHeaders, overrideInternalPlayerEngine = targetEngine,
                    allowEngineFailover = false)
            } else {
                val position = currentPlaybackPositionMs()?.coerceAtLeast(0L) ?: 0L
                releasePlayer(flushPlaybackState = false)
                loadSourceStreams(forceRefresh = false)
                withTimeoutOrNull(60_000L) {
                    uiState.first { state -> state.sourceAllStreams.any { candidate ->
                        (session.config.allAddons || session.config.allowedAddonNames.any { it.equals(candidate.addonName, ignoreCase = true) }) &&
                            !candidate.isExternal() && !candidate.isYouTube() &&
                            smartSourceCandidate(candidate).identity !in session.policy.attemptedSources
                    } || sourceStreamsFetchCompleted }
                }
                val rows = smartAvailableSources(session)
                val ranked = if (session.config.learning) SmartSourceBridge(context, session.config)
                    .rank(rows, session.current, contentType, currentVideoId ?: contentId) else rows
                if (smartSourceSession !== session || session.ended) return@launch
                session.queue.update(ranked)
                val next = session.queue.next(session.policy.attemptedSources)
                if (next == null) { exhaustSmartSources(session); return@launch }
                session.current = next
                updateSmartSourcePresentation(session)
                resetSmartSourceAttempt(session)
                if (position > 0L) _uiState.update { it.copy(pendingSeekPosition = position) }
                switchToSourceStream(next.payload, continueSmartSession = true)
            }
        }
    }
    return true
}

private fun resetSmartSourceAttempt(session: SmartSourceSession) {
    session.token = null
    session.engine = null
    session.firstFrameMs = null
    session.promptGate = SmartSourcePromptGate(session.config.waitSeconds * 1_000L)
    session.watchdog = FirstFrameWatchdog(session.timeoutMs, session.config.kids, maxTimeoutMs = session.timeoutMs)
    session.watchdog.start(SystemClock.elapsedRealtime())
    session.progressWatchdog = PlaybackProgressWatchdog()
    session.okReported = false
    session.transitioning = false
}

private fun PlayerRuntimeController.exhaustSmartSources(session: SmartSourceSession) {
    session.ended = true
    session.policy.cancel()
    session.queueJob?.cancel()
    session.monitorJob?.cancel()
    releasePlayer(flushPlaybackState = false)
    _uiState.update { it.copy(error = context.getString(com.nuvio.tv.R.string.danexus_no_source),
        showLoadingOverlay = false, isBuffering = false, isPlaying = false, smartSourceMessage = null, smartSourcePromptVisible = false, smartSourceSwitchSeconds = 0,
        showSwitchToMpvErrorAction = false, showPlayerEngineSwitchInfo = false) }
}

private fun PlayerRuntimeController.reportSmartSource(session: SmartSourceSession, health: PlaybackHealth,
    evidence: String? = null, httpStatus: Int? = null, stallMs: Long? = null) {
    val source = session.current
    val firstFrameMs = session.firstFrameMs
    val media = contentType
    val id = currentVideoId ?: contentId
    val displayTitle = contentName ?: title
    if (!session.config.learning) return
    scope.launch { runCatching { SmartSourceBridge(context, session.config).feedback(source, health, id,
        displayTitle, media, firstFrameMs, stallMs, httpStatus, evidence) } }
}

internal fun PlayerRuntimeController.dismissSlowSmartSourcePrompt() {
    val session = smartSourceSession
    // Waiting is an explicit choice: restart the full wait, without resetting attempted sources.
    if (session != null && session.firstFrameMs == null && !session.transitioning && !session.ended) {
        session.watchdog.start(SystemClock.elapsedRealtime())
        session.promptGate = SmartSourcePromptGate(session.config.waitSeconds * 1_000L)
    }
    _uiState.update { it.copy(smartSourcePromptVisible = false) }
}

internal fun PlayerRuntimeController.skipSlowSmartSource() {
    val session = smartSourceSession ?: return
    if (session.ended || session.transitioning) return
    dismissSlowSmartSourcePrompt()
    handleSmartSourceFailure(PlaybackHealth.USER_SKIP)
}

private fun PlayerRuntimeController.smartAvailableSources(session: SmartSourceSession): List<SmartSource<Stream>> =
    _uiState.value.sourceAllStreams.filter { stream ->
        (session.config.allAddons || session.config.allowedAddonNames.any { it.equals(stream.addonName, ignoreCase = true) }) &&
            !stream.isExternal() && !stream.isYouTube() &&
            (stream.getStreamUrl() != null || stream.isTorrent() || stream.isDirectDebrid())
    }.map(::smartSourceCandidate).distinctBy { it.identity }

private fun PlayerRuntimeController.updateSmartSourcePresentation(session: SmartSourceSession) {
    val next = session.queue.next(session.policy.attemptedSources + session.current.identity)?.payload
    val count = session.queue.snapshot().size.let { if (session.config.sourceLimit > 0) minOf(it, session.config.sourceLimit) else it }
    _uiState.update { it.copy(
        smartSourceCurrentName = smartSafeLabel(session.current.payload.name).ifBlank { smartSafeLabel(session.current.payload.addonName) },
        smartSourceNextName = next?.let { smartSafeLabel(it.name).ifBlank { smartSafeLabel(it.addonName) } }.orEmpty(),
        smartSourceNextAddon = next?.let { smartSafeLabel(it.addonName) }.orEmpty(),
        smartSourceNextLogo = next?.addonLogo,
        smartSourceAttempt = session.policy.attemptedSources.size.coerceAtLeast(1),
        smartSourceTotal = count,
        smartSourceMessage = if (session.firstFrameMs == null && !session.transitioning)
            context.getString(com.nuvio.tv.R.string.danexus_trying_source, session.policy.attemptedSources.size.coerceAtLeast(1), count)
            else it.smartSourceMessage,
    ) }
}
