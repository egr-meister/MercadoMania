package com.mercadomania.game.game

import kotlin.random.Random

/**
 * Difficulty description for one Matching Pairs level.
 *
 * @param pairs how many pairs are on the board (deck size is `pairs * 2`)
 * @param columns grid width
 * @param seconds countdown budget for the level
 */
data class LevelConfig(
    val level: Int,
    val pairs: Int,
    val columns: Int,
    val seconds: Int
) {
    val cardCount: Int get() = pairs * 2
}

/**
 * Pure, context-free game rules.
 *
 * Nothing here touches Android, Compose, storage or time, which is what makes
 * it directly unit-testable (see `app/src/test`).
 */
object GameLogic {

    /** Number of distinct themed match icons shipped as `item_01..item_12`. */
    const val ICON_COUNT: Int = 12

    /** Questions in every quiz category. */
    const val QUIZ_QUESTIONS_PER_CATEGORY: Int = 10

    /** The nine levels, in increasing difficulty. */
    val levels: List<LevelConfig> = listOf(
        LevelConfig(level = 1, pairs = 2, columns = 2, seconds = 60),
        LevelConfig(level = 2, pairs = 3, columns = 2, seconds = 70),
        LevelConfig(level = 3, pairs = 4, columns = 2, seconds = 70),
        LevelConfig(level = 4, pairs = 6, columns = 3, seconds = 80),
        LevelConfig(level = 5, pairs = 6, columns = 3, seconds = 80),
        LevelConfig(level = 6, pairs = 8, columns = 4, seconds = 100),
        LevelConfig(level = 7, pairs = 8, columns = 4, seconds = 100),
        LevelConfig(level = 8, pairs = 10, columns = 4, seconds = 110),
        LevelConfig(level = 9, pairs = 12, columns = 4, seconds = 120)
    )

    /** Total number of levels (9). */
    val levelCount: Int get() = levels.size

    /** The configuration for [level], or `null` when the level does not exist. */
    fun levelConfig(level: Int): LevelConfig? = levels.firstOrNull { it.level == level }

    /**
     * Builds a shuffled deck of icon indices for a board with [pairs] pairs.
     *
     * The result always contains `pairs * 2` entries and exactly two of each
     * chosen icon, so matching by icon index is unambiguous. [pairs] is clamped
     * to `1..iconCount` because a board cannot hold more distinct pairs than
     * there are icons.
     *
     * @return indices into `Assets.items`, shuffled with [random]
     */
    fun buildDeck(
        pairs: Int,
        iconCount: Int = ICON_COUNT,
        random: Random = Random.Default
    ): List<Int> {
        if (pairs <= 0 || iconCount <= 0) return emptyList()
        val usablePairs = pairs.coerceAtMost(iconCount)
        val chosen = (0 until iconCount).shuffled(random).take(usablePairs)
        return (chosen + chosen).shuffled(random)
    }

    /** True when [level] was cleared: every pair on it has been matched. */
    fun isLevelCompleted(level: Int, pairsBest: Map<String, Int>): Boolean {
        val config = levelConfig(level) ?: return false
        val best = pairsBest[level.toString()] ?: 0
        return best >= config.pairs
    }

    /** Level 1 is always open; every other level needs the previous one cleared. */
    fun isLevelUnlocked(level: Int, pairsBest: Map<String, Int>): Boolean {
        if (levelConfig(level) == null) return false
        if (level == levels.first().level) return true
        return isLevelCompleted(level - 1, pairsBest)
    }

    /** How many of the nine levels have been cleared. */
    fun completedLevelCount(pairsBest: Map<String, Int>): Int =
        levels.count { isLevelCompleted(it.level, pairsBest) }

    /** The furthest level the player may currently open. */
    fun highestUnlockedLevel(pairsBest: Map<String, Int>): Int =
        levels.lastOrNull { isLevelUnlocked(it.level, pairsBest) }?.level ?: levels.first().level

    /** The level after [level], or `null` when [level] is the last one. */
    fun nextLevel(level: Int): Int? =
        levels.firstOrNull { it.level == level + 1 }?.level

    /** How many quiz categories have at least one recorded score. */
    fun playedQuizCategoryCount(quizBest: Map<String, Int>, categoryIds: List<String>): Int =
        categoryIds.count { (quizBest[it] ?: 0) > 0 }

    /**
     * A stable per-round seed. Two rounds of the same level get different
     * boards, while a single round keeps its board across recompositions.
     */
    fun seedFor(level: Int, round: Int): Long = level * 1_000_003L + round * 7_919L
}
