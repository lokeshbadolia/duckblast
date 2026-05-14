package com.duckblast.game

import android.app.Application
import com.duckblast.game.audio.SoundManager
import com.duckblast.game.data.ObjectBoxStore
import com.duckblast.game.di.appModule
import com.duckblast.game.di.gameModule
import com.duckblast.game.di.storageModule
import com.duckblast.game.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class DuckBlastApp : Application(), KoinComponent {

    private val soundManager: SoundManager by inject()

    override fun onCreate() {
        super.onCreate()
        ObjectBoxStore.init(this)
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@DuckBlastApp)
            modules(storageModule, appModule, gameModule, viewModelModule)
        }
        // Kick off sound preload in background so the first game-screen launch
        // already has every buffer in memory.
        soundManager.preload()
    }

    override fun onTerminate() {
        soundManager.shutdown()
        ObjectBoxStore.close()
        super.onTerminate()
    }
}
