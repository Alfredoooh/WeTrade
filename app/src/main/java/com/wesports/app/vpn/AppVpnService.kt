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
    private var currentIp = ""
    private var currentPort = 443
    private var currentMode = "SSL"
    private var currentSni = ""
    private var currentPayload = ""

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
        return try {
            when (intent?.action) {
                ACTION_CONNECT -> {
                    currentIp = intent.getStringExtra(EXTRA_SERVER_IP) ?: ""
                    currentPort = intent.getIntExtra(EXTRA_SERVER_PORT, 443)
                    currentMode = intent.getStringExtra(EXTRA_MODE) ?: "SSL"
                    currentSni = intent.getStringExtra(EXTRA_SNI) ?: ""
                    currentPayload = intent.getStringExtra(EXTRA_PAYLOAD) ?: ""
                    if (currentIp.isEmpty()) { log("IP inválido"); START_NOT_STICKY }
                    else { startTunnel(); START_STICKY }
                }
                ACTION_DISCONNECT -> { stopTunnel(); START_NOT_STICKY }
                else -> START_NOT_STICKY
            }
        } catch (e: Exception) { log("onStartCommand: ${e.message}"); START_NOT_STICKY }
    }

    private fun startTunnel() {
        serviceJob?.cancel()
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                log("A iniciar túnel $currentMode → $currentIp:$currentPort")
                try { vpnInterface?.close() } catch (_: Exception) {}
                vpnInterface = null

                val socket = when (currentMode.uppercase()) {
                    "SSL", "SSL PROXY" -> connectSSL()
                    "HTTP" -> connectHTTP()
                    else -> connectSSL()
                } ?: run {
                    log("Falha ao conectar — a tentar novamente em 5s")
                    isConnected = false
                    onStatusChanged?.invoke(false)
                    delay(5000)
                    if (isActive) startTunnel()
                    return@launch
                }

                tunnelSocket = socket
                log("Socket OK — a criar interface VPN")

                val builder = Builder()
                    .setSession("WeSports VPN")
                    .addAddress("10.0.0.2", 32)
                    .addDnsServer("8.8.8.8")
                    .addDnsServer("1.1.1.1")
                    .addRoute("0.0.0.0", 0)
                    .setMtu(1500)

                vpnInterface = builder.establish() ?: run {
                    log("Falha ao criar interface VPN")
                    socket.close()
                    isConnected = false
                    onStatusChanged?.invoke(false)
                    return@launch
                }

                isConnected = true
                onStatusChanged?.invoke(true)
                log("VPN ativa — tráfego a ser encaminhado")

                val tunFd = vpnInterface!!.fileDescriptor
                val tunIn = FileInputStream(tunFd)
                val tunOut = FileOutputStream(tunFd)
                val sockOut = socket.getOutputStream()
                val sockIn = socket.getInputStream()

                val toServer = launch(Dispatchers.IO) {
                    val buf = ByteArray(32767)
                    while (isActive && !socket.isClosed) {
                        try {
                            val len = tunIn.read(buf)
                            if (len > 0) { sockOut.write(buf, 0, len); sockOut.flush() }
                        } catch (e: Exception) { log("→ server: ${e.message}"); break }
                    }
                }

                val toDevice = launch(Dispatchers.IO) {
                    val buf = ByteArray(32767)
                    while (isActive && !socket.isClosed) {
                        try {
                            val len = sockIn.read(buf)
                            if (len > 0) tunOut.write(buf, 0, len)
                            else if (len < 0) { log("Servidor fechou"); break }
                        } catch (e: Exception) { log("← device: ${e.message}"); break }
                    }
                }

                toServer.join()
                toDevice.join()

                isConnected = false
                onStatusChanged?.invoke(false)
                try { socket.close() } catch (_: Exception) {}
                try { vpnInterface?.close() } catch (_: Exception) {}
                vpnInterface = null
                log("Túnel encerrado — a reconectar em 5s")
                delay(5000)
                if (isActive) startTunnel()

            } catch (e: Exception) {
                log("Erro: ${e.message}")
                isConnected = false
                onStatusChanged?.invoke(false)
                try { vpnInterface?.close() } catch (_: Exception) {}
                vpnInterface = null
                delay(5000)
                if (isActive) startTunnel()
            }
        }
    }

    private fun connectSSL(): Socket? {
        return try {
            log("SSL → $currentIp:$currentPort SNI:${currentSni.ifEmpty { "none" }}")
            val trust = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(c: Array<java.security.cert.X509Certificate>, a: String) {}
                override fun checkServerTrusted(c: Array<java.security.cert.X509Certificate>, a: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, trust, java.security.SecureRandom())
            val raw = Socket()
            raw.connect(InetSocketAddress(currentIp, currentPort), 10000)
            protect(raw)
            val ssl = ctx.socketFactory.createSocket(raw, currentSni.ifEmpty { currentIp }, currentPort, true) as SSLSocket
            ssl.enabledProtocols = ssl.supportedProtocols.filter { it.contains("TLS") }.toTypedArray()
            if (currentSni.isNotEmpty()) {
                val p = SSLParameters()
                p.serverNames = listOf(SNIHostName(currentSni))
                ssl.sslParameters = p
            }
            ssl.startHandshake()
            log("SSL OK — ${ssl.session.protocol}")
            ssl
        } catch (e: Exception) { log("Erro SSL: ${e.message}"); null }
    }

    private fun connectHTTP(): Socket? {
        return try {
            log("HTTP → $currentIp:$currentPort host:$currentSni")
            val socket = Socket()
            socket.connect(InetSocketAddress(currentIp, currentPort), 10000)
            protect(socket)
            val out = socket.getOutputStream()
            val inp = BufferedReader(InputStreamReader(socket.getInputStream()))
            val req = if (currentPayload.isNotEmpty()) {
                currentPayload
                    .replace("[host]", currentSni)
                    .replace("[port]", currentPort.toString())
                    .replace("[crlf]", "\r\n")
                    .replace("[cr]", "\r")
                    .replace("[lf]", "\n")
            } else {
                "CONNECT $currentSni:$currentPort HTTP/1.1\r\nHost: $currentSni\r\nProxy-Connection: Keep-Alive\r\nUser-Agent: Mozilla/5.0\r\n\r\n"
            }
            out.write(req.toByteArray())
            out.flush()
            log("HTTP payload enviado")
            val resp = inp.readLine() ?: ""
            log("HTTP resp: $resp")
            if (resp.contains("200")) {
                while (inp.readLine()?.isNotEmpty() == true) {}
                log("HTTP CONNECT OK")
                socket
            } else {
                log("HTTP falhou: $resp")
                socket.close()
                null
            }
        } catch (e: Exception) { log("Erro HTTP: ${e.message}"); null }
    }

    private fun stopTunnel() {
        try { serviceJob?.cancel() } catch (_: Exception) {}
        try { tunnelSocket?.close() } catch (_: Exception) {}
        try { vpnInterface?.close() } catch (_: Exception) {}
        tunnelSocket = null
        vpnInterface = null
        isConnected = false
        onStatusChanged?.invoke(false)
        stopSelf()
    }

    private fun log(msg: String) { Log.d(TAG, msg); onLogMessage?.invoke(msg) }
    override fun onDestroy() { stopTunnel(); super.onDestroy() }
    override fun onRevoke() { stopTunnel(); super.onRevoke() }
}