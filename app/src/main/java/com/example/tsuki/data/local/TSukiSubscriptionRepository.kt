package com.example.tsuki.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tsukiSubsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tsuki_subscriptions",
    corruptionHandler = ReplaceFileCorruptionHandler(produceNewData = { emptyPreferences() })
)

data class TSukiChannelSubscription(
    val channelId: String,
    val channelName: String,
    val channelThumbnail: String,
    val subscribedAt: Long = System.currentTimeMillis(),
    val lastVideoId: String? = null,
    val lastCheckTime: Long = 0L
)

class TSukiSubscriptionRepository private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: TSukiSubscriptionRepository? = null
        fun getInstance(context: Context): TSukiSubscriptionRepository =
            instance ?: synchronized(this) {
                instance ?: TSukiSubscriptionRepository(context.applicationContext).also { instance = it }
            }
        private fun channelKey(id: String) = stringPreferencesKey("channel_$id")
        private val ORDER_KEY = stringPreferencesKey("subscriptions_order")
    }

    suspend fun subscribe(channel: TSukiChannelSubscription) {
        context.tsukiSubsDataStore.edit { p ->
            p[channelKey(channel.channelId)] = serialize(channel)
            val order = p[ORDER_KEY] ?: ""
            val list = if (order.isEmpty()) mutableListOf<String>() else order.split(",").toMutableList()
            if (!list.contains(channel.channelId)) {
                list.add(0, channel.channelId)
                p[ORDER_KEY] = list.joinToString(",")
            }
        }
    }

    suspend fun unsubscribe(channelId: String) {
        context.tsukiSubsDataStore.edit { p ->
            p.remove(channelKey(channelId))
            val order = p[ORDER_KEY] ?: ""
            if (order.isNotEmpty()) {
                val list = order.split(",").toMutableList()
                list.remove(channelId)
                p[ORDER_KEY] = list.joinToString(",")
            }
        }
    }

    fun isSubscribed(channelId: String): Flow<Boolean> =
        context.tsukiSubsDataStore.data.map { it.contains(channelKey(channelId)) }

    fun getAllSubscriptions(): Flow<List<TSukiChannelSubscription>> =
        context.tsukiSubsDataStore.data.map { p ->
            val order = p[ORDER_KEY] ?: ""
            if (order.isEmpty()) emptyList()
            else order.split(",").mapNotNull { id -> p[channelKey(id)]?.let { deserialize(it) } }
        }

    suspend fun getAllIds(): Set<String> {
        val order = context.tsukiSubsDataStore.data.map { it[ORDER_KEY] ?: "" }.first()
        return if (order.isEmpty()) emptySet() else order.split(",").toSet()
    }

    suspend fun getSubscriptionOnce(id: String): TSukiChannelSubscription? {
        val data = context.tsukiSubsDataStore.data.map { it[channelKey(id)] }.first()
        return data?.let { deserialize(it) }
    }

    private fun escape(value: String): String = value.replace("|", "%7C")

    private fun unescape(value: String): String = value.replace("%7C", "|")

    private fun serialize(c: TSukiChannelSubscription): String =
        listOf(escape(c.channelId), escape(c.channelName), escape(c.channelThumbnail), c.subscribedAt.toString(), escape(c.lastVideoId ?: ""), c.lastCheckTime.toString()).joinToString("|")

    private fun deserialize(s: String): TSukiChannelSubscription? {
        return try {
            val p = s.split("|")
            if (p.size < 4) return null
            TSukiChannelSubscription(
                channelId = unescape(p[0]),
                channelName = unescape(p[1]),
                channelThumbnail = unescape(p[2]),
                subscribedAt = p[3].toLongOrNull() ?: System.currentTimeMillis(),
                lastVideoId = unescape(p.getOrNull(4) ?: "").takeIf { it.isNotEmpty() },
                lastCheckTime = p.getOrNull(5)?.toLongOrNull() ?: 0L
            )
        } catch (_: Exception) { null }
    }
}
