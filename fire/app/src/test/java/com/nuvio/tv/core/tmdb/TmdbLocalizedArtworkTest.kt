package com.nuvio.tv.core.tmdb

import com.nuvio.tv.data.remote.api.*
import com.nuvio.tv.domain.model.ContentType
import io.mockk.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Response

class TmdbLocalizedArtworkTest {
    private fun api(logos: List<TmdbImage>): TmdbApi = mockk<TmdbApi>().also {
        coEvery { it.getMovieDetails(any(), any(), any()) } returns Response.success(
            TmdbDetailsResponse(id = 10, title = "A Odisseia", overview = "Sinopse"))
        coEvery { it.getMovieImages(any(), any(), any()) } returns Response.success(
            TmdbImagesResponse(logos = logos))
        coEvery { it.getMovieCredits(any(), any(), any()) } returns Response.success(TmdbCreditsResponse())
        coEvery { it.getMovieReleaseDates(any(), any()) } returns Response.success(TmdbMovieReleaseDatesResponse())
        coEvery { it.getMovieVideos(any(), any(), any()) } returns Response.success(TmdbVideosResponse(id = 10))
    }

    @Test fun portugueseLogoWinsInSharedHomeAndDetailsMetadataRegardlessOfEnglishRating() = runTest {
        val api = api(listOf(
            TmdbImage(filePath = "/english.png", iso6391 = "en", voteAverage = 10.0),
            TmdbImage(filePath = null, iso6391 = "pt"),
            TmdbImage(filePath = "/pt-low.png", iso6391 = "pt", voteAverage = 1.0),
            TmdbImage(filePath = "/portugues.png", iso6391 = "pt", voteAverage = 8.0)
        ))
        val service = TmdbMetadataService(api, StandardTestDispatcher(testScheduler))
        val result = service.fetchEnrichment("10", ContentType.MOVIE, "pt_BR")
        assertTrue(result!!.logo!!.endsWith("/portugues.png"))
        coVerify { api.getMovieDetails(10, any(), "pt-BR") }
        coVerify { api.getMovieImages(10, any(), "pt,en,null") }
    }

    @Test fun missingPortugueseLogoDoesNotReplaceExistingArtworkWithEnglish() = runTest {
        val service = TmdbMetadataService(api(listOf(
            TmdbImage(filePath = "/english.png", iso6391 = "en")
        )), StandardTestDispatcher(testScheduler))
        assertNull(service.fetchEnrichment("10", ContentType.MOVIE, "pt-BR")!!.logo)
    }

    @Test fun neutralLogoIsAvailableWhenLocalizedLogoIsMissing() = runTest {
        val service = TmdbMetadataService(api(listOf(
            TmdbImage(filePath = "/english.png", iso6391 = "en"),
            TmdbImage(filePath = "/neutral.png")
        )), StandardTestDispatcher(testScheduler))
        assertTrue(service.fetchEnrichment("10", ContentType.MOVIE, "pt-BR")!!.logo!!.endsWith("/neutral.png"))
    }

    @Test fun englishCacheDoesNotOverridePortugueseAndExplicitEnglishStillWorks() = runTest {
        val api = api(listOf(
            TmdbImage(filePath = "/english.png", iso6391 = "en"),
            TmdbImage(filePath = "/portugues.png", iso6391 = "pt")
        ))
        val service = TmdbMetadataService(api, StandardTestDispatcher(testScheduler))
        assertTrue(service.fetchEnrichment("10", ContentType.MOVIE, "en")!!.logo!!.endsWith("/english.png"))
        val portuguese = service.fetchEnrichment("10", ContentType.MOVIE, "pt-BR")
        assertTrue(portuguese!!.logo!!.endsWith("/portugues.png"))
        assertEquals(portuguese, service.fetchEnrichment("10", ContentType.MOVIE, "pt-BR"))
        coVerify(exactly = 2) { api.getMovieImages(any(), any(), any()) }
    }
}
