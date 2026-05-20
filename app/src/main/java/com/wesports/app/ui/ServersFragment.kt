package com.wesports.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View.OnClickListener
import android.widget.TextView
import com.wesports.app.R
import com.wesports.app.databinding.FragmentServersBinding
import com.wesports.app.model.Server
import com.wesports.app.vpn.VpnManager
import com.wesports.app.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class ServersFragment : Fragment() {

    private var _binding: FragmentServersBinding? = null
    private val binding get() = _binding!!
    private val client = OkHttpClient()
    private val servers = mutableListOf<Server>()
    private lateinit var adapter: ServerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentServersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ServerAdapter(servers) { server ->
            (activity as MainActivity).requestVpnPermission {
                VpnManager.connect(requireContext(), server)
                Toast.makeText(requireContext(), "A conectar a ${server.ip}...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        loadServers()
    }

    private fun loadServers() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://registrovpn.onrender.com/servers")
                        .build()
                    val response = client.newCall(request).execute()
                    response.body?.string()
                }
                result?.let {
                    val json = JSONObject(it)
                    val array = json.getJSONArray("servers")
                    servers.clear()
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val speedRaw = obj.optString("speed", "0")
                        val speedMbps = try { (speedRaw.toLong() / 1_000_000).toString() + " Mbps" } catch (e: Exception) { "-" }
                        servers.add(Server(
                            ip = obj.optString("ip", "-"),
                            country = obj.optString("country", "-"),
                            ping = obj.optString("ping", "-") + " ms",
                            speed = speedMbps,
                            ovpn = obj.optString("ovpn", "")
                        ))
                    }
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Erro ao carregar servidores", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
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
        val btnConnect: android.widget.Button = view.findViewById(R.id.btnConnect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_server, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val server = servers[position]
        holder.tvIp.text = server.ip
        holder.tvCountry.text = server.country
        holder.tvPing.text = server.ping
        holder.tvSpeed.text = server.speed
        holder.btnConnect.setOnClickListener { onConnect(server) }
    }

    override fun getItemCount() = servers.size
}