package com.duckblast.game.game.level

class LevelManager {

    var currentLevel: Int = 1
        private set

    fun current(): LevelConfig = LevelProgressionTable.configFor(currentLevel)

    fun advance() {
        currentLevel += 1
    }

    fun reset() {
        currentLevel = 1
    }

    fun setLevel(level: Int) {
        currentLevel = level.coerceAtLeast(1)
    }
}
