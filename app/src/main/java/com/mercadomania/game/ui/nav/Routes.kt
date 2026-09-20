package com.mercadomania.game.ui.nav

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose routes.
 *
 * Arguments travel as real Kotlin types instead of hand-formatted strings, and
 * every destination that takes one validates it before rendering (see
 * `NavGraph.kt`), so a stale back stack, a deleted save, or a hand-crafted
 * argument lands on the friendly fallback rather than crashing.
 */

@Serializable
data object LoadingRoute

@Serializable
data object MainRoute

@Serializable
data object MenuRoute

@Serializable
data object QuizCategoriesRoute

/** @param categoryId one of [com.mercadomania.game.game.QuizContent.categoryIds] */
@Serializable
data class QuizRoute(val categoryId: String)

@Serializable
data object LevelsRoute

/** @param level 1..9 */
@Serializable
data class PairsRoute(val level: Int)

@Serializable
data object ResultsRoute

@Serializable
data object SettingsRoute

@Serializable
data object RulesRoute
