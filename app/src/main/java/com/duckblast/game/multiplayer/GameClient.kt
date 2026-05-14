package com.duckblast.game.multiplayer

import kotlinx.coroutines.flow.SharedFlow

/**
 * Client-side multiplayer API. Sends presence heartbeats + game-end notices,
 * exposes the underlying [LanDiscovery] stream so callers can react to remote
 * events.
 */
class GameClient(private val discovery: LanDiscovery) {

    val incoming: SharedFlow<MultiplayerMessage> get() = discovery.incoming

    fun announce(name: String, profileId: Long, ip: String, isHost: Boolean) {
        discovery.send(
            MultiplayerMessage.Hello(
                name = name,
                profileId = profileId,
                ip = ip,
                isHost = isHost
            )
        )
    }

    fun reportGameEnd(profileId: Long, finalScore: Long) {
        discovery.send(MultiplayerMessage.GameEnd(profileId = profileId, finalScore = finalScore))
    }

    fun reportBye(profileId: Long) {
        discovery.send(MultiplayerMessage.Bye(profileId = profileId))
    }
}
