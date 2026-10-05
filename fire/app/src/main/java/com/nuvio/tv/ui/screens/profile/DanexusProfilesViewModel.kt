package com.nuvio.tv.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.core.profile.ProfileManager
import com.nuvio.tv.core.danexus.DanexusRecommendations
import com.nuvio.tv.core.danexus.ProfileRecommendation
import com.nuvio.tv.data.remote.supabase.AvatarRepository
import com.nuvio.tv.domain.model.Meta
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DanexusProfilesViewModel @Inject constructor(
    val profileManager: ProfileManager,
    private val avatars: AvatarRepository,
    @ApplicationContext context: Context,
): ViewModel() {
    private val store = DanexusRecommendations(context)
    private val _items = MutableStateFlow(store.read())
    val items: StateFlow<List<ProfileRecommendation>> = _items
    private val _avatarUrls = MutableStateFlow<Map<Int, String>>(emptyMap())
    val avatarUrls: StateFlow<Map<Int, String>> = _avatarUrls
    init {
        viewModelScope.launch {
            val catalog = runCatching { avatars.getAvatarCatalog() }.getOrDefault(emptyList())
            profileManager.profiles.collect { profiles ->
                _avatarUrls.value = profiles.mapNotNull { p ->
                    (p.avatarUrl ?: p.avatarId?.let { avatars.getAvatarImageUrl(it, catalog) })?.let { p.id to it }
                }.toMap()
                refresh()
            }
        }
    }
    fun refresh() { _items.value = store.read() }
    fun send(to: Int, meta: Meta) = send(listOf(to), meta)
    fun send(to: Collection<Int>, meta: Meta) {
        val active = profileManager.activeProfileId.value
        val available = profileManager.profiles.value.map { it.id }.toSet()
        val recipients = com.nuvio.tv.core.danexus.ProfileRecommendationRules.recipients(active, to, available)
        if (recipients.isEmpty()) return
        store.send(active, recipients, meta)
        refresh()
    }
    fun open(item: ProfileRecommendation) { store.markSeen(item.key, profileManager.activeProfileId.value); refresh() }
    fun dismiss(item: ProfileRecommendation) { store.dismiss(item.key, profileManager.activeProfileId.value); refresh() }
}
