package com.wilin.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.wilin.app.R
import com.wilin.app.databinding.FragmentSearchBinding

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val searchHistory = mutableListOf<String>()
    private var historyAdapter: SearchHistoryAdapter? = null

    companion object {
        private const val PREFS_HISTORY = "wilin_search_history"
        private const val KEY_HISTORY   = "history"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ctx       = requireContext()
        val iconTint  = ContextCompat.getColor(ctx, R.color.icon_tint)
        val iconSec   = ContextCompat.getColor(ctx, R.color.icon_tint_secondary)

        binding.searchIcon.setImageDrawable(svgDrawable("icons/svg/magnifying_glass_outline.svg", 20, iconSec))
        binding.searchClearBtn.setImageDrawable(svgDrawable("icons/svg/close.svg", 16, iconSec))

        loadHistory()

        historyAdapter = SearchHistoryAdapter(searchHistory) { query ->
            navigate(query)
        }
        binding.searchHistoryList.apply {
            layoutManager = LinearLayoutManager(ctx)
            adapter = historyAdapter
        }

        // Animação de entrada: transform container sobe + fade in
        binding.searchContainer.post {
            binding.searchContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(280)
                .setInterpolator(DecelerateInterpolator(2f))
                .start()
        }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                val text = s?.toString() ?: ""
                binding.searchClearBtn.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
                filterHistory(text)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchClearBtn.setOnClickListener {
            binding.searchInput.setText("")
        }

        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchInput.text.toString().trim()
                if (query.isNotEmpty()) navigate(query)
                true
            } else false
        }

        // Foco automático com teclado
        binding.searchInput.requestFocus()
        binding.searchInput.post {
            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.searchInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-animar ao voltar para o fragment
        binding.searchContainer.alpha   = 0f
        binding.searchContainer.scaleX  = 0.92f
        binding.searchContainer.scaleY  = 0.92f
        binding.searchContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator(2f))
            .start()
    }

    private fun navigate(input: String) {
        addToHistory(input)
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
        val intent = Intent(requireContext(), BrowserResponseActivity::class.java).apply {
            putExtra(BrowserResponseActivity.EXTRA_QUERY, input)
        }
        startActivity(intent)
    }

    private fun filterHistory(query: String) {
        val filtered = if (query.isEmpty()) searchHistory.take(8)
        else searchHistory.filter { it.contains(query, ignoreCase = true) }.take(8)

        if (filtered.isEmpty()) {
            binding.searchHistoryLabel.visibility = View.GONE
            binding.searchHistoryList.visibility  = View.GONE
        } else {
            binding.searchHistoryLabel.visibility = View.VISIBLE
            binding.searchHistoryList.visibility  = View.VISIBLE
            historyAdapter?.updateList(filtered)
        }
    }

    private fun loadHistory() {
        val prefs = requireContext().getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
        val raw   = prefs.getString(KEY_HISTORY, "") ?: ""
        searchHistory.clear()
        if (raw.isNotEmpty()) searchHistory.addAll(raw.split("|||").filter { it.isNotEmpty() })
    }

    private fun addToHistory(query: String) {
        searchHistory.remove(query)
        searchHistory.add(0, query)
        if (searchHistory.size > 50) searchHistory.removeAt(searchHistory.lastIndex)
        requireContext()
            .getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
            .edit().putString(KEY_HISTORY, searchHistory.joinToString("|||")).apply()
    }

    private fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
        val px  = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val svg = SVG.getFromAsset(requireContext().assets, path)
        svg.documentWidth  = px.toFloat()
        svg.documentHeight = px.toFloat()
        svg.renderToCanvas(Canvas(bmp))
        val drawable = BitmapDrawable(resources, bmp)
        drawable.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
        return drawable
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class SearchHistoryAdapter(
    private var items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SearchHistoryAdapter.VH>() {

    inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val tv = TextView(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            val hPad = (16 * context.resources.displayMetrics.density).toInt()
            val vPad = (12 * context.resources.displayMetrics.density).toInt()
            setPadding(hPad, vPad, hPad, vPad)
            textSize = 15f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)
        }
        return VH(tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.tv.text = items[position]
        holder.tv.setOnClickListener { onClick(items[position]) }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }
}