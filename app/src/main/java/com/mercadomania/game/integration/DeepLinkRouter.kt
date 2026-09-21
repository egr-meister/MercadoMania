package com.mercadomania.game.integration

/**
 * The single entry point for deep links. Push notifications hand URLs here, and the root
 * composable is the one thing that consumes them.
 *
 * Pure Kotlin, no Android imports.
 */
object DeepLinkRouter {

    private val lock = Any()
    private var handler: ((String) -> Unit)? = null
    // Links can arrive from a cold start (a push tap that launched the app) before any
    // composable has subscribed.
    private val pending = ArrayDeque<String>()

    fun setHandler(newHandler: (String) -> Unit) {
        val drained: List<String>
        synchronized(lock) {
            handler = newHandler
            drained = pending.toList()
            pending.clear()
        }
        // Delivered outside the lock: the handler touches Compose state and may re-enter.
        drained.forEach(newHandler)
    }

    fun clearHandler() {
        synchronized(lock) { handler = null }
    }

    fun handle(url: String?) {
        if (url.isNullOrBlank()) return
        val current: ((String) -> Unit)?
        synchronized(lock) {
            current = handler
            // Queued only when nobody is listening, so each link is delivered exactly once.
            if (current == null) pending.addLast(url)
        }
        current?.invoke(url)
    }
}
