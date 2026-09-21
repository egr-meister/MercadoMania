package com.mercadomania.game.integration

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeepLinkRouterTest {

    // DeepLinkRouter is a process-wide singleton; drain and detach any state a previous
    // test left behind so each test starts clean.
    @Before
    fun reset() {
        DeepLinkRouter.setHandler { /* drains any queued links into the void */ }
        DeepLinkRouter.clearHandler()
    }

    @After
    fun tearDown() {
        DeepLinkRouter.setHandler { }
        DeepLinkRouter.clearHandler()
    }

    @Test
    fun `link arriving before a handler is queued and delivered on registration`() {
        DeepLinkRouter.handle("https://offer/early")

        val received = mutableListOf<String>()
        DeepLinkRouter.setHandler { received.add(it) }

        assertEquals(listOf("https://offer/early"), received)
    }

    @Test
    fun `link arriving after a handler is delivered immediately`() {
        val received = mutableListOf<String>()
        DeepLinkRouter.setHandler { received.add(it) }

        DeepLinkRouter.handle("https://offer/live")

        assertEquals(listOf("https://offer/live"), received)
    }

    @Test
    fun `a cleared handler receives nothing and the link is requeued for the next one`() {
        val first = mutableListOf<String>()
        DeepLinkRouter.setHandler { first.add(it) }
        DeepLinkRouter.clearHandler()

        DeepLinkRouter.handle("https://offer/queued")
        assertTrue(first.isEmpty())

        val second = mutableListOf<String>()
        DeepLinkRouter.setHandler { second.add(it) }
        assertEquals(listOf("https://offer/queued"), second)
    }

    @Test
    fun `blank links are ignored`() {
        val received = mutableListOf<String>()
        DeepLinkRouter.setHandler { received.add(it) }

        DeepLinkRouter.handle(null)
        DeepLinkRouter.handle("")
        DeepLinkRouter.handle("   ")

        assertTrue(received.isEmpty())
    }
}
