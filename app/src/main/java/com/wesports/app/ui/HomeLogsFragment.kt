package com.wesports.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.wesports.app.databinding.FragmentHomeLogsBinding
import com.wesports.app.vpn.AppVpnService

class HomeLogsFragment : Fragment() {

    private var _binding: FragmentHomeLogsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        AppVpnService.onLogMessage = { msg ->
            activity?.runOnUiThread {
                if (_binding != null) {
                    binding.tvLogs.append("\n$msg")
                    binding.scrollLogs.post { binding.scrollLogs.fullScroll(View.FOCUS_DOWN) }
                }
            }
        }

        binding.btnClearLogs.setOnClickListener {
            binding.tvLogs.text = "Logs aparecerão aqui..."
        }
    }

    override fun onDestroyView() {
        AppVpnService.onLogMessage = null
        super.onDestroyView()
        _binding = null
    }
}