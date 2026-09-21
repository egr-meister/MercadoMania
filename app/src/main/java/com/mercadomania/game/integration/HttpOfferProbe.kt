package com.mercadomania.game.integration

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.WebSettings
import com.mercadomania.game.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Probes the offer URL with a real WebView User-Agent and reports the final status and
 * URL after following redirects by hand.
 */
class HttpOfferProbe(
    private val timeoutMillis: Int = 15_000,
    private val maxRedirects: Int = 10
) : OfferProbe {

    override fun probe(url: String): ProbeResult {
        var current = url
        var redirects = 0

        while (true) {
            val connection = (URL(current).openConnection() as HttpURLConnection)
            connection.requestMethod = "GET"
            // Redirects are followed by hand so the final URL can be reported back and
            // handed to the WebView - HttpURLConnection would not tell us where it ended.
            connection.instanceFollowRedirects = false
            connection.connectTimeout = timeoutMillis
            connection.readTimeout = timeoutMillis
            // Offer servers answer 404 to "Java/1.8.0". That 404 would pin White forever,
            // so the probe must look exactly like the WebView that follows it.
            connection.setRequestProperty("User-Agent", UserAgentProvider.userAgent)
            // THIS is the request the tracker registers as the click - it, not the
            // WebView, is what traverses the redirect chain to the Keitaro campaign link.
            // So the package-name header that marks the traffic as in-app has to ride
            // here, on every hop, or the bundle never reaches the tracker. Set on each
            // connection in the loop because Keitaro logs the click on the first hit to
            // the campaign URL, before any redirect.
            connection.setRequestProperty("X-Requested-With", BuildConfig.APPLICATION_ID)
            connection.setRequestProperty(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
            )
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            if (connection is HttpsURLConnection) {
                relaxTls(connection)
            }

            try {
                val code = connection.responseCode
                if (code in 300..399 && redirects < maxRedirects) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        // Offer servers redirect to market:// or intent:// often enough
                        // that this must not throw. Such a target IS the answer - hand it
                        // back so the decision comes out Black and OfferWebViewClient can
                        // pass it to the system.
                        val resolved = resolve(current, location)
                            ?: return ProbeResult(code, location)
                        // Location is often relative; resolve against the current URL.
                        current = resolved
                        redirects++
                        continue
                    }
                }
                return ProbeResult(code, current)
            } finally {
                connection.disconnect()
            }
        }
    }

    /** Returns null when the target is not something HttpURLConnection can open. */
    private fun resolve(current: String, location: String): String? = runCatching {
        val url = URL(URL(current), location)
        if (url.protocol.equals("http", true) || url.protocol.equals("https", true)) {
            url.toString()
        } else {
            null
        }
    }.getOrNull()

    /**
     * Accepts any certificate FOR THIS ONE CONNECTION.
     *
     * Offer domains frequently ship an incomplete fullchain, which would fail the probe
     * and pin the app to White for good. Scoping the relaxed factory to this instance
     * keeps the WebView - and every other connection in the app - on standard validation.
     * Never call setDefaultSSLSocketFactory here. The real fix is the server's chain.
     */
    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager", "BadHostnameVerifier")
    private fun relaxTls(connection: HttpsURLConnection) {
        runCatching {
            val trustManager = object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) = Unit
                override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) = Unit
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }
            val context = SSLContext.getInstance("TLS")
            context.init(null, arrayOf(trustManager), SecureRandom())
            connection.sslSocketFactory = context.socketFactory
            connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
        }.onFailure { Log.w("HttpOfferProbe", "Could not relax TLS for probe", it) }
    }
}

/**
 * Caches the WebView User-Agent at startup.
 *
 * WebSettings.getDefaultUserAgent() initialises the WebView provider, which is slow and
 * wants a real Context - not something the probe thread should do on every launch.
 */
object UserAgentProvider {

    private const val FALLBACK =
        "Mozilla/5.0 (Linux; Android 13; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"

    @Volatile
    private var cached: String? = null

    fun init(context: Context) {
        if (cached != null) return
        cached = runCatching { WebSettings.getDefaultUserAgent(context) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

    val userAgent: String get() = cached ?: FALLBACK
}
