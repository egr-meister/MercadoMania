package com.mercadomania.game.integration

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Keeps offer navigation inside the WebView, and hands anything that is not a web page -
 * payment apps, messengers, `intent://` links - to the system.
 */
class OfferWebViewClient(private val context: Context) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        return handleUrl(url)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        // Remember where the user actually is, so a relaunch resumes the offer flow
        // instead of restarting it from the landing page.
        //
        // Only real pages qualify. WebView fires onPageStarted with about:blank routinely
        // (window.open, a page clearing itself, some error paths), and since the routing
        // decision is sticky, caching that would pin the app to a blank screen for good.
        val scheme = url?.let { UrlSchemes.schemeOf(it) }
        if (scheme == "http" || scheme == "https") {
            IntegrationStorage.cachedOfferUrl = url
        }
    }

    private fun handleUrl(url: String): Boolean {
        if (!UrlSchemes.isExternal(url)) return false
        return try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (e: ActivityNotFoundException) {
            // Nothing on the device handles this scheme. Swallowing it keeps the offer
            // usable; letting the WebView try would show an ugly error page instead.
            Log.w("OfferWebViewClient", "No activity for $url", e)
            true
        }
    }
}
