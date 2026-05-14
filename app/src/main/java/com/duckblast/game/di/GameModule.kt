package com.duckblast.game.di

import com.duckblast.game.game.engine.GameEngine
import com.duckblast.game.game.level.LevelManager
import com.duckblast.game.game.scoring.ScoreSystem
import com.duckblast.game.ui.game.GameViewModel
import com.duckblast.game.ui.gameover.GameOverViewModel
import com.duckblast.game.ui.menu.MenuViewModel
import com.duckblast.game.ui.multiplayer.LobbyViewModel
import com.duckblast.game.ui.profile.ProfileViewModel
import com.duckblast.game.ui.scoreboard.ScoreboardViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * One [GameEngine] per game session (factory). [LevelManager] and
 * [ScoreSystem] are factories so each new engine gets a fresh pair.
 */
val gameModule = module {
    factory { LevelManager() }
    factory { ScoreSystem() }
    factory { GameEngine(get(), get(), get(), get()) }
}

val viewModelModule = module {
    viewModel { ProfileViewModel(get()) }
    viewModel { MenuViewModel(get(), get(), get()) }
    viewModel { GameViewModel(get(), get(), get(), get()) }
    viewModel { GameOverViewModel(get(), get()) }
    viewModel { ScoreboardViewModel(get(), get()) }
    viewModel { LobbyViewModel(get(), get()) }
}
