package com.duckblast.game.data.repository

import com.duckblast.game.data.model.GameRecord
import com.duckblast.game.data.model.GameRecord_
import com.duckblast.game.data.model.Profile
import io.objectbox.Box
import io.objectbox.kotlin.equal
import io.objectbox.query.QueryBuilder

class GameRecordRepository(
    private val recordBox: Box<GameRecord>,
    private val profileBox: Box<Profile>
) {
    fun insertRecord(record: GameRecord): Long {
        if (record.playedAt == 0L) record.playedAt = System.currentTimeMillis()
        return recordBox.put(record)
    }

    fun getRecordsForProfile(profileId: Long): List<GameRecord> =
        recordBox.query(GameRecord_.profileId.equal(profileId))
            .order(GameRecord_.playedAt, QueryBuilder.DESCENDING)
            .build()
            .use { it.find() }

    fun getTopScores(limit: Int = 20): List<GameRecord> =
        recordBox.query()
            .order(GameRecord_.score, QueryBuilder.DESCENDING)
            .build()
            .use { it.find(0, limit.toLong()) }

    fun getGlobalLeaderboard(limit: Int = 20): List<Pair<Profile, GameRecord>> {
        val top = getTopScores(limit)
        return top.mapNotNull { rec ->
            val profile = profileBox.get(rec.profileId) ?: return@mapNotNull null
            profile to rec
        }
    }

    fun deleteRecordsForProfile(profileId: Long) {
        val ids: LongArray = recordBox.query(GameRecord_.profileId.equal(profileId))
            .build()
            .use { it.findIds() }
        if (ids.isNotEmpty()) recordBox.remove(*ids)
    }
}
