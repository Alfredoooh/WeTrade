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
import com.wesports.app.model.Server
import com.wesports.app.model.Tweak
import com.wesports.app.vpn.AppVpnService
import com.wesports.app.vpn.VpnManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var selectedServer: Server? = null
    private var selectedTweak: Tweak? = null

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
                    val current = binding.tvLogs.text.toString()
                    binding.tvLogs.text = "$current\n$msg"
                    binding.scrollLogs.post { binding.scrollLogs.fullScroll(View.FOCUS_DOWN) }
                }
            }
        }

        binding.btnConnect.setOnClickListener {
            if (VpnManager.isConnected()) {
                VpnManager.disconnect(requireContext())
            } else {
                val server = selectedServer ?: Server("185.157.160.1", "JP", "10", "10 Mbps", "")
                val tweak = selectedTweak ?: loadFirstTweak()
                (activity as? MainActivity)?.requestVpnPermission {
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
        if (VpnManager.isConnected()) {
            binding.tvStatus.text = "Conectado"
            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#1a7a1a"))
            binding.btnConnect.text = "Desconectar"
            binding.btnConnect.setBackgroundColor(android.graphics.Color.parseColor("#CC0000"))
        } else {
            binding.tvStatus.text = "Desconectado"
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