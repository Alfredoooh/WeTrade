package com.wesports.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.wesports.app.MainActivity
import com.wesports.app.databinding.FragmentHomeBinding
import com.wesports.app.vpn.AppVpnService
import com.wesports.app.vpn.VpnManager
import com.wesports.app.model.Server

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var selectedServer: Server? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()

        AppVpnService.onStatusChanged = { connected ->
            activity?.runOnUiThread { updateUI() }
        }

        binding.btnConnect.setOnClickListener {
            if (VpnManager.isConnected()) {
                VpnManager.disconnect(requireContext())
            } else {
                val server = selectedServer ?: Server("185.157.160.1", "JP", "10", "10000000", "")
                (activity as MainActivity).requestVpnPermission {
                    VpnManager.connect(requireContext(), server)
                }
            }
        }
    }

    private fun updateUI() {
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
        super.onDestroyView()
        _binding = null
    }
}