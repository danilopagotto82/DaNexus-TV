@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.nuvio.tv.ui.screens.player

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.danexus.*
import com.nuvio.tv.ui.components.NuvioDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import tv.seekr.previews.core.PreviewTrack
import tv.seekr.previews.core.SeekrPreviews

@Composable
internal fun DanexusSeekPreviewHost(viewModel: PlayerViewModel, showSync: Boolean, onDismissSync: () -> Unit) {
    val context = LocalContext.current
    val options by remember { DanexusPreferences.observe(context) }.collectAsState()
    val revision by DanexusSeekrKey.revision.collectAsState()
    val ui by viewModel.uiState.collectAsState()
    val timeline by viewModel.playbackTimeline.collectAsState()
    val mediaKey = listOf(viewModel.controller.contentId, ui.currentSeason, ui.currentEpisode, ui.currentStreamUrl)
    var offset by remember(mediaKey) { mutableStateOf(0) }
    var track by remember(mediaKey, revision, options.seekPreviews) { mutableStateOf<PreviewTrack?>(null) }
    var frame by remember(mediaKey) { mutableStateOf<Bitmap?>(null) }
    var frameMs by remember(mediaKey) { mutableStateOf(0L) }
    var lastScrubPosition by remember(mediaKey) { mutableStateOf<Long?>(null) }
    val images = remember(mediaKey) { DanexusSeekPreviewImages() }
    val hasDuration = timeline.duration > 0 && !timeline.isLive
    LaunchedEffect(mediaKey, revision, options.seekPreviews, hasDuration) {
        track = null; frame = null; offset = 0
        if (!options.seekPreviews || !hasDuration) return@LaunchedEffect
        val key = DanexusSeekrKey.read(context)
        if (!DanexusSeekPreviewPolicy.validKeyFormat(key)) return@LaunchedEffect
        val content = DanexusSeekPreviewPolicy.content(viewModel.controller.contentId, ui.contentType, ui.currentSeason, ui.currentEpisode)
            ?: return@LaunchedEffect
        track = withTimeoutOrNull(12_000L) {
            withContext(Dispatchers.IO) { SeekrPreviews.create(apiKey = key, httpClient = DanexusSeekPreviewImages.http)
                .loadTrack(content, timeline.duration) }
        }
    }
    LaunchedEffect(ui.pendingPreviewSeekPosition) {
        val pending = ui.pendingPreviewSeekPosition
        if (pending != null) lastScrubPosition = pending else {
            delay(900L)
            lastScrubPosition = null
        }
    }
    LaunchedEffect(track, ui.showControls) {
        if (ui.showControls) track?.tileAt(timeline.currentPosition)?.let { images.image(it) }
    }
    val position = ui.pendingPreviewSeekPosition ?: lastScrubPosition ?: timeline.currentPosition
    val visible = options.seekPreviews && !timeline.isLive && (lastScrubPosition != null || ui.pendingPreviewSeekPosition != null || showSync)
    LaunchedEffect(track, position, offset, visible) {
        frame = null
        if (!visible) return@LaunchedEffect
        val active = track ?: return@LaunchedEffect
        active.offsetMs = offset.toLong()
        val cue = active.cueAt(position) ?: return@LaunchedEffect
        val correctedPosition = position + offset
        val nearest = if (correctedPosition - cue.startMs > cue.endMs - correctedPosition)
            active.cueAt(cue.endMs - offset) ?: cue else cue
        val bitmap = images.image(nearest.tile) ?: return@LaunchedEffect
        frameMs = DanexusSeekPreviewPolicy.framePosition(nearest.startMs, offset)
        frame = bitmap
    }
    @Composable fun preview() {
        frame?.let { bitmap ->
            Column(Modifier.width(192.dp).clip(RoundedCornerShape(12.dp))
                .background(Brush.verticalGradient(listOf(Color(0xE6090A0D), Color(0xEE080F19))))
                .border(1.dp, Color.White.copy(alpha = .28f), RoundedCornerShape(12.dp)).padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Image(bitmap.asImageBitmap(), stringResource(R.string.danexus_seek_frame), Modifier.fillMaxWidth().aspectRatio(16f/9f).clip(RoundedCornerShape(10.dp)))
                Text(stringResource(R.string.danexus_seek_time, previewTime(frameMs)), Modifier.padding(top = 4.dp))
            }
        }
    }
    if (visible && !showSync && frame != null) Box(Modifier.fillMaxSize().padding(bottom = 112.dp), contentAlignment = Alignment.BottomCenter) { preview() }
    if (showSync) NuvioDialog(onDismiss = onDismissSync, title = stringResource(R.string.danexus_seek_sync),
        subtitle = stringResource(R.string.danexus_seek_sync_desc), width = 560.dp) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (frame != null) preview() else Text(stringResource(R.string.danexus_seek_no_preview))
        }
        Text(stringResource(R.string.danexus_seek_offset, offset / 1000))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { offset = DanexusSeekPreviewPolicy.offset(offset - 1000) }) { Text("−1 s") }
            Button(onClick = { offset = 0 }) { Text(stringResource(R.string.danexus_seek_reset)) }
            Button(onClick = { offset = DanexusSeekPreviewPolicy.offset(offset + 1000) }) { Text("+1 s") }
        }
        Button(onClick = onDismissSync) { Text(stringResource(R.string.danexus_close)) }
    }
}

private fun previewTime(ms: Long): String {
    val seconds = ms / 1000
    return if (seconds >= 3600) "%d:%02d:%02d".format(seconds/3600, seconds/60%60, seconds%60)
        else "%d:%02d".format(seconds/60, seconds%60)
}
