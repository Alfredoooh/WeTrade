package com.wilin.app.vpn

import android.content.Context
import android.content.Intent
import com.wilin.app.model.Server
import com.wilin.app.model.Tweak

object VpnManager {

    fun connectWithTweak(context: Context, server: Server, tweak: Tweak) {
        val intent = Intent(context, AppVpnService::class.java).apply {
            action = AppVpnService.ACTION_CONNECT
            putExtra(AppVpnService.EXTRA_SERVER_IP,   server.ip)
            putExtra(AppVpnService.EXTRA_SERVER_PORT, server.port)
            putExtra(AppVpnService.EXTRA_MODE,        tweak.mode)
            putExtra(AppVpnService.EXTRA_SNI,         tweak.sni.ifEmpty { "free.facebook.com" })
            putExtra(AppVpnService.EXTRA_PAYLOAD,     tweak.payload)
        }
        context.startService(intent)
    }

    fun connect(context: Context, server: Server) {
        val intent = Intent(context, AppVpnService::class.java).apply {
            action = AppVpnService.ACTION_CONNECT
            putExtra(AppVpnService.EXTRA_SERVER_IP,   server.ip)
            putExtra(AppVpnService.EXTRA_SERVER_PORT, server.port)
            putExtra(AppVpnService.EXTRA_MODE,        "SSL")
            putExtra(AppVpnService.EXTRA_SNI,         "free.facebook.com")
            putExtra(AppVpnService.EXTRA_PAYLOAD,     "")
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