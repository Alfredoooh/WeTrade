// BrowserResponseActivity.kt
package com.wilin.app.ui

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.wilin.app.R
import com.wilin.app.databinding.ActivityBrowserResponseBinding

// Data class ao nível do ficheiro — partilhada por todas as funções, sem cast problemático
private data class PopupItem(val icon: String, val label: String, val action: () -> Unit)

class BrowserResponseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserResponseBinding
    private var currentTabId: String = ""
    private var isLoading = false
    private var isDesktopMode = false
    private var bottomBarVisible = true
    private var lastScrollY = 0

    private val searchHistory = mutableListOf<String>()
    private var historyAdapter: HistoryModalAdapter? = null

    companion object {
        const val EXTRA_QUERY  = "query"
        const val EXTRA_TAB_ID = "tab_id"
        private const val PREFS_HISTORY = "wilin_search_history"
        private const val KEY_HISTORY   = "history"
        private const val DESKTOP_UA    = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserResponseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        val isLight = !resources.configuration.isNightModeActive
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight

        TabManager.init(this)
        loadHistory()

        val iconTint      = ContextCompat.getColor(this, R.color.icon_tint)
        val iconSecondary = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        binding.btnBack.setImageDrawable(svgDrawable("icons/svg/arrow_left.svg", 24, iconSecondary))
        binding.btnForward.setImageDrawable(svgDrawable("icons/svg/arrow_right.svg", 24, iconSecondary))
        binding.btnReload.setImageDrawable(svgDrawable("icons/svg/refresh.svg", 20, iconSecondary))
        binding.tabsIcon.setImageDrawable(svgDrawable("icons/svg/tabs.svg", 24, iconTint))
        binding.btnMore.setImageDrawable(svgDrawable("icons/svg/more_vertical.svg", 24, iconTint))
        binding.modalSearchIcon.setImageDrawable(svgDrawable("icons/svg/magnifying_glass_outline.svg", 20, iconSecondary))
        binding.modalClearBtn.setImageDrawable(svgDrawable("icons/svg/close.svg", 16, iconSecondary))

        val tabId = intent.getStringExtra(EXTRA_TAB_ID)
        val query = intent.getStringExtra(EXTRA_QUERY) ?: ""

        if (tabId != null && TabManager.getTabs().any { it.id == tabId }) {
            currentTabId = tabId
            TabManager.setCurrentId(tabId)
        } else {
            val tab = TabManager.newTab()
            currentTabId = tab.id
        }

        updateTabsCount()
        setupWebView()

        val tab = TabManager.getCurrent()
        val loadUrl = when {
            query.isNotEmpty() -> { addToHistory(query); buildUrl(query) }
            tab != null && tab.url.isNotEmpty() -> tab.url
            else -> buildDuckDuckGoHome()
        }
        binding.webView.loadUrl(loadUrl)

        binding.btnBack.setOnClickListener { if (binding.webView.canGoBack()) binding.webView.goBack() }
        binding.btnForward.setOnClickListener { if (binding.webView.canGoForward()) binding.webView.goForward() }
        binding.btnReload.setOnClickListener {
            if (isLoading) binding.webView.stopLoading() else binding.webView.reload()
        }
        binding.btnTabs.setOnClickListener {
            TabManager.save(this)
            startActivity(Intent(this, TabsActivity::class.java))
        }
        binding.btnMore.setOnClickListener { showMoreMenu() }
        binding.urlBar.setOnClickListener { showSearchModal() }
        binding.searchModal.setOnClickListener { hideSearchModal() }
        setupModalInput()

        historyAdapter = HistoryModalAdapter(searchHistory) { q ->
            hideSearchModal()
            navigateTo(q)
        }
        binding.modalHistoryList.apply {
            layoutManager = LinearLayoutManager(this@BrowserResponseActivity)
            adapter = historyAdapter
        }

        binding.webView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 8 && bottomBarVisible && scrollY > 100) hideBottomBar()
            else if (dy < -8 && !bottomBarVisible) showBottomBar()
            lastScrollY = scrollY
        }
    }

    override fun onResume() {
        super.onResume()
        val isLight = !resources.configuration.isNightModeActive
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
        updateTabsCount()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled     = true
            settings.domStorageEnabled     = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls   = true
            settings.displayZoomControls   = false
            settings.loadWithOverviewMode  = true
            settings.useWideViewPort       = true
            settings.allowFileAccess       = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.mixedContentMode      = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimeType)
                    addRequestHeader("User-Agent", userAgent)
                    setDescription("A descarregar ficheiro…")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.substringAfterLast("/"))
                }
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(this@BrowserResponseActivity, "A descarregar…", Toast.LENGTH_SHORT).show()
            })

            setOnLongClickListener {
                val result = hitTestResult
                if (result.type == WebView.HitTestResult.IMAGE_TYPE ||
                    result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    val imgUrl = result.extra ?: return@setOnLongClickListener false
                    showImageContextMenu(imgUrl)
                    true
                } else false
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    TabManager.updateTab(currentTabId, url = request.url.toString())
                    return false
                }
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    isLoading = true
                    binding.progressBar.visibility = View.VISIBLE
                    val sec = ContextCompat.getColor(this@BrowserResponseActivity, R.color.icon_tint_secondary)
                    binding.btnReload.setImageDrawable(svgDrawable("icons/svg/close.svg", 20, sec))
                    updateUrlBar(url ?: "")
                    updateNavButtons()
                }
                override fun onPageFinished(view: WebView, url: String?) {
                    super.onPageFinished(view, url)
                    isLoading = false
                    binding.progressBar.visibility = View.GONE
                    val sec = ContextCompat.getColor(this@BrowserResponseActivity, R.color.icon_tint_secondary)
                    binding.btnReload.setImageDrawable(svgDrawable("icons/svg/refresh.svg", 20, sec))
                    updateUrlBar(url ?: "")
                    updateNavButtons()
                    TabManager.updateTab(currentTabId, url = url ?: "")
                    TabManager.save(this@BrowserResponseActivity)
                    loadFaviconViaJs(url ?: "")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    TabManager.updateTab(currentTabId, title = title ?: "")
                    updateTabsCount()
                }
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    val w = (binding.progressBar.parent as View).width
                    binding.progressBar.layoutParams =
                        binding.progressBar.layoutParams.also { it.width = (w * newProgress / 100) }
                    binding.progressBar.requestLayout()
                }
                override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                    super.onReceivedIcon(view, icon)
                    if (icon != null) {
                        binding.faviconImg.setImageBitmap(icon)
                        TabManager.updateTab(currentTabId, favicon = view?.url ?: "")
                    }
                }
            }
        }
    }

    private fun showImageContextMenu(imgUrl: String) {
        val iconTint  = ContextCompat.getColor(this, R.color.icon_tint)
        val bgColor   = ContextCompat.getColor(this, R.color.popup_background)
        val textColor = ContextCompat.getColor(this, R.color.text_primary)

        val items = listOf(
            PopupItem("icons/svg/download.svg", getString(R.string.download_image)) {
                downloadFile(imgUrl)
            },
            PopupItem("icons/svg/copy.svg", getString(R.string.copy_image_url)) {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("img_url", imgUrl))
                Toast.makeText(this, "URL copiado", Toast.LENGTH_SHORT).show()
            },
            PopupItem("icons/svg/external.svg", getString(R.string.open_image_new_tab)) {
                val t = TabManager.newTab(imgUrl)
                TabManager.setCurrentId(t.id)
                binding.webView.loadUrl(imgUrl)
            },
            PopupItem("icons/svg/share.svg", getString(R.string.share)) {
                startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, imgUrl) },
                    getString(R.string.share)
                ))
            },
        )

        showAnimatedPopup(items, iconTint, bgColor, textColor, Gravity.CENTER)
    }

    private fun downloadFile(url: String) {
        runCatching {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setDescription("A descarregar…")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.substringAfterLast("/"))
            }
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(this, "A descarregar…", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "Erro ao descarregar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadFaviconViaJs(pageUrl: String) {
        runCatching {
            val host = Uri.parse(pageUrl).host ?: return
            val faviconUrl = "https://www.google.com/s2/favicons?domain=$host&sz=32"
            Thread {
                runCatching {
                    val conn = java.net.URL(faviconUrl).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 3000
                    conn.readTimeout    = 3000
                    val bmp = android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                    if (bmp != null) runOnUiThread { binding.faviconImg.setImageBitmap(bmp) }
                    conn.disconnect()
                }
            }.start()
        }
    }

    private fun updateUrlBar(url: String) {
        val isHttps = url.startsWith("https://")
        val display = runCatching {
            Uri.parse(url).host?.removePrefix("www.") ?: url
        }.getOrDefault(url)

        binding.urlText.text = display

        val lockTint = if (isHttps)
            ContextCompat.getColor(this, R.color.colorPrimary)
        else
            ContextCompat.getColor(this, R.color.text_hint)

        val lockIcon = if (isHttps) "icons/svg/lock.svg" else "icons/svg/lock_open.svg"
        binding.lockIcon.setImageDrawable(svgDrawable(lockIcon, 12, lockTint))
    }

    private fun updateNavButtons() {
        val iconTint      = ContextCompat.getColor(this, R.color.icon_tint)
        val iconSecondary = ContextCompat.getColor(this, R.color.icon_tint_secondary)
        binding.btnBack.setImageDrawable(
            svgDrawable("icons/svg/arrow_left.svg", 24,
                if (binding.webView.canGoBack()) iconTint else iconSecondary))
        binding.btnForward.setImageDrawable(
            svgDrawable("icons/svg/arrow_right.svg", 24,
                if (binding.webView.canGoForward()) iconTint else iconSecondary))
    }

    private fun updateTabsCount() {
        val count = TabManager.count()
        if (count > 0) {
            binding.tabsCount.visibility = View.VISIBLE
            binding.tabsCount.text = if (count > 99) "99" else count.toString()
        } else {
            binding.tabsCount.visibility = View.GONE
        }
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

    private fun showSearchModal() {
        binding.searchModal.visibility = View.VISIBLE
        binding.searchModal.alpha = 0f
        binding.searchModal.animate().alpha(1f).setDuration(180).start()
        binding.modalContainer.translationY = 80f
        binding.modalContainer.animate()
            .translationY(0f).setDuration(280)
            .setInterpolator(DecelerateInterpolator(2f)).start()
        binding.modalSearchInput.setText(binding.webView.url ?: "")
        binding.modalSearchInput.selectAll()
        binding.modalSearchInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.modalSearchInput, InputMethodManager.SHOW_IMPLICIT)
        refreshHistoryModal()
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
                filterHistory(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.modalClearBtn.setOnClickListener { binding.modalSearchInput.setText("") }
        binding.modalSearchInput.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val input = v.text.toString().trim()
                if (input.isNotEmpty()) { hideSearchModal(); navigateTo(input) }
                true
            } else false
        }
    }

    private fun navigateTo(input: String) {
        addToHistory(input)
        binding.webView.loadUrl(buildUrl(input))
    }

    private fun ddgThemeParam(): String =
        if (resources.configuration.isNightModeActive) "d" else "l"

    private fun buildDuckDuckGoHome(): String {
        val prefs = getSharedPreferences("wilin_prefs", Context.MODE_PRIVATE)
        return when (prefs.getString("search_engine", "duckduckgo")) {
            "google" -> "https://www.google.com"
            "bing"   -> "https://www.bing.com"
            "brave"  -> "https://search.brave.com"
            else     -> "https://duckduckgo.com/?kae=${ddgThemeParam()}&k1=-1"
        }
    }

    private fun buildUrl(input: String): String {
        if (input.startsWith("http://") || input.startsWith("https://")) return input
        if (input.contains(".") && !input.contains(" ")) return "https://$input"
        val prefs = getSharedPreferences("wilin_prefs", Context.MODE_PRIVATE)
        return when (prefs.getString("search_engine", "duckduckgo")) {
            "google" -> "https://www.google.com/search?q=${Uri.encode(input)}"
            "bing"   -> "https://www.bing.com/search?q=${Uri.encode(input)}"
            "brave"  -> "https://search.brave.com/search?q=${Uri.encode(input)}"
            else     -> "https://duckduckgo.com/?q=${Uri.encode(input)}&kae=${ddgThemeParam()}&k1=-1"
        }
    }

    private fun refreshHistoryModal() { historyAdapter?.updateList(searchHistory.take(8)) }

    private fun filterHistory(query: String) {
        val filtered = if (query.isEmpty()) searchHistory.take(8)
        else searchHistory.filter { it.contains(query, ignoreCase = true) }.take(8)
        historyAdapter?.updateList(filtered)
    }

    private fun addToHistory(query: String) {
        searchHistory.remove(query)
        searchHistory.add(0, query)
        if (searchHistory.size > 50) searchHistory.removeLast()
        saveHistory()
    }

    private fun loadHistory() {
        val raw = getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "") ?: ""
        if (raw.isNotEmpty()) searchHistory.addAll(raw.split("|||").filter { it.isNotEmpty() })
    }

    private fun saveHistory() {
        getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
            .edit().putString(KEY_HISTORY, searchHistory.joinToString("|||")).apply()
    }

    private fun showMoreMenu() {
        val iconTint  = ContextCompat.getColor(this, R.color.icon_tint)
        val bgColor   = ContextCompat.getColor(this, R.color.popup_background)
        val textColor = ContextCompat.getColor(this, R.color.text_primary)

        val isBookmarked = isCurrentBookmarked()
        val items = listOf(
            PopupItem("icons/svg/bookmark${if (isBookmarked) "_filled" else ""}.svg",
                getString(if (isBookmarked) R.string.remove_bookmark else R.string.add_bookmark)) { toggleBookmark() },
            PopupItem("icons/svg/share.svg", getString(R.string.share)) { shareUrl() },
            PopupItem("icons/svg/copy.svg", getString(R.string.copy_url)) { copyUrl() },
            PopupItem("icons/svg/find.svg", getString(R.string.find_in_page)) { findInPage() },
            PopupItem("icons/svg/desktop.svg", getString(R.string.desktop_mode)) { toggleDesktopMode() },
            PopupItem("icons/svg/download.svg", "Descarregar página") { downloadFile(binding.webView.url ?: "") },
            PopupItem("icons/svg/history.svg", getString(R.string.history)) {
                startActivity(Intent(this, HistoryActivity::class.java))
            },
            PopupItem("icons/svg/external.svg", getString(R.string.open_in_browser)) { openExternal() },
        )

        showAnimatedPopup(items, iconTint, bgColor, textColor, Gravity.BOTTOM or Gravity.END)
    }

    private fun showAnimatedPopup(
        items: List<PopupItem>,
        iconTint: Int, bgColor: Int, textColor: Int, gravity: Int
    ) {
        val menuView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background  = ContextCompat.getDrawable(this@BrowserResponseActivity, R.drawable.popup_bg)
            val pad = (8 * resources.displayMetrics.density).toInt()
            setPadding(0, pad, 0, pad)
        }

        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                this.gravity = Gravity.CENTER_VERTICAL
                val h = (16 * resources.displayMetrics.density).toInt()
                val v = (12 * resources.displayMetrics.density).toInt()
                setPadding(h, v, h, v)
                isClickable = true; isFocusable = true
                background = ContextCompat.getDrawable(this@BrowserResponseActivity, R.drawable.ripple_item)
            }
            val iv = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (20 * resources.displayMetrics.density).toInt(),
                    (20 * resources.displayMetrics.density).toInt()
                ).also { it.marginEnd = (12 * resources.displayMetrics.density).toInt() }
                setImageDrawable(svgDrawable(item.icon, 20, iconTint))
            }
            val tv = TextView(this).apply { text = item.label; setTextColor(textColor); textSize = 14f }
            row.addView(iv); row.addView(tv)
            menuView.addView(row)
            row.setOnClickListener { item.action() }
        }

        menuView.scaleX = 0.85f; menuView.scaleY = 0.85f; menuView.alpha = 0f
        menuView.animate().scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(220).setInterpolator(OvershootInterpolator(1.2f)).start()

        val pop = PopupWindow(
            menuView,
            (220 * resources.displayMetrics.density).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        pop.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        pop.elevation = 12f
        pop.showAtLocation(binding.root, gravity,
            (12 * resources.displayMetrics.density).toInt(),
            (60 * resources.displayMetrics.density).toInt())
    }

    private fun shareUrl() {
        val url = binding.webView.url ?: return
        startActivity(Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url) },
            getString(R.string.share)
        ))
    }

    private fun copyUrl() {
        val url = binding.webView.url ?: return
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("url", url))
        Toast.makeText(this, getString(R.string.url_copied), Toast.LENGTH_SHORT).show()
    }

    private fun openExternal() {
        val url = binding.webView.url ?: return
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    private fun toggleDesktopMode() {
        isDesktopMode = !isDesktopMode
        binding.webView.settings.userAgentString = if (isDesktopMode) DESKTOP_UA else null
        binding.webView.reload()
    }

    private fun findInPage() {
        binding.webView.findAllAsync("")
        binding.webView.showFindDialog(null, true)
    }

    private fun isCurrentBookmarked(): Boolean {
        val url = binding.webView.url ?: return false
        val raw = getSharedPreferences("wilin_bookmarks", Context.MODE_PRIVATE)
            .getString("bookmarks", "") ?: ""
        return raw.split("|||").any { it.startsWith("$url::") }
    }

    private fun toggleBookmark() {
        val url   = binding.webView.url ?: return
        val title = binding.webView.title ?: url
        val prefs = getSharedPreferences("wilin_bookmarks", Context.MODE_PRIVATE)
        val list  = (prefs.getString("bookmarks", "") ?: "").split("|||")
            .filter { it.isNotEmpty() }.toMutableList()
        val existing = list.indexOfFirst { it.startsWith("$url::") }
        val msg: String
        if (existing >= 0) { list.removeAt(existing); msg = getString(R.string.bookmark_removed) }
        else { list.add(0, "$url::$title"); msg = getString(R.string.bookmark_added) }
        prefs.edit().putString("bookmarks", list.joinToString("|||")).apply()
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            binding.searchModal.visibility == View.VISIBLE -> hideSearchModal()
            binding.webView.canGoBack() -> binding.webView.goBack()
            else -> super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        TabManager.save(this)
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

class HistoryModalAdapter(
    private var items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<HistoryModalAdapter.VH>() {

    inner class VH(val tv: TextView) : RecyclerView.ViewHolder(tv)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val tv = TextView(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            val h = (16 * context.resources.displayMetrics.density).toInt()
            val v = (10 * context.resources.displayMetrics.density).toInt()
            setPadding(h, v, h, v)
            textSize = 14f; maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            isClickable = true; isFocusable = true
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