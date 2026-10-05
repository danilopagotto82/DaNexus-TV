package com.nuvio.tv.updater
import com.nuvio.tv.BuildConfig
import com.nuvio.tv.data.remote.api.GitHubReleaseApi
import com.nuvio.tv.data.remote.dto.GitHubReleaseDto
import com.nuvio.tv.updater.model.AppUpdate
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

internal class NoEligibleUpdateException(channel: UpdateChannel) :
    IllegalStateException("No compatible DaNexus APK release found for ${channel.storedValue} channel")
@Singleton
class UpdateRepository @Inject constructor(private val gitHubReleaseApi: GitHubReleaseApi) {
    suspend fun getLatestUpdate(channel: UpdateChannel): Result<AppUpdate> = try {
        // /latest may refer to the other edition in the shared repository.
        val releases = mutableListOf<GitHubReleaseDto>()
        var page = 1
        do {
            val response = gitHubReleaseApi.getReleases(
                BuildConfig.GITHUB_OWNER, BuildConfig.GITHUB_REPO, page = page++)
            check(response.isSuccessful) { "GitHub API error: ${response.code()}" }
            releases.addAll(response.body() ?: error("Empty GitHub release response"))
        } while (response.headers()["Link"].orEmpty().contains("rel=\"next\""))
        val (release, asset) = ReleaseSelector.eligibleReleases(releases, channel)
            .firstNotNullOfOrNull { release ->
                DanexusUpdateAssets.choose(release, "DaNexus-FireTV-", "armeabi-v7a")
                    ?.let { release to it }
            } ?: throw NoEligibleUpdateException(channel)
        val tag = release.tagName!!
        Result.success(AppUpdate(tag = tag, title = release.name?.takeIf(String::isNotBlank) ?: tag,
            notes = release.body.orEmpty(), releaseUrl = release.htmlUrl,
            assetName = asset.name, assetUrl = asset.browserDownloadUrl, assetSizeBytes = asset.size))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
