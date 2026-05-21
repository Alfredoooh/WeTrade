package com.wesports.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.wesports.app.MainActivity
import com.wesports.app.R
import kotlinx.coroutines.*
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.Selector
import javax.net.ssl.*

class AppVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private var tunnelSocket: Socket? = null
    private var currentIp = ""
    private var currentPort = 443
    private var currentMode = "SSL"
    private var currentSni = "free.facebook.com"
    private var currentPayload = ""
    private var reconnectAttempts = 0
    private val maxReconnectDelay = 30_000L
    private val baseReconnectDelay = 3_000L

    companion object {
        const val ACTION_CONNECT    = "com.wesports.app.VPN_CONNECT"
        const val ACTION_DISCONNECT = "com.wesports.app.VPN_DISCONNECT"
        const val EXTRA_SERVER_IP   = "server_ip"
        const val EXTRA_SERVER_PORT = "server_port"
        const val EXTRA_MODE        = "mode"
        const val EXTRA_SNI         = "sni"
        const val EXTRA_PAYLOAD     = "payload"
        const val TAG               = "AppVpnService"
        const val NOTIF_ID          = 1
        const val CHANNEL_ID        = "vpn_channel"

        @Volatile var isConnected = false
        @Volatile var onStatusChanged: ((Boolean) -> Unit)? = null
        @Volatile var onLogMessage: ((String) -> Unit)? = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            when (intent?.action) {
                ACTION_CONNECT -> {
                    currentIp      = intent.getStringExtra(EXTRA_SERVER_IP) ?: ""
                    currentPort    = intent.getIntExtra(EXTRA_SERVER_PORT, 443)
                    currentMode    = intent.getStringExtra(EXTRA_MODE) ?: "SSL"
                    currentSni     = intent.getStringExtra(EXTRA_SNI)?.ifEmpty { "free.facebook.com" } ?: "free.facebook.com"
                    currentPayload = intent.getStringExtra(EXTRA_PAYLOAD) ?: ""

                    if (currentIp.isEmpty()) {
                        log("IP inválido — abortar")
                        START_NOT_STICKY
                    } else {
                        startForeground(NOTIF_ID, buildNotification("A conectar…"))
                        reconnectAttempts = 0
                        launchTunnel()
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
            log("onStartCommand erro: ${e.message}")
            START_NOT_STICKY
        }
    }

    override fun onDestroy() { stopTunnel(); super.onDestroy() }
    override fun onRevoke()  { stopTunnel(); super.onRevoke() }

    // ─────────────────────────────────────────────────────────────────────────
    // Tunnel principal
    // ─────────────────────────────────────────────────────────────────────────

    private fun launchTunnel() {
        serviceJob?.cancel()
        serviceJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive) {
                runCatching { connectAndForward() }
                    .onFailure { log("Túnel caiu: ${it.message}") }

                if (!isActive) break

                isConnected = false
                onStatusChanged?.invoke(false)
                updateNotification("Desconectado — a reconectar…")

                val delay = minOf(baseReconnectDelay * (1L shl reconnectAttempts.coerceAtMost(4)), maxReconnectDelay)
                reconnectAttempts++
                log("A reconectar em ${delay / 1000}s (tentativa $reconnectAttempts)…")
                delay(delay)
            }
        }
    }

    private suspend fun connectAndForward() {
        log("A iniciar túnel $currentMode → $currentIp:$currentPort SNI:$currentSni")

        closeTunInterface()

        val socket = when (currentMode.uppercase()) {
            "SSL", "SSL PROXY" -> connectSSL()
            "HTTP"             -> connectHTTP()
            else               -> connectSSL()
        }

        if (socket == null || socket.isClosed) {
            log("Falha ao abrir socket")
            return
        }

        tunnelSocket = socket

        // ── Criar interface VPN ──────────────────────────────────────────────
        val builder = Builder()
            .setSession("WeSports VPN")
            .addAddress("10.8.0.2", 24)
            .addDnsServer("8.8.8.8")
            .addDnsServer("1.1.1.1")
            .addDnsServer("9.9.9.9")
            .addRoute("0.0.0.0", 0)
            .setMtu(1400)
            .setBlocking(true)

        // Não rotear o próprio servidor pelo túnel (evita loop)
        try {
            builder.addDisallowedApplication(packageName)
        } catch (_: Exception) {}

        val pfd = builder.establish() ?: run {
            log("Falha ao criar interface VPN — sem permissão?")
            socket.close()
            return
        }

        vpnInterface = pfd
        isConnected = true
        reconnectAttempts = 0
        onStatusChanged?.invoke(true)
        updateNotification("Conectado a $currentIp")
        log("VPN ativa — tráfego encaminhado via $currentIp:$currentPort")

        val tunFd  = pfd.fileDescriptor
        val tunIn  = FileInputStream(tunFd)
        val tunOut = FileOutputStream(tunFd)
        val sockOut: OutputStream
        val sockIn: InputStream

        try {
            sockOut = socket.getOutputStream()
            sockIn  = socket.getInputStream()
        } catch (e: Exception) {
            log("Erro ao obter streams: ${e.message}")
            closeTunInterface()
            socket.close()
            return
        }

        // ── Keepalive periódico ──────────────────────────────────────────────
        val keepaliveJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && !socket.isClosed) {
                delay(20_000)
                try {
                    // Enviar NOP byte para manter conexão viva
                    sockOut.write(0)
                    sockOut.flush()
                } catch (_: Exception) { break }
            }
        }

        val scope = CoroutineScope(Dispatchers.IO)

        // Tun → Server
        val toServer = scope.launch {
            val buf = ByteArray(32767)
            while (isActive && !socket.isClosed) {
                try {
                    val len = tunIn.read(buf)
                    if (len > 0) {
                        // Prefixar com tamanho para framing (2 bytes big-endian)
                        val frame = ByteArray(2 + len)
                        frame[0] = ((len shr 8) and 0xFF).toByte()
                        frame[1] = (len and 0xFF).toByte()
                        System.arraycopy(buf, 0, frame, 2, len)
                        sockOut.write(frame)
                        sockOut.flush()
                    }
                } catch (e: Exception) {
                    log("→ server: ${e.message}")
                    break
                }
            }
        }

        // Server → Tun
        val toDevice = scope.launch {
            val lenBuf = ByteArray(2)
            val dataBuf = ByteArray(32767)
            while (isActive && !socket.isClosed) {
                try {
                    // Ler 2 bytes de tamanho
                    var read = 0
                    while (read < 2) {
                        val r = sockIn.read(lenBuf, read, 2 - read)
                        if (r < 0) { log("Servidor fechou"); return@launch }
                        read += r
                    }
                    val len = ((lenBuf[0].toInt() and 0xFF) shl 8) or (lenBuf[1].toInt() and 0xFF)
                    if (len <= 0 || len > 32767) continue

                    var received = 0
                    while (received < len) {
                        val r = sockIn.read(dataBuf, received, len - received)
                        if (r < 0) { log("Servidor fechou no meio do pacote"); return@launch }
                        received += r
                    }
                    tunOut.write(dataBuf, 0, len)
                } catch (e: Exception) {
                    log("← device: ${e.message}")
                    break
                }
            }
        }

        toServer.join()
        toDevice.join()
        keepaliveJob.cancel()

        closeTunInterface()
        try { socket.close() } catch (_: Exception) {}
        isConnected = false
        onStatusChanged?.invoke(false)
        log("Túnel encerrado")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SSL connect
    // ─────────────────────────────────────────────────────────────────────────

    private fun connectSSL(): Socket? {
        return try {
            log("SSL → $currentIp:$currentPort SNI:$currentSni")

            // Trust all — servidor VPN usa cert auto-assinado
            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(c: Array<java.security.cert.X509Certificate>, a: String) {}
                override fun checkServerTrusted(c: Array<java.security.cert.X509Certificate>, a: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })

            val sslCtx = SSLContext.getInstance("TLS")
            sslCtx.init(null, trustAll, java.security.SecureRandom())

            val rawSocket = Socket()
            rawSocket.tcpNoDelay = true
            rawSocket.soTimeout = 30_000
            rawSocket.connect(InetSocketAddress(currentIp, currentPort), 15_000)
            protect(rawSocket)

            val ssl = sslCtx.socketFactory.createSocket(rawSocket, currentSni, currentPort, true) as SSLSocket
            ssl.soTimeout = 30_000

            // Preferir TLSv1.2 / TLSv1.3
            val supported = ssl.supportedProtocols.filter { it == "TLSv1.2" || it == "TLSv1.3" }.toTypedArray()
            if (supported.isNotEmpty()) ssl.enabledProtocols = supported

            // SNI
            val params = SSLParameters()
            params.serverNames = listOf(SNIHostName(currentSni))
            ssl.sslParameters = params

            ssl.startHandshake()
            log("SSL OK — ${ssl.session.protocol} cipher:${ssl.session.cipherSuite}")

            // Handshake HTTP CONNECT para criar túnel sobre SSL (para servidores SSH-over-SSL)
            val out = ssl.getOutputStream()
            val inp = BufferedReader(InputStreamReader(ssl.getInputStream()))

            val connectReq = "CONNECT $currentSni:22 HTTP/1.1\r\nHost: $currentSni\r\nUser-Agent: Mozilla/5.0\r\nProxy-Connection: Keep-Alive\r\n\r\n"
            out.write(connectReq.toByteArray())
            out.flush()
            log("HTTP CONNECT enviado sobre SSL")

            val resp = inp.readLine() ?: ""
            log("HTTP CONNECT resp: $resp")

            if (resp.contains("200")) {
                // Consumir headers restantes
                while (true) {
                    val line = inp.readLine() ?: break
                    if (line.isEmpty()) break
                }
                log("Túnel SSL+HTTP CONNECT estabelecido")
                ssl
            } else {
                // Tentar sem CONNECT — pipe SSL direto
                log("CONNECT falhou, a usar SSL direto")
                ssl
            }
        } catch (e: Exception) {
            log("Erro SSL: ${e.message}")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP CONNECT
    // ─────────────────────────────────────────────────────────────────────────

    private fun connectHTTP(): Socket? {
        return try {
            log("HTTP CONNECT → $currentIp:$currentPort host:$currentSni")
            val socket = Socket()
            socket.tcpNoDelay = true
            socket.soTimeout = 30_000
            socket.connect(InetSocketAddress(currentIp, currentPort), 15_000)
            protect(socket)

            val out = socket.getOutputStream()
            val inp = BufferedReader(InputStreamReader(socket.getInputStream()))

            val req = if (currentPayload.isNotEmpty()) {
                currentPayload
                    .replace("[host]",  currentSni)
                    .replace("[port]",  currentPort.toString())
                    .replace("[crlf]",  "\r\n")
                    .replace("[cr]",    "\r")
                    .replace("[lf]",    "\n")
            } else {
                "CONNECT $currentSni:443 HTTP/1.1\r\n" +
                "Host: $currentSni\r\n" +
                "Proxy-Connection: Keep-Alive\r\n" +
                "User-Agent: Mozilla/5.0\r\n\r\n"
            }

            out.write(req.toByteArray())
            out.flush()
            log("HTTP payload enviado")

            val resp = inp.readLine() ?: ""
            log("HTTP resp: $resp")

            return if (resp.contains("200")) {
                while (true) {
                    val line = inp.readLine() ?: break
                    if (line.isEmpty()) break
                }
                log("HTTP CONNECT OK")
                socket
            } else {
                log("HTTP falhou: $resp")
                socket.close()
                null
            }
        } catch (e: Exception) {
            log("Erro HTTP: ${e.message}")
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun closeTunInterface() {
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
    }

    private fun stopTunnel() {
        serviceJob?.cancel()
        serviceJob = null
        try { tunnelSocket?.close() } catch (_: Exception) {}
        tunnelSocket = null
        closeTunInterface()
        isConnected = false
        onStatusChanged?.invoke(false)
        stopForeground(true)
        stopSelf()
        log("VPN desligada")
    }

    private fun log(msg: String) {
        Log.d(TAG, msg)
        onLogMessage?.invoke(msg)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notificação Foreground
    // ─────────────────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "WeSports VPN", NotificationManager.IMPORTANCE_LOW)
            ch.description = "Estado da VPN"
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("WeSports VPN")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("WeSports VPN")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .build()
        }
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(NOTIF_ID, buildNotification(text))
    }
}