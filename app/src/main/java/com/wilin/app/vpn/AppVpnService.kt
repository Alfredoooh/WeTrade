package com.wilin.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.wilin.app.MainActivity
import com.wilin.app.R
import kotlinx.coroutines.*
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.*

class AppVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private var tunnelSocket: Socket? = null

    private var currentIp      = ""
    private var currentPort    = 443
    private var currentMode    = "SSL"
    private var currentSni     = "free.facebook.com"
    private var currentPayload = ""

    private var reconnectAttempts = 0

    companion object {
        const val ACTION_CONNECT    = "com.wilin.app.VPN_CONNECT"
        const val ACTION_DISCONNECT = "com.wilin.app.VPN_DISCONNECT"
        const val EXTRA_SERVER_IP   = "server_ip"
        const val EXTRA_SERVER_PORT = "server_port"
        const val EXTRA_MODE        = "mode"
        const val EXTRA_SNI         = "sni"
        const val EXTRA_PAYLOAD     = "payload"
        const val TAG               = "AppVpnService"
        const val NOTIF_ID          = 1
        const val CHANNEL_ID        = "vpn_channel"

        @Volatile var isConnected    = false
        @Volatile var onStatusChanged: ((Boolean) -> Unit)? = null
        @Volatile var onLogMessage:    ((String)  -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return when (intent?.action) {
            ACTION_CONNECT -> {
                currentIp      = intent.getStringExtra(EXTRA_SERVER_IP)   ?: ""
                currentPort    = intent.getIntExtra(EXTRA_SERVER_PORT, 443)
                currentMode    = intent.getStringExtra(EXTRA_MODE)        ?: "SSL"
                currentSni     = intent.getStringExtra(EXTRA_SNI)?.ifEmpty { "free.facebook.com" } ?: "free.facebook.com"
                currentPayload = intent.getStringExtra(EXTRA_PAYLOAD)     ?: ""

                if (currentIp.isEmpty()) {
                    log("IP vazio — abortar")
                    START_NOT_STICKY
                } else {
                    startForeground(NOTIF_ID, buildNotification("A conectar a $currentIp…"))
                    reconnectAttempts = 0
                    startTunnelLoop()
                    START_STICKY
                }
            }
            ACTION_DISCONNECT -> {
                stopEverything()
                START_NOT_STICKY
            }
            else -> START_NOT_STICKY
        }
    }

    // ─── Loop principal com reconexão exponencial ───────────────────────────

    private fun startTunnelLoop() {
        serviceJob?.cancel()
        serviceJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            while (isActive) {
                try {
                    runTunnel()
                } catch (e: Exception) {
                    log("Túnel caiu: ${e.message}")
                }

                if (!isActive) break

                isConnected = false
                onStatusChanged?.invoke(false)

                // Backoff exponencial: 3s, 6s, 12s, 24s, máximo 30s
                val delay = minOf(3_000L * (1L shl reconnectAttempts.coerceAtMost(3)), 30_000L)
                reconnectAttempts++
                log("Reconectar em ${delay / 1000}s (tentativa $reconnectAttempts)")
                updateNotification("A reconectar em ${delay / 1000}s…")
                delay(delay)
            }
        }
    }

    // ─── Tunnel core ────────────────────────────────────────────────────────

    private suspend fun runTunnel() = withContext(Dispatchers.IO) {
        log("Modo=$currentMode IP=$currentIp:$currentPort SNI=$currentSni")

        closeTun()

        val socket: Socket = when (currentMode.uppercase()) {
            "SSL", "SSL PROXY" -> openSSL() ?: run { log("SSL falhou"); return@withContext }
            "HTTP"             -> openHTTP() ?: run { log("HTTP falhou"); return@withContext }
            else               -> openSSL()  ?: run { log("Fallback SSL falhou"); return@withContext }
        }

        tunnelSocket = socket

        // ── Interface VPN ────────────────────────────────────────────────────
        val pfd = Builder()
            .setSession("wilin VPN")
            .addAddress("10.8.0.2", 24)
            .addDnsServer("8.8.8.8")
            .addDnsServer("1.1.1.1")
            .addDnsServer("9.9.9.9")
            .addRoute("0.0.0.0", 0)
            .setMtu(1400)
            .setBlocking(true)
            .establish()
            ?: run { log("Falha establish() — sem permissão VPN"); socket.close(); return@withContext }

        vpnInterface = pfd
        isConnected  = true
        reconnectAttempts = 0
        onStatusChanged?.invoke(true)
        updateNotification("Conectado · $currentIp · SNI:$currentSni")
        log("VPN ativa · interface criada")

        val tunIn  = FileInputStream(pfd.fileDescriptor)
        val tunOut = FileOutputStream(pfd.fileDescriptor)
        val sOut   = socket.getOutputStream()
        val sIn    = socket.getInputStream()

        // Keepalive a cada 25s
        val ka = launch {
            while (isActive && !socket.isClosed) {
                delay(25_000)
                try { sOut.write(byteArrayOf(0)); sOut.flush() } catch (_: Exception) { break }
            }
        }

        // Tun → Servidor (com framing 2-byte length prefix)
        val t2s = launch {
            val buf = ByteArray(32767)
            while (isActive && !socket.isClosed) {
                try {
                    val len = tunIn.read(buf)
                    if (len > 0) {
                        val frame = ByteArray(2 + len)
                        frame[0] = ((len shr 8) and 0xFF).toByte()
                        frame[1] = (len and 0xFF).toByte()
                        System.arraycopy(buf, 0, frame, 2, len)
                        sOut.write(frame)
                        sOut.flush()
                    }
                } catch (e: Exception) { log("tun→srv: ${e.message}"); break }
            }
        }

        // Servidor → Tun (lê 2-byte length prefix)
        val s2t = launch {
            val hdr = ByteArray(2)
            val buf = ByteArray(32767)
            while (isActive && !socket.isClosed) {
                try {
                    var r = 0
                    while (r < 2) {
                        val n = sIn.read(hdr, r, 2 - r)
                        if (n < 0) { log("Servidor fechou"); return@launch }
                        r += n
                    }
                    val len = ((hdr[0].toInt() and 0xFF) shl 8) or (hdr[1].toInt() and 0xFF)
                    if (len in 1..32767) {
                        var got = 0
                        while (got < len) {
                            val n = sIn.read(buf, got, len - got)
                            if (n < 0) { log("Servidor fechou mid-packet"); return@launch }
                            got += n
                        }
                        tunOut.write(buf, 0, len)
                    }
                } catch (e: Exception) { log("srv→tun: ${e.message}"); break }
            }
        }

        t2s.join(); s2t.join(); ka.cancel()

        closeTun()
        try { socket.close() } catch (_: Exception) {}
        isConnected = false
        onStatusChanged?.invoke(false)
        log("Túnel encerrado")
    }

    // ─── SSL ────────────────────────────────────────────────────────────────

    private fun openSSL(): Socket? {
        return try {
            log("SSL → $currentIp:$currentPort SNI:$currentSni")

            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(c: Array<java.security.cert.X509Certificate>, a: String) {}
                override fun checkServerTrusted(c: Array<java.security.cert.X509Certificate>, a: String) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })
            val ctx = SSLContext.getInstance("TLS")
            ctx.init(null, trustAll, java.security.SecureRandom())

            // ⚠️ protect() ANTES do handshake para não criar loop de roteamento
            val raw = Socket()
            raw.tcpNoDelay = true
            raw.soTimeout  = 30_000
            raw.connect(InetSocketAddress(currentIp, currentPort), 15_000)
            protect(raw)   // ← crítico: excluir este socket do túnel VPN

            val ssl = ctx.socketFactory.createSocket(raw, currentSni, currentPort, true) as SSLSocket
            ssl.soTimeout = 30_000

            val protos = ssl.supportedProtocols.filter { it == "TLSv1.2" || it == "TLSv1.3" }.toTypedArray()
            if (protos.isNotEmpty()) ssl.enabledProtocols = protos

            val p = SSLParameters()
            p.serverNames = listOf(SNIHostName(currentSni))
            ssl.sslParameters = p

            ssl.startHandshake()
            log("SSL OK — ${ssl.session.protocol} / ${ssl.session.cipherSuite}")

            // HTTP CONNECT sobre SSL para estabelecer túnel ao SSH interno
            val out = ssl.getOutputStream()
            val inp = BufferedReader(InputStreamReader(ssl.getInputStream()))

            val connectReq = "CONNECT $currentSni:22 HTTP/1.1\r\nHost: $currentSni\r\nUser-Agent: Mozilla/5.0\r\nProxy-Connection: Keep-Alive\r\n\r\n"
            out.write(connectReq.toByteArray())
            out.flush()
            log("HTTP CONNECT enviado")

            val resp = inp.readLine() ?: ""
            log("HTTP CONNECT resp: $resp")

            if (resp.contains("200")) {
                var line = inp.readLine()
                while (!line.isNullOrEmpty()) { line = inp.readLine() }
                log("HTTP CONNECT OK — túnel SSL estabelecido")
            } else {
                log("HTTP CONNECT falhou — SSL direto sem proxy")
            }
            ssl
        } catch (e: Exception) {
            log("Erro SSL: ${e.message}")
            null
        }
    }

    // ─── HTTP CONNECT ───────────────────────────────────────────────────────

    private fun openHTTP(): Socket? {
        return try {
            log("HTTP → $currentIp:$currentPort host:$currentSni")

            val socket = Socket()
            socket.tcpNoDelay = true
            socket.soTimeout  = 30_000
            socket.connect(InetSocketAddress(currentIp, currentPort), 15_000)
            protect(socket)  // ← crítico

            val out = socket.getOutputStream()
            val inp = BufferedReader(InputStreamReader(socket.getInputStream()))

            val req = if (currentPayload.isNotEmpty()) {
                currentPayload
                    .replace("[host]", currentSni)
                    .replace("[port]", currentPort.toString())
                    .replace("[crlf]", "\r\n")
                    .replace("[cr]",   "\r")
                    .replace("[lf]",   "\n")
            } else {
                "CONNECT $currentSni:443 HTTP/1.1\r\nHost: $currentSni\r\nProxy-Connection: Keep-Alive\r\nUser-Agent: Mozilla/5.0\r\n\r\n"
            }

            out.write(req.toByteArray())
            out.flush()
            log("HTTP payload enviado")

            val resp = inp.readLine() ?: ""
            log("HTTP resp: $resp")

            return if (resp.contains("200")) {
                var line = inp.readLine()
                while (!line.isNullOrEmpty()) { line = inp.readLine() }
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

    // ─── Helpers ────────────────────────────────────────────────────────────

    private fun closeTun() {
        try { vpnInterface?.close() } catch (_: Exception) {}
        vpnInterface = null
    }

    private fun stopEverything() {
        serviceJob?.cancel()
        serviceJob = null
        try { tunnelSocket?.close() } catch (_: Exception) {}
        tunnelSocket = null
        closeTun()
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

    // ─── Notificação ────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "wilin VPN", NotificationManager.IMPORTANCE_LOW)
            ch.description = "Estado da ligação VPN"
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
                .setContentTitle("wilin VPN")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("wilin VPN")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pi)
                .setOngoing(true)
                .build()
        }
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, buildNotification(text))
    }

    override fun onRevoke()  { stopEverything(); super.onRevoke() }
    override fun onDestroy() { stopEverything(); super.onDestroy() }
}