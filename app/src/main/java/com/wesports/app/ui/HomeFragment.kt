package com.wesports.app.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wesports.app.MainActivity
import com.wesports.app.databinding.FragmentHomeBinding
import com.wesports.app.model.Tweak
import com.wesports.app.vpn.AppVpnService
import com.wesports.app.vpn.VpnManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()

        AppVpnService.onStatusChanged = {
            activity?.runOnUiThread { if (_binding != null) updateUI() }
        }

        AppVpnService.onLogMessage = { msg ->
            activity?.runOnUiThread {
                if (_binding != null) {
                    binding.tvLogs.append("\n$msg")
                    binding.scrollLogs.post { binding.scrollLogs.fullScroll(View.FOCUS_DOWN) }
                }
            }
        }

        binding.btnConnect.setOnClickListener {
            if (VpnManager.isConnected()) {
                VpnManager.disconnect(requireContext())
            } else {
                val activity = activity as? MainActivity ?: return@setOnClickListener
                val server = activity.selectedServer
                if (server == null) {
                    binding.tvLogs.append("\nSeleciona um servidor primeiro em Servidores")
                    return@setOnClickListener
                }
                val tweak = loadFirstTweak()
                activity.requestVpnPermission {
                    if (tweak != null) {
                        VpnManager.connectWithTweak(requireContext(), server, tweak)
                    } else {
                        VpnManager.connect(requireContext(), server)
                    }
                }
            }
        }
    }

    private fun loadFirstTweak(): Tweak? {
        return try {
            val prefs = requireContext().getSharedPreferences("tweaks", Context.MODE_PRIVATE)
            val json = prefs.getString("list", "[]") ?: "[]"
            val type = object : TypeToken<List<Tweak>>() {}.type
            val list: List<Tweak> = Gson().fromJson(json, type)
            list.firstOrNull()
        } catch (e: Exception) { null }
    }

    private fun updateUI() {
        if (!isAdded || _binding == null) return
        val server = (activity as? MainActivity)?.selectedServer
        if (VpnManager.isConnected()) {
            binding.tvStatus.text = "Conectado"
            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#1a7a1a"))
            binding.btnConnect.text = "Desconectar"
            binding.btnConnect.setBackgroundColor(android.graphics.Color.parseColor("#CC0000"))
        } else {
            binding.tvStatus.text = if (server != null) "Servidor: ${server.ip}" else "Seleciona um servidor"
            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#888888"))
            binding.btnConnect.text = "Conectar"
            binding.btnConnect.setBackgroundColor(android.graphics.Color.parseColor("#111111"))
        }
    }

    override fun onDestroyView() {
        AppVpnService.onStatusChanged = null
        AppVpnService.onLogMessage = null
        super.onDestroyView()
        _binding = null
    }
}