package com.wesports.app.vpn

import android.content.Context
import android.content.Intent
import com.wesports.app.model.Server

object VpnManager {

    fun connect(context: Context, server: Server) {
        val intent = Intent(context, AppVpnService::class.java).apply {
            action = AppVpnService.ACTION_CONNECT
            putExtra(AppVpnService.EXTRA_SERVER_IP, server.ip)
        }
        context.startService(intent)
    }

    fun disconnect(context: Context) {
        val intent = Intent(context, AppVpnService::class.java).apply {
            action = AppVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    fun isConnected(): Boolean = AppVpnService.isConnected
}