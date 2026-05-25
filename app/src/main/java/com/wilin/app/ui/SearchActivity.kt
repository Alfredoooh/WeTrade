package com.wilin.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.caverock.androidsvg.SVG
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.wilin.app.R
import com.wilin.app.databinding.ActivitySearchBinding

// Data class ao nível do ficheiro — sem data class local dentro de funções
private data class SearchPopupItem(val icon: String, val label: String, val action: () -> Unit)

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var insetsController: WindowInsetsControllerCompat
    private val searchHistory = mutableListOf<String>()
    private var historyAdapter: SearchSuggestAdapter? = null

    companion object {
        private const val PREFS_HISTORY = "wilin_search_history"
        private const val KEY_HISTORY   = "history"
        private const val SEARCH_TRANSITION_NAME = "search_container_transition"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.searchInputContainer.transitionName = SEARCH_TRANSITION_NAME

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = android.R.id.content
            scrimColor = Color.TRANSPARENT
            duration = 300L
        }
        window.sharedElementReturnTransition = MaterialContainerTransform().apply {
            drawingViewId = android.R.id.content
            scrimColor = Color.TRANSPARENT
            duration = 220L
        }

        insetsController = WindowInsetsControllerCompat(window, window.decorView)
        window.statusBarColor = ContextCompat.getColor(this, R.color.appbar_background)
        applyStatusBarTheme()

        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)
        val iconSec  = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        binding.btnBack.setImageDrawable(svgDrawable("icons/svg/back_arrow.svg", 24, iconTint))
        binding.btnBack.setOnClickListener { finishWithAnim() }
        binding.searchIcon.setImageDrawable(svgDrawable("icons/svg/magnifying_glass_outline.svg", 20, iconSec))
        binding.btnClear.setImageDrawable(svgDrawable("icons/svg/close.svg", 16, iconSec))
        binding.btnMore.setImageDrawable(svgDrawable("icons/svg/more_vertical.svg", 20, iconTint))

        loadHistory()

        postponeEnterTransition()
        binding.searchInputContainer.doOnPreDraw { startPostponedEnterTransition() }

        historyAdapter = SearchSuggestAdapter(searchHistory.take(10)) { query ->
            navigate(query)
        }
        binding.suggestionsList.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = historyAdapter
        }


        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                val text = s?.toString() ?: ""
                binding.btnClear.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
                filterSuggestions(text)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClear.setOnClickListener { binding.searchInput.setText("") }

        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val q = binding.searchInput.text.toString().trim()
                if (q.isNotEmpty()) navigate(q)
                true
            } else false
        }

        binding.btnMore.setOnClickListener { showMorePopup() }

        // Mantém a transição estável; o foco/teclado podem ser ativados manualmente ao tocar no input.
    }

    override fun onResume() {
        super.onResume()
        applyStatusBarTheme()
    }

    private fun applyStatusBarTheme() {
        val isLight = !resources.configuration.isNightModeActive
        window.statusBarColor = ContextCompat.getColor(this, R.color.appbar_background)
        insetsController.isAppearanceLightStatusBars = isLight
    }

    private fun navigate(input: String) {
        addToHistory(input)
        val intent = Intent(this, BrowserResponseActivity::class.java).apply {
            putExtra(BrowserResponseActivity.EXTRA_QUERY, input)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun finishWithAnim() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun filterSuggestions(query: String) {
        val filtered = if (query.isEmpty()) searchHistory.take(10)
        else searchHistory.filter { it.contains(query, ignoreCase = true) }.take(10)
        historyAdapter?.updateList(filtered)
        binding.recentLabel.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showMorePopup() {
        val bgColor   = ContextCompat.getColor(this, R.color.popup_background)
        val textColor = ContextCompat.getColor(this, R.color.text_primary)
        val iconTint  = ContextCompat.getColor(this, R.color.icon_tint)

        val items = listOf(
            SearchPopupItem("icons/svg/ai.svg", getString(R.string.ai_search)) {
                navigate("https://chat.openai.com")
            },
            SearchPopupItem("icons/svg/search_engine.svg", getString(R.string.search_engine)) {
                showEngineSelector()
            },
            SearchPopupItem("icons/svg/incognito.svg", getString(R.string.incognito)) {
                startActivity(Intent(this, IncognitoActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            },
            SearchPopupItem("icons/svg/history.svg", getString(R.string.history)) {
                startActivity(Intent(this, HistoryActivity::class.java))
            },
        )

        val menuView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background  = ContextCompat.getDrawable(this@SearchActivity, R.drawable.popup_bg)
            val pad = (8 * resources.displayMetrics.density).toInt()
            setPadding(0, pad, 0, pad)
            elevation = 12f
        }

        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
                val h = (16 * resources.displayMetrics.density).toInt()
                val v = (13 * resources.displayMetrics.density).toInt()
                setPadding(h, v, h, v)
                isClickable = true
                isFocusable = true
                background  = ContextCompat.getDrawable(this@SearchActivity, R.drawable.ripple_item)
            }
            val iv = android.widget.ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (20 * resources.displayMetrics.density).toInt(),
                    (20 * resources.displayMetrics.density).toInt()
                ).also { it.marginEnd = (12 * resources.displayMetrics.density).toInt() }
                setImageDrawable(svgDrawable(item.icon, 20, iconTint))
            }
            val tv = TextView(this).apply {
                text = item.label
                setTextColor(textColor)
                textSize = 14f
            }
            row.addView(iv)
            row.addView(tv)
            menuView.addView(row)
            row.setOnClickListener { item.action() }
        }

        val pop = PopupWindow(
            menuView,
            (200 * resources.displayMetrics.density).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        pop.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        pop.elevation = 12f
        pop.animationStyle = android.R.style.Animation_Dialog
        pop.showAtLocation(binding.root, Gravity.TOP or Gravity.END,
            (12 * resources.displayMetrics.density).toInt(),
            (56 * resources.displayMetrics.density).toInt())
    }

    private fun showEngineSelector() {
        val engines = arrayOf("DuckDuckGo", "Google", "Bing", "Brave")
        val prefs   = getSharedPreferences("wilin_prefs", Context.MODE_PRIVATE)
        val current = prefs.getString("search_engine", "duckduckgo") ?: "duckduckgo"
        val idx = when (current) { "google" -> 1; "bing" -> 2; "brave" -> 3; else -> 0 }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.search_engine))
            .setSingleChoiceItems(engines, idx) { dialog, which ->
                val v = when (which) { 1 -> "google"; 2 -> "bing"; 3 -> "brave"; else -> "duckduckgo" }
                prefs.edit().putString("search_engine", v).apply()
                dialog.dismiss()
            }.show()
    }

    private fun addToHistory(query: String) {
        searchHistory.remove(query)
        searchHistory.add(0, query)
        if (searchHistory.size > 50) searchHistory.removeLast()
        getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
            .edit().putString(KEY_HISTORY, searchHistory.joinToString("|||")).apply()
    }

    private fun loadHistory() {
        val raw = getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "") ?: ""
        searchHistory.clear()
        if (raw.isNotEmpty()) searchHistory.addAll(raw.split("|||").filter { it.isNotEmpty() })
    }

    override fun onBackPressed() {
        finishWithAnim()
    }

    private fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
        val px  = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val svg = SVG.getFromAsset(assets, path)
        svg.documentWidth  = px.toFloat()
        svg.documentHeight = px.toFloat()
        svg.renderToCanvas(Canvas(bmp))
        val drawable = BitmapDrawable(resources, bmp)
        drawable.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
        return drawable
    }
}