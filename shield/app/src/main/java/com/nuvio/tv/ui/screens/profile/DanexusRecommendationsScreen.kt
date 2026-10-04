@file:OptIn(androidx.tv.material3.ExperimentalTvMaterial3Api::class)
package com.nuvio.tv.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.nuvio.tv.domain.model.Meta
import com.nuvio.tv.core.danexus.ProfileRecommendationRules
import com.nuvio.tv.ui.components.NuvioDialog
import com.nuvio.tv.ui.components.ProfileAvatarCircle

@Composable
fun DanexusRecommendDialog(meta: Meta, onDismiss: () -> Unit, viewModel: DanexusProfilesViewModel = hiltViewModel()) {
    val profiles by viewModel.profileManager.profiles.collectAsState()
    val active by viewModel.profileManager.activeProfileId.collectAsState()
    val avatarUrls by viewModel.avatarUrls.collectAsState()
    val records by viewModel.items.collectAsState()
    val focus = remember { FocusRequester() }
    val recipients = profiles.filter { it.id != active }
    LaunchedEffect(meta.id) { viewModel.refresh(); repeat(2) { withFrameNanos { } }; runCatching { focus.requestFocus() } }
    NuvioDialog(onDismiss = onDismiss, title = stringResource(R.string.danexus_recommend_film), subtitle = meta.name, width = 600.dp,
        containerBrush = Brush.linearGradient(listOf(Color(0xF5122334), Color(0xF5231B39)))) {
        Text(stringResource(R.string.danexus_recommend_dialog_desc))
        if (recipients.isEmpty()) Text(stringResource(R.string.danexus_recommend_no_profiles))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recipients, key = { it.id }) { p ->
                val sent = records.any { it.from == active && it.to == p.id && it.contentId == meta.id && it.type == meta.apiType }
                Button(onClick = { viewModel.send(p.id, meta) }, modifier = Modifier.width(104.dp).then(if (p == recipients.firstOrNull()) Modifier.focusRequester(focus) else Modifier)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileAvatarCircle(p.name, p.avatarColorHex, size = 48.dp, avatarImageUrl = avatarUrls[p.id])
                        Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (sent) Text(stringResource(R.string.danexus_recommend_sent), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        Button(onClick = onDismiss, modifier = if (recipients.isEmpty()) Modifier.focusRequester(focus) else Modifier) { Text(stringResource(R.string.danexus_close)) }
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
    LaunchedEffect(active) { viewModel.refresh() }
    val visible = ProfileRecommendationRules.inbox(records, active, sent, filter)
    Column(Modifier.fillMaxSize().background(Color(0xFF090E1D)).padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.danexus_recommendations), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.danexus_recommend_inbox_desc), color = Color(0xFFADBCD1))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { sent = false; filter = null }) { Text(stringResource(R.string.danexus_recommend_received)) }
            Button(onClick = { sent = true; filter = null }) { Text(stringResource(R.string.danexus_recommend_sent_tab)) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Button(onClick = { filter = null }) { Text(stringResource(R.string.danexus_recommend_all)) } }
            items(profiles.filter { it.id != active }, key = { it.id }) { p ->
                Button(onClick = { filter = p.id }) {
                    ProfileAvatarCircle(p.name, p.avatarColorHex, size = 30.dp, avatarImageUrl = avatarUrls[p.id])
                    Spacer(Modifier.width(8.dp)); Text(p.name)
                }
            }
        }
        if (visible.isEmpty()) Text(stringResource(R.string.danexus_recommend_empty), color = Color(0xFFAEBCD1))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(visible, key = { it.key }) { item ->
                Card(onClick = { viewModel.open(item); onOpen(item.contentId, item.type) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(item.poster, null, Modifier.width(64.dp).height(90.dp), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium)
                            val person = profiles.firstOrNull { it.id == if (sent) item.to else item.from }
                            Text(stringResource(if (sent) R.string.danexus_recommend_to else R.string.danexus_recommend_from, person?.name.orEmpty()), color = Color(0xFFB7CAE0))
                            if (!sent && !item.seen) Text(stringResource(R.string.danexus_recommend_new), color = Color(0xFF81DFE5))
                        }
                        Button(onClick = { viewModel.dismiss(item) }) { Text(stringResource(R.string.danexus_recommend_remove)) }
                    }
                }
            }
        }
    }
}
