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
 *  - storage first (everyone reads from it);
 *  - the accelerometer window is a whole second, so sampling must begin at launch, not
 *    where the offer URL is assembled;
 *  - the FCM push token must be requested before the single click, so [PushManager.init]
 *    runs at launch too.
 */
class MercadoManiaIntegrationApp : MercadoApp() {
    override fun onCreate() {
        super.onCreate() // the game's own repository / sound / preference sync, unchanged
        IntegrationStorage.init(this)
        DeviceSignals.init(this)
        AccelerometerProbe.start(this)
        AdjustManager.init(this)
        PushManager.init(this)
    }
}
