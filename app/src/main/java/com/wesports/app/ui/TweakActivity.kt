package com.wesports.app.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wesports.app.databinding.ActivityTweakBinding
import com.wesports.app.model.Tweak
import java.util.UUID

class TweakActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTweakBinding

    // SNI pré-definidos por operadora/uso
    private val sniPresets = listOf(
        "free.facebook.com",
        "zero.facebook.com",
        "graph.facebook.com",
        "b-api.facebook.com",
        "static.xx.fbcdn.net",
        "edge-mqtt.facebook.com",
        "www.unitel.ao",
        "internet.unitel.ao",
        "wap.unitel.ao",
        "streaming.unitel.ao",
        "www.movicel.ao",
        "movicel.ao",
        "www.africell.ao",
        "www.claro.com.br",
        "zero.claro.com.br",
        "www.tim.com.br",
        "www.vivo.com.br",
        "tunnel.spotify.com",
        "www.instagram.com",
        "www.whatsapp.com",
        "wss.whatsapp.net",
        "www.tiktok.com",
        "www.youtube.com",
        "clients1.google.com",
        "www.google.com",
        "accounts.google.com",
        "Personalizado..."
    )

    private val modes = listOf("SSL", "SSL PROXY", "HTTP", "DIRECT")
    private val types = listOf("OCSWS", "PAYLOAD", "SSH", "V2RAY", "TROJAN")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTweakBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Spinners
        binding.spinnerMode.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        binding.spinnerType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)
        binding.spinnerSni.adapter  = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sniPresets)

        // Modo muda visibilidade
        binding.spinnerMode.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val m = modes[pos]
                binding.layoutSni.visibility     = if (m == "SSL" || m == "SSL PROXY") View.VISIBLE else View.GONE
                binding.layoutPayload.visibility  = if (m == "HTTP") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        })

        // SNI preset muda campo manual
        binding.spinnerSni.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val selected = sniPresets[pos]
                if (selected == "Personalizado...") {
                    binding.etSniCustom.visibility = View.VISIBLE
                    binding.etSniCustom.requestFocus()
                } else {
                    binding.etSniCustom.visibility = View.GONE
                    binding.etSniCustom.setText("")
                }
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        })

        // Default: SSL com free.facebook.com visível
        binding.spinnerMode.setSelection(0)

        binding.btnSave.setOnClickListener { save() }
    }

    private fun save() {
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Insere um nome", Toast.LENGTH_SHORT).show()
            return
        }

        val mode = binding.spinnerMode.selectedItem.toString()

        val sni = when {
            mode != "SSL" && mode != "SSL PROXY" -> ""
            binding.etSniCustom.visibility == View.VISIBLE -> {
                binding.etSniCustom.text.toString().trim().ifEmpty {
                    Toast.makeText(this, "Insere um SNI personalizado", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            else -> {
                val sel = binding.spinnerSni.selectedItem.toString()
                if (sel == "Personalizado...") {
                    Toast.makeText(this, "Insere um SNI personalizado", Toast.LENGTH_SHORT).show()
                    return
                }
                sel
            }
        }

        if ((mode == "SSL" || mode == "SSL PROXY") && sni.isEmpty()) {
            Toast.makeText(this, "SNI obrigatório para modo SSL", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = if (mode == "HTTP") binding.etPayload.text.toString().trim() else ""

        val tweak = Tweak(
            id            = UUID.randomUUID().toString(),
            name          = name,
            message       = "",
            mode          = mode,
            type          = binding.spinnerType.selectedItem.toString(),
            sni           = sni,
            payload       = payload,
            expirationDate = false,
            hwid          = false,
            passwordLock  = false,
            mobileDataOnly = binding.cbMobileData.isChecked,
            blockRooted    = binding.cbBlockRooted.isChecked
        )

        saveTweak(tweak)
        setResult(RESULT_OK)
        finish()
    }

    private fun saveTweak(tweak: Tweak) {
        val prefs = getSharedPreferences("tweaks", Context.MODE_PRIVATE)
        val json  = prefs.getString("list", "[]") ?: "[]"
        val type  = object : com.google.gson.reflect.TypeToken<MutableList<Tweak>>() {}.type
        val list: MutableList<Tweak> = Gson().fromJson(json, type)
        list.add(tweak)
        prefs.edit().putString("list", Gson().toJson(list)).apply()
    }
}