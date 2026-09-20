package com.mercadomania.game

import com.mercadomania.game.game.GameLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameLogicTest {

    // ---- Level table ------------------------------------------------------

    @Test
    fun `nine levels with the specified difficulty curve`() {
        assertEquals(9, GameLogic.levelCount)
        val expected = listOf(
            Triple(2, 2, 60),
            Triple(3, 2, 70),
            Triple(4, 2, 70),
            Triple(6, 3, 80),
            Triple(6, 3, 80),
            Triple(8, 4, 100),
            Triple(8, 4, 100),
            Triple(10, 4, 110),
            Triple(12, 4, 120)
        )
        GameLogic.levels.forEachIndexed { index, config ->
            assertEquals(index + 1, config.level)
            assertEquals(expected[index], Triple(config.pairs, config.columns, config.seconds))
        }
    }

    @Test
    fun `levelConfig returns null outside the table`() {
        assertNotNull(GameLogic.levelConfig(1))
        assertNotNull(GameLogic.levelConfig(9))
        assertNull(GameLogic.levelConfig(0))
        assertNull(GameLogic.levelConfig(10))
        assertNull(GameLogic.levelConfig(-5))
        assertNull(GameLogic.levelConfig(Int.MAX_VALUE))
    }

    // ---- Deck building ----------------------------------------------------

    @Test
    fun `deck has two of every icon and the right card count`() {
        GameLogic.levels.forEach { config ->
            val deck = GameLogic.buildDeck(config.pairs, random = Random(config.level.toLong()))
            assertEquals(
                "level ${config.level} card count",
                config.pairs * 2,
                deck.size
            )
            val counts = deck.groupingBy { it }.eachCount()
            assertEquals(
                "level ${config.level} distinct icons",
                config.pairs,
                counts.size
            )
            assertTrue(
                "level ${config.level} every icon appears exactly twice",
                counts.values.all { it == 2 }
            )
            assertTrue(
                "level ${config.level} icon indices in range",
                deck.all { it in 0 until GameLogic.ICON_COUNT }
            )
        }
    }

    @Test
    fun `deck is empty for non positive input`() {
        assertTrue(GameLogic.buildDeck(0).isEmpty())
        assertTrue(GameLogic.buildDeck(-3).isEmpty())
        assertTrue(GameLogic.buildDeck(4, iconCount = 0).isEmpty())
    }

    @Test
    fun `deck clamps to the number of available icons`() {
        val deck = GameLogic.buildDeck(pairs = 50, iconCount = 12, random = Random(1))
        assertEquals(24, deck.size)
        assertEquals(12, deck.distinct().size)
        assertTrue(deck.groupingBy { it }.eachCount().values.all { it == 2 })
    }

    @Test
    fun `deck is deterministic for a given seed and different across rounds`() {
        val a = GameLogic.buildDeck(8, random = Random(GameLogic.seedFor(6, 0)))
        val b = GameLogic.buildDeck(8, random = Random(GameLogic.seedFor(6, 0)))
        val c = GameLogic.buildDeck(8, random = Random(GameLogic.seedFor(6, 1)))
        assertEquals(a, b)
        assertFalse("a new round should reshuffle", a == c)
    }

    // ---- Unlocking and completion ----------------------------------------

    @Test
    fun `level one is always unlocked and higher levels are not`() {
        val empty = emptyMap<String, Int>()
        assertTrue(GameLogic.isLevelUnlocked(1, empty))
        (2..9).forEach { assertFalse("level $it", GameLogic.isLevelUnlocked(it, empty)) }
        assertFalse(GameLogic.isLevelUnlocked(0, empty))
        assertFalse(GameLogic.isLevelUnlocked(10, empty))
    }

    @Test
    fun `clearing a level unlocks exactly the next one`() {
        val best = mapOf("1" to 2)
        assertTrue(GameLogic.isLevelCompleted(1, best))
        assertTrue(GameLogic.isLevelUnlocked(2, best))
        assertFalse(GameLogic.isLevelUnlocked(3, best))
        assertEquals(2, GameLogic.highestUnlockedLevel(best))
    }

    @Test
    fun `a partial best does not complete a level`() {
        val best = mapOf("4" to 5) // level 4 needs 6 pairs
        assertFalse(GameLogic.isLevelCompleted(4, best))
        assertFalse(GameLogic.isLevelUnlocked(5, best))
        assertEquals(0, GameLogic.completedLevelCount(best))
    }

    @Test
    fun `completedLevelCount counts only fully cleared levels`() {
        val best = mapOf(
            "1" to 2,   // cleared
            "2" to 3,   // cleared
            "3" to 1,   // partial
            "9" to 12,  // cleared
            "42" to 99  // unknown level, ignored
        )
        assertEquals(3, GameLogic.completedLevelCount(best))
    }

    @Test
    fun `completedLevelCount is zero for empty and junk data`() {
        assertEquals(0, GameLogic.completedLevelCount(emptyMap()))
        assertEquals(0, GameLogic.completedLevelCount(mapOf("abc" to 5, "" to 9, "-1" to 3)))
    }

    @Test
    fun `all levels cleared`() {
        val best = GameLogic.levels.associate { it.level.toString() to it.pairs }
        assertEquals(9, GameLogic.completedLevelCount(best))
        assertEquals(9, GameLogic.highestUnlockedLevel(best))
        assertTrue(GameLogic.levels.all { GameLogic.isLevelUnlocked(it.level, best) })
    }

    @Test
    fun `nextLevel walks the table and stops at the end`() {
        assertEquals(2, GameLogic.nextLevel(1))
        assertEquals(9, GameLogic.nextLevel(8))
        assertNull(GameLogic.nextLevel(9))
        assertNull(GameLogic.nextLevel(99))
    }

    @Test
    fun `playedQuizCategoryCount counts categories with a score`() {
        val ids = listOf("treasures", "trade", "legends")
        assertEquals(0, GameLogic.playedQuizCategoryCount(emptyMap(), ids))
        assertEquals(
            2,
            GameLogic.playedQuizCategoryCount(
                mapOf("treasures" to 7, "trade" to 0, "legends" to 1, "ghost" to 9),
                ids
            )
        )
    }
}
