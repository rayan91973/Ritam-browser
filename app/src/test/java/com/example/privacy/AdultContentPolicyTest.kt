package com.example.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AdultContentPolicyTest {

    @Test
    fun `blocked adult domains are rejected`() {
        assertNotNull(AdultContentPolicy.checkUrl("https://www.pornhub.com/view_video.php?viewkey=1"))
        assertNotNull(AdultContentPolicy.checkInput("xhamster.com"))
        assertNotNull(AdultContentPolicy.checkInput("https://m.xvideos.com/video123"))
        assertEquals(true, AdultContentPolicy.isBlockedHost("cdn.pornhub.com"))
    }

    @Test
    fun `explicit typed queries are rejected`() {
        assertNotNull(AdultContentPolicy.checkInput("porn"))
        assertNotNull(AdultContentPolicy.checkInput("free porn videos"))
        assertNotNull(AdultContentPolicy.checkInput("nude girls"))
        assertNotNull(AdultContentPolicy.checkInput("hentai"))
        assertNotNull(AdultContentPolicy.checkInput("naked pictures"))
        assertNotNull(AdultContentPolicy.checkInput("sex videos"))
    }

    @Test
    fun `ordinary searches and safe phrases are allowed`() {
        assertNull(AdultContentPolicy.checkInput("kotlin coroutines tutorial"))
        assertNull(AdultContentPolicy.checkInput("weather in essex"))
        assertNull(AdultContentPolicy.checkInput("sussex county council"))
        assertNull(AdultContentPolicy.checkInput("unisex baby names"))
        assertNull(AdultContentPolicy.checkInput("sex education curriculum"))
        assertNull(AdultContentPolicy.checkInput("sexual health clinic"))
        assertNull(AdultContentPolicy.checkInput("naked mole rat habitat"))
    }

    @Test
    fun `normal websites are allowed`() {
        assertNull(AdultContentPolicy.checkUrl("https://www.youtube.com/watch?v=abc123"))
        assertNull(AdultContentPolicy.checkUrl("https://en.wikipedia.org/wiki/Architecture"))
        assertNull(AdultContentPolicy.checkUrl("https://news.example.com/article/technology"))
        assertNull(AdultContentPolicy.checkUrl("ritam://sanctuary/home"))
    }

    @Test
    fun `search engine result urls are judged by their query`() {
        assertNotNull(AdultContentPolicy.checkInput("https://www.google.com/search?q=porn"))
        assertNull(AdultContentPolicy.checkInput("https://duckduckgo.com/?q=android+webview+ads"))
    }
}
