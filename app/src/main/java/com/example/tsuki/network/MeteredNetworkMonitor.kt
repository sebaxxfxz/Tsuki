package com.example.tsuki.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MeteredNetworkMonitor(context: Context) {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isMetered = MutableStateFlow(queryMetered())
    val isMetered: StateFlow<Boolean> = _isMetered.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            _isMetered.value = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        }

        override fun onAvailable(network: Network) {
            _isMetered.value = queryMetered()
        }

        override fun onLost(network: Network) {
            _isMetered.value = queryMetered()
        }
    }

    init {
        runCatching {
            connectivityManager.registerDefaultNetworkCallback(callback)
        }
    }

    private fun queryMetered(): Boolean = !isActiveNotMetered()

    private fun isActiveNotMetered(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
