package com.mercadomania.game

import com.mercadomania.game.data.GameData
import com.mercadomania.game.data.GameSerializer
import com.mercadomania.game.data.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSerializerTest {

    @Test
    fun `round trip preserves everything`() {
        val original = GameData(
            quizBest = mapOf("treasures" to 8, "trade" to 10),
            pairsBest = mapOf("1" to 2, "6" to 8),
            settings = Settings(soundEnabled = false)
        )
        val decoded = GameSerializer.decode(GameSerializer.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun `null input yields defaults`() {
        val data = GameSerializer.decode(null)
        assertEquals(GameData(), data)
        assertTrue(data.settings.soundEnabled)
        assertTrue(data.quizBest.isEmpty())
        assertTrue(data.pairsBest.isEmpty())
    }

    @Test
    fun `empty and blank input yields defaults`() {
        assertEquals(GameData(), GameSerializer.decode(""))
        assertEquals(GameData(), GameSerializer.decode("   "))
        assertEquals(GameData(), GameSerializer.decode("\n\t "))
    }

    @Test
    fun `corrupted input yields defaults instead of throwing`() {
        val junk = listOf(
            "{",
            "}",
            "not json at all",
            "{\"quizBest\":",
            "{\"quizBest\":[1,2,3]}",
            "[]",
            "\u0000\u0001",
            "{\"quizBest\":{\"a\":\"not-a-number\"}}"
        )
        junk.forEach { raw ->
            assertEquals("input: $raw", GameData(), GameSerializer.decode(raw))
        }
    }

    @Test
    fun `unknown keys are ignored for forward compatibility`() {
        val raw = """
            {
              "quizBest": {"treasures": 6},
              "pairsBest": {"2": 3},
              "settings": {"soundEnabled": false, "hapticsEnabled": true},
              "futureField": {"nested": [1, 2, 3]},
              "anotherFuture": "whatever"
            }
        """.trimIndent()
        val data = GameSerializer.decode(raw)
        assertEquals(6, data.quizBestFor("treasures"))
        assertEquals(3, data.pairsBestFor(2))
        assertFalse(data.settings.soundEnabled)
    }

    @Test
    fun `missing fields fall back to defaults`() {
        assertEquals(GameData(), GameSerializer.decode("{}"))

        val onlyQuiz = GameSerializer.decode("""{"quizBest":{"trade":9}}""")
        assertEquals(9, onlyQuiz.quizBestFor("trade"))
        assertTrue(onlyQuiz.pairsBest.isEmpty())
        assertTrue(onlyQuiz.settings.soundEnabled)

        val onlySettings = GameSerializer.decode("""{"settings":{}}""")
        assertTrue(onlySettings.settings.soundEnabled)
    }

    @Test
    fun `negative stored values are clamped to zero`() {
        val data = GameSerializer.decode(
            """{"quizBest":{"trade":-4},"pairsBest":{"3":-9}}"""
        )
        assertEquals(0, data.quizBestFor("trade"))
        assertEquals(0, data.pairsBestFor(3))
    }

    @Test
    fun `lookups for unknown keys are safe`() {
        val data = GameData(quizBest = mapOf("trade" to 5), pairsBest = mapOf("2" to 3))
        assertEquals(0, data.quizBestFor("nope"))
        assertEquals(0, data.quizBestFor(""))
        assertEquals(0, data.pairsBestFor(99))
        assertEquals(0, data.pairsBestFor(-1))
    }

    @Test
    fun `encode never throws and always produces decodable output`() {
        val big = GameData(
            quizBest = (1..200).associate { "cat$it" to it },
            pairsBest = (1..200).associate { "$it" to it }
        )
        val encoded = GameSerializer.encode(big)
        assertTrue(encoded.isNotBlank())
        assertEquals(big, GameSerializer.decode(encoded))
    }
}
