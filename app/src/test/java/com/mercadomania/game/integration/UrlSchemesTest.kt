package com.mercadomania.game.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlSchemesTest {

    @Test
    fun `allowed schemes stay inside the webview`() {
        for (url in listOf(
            "https://domain.com/x",
            "http://domain.com/x",
            "about:blank",
            "data:text/html,<h1>hi</h1>",
            "javascript:void(0)",
            "blob:https://domain.com/uuid",
            "file:///android_asset/x.html"
        )) {
            assertFalse(url, UrlSchemes.isExternal(url))
        }
    }

    @Test
    fun `unknown schemes are external`() {
        for (url in listOf(
            "intent://scan/#Intent;scheme=zxing;end",
            "market://details?id=com.x",
            "tel:+123456",
            "mailto:a@b.com",
            "whatsapp://send?text=hi",
            "tg://resolve?domain=x"
        )) {
            assertTrue(url, UrlSchemes.isExternal(url))
        }
    }

    @Test
    fun `relative urls have no scheme and stay internal`() {
        assertFalse(UrlSchemes.isExternal("/path/to/page"))
        assertFalse(UrlSchemes.isExternal("page.html?x=1"))
        // A path segment containing a colon is not a scheme.
        assertFalse(UrlSchemes.isExternal("/path:with:colons"))
    }

    @Test
    fun `scheme detection follows rfc 3986`() {
        assertEquals("https", UrlSchemes.schemeOf("HTTPS://x"))
        assertEquals("intent", UrlSchemes.schemeOf("intent://x"))
        assertNull(UrlSchemes.schemeOf("/no/scheme"))
        // Must start with a letter.
        assertNull(UrlSchemes.schemeOf("2go:x"))
    }
}
