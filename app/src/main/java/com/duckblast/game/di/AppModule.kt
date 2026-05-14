package com.duckblast.game.di

import com.duckblast.game.audio.SoundManager
import com.duckblast.game.haptics.VibrationManager
import com.duckblast.game.multiplayer.MultiplayerManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single { SoundManager(androidContext()) }
    single { VibrationManager(androidContext()) }
    single { MultiplayerManager(androidContext()) }
}
