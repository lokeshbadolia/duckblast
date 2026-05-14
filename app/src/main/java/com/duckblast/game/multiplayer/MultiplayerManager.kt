package com.duckblast.game.multiplayer

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Singleton facade injected via Koin. Owns the [LanDiscovery] transport, the
 * host/client wrappers, and the canonical view of the lobby.
 *
 * Lifecycle: [startHost] / [startClient] opens the multicast session and
 * starts heartbeats. [leaveSession] tears everything down — call it from the
 * last ViewModel that needs the session (typically [com.duckblast.game.ui.game.GameViewModel.onCleared]).
 */
class MultiplayerManager(context: Context) {

    private val appContext = context.applicationContext
    private val discovery = LanDiscovery(appContext)
    val server = GameServer(discovery)
    val client = GameClient(discovery)

    data class RemotePlayer(
        val profileId: Long,
        val name: String,
        val ip: String,
        val isHost: Boolean,
        val score: Long = 0L,
        val level: Int = 0,
        val isAlive: Boolean = true,
        val lastSeenMs: Long = System.currentTimeMillis()
    )

    private val _players = MutableStateFlow<List<RemotePlayer>>(emptyList())
    val players: StateFlow<List<RemotePlayer>> = _players.asStateFlow()

    private val _gameStart = MutableSharedFlow<Long>(extraBufferCapacity = 4)
    val gameStart: SharedFlow<Long> = _gameStart.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var heartbeatJob: Job? = null
    private var pruneJob: Job? = null
    private var collectJob: Job? = null

    var selfProfileId: Long = 0L
        private set
    var selfName: String = ""
        private set
    var selfIp: String = ""
        private set
    var isHost: Boolean = false
        private set
    private var active: Boolean = false

    fun startHost(profileId: Long, name: String) {
        beginSession(profileId, name, host = true)
    }

    fun startClient(profileId: Long, name: String) {
        beginSession(profileId, name, host = false)
    }

    @Synchronized
    private fun beginSession(profileId: Long, name: String, host: Boolean) {
        if (active) return
        selfProfileId = profileId
        selfName = name
        selfIp = resolveLocalIp()
        isHost = host
        active = true
        discovery.start()
        collectJob = scope.launch {
            discovery.incoming.collect { msg -> handleMessage(msg) }
        }
        heartbeatJob = scope.launch {
            while (isActive) {
                client.announce(selfName, selfProfileId, selfIp, isHost)
                delay(1000L)
            }
        }
        pruneJob = scope.launch {
            while (isActive) {
                delay(2000L)
                val cutoff = System.currentTimeMillis() - 5000L
                _players.update { list ->
                    list.filter { it.lastSeenMs > cutoff && it.profileId != selfProfileId }
                }
            }
        }
    }

    private fun handleMessage(msg: MultiplayerMessage) {
        when (msg) {
            is MultiplayerMessage.Hello -> upsertHello(msg)
            is MultiplayerMessage.StartGame -> _gameStart.tryEmit(msg.seed)
            is MultiplayerMessage.ScoreUpdate -> applyScore(msg)
            is MultiplayerMessage.GameEnd -> applyGameEnd(msg)
            is MultiplayerMessage.Bye -> _players.update { list -> list.filter { it.profileId != msg.profileId } }
        }
    }

    private fun upsertHello(msg: MultiplayerMessage.Hello) {
        if (msg.profileId == selfProfileId) return
        val now = System.currentTimeMillis()
        _players.update { list ->
            val existing = list.firstOrNull { it.profileId == msg.profileId }
            if (existing != null) {
                list.map {
                    if (it.profileId == msg.profileId) it.copy(
                        name = msg.name,
                        ip = msg.ip,
                        isHost = msg.isHost,
                        lastSeenMs = now
                    ) else it
                }
            } else {
                list + RemotePlayer(
                    profileId = msg.profileId,
                    name = msg.name,
                    ip = msg.ip,
                    isHost = msg.isHost,
                    lastSeenMs = now
                )
            }
        }
    }

    private fun applyScore(msg: MultiplayerMessage.ScoreUpdate) {
        if (msg.profileId == selfProfileId) return
        val now = System.currentTimeMillis()
        _players.update { list ->
            val existing = list.firstOrNull { it.profileId == msg.profileId }
            if (existing != null) {
                list.map {
                    if (it.profileId == msg.profileId) it.copy(
                        score = msg.score,
                        level = msg.level,
                        isAlive = msg.isAlive,
                        lastSeenMs = now
                    ) else it
                }
            } else {
                list + RemotePlayer(
                    profileId = msg.profileId,
                    name = "Player ${msg.profileId}",
                    ip = "",
                    isHost = false,
                    score = msg.score,
                    level = msg.level,
                    isAlive = msg.isAlive,
                    lastSeenMs = now
                )
            }
        }
    }

    private fun applyGameEnd(msg: MultiplayerMessage.GameEnd) {
        if (msg.profileId == selfProfileId) return
        _players.update { list ->
            list.map {
                if (it.profileId == msg.profileId) it.copy(
                    score = msg.finalScore,
                    isAlive = false,
                    lastSeenMs = System.currentTimeMillis()
                ) else it
            }
        }
    }

    fun startGame(seed: Long) {
        if (!isHost) return
        server.broadcastStart(seed)
    }

    fun publishScore(score: Long, level: Int, alive: Boolean) {
        if (selfProfileId == 0L || !active) return
        server.broadcastScore(selfProfileId, score, level, alive)
    }

    fun publishGameEnd(finalScore: Long) {
        if (selfProfileId == 0L || !active) return
        client.reportGameEnd(selfProfileId, finalScore)
    }

    @Synchronized
    fun leaveSession() {
        if (!active) return
        runCatching { client.reportBye(selfProfileId) }
        heartbeatJob?.cancel(); heartbeatJob = null
        pruneJob?.cancel(); pruneJob = null
        collectJob?.cancel(); collectJob = null
        discovery.stop()
        _players.value = emptyList()
        isHost = false
        active = false
    }

    private fun resolveLocalIp(): String {
        return runCatching {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
                ?.hostAddress ?: "127.0.0.1"
        }.getOrElse { "127.0.0.1" }
    }
}
