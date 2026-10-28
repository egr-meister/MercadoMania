package com.mercadomania.game.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.mercadomania.game.R
import com.mercadomania.game.integration.AdjustManager
import com.mercadomania.game.integration.DeepLinkRouter
import com.mercadomania.game.integration.IntegrationStorage
import com.mercadomania.game.integration.RouteDecision
import com.mercadomania.game.integration.TrafficRouter
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.nav.MercadoNavGraph
import com.mercadomania.game.ui.screens.OfferWebViewScreen
import com.mercadomania.game.ui.theme.BazaarPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val ATTRIBUTION_TIMEOUT_MS = 8_000L

/**
 * The root orchestrator. It decides, once per launch, whether this install sees the game
 * (White) or the offer (Black), and it lets a push deep link override that decision while
 * the branch is still Black.
 *
 * This is the only integration touch point inside the UI: the game itself
 * ([MercadoNavGraph]) is rendered unchanged as the White branch. There is no HTTP probe —
 * the WebView opens the campaign link itself and [com.mercadomania.game.integration.OfferWebViewClient]
 * turns the main-frame response into the verdict.
 */
@Composable
fun MercadoManiaRoot() {
    var deepLinkUrl by remember { mutableStateOf<String?>(null) }
    var routeGate by remember { mutableStateOf<RouteDecision?>(null) }
    var painted by remember { mutableStateOf(false) }
    var reroutes by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        DeepLinkRouter.setHandler { url -> deepLinkUrl = url }
        onDispose { DeepLinkRouter.clearHandler() }
    }

    LaunchedEffect(reroutes) {
        routeGate = null
        painted = false
        // On a first launch the offer link is worthless until attribution lands, so wait —
        // but bounded, because Adjust may never answer and a splash that never ends is worse
        // than an unattributed offer. On a rebuild (dead cache) the branch is already known,
        // so no wait.
        if (!withContext(Dispatchers.IO) { IntegrationStorage.hasRouteDecision() }) {
            withTimeoutOrNull(ATTRIBUTION_TIMEOUT_MS + 2_000L) {
                suspendCancellableCoroutine { continuation ->
                    AdjustManager.awaitAttribution(ATTRIBUTION_TIMEOUT_MS) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                }
            }
        }
        TrafficRouter.route { decision -> routeGate = decision }
    }

    val currentDeepLink = deepLinkUrl
    when {
        // A push is a return visit, not a new reason to ask the tracker: it never changes
        // the branch, and a White-locked device stays White.
        currentDeepLink != null && !IntegrationStorage.isWhiteLocked ->
            OfferWebViewScreen(url = currentDeepLink, decideBranch = false)

        routeGate is RouteDecision.Black -> {
            val black = routeGate as RouteDecision.Black
            Box(Modifier.fillMaxSize()) {
                // Keyed on the address AND its source: a rebuild after a dead cache is a
                // different load with a different error meaning, so it needs its own WebView.
                key(black.url, black.fromCache) {
                    OfferWebViewScreen(
                        url = black.url,
                        fromCache = black.fromCache,
                        decideBranch = true,
                        onWhiteLocked = {
                            IntegrationStorage.lockWhite()
                            routeGate = RouteDecision.White
                        },
                        onCacheDead = {
                            IntegrationStorage.clearOfferUrl()
                            reroutes++
                        },
                        onFreshUnavailable = { routeGate = RouteDecision.White },
                        onPainted = { url ->
                            IntegrationStorage.saveOfferUrl(url)
                            painted = true
                        },
                    )
                }
                // Until the offer actually paints, keep the splash: a White install must not
                // see a flash of the offer screen on its way to a 404.
                if (!painted) IntegrationSplash()
            }
        }

        routeGate is RouteDecision.White -> WhiteContent()

        else -> IntegrationSplash()
    }
}

/** The game itself, exactly as [com.mercadomania.game.MainActivity] rendered it. */
@Composable
private fun WhiteContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BazaarPalette.Midnight)
    ) {
        MercadoNavGraph()
    }
}

/**
 * The wait-for-routing splash. Deliberately matches the game's own loading look - the
 * bazaar backdrop and the wordmark - so the hand-off to White is seamless when the
 * decision is instant.
 */
@Composable
private fun IntegrationSplash() {
    BazaarBackdrop {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            val screenWidth = maxWidth
            Image(
                painter = painterResource(Assets.logo),
                contentDescription = stringResource(R.string.cd_logo),
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(screenWidth * 0.72f)
                    .clearAndSetSemantics { }
            )
        }
    }
}
