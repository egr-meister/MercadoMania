package com.mercadomania.game.integration

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Persisted state for the White/Black layer, on its OWN SharedPreferences file so it can
 * never collide with keys the game already stores. [init] is called once, from the
 * Application, before anything else in the integration runs.
 */
object IntegrationStorage {

    private const val PREFS_NAME = "integration_prefs"
    private const val KEY_ATTRIBUTION = "attribution"
    private const val KEY_ATTRIBUTION_SETTLED = "attribution_settled"
    private const val KEY_APPSFLYER_ID = "appsflyer_id"
    private const val KEY_CACHED_OFFER_URL = "cached_offer_url"
    private const val KEY_WHITE_LOCKED = "white_locked"
    private const val KEY_HOLDBACK_DECIDED = "holdback_decided"
    private const val KEY_HOLDBACK = "holdback"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- attribution ---------------------------------------------------------

    /** Replaces the stored conversion data wholesale. */
    fun saveAttribution(data: Map<String, Any?>) {
        writeAttribution(normalize(data))
    }

    /**
     * Adds to the stored conversion data. Later events (onAppOpenAttribution) carry
     * fewer keys than the install event, and dropping the ones they omit would throw
     * away the install attribution we already have.
     */
    fun mergeAttribution(data: Map<String, Any?>) {
        val merged = LinkedHashMap(attributionParams)
        merged.putAll(normalize(data))
        writeAttribution(merged)
    }

    fun attributionValue(key: String): String? = attributionParams[key]

    /** Everything stored, in the order it was written. */
    val attributionParams: LinkedHashMap<String, String>
        get() {
            val result = LinkedHashMap<String, String>()
            val raw = prefs.getString(KEY_ATTRIBUTION, null) ?: return result
            runCatching {
                val json = JSONObject(raw)
                // Android's JSONObject is backed by a LinkedHashMap, so keys() preserves
                // insertion order and sub1..subN stay in their original sequence.
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    result[key] = json.optString(key, "")
                }
            }
            return result
        }

    val hasAttribution: Boolean get() = attributionParams.isNotEmpty()

    /**
     * True once AppsFlyer has answered at all - success or failure. Conversion data only
     * arrives on the install that carried it, so without this flag every later launch
     * would block on a callback that never fires.
     */
    var attributionSettled: Boolean
        get() = prefs.getBoolean(KEY_ATTRIBUTION_SETTLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ATTRIBUTION_SETTLED, value).apply()

    var appsFlyerId: String?
        get() = prefs.getString(KEY_APPSFLYER_ID, null)
        set(value) {
            // Guarded here as well as at the call site: once we have a UID, an empty
            // one arriving later must never replace it.
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_APPSFLYER_ID, value).apply()
        }

    // --- routing decision ----------------------------------------------------

    var cachedOfferUrl: String?
        get() = prefs.getString(KEY_CACHED_OFFER_URL, null)
        set(value) {
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_CACHED_OFFER_URL, value).apply()
        }

    val hasCachedOffer: Boolean get() = !cachedOfferUrl.isNullOrBlank()

    var whiteLocked: Boolean
        get() = prefs.getBoolean(KEY_WHITE_LOCKED, false)
        set(value) = prefs.edit().putBoolean(KEY_WHITE_LOCKED, value).apply()

    /** A branch has already been pinned, so startup can skip the network entirely. */
    val hasRouteDecision: Boolean get() = whiteLocked || hasCachedOffer

    // --- AppsFlyer hold-back -------------------------------------------------

    /** Whether the hold-back bucket has been rolled - the roll must happen only once. */
    var holdbackDecided: Boolean
        get() = prefs.getBoolean(KEY_HOLDBACK_DECIDED, false)
        set(value) = prefs.edit().putBoolean(KEY_HOLDBACK_DECIDED, value).apply()

    /** Whether this install is held back (AppsFlyer never initialised). Sticky. */
    var holdback: Boolean
        get() = prefs.getBoolean(KEY_HOLDBACK, false)
        set(value) = prefs.edit().putBoolean(KEY_HOLDBACK, value).apply()

    // --- internals -----------------------------------------------------------

    private fun writeAttribution(params: Map<String, String>) {
        val json = JSONObject()
        params.forEach { (key, value) -> json.put(key, value) }
        prefs.edit().putString(KEY_ATTRIBUTION, json.toString()).apply()
    }

    /**
     * Conversion data is Map<String, Any> - values may be numbers, booleans or null.
     * Everything present is stringified and kept, empty strings included, because the
     * tracker distinguishes "sent as empty" from "not sent". A real null is skipped
     * rather than stored: "null" is a four-character value, not an absence.
     */
    private fun normalize(data: Map<String, Any?>): LinkedHashMap<String, String> {
        val result = LinkedHashMap<String, String>()
        data.forEach { (key, value) ->
            if (key.isBlank()) return@forEach
            if (value == null) return@forEach
            result[key] = value.toString()
        }
        return result
    }
}
