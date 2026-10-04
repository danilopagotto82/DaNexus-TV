package com.nuvio.tv.ui.screens.player

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
            .background(Brush.linearGradient(listOf(Color(0xDA122638), Color(0xDC161C30), Color(0xD82C1C46))))
            .border(1.dp, Brush.linearGradient(listOf(Color(0x996BDEEF), Color(0x775E72A3), Color(0x998D60CC))), shape)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Image(painterResource(R.drawable.danexus_avatar), null, Modifier.size(32.dp), contentScale = ContentScale.Fit)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.danexus_source_card_label), style = MaterialTheme.typography.labelMedium, color = Color(0xFF83DAEB))
                Text(stringResource(R.string.danexus_slow_source_title), style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Text("$attempt/$total", style = MaterialTheme.typography.labelLarge, color = Color(0xFFCBD9F2))
        }
        Text(stringResource(R.string.danexus_source_current, currentSource), maxLines = 1,
            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = Color(0xFFBDCCDF))
        Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = .06f), RoundedCornerShape(14.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!nextLogo.isNullOrBlank()) AsyncImage(model = nextLogo, contentDescription = null,
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)), contentScale = ContentScale.Fit)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.danexus_source_next_label), style = MaterialTheme.typography.labelSmall, color = Color(0xFF9AAAC6))
                Text(nextSource.ifBlank { stringResource(R.string.danexus_source_loading_next) },
                    maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, color = Color.White)
                if (nextAddon.isNotBlank() && nextAddon != nextSource) Text(nextAddon, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFAADF))
            }
        }
        Text(stringResource(R.string.danexus_slow_source_countdown, secondsRemaining.coerceAtLeast(1)),
            style = MaterialTheme.typography.labelLarge, color = Color(0xFFBDECF3))
        Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = .12f))) {
            Box(Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF62D7E8), Color(0xFFB586F0)))))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onNextSource, modifier = Modifier.weight(1f).focusRequester(nextFocus)) {
                Text(stringResource(R.string.danexus_slow_source_next), maxLines = 1)
            }
            Button(onClick = onWait, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.danexus_slow_source_wait), maxLines = 1)
            }
        }
    }
}
