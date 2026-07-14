package com.healthbridge.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log

class NetworkMonitor(context: Context) {

    private val TAG = "NetworkMonitor"
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    var isConnected: Boolean = false
        private set

    private var onConnected: (() -> Unit)? = null
    private var onDisconnected: (() -> Unit)? = null

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            isConnected = true
            Log.d(TAG, "Network available")
            onConnected?.invoke()
        }

        override fun onLost(network: Network) {
            isConnected = false
            Log.d(TAG, "Network lost")
            onDisconnected?.invoke()
        }
    }

    fun start(onConnected: (() -> Unit)? = null, onDisconnected: (() -> Unit)? = null) {
        this.onConnected = onConnected
        this.onDisconnected = onDisconnected

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)

        isConnected = cm.activeNetwork?.let { net ->
            cm.getNetworkCapabilities(net)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } ?: false

        Log.d(TAG, "NetworkMonitor started, connected=$isConnected")
    }

    fun stop() {
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            Log.e(TAG, "Stop error: ${e.message}")
        }
        Log.d(TAG, "NetworkMonitor stopped")
    }
}
