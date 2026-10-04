from pathlib import Path
root = Path(__file__).resolve().parents[3]
edits = {}
def edit(path, old, new, count=1):
 text = edits.get(path, path.read_text(encoding='utf-8'))
 assert text.count(old) == count, (str(path), old[:90], text.count(old))
 edits[path] = text.replace(old, new)
gate = '''package com.nuvio.tv.ui.screens.player.smartsource

/** One invitation per attempt; the playback deadline and attempt budget remain unchanged. */
internal class SmartSourcePromptGate {
    private var offered = false
    fun offerIfDue(waitingMs: Long, eligible: Boolean): Boolean {
        if (offered || !eligible || waitingMs < 5_000L) return false
        offered = true
        return true
    }
}
'''
component = '''package com.nuvio.tv.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SmartSourceSlowStartCard(
    secondsRemaining: Int,
    timeoutSeconds: Int,
    onNextSource: () -> Unit,
    onWait: () -> Unit,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextFocus = remember { FocusRequester() }
    val latestClosed by rememberUpdatedState(onClosed)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        withFrameNanos { }
        runCatching { nextFocus.requestFocus() }
    }
    DisposableEffect(Unit) { onDispose { latestClosed() } }
    val progress = (1f - secondsRemaining.toFloat() / timeoutSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "smartSourceWaitProgress",
    )
    Column(
        modifier = modifier.width(310.dp)
            .background(Color(0xF21B2534), RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFF526B87), RoundedCornerShape(18.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.danexus_slow_source_title), style = MaterialTheme.typography.titleMedium, color = Color.White)
        Text(stringResource(R.string.danexus_slow_source_body), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD0DEEC))
        Text(stringResource(R.string.danexus_slow_source_countdown, secondsRemaining.coerceAtLeast(1)), style = MaterialTheme.typography.labelLarge, color = Color(0xFFB7D6FF))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF42536A))) {
            Box(Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().background(Color(0xFF9BCBFF)))
        }
        Button(onClick = onNextSource, modifier = Modifier.fillMaxWidth().focusRequester(nextFocus)) {
            Text(stringResource(R.string.danexus_slow_source_next))
        }
        Button(onClick = onWait, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.danexus_slow_source_wait))
        }
    }
}
'''
tests = '''package com.nuvio.tv.ui.screens.player.smartsource

import org.junit.Assert.*
import org.junit.Test

class SmartSourcePromptTest {
    @Test fun noPromptBeforeFiveActiveSeconds() {
        val gate = SmartSourcePromptGate()
        assertFalse(gate.offerIfDue(4_999L, true))
        assertTrue(gate.offerIfDue(5_000L, true))
    }
    @Test fun promptIsOfferedOnlyOncePerAttempt() {
        val gate = SmartSourcePromptGate()
        assertTrue(gate.offerIfDue(5_000L, true))
        assertFalse(gate.offerIfDue(7_000L, true))
        assertFalse(gate.offerIfDue(12_000L, true))
    }
    @Test fun pausedOrStalePlaybackCannotOfferPrompt() {
        val gate = SmartSourcePromptGate()
        assertFalse(gate.offerIfDue(6_000L, false))
        assertTrue(gate.offerIfDue(6_000L, true))
    }
    @Test fun newAttemptCanOfferItsOwnPrompt() {
        assertTrue(SmartSourcePromptGate().offerIfDue(5_000L, true))
        assertTrue(SmartSourcePromptGate().offerIfDue(5_000L, true))
    }
    @Test fun pausedWallTimeDoesNotReachPromptThreshold() {
        val clock = FirstFrameWatchdog(12_000L)
        val gate = SmartSourcePromptGate()
        clock.start(0L)
        clock.tick(4_000L, false)
        clock.tick(8_000L, true)
        clock.tick(14_000L, false)
        assertFalse(gate.offerIfDue(clock.waitingMs, true))
        clock.tick(15_000L, false)
        assertTrue(gate.offerIfDue(clock.waitingMs, true))
    }
}
'''
for edition in ('FireTV', 'Shield'):
 project = root / ('DaNexus-' + edition)
 src = project / 'app/src/main/java/com/nuvio/tv/ui/screens/player'
 assert not (src / 'SmartSourceSlowStartCard.kt').exists(), 'Migration was already applied'
 p = src / 'PlayerUiState.kt'
 edit(p, '    val smartSourceMessage: String? = null,', '    val smartSourceMessage: String? = null,\n    val smartSourcePromptVisible: Boolean = false,\n    val smartSourceSwitchSeconds: Int = 0,\n    val smartSourceTimeoutSeconds: Int = 7,')
 edit(p, 'sealed class PlayerEvent {', 'sealed class PlayerEvent {\n    data object OnSmartSourceNext : PlayerEvent()\n    data object OnSmartSourceWait : PlayerEvent()')
 p = src / 'PlayerRuntimeControllerPlaybackEvents.kt'
 edit(p, '    when (event) {\n        PlayerEvent.OnPlayPause -> {', '    when (event) {\n        PlayerEvent.OnSmartSourceNext -> skipSlowSmartSource()\n        PlayerEvent.OnSmartSourceWait -> dismissSlowSmartSourcePrompt()\n        PlayerEvent.OnPlayPause -> {')
 p = src / 'smartsource/FirstFrameWatchdog.kt'
 edit(p, '    private var elapsedMs = 0L', '    private var elapsedMs = 0L\n    val waitingMs: Long get() = elapsedMs\n    val remainingMs: Long get() = (timeoutMs - elapsedMs).coerceAtLeast(0L)')
 p = src / 'PlayerRuntimeControllerSmartSource.kt'
 edit(p, '    var firstFrameMs: Long? = null', '    var firstFrameMs: Long? = null\n    var promptGate = SmartSourcePromptGate()')
 old = '                state.pendingPreviewSeekPosition != null'
 edit(p, old, old + ' || state.showSourcesPanel || state.showEpisodesPanel ||\n                state.showAudioOverlay || state.showSubtitleOverlay || state.showMoreDialog || state.showSpeedDialog')
 anchor = '''                    handleSmartSourceFailure(PlaybackHealth.NETWORK_TIMEOUT)
                    continue
                }
            } else {'''
 replacement = '''                    handleSmartSourceFailure(PlaybackHealth.NETWORK_TIMEOUT)
                    continue
                }
                val eligible = !suspended && session.token?.let(session.policy::isCurrent) == true
                val offer = session.promptGate.offerIfDue(session.watchdog.waitingMs, eligible)
                if (offer || (eligible && _uiState.value.smartSourcePromptVisible)) {
                    val seconds = ((session.watchdog.remainingMs + 999L) / 1_000L).toInt()
                    _uiState.update { it.copy(smartSourcePromptVisible = true,
                        smartSourceSwitchSeconds = seconds,
                        smartSourceTimeoutSeconds = ((session.watchdog.timeoutMs + 999L) / 1_000L).toInt()) }
                }
            } else {'''
 edit(p, anchor, replacement)
 text = edits[p]
 text = text.replace('smartSourceMessage = null', 'smartSourceMessage = null, smartSourcePromptVisible = false, smartSourceSwitchSeconds = 0')
 text = text.replace('smartSourceMessage = context.getString(com.nuvio.tv.R.string.danexus_trying_source,', 'smartSourcePromptVisible = false, smartSourceSwitchSeconds = 0,\n        smartSourceMessage = context.getString(com.nuvio.tv.R.string.danexus_trying_source,')
 text = text.replace('    session.firstFrameMs = null', '    session.firstFrameMs = null\n    session.promptGate = SmartSourcePromptGate()')
 text += '''
internal fun PlayerRuntimeController.dismissSlowSmartSourcePrompt() {
    _uiState.update { it.copy(smartSourcePromptVisible = false) }
}

internal fun PlayerRuntimeController.skipSlowSmartSource() {
    val session = smartSourceSession ?: return
    if (session.ended || session.transitioning || session.firstFrameMs != null ||
        !_uiState.value.smartSourcePromptVisible) return
    dismissSlowSmartSourcePrompt()
    handleSmartSourceFailure(PlaybackHealth.USER_SKIP)
}
'''
 edits[p] = text
 p = src / 'PlayerScreen.kt'
 edit(p, '    val handleBackPress = handleBackPress@{', '''    val handleBackPress = handleBackPress@{
        if (uiState.smartSourcePromptVisible) {
            viewModel.onEvent(PlayerEvent.OnSmartSourceWait)
            return@handleBackPress
        }''')
 anchor = '            .onKeyEvent { keyEvent ->'
 text = edits[p]
 assert anchor in text
 text = text.replace(anchor, anchor + '''
                if (uiState.smartSourcePromptVisible && keyEvent.nativeKeyEvent.keyCode in setOf(
                    KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                    KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                    return@onKeyEvent false
                }''', 1)
 anchor = '        LoadingOverlay('
 assert text.count(anchor) == 1
 text = text.replace(anchor, '''        if (uiState.smartSourcePromptVisible && uiState.error == null && !postPlayRecommendationState.isVisible) {
            SmartSourceSlowStartCard(
                secondsRemaining = uiState.smartSourceSwitchSeconds,
                timeoutSeconds = uiState.smartSourceTimeoutSeconds,
                onNextSource = { viewModel.onEvent(PlayerEvent.OnSmartSourceNext) },
                onWait = { viewModel.onEvent(PlayerEvent.OnSmartSourceWait) },
                onClosed = { runCatching { containerFocusRequester.requestFocus() } },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 30.dp, bottom = 34.dp).zIndex(4f),
            )
        }

''' + anchor)
 edits[p] = text
 edits[src / 'SmartSourceSlowStartCard.kt'] = component
 edits[src / 'smartsource/SmartSourcePromptGate.kt'] = gate
 edits[project / 'app/src/test/java/com/nuvio/tv/ui/screens/player/smartsource/SmartSourcePromptTest.kt'] = tests
 for locale, values in [('values', ['Taking longer to start?', 'You can try another source now. If this one does not start, we will switch automatically.', 'Automatic switch in %1$d s', 'Change source', 'Keep waiting']), ('values-pt-rBR', ['Demorando para iniciar?', 'Você pode tentar outra fonte agora. Se esta não iniciar, mudaremos automaticamente.', 'Troca automática em %1$d s', 'Mudar fonte', 'Continuar aguardando'])]:
  names = ['title', 'body', 'countdown', 'next', 'wait']
  xml = '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n' + ''.join('    <string name="danexus_slow_source_' + k + '">' + v + '</string>\n' for k, v in zip(names, values)) + '</resources>\n'
  edits[project / 'app/src/main/res' / locale / 'danexus_slow_source_strings.xml'] = xml
for path, text in edits.items():
 path.parent.mkdir(parents=True, exist_ok=True)
 path.write_text(text, encoding='utf-8')
print('Five-second prompt integrated in both editions:', len(edits), 'files')
