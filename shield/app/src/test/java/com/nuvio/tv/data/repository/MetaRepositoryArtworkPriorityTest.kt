package com.nuvio.tv.data.repository

import android.content.Context
import com.nuvio.tv.core.network.NetworkResult
import com.nuvio.tv.data.remote.api.AddonApi
import com.nuvio.tv.data.remote.dto.MetaDto
import com.nuvio.tv.data.remote.dto.MetaResponseDto
import com.nuvio.tv.domain.model.Addon
import com.nuvio.tv.domain.model.AddonResource
import com.nuvio.tv.domain.model.ContentType
import com.nuvio.tv.domain.repository.AddonRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class MetaRepositoryArtworkPriorityTest {
    private val id = "tt1234567"
    private val portuguese = addon("pt")
    private val english = addon("en")

    private fun addon(locale: String) = Addon(
        id = locale, name = locale, version = "1", description = null, logo = null,
        baseUrl = "https://$locale.example", catalogs = emptyList(),
        types = listOf(ContentType.MOVIE), rawTypes = listOf("movie"),
        resources = listOf(AddonResource("meta", listOf("movie"), listOf("tt"))),
        idPrefixes = listOf("tt")
    )

    private fun api(): AddonApi = mockk<AddonApi>().also {
        coEvery { it.getMeta(any()) } coAnswers {
            val locale = if (firstArg<String>().contains("pt.example")) "pt" else "en"
            Response.success(MetaResponseDto(meta = MetaDto(
                id = id, type = "movie", name = locale, logo = "https://images.example/$locale.png"
            )))
        }
    }

    private fun repository(api: AddonApi): MetaRepositoryImpl {
        val context = mockk<Context> { every { getString(any()) } returns "Episode" }
        val addons = mockk<AddonRepository> {
            every { getInstalledAddons() } returns flowOf(listOf(portuguese, english))
        }
        return MetaRepositoryImpl(context, api, addons, mockk(relaxed = true), mockk(relaxed = true))
    }

    @Test fun directEnglishLookupDoesNotChangeConfiguredMetadataPriority() = runTest {
        val api = api()
        val repository = repository(api)
        repository.getMeta(english.baseUrl, "movie", id).last()
        val result = repository.getMetaFromAllAddons("movie", id).last()
        assertEquals("https://images.example/pt.png", (result as NetworkResult.Success).data.logo)
    }

    @Test fun detailsCacheCannotReplaceCatalogArtworkInSourceAwareEnrichment() = runTest {
        val repository = repository(api())
        assertTrue(repository.getMetaFromAllAddons("movie", id).last() is NetworkResult.Success)
        val artwork = repository.getMetaFromAllAddons("movie", id, portuguese.baseUrl + "/").last()
        assertTrue(artwork is NetworkResult.Error)
        assertEquals(NetworkResult.SOURCE_SUFFICIENT_CODE, (artwork as NetworkResult.Error).code)
    }

    @Test fun chosenAddonReusesItsOwnCacheWithoutChangingSourcePriority() = runTest {
        val api = api()
        val repository = repository(api)
        repository.getMeta(portuguese.baseUrl, "movie", id).last()
        val result = repository.getMetaFromAllAddons("movie", id).last()
        assertEquals("pt", (result as NetworkResult.Success).data.name)
        coVerify(exactly = 1) { api.getMeta(any()) }
    }
}
