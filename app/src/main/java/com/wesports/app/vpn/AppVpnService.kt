package com.wesports.app.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel

class AppVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var running = false

    companion object {
        const val ACTION_CONNECT = "com.wesports.app.VPN_CONNECT"
        const val ACTION_DISCONNECT = "com.wesports.app.VPN_DISCONNECT"
        const val EXTRA_SERVER_IP = "server_ip"
        const val TAG = "AppVpnService"
        var isConnected = false
        var onStatusChanged: ((Boolean) -> Unit)? = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_CONNECT -> {
                val serverIp = intent.getStringExtra(EXTRA_SERVER_IP) ?: ""
                connect(serverIp)
                START_STICKY
            }
            ACTION_DISCONNECT -> {
                disconnect()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    private fun connect(serverIp: String) {
        try {
            val builder = Builder()
            builder.setSession("WeSports VPN")
            builder.addAddress("10.0.0.2", 32)
            builder.addDnsServer("8.8.8.8")
            builder.addRoute("0.0.0.0", 0)
            vpnInterface = builder.establish()
            running = true
            isConnected = true
            onStatusChanged?.invoke(true)
            Log.d(TAG, "VPN conectado ao servidor $serverIp")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao conectar VPN: ${e.message}")
            isConnected = false
            onStatusChanged?.invoke(false)
        }
    }

    private fun disconnect() {
        running = false
        vpnInterface?.close()
        vpnInterface = null
        isConnected = false
        onStatusChanged?.invoke(false)
        stopSelf()
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }
}