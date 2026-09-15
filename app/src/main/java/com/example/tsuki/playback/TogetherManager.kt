package com.example.tsuki.playback

import android.content.Context
import java.math.BigInteger
import java.net.NetworkInterface
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.example.tsuki.R
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.together.AddTrackMode
import com.example.tsuki.together.ControlAction
import com.example.tsuki.together.HostTransferred
import com.example.tsuki.together.TogetherClient
import com.example.tsuki.together.TogetherClientEvent
import com.example.tsuki.together.TogetherClock
import com.example.tsuki.together.TogetherJoinInfo
import com.example.tsuki.together.TogetherLink
import com.example.tsuki.together.TogetherOnlineApi
import com.example.tsuki.together.TogetherOnlineEndpoint
import com.example.tsuki.together.TogetherOnlineApiException
import com.example.tsuki.together.TogetherOnlineHost
import com.example.tsuki.together.TogetherParticipant
import com.example.tsuki.together.TogetherPlaybackSync
import com.example.tsuki.together.TogetherRole
import com.example.tsuki.together.TogetherRoomSettings
import com.example.tsuki.together.TogetherRoomState
import com.example.tsuki.together.TogetherServer
import com.example.tsuki.together.TogetherServerEvent
import com.example.tsuki.together.TogetherSessionState
import com.example.tsuki.together.TogetherTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@UnstableApi
class TogetherManager(private val context: Context, private val controller: PlayerController) {

    companion object {
        private const val TAG = "TogetherManager"
        const val HOST_ID = "host"
        private const val ONLINE_TIMEOUT_MS = 5000L
        private const val LAN_TIMEOUT_MS = 2000L
    }

    val sessionState = MutableStateFlow<TogetherSessionState>(TogetherSessionState.Idle)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var server: TogetherServer? = null
    private var onlineHost: TogetherOnlineHost? = null
    private var client: TogetherClient? = null
    private var broadcastJob: Job? = null
    private var onlineConnectJob: Job? = null
    private var clientEventsJob: Job? = null
    private var heartbeatJob: Job? = null
    private var clock: TogetherClock? = null
    @Volatile private var selfParticipantId: String? = null
    @Volatile private var authorityParticipantId: String? = null
    @Volatile private var isOnlineSession: Boolean = false
    @Volatile private var applyingRemote: Boolean = false
    @Volatile private var suppressEchoUntilElapsedMs: Long = 0L
    @Volatile private var lastAppliedRoomStateSentAtElapsedMs: Long = 0L
    private var lastRemoteAppliedPlayWhenReady: Boolean? = null
    private var lastRemoteAppliedIndex: Int = -1
    private var lastSentControlAtElapsedMs: Long = 0L
    private var lastSentControlAction: ControlAction? = null
    @Volatile private var pendingGuestControl: PendingGuestControl? = null
    private val participantNames = ConcurrentHashMap<String, String>()
    private var lastNoticeAtElapsedMs: Long = 0L
    private var lastNoticeKey: String? = null

    private class PendingGuestControl(
        val desiredIsPlaying: Boolean? = null,
        val desiredIndex: Int? = null,
        val desiredTrackId: String? = null,
        val requestedAtElapsedMs: Long,
        val expiresAtElapsedMs: Long,
    )

    fun isActive(): Boolean = sessionState.value !is TogetherSessionState.Idle

    fun isGuest(): Boolean {
        val joined = sessionState.value as? TogetherSessionState.Joined
        return joined?.role is TogetherRole.Guest
    }

    fun canGuestControl(): Boolean {
        val joined = sessionState.value as? TogetherSessionState.Joined ?: return false
        return joined.role is TogetherRole.Guest &&
            joined.roomState.settings.allowGuestsToControlPlayback &&
            authorityParticipantId != selfParticipantId
    }

    fun canGuestAdd(): Boolean {
        val joined = sessionState.value as? TogetherSessionState.Joined ?: return false
        return joined.role is TogetherRole.Guest && joined.roomState.settings.allowGuestsToAddTracks
    }

    fun shouldSuppressLocalPlaybackAdvance(): Boolean = isGuest()

    private fun showNotice(message: String, key: String) {
        val now = SystemClock.elapsedRealtime()
        if (key == lastNoticeKey && now - lastNoticeAtElapsedMs < 4000L) return
        lastNoticeAtElapsedMs = now
        lastNoticeKey = key
        controller.postToast(message)
    }

    private fun tokenOrNull(): String? =
        runCatching { com.example.tsuki.BuildConfig.TOGETHER_BEARER_TOKEN }
            .getOrNull()?.trim()?.takeIf { it.isNotBlank() }

    private fun getOrCreateClientId(): String {
        val prefs = context.getSharedPreferences("together_prefs", Context.MODE_PRIVATE)
        val existing = prefs.getString("client_id", null)?.trim().orEmpty()
        if (existing.isNotBlank()) return existing
        val generated = UUID.randomUUID().toString()
        prefs.edit().putString("client_id", generated).apply()
        return generated
    }

    private fun getLocalIpv4Address(): String? =
        runCatching {
            val all = NetworkInterface.getNetworkInterfaces().toList()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList().asSequence() }
                .filterIsInstance<java.net.Inet4Address>()
                .mapNotNull { it.hostAddress }
                .filter { it.isNotBlank() && it != "127.0.0.1" }
                .toList()
            all.firstOrNull { it.startsWith("192.168.") || it.startsWith("10.") || it.startsWith("172.") }
                ?: all.firstOrNull()
        }.getOrNull()

    private fun togetherOnlineErrorMessage(t: Throwable): String {
        if (t is TogetherOnlineApiException) {
            val code = t.statusCode
            return when {
                code == 404 -> context.getString(R.string.tm_session_gone)
                code != null && code in 500..599 -> "Error del servidor"
                else -> t.message ?: context.getString(R.string.tm_no_network)
            }
        }
        return when (t) {
            is java.net.UnknownHostException -> "Servidor no accesible"
            is java.net.ConnectException -> "Servidor no accesible"
            is java.net.SocketTimeoutException -> context.getString(R.string.tm_timeout)
            else -> t.message ?: context.getString(R.string.tm_no_network)
        }
    }

    fun startTogetherHost(
        port: Int,
        displayName: String,
        settings: TogetherRoomSettings,
    ) {
        ioScope.launch {
            stopInternal()
            isOnlineSession = false

            val localIp = getLocalIpv4Address()
            val sessionId = UUID.randomUUID().toString()
            val sessionKey = UUID.randomUUID().toString()
            val joinInfo = TogetherJoinInfo(
                host = localIp ?: "127.0.0.1",
                port = port,
                sessionId = sessionId,
                sessionKey = sessionKey,
            )
            val joinLink = TogetherLink.encode(joinInfo)

            val srv = TogetherServer(
                scope = ioScope,
                sessionId = sessionId,
                sessionKey = sessionKey,
                hostDisplayName = displayName.trim().ifBlank { "TSuki" },
                initialSettings = settings,
                hostParticipantId = HOST_ID,
            )

            srv.onEvent = { event ->
                ioScope.launch { handleHostEvent(event) { srv.currentSettings() } }
            }

            runCatching { srv.start(port) }
                .onFailure {
                    Log.w(TAG, "LAN server failed: ${it.message}")
                    sessionState.value = TogetherSessionState.Error(
                        message = "No se pudo iniciar el servidor en el puerto $port",
                        recoverable = true,
                    )
                    return@launch
                }
            server = srv

            scope.launch {
                sessionState.value = TogetherSessionState.Hosting(
                    sessionId = sessionId,
                    joinLink = joinLink,
                    localAddressHint = localIp,
                    port = port,
                    settings = settings,
                    roomState = null,
                )
            }

            broadcastJob?.cancel()
            broadcastJob = ioScope.launch {
                while (server === srv) {
                    if (authorityParticipantId == null || authorityParticipantId == HOST_ID) {
                        val state = buildRoomState(
                            sessionId = sessionId,
                            hostId = HOST_ID,
                            settings = srv.currentSettings(),
                            participants = srv.currentParticipants(),
                        )
                        srv.broadcastRoomState(state)
                        scope.launch {
                            val hosting = sessionState.value as? TogetherSessionState.Hosting
                            if (hosting?.sessionId == sessionId) {
                                sessionState.value = hosting.copy(
                                    settings = srv.currentSettings(),
                                    roomState = state.copy(
                                        participants = srv.currentParticipants(),
                                        settings = srv.currentSettings(),
                                    ),
                                )
                            }
                        }
                    }
                    delay(TogetherPlaybackSync.BroadcastIntervalMs)
                }
            }
        }
    }

    fun startTogetherOnlineHost(
        displayName: String,
        settings: TogetherRoomSettings,
    ) {
        ioScope.launch {
            stopInternal()
            isOnlineSession = true

            val baseUrl = TogetherOnlineEndpoint.baseUrlOrNull(context) ?: run {
                sessionState.value = TogetherSessionState.Error("Servidor online no configurado", true)
                return@launch
            }
            val token = tokenOrNull() ?: run {
                sessionState.value = TogetherSessionState.Error(
                    "Falta TOGETHER_BEARER_TOKEN en local.properties para el modo online",
                    true,
                )
                return@launch
            }

            val api = TogetherOnlineApi(baseUrl = baseUrl, bearerToken = token)
            val hostName = displayName.trim().ifBlank { "TSuki" }

            val created = runCatching { api.createSession(hostDisplayName = hostName, settings = settings) }
                .getOrElse {
                    sessionState.value = TogetherSessionState.Error(togetherOnlineErrorMessage(it), true)
                    return@launch
                }

            val host = TogetherOnlineHost(
                externalScope = ioScope,
                sessionId = created.sessionId,
                sessionKey = created.hostKey,
                hostId = HOST_ID,
                hostDisplayName = hostName,
                initialSettings = created.settings,
                clientId = getOrCreateClientId(),
                bearerToken = token,
            )

            host.onEvent = { event ->
                ioScope.launch { handleHostEvent(event) { host.currentSettings() } }
            }

            onlineHost = host

            scope.launch {
                sessionState.value = TogetherSessionState.HostingOnline(
                    sessionId = created.sessionId,
                    code = created.code,
                    settings = created.settings,
                    roomState = null,
                )
            }

            val wsUrl = TogetherOnlineEndpoint.onlineWebSocketUrlOrNull(created.wsUrl, baseUrl)
            if (wsUrl == null) {
                sessionState.value = TogetherSessionState.Error(context.getString(R.string.tm_bad_ws), true)
                ioScope.launch { stopInternal() }
                return@launch
            }

            onlineConnectJob?.cancel()
            onlineConnectJob = ioScope.launch { host.connect(wsUrl) }

            broadcastJob?.cancel()
            broadcastJob = ioScope.launch {
                while (onlineHost === host) {
                    val state =
                        if (authorityParticipantId == null || authorityParticipantId == HOST_ID) {
                            buildRoomState(
                                sessionId = created.sessionId,
                                hostId = HOST_ID,
                                settings = host.currentSettings(),
                                participants = host.currentParticipants(),
                            )
                        } else {
                            null
                        }
                    if (state != null) {
                        host.broadcastRoomState(state)
                        scope.launch {
                            val hosting = sessionState.value as? TogetherSessionState.HostingOnline
                            if (hosting?.sessionId == created.sessionId) {
                                val currentSettings = host.currentSettings()
                                sessionState.value = hosting.copy(
                                    settings = currentSettings,
                                    roomState = state.copy(
                                        participants = host.currentParticipants(),
                                        settings = currentSettings,
                                    ),
                                )
                            }
                        }
                    }
                    delay(TogetherPlaybackSync.BroadcastIntervalMs)
                }
            }
        }
    }

    fun joinTogether(rawLink: String, displayName: String) {
        val joinInfo = TogetherLink.decode(rawLink)
        if (joinInfo == null) {
            sessionState.value = TogetherSessionState.Error(context.getString(R.string.tm_bad_link), true)
            return
        }

        scope.launch { sessionState.value = TogetherSessionState.Joining(joinInfo.toDeepLink()) }

        ioScope.launch {
            stopInternal()
            isOnlineSession = false
            val c = TogetherClient(ioScope, clientId = getOrCreateClientId())
            client = c
            clock = TogetherClock()
            selfParticipantId = null

            clientEventsJob?.cancel()
            clientEventsJob = ioScope.launch {
                c.events.collect { event -> handleClientEvent(event, joinInfo.sessionId, displayName) }
            }

            c.connect(joinInfo, displayName.trim().ifBlank { "Invitado" })
        }
    }

    fun joinTogetherOnline(code: String, displayName: String) {
        val trimmedCode = code.trim()
        if (trimmedCode.isBlank()) {
            sessionState.value = TogetherSessionState.Error(context.getString(R.string.tm_bad_code), true)
            return
        }

        scope.launch { sessionState.value = TogetherSessionState.JoiningOnline(trimmedCode) }

        ioScope.launch {
            stopInternal()
            isOnlineSession = true

            val baseUrl = TogetherOnlineEndpoint.baseUrlOrNull(context) ?: run {
                sessionState.value = TogetherSessionState.Error("Servidor online no configurado", true)
                return@launch
            }
            val token = tokenOrNull() ?: run {
                sessionState.value = TogetherSessionState.Error(
                    "Falta TOGETHER_BEARER_TOKEN en local.properties para el modo online",
                    true,
                )
                return@launch
            }

            val api = TogetherOnlineApi(baseUrl = baseUrl, bearerToken = token)
            val resolved = runCatching { api.resolveCode(trimmedCode) }
                .getOrElse {
                    sessionState.value = TogetherSessionState.Error(togetherOnlineErrorMessage(it), true)
                    return@launch
                }

            val c = TogetherClient(ioScope, clientId = getOrCreateClientId(), bearerToken = token)
            client = c
            clock = TogetherClock()
            selfParticipantId = null

            clientEventsJob?.cancel()
            clientEventsJob = ioScope.launch {
                c.events.collect { event -> handleClientEvent(event, resolved.sessionId, displayName) }
            }

            val wsUrl = TogetherOnlineEndpoint.onlineWebSocketUrlOrNull(resolved.wsUrl, baseUrl)
            if (wsUrl == null) {
                sessionState.value = TogetherSessionState.Error(context.getString(R.string.tm_bad_ws), true)
                ioScope.launch { stopInternal() }
                return@launch
            }

            c.connect(
                wsUrl = wsUrl,
                sessionId = resolved.sessionId,
                sessionKey = resolved.guestKey,
                displayName = displayName.trim().ifBlank { "Invitado" },
            )
        }
    }

    private suspend fun handleClientEvent(
        event: TogetherClientEvent,
        sessionId: String,
        displayName: String,
    ) {
        when (event) {
            is TogetherClientEvent.Welcome -> {
                selfParticipantId = event.welcome.participantId
                scope.launch {
                    val selfName = displayName.trim().ifBlank { "Invitado" }
                    val initial = TogetherRoomState(
                        sessionId = sessionId,
                        hostId = HOST_ID,
                        participants = listOf(
                            TogetherParticipant(
                                id = event.welcome.participantId,
                                name = selfName,
                                isHost = false,
                                isPending = event.welcome.isPending,
                                isConnected = true,
                            ),
                        ),
                        settings = event.welcome.settings,
                        queue = emptyList(),
                        queueHash = "",
                        currentIndex = 0,
                        isPlaying = false,
                        positionMs = 0L,
                        repeatMode = 0,
                        shuffleEnabled = false,
                        sentAtElapsedRealtimeMs = SystemClock.elapsedRealtime(),
                    )
                    sessionState.value = TogetherSessionState.Joined(
                        role = TogetherRole.Guest,
                        sessionId = sessionId,
                        selfParticipantId = event.welcome.participantId,
                        roomState = initial,
                    )
                }
                startHeartbeatLoop(sessionId)
            }

            is TogetherClientEvent.RoomState -> applyRemoteRoomState(event.state)

            is TogetherClientEvent.HostTransferred -> handleClientHostTransferred(event.transfer)

            is TogetherClientEvent.ControlRequested -> {
                val joined = sessionState.value as? TogetherSessionState.Joined
                if (authorityParticipantId == selfParticipantId &&
                    joined?.roomState?.settings?.allowGuestsToControlPlayback == true
                ) {
                    applyHostControl(event.request.action)
                }
            }

            is TogetherClientEvent.AddTrackRequested -> {
                val joined = sessionState.value as? TogetherSessionState.Joined
                if (authorityParticipantId == selfParticipantId &&
                    joined?.roomState?.settings?.allowGuestsToAddTracks == true
                ) {
                    applyHostAddTrack(event.request.track, event.request.mode)
                }
            }

            is TogetherClientEvent.JoinDecision -> {
                if (!event.decision.approved) {
                    sessionState.value = TogetherSessionState.Error("No autorizado", true)
                    ioScope.launch { stopInternal() }
                }
            }

            is TogetherClientEvent.ServerIssue -> {
                when (event.code) {
                    "GUEST_CONTROL_DISABLED" -> {
                        showNotice(context.getString(R.string.tm_guest_control), "GUEST_CONTROL_DISABLED")
                        val joined = sessionState.value as? TogetherSessionState.Joined
                        if (joined?.role is TogetherRole.Guest) {
                            pendingGuestControl = null
                            lastSentControlAction = null
                            applyRemoteRoomState(joined.roomState, force = true)
                        }
                    }

                    "GUEST_ADD_DISABLED" -> showNotice(context.getString(R.string.tm_guest_add), "GUEST_ADD_DISABLED")

                    "HOST_OFFLINE" -> showNotice(context.getString(R.string.tm_host_left), "HOST_OFFLINE")

                    else -> {
                        sessionState.value = TogetherSessionState.Error(event.message, true)
                        ioScope.launch { stopInternal() }
                    }
                }
            }

            is TogetherClientEvent.HeartbeatPong -> {
                val clk = clock ?: return
                clk.onPong(
                    sentAtElapsedMs = event.pong.clientElapsedRealtimeMs,
                    receivedAtElapsedMs = event.receivedAtElapsedRealtimeMs,
                    serverElapsedMs = event.pong.serverElapsedRealtimeMs,
                )
            }

            is TogetherClientEvent.Error -> {
                sessionState.value = TogetherSessionState.Error(event.message, true)
                ioScope.launch { stopInternal() }
            }

            TogetherClientEvent.Disconnected -> {
                if (sessionState.value is TogetherSessionState.Idle) return
                sessionState.value = TogetherSessionState.Error(
                    message = if (isGuest()) context.getString(R.string.tm_host_left) else context.getString(R.string.tm_no_network),
                    recoverable = true,
                )
                ioScope.launch { stopInternal() }
            }
        }
    }

    private fun startHeartbeatLoop(sessionId: String) {
        heartbeatJob?.cancel()
        heartbeatJob = ioScope.launch {
            var pingId = 0L
            while (client != null && isActive()) {
                client?.sendHeartbeat(sessionId = sessionId, pingId = pingId++, clientElapsedRealtimeMs = SystemClock.elapsedRealtime())
                delay(2000)
            }
        }
    }

    fun leaveTogether() {
        scope.launch { sessionState.value = TogetherSessionState.Idle }
        ioScope.launch { stopInternal() }
    }

    fun updateTogetherSettings(settings: TogetherRoomSettings) {
        val srv = server
        val oh = onlineHost
        if (srv == null && oh == null) return
        ioScope.launch {
            srv?.updateSettings(settings)
            oh?.updateSettings(settings)
        }
    }

    fun approveTogetherParticipant(participantId: String, approved: Boolean) {
        val srv = server
        val oh = onlineHost
        if (srv == null && oh == null) return
        ioScope.launch {
            srv?.approveParticipant(participantId, approved)
            oh?.approveParticipant(participantId, approved)
        }
    }

    fun kickTogetherParticipant(participantId: String, reason: String? = null) {
        val oh = onlineHost ?: return
        ioScope.launch { oh.kickParticipant(participantId, reason) }
    }

    fun banTogetherParticipant(participantId: String, reason: String? = null) {
        val oh = onlineHost ?: return
        ioScope.launch { oh.banParticipant(participantId, reason) }
    }

    fun transferTogetherHostOwnership(participantId: String) {
        val targetId = participantId.trim()
        if (targetId.isBlank() || targetId == HOST_ID || targetId == selfParticipantId) return
        val srv = server
        val oh = onlineHost
        val c = client
        val joined = sessionState.value as? TogetherSessionState.Joined
        ioScope.launch {
            when {
                srv != null -> srv.transferHostOwnership(targetId)
                oh != null -> oh.transferHostOwnership(targetId)
                joined?.role is TogetherRole.Host && c != null -> c.transferHostOwnership(joined.sessionId, targetId)
            }
        }
    }

    fun requestControl(action: ControlAction) {
        val c = client ?: return
        val state = sessionState.value as? TogetherSessionState.Joined ?: return
        if (state.role !is TogetherRole.Guest) return
        if (!state.roomState.settings.allowGuestsToControlPlayback) {
            showNotice(context.getString(R.string.tm_guest_control), "GUEST_CONTROL_DISABLED_LOCAL")
            return
        }
        val now = SystemClock.elapsedRealtime()
        val lastAction = lastSentControlAction
        val lastAt = lastSentControlAtElapsedMs
        if (lastAction == action && now - lastAt < 350L) return
        lastSentControlAction = action
        lastSentControlAtElapsedMs = now

        val timeout = if (isOnlineSession) ONLINE_TIMEOUT_MS else LAN_TIMEOUT_MS
        pendingGuestControl = when (action) {
            ControlAction.Play -> PendingGuestControl(desiredIsPlaying = true, requestedAtElapsedMs = now, expiresAtElapsedMs = now + timeout)
            ControlAction.Pause -> PendingGuestControl(desiredIsPlaying = false, requestedAtElapsedMs = now, expiresAtElapsedMs = now + timeout)
            is ControlAction.SeekToIndex -> PendingGuestControl(desiredIndex = action.index.coerceAtLeast(0), requestedAtElapsedMs = now, expiresAtElapsedMs = now + timeout)
            is ControlAction.SeekToTrack -> PendingGuestControl(desiredTrackId = action.trackId.trim().ifBlank { null }, requestedAtElapsedMs = now, expiresAtElapsedMs = now + timeout)
            else -> pendingGuestControl
        }
        c.requestControl(state.sessionId, action)
    }

    fun requestAddTrack(track: TogetherTrack, mode: AddTrackMode) {
        val c = client ?: return
        val state = sessionState.value as? TogetherSessionState.Joined ?: return
        if (state.role !is TogetherRole.Guest) return
        if (!state.roomState.settings.allowGuestsToAddTracks) {
            showNotice(context.getString(R.string.tm_guest_add), "GUEST_ADD_DISABLED_LOCAL")
            return
        }
        c.requestAddTrack(state.sessionId, track, mode)
    }

    private suspend fun handleHostEvent(
        event: TogetherServerEvent,
        currentSettings: suspend () -> TogetherRoomSettings,
    ) {
        when (event) {
            is TogetherServerEvent.ControlRequested -> {
                if (!currentSettings().allowGuestsToControlPlayback) return
                applyHostControl(event.request.action)
            }

            is TogetherServerEvent.AddTrackRequested -> {
                if (!currentSettings().allowGuestsToAddTracks) return
                applyHostAddTrack(event.request.track, event.request.mode)
            }

            is TogetherServerEvent.ParticipantJoined -> {
                val participant = event.participant
                if (!participant.isHost && !participant.isPending) {
                    participantNames[participant.id] = participant.name
                    showNotice(context.getString(R.string.tm_joined, participant.name), "JOIN_${participant.id}")
                }
            }

            is TogetherServerEvent.ParticipantLeft -> {
                val name = participantNames.remove(event.participantId) ?: return
                showNotice(context.getString(R.string.tm_left, name), "LEAVE_${event.participantId}")
            }

            is TogetherServerEvent.HostTransferred -> handleHostTransferred(event.participantId)

            is TogetherServerEvent.RoomStateReceived -> {
                if (event.state.hostId != HOST_ID) {
                    selfParticipantId = HOST_ID
                    applyRemoteRoomState(event.state, force = true)
                }
            }

            is TogetherServerEvent.Error -> {
                if (sessionState.value is TogetherSessionState.Idle) return
                sessionState.value = TogetherSessionState.Error(event.message, true)
                ioScope.launch { stopInternal() }
            }

            else -> Unit
        }
    }

    suspend fun applyHostControl(action: ControlAction) {
        withContext(Dispatchers.Main) {
            val mc = controller.mediaController ?: return@withContext
            when (action) {
                ControlAction.Play -> if (!mc.isPlaying) mc.play()
                ControlAction.Pause -> if (mc.isPlaying) mc.pause()

                is ControlAction.SeekTo -> mc.seekTo(action.positionMs.coerceAtLeast(0L))

                ControlAction.SkipNext -> if (mc.hasNextMediaItem() || controller.uiState.value.queueIndex < controller.uiState.value.queue.lastIndex) {
                    controller.playNext()
                }

                ControlAction.SkipPrevious -> controller.playPrevious()

                is ControlAction.SeekToTrack -> {
                    val trackId = action.trackId.trim()
                    if (trackId.isNotBlank()) {
                        val idx = controller.uiState.value.queue.indexOfFirst {
                            it.id == trackId || it.videoId == trackId
                        }
                        if (idx >= 0) controller.playQueueInternal(idx, action.positionMs.coerceAtLeast(0L))
                    }
                }

                is ControlAction.SeekToIndex -> {
                    val idx = action.index.coerceAtLeast(0)
                    if (idx < controller.uiState.value.queue.size) {
                        controller.playQueueInternal(idx, action.positionMs.coerceAtLeast(0L))
                    }
                }

                is ControlAction.SetRepeatMode -> controller.setRepeatModeInternal(action.repeatMode)

                is ControlAction.SetShuffleEnabled -> controller.setShuffleInternal(action.shuffleEnabled)
            }
        }
    }

    private suspend fun applyHostAddTrack(track: TogetherTrack, mode: AddTrackMode) {
        withContext(Dispatchers.Main) {
            controller.addTrackFromTogether(track.toMediaTrack(), mode == AddTrackMode.PLAY_NEXT)
        }
    }

    private suspend fun buildRoomState(
        sessionId: String,
        hostId: String,
        settings: TogetherRoomSettings? = null,
        participants: List<TogetherParticipant>? = null,
    ): TogetherRoomState =
        withContext(Dispatchers.Main) {
            val ui = controller.uiState.value
            val mc = controller.mediaController
            val effectiveSettings = settings
                ?: server?.currentSettings()
                ?: onlineHost?.currentSettings()
                ?: TogetherRoomSettings()
            val effectiveParticipants = participants
                ?: server?.currentParticipants()
                ?: onlineHost?.currentParticipants()
                ?: emptyList()
            val tracks = ui.queue.map { t ->
                TogetherTrack(
                    id = t.videoId ?: t.id,
                    title = t.title,
                    artists = listOf(t.artist),
                    durationSec = t.durationSeconds,
                    thumbnailUrl = t.artworkUrl,
                )
            }
            TogetherRoomState(
                sessionId = sessionId,
                hostId = hostId,
                settings = effectiveSettings,
                participants = effectiveParticipants,
                queue = tracks,
                queueHash = md5(tracks.joinToString(separator = "|") { it.id }),
                currentIndex = ui.queueIndex.coerceAtLeast(0),
                isPlaying = mc?.isPlaying == true,
                positionMs = mc?.currentPosition?.coerceAtLeast(0L) ?: 0L,
                repeatMode = mc?.repeatMode ?: Player.REPEAT_MODE_OFF,
                shuffleEnabled = ui.shuffleEnabled,
                sentAtElapsedRealtimeMs = SystemClock.elapsedRealtime(),
            )
        }

    private fun markHostParticipant(state: TogetherRoomState, hostId: String): TogetherRoomState =
        state.copy(
            hostId = hostId,
            participants = state.participants.map { it.copy(isHost = it.id == hostId) },
        )

    private fun handleHostTransferred(participantId: String) {
        authorityParticipantId = participantId
        if (participantId != HOST_ID) {
            selfParticipantId = HOST_ID
        }
        scope.launch {
            when (val current = sessionState.value) {
                is TogetherSessionState.Hosting -> {
                    val roomState = current.roomState?.let { markHostParticipant(it, participantId) }
                    sessionState.value = TogetherSessionState.Joined(
                        role = if (participantId == HOST_ID) TogetherRole.Host else TogetherRole.Guest,
                        sessionId = current.sessionId,
                        selfParticipantId = HOST_ID,
                        roomState = roomState ?: TogetherRoomState(sessionId = current.sessionId, hostId = participantId),
                    )
                }

                is TogetherSessionState.HostingOnline -> {
                    val roomState = current.roomState?.let { markHostParticipant(it, participantId) }
                    sessionState.value = TogetherSessionState.Joined(
                        role = if (participantId == HOST_ID) TogetherRole.Host else TogetherRole.Guest,
                        sessionId = current.sessionId,
                        selfParticipantId = HOST_ID,
                        roomState = roomState ?: TogetherRoomState(sessionId = current.sessionId, hostId = participantId),
                    )
                }

                is TogetherSessionState.Joined -> {
                    sessionState.value = current.copy(
                        role = if (current.selfParticipantId == participantId) TogetherRole.Host else TogetherRole.Guest,
                        roomState = markHostParticipant(current.roomState, participantId),
                    )
                }

                else -> Unit
            }
        }
    }

    private fun handleClientHostTransferred(transfer: HostTransferred) {
        val participantId = transfer.participantId
        handleHostTransferred(participantId)
        val c = client ?: return
        if (participantId != selfParticipantId) return
        broadcastJob?.cancel()
        broadcastJob = ioScope.launch {
            while (client === c && authorityParticipantId == participantId) {
                val state = buildRoomState(sessionId = transfer.sessionId, hostId = participantId)
                c.sendRoomState(state)
                delay(TogetherPlaybackSync.BroadcastIntervalMs)
            }
        }
    }

    suspend fun applyRemoteRoomState(state: TogetherRoomState, force: Boolean = false) {
        val pid = selfParticipantId ?: return
        val now = SystemClock.elapsedRealtime()

        val pending = pendingGuestControl
        if (force) {
            pendingGuestControl = null
        } else if (pending != null) {
            val currentTrackId = state.queue.getOrNull(state.currentIndex.coerceAtLeast(0))?.id
            val mismatch =
                (pending.desiredIsPlaying != null && state.isPlaying != pending.desiredIsPlaying) ||
                    (pending.desiredIndex != null && state.currentIndex != pending.desiredIndex) ||
                    (pending.desiredTrackId != null && currentTrackId != pending.desiredTrackId)
            if (now >= pending.expiresAtElapsedMs) {
                if ((pending.desiredIndex != null || pending.desiredTrackId != null) &&
                    now - pending.requestedAtElapsedMs >= 1200L &&
                    mismatch
                ) {
                    showNotice(context.getString(R.string.tm_seek_fail), "GUEST_SEEK_TIMEOUT")
                }
                pendingGuestControl = null
            } else {
                if (mismatch) return
                pendingGuestControl = null
            }
        }

        val sentAt = state.sentAtElapsedRealtimeMs
        if (TogetherPlaybackSync.isStaleRoomState(
                sentAtElapsedRealtimeMs = sentAt,
                lastAppliedSentAtElapsedRealtimeMs = lastAppliedRoomStateSentAtElapsedMs,
                force = force,
            )
        ) {
            return
        }

        val targetPos = TogetherPlaybackSync.targetPositionMs(
            state = state,
            isOnlineSession = isOnlineSession,
            clockSnapshot = if (isOnlineSession) null else clock?.snapshot(),
            nowElapsedRealtimeMs = now,
        )

        withContext(Dispatchers.Main) {
            applyingRemote = true
            suppressEchoUntilElapsedMs = TogetherPlaybackSync.echoSuppressionUntil(SystemClock.elapsedRealtime())
            try {
                val desiredIds = state.queue.map { it.id }
                val desiredHash = state.queueHash
                val localIds = controller.uiState.value.queue.map { it.videoId ?: it.id }.filter { it.isNotBlank() }
                val localHash = if (localIds.isEmpty()) "" else md5(localIds.joinToString(separator = "|"))
                val needsRebuild = TogetherPlaybackSync.needsQueueRebuild(
                    desiredHash = desiredHash,
                    desiredIds = desiredIds,
                    localHash = localHash,
                    localIds = localIds,
                )

                if (state.queue.isNotEmpty() && needsRebuild) {
                    val startIndex = state.currentIndex.coerceIn(0, state.queue.lastIndex)
                    controller.applyRemoteQueue(
                        tracks = state.queue.map { it.toMediaTrack() },
                        startIndex = startIndex,
                        startPositionMs = targetPos,
                        playWhenReady = state.isPlaying,
                        repeatMode = state.repeatMode,
                        shuffleEnabled = state.shuffleEnabled,
                    )
                    lastRemoteAppliedIndex = startIndex
                } else {
                    val ui = controller.uiState.value
                    val index = if (ui.queue.isNotEmpty()) state.currentIndex.coerceIn(0, ui.queue.lastIndex) else 0
                    val indexChanged = ui.queue.isNotEmpty() && index != ui.queueIndex

                    if (indexChanged) {
                        controller.applyRemoteIndex(index, targetPos, state.isPlaying)
                    } else {
                        val mc = controller.mediaController
                        val positionNow = mc?.currentPosition ?: 0L
                        val shouldSeekForDrift = TogetherPlaybackSync.shouldSeekForDrift(
                            currentPositionMs = positionNow,
                            targetPositionMs = targetPos,
                            isPlaying = state.isPlaying,
                            isOnlineSession = isOnlineSession,
                        )
                        if (shouldSeekForDrift || (!state.isPlaying && mc?.isPlaying == true)) {
                            mc?.seekTo(targetPos)
                        }
                        if (mc?.isPlaying == true && !state.isPlaying) mc.pause()
                        else if (mc?.isPlaying == false && state.isPlaying) mc.play()
                    }
                    lastRemoteAppliedIndex = index
                }
                lastRemoteAppliedPlayWhenReady = state.isPlaying
                lastAppliedRoomStateSentAtElapsedMs = sentAt

                val currentRole = (sessionState.value as? TogetherSessionState.Joined)?.role ?: TogetherRole.Guest
                sessionState.value = TogetherSessionState.Joined(
                    role = currentRole,
                    sessionId = state.sessionId,
                    selfParticipantId = pid,
                    roomState = state,
                )
            } finally {
                applyingRemote = false
            }
        }
    }

    fun isEchoSuppressed(): Boolean =
        applyingRemote || SystemClock.elapsedRealtime() < suppressEchoUntilElapsedMs

    fun onLocalSeekTransition(trackId: String?, index: Int, positionMs: Long) {
        if (!canGuestControl()) return
        val now = SystemClock.elapsedRealtime()
        if (isEchoSuppressed() && lastRemoteAppliedIndex == index) return
        requestControl(
            if (trackId.isNullOrBlank()) {
                ControlAction.SeekToIndex(index = index, positionMs = positionMs.coerceAtLeast(0L))
            } else {
                ControlAction.SeekToTrack(trackId = trackId, positionMs = positionMs.coerceAtLeast(0L))
            },
        )
    }

    fun onLocalPlayWhenReadyChanged(playWhenReady: Boolean) {
        if (!canGuestControl()) return
        if (isEchoSuppressed() && lastRemoteAppliedPlayWhenReady != null && lastRemoteAppliedPlayWhenReady == playWhenReady) return
        requestControl(if (playWhenReady) ControlAction.Play else ControlAction.Pause)
    }

    private suspend fun stopInternal() {
        broadcastJob?.cancel()
        broadcastJob = null
        onlineConnectJob?.cancel()
        onlineConnectJob = null
        clientEventsJob?.cancel()
        clientEventsJob = null
        heartbeatJob?.cancel()
        heartbeatJob = null

        clock = null
        selfParticipantId = null
        authorityParticipantId = null
        participantNames.clear()
        isOnlineSession = false
        applyingRemote = false
        suppressEchoUntilElapsedMs = 0L
        lastAppliedRoomStateSentAtElapsedMs = 0L
        lastRemoteAppliedPlayWhenReady = null
        lastRemoteAppliedIndex = -1
        lastSentControlAtElapsedMs = 0L
        lastSentControlAction = null
        pendingGuestControl = null

        try { client?.disconnect() } catch (_: Exception) {}
        client = null

        try { onlineHost?.disconnect() } catch (_: Exception) {}
        onlineHost = null

        try { server?.stop() } catch (_: Exception) {}
        server = null

        sessionState.value = TogetherSessionState.Idle
    }

    private fun TogetherTrack.toMediaTrack(): MediaTrack =
        MediaTrack(
            id = id,
            title = title,
            artist = artists.joinToString(", ").ifBlank { "YouTube" },
            durationMs = durationSec.toLong() * 1000L,
            artworkUrl = thumbnailUrl,
            videoId = id.takeIf { it.length == 11 },
        )

    private fun md5(str: String): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(str.toByteArray())).toString(16).padStart(32, '0')
    }
}
