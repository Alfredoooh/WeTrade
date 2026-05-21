package com.wesports.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wesports.app.MainActivity
import com.wesports.app.R
import com.wesports.app.databinding.FragmentServersBinding
import com.wesports.app.model.Server
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ServersFragment : Fragment() {

    private var _binding: FragmentServersBinding? = null
    private val binding get() = _binding!!
    private val servers = mutableListOf<Server>()
    private lateinit var adapter: ServerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentServersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ServerAdapter(servers) { server ->
            val activity = activity as? MainActivity ?: return@ServerAdapter
            activity.selectedServer = server
            Toast.makeText(requireContext(), "Servidor selecionado: ${server.ip}", Toast.LENGTH_SHORT).show()
            // Navegar para Home e iniciar VPN
            activity.binding.bottomNav.selectedItemId = R.id.nav_home
            activity.requestVpnPermission {
                com.wesports.app.vpn.VpnManager.connect(requireContext(), server)
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        loadServers()
    }

    private fun loadServers() {
        if (_binding == null) return
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()
                    val request = Request.Builder()
                        .url("https://registrovpn.onrender.com/servers")
                        .build()
                    client.newCall(request).execute().body?.string()
                }
                if (_binding == null) return@launch
                result?.let {
                    val json = JSONObject(it)
                    val array = json.getJSONArray("servers")
                    servers.clear()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val pingMs = obj.optInt("ping", 0)
                        servers.add(
                            Server(
                                ip = obj.optString("ip", "-"),
                                country = obj.optString("country", "-"),
                                ping = "${pingMs} ms",
                                speed = "0 Mbps",
                                ovpn = obj.optString("ovpn", ""),
                                port = obj.optInt("port", 443),
                                type = obj.optString("type", "SSL")
                            )
                        )
                    }
                    // Ordenar por ping
                    servers.sortBy { s -> s.ping.replace(" ms", "").toIntOrNull() ?: 9999 }
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                if (_binding == null) return@launch
                Toast.makeText(requireContext(), "Erro ao carregar servidores: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                if (_binding != null) binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ServerAdapter(
    private val servers: List<Server>,
    private val onConnect: (Server) -> Unit
) : RecyclerView.Adapter<ServerAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvIp: TextView = view.findViewById(R.id.tvIp)
        val tvCountry: TextView = view.findViewById(R.id.tvCountry)
        val tvPing: TextView = view.findViewById(R.id.tvPing)
        val tvSpeed: TextView = view.findViewById(R.id.tvSpeed)
        val btnConnect: Button = view.findViewById(R.id.btnConnect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_server, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val s = servers[position]
        holder.tvIp.text = s.ip
        holder.tvCountry.text = "${s.country} · ${s.type}"
        holder.tvPing.text = s.ping
        holder.tvSpeed.text = s.speed
        holder.btnConnect.setOnClickListener { onConnect(s) }
    }

    override fun getItemCount() = servers.size
}