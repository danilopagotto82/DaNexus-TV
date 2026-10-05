package com.nuvio.tv.updater

import com.nuvio.tv.BuildConfig
import com.nuvio.tv.data.remote.api.GitHubReleaseApi
import com.nuvio.tv.data.remote.dto.GitHubAssetDto
import com.nuvio.tv.data.remote.dto.GitHubReleaseDto
import io.mockk.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class DanexusUpdateRepositoryTest {
    private val api = mockk<GitHubReleaseApi>()
    private val repository = UpdateRepository(api)
    private fun release(version: String, prefix: String = "DaNexus-FireTV-",
                        abi: String = "armeabi-v7a", draft: Boolean = false): GitHubReleaseDto {
        val name = prefix + version + "-$abi.apk"
        return GitHubReleaseDto(tagName = "v$version", body = "Arte PT-BR", draft = draft,
            assets = listOf(GitHubAssetDto(name,
                "https://github.com/danilopagotto82/DaNexus-TV/releases/download/v$version/$name", 42)))
    }

    @Test fun selectsNewerFireBuildWithoutMixingShieldOrDrafts() = runTest {
        coEvery { api.getReleases(any(), any(), any(), any()) } returns Response.success(listOf(
            release("9.0.0", "DaNexus-Shield-", "arm64-v8a"),
            release("8.0.0", draft = true),
            release("1.1.0-beta.4-danexus.9"),
            release("1.1.0-beta.4-danexus.10")))
        val result = repository.getLatestUpdate(UpdateChannel.BETA).getOrThrow()
        assertEquals("v1.1.0-beta.4-danexus.10", result.tag)
        assertEquals("Arte PT-BR", result.notes)
        assertTrue(VersionUtils.isRemoteNewer(result.tag, "1.1.0-beta.4-danexus.9"))
        coVerify { api.getReleases("danilopagotto82", "DaNexus-TV", 100, 1) }
    }

    @Test fun stableChannelFindsItsOwnEditionInsteadOfRepositoryLatest() = runTest {
        coEvery { api.getReleases(any(), any(), any(), any()) } returns Response.success(listOf(
            release("4.0.0", "DaNexus-Shield-", "arm64-v8a"),
            release("3.0.0-beta.1"),
            release("2.0.0")))
        assertEquals("v2.0.0", repository.getLatestUpdate(UpdateChannel.STABLE).getOrThrow().tag)
        coVerify(exactly = 0) { api.getLatestRelease(any(), any()) }
    }

    @Test fun paginatedFeedAndCompatibleAssetAreRequired() = runTest {
        coEvery { api.getReleases(any(), any(), 100, 1) } returns Response.success(
            listOf(release("5.0.0", abi = "arm64-v8a")),
            Headers.headersOf("Link", "<https://api.github.com/next>; rel=\"next\""))
        coEvery { api.getReleases(any(), any(), 100, 2) } returns Response.success(listOf(release("2.0.0")))
        assertEquals("v2.0.0", repository.getLatestUpdate(UpdateChannel.BETA).getOrThrow().tag)
    }

    @Test fun cancellationIsNotConvertedIntoAReleaseResult() = runTest {
        coEvery { api.getReleases(any(), any(), any(), any()) } throws CancellationException()
        try {
            repository.getLatestUpdate(UpdateChannel.BETA)
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
    }
}
