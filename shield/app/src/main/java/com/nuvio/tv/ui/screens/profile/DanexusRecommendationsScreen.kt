@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.core.danexus.ProfileRecommendation
import com.nuvio.tv.core.danexus.ProfileRecommendationRules
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.ui.components.DanexusActionButton
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.components.ProfileAvatarCircle
import com.nuvio.tv.ui.theme.DanexusCinematic

@Composable
fun DanexusRecommendDialog(meta: Meta, onDismiss: () -> Unit, viewModel: DanexusProfilesViewModel = hiltViewModel()) {
    val profiles by viewModel.profileManager.profiles.collectAsState()
    val active by viewModel.profileManager.activeProfileId.collectAsState()
    val avatarUrls by viewModel.avatarUrls.collectAsState()
    val records by viewModel.items.collectAsState()
    val firstProfileFocus = remember { FocusRequester() }
    val cancelFocus = remember { FocusRequester() }
    val recipients = profiles.filter { it.id != active }
    var selectedRecipients by remember(active, meta.id, meta.apiType) { mutableStateOf(emptySet<Int>()) }
    var deliveredCount by remember(active, meta.id, meta.apiType) { mutableStateOf(0) }
    val selectedTargets = ProfileRecommendationRules.recipients(active, selectedRecipients, recipients.map { it.id }.toSet())
    val allSelected = recipients.isNotEmpty() && selectedTargets.size == recipients.size

    LaunchedEffect(active, meta.id, meta.apiType, recipients.map { it.id }) {
        viewModel.refresh()
        repeat(2) { withFrameNanos { } }
        runCatching { if (recipients.isEmpty()) cancelFocus.requestFocus() else firstProfileFocus.requestFocus() }
    }

    NuvioDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.danexus_recommend_film),
        width = 680.dp,
        contentSpacing = 14.dp,
        containerBrush = DanexusCinematic.panelBrush,
        containerBorderColor = DanexusCinematic.edge,
        backgroundContent = {
            RecommendationArtwork(meta.background ?: meta.landscapePoster, Modifier.matchParentSize())
        }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RecommendationPoster(meta.poster, Modifier.width(54.dp).height(78.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(meta.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.danexus_recommend_choose_confirm), style = MaterialTheme.typography.bodySmall,
                    color = DanexusCinematic.secondaryText)
            }
        }

        if (recipients.isEmpty()) {
            Text(stringResource(R.string.danexus_recommend_no_profiles), color = DanexusCinematic.secondaryText)
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recipients, key = { it.id }) { profile ->
                    val isSelected = profile.id in selectedTargets
                    val alreadySent = records.any { it.from == active && it.to == profile.id && it.contentId == meta.id && it.type == meta.apiType }
                    DanexusActionButton(
                        onClick = {
                            selectedRecipients = if (isSelected) selectedTargets - profile.id else selectedTargets + profile.id
                            deliveredCount = 0
                        },
                        selected = isSelected,
                        modifier = Modifier.width(112.dp).then(
                            if (profile.id == recipients.first().id) Modifier.focusRequester(firstProfileFocus) else Modifier
                        ),
                        contentPadding = PaddingValues(10.dp)
                    ) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(48.dp)) {
                                ProfileAvatarCircle(profile.name, profile.avatarColorHex, size = 48.dp, avatarImageUrl = avatarUrls[profile.id])
                                if (isSelected) Box(
                                    Modifier.align(Alignment.BottomEnd).size(18.dp).background(DanexusCinematic.focus, RoundedCornerShape(9.dp)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = DanexusCinematic.black) }
                            }
                            Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                            Text(stringResource(if (isSelected) R.string.danexus_recommend_selected else if (alreadySent)
                                R.string.danexus_recommend_sent else R.string.danexus_recommend_select),
                                style = MaterialTheme.typography.labelSmall, color = DanexusCinematic.secondaryText)
                        }
                    }
                }
            }
        }

        Text(
            if (deliveredCount > 0) stringResource(R.string.danexus_recommend_send_complete, deliveredCount)
            else stringResource(R.string.danexus_recommend_selection_count, selectedTargets.size),
            style = MaterialTheme.typography.bodySmall,
            color = DanexusCinematic.secondaryText
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (recipients.isNotEmpty()) {
                DanexusActionButton(enabled = selectedTargets.isNotEmpty(), onClick = {
                    viewModel.send(selectedTargets, meta)
                    deliveredCount = selectedTargets.size
                    selectedRecipients = emptySet()
                }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.danexus_recommend_confirm_selected, selectedTargets.size), maxLines = 1)
                }
                DanexusActionButton(selected = allSelected, onClick = {
                    selectedRecipients = recipients.map { it.id }.toSet()
                    deliveredCount = 0
                }) { Text(stringResource(R.string.danexus_recommend_send_all), maxLines = 1) }
            }
            DanexusActionButton(onClick = onDismiss, modifier = Modifier.focusRequester(cancelFocus)) {
                Text(stringResource(if (deliveredCount > 0) R.string.danexus_close else R.string.danexus_recommend_cancel))
            }
        }
    }
}

@Composable
fun DanexusRecommendationsScreen(onOpen: (String, String) -> Unit, viewModel: DanexusProfilesViewModel = hiltViewModel()) {
    val profiles by viewModel.profileManager.profiles.collectAsState()
    val active by viewModel.profileManager.activeProfileId.collectAsState()
    val avatarUrls by viewModel.avatarUrls.collectAsState()
    val records by viewModel.items.collectAsState()
    var sent by remember(active) { mutableStateOf(false) }
    var filter by remember(active) { mutableStateOf<Int?>(null) }
    var focusedKey by remember(active) { mutableStateOf<String?>(null) }
    var pendingRemoval by remember(active) { mutableStateOf<ProfileRecommendation?>(null) }
    var focusAfterRemoval by remember(active) { mutableStateOf<String?>(null) }
    var removalEpoch by remember(active) { mutableStateOf(0) }
    val receivedFocus = remember { FocusRequester() }
    val cardFocus = remember(active) { mutableMapOf<String, FocusRequester>() }
    val visible = ProfileRecommendationRules.inbox(records, active, sent, filter)
    val artwork = visible.firstOrNull { it.key == focusedKey }?.backdrop ?: visible.firstOrNull()?.backdrop

    LaunchedEffect(active) { viewModel.refresh() }
    LaunchedEffect(active, removalEpoch) {
        repeat(2) { withFrameNanos { } }
        runCatching { (focusAfterRemoval?.let { cardFocus[it] } ?: receivedFocus).requestFocus() }
    }

    Box(Modifier.fillMaxSize().background(DanexusCinematic.screenBrush)) {
        RecommendationArtwork(artwork, Modifier.matchParentSize())
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.danexus_recommendations), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.danexus_recommend_inbox_desc), style = MaterialTheme.typography.bodyMedium,
                color = DanexusCinematic.secondaryText)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DanexusActionButton(selected = !sent, onClick = { sent = false; filter = null }, modifier = Modifier.focusRequester(receivedFocus)) {
                    Text(stringResource(R.string.danexus_recommend_received))
                }
                DanexusActionButton(selected = sent, onClick = { sent = true; filter = null }) {
                    Text(stringResource(R.string.danexus_recommend_sent_tab))
                }
            }
            LazyRow(contentPadding = PaddingValues(4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    DanexusActionButton(selected = filter == null, onClick = { filter = null }) {
                        Text(stringResource(R.string.danexus_recommend_all))
                    }
                }
                items(profiles.filter { it.id != active }, key = { it.id }) { profile ->
                    DanexusActionButton(selected = filter == profile.id, onClick = { filter = profile.id }) {
                        ProfileAvatarCircle(profile.name, profile.avatarColorHex, size = 26.dp, avatarImageUrl = avatarUrls[profile.id])
                        Spacer(Modifier.width(8.dp))
                        Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (visible.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Default.Movie, null, Modifier.size(36.dp), tint = DanexusCinematic.secondaryText)
                        Text(stringResource(R.string.danexus_recommend_empty), color = DanexusCinematic.secondaryText)
                    }
                }
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(6.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(visible, key = { it.key }) { item ->
                        val person = profiles.firstOrNull { it.id == if (sent) item.to else item.from }
                        val shape = RoundedCornerShape(14.dp)
                        val requester = cardFocus.getOrPut(item.key) { FocusRequester() }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Card(
                                onClick = { viewModel.open(item); onOpen(item.contentId, item.type) },
                                modifier = Modifier.weight(1f).focusRequester(requester).onFocusChanged { if (it.isFocused) focusedKey = item.key },
                                colors = CardDefaults.colors(containerColor = Color.Transparent, focusedContainerColor = Color.Transparent),
                                shape = CardDefaults.shape(shape),
                                scale = CardDefaults.scale(focusedScale = 1.012f),
                                border = CardDefaults.border(border = Border.None,
                                    focusedBorder = Border(BorderStroke(2.dp, DanexusCinematic.focus), shape = shape))
                            ) {
                                Box(Modifier.fillMaxWidth().height(124.dp).background(DanexusCinematic.cardBrush)) {
                                    item.backdrop?.let { background ->
                                        AsyncImage(background, null, Modifier.fillMaxWidth(0.5f).fillMaxHeight().align(Alignment.CenterEnd), contentScale = ContentScale.Crop)
                                        Box(Modifier.matchParentSize().background(DanexusCinematic.artworkScrim))
                                    }
                                    Row(Modifier.fillMaxSize().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                        RecommendationPoster(item.poster, Modifier.width(70.dp).height(100.dp))
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = DanexusCinematic.text,
                                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                if (person != null) ProfileAvatarCircle(person.name, person.avatarColorHex, size = 24.dp, avatarImageUrl = avatarUrls[person.id])
                                                Text(stringResource(if (sent) R.string.danexus_recommend_to else R.string.danexus_recommend_from,
                                                    person?.name ?: stringResource(R.string.danexus_recommend_missing_profile)),
                                                    style = MaterialTheme.typography.bodyMedium, color = DanexusCinematic.secondaryText,
                                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        if (!sent && !item.seen) Text(stringResource(R.string.danexus_recommend_new),
                                            style = MaterialTheme.typography.labelMedium, color = DanexusCinematic.text,
                                            modifier = Modifier.align(Alignment.Top).background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }
                            }
                            // Remover remains a sibling: clicking a card can never delete a recommendation.
                            DanexusActionButton(destructive = true, onClick = { pendingRemoval = item }) {
                                Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.danexus_recommend_remove))
                            }
                        }
                    }
                }
            }
        }
    }

    pendingRemoval?.let { item ->
        RecommendationRemovalDialog(item, onDismiss = { pendingRemoval = null }, onConfirm = {
            val remaining = visible.filter { it.key != item.key }
            val index = visible.indexOfFirst { it.key == item.key }.coerceAtLeast(0)
            focusAfterRemoval = remaining.getOrNull(index.coerceAtMost((remaining.size - 1).coerceAtLeast(0)))?.key
            viewModel.dismiss(item)
            pendingRemoval = null
            removalEpoch++
        })
    }
}

@Composable
private fun RecommendationRemovalDialog(item: ProfileRecommendation, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(item.key) { repeat(2) { withFrameNanos { } }; runCatching { cancelFocus.requestFocus() } }
    NuvioDialog(onDismiss = onDismiss, title = stringResource(R.string.danexus_recommend_remove_title), subtitle = item.title,
        containerBrush = DanexusCinematic.panelBrush, containerBorderColor = DanexusCinematic.edge,
        backgroundContent = { RecommendationArtwork(item.backdrop, Modifier.matchParentSize()) }) {
        Text(stringResource(R.string.danexus_recommend_remove_desc), style = MaterialTheme.typography.bodyMedium,
            color = DanexusCinematic.secondaryText)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DanexusActionButton(onClick = onDismiss, modifier = Modifier.focusRequester(cancelFocus)) {
                Text(stringResource(R.string.danexus_recommend_cancel))
            }
            DanexusActionButton(onClick = onConfirm, destructive = true) {
                Icon(Icons.Default.DeleteOutline, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.danexus_recommend_remove))
            }
        }
    }
}

@Composable
private fun RecommendationPoster(poster: String?, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(DanexusCinematic.graphite), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Movie, null, Modifier.size(24.dp), tint = DanexusCinematic.secondaryText.copy(alpha = 0.4f))
        if (!poster.isNullOrBlank()) AsyncImage(poster, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun RecommendationArtwork(artwork: String?, modifier: Modifier) {
    if (!artwork.isNullOrBlank()) Box(modifier) {
        AsyncImage(artwork, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, alignment = Alignment.CenterEnd)
        Box(Modifier.matchParentSize().background(DanexusCinematic.backdropBrush))
        Box(Modifier.matchParentSize().background(DanexusCinematic.bottomScrim))
        // Feather the outer edges into the screen, preserving the image in the middle.
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            0f to Color.Transparent, 0.72f to Color.Transparent,
            0.90f to DanexusCinematic.black.copy(alpha = 0.35f),
            1f to DanexusCinematic.black.copy(alpha = 0.92f)
        )))
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(
            0f to DanexusCinematic.black.copy(alpha = 0.8f),
            0.12f to Color.Transparent, 0.78f to Color.Transparent,
            1f to DanexusCinematic.black.copy(alpha = 0.65f)
        )))
    }
}
