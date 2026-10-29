package com.mercadomania.game.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.mercadomania.game.BuildConfig
import com.mercadomania.game.integration.OfferWebViewClient
import com.mercadomania.game.integration.UrlSchemes
import com.mercadomania.game.ui.components.BazaarBackdrop

/**
 * The Black surface: the offer, rendered in a full-screen WebView.
 *
 * The defaults are wrong for an offer, so everything it needs is set up here: JS and DOM
 * storage, third-party cookies (blocked by default — pixels and iframe checkouts need
 * them), a cookie flush on pause so the click cookie survives to the next launch, a file
 * chooser (document upload), a download handler, and a retry screen (the default WebView
 * error page gives no way back).
 *
 * The verdict/pinning behaviour lives in [OfferWebViewClient]; this screen only wires it
 * up from the callbacks the root gate passes in.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OfferWebViewScreen(
    url: String,
    fromCache: Boolean = false,
    decideBranch: Boolean = true,
    onWhiteLocked: () -> Unit = {},
    onCacheDead: () -> Unit = {},
    onFreshUnavailable: () -> Unit = {},
    onPainted: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Holds the live WebView so back navigation can consult its history.
    val webViewRef = remember { arrayOfNulls<WebView>(1) }
    // Load-once flag keyed on the URL. Comparing against webView.url instead would drag the
    // user back to the landing page on every recomposition after they navigate.
    val loaded = remember(url) { booleanArrayOf(false) }

    var showRetry by remember(url) { mutableStateOf(false) }

    // The outstanding file-chooser callback. It MUST be answered exactly once in every
    // outcome, or the page's file input dies for good.
    val fileCallback = remember { arrayOfNulls<ValueCallback<Array<Uri>>>(1) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = fileCallback[0]
        fileCallback[0] = null
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        callback?.onReceiveValue(uris)
    }

    // WebView flushes cookies lazily; without this the click cookie and session do not
    // survive to the next launch and the user returns as a brand-new visitor.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                runCatching { CookieManager.getInstance().flush() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler {
        val webView = webViewRef[0]
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            // Without this the user is trapped: there is no White UI behind the offer.
            activity?.finish()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                with(settings) {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    @Suppress("DEPRECATION")
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    javaScriptCanOpenWindowsAutomatically = true
                    // Offer pages routinely pull assets over http from an https shell.
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                // Third-party cookies are blocked by default; pixels, iframe checkouts and
                // partner redirects rely on them.
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webChromeClient = object : WebChromeClient() {
                    override fun onShowFileChooser(
                        webView: WebView?,
                        callback: ValueCallback<Array<Uri>>?,
                        params: FileChooserParams?,
                    ): Boolean {
                        // Answer any previous outstanding callback before replacing it.
                        fileCallback[0]?.onReceiveValue(null)
                        fileCallback[0] = callback
                        val intent = params?.createIntent()
                        return try {
                            if (intent != null) {
                                fileChooserLauncher.launch(intent)
                                true
                            } else {
                                fileCallback[0] = null
                                false
                            }
                        } catch (e: ActivityNotFoundException) {
                            fileCallback[0]?.onReceiveValue(null)
                            fileCallback[0] = null
                            false
                        }
                    }
                }

                webViewClient = OfferWebViewClient(
                    context = ctx,
                    fromCache = fromCache,
                    decideBranch = decideBranch,
                    onWhiteLocked = onWhiteLocked,
                    onCacheDead = onCacheDead,
                    onFreshUnavailable = onFreshUnavailable,
                    onPainted = onPainted,
                    onErrorAfterPaint = { showRetry = true },
                )

                // WebView cannot download on its own; hand http/https downloads to the
                // system. blob:/data: mean nothing outside the WebView that made them.
                setDownloadListener { downloadUrl, _, _, _, _ ->
                    val scheme = UrlSchemes.schemeOf(downloadUrl)
                    if (scheme == "http" || scheme == "https") {
                        runCatching {
                            ctx.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }

                // Since WebView M108 the X-Requested-With header is dropped by default. The
                // official way back is an origin allow-list, set BEFORE the first load.
                // "*" covers every domain in the redirect chain and every subresource.
                runCatching {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, setOf("*"))
                    }
                }.onFailure {
                    Log.w("OfferWebViewScreen", "X-Requested-With allow-list not applied", it)
                }

                webViewRef[0] = this
            }
        },
        update = { webView ->
            webViewRef[0] = webView
            if (!loaded[0]) {
                loaded[0] = true
                // Fallback header for the main frame (does not survive redirects/subframes;
                // the allow-list above is what covers those).
                webView.loadUrl(url, mapOf("X-Requested-With" to BuildConfig.APPLICATION_ID))
            }
        },
        onRelease = { webView ->
            // Offer pages are ad-heavy: without this the Activity leaks on every
            // configuration change and JS timers and media keep running.
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
            webViewRef[0] = null
            // Answer any dangling file-chooser callback so a torn-down page leaves nothing
            // waiting.
            fileCallback[0]?.onReceiveValue(null)
            fileCallback[0] = null
        }
    )

    if (showRetry) {
        OfferErrorOverlay(
            onRetry = {
                showRetry = false
                webViewRef[0]?.reload()
            }
        )
    }
}

@Composable
private fun OfferErrorOverlay(onRetry: () -> Unit) {
    BazaarBackdrop {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "Connection problem")
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text(text = "Retry")
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
