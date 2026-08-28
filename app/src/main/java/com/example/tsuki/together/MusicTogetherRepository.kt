package com.example.tsuki.together

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private val Context.togetherPrefs by preferencesDataStore(name = "together_settings")
private val KeyName = stringPreferencesKey("together_display_name")
private val KeyPort = intPreferencesKey("together_default_port")
private val KeyAdd = booleanPreferencesKey("together_allow_guests_to_add_tracks")
private val KeyControl = booleanPreferencesKey("together_allow_guests_to_control_playback")
private val KeyApproval = booleanPreferencesKey("together_require_host_approval_to_join")
private val KeyLastLink = stringPreferencesKey("together_last_join_link")
private val KeyWelcome = booleanPreferencesKey("together_welcome_shown")

enum class MusicTogetherConnectionMode { LAN, ONLINE }

data class MusicTogetherPreferences(
    val displayName: String,
    val port: Int,
    val allowGuestsToAddTracks: Boolean,
    val allowGuestsToControlPlayback: Boolean,
    val requireHostApprovalToJoin: Boolean,
    val lastJoinLink: String,
    val welcomeShown: Boolean
)

data class MusicTogetherSnapshot(val preferences: MusicTogetherPreferences, val sessionState: TogetherSessionState)

class MusicTogetherRepository private constructor(private val ctx: Context) {
    private val ctrl = MutableStateFlow<com.example.tsuki.playback.PlayerController?>(null)

    suspend fun currentDisplayName(): String = ctx.togetherPrefs.data.first()[KeyName] ?: Build.MODEL?.takeIf { it.isNotBlank() } ?: "TSuki"

    val preferences: Flow<MusicTogetherPreferences> = ctx.togetherPrefs.data.map { p ->
        MusicTogetherPreferences(
            displayName = p[KeyName] ?: Build.MODEL?.takeIf { it.isNotBlank() } ?: "TSuki",
            port = p[KeyPort] ?: 42117,
            allowGuestsToAddTracks = p[KeyAdd] ?: true,
            allowGuestsToControlPlayback = p[KeyControl] ?: false,
            requireHostApprovalToJoin = p[KeyApproval] ?: false,
            lastJoinLink = p[KeyLastLink] ?: "",
            welcomeShown = p[KeyWelcome] ?: false
        )
    }.distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val sessionState: Flow<TogetherSessionState> = ctrl.flatMapLatest { it?.togetherSessionState ?: flowOf(TogetherSessionState.Idle) }

    fun attachController(c: com.example.tsuki.playback.PlayerController?) { ctrl.value = c }
    fun attachedController(): com.example.tsuki.playback.PlayerController? = ctrl.value

    suspend fun setDisplayName(v: String) { ctx.togetherPrefs.edit { it[KeyName] = v } }
    suspend fun setPort(v: Int) { ctx.togetherPrefs.edit { it[KeyPort] = v } }
    suspend fun setAllowGuestsToAddTracks(v: Boolean) { ctx.togetherPrefs.edit { it[KeyAdd] = v } }
    suspend fun setAllowGuestsToControlPlayback(v: Boolean) { ctx.togetherPrefs.edit { it[KeyControl] = v } }
    suspend fun setRequireHostApprovalToJoin(v: Boolean) { ctx.togetherPrefs.edit { it[KeyApproval] = v } }
    suspend fun setLastJoinLink(v: String) { ctx.togetherPrefs.edit { it[KeyLastLink] = v } }
    suspend fun setWelcomeShown(v: Boolean) { ctx.togetherPrefs.edit { it[KeyWelcome] = v } }

    fun startSession(mode: MusicTogetherConnectionMode, displayName: String, port: Int, settings: TogetherRoomSettings) {
        val c = ctrl.value ?: return
        when (mode) {
            MusicTogetherConnectionMode.LAN -> c.togetherManager.startTogetherHost(port, displayName, settings)
            MusicTogetherConnectionMode.ONLINE -> c.togetherManager.startTogetherOnlineHost(displayName, settings)
        }
    }

    fun joinSession(mode: MusicTogetherConnectionMode, rawInput: String, displayName: String) {
        val c = ctrl.value ?: return
        when (mode) {
            MusicTogetherConnectionMode.LAN -> c.togetherManager.joinTogether(rawInput, displayName)
            MusicTogetherConnectionMode.ONLINE -> c.togetherManager.joinTogetherOnline(rawInput, displayName)
        }
    }

    fun leaveSession() { ctrl.value?.togetherManager?.leaveTogether() }
    fun updateSettings(s: TogetherRoomSettings) { ctrl.value?.togetherManager?.updateTogetherSettings(s) }
    fun approveParticipant(id: String, approved: Boolean) { ctrl.value?.togetherManager?.approveTogetherParticipant(id, approved) }
    fun kickParticipant(id: String) { ctrl.value?.togetherManager?.kickTogetherParticipant(id) }
    fun banParticipant(id: String) { ctrl.value?.togetherManager?.banTogetherParticipant(id) }
    fun transferHostOwnership(id: String) { ctrl.value?.togetherManager?.transferTogetherHostOwnership(id) }

    companion object {
        @Volatile private var inst: MusicTogetherRepository? = null
        fun getInstance(context: Context): MusicTogetherRepository = inst ?: synchronized(this) {
            inst ?: MusicTogetherRepository(context.applicationContext).also { inst = it }
        }
    }
}
