package com.mercadomania.game.integration

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.UUID

/**
 * Singleton over its OWN SharedPreferences file, so it can never collide with keys the
 * host game already stores (the game uses DataStore). `init(context)` is called once,
 * from Application.onCreate; the probe needs synchronous reads from a background thread.
 *
 * Holds: the whole attribution map, the Adjust `adid`, the push token, the offer-URL
 * cache with its write time, and the permanent White verdict.
 */
object IntegrationStorage {

    private const val PREFS_NAME = "integration_prefs"
    private const val KEY_ATTRIBUTION = "attribution"
    private const val KEY_ADID = "adid"
    private const val KEY_GPS_ADID = "gps_adid"
    private const val KEY_PUSH_TOKEN = "push_token"
    private const val KEY_PUSH_ID = "push_id"
    private const val KEY_CACHED_OFFER_URL = "cached_offer_url"
    private const val KEY_CACHED_OFFER_AT = "cached_offer_at"
    private const val KEY_WHITE_LOCKED = "white_locked"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // --- attribution ---------------------------------------------------------

    /**
     * Adds to the stored attribution rather than replacing it: attribution can arrive again
     * (re-attribution), and those payloads are not required to be a superset of the first.
     */
    fun mergeAttribution(data: Map<String, Any?>) {
        val merged = LinkedHashMap(attributionParams)
        merged.putAll(normalize(data))
        writeAttribution(merged)
    }

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

    // --- identifiers ---------------------------------------------------------

    /** Empty value ignored: that is "not ready yet", not "there is no value". */
    var adid: String?
        get() = prefs.getString(KEY_ADID, null)
        set(value) {
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_ADID, value).apply()
        }

    /**
     * The Google Advertising ID (GAID), the primary match key for Adjust postbacks. A blank
     * read means "not ready yet", so it must not clear a stored one.
     */
    var gpsAdid: String?
        get() = prefs.getString(KEY_GPS_ADID, null)
        set(value) {
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_GPS_ADID, value).apply()
        }

    var pushToken: String?
        get() = prefs.getString(KEY_PUSH_TOKEN, null)
        set(value) {
            if (value.isNullOrBlank()) return
            prefs.edit().putString(KEY_PUSH_TOKEN, value).apply()
        }

    /**
     * A stable per-install id, generated once and persisted, sent to the tracker as
     * `push_id` (extra_param_7) next to the FCM `push_token`. The push pipeline needs an
     * identifier that survives across launches; the FCM token can rotate, this does not
     * (until Clear data / reinstall).
     */
    val pushId: String
        get() {
            prefs.getString(KEY_PUSH_ID, null)?.let { return it }
            val generated = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_PUSH_ID, generated).apply()
            return generated
        }

    // --- offer address: cache with a TTL -------------------------------------

    /** The cached offer address if it is still fresh, otherwise null. */
    fun freshOfferUrl(now: Long = System.currentTimeMillis()): String? {
        val url = prefs.getString(KEY_CACHED_OFFER_URL, null)
        if (url.isNullOrBlank()) return null
        val savedAt = prefs.getLong(KEY_CACHED_OFFER_AT, 0L)
        return if (OfferCache.isFresh(savedAt, now, OfferCache.OFFER_TTL_MS)) url else null
    }

    fun saveOfferUrl(url: String, now: Long = System.currentTimeMillis()) {
        if (url.isBlank()) return
        prefs.edit()
            .putString(KEY_CACHED_OFFER_URL, url)
            .putLong(KEY_CACHED_OFFER_AT, now)
            .apply()
    }

    /** Resets ONLY the address cache. Never touches the White verdict. */
    fun clearOfferUrl() {
        prefs.edit()
            .remove(KEY_CACHED_OFFER_URL)
            .remove(KEY_CACHED_OFFER_AT)
            .apply()
    }

    // --- branch: White is pinned forever -------------------------------------

    /**
     * If the tracker ever answered 404, this device opens White until it is reinstalled.
     * There is deliberately no time argument and no reset method: irreversibility is the
     * point of this flag, not a missing feature. The only way to clear it is Clear data or
     * a reinstall, i.e. a new install.
     */
    val isWhiteLocked: Boolean get() = prefs.getBoolean(KEY_WHITE_LOCKED, false)

    fun lockWhite() {
        prefs.edit().putBoolean(KEY_WHITE_LOCKED, true).apply()
    }

    /** A branch has already been pinned, so startup can skip the network entirely. */
    fun hasRouteDecision(now: Long = System.currentTimeMillis()): Boolean =
        isWhiteLocked || freshOfferUrl(now) != null

    // --- internals -----------------------------------------------------------

    private fun writeAttribution(params: Map<String, String>) {
        val json = JSONObject()
        params.forEach { (key, value) -> json.put(key, value) }
        prefs.edit().putString(KEY_ATTRIBUTION, json.toString()).apply()
    }

    /**
     * Everything present is stringified and kept, empty strings included, because the
     * tracker distinguishes "sent as empty" from "not sent". A real null is skipped rather
     * than stored: "null" is a value, not an absence.
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
