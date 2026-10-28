package com.mercadomania.game.integration

/**
 * The parts of an FCM data message the app cares about. A push may carry a deep link in
 * `url`; without one it is a plain return-to-app nudge.
 */
data class PushPayload(
    val title: String?,
    val body: String?,
    val url: String?,
) {
    companion object {
        const val KEY_URL = "url"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"

        fun from(data: Map<String, String>): PushPayload = PushPayload(
            title = data[KEY_TITLE]?.takeIf { it.isNotBlank() },
            body = data[KEY_BODY]?.takeIf { it.isNotBlank() },
            url = data[KEY_URL]?.takeIf { it.isNotBlank() },
        )
    }
}
