package com.nuvio.tv.updater

import com.nuvio.tv.data.remote.dto.GitHubAssetDto
import com.nuvio.tv.data.remote.dto.GitHubReleaseDto

/** Bind an APK to its edition, architecture, version and DaNexus release URL. */
internal object DanexusUpdateAssets {
    fun choose(release: GitHubReleaseDto, prefix: String, abi: String): GitHubAssetDto? {
        val tag = release.tagName?.trim()?.takeIf { VersionUtils.parse(it) != null } ?: return null
        val name = prefix + VersionUtils.normalize(tag) + "-" + abi + ".apk"
        val url = "https://github.com/danilopagotto82/DaNexus-TV/releases/download/$tag/$name"
        return release.assets.firstOrNull {
            it.name == name && it.browserDownloadUrl == url && (it.size == null || it.size > 0L)
        }
    }
    fun notesSummary(notes: String): String = notes
        .replace(Regex("<!--[\\s\\S]*?-->"), "")
        .lineSequence().map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .map { it.trimStart('-', '*', ' ') }
        .take(2).joinToString(" • ").take(240)
}
