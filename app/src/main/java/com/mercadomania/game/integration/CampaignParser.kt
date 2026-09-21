package com.mercadomania.game.integration

/**
 * Splits an AppsFlyer `campaign` value into positional sub-parameters.
 *
 * The network packs several fields into one underscore-joined string; the tracker on the
 * receiving end expects them back as sub1..subN alongside the untouched original.
 *
 * Pure Kotlin, no Android imports, so the URL logic can be reasoned about and tested
 * without an emulator.
 */
object CampaignParser {

    fun parse(campaign: String): LinkedHashMap<String, String> {
        val result = LinkedHashMap<String, String>()
        // "".split('_') yields [""], which would invent a bogus sub1.
        if (campaign.isEmpty()) return result
        campaign.split('_').forEachIndexed { index, segment ->
            // Empty segments are kept: they hold the position of the fields after them.
            result["sub${index + 1}"] = segment
        }
        return result
    }

    /** Renders pairs as `k=v&k=v`, encoding only what is not already encoded. */
    fun toQuery(pairs: Map<String, String>): String =
        pairs.entries.joinToString("&") { (key, value) ->
            "${PercentEncoder.encodeQuery(key)}=${PercentEncoder.encodeQuery(value)}"
        }
}

/**
 * Idempotent percent-encoding.
 *
 * Two requirements pull in opposite directions: values that already contain `%26` must
 * survive untouched (android.net.Uri.Builder would make them `%2526`), while a raw `&`
 * arriving inside a value must not be allowed to split the query. Copying valid `%XX`
 * triplets verbatim and encoding everything else satisfies both, and makes encode()
 * safe to apply twice.
 */
internal object PercentEncoder {

    private const val HEX = "0123456789ABCDEF"

    /** unreserved (RFC 3986) plus the sub-delims a query value may carry literally. */
    private const val QUERY_EXTRA_SAFE = "/:@!$'(),*"

    fun encodeQuery(value: String): String = encode(value, QUERY_EXTRA_SAFE)

    /**
     * Path segments allow unreserved only - a literal `/` here would forge a new segment.
     * Used when appending the sub1 path segment to the offer URL.
     */
    fun encodePathSegment(value: String): String = encode(value, "")

    private fun encode(value: String, extraSafe: String): String {
        val out = StringBuilder(value.length + 16)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '%' && i + 2 < value.length && isHex(value[i + 1]) && isHex(value[i + 2])) {
                out.append(value, i, i + 3)
                i += 3
                continue
            }
            if (isSafe(c, extraSafe)) {
                out.append(c)
                i++
                continue
            }
            // Work in code points so a surrogate pair encodes as one 4-byte UTF-8
            // sequence instead of two mojibake halves.
            val codePoint = value.codePointAt(i)
            val chars = Character.toChars(codePoint)
            for (byte in String(chars).toByteArray(Charsets.UTF_8)) {
                val b = byte.toInt() and 0xFF
                out.append('%').append(HEX[b shr 4]).append(HEX[b and 0x0F])
            }
            i += Character.charCount(codePoint)
        }
        return out.toString()
    }

    private fun isSafe(c: Char, extraSafe: String): Boolean =
        c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' ||
            c == '-' || c == '.' || c == '_' || c == '~' ||
            (extraSafe.isNotEmpty() && extraSafe.indexOf(c) >= 0)

    private fun isHex(c: Char): Boolean =
        c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'
}
