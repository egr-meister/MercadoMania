package com.mercadomania.game.integration

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Takes the White/Black verdict from the main-frame response — there is no HTTP probe.
 *
 * The verdict window is open only until the first page actually paints:
 *  - a real 404 on a freshly built campaign URL → White, locked forever;
 *  - any error on a CACHED address → the address is dead: reset the cache and rebuild the
 *    campaign link (the branch stays Black);
 *  - a network error on a fresh URL → not an answer: White for this launch only, nothing
 *    pinned, the next launch asks again;
 *  - any other HTTP status on a fresh URL (200, 302 resolved, 403, 500) → Black; the
 *    WebView renders whatever came back.
 *
 * Once a page has painted the window is closed: a 404 deep inside the offer funnel must
 * never be read as "the tracker said White" and throw the user out — irreversibly. An
 * error after paint instead offers a retry ([onErrorAfterPaint]).
 *
 * With [decideBranch] = false (a push return visit) no verdict is taken and nothing is
 * pinned: a push is a return, not a new reason to ask the tracker about the branch.
 *
 * @param fromCache whether this WebView is showing a cached address or a fresh campaign URL
 * @param decideBranch whether this load may change the branch / pin the address
 * @param onWhiteLocked fresh 404: lock White and show the game
 * @param onCacheDead cached address failed: clear the cache and re-route (rebuild the link)
 * @param onFreshUnavailable fresh URL failed without a 404 verdict: White for this launch
 * @param onPainted the first real page committed: pin its address
 * @param onErrorAfterPaint a main-frame error after the first paint: offer a retry
 */
class OfferWebViewClient(
    private val context: Context,
    private val fromCache: Boolean = false,
    private val decideBranch: Boolean = true,
    private val onWhiteLocked: () -> Unit = {},
    private val onCacheDead: () -> Unit = {},
    private val onFreshUnavailable: () -> Unit = {},
    private val onPainted: (String) -> Unit = {},
    private val onErrorAfterPaint: () -> Unit = {},
) : WebViewClient() {

    private var painted = false
    private var decided = false

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        return handleExternal(url)
    }

    override fun onPageCommitVisible(view: WebView?, url: String?) {
        super.onPageCommitVisible(view, url)
        val scheme = url?.let { UrlSchemes.schemeOf(it) }
        if (scheme != "http" && scheme != "https") return
        if (painted) return
        painted = true
        // The first really rendered page. Caching on every onPageStarted would fill the
        // cache with about:blank and intermediate redirects; the decision is sticky, so
        // that would pin the install to a blank screen.
        if (decideBranch) onPainted(url!!)
    }

    override fun onReceivedHttpError(
        view: WebView?,
        request: WebResourceRequest?,
        errorResponse: WebResourceResponse?,
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (request?.isForMainFrame != true) return
        if (painted) {
            // Deep inside the funnel — never a verdict. Offer a retry instead.
            if (decideBranch) onErrorAfterPaint()
            return
        }
        if (!decideBranch || decided) return
        val status = errorResponse?.statusCode ?: return

        when {
            // Dead cached address (any HTTP error, 404 included): not a tracker verdict,
            // just a page that died. Reset the cache and rebuild; the branch stays Black.
            fromCache -> {
                decided = true
                Log.i(TAG, "Cached offer address returned HTTP $status — rebuilding")
                onCacheDead()
            }
            // Real 404 on a fresh campaign URL: the tracker says "this install gets the app".
            RoutingRules.isWhite(status) -> {
                decided = true
                Log.i(TAG, "Tracker answered 404 — locking White")
                onWhiteLocked()
            }
            // Any other status on a fresh URL (403, 5xx…): the offer exists, let it render.
            else -> Unit
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?,
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame != true) return
        val url = request.url?.toString().orEmpty()
        // Offers redirect to market:// and intent:// constantly; those are handled in
        // shouldOverrideUrlLoading, and an error screen over a working flow is a bug.
        if (url == "about:blank") return
        if (error?.errorCode == ERROR_UNSUPPORTED_SCHEME) return
        if (painted) {
            if (decideBranch) onErrorAfterPaint()
            return
        }
        if (!decideBranch || decided) return

        decided = true
        if (fromCache) {
            // The pinned address will not open at all. Reset and rebuild, Black stays Black.
            Log.i(TAG, "Cached offer address failed to load — rebuilding")
            onCacheDead()
        } else {
            // DNS/TLS/no connection on a fresh URL is NOT an answer: show White this launch
            // but pin nothing, so the next launch asks the tracker again.
            Log.i(TAG, "Network error on a fresh offer URL — White for this launch only")
            onFreshUnavailable()
        }
    }

    /** External schemes (market://, intent://, tel:, mailto:…) go to the system. */
    private fun handleExternal(url: String): Boolean {
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
            Log.w(TAG, "No activity for $url", e)
            true
        }
    }

    private companion object {
        const val TAG = "OfferWebViewClient"
    }
}
