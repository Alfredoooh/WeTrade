package com.wilin.app.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.wilin.app.databinding.BottomSheetAddTweakBinding
import com.wilin.app.model.Tweak
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

        binding.spinnerMode.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
                val mode = modes[position]
                if (mode == "SSL" || mode == "SSL PROXY") {
                    binding.layoutSni.visibility = View.VISIBLE
                } else {
                    binding.layoutSni.visibility = View.GONE
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Insere um nome", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val mode = binding.spinnerMode.selectedItem.toString()
            val sni = binding.etSni.text.toString().trim()

            if ((mode == "SSL" || mode == "SSL PROXY") && sni.isEmpty()) {
                Toast.makeText(requireContext(), "Insere uma SNI válida (ex: free.facebook.com)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tweak = Tweak(
                id = UUID.randomUUID().toString(),
                name = name,
                message = binding.etMessage.text.toString().trim(),
                mode = mode,
                type = binding.spinnerType.selectedItem.toString(),
                sni = sni,
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