package com.mercadomania.game.data

import kotlinx.serialization.json.Json

/**
 * Context-free encode/decode for [GameData], kept out of the repository so it
 * can be unit tested on the JVM without Android.
 *
 * Decoding is total: null, empty, blank, truncated, corrupted, or
 * structurally-wrong JSON all return [GameData] defaults instead of throwing.
 */
object GameSerializer {

    private val json = Json {
        // Forward compatibility: a payload written by a newer build that added
        // fields must still load in an older one.
        ignoreUnknownKeys = true
        // Backward compatibility: write every field, including defaults, so a
        // payload is always self-describing.
        encodeDefaults = true
        isLenient = true
        coerceInputValues = true
        allowStructuredMapKeys = false
    }

    /** Serializes [data]. Never throws; falls back to an empty-state payload. */
    fun encode(data: GameData): String =
        runCatching { json.encodeToString(GameData.serializer(), data) }
            .getOrElse { EMPTY_PAYLOAD }

    /** Deserializes [raw]. Never throws; unreadable input yields defaults. */
    fun decode(raw: String?): GameData {
        if (raw.isNullOrBlank()) return GameData()
        return runCatching { json.decodeFromString(GameData.serializer(), raw) }
            .getOrElse { GameData() }
            .sanitized()
    }

    /**
     * Clamps anything that a hand-edited or corrupted payload could smuggle in
     * (negative scores, absurd values) so the UI never has to defend itself.
     */
    private fun GameData.sanitized(): GameData = copy(
        quizBest = quizBest.mapValues { (_, v) -> v.coerceAtLeast(0) },
        pairsBest = pairsBest.mapValues { (_, v) -> v.coerceAtLeast(0) }
    )

    private val EMPTY_PAYLOAD: String =
        runCatching { json.encodeToString(GameData.serializer(), GameData()) }
            .getOrElse { "{}" }
}
