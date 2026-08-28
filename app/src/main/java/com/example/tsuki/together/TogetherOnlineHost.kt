package com.example.tsuki.together

import androidx.compose.runtime.Immutable
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.TimeUnit

@Immutable
sealed class TogetherOnlineHostState {
    data object Idle : TogetherOnlineHostState()
    data object Connecting : TogetherOnlineHostState()
    data class Connected(val wsUrl: String, val sessionId: String, val hostParticipantId: String) : TogetherOnlineHostState()
}

class TogetherOnlineHost(
    externalScope: CoroutineScope,
    val sessionId: String,
    private val sessionKey: String,
    private val hostId: String,
    private val hostDisplayName: String,
    initialSettings: TogetherRoomSettings,
    clientId: String = UUID.randomUUID().toString(),
    private val bearerToken: String? = null
) {
    private val net = HttpClient(OkHttp) {
        engine { config { connectTimeout(15, TimeUnit.SECONDS); readTimeout(30, TimeUnit.SECONDS); writeTimeout(15, TimeUnit.SECONDS); pingInterval(25, TimeUnit.SECONDS); retryOnConnectionFailure(true) } }
        install(WebSockets) { pingIntervalMillis = 25_000 }
    }
    private val scope = CoroutineScope(externalScope.coroutineContext + SupervisorJob())
    private val lock = Mutex()
    private var cfg: TogetherRoomSettings = initialSettings
    private var ws: WebSocketSession? = null
    private var job: Job? = null
    private var mePid: String? = null
    private var authorityPid: String? = null
    private val cid = clientId.trim().ifBlank { UUID.randomUUID().toString() }.take(64)
    private val tok = bearerToken?.trim()?.takeIf { it.isNotEmpty() }

    private data class Guest(val pid: String, val cid: String, val name: String, var pending: Boolean)
    private val guests = LinkedHashMap<String, Guest>()
    @Volatile private var snapshot: List<TogetherParticipant> = emptyList()
    var onEvent: ((TogetherServerEvent) -> Unit)? = null

    suspend fun connect(wsUrl: String) {
        disconnect()
        mePid = null; authorityPid = null; guests.clear(); snapshot = emptyList()
        if (tok == null) { onEvent?.invoke(TogetherServerEvent.Error("Together token is missing")); return }
        val trimmed = wsUrl.trim()
        val trials = listOfNotNull(trimmed, alt(trimmed)).distinct()
        var last: Throwable? = null
        for (cand in trials) {
            try {
                net.webSocket(urlString = cand, request = { header("Authorization", "Bearer $tok") }) {
                    ws = this
                    send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), ClientHello(TogetherProtocolVersion, sessionId, sessionKey, cid, hostDisplayName.trim())))
                    runLoop(this, cand)
                }
                return
            } catch (t: Throwable) { last = t }
        }
        onEvent?.invoke(TogetherServerEvent.Error(failMsg(last), last))
    }

    private fun alt(url: String): String? {
        val t = url.trim()
        return when { t.startsWith("ws://") -> "wss://${t.removePrefix("ws://")}" ; t.startsWith("wss://") -> "ws://${t.removePrefix("wss://")}" ; else -> null }
    }

    private fun failMsg(t: Throwable?): String {
        val r = generateSequence(t) { it.cause }.lastOrNull()
        val raw = r?.message?.trim().orEmpty()
        val why = when (r) {
            is java.net.UnknownHostException -> "Server not found"
            is java.net.ConnectException -> "Connection refused"
            is java.net.SocketTimeoutException -> "Connection timed out"
            is javax.net.ssl.SSLHandshakeException -> "Secure connection failed"
            is IllegalArgumentException -> if (raw.contains("ws", true) && raw.contains("scheme", true)) "Invalid server websocket URL" else null
            else -> null
        }
        val d = why ?: raw.takeIf { it.isNotEmpty() }
        return if (d == null) "Connection failed" else "Connection failed: $d"
    }

    suspend fun disconnect() {
        job?.cancel(); job?.cancelAndJoin(); job = null
        runCatching { ws?.close(CloseReason(CloseReason.Codes.NORMAL, "Disconnect")) }
        ws = null; mePid = null; authorityPid = null; guests.clear(); snapshot = emptyList()
    }

    fun currentParticipants(): List<TogetherParticipant> = snapshot
    suspend fun currentSettings(): TogetherRoomSettings = lock.withLock { cfg }
    suspend fun updateSettings(s: TogetherRoomSettings) { lock.withLock { cfg = s } }

    suspend fun approveParticipant(participantId: String, approved: Boolean) {
        val g = guests[participantId] ?: return
        if (!g.pending) return
        if (!approved) {
            runCatching { ws?.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), JoinDecision(sessionId, participantId, false))) }
            g.pending = false; return
        }
        g.pending = false
        runCatching { ws?.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), JoinDecision(sessionId, participantId, true))) }
        onEvent?.invoke(TogetherServerEvent.ParticipantJoined(TogetherParticipant(participantId, g.name, false, false, true)))
        rebuild()
    }

    suspend fun kickParticipant(participantId: String, reason: String?) {
        if (!guests.containsKey(participantId)) return
        runCatching { ws?.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), KickParticipant(sessionId, participantId, reason))) }
    }

    suspend fun banParticipant(participantId: String, reason: String?) {
        if (!guests.containsKey(participantId)) return
        runCatching { ws?.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), BanParticipant(sessionId, participantId, reason))) }
    }

    suspend fun transferHostOwnership(participantId: String) {
        val g = guests[participantId] ?: return
        if (g.pending) return
        runCatching { ws?.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), HostTransfer(sessionId, participantId))) }
    }

    suspend fun broadcastRoomState(state: TogetherRoomState) {
        val s = lock.withLock { cfg }
        val host = authorityPid ?: hostId
        rebuild()
        val out = state.copy(hostId = host, settings = s, participants = snapshot)
        runCatching { ws?.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), RoomStateMessage(out))) }
    }

    private fun rebuild() {
        val h = authorityPid ?: hostId
        val hostEntry = TogetherParticipant(hostId, hostDisplayName, h == hostId, false, true)
        val gl = guests.values.sortedBy { it.name.lowercase() }.map { TogetherParticipant(it.pid, it.name, it.pid == h, it.pending, true) }
        snapshot = buildList { add(hostEntry); addAll(gl) }
    }

    private suspend fun runLoop(session: WebSocketSession, wsUrl: String) {
        job = scope.launch {
            try {
                while (true) {
                    val frame = try { session.incoming.receive() } catch (_: ClosedReceiveChannelException) { break }
                    val txt = (frame as? Frame.Text)?.readText() ?: continue
                    val msg = runCatching { TogetherJson.json.decodeFromString(TogetherMessage.serializer(), txt) }.getOrElse {
                        onEvent?.invoke(TogetherServerEvent.Error("Failed to decode message", it)); continue
                    }
                    when (msg) {
                        is ServerWelcome -> if (msg.sessionId == sessionId) { mePid = msg.participantId; lock.withLock { cfg = msg.settings }; authorityPid = hostId }
                        is JoinRequest -> if (msg.sessionId == sessionId) {
                            val p = msg.participant.copy(isHost = false, isConnected = true, isPending = true)
                            guests[p.id] = Guest(p.id, "", p.name, true); rebuild(); onEvent?.invoke(TogetherServerEvent.JoinRequested(p))
                        }
                        is ParticipantJoined -> if (msg.sessionId == sessionId) {
                            val p = msg.participant.copy(isHost = false, isConnected = true, isPending = false)
                            guests[p.id] = Guest(p.id, "", p.name, false); rebuild(); onEvent?.invoke(TogetherServerEvent.ParticipantJoined(p))
                        }
                        is ParticipantLeft -> if (msg.sessionId == sessionId) {
                            guests.remove(msg.participantId)
                            if (authorityPid == msg.participantId) authorityPid = hostId
                            rebuild(); onEvent?.invoke(TogetherServerEvent.ParticipantLeft(msg.participantId, msg.reason))
                        }
                        is RoomStateMessage -> if (msg.state.sessionId == sessionId) onEvent?.invoke(TogetherServerEvent.RoomStateReceived(msg.state))
                        is HostTransferred -> if (msg.sessionId == sessionId) { authorityPid = msg.participantId; rebuild(); onEvent?.invoke(TogetherServerEvent.HostTransferred(msg.participantId)) }
                        is ControlRequest -> if (msg.sessionId == sessionId) onEvent?.invoke(TogetherServerEvent.ControlRequested(msg))
                        is AddTrackRequest -> if (msg.sessionId == sessionId) onEvent?.invoke(TogetherServerEvent.AddTrackRequested(msg))
                        is ServerError -> onEvent?.invoke(TogetherServerEvent.Error(msg.message))
                        else -> Unit
                    }
                }
            } catch (t: Throwable) { onEvent?.invoke(TogetherServerEvent.Error("Connection loop failed", t)) }
            finally {
                mePid = null; authorityPid = null; guests.clear(); snapshot = emptyList()
                runCatching { session.close(CloseReason(CloseReason.Codes.NORMAL, "Disconnected")) }
                onEvent?.invoke(TogetherServerEvent.Error("Disconnected"))
            }
        }
        job?.join()
    }
}
