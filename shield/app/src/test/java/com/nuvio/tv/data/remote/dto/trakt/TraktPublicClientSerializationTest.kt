package com.nuvio.tv.data.remote.dto.trakt

import com.squareup.moshi.Moshi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TraktPublicClientSerializationTest {
    private val moshi = Moshi.Builder().build()

    @Test fun publicDeviceTokenRequestDoesNotSendSecret() {
        val json = moshi.adapter(TraktDeviceTokenRequestDto::class.java)
            .toJson(TraktDeviceTokenRequestDto(code = "device-code", clientId = "public-client"))
        assertTrue(json.contains("\"client_id\":\"public-client\""))
        assertFalse(json.contains("client_secret"))
    }

    @Test fun publicRefreshRequestDoesNotSendSecret() {
        val json = moshi.adapter(TraktRefreshTokenRequestDto::class.java)
            .toJson(TraktRefreshTokenRequestDto(refreshToken = "refresh-token", clientId = "public-client",
                redirectUri = "https://example.test/"))
        assertFalse(json.contains("client_secret"))
        assertTrue(json.contains("refresh_token"))
    }

    @Test fun publicRevokeRequestDoesNotSendSecret() {
        val json = moshi.adapter(TraktRevokeRequestDto::class.java)
            .toJson(TraktRevokeRequestDto(token = "access-token", clientId = "public-client"))
        assertFalse(json.contains("client_secret"))
    }

    @Test fun existingConfidentialClientRemainsCompatible() {
        val json = moshi.adapter(TraktDeviceTokenRequestDto::class.java)
            .toJson(TraktDeviceTokenRequestDto(code = "device-code", clientId = "legacy-client",
                clientSecret = "test-secret"))
        assertTrue(json.contains("\"client_secret\":\"test-secret\""))
    }
}
