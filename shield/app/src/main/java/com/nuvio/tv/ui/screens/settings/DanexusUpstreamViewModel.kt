package com.nuvio.tv.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nuvio.tv.R
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.data.remote.api.GitHubReleaseApi
import com.nuvio.tv.updater.VersionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DanexusUpstreamViewModel @Inject constructor(private val api: GitHubReleaseApi, @ApplicationContext private val context: Context): ViewModel() {
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status
    private var checking = false
    fun check() {
        if (checking) return
        checking = true
        _status.value = context.getString(R.string.danexus_upstream_checking)
        viewModelScope.launch {
            _status.value = runCatching {
                val response = api.getReleases("ysosrs123", "NuvioTV-Fork")
                if (!response.isSuccessful) error("HTTP ${response.code()}")
                val latest = response.body().orEmpty().filter { !it.draft && VersionUtils.parse(it.tagName) != null }
                    .maxByOrNull { VersionUtils.parse(it.tagName)!! }
                    ?: error("No release")
                val tag = latest.tagName.orEmpty()
                val current = BuildConfig.VERSION_NAME.substringBefore("-danexus")
                if (VersionUtils.isRemoteNewer(tag, current)) context.getString(R.string.danexus_upstream_available, tag, current)
                else context.getString(R.string.danexus_upstream_current, current)
            }.getOrElse { context.getString(R.string.danexus_upstream_failed) }
            checking = false
        }
    }
}
