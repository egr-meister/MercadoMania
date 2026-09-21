package com.mercadomania.game.integration

import kotlin.random.Random

/**
 * AppsFlyer hold-back: a random 1-in-[RATE] share of installs never initialise AppsFlyer.
 *
 * The point is a clean control group. Those installs carry no attribution, and without a
 * marker they would be indistinguishable from installs where AppsFlyer simply failed to
 * answer - two very different things on the tracker side. So the bucket is stamped onto
 * the offer URL as [PARAM_KEY] = "holdback" / "normal".
 *
 * The decision is rolled ONCE and persisted: a user must stay in the same bucket across
 * launches, or their attribution would flip-flop between runs. [decide] is pure so the
 * 1-in-RATE rule can be read and tested on its own; persistence lives in
 * [IntegrationStorage].
 */
object Holdback {

    /** 1 in 5 installs are held back (20%). */
    const val RATE = 5

    /** The query key that marks the bucket on the offer URL. */
    const val PARAM_KEY = "sub_id_15"

    const val VALUE_HOLDBACK = "holdback"
    const val VALUE_NORMAL = "normal"

    /**
     * Pure: given a uniform roll in `0 until rate`, `0` is the hold-back bucket. With the
     * default rate that is exactly one in five. A rate of 1 holds everyone back; a rate of
     * 0 or less holds no one, so a mis-set rate fails safe to "AppsFlyer on".
     */
    fun decide(roll: Int, rate: Int = RATE): Boolean =
        rate >= 1 && roll % rate == 0

    /**
     * Rolls the bucket once and remembers it. Call from Application.onCreate BEFORE
     * AppsFlyerManager.init, which reads [isHoldback] to decide whether to start the SDK.
     */
    fun resolve(rate: Int = RATE, random: Random = Random.Default) {
        if (IntegrationStorage.holdbackDecided) return
        val roll = random.nextInt(rate.coerceAtLeast(1))
        IntegrationStorage.holdback = decide(roll, rate)
        IntegrationStorage.holdbackDecided = true
    }

    /** True once [resolve] has put this install in the hold-back bucket. */
    val isHoldback: Boolean get() = IntegrationStorage.holdback

    /** The value for [PARAM_KEY] on the offer URL. */
    fun marker(): String = if (IntegrationStorage.holdback) VALUE_HOLDBACK else VALUE_NORMAL
}
