package com.mercadomania.game.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.mercadomania.game.R
import com.mercadomania.game.audio.SoundManager
import com.mercadomania.game.data.GameRepository
import com.mercadomania.game.game.GameLogic
import com.mercadomania.game.game.QuizContent
import com.mercadomania.game.mercadoAppOrNull
import com.mercadomania.game.ui.screens.FallbackScreen
import com.mercadomania.game.ui.screens.LevelsScreen
import com.mercadomania.game.ui.screens.LoadingScreen
import com.mercadomania.game.ui.screens.MainScreen
import com.mercadomania.game.ui.screens.MenuScreen
import com.mercadomania.game.ui.screens.PairsScreen
import com.mercadomania.game.ui.screens.QuizCategoriesScreen
import com.mercadomania.game.ui.screens.QuizScreen
import com.mercadomania.game.ui.screens.ResultsSummaryScreen
import com.mercadomania.game.ui.screens.RulesScreen
import com.mercadomania.game.ui.screens.SettingsScreen
import com.mercadomania.game.ui.viewmodel.GameDataViewModel
import com.mercadomania.game.ui.viewmodel.GameDataViewModelFactory
import com.mercadomania.game.ui.viewmodel.LoadingViewModel
import com.mercadomania.game.ui.viewmodel.LoadingViewModelFactory
import com.mercadomania.game.ui.viewmodel.PairsOutcome
import com.mercadomania.game.ui.viewmodel.PairsViewModel
import com.mercadomania.game.ui.viewmodel.PairsViewModelFactory
import com.mercadomania.game.ui.viewmodel.QuizViewModel
import com.mercadomania.game.ui.viewmodel.QuizViewModelFactory

/**
 * The whole navigation graph, using Navigation Compose type-safe routes.
 *
 * Safety rules applied at every destination:
 *  - argument extraction is wrapped, so a malformed or stale entry cannot throw
 *  - arguments are validated against real content (category ids, level range,
 *    unlock state) before the screen is built
 *  - anything that fails validation renders [FallbackScreen] with a Back
 *  - an empty or wiped save resolves to defaults, never to a crash
 */
@Composable
fun MercadoNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val app = remember(context) { context.mercadoAppOrNull() }

    if (app == null) {
        // Only reachable if the Application class were replaced (for example in
        // a tooling preview). Better a friendly panel than a crash.
        FallbackScreen(onBack = { navController.popBackStack() }, modifier = modifier)
        return
    }

    val repository: GameRepository = app.repository
    val soundManager: SoundManager = app.soundManager
    val sharedFactory = remember(repository, soundManager) {
        GameDataViewModelFactory(repository, soundManager)
    }

    NavHost(
        navController = navController,
        startDestination = LoadingRoute,
        modifier = modifier.fillMaxSize()
    ) {

        composable<LoadingRoute> {
            val vm: LoadingViewModel = viewModel(
                factory = LoadingViewModelFactory(context, repository, soundManager)
            )
            val state by vm.state.collectAsStateWithLifecycle()

            LaunchedEffect(state.ready) {
                if (state.ready) {
                    navController.navigate(MainRoute) {
                        // The loading screen must not come back on Back.
                        popUpTo(LoadingRoute) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            LoadingScreen(progress = state.progress)
        }

        composable<MainRoute> {
            val vm: GameDataViewModel = viewModel(factory = sharedFactory)
            MainScreen(
                onStart = {
                    vm.click()
                    navController.navigateSingleTop(MenuRoute)
                }
            )
        }

        composable<MenuRoute> {
            val vm: GameDataViewModel = viewModel(factory = sharedFactory)
            MenuScreen(
                onQuiz = { vm.click(); navController.navigateSingleTop(QuizCategoriesRoute) },
                onPairs = { vm.click(); navController.navigateSingleTop(LevelsRoute) },
                onResults = { vm.click(); navController.navigateSingleTop(ResultsRoute) },
                onSettings = { vm.click(); navController.navigateSingleTop(SettingsRoute) },
                onRules = { vm.click(); navController.navigateSingleTop(RulesRoute) },
                onBack = { vm.click(); navController.popBackStack() }
            )
        }

        composable<QuizCategoriesRoute> {
            val vm: GameDataViewModel = viewModel(factory = sharedFactory)
            val state by vm.state.collectAsStateWithLifecycle()
            QuizCategoriesScreen(
                quizBest = state.data.quizBest,
                onCategory = { id ->
                    vm.click()
                    if (QuizContent.category(id) != null) {
                        navController.navigateSingleTop(QuizRoute(id))
                    }
                },
                onBack = { vm.click(); navController.popBackStack() }
            )
        }

        composable<QuizRoute> { entry ->
            val route = runCatching { entry.toRoute<QuizRoute>() }.getOrNull()
            val category = QuizContent.category(route?.categoryId)

            if (category == null) {
                FallbackScreen(onBack = { navController.popBackStack() })
                return@composable
            }

            val vm: QuizViewModel = viewModel(
                factory = QuizViewModelFactory(repository, soundManager, category.id)
            )
            val state by vm.state.collectAsStateWithLifecycle()

            if (!state.valid) {
                FallbackScreen(onBack = { navController.popBackStack() })
                return@composable
            }

            QuizScreen(
                state = state,
                onAnswer = vm::answer,
                onPause = vm::pause,
                onResume = vm::resume,
                onRestart = vm::restart,
                onMenu = { navController.goToMenu() },
                onBack = { navController.popBackStack() },
                onContinue = { navController.popBackStack() }
            )
        }

        composable<LevelsRoute> {
            val vm: GameDataViewModel = viewModel(factory = sharedFactory)
            val state by vm.state.collectAsStateWithLifecycle()
            LevelsScreen(
                pairsBest = state.data.pairsBest,
                onLevel = { level ->
                    vm.click()
                    if (GameLogic.isLevelUnlocked(level, state.data.pairsBest)) {
                        navController.navigateSingleTop(PairsRoute(level))
                    }
                },
                onRules = { vm.click(); navController.navigateSingleTop(RulesRoute) },
                onBack = { vm.click(); navController.popBackStack() }
            )
        }

        composable<PairsRoute> { entry ->
            val route = runCatching { entry.toRoute<PairsRoute>() }.getOrNull()
            val level = route?.level ?: -1
            val config = GameLogic.levelConfig(level)

            val gate: GameDataViewModel = viewModel(factory = sharedFactory)
            val gateState by gate.state.collectAsStateWithLifecycle()

            if (config == null) {
                FallbackScreen(onBack = { navController.popBackStack() })
                return@composable
            }

            // A restored back stack can point at a level that is no longer
            // unlocked (for example after a progress reset).
            if (gateState.loaded && !GameLogic.isLevelUnlocked(level, gateState.data.pairsBest)) {
                FallbackScreen(
                    onBack = { navController.popBackStack() },
                    message = stringResource(R.string.fallback_locked)
                )
                return@composable
            }

            val vm: PairsViewModel = viewModel(
                factory = PairsViewModelFactory(repository, soundManager, level)
            )
            val state by vm.state.collectAsStateWithLifecycle()

            if (!state.valid) {
                FallbackScreen(onBack = { navController.popBackStack() })
                return@composable
            }

            PairsScreen(
                state = state,
                onFlip = vm::flip,
                onPause = vm::pause,
                onResume = vm::resume,
                onRestart = vm::restart,
                onMenu = { navController.goToMenu() },
                onBack = { navController.popBackStack() },
                onContinue = {
                    val next = GameLogic.nextLevel(level)
                    if (state.outcome == PairsOutcome.WIN && next != null) {
                        navController.navigate(PairsRoute(next)) {
                            popUpTo(LevelsRoute) { inclusive = false }
                            launchSingleTop = true
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                continueEnabled = true
            )
        }

        composable<ResultsRoute> {
            val vm: GameDataViewModel = viewModel(factory = sharedFactory)
            val state by vm.state.collectAsStateWithLifecycle()
            ResultsSummaryScreen(
                data = state.data,
                onBack = { vm.click(); navController.popBackStack() }
            )
        }

        composable<SettingsRoute> {
            val vm: GameDataViewModel = viewModel(factory = sharedFactory)
            val state by vm.state.collectAsStateWithLifecycle()
            SettingsScreen(
                soundEnabled = state.data.settings.soundEnabled,
                onSoundChange = vm::setSoundEnabled,
                onReset = vm::resetProgress,
                onBack = { vm.click(); navController.popBackStack() }
            )
        }

        composable<RulesRoute> {
            val vm: GameDataViewModel = viewModel(factory = sharedFactory)
            RulesScreen(onBack = { vm.click(); navController.popBackStack() })
        }
    }
}

/** Navigate without stacking duplicates of the same destination. */
private fun NavHostController.navigateSingleTop(route: Any) {
    runCatching {
        navigate(route) { launchSingleTop = true }
    }
}

/**
 * "Menu" from a pause or results panel: return to the existing Menu entry when
 * there is one, otherwise build a fresh path from Main.
 */
private fun NavHostController.goToMenu() {
    val popped = runCatching { popBackStack(MenuRoute, inclusive = false) }.getOrDefault(false)
    if (!popped) {
        runCatching {
            navigate(MenuRoute) {
                popUpTo(MainRoute) { inclusive = false }
                launchSingleTop = true
            }
        }
    }
}
