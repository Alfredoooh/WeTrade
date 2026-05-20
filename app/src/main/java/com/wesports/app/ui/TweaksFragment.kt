package com.wesports.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wesports.app.R
import com.wesports.app.databinding.FragmentTweaksBinding
import com.wesports.app.model.Tweak
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.content.Context

class TweaksFragment : Fragment() {

    private var _binding: FragmentTweaksBinding? = null
    private val binding get() = _binding!!
    private val tweaks = mutableListOf<Tweak>()
    private lateinit var adapter: TweakAdapter

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
            val sheet = AddTweakBottomSheet { tweak ->
                tweaks.add(tweak)
                saveTweaks()
                adapter.notifyDataSetChanged()
            }
            sheet.show(parentFragmentManager, "AddTweak")
        }
    }

    private fun loadTweaks() {
        val prefs = requireContext().getSharedPreferences("tweaks", Context.MODE_PRIVATE)
        val json = prefs.getString("list", "[]")
        val type = object : TypeToken<List<Tweak>>() {}.type
        val loaded: List<Tweak> = Gson().fromJson(json, type)
        tweaks.clear()
        tweaks.addAll(loaded)
        adapter.notifyDataSetChanged()
    }

    private fun saveTweaks() {
        val prefs = requireContext().getSharedPreferences("tweaks", Context.MODE_PRIVATE)
        prefs.edit().putString("list", Gson().toJson(tweaks)).apply()
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
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvMode: TextView = view.findViewById(R.id.tvMode)
        val btnDelete: android.widget.Button = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tweak, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tweak = tweaks[position]
        holder.tvName.text = tweak.name
        holder.tvMode.text = "${tweak.mode} · ${tweak.type}"
        holder.btnDelete.setOnClickListener { onDelete(tweak) }
    }

    override fun getItemCount() = tweaks.size
}