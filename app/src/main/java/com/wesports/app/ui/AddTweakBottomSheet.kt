package com.wesports.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wesports.app.databinding.BottomSheetAddTweakBinding
import com.wesports.app.model.Tweak
import java.util.UUID

class AddTweakBottomSheet(
    private val onSave: (Tweak) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddTweakBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetAddTweakBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val modes = listOf("DIRECT", "HTTP", "SSL", "SSL PROXY", "UDPQ", "SHADOWSOCKS")
        val types = listOf("OCSWS", "PAYLOAD", "SSH", "V2RAY", "TROJAN")

        binding.spinnerMode.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, modes)
        binding.spinnerType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Insere um nome", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val tweak = Tweak(
                id = UUID.randomUUID().toString(),
                name = name,
                message = binding.etMessage.text.toString().trim(),
                mode = binding.spinnerMode.selectedItem.toString(),
                type = binding.spinnerType.selectedItem.toString(),
                expirationDate = binding.cbExpiration.isChecked,
                hwid = binding.cbHwid.isChecked,
                passwordLock = binding.cbPassword.isChecked,
                mobileDataOnly = binding.cbMobileData.isChecked,
                blockRooted = binding.cbBlockRooted.isChecked
            )
            onSave(tweak)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}