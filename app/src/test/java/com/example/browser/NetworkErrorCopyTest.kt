package com.example.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkErrorCopyTest {

    @Test
    fun `offline errors explain the connection`() {
        val text = NetworkErrorCopy.describe("net::ERR_INTERNET_DISCONNECTED")
        assertTrue(text.contains("offline", ignoreCase = true))
        assertTrue(text.contains("ERR_INTERNET_DISCONNECTED"))
    }

    @Test
    fun `dns errors are actionable`() {
        val text = NetworkErrorCopy.describe("net::ERR_NAME_NOT_RESOLVED")
        assertTrue(text.contains("couldn't be found"))
        assertTrue(text.contains("ERR_NAME_NOT_RESOLVED"))
    }

    @Test
    fun `unknown codes still get a readable sentence`() {
        val text = NetworkErrorCopy.describe("net::ERR_SOMETHING_NEW")
        assertTrue(text.contains("Unable to reach this page"))
        assertFalse(text.startsWith("net::"))
    }

    @Test
    fun `blank description falls back without leaking a code`() {
        val text = NetworkErrorCopy.describe(null)
        assertTrue(text.contains("Unable to reach"))
        assertFalse(text.contains("ERR_"))
    }
}
