package com.wesports.app.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.*

class AppVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private var tunnelSocket: Socket? = null

    companion object {
        const val ACTION_CONNECT = "com.wesports.app.VPN_CONNECT"
        const val ACTION_DISCONNECT = "com.wesports.app.VPN_DISCONNECT"
        const val EXTRA_SERVER_IP = "server_ip"
        const val EXTRA_SERVER_PORT = "server_port"
        const val EXTRA_MODE = "mode"
        const val EXTRA_SNI = "sni"
        const val EXTRA_PAYLOAD = "payload"
        const val TAG = "AppVpnService"
        var isConnected = false
        var onStatusChanged: ((Boolean) -> Unit)? = null
        var onLogMessage: ((String) -> Unit)? = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_CONNECT -> {
                val ip = intent.getStringExtra(EXTRA_SERVER_IP) ?: ""
                val port = intent.getIntExtra(EXTRA_SERVER_PORT, 443)
                val mode = intent.getStringExtra(EXTRA_MODE) ?: "SSL"
                val sni = intent.getStringExtra(EXTRA_SNI) ?: ""
                val payload = intent.getStringExtra(EXTRA_PAYLOAD) ?: ""
                startTunnel(ip, port, mode, sni, payload)
                START_STICKY
            }
            ACTION_DISCONNECT -> {
                stopTunnel()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    private fun startTunnel(ip: String, port: Int, mode: String, sni: String, payload: String) {
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                log("A iniciar túnel $mode para $ip:$port")

                val socket = when (mode) {
                    "SSL", "SSL PROXY" -> connectSSL(ip, port, sni)
                    "HTTP" -> connectHTTP(ip, port, sni, payload)
                    else -> connectSSL(ip, port, sni)
                }

                tunnelSocket = socket

                if (socket == null || !socket.isConnected) {
                    log("Falha ao conectar ao servidor")
                    isConnected = false
                    onStatusChanged?.invoke(false)
                    return@launch
                }

                log("Socket conectado, a estabelecer interface VPN...")

                val builder = Builder()
                builder.setSession("WeSports VPN")
                builder.addAddress("10.0.0.2", 32)
                builder.addDnsServer("8.8.8.8")
                builder.addDnsServer("1.1.1.1")
                builder.addRoute("0.0.0.0", 0)
                builder.setMtu(1500)
                protect(socket)

                vpnInterface = builder.establish()

                if (vpnInterface == null) {
                    log("Falha ao estabelecer interface VPN")
                    socket.close()
                    isConnected = false
                    onStatusChanged?.invoke(false)
                    return@launch
                }

                isConnected = true
                onStatusChanged?.invoke(true)
                log("VPN conectada com sucesso")

                val tunIn = FileInputStream(vpnInterface!!.fileDescriptor)
                val tunOut = FileOutputStream(vpnInterface!!.fileDescriptor)
                val sockOut = socket.getOutputStream()
                val sockIn = socket.getInputStream()

                val toServer = launch {
                    val buf = ByteArray(32767)
                    while (isActive && socket.isConnected) {
                        try {
                            val len = tunIn.read(buf)
                            if (len > 0) {
                                sockOut.write(buf, 0, len)
                                sockOut.flush()
                            }
                        } catch (e: Exception) {
                            break
                        }
                    }
                }

                val toDevice = launch {
                    val buf = ByteArray(32767)
                    while (isActive && socket.isConnected) {
                        try {
                            val len = sockIn.read(buf)
                            if (len > 0) {
                                tunOut.write(buf, 0, len)
                            }
                        } catch (e: Exception) {
                            break
                        }
                    }
                }

                toServer.join()
                toDevice.join()

                log("Túnel encerrado")
                stopTunnel()

            } catch (e: Exception) {
                log("Erro no túnel: ${e.message}")
                isConnected = false
                onStatusChanged?.invoke(false)
            }
        }
    }

    private fun connectSSL(ip: String, port: Int, sni: String): Socket? {
        return try {
            log("A conectar SSL a $ip:$port com SNI: $sni")
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            }), java.security.SecureRandom())

            val factory = sslContext.socketFactory
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), 10000)
            protect(socket)

            val sslSocket = factory.createSocket(socket, sni.ifEmpty { ip }, port, true) as SSLSocket
            sslSocket.enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")

            if (sni.isNotEmpty()) {
                val params = sslSocket.sslParameters
                params.serverNames = listOf(javax.net.ssl.SNIHostName(sni))
                sslSocket.sslParameters = params
            }

            sslSocket.startHandshake()
            log("SSL handshake OK")
            sslSocket
        } catch (e: Exception) {
            log("Erro SSL: ${e.message}")
            null
        }
    }

    private fun connectHTTP(ip: String, port: Int, host: String, payload: String): Socket? {
        return try {
            log("A conectar HTTP INJECT a $ip:$port")
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), 10000)
            protect(socket)

            val out = socket.getOutputStream()
            val inp = socket.getInputStream()

            val connectRequest = if (payload.isNotEmpty()) {
                payload
                    .replace("[host]", host)
                    .replace("[port]", port.toString())
                    .replace("[crlf]", "\r\n")
                    .replace("[cr]", "\r")
                    .replace("[lf]", "\n")
            } else {
                "CONNECT $host:$port HTTP/1.1\r\nHost: $host\r\nProxy-Connection: Keep-Alive\r\n\r\n"
            }

            out.write(connectRequest.toByteArray())
            out.flush()
            log("HTTP payload enviado")

            val response = StringBuilder()
            val buf = ByteArray(4096)
            val len = inp.read(buf)
            if (len > 0) response.append(String(buf, 0, len))

            log("HTTP response: ${response.toString().take(100)}")

            if (response.contains("200")) {
                log("HTTP CONNECT OK")
                socket
            } else {
                log("HTTP CONNECT falhou: ${response.toString().take(200)}")
                socket.close()
                null
            }
        } catch (e: Exception) {
            log("Erro HTTP: ${e.message}")
            null
        }
    }

    private fun stopTunnel() {
        try {
            serviceJob?.cancel()
            tunnelSocket?.close()
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "stopTunnel error: ${e.message}")
        } finally {
            tunnelSocket = null
            vpnInterface = null
            isConnected = false
            onStatusChanged?.invoke(false)
            stopSelf()
        }
    }

    private fun log(msg: String) {
        Log.d(TAG, msg)
        onLogMessage?.invoke(msg)
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopTunnel()
        super.onRevoke()
    }
}