package com.nuvio.tv.ui.screens.player.smartsource

import com.nuvio.tv.ui.screens.player.smartOpaqueKey
import com.nuvio.tv.ui.screens.player.smartSafeLabel
import com.nuvio.tv.ui.screens.player.validBackendBaseUrl
import org.junit.Assert.*
import org.junit.Test

class SmartSourcePrivacyTest {
    @Test fun onlyTrustedConfiguredBackendOriginsAccepted() {
        assertEquals("http://127.0.0.1:8787", validBackendBaseUrl("http://127.0.0.1:8787/"))
        assertEquals("http://192.168.1.4:8787", validBackendBaseUrl("http://192.168.1.4:8787"))
        assertNull(validBackendBaseUrl("http://192.168.evil.example:8787"))
        assertNull(validBackendBaseUrl("http://192.168.1.4.evil.example:8787"))
        assertNull(validBackendBaseUrl("http://172.99.1.1"))
        assertNull(validBackendBaseUrl("http://public.example"))
        assertNull(validBackendBaseUrl("https://user:secret@backend.test"))
        assertNull(validBackendBaseUrl("https://backend.test/?token=secret"))
    }
    @Test fun privateUrlAndHeadersOnlyAffectOpaqueIdentity() {
        val secret = "https://cdn.test/private?token=secret\u0000authorization:Bearer private"
        val key = smartOpaqueKey(secret)
        assertEquals(64, key.length)
        assertFalse(key.contains("secret"))
        assertNotEquals(key, smartOpaqueKey(secret + "different-header"))
    }
    @Test fun labelsStripUrlsAndControlCharactersBeforePersistence() {
        assertEquals("Labirinto", smartSafeLabel("Labirinto https://private.test/token?secret=1\n"))
        assertEquals(120, smartSafeLabel("a".repeat(500)).length)
    }
}
