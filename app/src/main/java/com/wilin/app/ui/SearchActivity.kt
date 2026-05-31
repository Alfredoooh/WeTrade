package com.wilin.app.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.caverock.androidsvg.SVG
import com.wilin.app.R
import com.wilin.app.databinding.ActivitySearchBinding
import java.util.Locale

class SearchActivity : BaseActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val searchHistory = mutableListOf<String>()
    private var historyAdapter: SearchSuggestAdapter? = null

    private val localSuggestions = listOf(
        "google.com", "youtube.com", "facebook.com", "wikipedia.org",
        "amazon.com", "twitter.com", "instagram.com", "reddit.com",
        "netflix.com", "linkedin.com", "github.com", "stackoverflow.com",
        "news", "weather", "sports", "music", "movies", "recipes",
        "how to", "what is", "best", "top 10"
    )

    companion object {
        private const val PREFS_HISTORY = "wilin_search_history"
        private const val KEY_HISTORY   = "history"
        const val EXTRA_PREFILL         = "prefill_query"
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("wilin_prefs", Context.MODE_PRIVATE)
        when (prefs.getString("theme", "light")) {
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else   -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        val lang = prefs.getString("language", "") ?: ""
        val base = if (lang.isNotEmpty()) {
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        } else newBase
        super.attachBaseContext(base)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)
        val iconSec  = ContextCompat.getColor(this, R.color.icon_tint_secondary)
        val blue     = ContextCompat.getColor(this, R.color.colorPrimary)

        binding.btnBack.setImageDrawable(svgDrawable("icons/svg/back_arrow.svg", 24, iconTint))
        binding.btnBack.setOnClickListener { finishWithAnim() }
        binding.searchIcon.setImageDrawable(svgDrawable("icons/svg/magnifying_glass_outline.svg", 20, iconSec))
        binding.btnClear.setImageDrawable(svgDrawable("icons/svg/close.svg", 16, iconSec))
        binding.askAiIcon.setImageDrawable(svgDrawable("icons/svg/ai.svg", 15, blue))

        binding.btnAskAi.setOnClickListener {
            startActivity(Intent(this, AiSearchActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        loadHistory()

        historyAdapter = SearchSuggestAdapter(searchHistory.take(10)) { query -> navigate(query) }
        binding.suggestionsList.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = historyAdapter
        }

        val prefill = intent.getStringExtra(EXTRA_PREFILL) ?: ""
        if (prefill.isNotEmpty()) {
            binding.searchInput.setText(prefill)
            binding.searchInput.setSelection(prefill.length)
            binding.btnClear.visibility = View.VISIBLE
            showSuggestionsFor(prefill)
        } else {
            binding.recentLabel.text = getString(R.string.recent)
            binding.recentLabel.visibility = if (searchHistory.isNotEmpty()) View.VISIBLE else View.GONE
            historyAdapter?.updateList(searchHistory.take(10))
        }

        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                val text = s?.toString() ?: ""
                binding.btnClear.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
                if (text.isEmpty()) {
                    binding.recentLabel.text = getString(R.string.recent)
                    binding.recentLabel.visibility = if (searchHistory.isNotEmpty()) View.VISIBLE else View.GONE
                    historyAdapter?.updateList(searchHistory.take(10))
                } else {
                    showSuggestionsFor(text)
                }
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

        binding.searchInput.requestFocus()
        binding.searchInput.post {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(binding.searchInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun showSuggestionsFor(query: String) {
        binding.recentLabel.text = getString(R.string.nav_search)
        binding.recentLabel.visibility = View.VISIBLE

        val histMatch = searchHistory.filter { it.contains(query, ignoreCase = true) }.take(4)
        val dynamic = listOf(query, "$query site", "$query tutorial", "$query como fazer", "$query o que é")
        val combined = (histMatch + dynamic).distinct().take(10)
        historyAdapter?.updateList(combined)
    }

    private fun navigate(input: String) {
        addToHistory(input)
        startActivity(Intent(this, BrowserResponseActivity::class.java).apply {
            putExtra(BrowserResponseActivity.EXTRA_QUERY, input)
        })
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun finishWithAnim() {
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun addToHistory(query: String) {
        searchHistory.remove(query); searchHistory.add(0, query)
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

    override fun onBackPressed() { finishWithAnim() }

    private fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
        val px  = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        SVG.getFromAsset(assets, path).apply {
            documentWidth  = px.toFloat()
            documentHeight = px.toFloat()
            renderToCanvas(Canvas(bmp))
        }
        return BitmapDrawable(resources, bmp).also {
            it.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
        }
    }
}