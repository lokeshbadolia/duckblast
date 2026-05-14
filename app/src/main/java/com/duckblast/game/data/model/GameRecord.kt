package com.duckblast.game.data.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class GameRecord(
    @Id var id: Long = 0,
    @Index var profileId: Long = 0,
    var score: Long = 0,
    var levelReached: Int = 0,
    var ducksHit: Int = 0,
    var shotsFired: Int = 0,
    var accuracy: Float = 0f,
    var playedAt: Long = System.currentTimeMillis(),
    var durationSeconds: Int = 0,
    var isPerfectGame: Boolean = false
)
