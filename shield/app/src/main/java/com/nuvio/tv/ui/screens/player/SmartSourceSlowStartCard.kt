package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.ui.theme.DanexusCinematic

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.ui.components.DanexusActionButton

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun SmartSourceSlowStartCard(
    secondsRemaining: Int,
    timeoutSeconds: Int,
    currentSource: String = "",
    nextSource: String = "",
    nextAddon: String = "",
    nextLogo: String? = null,
    attempt: Int = 1,
    total: Int = 1,
    onNextSource: () -> Unit,
    onWait: () -> Unit,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextFocus = remember { FocusRequester() }
    val latestClosed by rememberUpdatedState(onClosed)
    LaunchedEffect(Unit) {
        repeat(2) { withFrameNanos { } }
        runCatching { nextFocus.requestFocus() }
    }
    DisposableEffect(Unit) { onDispose { latestClosed() } }
    val progress = (secondsRemaining.toFloat() / timeoutSeconds.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(progress,
        animationSpec = androidx.compose.animation.core.tween(250), label = "sourceCountdown")
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier.width(332.dp).clip(shape)
            .background(DanexusCinematic.panelBrush)
            .border(1.dp, DanexusCinematic.edge, shape)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(R.drawable.danexus_avatar), null, Modifier.size(32.dp), contentScale = ContentScale.Fit)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.danexus_source_card_label), style = MaterialTheme.typography.labelMedium, color = DanexusCinematic.secondaryText)
                Text(stringResource(R.string.danexus_slow_source_title), style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Text("$attempt/$total", style = MaterialTheme.typography.labelLarge, color = DanexusCinematic.secondaryText)
        }
        Text(stringResource(R.string.danexus_source_current, currentSource), maxLines = 1,
            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = DanexusCinematic.secondaryText)
        Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = .06f), RoundedCornerShape(14.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!nextLogo.isNullOrBlank()) AsyncImage(model = nextLogo, contentDescription = null,
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)), contentScale = ContentScale.Fit)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.danexus_source_next_label), style = MaterialTheme.typography.labelSmall, color = DanexusCinematic.secondaryText)
                Text(nextSource.ifBlank { stringResource(R.string.danexus_source_loading_next) },
                    maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, color = Color.White)
                if (nextAddon.isNotBlank() && nextAddon != nextSource) Text(nextAddon, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = DanexusCinematic.secondaryText)
            }
        }
        Text(stringResource(R.string.danexus_slow_source_countdown, secondsRemaining.coerceAtLeast(1)),
            style = MaterialTheme.typography.labelLarge, color = DanexusCinematic.secondaryText)
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = .12f))) {
            Box(Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().background(DanexusCinematic.progressBrush))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DanexusActionButton(onClick = onNextSource, modifier = Modifier.weight(1f).focusRequester(nextFocus)) {
                Text(stringResource(R.string.danexus_slow_source_next), maxLines = 1)
            }
            DanexusActionButton(onClick = onWait, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.danexus_slow_source_wait), maxLines = 1)
            }
        }
    }
}
