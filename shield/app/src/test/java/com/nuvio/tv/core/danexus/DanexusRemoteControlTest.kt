package com.nuvio.tv.core.danexus

import fi.iki.elonen.NanoHTTPD
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DanexusRemoteControlTest {
    private fun request(
        pin: String? = "123456", command: String = "ok", ip: String = "192.168.0.8",
        origin: String? = "http://192.168.0.10:8790",
    ): NanoHTTPD.IHTTPSession = mockk {
        every { remoteIpAddress } returns ip
        every { method } returns NanoHTTPD.Method.POST
        every { uri } returns "/command"
        every { headers } returns buildMap {
            put("host", "192.168.0.10:8790")
            pin?.let { put("x-danexus-pin", it) }
            origin?.let { put("origin", it) }
        }
        every { parameters } returns mapOf("key" to listOf(command))
    }

    @Test fun pairedLocalPhoneDispatchesExactlyTheRequestedCommand() = runTest {
        val received = async(start = CoroutineStart.UNDISPATCHED) { DanexusRemoteControl.commands.first() }
        val response = DanexusRemoteControl.RemoteServer("123456").serve(request(command = "sources"))
        assertEquals(NanoHTTPD.Response.Status.OK, response.status)
        assertEquals("sources", received.await())
    }

    @Test fun unpairedPhoneCannotSendCommands() {
        assertEquals(NanoHTTPD.Response.Status.FORBIDDEN,
            DanexusRemoteControl.RemoteServer("123456").serve(request(pin = null)).status)
    }

    @Test fun externalClientCannotSendCommandsEvenWithCorrectCode() {
        assertEquals(NanoHTTPD.Response.Status.FORBIDDEN,
            DanexusRemoteControl.RemoteServer("123456").serve(request(ip = "203.0.113.1")).status)
    }

    @Test fun pageFromAnotherOriginCannotSendCommands() {
        assertEquals(NanoHTTPD.Response.Status.FORBIDDEN,
            DanexusRemoteControl.RemoteServer("123456").serve(request(origin = "https://example.org")).status)
    }

    @Test fun invalidCommandIsRejected() {
        assertEquals(NanoHTTPD.Response.Status.BAD_REQUEST,
            DanexusRemoteControl.RemoteServer("123456").serve(request(command = "install")).status)
    }

    @Test fun repeatedIncorrectCodesTemporarilyBlockTheClient() {
        val server = DanexusRemoteControl.RemoteServer("123456")
        repeat(5) { assertEquals(NanoHTTPD.Response.Status.FORBIDDEN, server.serve(request(pin = "000000")).status) }
        assertEquals(NanoHTTPD.Response.Status.FORBIDDEN, server.serve(request()).status)
    }
}
