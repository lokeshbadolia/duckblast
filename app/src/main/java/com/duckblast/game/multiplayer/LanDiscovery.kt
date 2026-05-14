package com.duckblast.game.multiplayer

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.SocketException

/**
 * UDP multicast transport for DuckBlast multiplayer.
 *
 * Acquires a [WifiManager.MulticastLock] so packets aren't filtered while the
 * screen is on, joins the [MULTICAST_GROUP]/[MULTICAST_PORT] group, and emits
 * every parsed [MultiplayerMessage] on [incoming]. Everything is best-effort;
 * unparsable packets are dropped silently.
 */
class LanDiscovery(private val context: Context) {

    private val json: Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _incoming = MutableSharedFlow<MultiplayerMessage>(extraBufferCapacity = 128)
    val incoming: SharedFlow<MultiplayerMessage> = _incoming.asSharedFlow()

    private var socket: MulticastSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var scope: CoroutineScope? = null
    private var receiveJob: Job? = null
    private val groupAddress: InetAddress by lazy { InetAddress.getByName(MULTICAST_GROUP) }

    @Synchronized
    fun start() {
        if (socket != null) return
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        multicastLock = wifi?.createMulticastLock(MULTICAST_TAG)?.apply {
            setReferenceCounted(true)
            acquire()
        }
        runCatching {
            socket = MulticastSocket(MULTICAST_PORT).apply {
                reuseAddress = true
                @Suppress("DEPRECATION")
                joinGroup(groupAddress)
            }
        }.onFailure {
            releaseLock()
            socket = null
            return
        }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        receiveJob = scope?.launch { receiveLoop() }
    }

    @Synchronized
    fun stop() {
        receiveJob?.cancel()
        receiveJob = null
        scope?.cancel()
        scope = null
        socket?.let { s ->
            runCatching {
                @Suppress("DEPRECATION")
                s.leaveGroup(groupAddress)
            }
            runCatching { s.close() }
        }
        socket = null
        releaseLock()
    }

    fun send(message: MultiplayerMessage) {
        val active = socket ?: return
        val current = scope ?: return
        current.launch {
            try {
                val payload = json.encodeToString(MultiplayerMessage.serializer(), message).toByteArray()
                val packet = DatagramPacket(payload, payload.size, groupAddress, MULTICAST_PORT)
                active.send(packet)
            } catch (_: Exception) {
                // best-effort; LAN flakiness is fine for this game
            }
        }
    }

    private suspend fun receiveLoop() {
        val active = socket ?: return
        val buffer = ByteArray(2048)
        val packet = DatagramPacket(buffer, buffer.size)
        while (currentScope().isActive) {
            try {
                active.receive(packet)
                val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                val msg = runCatching {
                    json.decodeFromString(MultiplayerMessage.serializer(), text)
                }.getOrNull() ?: continue
                _incoming.tryEmit(msg)
            } catch (_: SocketException) {
                break
            } catch (_: InterruptedException) {
                break
            } catch (_: Exception) {
                // malformed packet — keep listening
            }
        }
    }

    private fun currentScope(): CoroutineScope = scope ?: CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun releaseLock() {
        multicastLock?.let { lock ->
            runCatching { if (lock.isHeld) lock.release() }
        }
        multicastLock = null
    }

    companion object {
        const val MULTICAST_GROUP: String = "239.255.12.99"
        const val MULTICAST_PORT: Int = 47777
        private const val MULTICAST_TAG = "duckblast-multicast"
    }
}
