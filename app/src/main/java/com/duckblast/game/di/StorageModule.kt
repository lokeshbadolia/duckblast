package com.duckblast.game.di

import com.duckblast.game.data.ObjectBoxStore
import com.duckblast.game.data.model.GameRecord
import com.duckblast.game.data.model.Profile
import com.duckblast.game.data.repository.GameRecordRepository
import com.duckblast.game.data.repository.ProfileRepository
import io.objectbox.Box
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

private val PROFILE_BOX = named("profileBox")
private val GAME_RECORD_BOX = named("gameRecordBox")

val storageModule = module {
    single<Box<Profile>>(PROFILE_BOX) { ObjectBoxStore.store.boxFor(Profile::class.java) }
    single<Box<GameRecord>>(GAME_RECORD_BOX) { ObjectBoxStore.store.boxFor(GameRecord::class.java) }
    single { ProfileRepository(androidContext(), get(PROFILE_BOX)) }
    single { GameRecordRepository(get(GAME_RECORD_BOX), get(PROFILE_BOX)) }
}
