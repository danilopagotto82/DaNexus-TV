package com.nuvio.tv.core.danexus

import org.junit.Assert.*
import org.junit.Test
import tv.seekr.previews.core.SeekrContent

class DanexusSeekPreviewPolicyTest {
    @Test fun movieAndEpisodeIdentifiersStaySeparate() {
        assertEquals(SeekrContent.Movie(imdbId = "tt0133093"), DanexusSeekPreviewPolicy.content("tt0133093", "movie", null, null))
        assertEquals(SeekrContent.Episode(showTmdbId = 1399, season = 1, episode = 5), DanexusSeekPreviewPolicy.content("tmdb:1399:1:5", "series", 1, 5))
        assertNull(DanexusSeekPreviewPolicy.content("tt0903747", "series", null, null))
        assertNull(DanexusSeekPreviewPolicy.content("custom:demo", "movie", null, null))
        assertNull(DanexusSeekPreviewPolicy.content("1399", "channel", null, null))
    }
    @Test fun onlySignedSpriteHostCanReceiveTileRequests() {
        assertTrue(DanexusSeekPreviewPolicy.trustedTile("https://sprites.seekr.tv/demo.jpg?exp=1&sig=fake"))
        for (url in listOf("http://sprites.seekr.tv/a", "https://sprites.seekr.tv.evil.example/a", "https://api.seekr.tv/a", "https://user@sprites.seekr.tv/a", "https://sprites.seekr.tv:444/a")) assertFalse(url, DanexusSeekPreviewPolicy.trustedTile(url))
    }
    @Test fun offsetsAreBoundedAndLabelTheActualFrame() {
        assertEquals(120_000, DanexusSeekPreviewPolicy.offset(999_000))
        assertEquals(-120_000, DanexusSeekPreviewPolicy.offset(-999_000))
        assertEquals(630_000L, DanexusSeekPreviewPolicy.framePosition(600_000L, -30_000))
        assertEquals(0L, DanexusSeekPreviewPolicy.framePosition(0L, 10_000))
    }
    @Test fun invalidKeysAreRejectedBeforeNetworkUse() {
        assertTrue(DanexusSeekPreviewPolicy.validKeyFormat("sk_live_" + "a".repeat(64)))
        assertFalse(DanexusSeekPreviewPolicy.validKeyFormat("sk_live_sample"))
        assertFalse(DanexusSeekPreviewPolicy.validKeyFormat("sk_live_" + "g".repeat(64)))
    }
}
