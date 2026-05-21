package com.wilin.app.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wilin.app.MainActivity
import com.wilin.app.databinding.FragmentHomeConnectionBinding
import com.wilin.app.model.Tweak
import com.wilin.app.vpn.AppVpnService
import com.wilin.app.vpn.VpnManager

class HomeConnectionFragment : Fragment() {

    private var _binding: FragmentHomeConnectionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()

        AppVpnService.onStatusChanged = {
            activity?.runOnUiThread { if (_binding != null) updateUI() }
        }

        binding.btnConnect.setOnClickListener {
            if (VpnManager.isConnected()) {
                VpnManager.disconnect(requireContext())
            } else {
                val activity = activity as? MainActivity ?: return@setOnClickListener
                val server = activity.selectedServer
                if (server == null) {
                    binding.tvStatus.text = "Seleciona um servidor primeiro"
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
            binding.btnConnect.text = "Parar"
            binding.btnConnect.setBackgroundColor(android.graphics.Color.parseColor("#CC0000"))
            binding.tvServerInfo.text = server?.let { "${it.ip} · ${it.country}" } ?: ""
        } else {
            binding.tvStatus.text = "Desconectado"
            binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#888888"))
            binding.btnConnect.text = "Iniciar"
            binding.btnConnect.setBackgroundColor(android.graphics.Color.parseColor("#007AFF"))
            binding.tvServerInfo.text = server?.let { "Servidor: ${it.ip}" } ?: "Nenhum servidor selecionado"
        }
    }

    override fun onDestroyView() {
        AppVpnService.onStatusChanged = null
        super.onDestroyView()
        _binding = null
    }
}