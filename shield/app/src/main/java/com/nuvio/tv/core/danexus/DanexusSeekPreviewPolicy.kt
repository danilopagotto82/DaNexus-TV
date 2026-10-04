package com.nuvio.tv.core.danexus

import java.net.URI
import tv.seekr.previews.core.SeekrContent

object DanexusSeekPreviewPolicy {
    fun content(id: String?, type: String?, season: Int?, episode: Int?): SeekrContent? {
        val raw = id?.trim()?.removePrefix("tmdb:")?.removePrefix("movie:")?.removePrefix("series:")
            ?.substringBefore(':')?.substringBefore('/') ?: return null
        val imdb = raw.takeIf { Regex("tt[0-9]{5,12}").matches(it) }
        val tmdb = raw.toIntOrNull()?.takeIf { it > 0 }
        if (imdb == null && tmdb == null) return null
        return when (type?.lowercase()) {
            "series", "tv", "show" -> if (season != null && season >= 0 && episode != null && episode > 0)
                SeekrContent.Episode(showTmdbId = tmdb, showImdbId = imdb, season = season, episode = episode) else null
            "movie", "film" -> SeekrContent.Movie(tmdbId = tmdb, imdbId = imdb)
            else -> null
        }
    }
    fun trustedTile(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.host.equals("sprites.seekr.tv", true) && uri.userInfo == null && uri.port in listOf(-1, 443)
    }.getOrDefault(false)
    fun validKeyFormat(key: String) = Regex("sk_live_[a-fA-F0-9]{64}").matches(key.trim())
    fun offset(value: Int) = value.coerceIn(-120_000, 120_000)
    fun framePosition(cueMs: Long, offsetMs: Int) = (cueMs - offsetMs).coerceAtLeast(0L)
}
