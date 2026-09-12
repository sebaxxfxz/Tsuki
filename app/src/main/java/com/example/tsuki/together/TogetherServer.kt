package com.example.tsuki.together

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

sealed interface TogetherServerEvent {
    data class JoinRequested(val participant: TogetherParticipant) : TogetherServerEvent
    data class ParticipantJoined(val participant: TogetherParticipant) : TogetherServerEvent
    data class ParticipantLeft(val participantId: String, val reason: String?) : TogetherServerEvent
    data class ControlRequested(val request: ControlRequest) : TogetherServerEvent
    data class AddTrackRequested(val request: AddTrackRequest) : TogetherServerEvent
    data class RoomStateReceived(val state: TogetherRoomState) : TogetherServerEvent
    data class HostTransferred(val participantId: String) : TogetherServerEvent
    data class Error(val message: String, val throwable: Throwable? = null) : TogetherServerEvent
}

class TogetherServer(
    private val scope: CoroutineScope,
    val sessionId: String,
    private val sessionKey: String,
    private val hostDisplayName: String,
    initialSettings: TogetherRoomSettings,
    private val hostParticipantId: String = "host"
) {
    private val guard = Mutex()
    private var cfg: TogetherRoomSettings = initialSettings
    private var ktorEngine: EmbeddedServer<*, *>? = null
    private var activeAuthorityId: String? = null

    @Volatile private var cachedParticipants: List<TogetherParticipant> = emptyList()

    private data class Conn(
        val pid: String,
        val cid: String,
        val display: String,
        val socket: WebSocketSession,
        var pending: Boolean
    )

    private val conns = ConcurrentHashMap<String, Conn>()
    var onEvent: ((TogetherServerEvent) -> Unit)? = null

    fun currentParticipants(): List<TogetherParticipant> = cachedParticipants
    suspend fun currentSettings(): TogetherRoomSettings = guard.withLock { cfg }

    suspend fun start(port: Int) {
        guard.withLock {
            if (ktorEngine != null) return
            ktorEngine = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                install(WebSockets)
                routing { webSocket("/together") { handleConn() } }
            }.also { it.start(wait = false) }
        }
    }

    suspend fun stop() {
        val eng = guard.withLock { val e = ktorEngine ?: return; ktorEngine = null; e }
        conns.values.forEach { runCatching { it.socket.close(CloseReason(CloseReason.Codes.NORMAL, "Session ended")) } }
        conns.clear()
        runCatching { eng.stop(1000, 2000) }
    }

    suspend fun updateSettings(newSettings: TogetherRoomSettings) { guard.withLock { cfg = newSettings } }

    suspend fun transferHostOwnership(participantId: String) {
        val target = conns[participantId] ?: return
        if (target.pending) return
        guard.withLock { activeAuthorityId = participantId }
        val payload = TogetherJson.json.encodeToString(TogetherMessage.serializer(), HostTransferred(sessionId, participantId))
        conns.values.forEach { runCatching { it.socket.send(payload) } }
        onEvent?.invoke(TogetherServerEvent.HostTransferred(participantId))
    }

    suspend fun approveParticipant(participantId: String, approved: Boolean) {
        val c = conns[participantId] ?: return
        if (!c.pending) return
        if (!approved) {
            runCatching { c.socket.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), JoinDecision(sessionId, participantId, false))) }
            runCatching { c.socket.close(CloseReason(CloseReason.Codes.NORMAL, "Not approved")) }
            conns.remove(participantId)
            onEvent?.invoke(TogetherServerEvent.ParticipantLeft(participantId, "Not approved"))
            return
        }
        c.pending = false
        runCatching { c.socket.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), JoinDecision(sessionId, participantId, true))) }
        onEvent?.invoke(TogetherServerEvent.ParticipantJoined(TogetherParticipant(participantId, c.display, false, false, true)))
    }

    suspend fun broadcastRoomState(state: TogetherRoomState) {
        val (snapSettings, hostId) = guard.withLock { cfg to activeAuthorityId }
        val effectiveHost = hostId ?: hostParticipantId
        val hostEntry = TogetherParticipant(hostParticipantId, hostDisplayName, effectiveHost == hostParticipantId, false, true)
        val list = buildList {
            add(hostEntry)
            addAll(conns.values.sortedBy { it.display.lowercase() }.map {
                TogetherParticipant(it.pid, it.display, it.pid == effectiveHost, it.pending, true)
            })
        }
        cachedParticipants = list
        val base = state.copy(hostId = effectiveHost, participants = list, settings = snapSettings)
        conns.values.forEach { conn ->
            val out = if (conn.pending) base.copy(queue = emptyList(), queueHash = "", currentIndex = 0, isPlaying = false, positionMs = 0L) else base
            runCatching { conn.socket.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), RoomStateMessage(out))) }
        }
    }

    private suspend fun relayToAuthority(msg: TogetherMessage): Boolean {
        val aid = guard.withLock { activeAuthorityId } ?: return false
        val auth = conns[aid] ?: return false
        if (auth.pending) return false
        return runCatching { auth.socket.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), msg)) }.isSuccess
    }

    private suspend fun WebSocketSession.handleConn() {
        val first = try { incoming.receive() as? Frame.Text } catch (_: ClosedReceiveChannelException) { null }
        val hello = runCatching {
            val t = first?.readText().orEmpty()
            TogetherJson.json.decodeFromString(TogetherMessage.serializer(), t) as? ClientHello
        }.getOrNull()
        if (hello == null) { close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Handshake required")); return }
        if (hello.protocolVersion != TogetherProtocolVersion) {
            send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), ServerError(hello.sessionId, "Unsupported protocol version")))
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Unsupported protocol")); return
        }
        if (hello.sessionId != sessionId || hello.sessionKey != sessionKey) {
            send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), ServerError(hello.sessionId, "Invalid session")))
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Invalid session")); return
        }
        val pid = UUID.randomUUID().toString()
        val pendingFlag = guard.withLock { cfg.requireHostApprovalToJoin }
        val conn = Conn(pid, hello.clientId, hello.displayName.trim().ifBlank { "Guest" }, this, pendingFlag)
        conns[pid] = conn
        send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), ServerWelcome(TogetherProtocolVersion, sessionId, pid, ServerRole.GUEST, pendingFlag, guard.withLock { cfg })))
        val part = TogetherParticipant(pid, conn.display, false, pendingFlag, true)
        onEvent?.invoke(if (pendingFlag) TogetherServerEvent.JoinRequested(part) else TogetherServerEvent.ParticipantJoined(part))
        try {
            for (frame in incoming) {
                val txt = (frame as? Frame.Text)?.readText() ?: continue
                val msg = runCatching { TogetherJson.json.decodeFromString(TogetherMessage.serializer(), txt) }.getOrElse {
                    onEvent?.invoke(TogetherServerEvent.Error("Failed to decode message", it)); continue
                }
                when (msg) {
                    is RoomStateMessage -> {
                        val isAuth = guard.withLock { activeAuthorityId == pid }
                        if (isAuth && msg.state.sessionId == sessionId) {
                            val rs = msg.state.copy(hostId = pid)
                            broadcastRoomState(rs)
                            onEvent?.invoke(TogetherServerEvent.RoomStateReceived(rs))
                        }
                    }
                    is HeartbeatPing -> send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), HeartbeatPong(sessionId, msg.pingId, msg.clientElapsedRealtimeMs, android.os.SystemClock.elapsedRealtime())))
                    is ControlRequest -> if (!conn.pending && !relayToAuthority(msg)) onEvent?.invoke(TogetherServerEvent.ControlRequested(msg))
                    is AddTrackRequest -> if (!conn.pending && !relayToAuthority(msg)) onEvent?.invoke(TogetherServerEvent.AddTrackRequested(msg))
                    is ClientLeave -> if (msg.participantId == pid) { close(CloseReason(CloseReason.Codes.NORMAL, "Left")); break }
                    is HostTransfer -> {
                        val can = guard.withLock { activeAuthorityId == pid }
                        if (can && msg.sessionId == sessionId) transferHostOwnership(msg.participantId)
                    }
                    else -> Unit
                }
            }
        } catch (t: Throwable) { onEvent?.invoke(TogetherServerEvent.Error("Client loop failed", t)) }
        finally {
            conns.remove(pid)
            guard.withLock {
                if (activeAuthorityId == pid) {
                    activeAuthorityId = null
                }
            }
            onEvent?.invoke(TogetherServerEvent.ParticipantLeft(pid, "Disconnected"))
            runCatching { close() }
        }
    }
}
