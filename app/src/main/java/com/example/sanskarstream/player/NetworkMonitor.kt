package com.example.sanskarstream.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Wi-Fi/LAN gaya aur wapas aaya -> [reconnects] emit. Offline tiles aur channel list
 * isse turant retry karte hain (60s backoff ka wait nahi).
 */
object NetworkMonitor {
    private val _reconnects = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val reconnects: SharedFlow<Unit> = _reconnects.asSharedFlow()

    @Volatile
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                private var hadNetwork = cm.activeNetwork != null

                override fun onAvailable(network: Network) {
                    if (!hadNetwork) _reconnects.tryEmit(Unit)
                    hadNetwork = true
                }

                override fun onLost(network: Network) {
                    hadNetwork = false
                }
            })
        }
    }
}
