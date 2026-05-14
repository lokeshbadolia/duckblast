package com.duckblast.game.multiplayer

/**
 * Host-side multiplayer API. The "server" in DuckBlast is really just the
 * device that decides when to drop the [MultiplayerMessage.StartGame] packet
 * with the shared RNG seed — once that fires, every device plays its own copy
 * of the run independently using the deterministic seed.
 */
class GameServer(private val discovery: LanDiscovery) {

    fun broadcastStart(seed: Long, countdownSec: Int = 3) {
        discovery.send(MultiplayerMessage.StartGame(seed = seed, countdownSec = countdownSec))
    }

    fun broadcastScore(profileId: Long, score: Long, level: Int, alive: Boolean) {
        discovery.send(
            MultiplayerMessage.ScoreUpdate(
                profileId = profileId,
                score = score,
                level = level,
                isAlive = alive
            )
        )
    }
}
