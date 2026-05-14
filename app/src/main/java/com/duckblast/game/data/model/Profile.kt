package com.duckblast.game.data.model

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.annotation.Index

@Entity
data class Profile(
    @Id var id: Long = 0,
    @Index var name: String = "",
    var avatarColorHex: String = "#FF6B6B",
    var highScore: Long = 0,
    var totalDucksHit: Int = 0,
    var totalShotsFired: Int = 0,
    var totalGamesPlayed: Int = 0,
    var highestLevelReached: Int = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var lastPlayedAt: Long = 0,
    var streakDays: Int = 0,
    var lastPlayedDate: String = ""
) {
    val accuracy: Float
        get() = if (totalShotsFired == 0) 0f else totalDucksHit.toFloat() / totalShotsFired

    val initial: String
        get() = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}
