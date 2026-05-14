package com.duckblast.game.multiplayer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format for all LAN multiplayer traffic. The sealed hierarchy uses
 * kotlinx.serialization's polymorphic JSON support with `"type"` as the
 * discriminator field.
 */
@Serializable
sealed class MultiplayerMessage {

    /** Heartbeat advertising a hunter on the LAN. Sent every ~1 s. */
    @Serializable
    @SerialName("hello")
    data class Hello(
        val name: String,
        val profileId: Long,
        val ip: String,
        val isHost: Boolean = false
    ) : MultiplayerMessage()

    /** Host → all clients: kick off the run with a shared RNG seed. */
    @Serializable
    @SerialName("start")
    data class StartGame(
        val seed: Long,
        val countdownSec: Int = 3
    ) : MultiplayerMessage()

    /** Periodic live-score broadcast while playing. */
    @Serializable
    @SerialName("score")
    data class ScoreUpdate(
        val profileId: Long,
        val score: Long,
        val level: Int,
        val isAlive: Boolean
    ) : MultiplayerMessage()

    /** Player ran out of lives — their final tally. */
    @Serializable
    @SerialName("end")
    data class GameEnd(
        val profileId: Long,
        val finalScore: Long
    ) : MultiplayerMessage()

    /** Graceful leave — let everyone else prune us from the lobby. */
    @Serializable
    @SerialName("bye")
    data class Bye(val profileId: Long) : MultiplayerMessage()
}
