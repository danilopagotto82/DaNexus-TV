package com.nuvio.tv.updater
import com.nuvio.tv.data.remote.dto.GitHubAssetDto
import com.nuvio.tv.data.remote.dto.GitHubReleaseDto
import org.junit.Assert.*
import org.junit.Test

class DanexusUpdateAssetsTest {
    private val tag = "v1.1.0-beta.4-danexus.10"
    private fun release(prefix: String = "DaNexus-FireTV-", abi: String = "armeabi-v7a"): GitHubReleaseDto {
        val name = prefix + tag.removePrefix("v") + "-$abi.apk"
        return GitHubReleaseDto(tagName = tag, assets = listOf(GitHubAssetDto(name,
            "https://github.com/danilopagotto82/DaNexus-TV/releases/download/$tag/$name", 42L)))
    }
    @Test fun editionAndArchitectureCannotBeMixed() {
        assertNotNull(DanexusUpdateAssets.choose(release(), "DaNexus-FireTV-", "armeabi-v7a"))
        assertNull(DanexusUpdateAssets.choose(release(), "DaNexus-Shield-", "arm64-v8a"))
        assertNull(DanexusUpdateAssets.choose(release(abi = "arm64-v8a"), "DaNexus-FireTV-", "armeabi-v7a"))
        assertNotNull(DanexusUpdateAssets.choose(release("DaNexus-Shield-", "arm64-v8a"), "DaNexus-Shield-", "arm64-v8a"))
    }
    @Test fun assetMustMatchVersionAndTrustedUrl() {
        val release = release()
        for (asset in listOf(release.assets.single().copy(browserDownloadUrl = "https://example.com/update.apk"),
            release.assets.single().copy(name = "DaNexus-FireTV-1.0.0-armeabi-v7a.apk"),
            release.assets.single().copy(size = 0L)))
            assertNull(DanexusUpdateAssets.choose(release.copy(assets = listOf(asset)), "DaNexus-FireTV-", "armeabi-v7a"))
    }
    @Test fun summaryHidesMetadataAndKeepsChanges() {
        assertEquals("Arte PT-BR • MDBList configurado", DanexusUpdateAssets.notesSummary(
            "# Novidades\n\n- Arte PT-BR\n- MDBList configurado\n<!-- nuvio-fork-version-code: 1461 -->"))
        assertEquals("", DanexusUpdateAssets.notesSummary(""))
    }
}
