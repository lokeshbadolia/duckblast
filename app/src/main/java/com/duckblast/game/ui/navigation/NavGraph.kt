package com.duckblast.game.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.duckblast.game.ui.game.GameNavigation
import com.duckblast.game.ui.game.GameScreen
import com.duckblast.game.ui.gameover.GameOverScreen
import com.duckblast.game.ui.menu.MenuScreen
import com.duckblast.game.ui.multiplayer.LobbyScreen
import com.duckblast.game.ui.profile.ProfileScreen
import com.duckblast.game.ui.scoreboard.ScoreboardScreen
import com.duckblast.game.ui.splash.SplashScreen

@Composable
fun DuckBlastNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onProfilePresent = {
                    navController.navigate(Screen.Menu.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNoProfile = {
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onProfileSelected = {
                    navController.navigate(Screen.Menu.route) {
                        popUpTo(Screen.Profile.route) { inclusive = true }
                    }
                },
                onBack = if (navController.previousBackStackEntry != null) {
                    { navController.popBackStack() }
                } else null
            )
        }

        composable(Screen.Menu.route) {
            MenuScreen(
                onPlay = { navController.navigate(Screen.Game.build()) },
                onMultiplayer = { navController.navigate(Screen.Lobby.route) },
                onScoreboard = { navController.navigate(Screen.Scoreboard.route) },
                onChangeHunter = { navController.navigate(Screen.Profile.route) }
            )
        }

        composable(
            route = Screen.Game.route,
            arguments = listOf(
                navArgument(Screen.Game.ARG_MULTIPLAYER) {
                    type = NavType.BoolType; defaultValue = false
                },
                navArgument(Screen.Game.ARG_SEED) {
                    type = NavType.LongType; defaultValue = 0L
                }
            )
        ) { entry ->
            val multiplayer = entry.arguments?.getBoolean(Screen.Game.ARG_MULTIPLAYER) ?: false
            val seed = entry.arguments?.getLong(Screen.Game.ARG_SEED) ?: 0L
            GameScreen(
                multiplayer = multiplayer,
                seed = seed,
                onNavigateToMenu = {
                    navController.popBackStack(Screen.Menu.route, inclusive = false)
                },
                onNavigateToGameOver = { nav: GameNavigation.ToGameOver ->
                    navController.navigate(
                        Screen.GameOver.build(
                            score = nav.score,
                            level = nav.level,
                            ducksHit = nav.ducksHit,
                            shotsFired = nav.shotsFired,
                            durationSec = nav.durationSec,
                            perfect = nav.perfect,
                            previousBest = nav.previousBest
                        )
                    ) {
                        popUpTo(Screen.Menu.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.GameOver.route,
            arguments = listOf(
                navArgument(Screen.GameOver.ARG_SCORE) { type = NavType.LongType },
                navArgument(Screen.GameOver.ARG_LEVEL) { type = NavType.IntType },
                navArgument(Screen.GameOver.ARG_DUCKS_HIT) { type = NavType.IntType },
                navArgument(Screen.GameOver.ARG_SHOTS_FIRED) { type = NavType.IntType },
                navArgument(Screen.GameOver.ARG_DURATION) { type = NavType.IntType },
                navArgument(Screen.GameOver.ARG_PERFECT) { type = NavType.BoolType },
                navArgument(Screen.GameOver.ARG_PREVIOUS_BEST) { type = NavType.LongType }
            )
        ) { entry ->
            val args = entry.arguments
            GameOverScreen(
                score = args?.getLong(Screen.GameOver.ARG_SCORE) ?: 0L,
                level = args?.getInt(Screen.GameOver.ARG_LEVEL) ?: 1,
                ducksHit = args?.getInt(Screen.GameOver.ARG_DUCKS_HIT) ?: 0,
                shotsFired = args?.getInt(Screen.GameOver.ARG_SHOTS_FIRED) ?: 0,
                durationSec = args?.getInt(Screen.GameOver.ARG_DURATION) ?: 0,
                perfect = args?.getBoolean(Screen.GameOver.ARG_PERFECT) ?: false,
                previousBest = args?.getLong(Screen.GameOver.ARG_PREVIOUS_BEST) ?: 0L,
                onPlayAgain = {
                    navController.navigate(Screen.Game.build()) {
                        popUpTo(Screen.Menu.route) { inclusive = false }
                    }
                },
                onMenu = {
                    navController.popBackStack(Screen.Menu.route, inclusive = false)
                }
            )
        }

        composable(Screen.Scoreboard.route) {
            ScoreboardScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Lobby.route) {
            LobbyScreen(
                onStartGame = { seed ->
                    navController.navigate(Screen.Game.build(multiplayer = true, seed = seed)) {
                        popUpTo(Screen.Menu.route) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
