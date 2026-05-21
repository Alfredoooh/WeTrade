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
                    if (currentIp.isEmpty()) {
                        log("IP do servidor inválido")
                        START_NOT_STICKY
                    } else {
                        startTunnel()
                        START_STICKY
                    }
                }
                ACTION_DISCONNECT -> {
                    stopTunnel()
                    START_NOT_STICKY
                }
                else -> START_NOT_STICKY
            }
        } catch (e: Exception) {
            log("onStartCommand error: ${e.message}")
            START_NOT_STICKY
        }
    }

    private fun startTunnel() {
        serviceJob?.cancel()
        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                log("A iniciar túnel $currentMode para $currentIp:$currentPort")

                // Fecha interface VPN anterior
                try { vpnInterface?.close() } catch (e: Exception) {}
                vpnInterface = null

                // Cria socket ANTES do túnel VPN estar ativo
                val socket = when (currentMode.uppercase()) {
                    "SSL", "SSL PROXY" -> connectSSL()
                    "HTTP" -> connectHTTP()
                    else -> connectSSL()
                }

                if (socket == null || !socket.isConnected) {
                    log("Falha ao conectar ao servidor")
                    isConnected = false
                    onStatusChanged?.invoke(false)
                    return@launch
                }

                tunnelSocket = socket
                log("Socket conectado, a estabelecer interface VPN...")

                // Cria interface VPN depois do socket estar protegido
                val builder = Builder()
                builder.setSession("WeSports VPN")
                builder.addAddress("10.0.0.2", 32)
                builder.addDnsServer("8.8.8.8")
                builder.addDnsServer("1.1.1.1")
                builder.addRoute("0.0.0.0", 0)
                builder.setMtu(1500)

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
                log("VPN conectada — a encaminhar tráfego")

                val tunIn = FileInputStream(vpnInterface!!.fileDescriptor)
                val tunOut = FileOutputStream(vpnInterface!!.fileDescriptor)
                val sockOut = socket.getOutputStream()
                val sockIn = socket.getInputStream()

                val toServer = launch(Dispatchers.IO) {
                    val buf = ByteArray(4096)
                    while (isActive) {
                        try {
                            val len = tunIn.read(buf)
                            if (len > 0) {
                                // Envia tamanho + dados
                                val header = byteArrayOf(
                                    (len shr 8).toByte(),
                                    (len and 0xFF).toByte()
                                )
                                sockOut.write(header)
                                sockOut.write(buf, 0, len)
                                sockOut.flush()
                            }
                        } catch (e: Exception) {
                            log("toServer error: ${e.message}")
                            break
                        }
                    }
                }

                val toDevice = launch(Dispatchers.IO) {
                    while (isActive) {
                        try {
                            val h1 = sockIn.read()
                            val h2 = sockIn.read()
                            if (h1 < 0 || h2 < 0) {
                                log("Servidor fechou conexão")
                                break
                            }
                            val len = (h1 shl 8) or h2
                            if (len <= 0 || len > 65535) continue
                            val buf = ByteArray(len)
                            var read = 0
                            while (read < len) {
                                val r = sockIn.read(buf, read, len - read)
                                if (r < 0) break
                                read += r
                            }
                            if (read == len) tunOut.write(buf)
                        } catch (e: Exception) {
                            log("toDevice error: ${e.message}")
                            break
                        }
                    }
                }

                toServer.join()
                toDevice.join()

                log("Túnel encerrado, a reconectar em 3s...")
                isConnected = false
                onStatusChanged?.invoke(false)
                try { vpnInterface?.close() } catch (e: Exception) {}
                vpnInterface = null
                try { socket.close() } catch (e: Exception) {}

                delay(3000)
                if (isActive) startTunnel()

            } catch (e: Exception) {
                log("Erro no túnel: ${e.message}")
                isConnected = false
                onStatusChanged?.invoke(false)
                try { vpnInterface?.close() } catch (ex: Exception) {}
                vpnInterface = null
                delay(5000)
                if (serviceJob?.isActive == true) startTunnel()
            }
        }
    }

    private fun connectSSL(): Socket? {
        return try {
            log("A conectar SSL a $currentIp:$currentPort SNI: ${currentSni.ifEmpty { "none" }}")

            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAll, java.security.SecureRandom())

            val rawSocket = Socket()
            rawSocket.connect(InetSocketAddress(currentIp, currentPort), 10000)
            protect(rawSocket)

            val sslSocket = sslContext.socketFactory.createSocket(
                rawSocket, currentSni.ifEmpty { currentIp }, currentPort, true
            ) as SSLSocket

            sslSocket.enabledProtocols = sslSocket.supportedProtocols
                .filter { it.contains("TLS") }.toTypedArray()

            if (currentSni.isNotEmpty()) {
                val params = SSLParameters()
                params.serverNames = listOf(SNIHostName(currentSni))
                sslSocket.sslParameters = params
            }

            sslSocket.startHandshake()
            log("SSL handshake OK — ${sslSocket.session.protocol}")
            sslSocket

        } catch (e: Exception) {
            log("Erro SSL: ${e.message}")
            null
        }
    }

    private fun connectHTTP(): Socket? {
        return try {
            log("A conectar HTTP a $currentIp:$currentPort host: $currentSni")

            val socket = Socket()
            socket.connect(InetSocketAddress(currentIp, currentPort), 10000)
            protect(socket)

            val out = socket.getOutputStream()
            val inp = BufferedReader(InputStreamReader(socket.getInputStream()))

            val request = if (currentPayload.isNotEmpty()) {
                currentPayload
                    .replace("[host]", currentSni)
                    .replace("[port]", currentPort.toString())
                    .replace("[crlf]", "\r\n")
                    .replace("[cr]", "\r")
                    .replace("[lf]", "\n")
            } else {
                "CONNECT $currentSni:$currentPort HTTP/1.1\r\nHost: $currentSni\r\nProxy-Connection: Keep-Alive\r\nUser-Agent: Mozilla/5.0\r\n\r\n"
            }

            out.write(request.toByteArray())
            out.flush()
            log("HTTP payload enviado")

            val responseLine = inp.readLine() ?: ""
            log("HTTP response: $responseLine")

            if (responseLine.contains("200")) {
                while (inp.readLine()?.isNotEmpty() == true) {}
                log("HTTP CONNECT OK")
                socket
            } else {
                log("HTTP CONNECT falhou: $responseLine")
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