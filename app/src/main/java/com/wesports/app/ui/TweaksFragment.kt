package com.wesports.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wesports.app.R
import com.wesports.app.databinding.FragmentTweaksBinding
import com.wesports.app.model.Tweak

class TweaksFragment : Fragment() {

    private var _binding: FragmentTweaksBinding? = null
    private val binding get() = _binding!!
    private val tweaks = mutableListOf<Tweak>()
    private lateinit var adapter: TweakAdapter

    private val addTweakLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadTweaks()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTweaksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TweakAdapter(tweaks) { tweak ->
            tweaks.remove(tweak)
            saveTweaks()
            adapter.notifyDataSetChanged()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        loadTweaks()

        binding.btnAdd.setOnClickListener {
            val intent = Intent(requireContext(), TweakActivity::class.java)
            addTweakLauncher.launch(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadTweaks()
    }

    private fun loadTweaks() {
        if (_binding == null) return
        try {
            val prefs = requireContext().getSharedPreferences("tweaks", Context.MODE_PRIVATE)
            val json  = prefs.getString("list", "[]") ?: "[]"
            val type  = object : TypeToken<List<Tweak>>() {}.type
            val loaded: List<Tweak> = Gson().fromJson(json, type)
            tweaks.clear()
            tweaks.addAll(loaded)
            adapter.notifyDataSetChanged()
        } catch (_: Exception) { tweaks.clear() }
    }

    private fun saveTweaks() {
        try {
            val prefs = requireContext().getSharedPreferences("tweaks", Context.MODE_PRIVATE)
            prefs.edit().putString("list", Gson().toJson(tweaks)).apply()
        } catch (_: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class TweakAdapter(
    private val tweaks: List<Tweak>,
    private val onDelete: (Tweak) -> Unit
) : RecyclerView.Adapter<TweakAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView   = view.findViewById(R.id.tvName)
        val tvMode: TextView   = view.findViewById(R.id.tvMode)
        val btnDelete: Button  = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_tweak, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = tweaks[position]
        holder.tvName.text  = t.name
        holder.tvMode.text  = "${t.mode} · ${t.type}" + if (t.sni.isNotEmpty()) " · ${t.sni}" else ""
        holder.btnDelete.setOnClickListener { onDelete(t) }
    }

    override fun getItemCount() = tweaks.size
}