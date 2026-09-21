package com.mercadomania.game.integration

import com.mercadomania.game.MercadoApp

/**
 * The Application for the White/Black build.
 *
 * The game already ships its own Application ([MercadoApp]) that creates the repository
 * and the sound manager, so rather than replacing it this subclass extends it: the game's
 * own initialisation runs first, unchanged, and the integration layer is bootstrapped on
 * top. Because this IS-A [MercadoApp], `context.mercadoAppOrNull()` keeps resolving and
 * the game keeps finding its repository and sound manager.
 *
 * The manifest's `android:name` must point at this class.
 *
 * Order matters:
 *  - storage before the AppsFlyer manager (the manager reads `attributionSettled`);
 *  - the user agent before anything can probe;
 *  - OneSignal after AppsFlyer, so the AppsFlyer UID can seed the OneSignal external id.
 */
class MercadoManiaIntegrationApp : MercadoApp() {
    override fun onCreate() {
        super.onCreate() // the game's own repository / sound / preference sync, unchanged
        IntegrationStorage.init(this)
        // Roll the AppsFlyer hold-back bucket once, before the manager reads it. Sticky
        // for the life of the install.
        Holdback.resolve()
        DeviceSignals.init(this)
        // Sampling takes a full second, so it must begin at launch: starting it where the
        // offer URL is assembled would mean blocking there for the whole window.
        AccelerometerProbe.start(this)
        UserAgentProvider.init(this)
        AppsFlyerManager.init(this)
        OneSignalManager.init(this)
    }
}
