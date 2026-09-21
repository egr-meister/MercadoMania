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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.mercadomania.game.R
import com.mercadomania.game.integration.AppsFlyerManager
import com.mercadomania.game.integration.DeepLinkRouter
import com.mercadomania.game.integration.IntegrationStorage
import com.mercadomania.game.integration.RouteDecision
import com.mercadomania.game.integration.TrafficRouter
import com.mercadomania.game.ui.components.BazaarBackdrop
import com.mercadomania.game.ui.nav.MercadoNavGraph
import com.mercadomania.game.ui.screens.OfferWebViewScreen
import com.mercadomania.game.ui.theme.BazaarPalette
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val ATTRIBUTION_TIMEOUT_MS = 8_000L

/**
 * The root orchestrator. It decides, once per launch, whether this install sees the game
 * (White) or the offer (Black), and it lets a push deep link override that decision.
 *
 * This is the only integration touch point inside the UI: the game itself
 * ([MercadoNavGraph]) is rendered unchanged as the White branch.
 */
@Composable
fun MercadoManiaRoot() {
    var deepLinkUrl by remember { mutableStateOf<String?>(null) }
    var routeGate by remember { mutableStateOf<RouteDecision?>(null) }

    DisposableEffect(Unit) {
        DeepLinkRouter.setHandler { url -> deepLinkUrl = url }
        onDispose { DeepLinkRouter.clearHandler() }
    }

    LaunchedEffect(Unit) {
        // On a first launch the offer URL is worthless until conversion data lands, so
        // wait - but bounded, because AppsFlyer may never answer and a splash that never
        // ends is worse than an unattributed offer.
        if (!IntegrationStorage.hasRouteDecision) {
            withTimeoutOrNull(ATTRIBUTION_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    AppsFlyerManager.setOnAttributionResolved {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    continuation.invokeOnCancellation {
                        AppsFlyerManager.setOnAttributionResolved(null)
                    }
                }
            }
            AppsFlyerManager.setOnAttributionResolved(null)
        }
        TrafficRouter.route { decision -> routeGate = decision }
    }

    val currentDeepLink = deepLinkUrl
    when {
        // A deep link is an explicit instruction and outranks the routing decision.
        currentDeepLink != null -> OfferWebViewScreen(currentDeepLink)
        routeGate is RouteDecision.Black -> OfferWebViewScreen((routeGate as RouteDecision.Black).url)
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
