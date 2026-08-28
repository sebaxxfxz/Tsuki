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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed interface TogetherClientEvent {
    data class Welcome(val welcome: ServerWelcome) : TogetherClientEvent
    data class RoomState(val state: TogetherRoomState) : TogetherClientEvent
    data class JoinDecision(val decision: com.example.tsuki.together.JoinDecision) : TogetherClientEvent
    data class HostTransferred(val transfer: com.example.tsuki.together.HostTransferred) : TogetherClientEvent
    data class ControlRequested(val request: ControlRequest) : TogetherClientEvent
    data class AddTrackRequested(val request: AddTrackRequest) : TogetherClientEvent
    data class ServerIssue(val message: String, val code: String? = null) : TogetherClientEvent
    data class Error(val message: String, val throwable: Throwable? = null) : TogetherClientEvent
    data class HeartbeatPong(val pong: com.example.tsuki.together.HeartbeatPong, val receivedAtElapsedRealtimeMs: Long) : TogetherClientEvent
    data object Disconnected : TogetherClientEvent
}

@Immutable
sealed class TogetherClientState {
    data object Idle : TogetherClientState()
    data class Connecting(val joinInfo: TogetherJoinInfo) : TogetherClientState()
    data class Connected(val session: TogetherJoinInfo) : TogetherClientState()
    data class ConnectingRemote(val wsUrl: String, val sessionId: String) : TogetherClientState()
    data class ConnectedRemote(val wsUrl: String, val sessionId: String) : TogetherClientState()
}

class TogetherClient(
    private val externalScope: CoroutineScope,
    clientId: String = UUID.randomUUID().toString(),
    private val bearerToken: String? = null
) {
    private val http = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(15, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(15, TimeUnit.SECONDS)
                pingInterval(25, TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
            }
        }
        install(WebSockets) { pingIntervalMillis = 25_000 }
    }
    private val jobScope = CoroutineScope(externalScope.coroutineContext + SupervisorJob())
    private val _state = MutableStateFlow<TogetherClientState>(TogetherClientState.Idle)
    val state = _state.asStateFlow()
    private val _events = MutableSharedFlow<TogetherClientEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()
    private var ws: WebSocketSession? = null
    private var loop: Job? = null
    private var meId: String? = null
    private val cid = clientId.trim().ifBlank { UUID.randomUUID().toString() }.take(64)
    private val tokenNorm: String? = bearerToken?.trim()?.takeIf { it.isNotEmpty() }

    fun connect(joinInfo: TogetherJoinInfo, displayName: String) {
        jobScope.launch {
            disconnect()
            _state.value = TogetherClientState.Connecting(joinInfo)
            val primary = joinInfo.toWebSocketUrl()
            val candidates = listOfNotNull(primary, altScheme(primary)).distinct()
            var last: Throwable? = null
            for (url in candidates) {
                try {
                    http.webSocket(urlString = url, request = { if (tokenNorm != null) header("Authorization", "Bearer $tokenNorm") }) {
                        ws = this
                        send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), ClientHello(TogetherProtocolVersion, joinInfo.sessionId, joinInfo.sessionKey, cid, displayName.trim())))
                        _state.value = TogetherClientState.Connected(joinInfo)
                        runLoop(this, joinInfo.sessionId)
                    }
                    return@launch
                } catch (t: Throwable) { last = t }
            }
            _events.tryEmit(TogetherClientEvent.Error(failureMsg(last), last))
            _state.value = TogetherClientState.Idle
        }
    }

    fun connect(wsUrl: String, sessionId: String, sessionKey: String, displayName: String) {
        jobScope.launch {
            disconnect()
            _state.value = TogetherClientState.ConnectingRemote(wsUrl, sessionId)
            val candidates = listOfNotNull(wsUrl.trim(), altScheme(wsUrl.trim())).distinct()
            var last: Throwable? = null
            for (url in candidates) {
                try {
                    http.webSocket(urlString = url, request = { if (tokenNorm != null) header("Authorization", "Bearer $tokenNorm") }) {
                        ws = this
                        send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), ClientHello(TogetherProtocolVersion, sessionId, sessionKey, cid, displayName.trim().ifBlank { "Guest" })))
                        _state.value = TogetherClientState.ConnectedRemote(url, sessionId)
                        runLoop(this, sessionId)
                    }
                    return@launch
                } catch (t: Throwable) { last = t }
            }
            _events.tryEmit(TogetherClientEvent.Error(failureMsg(last), last))
            _state.value = TogetherClientState.Idle
        }
    }

    private fun altScheme(url: String): String? {
        val t = url.trim()
        return when {
            t.startsWith("ws://") -> "wss://${t.removePrefix("ws://")}"
            t.startsWith("wss://") -> "ws://${t.removePrefix("wss://")}"
            else -> null
        }
    }

    private fun failureMsg(t: Throwable?): String {
        val root = generateSequence(t) { it.cause }.lastOrNull()
        val raw = root?.message?.trim().orEmpty()
        val reason = when (root) {
            is java.net.UnknownHostException -> "Server not found"
            is java.net.ConnectException -> "Connection refused"
            is java.net.SocketTimeoutException -> "Connection timed out"
            is javax.net.ssl.SSLHandshakeException -> "Secure connection failed"
            is IllegalArgumentException -> if (raw.contains("ws", true) && raw.contains("scheme", true)) "Invalid server websocket URL" else null
            else -> null
        }
        val detail = reason ?: raw.takeIf { it.isNotEmpty() }
        return if (detail == null) "Connection failed" else "Connection failed: $detail"
    }

    suspend fun disconnect() {
        loop?.cancel(); loop?.cancelAndJoin(); loop = null
        runCatching { ws?.close(CloseReason(CloseReason.Codes.NORMAL, "Disconnect")) }
        ws = null; meId = null
        _state.value = TogetherClientState.Idle
    }

    fun requestControl(sessionId: String, action: ControlAction) {
        val pid = meId ?: return
        jobScope.launch { ws?.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), ControlRequest(sessionId, pid, action))) }
    }

    fun requestAddTrack(sessionId: String, track: TogetherTrack, mode: AddTrackMode) {
        val pid = meId ?: return
        jobScope.launch { ws?.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), AddTrackRequest(sessionId, pid, track, mode))) }
    }

    fun sendRoomState(state: TogetherRoomState) {
        jobScope.launch { ws?.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), RoomStateMessage(state))) }
    }

    fun transferHostOwnership(sessionId: String, participantId: String) {
        jobScope.launch { ws?.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), HostTransfer(sessionId, participantId))) }
    }

    fun sendHeartbeat(sessionId: String, pingId: Long, clientElapsedRealtimeMs: Long) {
        jobScope.launch { ws?.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), HeartbeatPing(sessionId, pingId, clientElapsedRealtimeMs))) }
    }

    private suspend fun runLoop(session: WebSocketSession, sessionId: String) {
        loop = jobScope.launch {
            try {
                while (true) {
                    val frame = try { session.incoming.receive() } catch (_: ClosedReceiveChannelException) { break }
                    val text = (frame as? Frame.Text)?.readText() ?: continue
                    val msg = runCatching { TogetherJson.json.decodeFromString(TogetherMessage.serializer(), text) }.getOrElse {
                        _events.tryEmit(TogetherClientEvent.Error("Failed to decode message", it)); continue
                    }
                    when (msg) {
                        is ServerWelcome -> if (msg.sessionId == sessionId) { meId = msg.participantId; _events.tryEmit(TogetherClientEvent.Welcome(msg)) }
                        is RoomStateMessage -> if (msg.state.sessionId == sessionId) _events.tryEmit(TogetherClientEvent.RoomState(msg.state))
                        is com.example.tsuki.together.JoinDecision -> if (msg.sessionId == sessionId && msg.participantId == meId) _events.tryEmit(TogetherClientEvent.JoinDecision(msg))
                        is com.example.tsuki.together.HostTransferred -> if (msg.sessionId == sessionId) _events.tryEmit(TogetherClientEvent.HostTransferred(msg))
                        is KickParticipant -> if (msg.sessionId == sessionId && msg.participantId == meId) { _events.tryEmit(TogetherClientEvent.Error(msg.reason?.trim().orEmpty().ifBlank { "Kicked" })); break }
                        is BanParticipant -> if (msg.sessionId == sessionId && msg.participantId == meId) { _events.tryEmit(TogetherClientEvent.Error(msg.reason?.trim().orEmpty().ifBlank { "Banned" })); break }
                        is HeartbeatPong -> if (msg.sessionId == sessionId) _events.tryEmit(TogetherClientEvent.HeartbeatPong(msg, android.os.SystemClock.elapsedRealtime()))
                        is ControlRequest -> if (msg.sessionId == sessionId) _events.tryEmit(TogetherClientEvent.ControlRequested(msg))
                        is AddTrackRequest -> if (msg.sessionId == sessionId) _events.tryEmit(TogetherClientEvent.AddTrackRequested(msg))
                        is ServerError -> _events.tryEmit(TogetherClientEvent.ServerIssue(msg.message, msg.code))
                        else -> Unit
                    }
                }
            } catch (t: Throwable) { _events.tryEmit(TogetherClientEvent.Error("Connection loop failed", t)) }
            finally { _events.tryEmit(TogetherClientEvent.Disconnected); _state.value = TogetherClientState.Idle }
        }
        loop?.join()
    }
}
