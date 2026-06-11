package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun testParseVlessLink_valid() {
        // vless://secret@host:port?sni=google.com#Name
        val link = "vless://8a2a078d-1943-42e1-85b4-ec8fca6c0e86@142.250.74.46:443?security=reality&sni=google.com#Tehran-Reality"
        val state = parseConnectionLink(link, isFa = false)
        
        assertTrue(state is LinkValidationState.Valid)
        val valid = state as LinkValidationState.Valid
        assertEquals("Tehran-Reality", valid.name)
        assertEquals("142.250.74.46", valid.host)
        assertEquals(443, valid.port)
        assertEquals("VLESS (Reality)", valid.protocol)
        assertEquals("8a2a078d-1943-42e1-85b4-ec8fca6c0e86", valid.secret)
        assertEquals("google.com", valid.sni)
    }

    @Test
    fun testParseShadowsocksLink_valid() {
        // Base64 encoded "aes-128-gcm:password" is "YWVzLTEyOC1nY206cGFzc3dvcmQ="
        val link = "ss://YWVzLTEyOC1nY206cGFzc3dvcmQ=@50.116.50.31:1080#US-Shadowsocks"
        val state = parseConnectionLink(link, isFa = false)

        assertTrue(state is LinkValidationState.Valid)
        val valid = state as LinkValidationState.Valid
        assertEquals("US-Shadowsocks", valid.name)
        assertEquals("50.116.50.31", valid.host)
        assertEquals(1080, valid.port)
        assertEquals("ShadowSocks", valid.protocol)
        assertEquals("aes-128-gcm:password", valid.secret)
    }

    @Test
    fun testParseTrojanLink_valid() {
        val link = "trojan://mypass123@192.168.1.100:443?sni=wikipedia.org#MyTrojan"
        val state = parseConnectionLink(link, isFa = false)

        assertTrue(state is LinkValidationState.Valid)
        val valid = state as LinkValidationState.Valid
        assertEquals("MyTrojan", valid.name)
        assertEquals("192.168.1.100", valid.host)
        assertEquals(443, valid.port)
        assertEquals("Trojan", valid.protocol)
        assertEquals("mypass123", valid.secret)
        assertEquals("wikipedia.org", valid.sni)
    }

    @Test
    fun testParseLink_invalidScheme() {
        val link = "http://google.com"
        val state = parseConnectionLink(link, isFa = false)
        assertTrue(state is LinkValidationState.Invalid)
    }

    @Test
    fun testParseLink_empty() {
        val state = parseConnectionLink("", isFa = false)
        assertTrue(state is LinkValidationState.Empty)
    }
}
