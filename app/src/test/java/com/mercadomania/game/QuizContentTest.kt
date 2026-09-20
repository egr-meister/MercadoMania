package com.mercadomania.game

import com.mercadomania.game.game.GameLogic
import com.mercadomania.game.game.QuizContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the shape of the quiz content so replacing the placeholder questions
 * cannot silently break the game.
 */
class QuizContentTest {

    @Test
    fun `three categories with stable ids`() {
        assertEquals(3, QuizContent.categories.size)
        assertEquals(listOf("treasures", "trade", "legends"), QuizContent.categoryIds)
        assertEquals(
            "category ids must be unique",
            QuizContent.categoryIds.size,
            QuizContent.categoryIds.distinct().size
        )
    }

    @Test
    fun `ten valid questions per category`() {
        QuizContent.categories.forEach { category ->
            assertEquals(
                "category ${category.id}",
                GameLogic.QUIZ_QUESTIONS_PER_CATEGORY,
                category.questions.size
            )
            category.questions.forEachIndexed { index, question ->
                assertTrue(
                    "${category.id} question $index is valid",
                    question.isValid
                )
                assertEquals(
                    "${category.id} question $index option count",
                    4,
                    question.options.size
                )
                assertTrue(
                    "${category.id} question $index has distinct options",
                    question.options.distinct().size == question.options.size
                )
                assertTrue(
                    "${category.id} question $index correctIndex in range",
                    question.correctIndex in question.options.indices
                )
            }
        }
    }

    @Test
    fun `category emblems point at a real match icon`() {
        QuizContent.categories.forEach { category ->
            assertTrue(
                "category ${category.id} iconIndex",
                category.iconIndex in 0 until GameLogic.ICON_COUNT
            )
        }
    }

    @Test
    fun `every category has its own mascot`() {
        QuizContent.categories.forEach { category ->
            assertTrue(
                "category ${category.id} mascotIndex",
                category.mascotIndex in 0 until MASCOT_COUNT
            )
        }
        val used = QuizContent.categories.map { it.mascotIndex }
        assertEquals(
            "one mascot per category, no repeats",
            used.size,
            used.distinct().size
        )
    }

    private companion object {
        /** Mirrors `Assets.mascots.size` without pulling Android into the test. */
        const val MASCOT_COUNT = 3
    }

    @Test
    fun `lookup is safe for unknown ids`() {
        assertNotNull(QuizContent.category("treasures"))
        assertNull(QuizContent.category("does-not-exist"))
        assertNull(QuizContent.category(""))
        assertNull(QuizContent.category(null))
    }
}
