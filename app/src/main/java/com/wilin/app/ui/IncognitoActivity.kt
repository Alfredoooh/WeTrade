package com.wilin.app.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.caverock.androidsvg.SVG
import com.wilin.app.R
import com.wilin.app.databinding.ActivityIncognitoBinding

class IncognitoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncognitoBinding
    private var isLoading = false
    private var bottomBarVisible = true

    companion object {
        private const val DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncognitoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        // Modo incógnito sempre escuro na statusbar
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        val white = 0xFFFFFFFF.toInt()
        val grey  = 0xFFAAAAAA.toInt()

        binding.btnBack.setImageDrawable(svgDrawable("icons/svg/arrow_left.svg", 24, grey))
        binding.btnForward.setImageDrawable(svgDrawable("icons/svg/arrow_right.svg", 24, grey))
        binding.btnReload.setImageDrawable(svgDrawable("icons/svg/refresh.svg", 20, grey))
        binding.btnMore.setImageDrawable(svgDrawable("icons/svg/more_vertical.svg", 24, white))
        binding.incognitoIcon.setImageDrawable(svgDrawable("icons/svg/incognito.svg", 18, grey))
        binding.modalSearchIcon.setImageDrawable(svgDrawable("icons/svg/magnifying_glass_outline.svg", 20, grey))
        binding.modalClearBtn.setImageDrawable(svgDrawable("icons/svg/close.svg", 16, grey))

        setupWebView()
        binding.webView.loadUrl("https://duckduckgo.com/?kae=d&k1=-1")

        binding.btnBack.setOnClickListener { if (binding.webView.canGoBack()) binding.webView.goBack() }
        binding.btnForward.setOnClickListener { if (binding.webView.canGoForward()) binding.webView.goForward() }
        binding.btnReload.setOnClickListener {
            if (isLoading) binding.webView.stopLoading() else binding.webView.reload()
        }
        binding.btnClose.setOnClickListener { finish() }
        binding.urlBar.setOnClickListener { showSearchModal() }
        binding.searchModal.setOnClickListener { hideSearchModal() }
        setupModalInput()

        binding.webView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 8 && bottomBarVisible && scrollY > 100) hideBottomBar()
            else if (dy < -8 && !bottomBarVisible) showBottomBar()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled    = true
            settings.domStorageEnabled    = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls  = true
            settings.displayZoomControls  = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort      = true
            // Sem persistência de dados
            clearCache(true)
            clearHistory()
            clearFormData()

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    isLoading = true
                    binding.progressBar.visibility = View.VISIBLE
                    updateUrlBar(url ?: "")
                }
                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)
                    isLoading = false
                    binding.progressBar.visibility = View.GONE
                    updateUrlBar(url ?: "")
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    val w = (binding.progressBar.parent as View).width
                    binding.progressBar.layoutParams =
                        binding.progressBar.layoutParams.also { it.width = (w * newProgress / 100) }
                    binding.progressBar.requestLayout()
                }
            }
        }
    }

    private fun updateUrlBar(url: String) {
        val display = runCatching {
            Uri.parse(url).host?.removePrefix("www.") ?: url
        }.getOrDefault(url)
        binding.urlText.text = display
    }

    private fun showSearchModal() {
        binding.searchModal.visibility = View.VISIBLE
        binding.searchModal.alpha = 0f
        binding.searchModal.animate().alpha(1f).setDuration(180).start()
        binding.modalContainer.translationY = 80f
        binding.modalContainer.animate().translationY(0f).setDuration(260)
            .setInterpolator(DecelerateInterpolator(2f)).start()
        binding.modalSearchInput.setText(binding.webView.url ?: "")
        binding.modalSearchInput.selectAll()
        binding.modalSearchInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.modalSearchInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideSearchModal() {
        binding.searchModal.animate().alpha(0f).setDuration(150).withEndAction {
            binding.searchModal.visibility = View.GONE
        }.start()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.modalSearchInput.windowToken, 0)
        binding.modalSearchInput.setText("")
    }

    private fun setupModalInput() {
        binding.modalSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                binding.modalClearBtn.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.modalClearBtn.setOnClickListener { binding.modalSearchInput.setText("") }
        binding.modalSearchInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val input = v.text.toString().trim()
                if (input.isNotEmpty()) {
                    hideSearchModal()
                    binding.webView.loadUrl(buildUrl(input))
                }
                true
            } else false
        }
    }

    private fun buildUrl(input: String): String = when {
        input.startsWith("http://") || input.startsWith("https://") -> input
        input.contains(".") && !input.contains(" ") -> "https://$input"
        else -> "https://duckduckgo.com/?q=${Uri.encode(input)}&kae=d&k1=-1"
    }

    private fun hideBottomBar() {
        if (!bottomBarVisible) return
        bottomBarVisible = false
        binding.bottomBar.animate().translationY(binding.bottomBar.height.toFloat()).setDuration(200).start()
    }

    private fun showBottomBar() {
        if (bottomBarVisible) return
        bottomBarVisible = true
        binding.bottomBar.animate().translationY(0f).setDuration(200).start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpar dados privados ao fechar
        binding.webView.clearCache(true)
        binding.webView.clearHistory()
        binding.webView.clearFormData()
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
        android.webkit.CookieManager.getInstance().flush()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            binding.searchModal.visibility == View.VISIBLE -> hideSearchModal()
            binding.webView.canGoBack() -> binding.webView.goBack()
            else -> super.onBackPressed()
        }
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