package com.example.tsuki.auth

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.tsuki.network.YouTubeAccountInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.authDataStore by preferencesDataStore(
    name = "youtube_auth_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() })
)

class YouTubeAuthManager(private val context: Context) {

    companion object {
        private val KEY_COOKIE = stringPreferencesKey("yt_cookie")
        private val KEY_VISITOR_DATA = stringPreferencesKey("yt_visitor_data")
        private val KEY_DATA_SYNC_ID = stringPreferencesKey("yt_data_sync_id")
        private val KEY_ACCOUNT_INFO = stringPreferencesKey("yt_account_info")
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    val cookie: Flow<String?> = context.authDataStore.data.map { it[KEY_COOKIE] }
    val visitorData: Flow<String?> = context.authDataStore.data.map { it[KEY_VISITOR_DATA] }
    val dataSyncId: Flow<String?> = context.authDataStore.data.map { it[KEY_DATA_SYNC_ID] }

    val accountInfo: Flow<YouTubeAccountInfo?> = context.authDataStore.data.map { prefs ->
        prefs[KEY_ACCOUNT_INFO]?.let {
            try {
                json.decodeFromString<YouTubeAccountInfo>(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    val isLoggedIn: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        val c = prefs[KEY_COOKIE]
        !c.isNullOrBlank() && (c.contains("SAPISID") || c.contains("__Secure-3PAPISID") || c.contains("SID"))
    }

    suspend fun saveSession(
        cookie: String,
        visitorData: String? = null,
        dataSyncId: String? = null,
        accountInfo: YouTubeAccountInfo? = null
    ) {
        context.authDataStore.edit { prefs ->
            prefs[KEY_COOKIE] = cookie
            if (visitorData != null) prefs[KEY_VISITOR_DATA] = visitorData else prefs.remove(KEY_VISITOR_DATA)
            if (dataSyncId != null) prefs[KEY_DATA_SYNC_ID] = dataSyncId else prefs.remove(KEY_DATA_SYNC_ID)
            if (accountInfo != null) {
                prefs[KEY_ACCOUNT_INFO] = json.encodeToString(YouTubeAccountInfo.serializer(), accountInfo)
            } else {
                prefs.remove(KEY_ACCOUNT_INFO)
            }
        }
    }

    suspend fun logout() {
        context.authDataStore.edit { prefs ->
            prefs.remove(KEY_COOKIE)
            prefs.remove(KEY_VISITOR_DATA)
            prefs.remove(KEY_DATA_SYNC_ID)
            prefs.remove(KEY_ACCOUNT_INFO)
        }
    }

}
