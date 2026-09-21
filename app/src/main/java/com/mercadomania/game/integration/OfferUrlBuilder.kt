package com.mercadomania.game.integration

/**
 * Assembles the offer URL from the base URL and everything AppsFlyer reported.
 *
 * Built by string concatenation on purpose: android.net.Uri.Builder percent-encodes
 * values that are already encoded, so a campaign carrying `%26` would reach the tracker
 * as `%2526`.
 *
 * Two things this builder guarantees, both load-bearing for attribution:
 *  - every parameter AppsFlyer sent survives into the query, `campaign` included, even
 *    after `campaign` has been exploded into sub1..subN;
 *  - `sub1` is written into the path directly after the domain AND kept in the query.
 *
 * Pure Kotlin, no Android imports.
 */
object OfferUrlBuilder {

    private const val CAMPAIGN_KEY = "campaign"
    private const val APPSFLYER_ID_KEY = "appsflyer_id"
    private const val SUB1_KEY = "sub1"

    /**
     * @param extra caller-supplied parameters (device signals sub12/sub13/sub14) that must
     *   override any same-named segment the campaign produced. A 12+-segment campaign makes
     *   its own sub12, and the URL must never carry it twice.
     */
    fun build(
        baseUrl: String,
        appsFlyerParams: Map<String, String>,
        appsFlyerId: String?,
        extra: Map<String, String> = emptyMap()
    ): String {
        val base = baseUrl.trim()
        if (base.isEmpty()) return ""

        // Split without decoding - anything already encoded must pass through as-is.
        val hashIndex = base.indexOf('#')
        val fragment = if (hashIndex >= 0) base.substring(hashIndex) else ""
        val withoutFragment = if (hashIndex >= 0) base.substring(0, hashIndex) else base

        val questionIndex = withoutFragment.indexOf('?')
        var path = if (questionIndex >= 0) withoutFragment.substring(0, questionIndex) else withoutFragment
        val baseQuery = if (questionIndex >= 0) withoutFragment.substring(questionIndex + 1) else ""

        val params = LinkedHashMap<String, String>()
        parseQuery(baseQuery, params)
        // Every key AppsFlyer sent, in arrival order. Filtering to a known subset here is
        // the single most common way this integration silently loses attribution.
        params.putAll(appsFlyerParams)

        val campaign = appsFlyerParams[CAMPAIGN_KEY].orEmpty()
        val subs = CampaignParser.parse(campaign)

        // The tracker reads the raw campaign as well as the exploded parts, so it stays.
        if (campaign.isNotEmpty()) params[CAMPAIGN_KEY] = campaign

        // Parsed subs win over any sub1..subN AppsFlyer happened to send itself.
        // Re-putting an existing key replaces the value in place, so ordering stays
        // deterministic and no query key is emitted twice.
        params.putAll(subs)

        // Device signals (and any other caller extras) win over same-named campaign
        // segments: sub12/sub13/sub14 replace a 12+-segment campaign's own sub12.. in
        // place, so the URL never carries a duplicate key.
        params.putAll(extra)

        // A blank id would reach the tracker as an empty appsflyer_id, which reads as
        // "attributed to nothing" rather than "not known yet".
        if (!appsFlyerId.isNullOrBlank()) params[APPSFLYER_ID_KEY] = appsFlyerId.trim()

        // sub1 also becomes a path segment right after the domain (or after the base
        // path, if the base URL carries one). It stays in the query as well - the two
        // are read independently on the tracker side.
        val sub1 = subs[SUB1_KEY]?.trim().orEmpty()
        if (sub1.isNotEmpty()) {
            path = appendPathSegment(path, sub1)
        }

        val query = CampaignParser.toQuery(params)

        return buildString {
            append(path)
            if (query.isNotEmpty()) {
                append('?')
                append(query)
            }
            append(fragment)
        }
    }

    /**
     * Appends [sub1] as a path segment, keeping exactly one separator slash.
     *
     * Idempotent: if [build] is accidentally run again on a URL it already produced, the
     * last segment is already this sub1 and nothing is appended a second time.
     */
    private fun appendPathSegment(path: String, sub1: String): String {
        val encoded = PercentEncoder.encodePathSegment(sub1)
        if (path.trimEnd('/').endsWith("/$encoded")) return path
        return if (path.endsWith("/")) "$path$encoded" else "$path/$encoded"
    }

    private fun parseQuery(query: String, into: LinkedHashMap<String, String>) {
        if (query.isEmpty()) return
        for (pair in query.split('&')) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            if (eq < 0) into[pair] = "" else into[pair.substring(0, eq)] = pair.substring(eq + 1)
        }
    }
}
