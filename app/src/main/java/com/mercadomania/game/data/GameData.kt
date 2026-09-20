package com.mercadomania.game.data

import kotlinx.serialization.Serializable

/**
 * Player settings.
 *
 * Every field has a default so that a payload written by an older (or newer)
 * build still decodes into a usable object.
 */
@Serializable
data class Settings(
    val soundEnabled: Boolean = true
)

/**
 * The single blob of persisted state for Mercado Mania.
 *
 * It is stored as one serialized JSON string inside DataStore Preferences.
 *
 * - [quizBest]  category id -> best score (0..10)
 * - [pairsBest] level number as string -> best number of matched pairs
 *
 * Maps are used instead of fixed fields so that adding a quiz category or a
 * pairs level never invalidates stored data.
 */
@Serializable
data class GameData(
    val quizBest: Map<String, Int> = emptyMap(),
    val pairsBest: Map<String, Int> = emptyMap(),
    val settings: Settings = Settings()
) {
    /** Best score for [categoryId], or 0 when the category was never played. */
    fun quizBestFor(categoryId: String): Int = quizBest[categoryId]?.coerceAtLeast(0) ?: 0

    /** Best matched-pair count for [level], or 0 when the level was never played. */
    fun pairsBestFor(level: Int): Int = pairsBest[level.toString()]?.coerceAtLeast(0) ?: 0
}
