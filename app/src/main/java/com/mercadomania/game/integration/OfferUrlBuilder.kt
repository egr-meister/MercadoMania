package com.mercadomania.game.integration

/**
 * Assembles the offer URL from the base URL and everything Adjust reported.
 *
 * Built by string concatenation on purpose: android.net.Uri.Builder percent-encodes
 * values that are already encoded, so a campaign carrying `%26` would reach the tracker
 * as `%2526`.
 *
 * In the "prily" campaign group `sub1` (parsed out of `campaign`) is also placed as a path
 * segment right after the base path, while still kept in the query — the offer server
 * routes on the path segment and the tracker still reads sub1 from the query. In "aso" the
 * path is left untouched.
 */
object OfferUrlBuilder {

    private const val CAMPAIGN_KEY = "campaign"
    private const val ADID_KEY = "adid"
    private const val SUB1_KEY = "sub1"

    fun build(
        baseUrl: String,
        attribution: Map<String, String>,
        adid: String?,
        // Device signals (sub12/sub13/sub14), push_token, push_id and any other extras.
        // Applied AFTER the campaign subs so they win: a 12+ segment campaign produces its
        // own sub12, and without this override the URL would carry sub12 twice.
        extras: Map<String, String> = emptyMap(),
        // "prily" group: sub1 also goes into the path after the alias. "aso": path untouched.
        sub1InPath: Boolean = false,
    ): String {
        val base = baseUrl.trim()
        if (base.isEmpty()) return ""

        // Split without decoding — anything already encoded must pass through as-is.
        val hashIndex = base.indexOf('#')
        val fragment = if (hashIndex >= 0) base.substring(hashIndex) else ""
        val withoutFragment = if (hashIndex >= 0) base.substring(0, hashIndex) else base

        val questionIndex = withoutFragment.indexOf('?')
        val path = if (questionIndex >= 0) withoutFragment.substring(0, questionIndex) else withoutFragment
        val baseQuery = if (questionIndex >= 0) withoutFragment.substring(questionIndex + 1) else ""

        val params = LinkedHashMap<String, String>()
        parseQuery(baseQuery, params)
        // Every key the tracker sent, in arrival order. Filtering to a known subset here is
        // the single most common way this integration silently loses attribution.
        params.putAll(attribution)

        val campaign = attribution[CAMPAIGN_KEY].orEmpty()
        val subs = CampaignParser.parse(campaign)

        // The tracker reads the raw campaign as well as the exploded parts, so it stays.
        if (campaign.isNotEmpty()) params[CAMPAIGN_KEY] = campaign

        // Parsed subs win over any sub1..subN the tracker happened to send itself.
        // Re-putting an existing key replaces the value in place, so ordering stays
        // deterministic and no query key is emitted twice.
        params.putAll(subs)

        // Extras override same-named campaign subs (e.g. sub12 from a long campaign).
        params.putAll(extras)

        // A blank id would reach the tracker as an empty adid, which reads as "attributed
        // to nothing" rather than "not known yet".
        if (!adid.isNullOrBlank()) params[ADID_KEY] = adid.trim()

        val query = CampaignParser.toQuery(params)

        // sub1 into the path only in prily mode; empty/absent sub1 leaves the path unchanged.
        val sub1 = subs[SUB1_KEY].orEmpty()
        val finalPath = if (sub1InPath && sub1.isNotEmpty()) appendPathSegment(path, sub1) else path

        return buildString {
            append(finalPath)
            if (query.isNotEmpty()) {
                append('?')
                append(query)
            }
            append(fragment)
        }
    }

    /**
     * Appends a single encoded segment after the existing path. Idempotent: if the path
     * already ends with this segment (build() called twice on its own output), it is not
     * added again.
     */
    private fun appendPathSegment(path: String, segment: String): String {
        val encoded = PercentEncoder.encodePathSegment(segment)
        val trimmed = path.trimEnd('/')
        val lastSegment = trimmed.substringAfterLast('/', "")
        return if (lastSegment == encoded) path else "$trimmed/$encoded"
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
