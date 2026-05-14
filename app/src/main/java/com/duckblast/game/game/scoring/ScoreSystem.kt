package com.duckblast.game.game.scoring

class ScoreSystem {

    var score: Long = 0L
        private set
    var ducksHit: Int = 0
        private set
    var shotsFired: Int = 0
        private set

    val accuracy: Float
        get() = if (shotsFired == 0) 0f else ducksHit.toFloat() / shotsFired

    fun addHit(points: Int) {
        score += points
        ducksHit += 1
    }

    fun addShot() {
        shotsFired += 1
    }

    fun addBonus(points: Int) {
        score += points
    }

    fun reset() {
        score = 0L
        ducksHit = 0
        shotsFired = 0
    }
}
