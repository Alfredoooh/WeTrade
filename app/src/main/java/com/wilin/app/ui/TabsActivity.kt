package com.wilin.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.wilin.app.R
import com.wilin.app.databinding.ActivityTabsBinding

class TabsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTabsBinding
    private lateinit var tabsAdapter: TabsAdapter
    private lateinit var bookmarksAdapter: BookmarksTabAdapter
    private var showBookmarks = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTabsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        val isLight = !resources.configuration.isNightModeActive
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight

        TabManager.init(this)

        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)

        binding.btnCloseTabsActivity.setImageDrawable(svgDrawable("icons/svg/back_arrow.svg", 24, iconTint))
        binding.btnCloseTabsActivity.setOnClickListener { finish() }

        binding.btnNewTab.setImageDrawable(svgDrawable("icons/svg/add.svg", 24, iconTint))
        binding.btnNewTab.setOnClickListener {
            TabManager.newTab()
            TabManager.save(this)
            openCurrentTab()
        }

        // Selector Tabs | Favoritos
        binding.tabModeNormal.setOnClickListener {
            if (showBookmarks) {
                showBookmarks = false
                updateSelectorUI()
                binding.tabsRecycler.adapter = tabsAdapter
                tabsAdapter.updateTabs(TabManager.getTabs().toMutableList(), TabManager.getCurrentId())
            }
        }
        binding.tabModeBookmarks.setOnClickListener {
            if (!showBookmarks) {
                showBookmarks = true
                updateSelectorUI()
                binding.tabsRecycler.adapter = bookmarksAdapter
                bookmarksAdapter.reload(loadBookmarks())
            }
        }

        tabsAdapter = TabsAdapter(
            tabs      = TabManager.getTabs().toMutableList(),
            currentId = TabManager.getCurrentId(),
            onSelect  = { tab ->
                TabManager.setCurrentId(tab.id)
                TabManager.save(this)
                openCurrentTab()
            },
            onClose = { tab ->
                TabManager.closeTab(tab.id)
                TabManager.save(this)
                tabsAdapter.updateTabs(TabManager.getTabs().toMutableList(), TabManager.getCurrentId())
                updateTabCountLabel()
                if (TabManager.count() == 0) finish()
            },
            svgFn = { path, size, tint -> svgDrawable(path, size, tint) }
        )

        bookmarksAdapter = BookmarksTabAdapter(
            items = loadBookmarks(),
            onSelect = { url ->
                TabManager.init(this)
                val tab = TabManager.newTab(url)
                TabManager.save(this)
                startActivity(
                    Intent(this, BrowserResponseActivity::class.java).apply {
                        putExtra(BrowserResponseActivity.EXTRA_TAB_ID, tab.id)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                )
                finish()
            },
            svgFn = { path, size, tint -> svgDrawable(path, size, tint) }
        )

        binding.tabsRecycler.layoutManager = GridLayoutManager(this, 2)
        binding.tabsRecycler.adapter = tabsAdapter

        binding.btnDone.setOnClickListener { finish() }

        updateTabCountLabel()
        updateSelectorUI()
    }

    private fun updateSelectorUI() {
        val primary = ContextCompat.getColor(this, R.color.text_primary)
        val secondary = ContextCompat.getColor(this, R.color.text_secondary)
        if (!showBookmarks) {
            binding.tabModeNormalText.setTextColor(primary)
            binding.tabModeBookmarksText.setTextColor(secondary)
            binding.tabIndicatorNormal.visibility = View.VISIBLE
            binding.tabIndicatorBookmarks.visibility = View.INVISIBLE
        } else {
            binding.tabModeBookmarksText.setTextColor(primary)
            binding.tabModeNormalText.setTextColor(secondary)
            binding.tabIndicatorBookmarks.visibility = View.VISIBLE
            binding.tabIndicatorNormal.visibility = View.INVISIBLE
        }
    }

    private fun updateTabCountLabel() {
        binding.tabModeNormalText.text = TabManager.count().toString()
    }

    private fun openCurrentTab() {
        val tab = TabManager.getCurrent() ?: return
        startActivity(
            Intent(this, BrowserResponseActivity::class.java).apply {
                putExtra(BrowserResponseActivity.EXTRA_TAB_ID, tab.id)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        finish()
    }

    private fun loadBookmarks(): List<Pair<String, String>> {
        val raw = getSharedPreferences("wilin_bookmarks", Context.MODE_PRIVATE)
            .getString("bookmarks", "") ?: ""
        return if (raw.isEmpty()) emptyList()
        else raw.split("|||").filter { it.contains("::") }.map {
            val parts = it.split("::", limit = 2)
            Pair(parts[0], if (parts.size > 1) parts[1] else parts[0])
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

// Tabs mostrados como screenshot estático (Bitmap capturado do WebView em BrowserResponseActivity)
// Para gerar screenshots guardamos no TabManager via updateTab — aqui usamos cor de fundo + favicon
class TabsAdapter(
    private var tabs: MutableList<BrowserTab>,
    private var currentId: String,
    private val onSelect: (BrowserTab) -> Unit,
    private val onClose: (BrowserTab) -> Unit,
    private val svgFn: (String, Int, Int) -> BitmapDrawable
) : RecyclerView.Adapter<TabsAdapter.VH>() {

    inner class VH(
        val root: FrameLayout,
        val preview: ImageView,
        val favicon: ImageView,
        val title: TextView,
        val close: ImageView,
        val activeBorder: View
    ) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx  = parent.context
        val dp   = ctx.resources.displayMetrics.density
        val blue = ContextCompat.getColor(ctx, R.color.colorPrimary)
        val bg   = ContextCompat.getColor(ctx, R.color.card_background)
        val textC = ContextCompat.getColor(ctx, R.color.text_primary)
        val iconT = ContextCompat.getColor(ctx, R.color.icon_tint)

        val card = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (180 * dp).toInt()
            ).also { val m = (6 * dp).toInt(); it.setMargins(m, m, m, m) }
            background = ContextCompat.getDrawable(ctx, R.drawable.tab_card_bg)
            clipToOutline = true
        }

        // Preview: ImageView que mostrará screenshot guardado — fallback cor sólida
        val preview = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(bg)
        }

        // Barra inferior com favicon + título
        val titleBar = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (40 * dp).toInt()
            ).also { it.gravity = android.view.Gravity.BOTTOM }
            setBackgroundColor(bg)
            gravity = android.view.Gravity.CENTER_VERTICAL
            val p = (8 * dp).toInt()
            setPadding(p, 0, p, 0)
        }

        val faviconIv = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                (16 * dp).toInt(), (16 * dp).toInt()
            ).also { it.marginEnd = (6 * dp).toInt() }
        }

        val titleTv = TextView(ctx).apply {
            textSize = 11f
            setTextColor(textC)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleBar.addView(faviconIv)
        titleBar.addView(titleTv)

        // Borda azul no topo quando activo
        val activeBorder = View(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, (3 * dp).toInt()
            ).also { it.gravity = android.view.Gravity.TOP }
            setBackgroundColor(blue)
            visibility = View.GONE
        }

        // Botão fechar
        val closeBtn = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                (32 * dp).toInt(), (32 * dp).toInt()
            ).also { it.gravity = android.view.Gravity.TOP or android.view.Gravity.END }
            val p = (6 * dp).toInt(); setPadding(p, p, p, p)
            setImageDrawable(svgFn("icons/svg/close.svg", 16, iconT))
            background = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)
            isClickable = true; isFocusable = true
        }

        card.addView(preview)
        card.addView(titleBar)
        card.addView(activeBorder)
        card.addView(closeBtn)

        return VH(card, preview, faviconIv, titleTv, closeBtn, activeBorder)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tab = tabs[position]
        holder.title.text = tab.title.ifEmpty { tab.url }
        holder.activeBorder.visibility = if (tab.id == currentId) View.VISIBLE else View.GONE

        // Screenshot guardado em TabScreenshots
        val bmp = TabScreenshots.get(tab.id)
        if (bmp != null) {
            holder.preview.setImageBitmap(bmp)
        } else {
            holder.preview.setImageDrawable(null)
        }

        // Favicon via Google S2
        if (tab.url.isNotEmpty()) {
            val host = runCatching { android.net.Uri.parse(tab.url).host ?: "" }.getOrDefault("")
            if (host.isNotEmpty()) {
                Thread {
                    runCatching {
                        val url = "https://www.google.com/s2/favicons?domain=$host&sz=32"
                        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = 2000; conn.readTimeout = 2000
                        val bmpFav = android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                        conn.disconnect()
                        if (bmpFav != null) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                holder.favicon.setImageBitmap(bmpFav)
                            }
                        }
                    }
                }.start()
            }
        }

        holder.root.setOnClickListener {
            holder.root.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).withEndAction {
                holder.root.animate().scaleX(1f).scaleY(1f).setDuration(80).withEndAction {
                    onSelect(tab)
                }.start()
            }.start()
        }
        holder.close.setOnClickListener { onClose(tab) }
    }

    override fun getItemCount() = tabs.size

    fun updateTabs(newTabs: MutableList<BrowserTab>, newCurrentId: String) {
        tabs = newTabs; currentId = newCurrentId
        notifyDataSetChanged()
    }
}

// Adapter para favoritos no mesmo estilo de grid
class BookmarksTabAdapter(
    private var items: List<Pair<String, String>>,
    private val onSelect: (String) -> Unit,
    private val svgFn: (String, Int, Int) -> BitmapDrawable
) : RecyclerView.Adapter<BookmarksTabAdapter.VH>() {

    inner class VH(
        val root: FrameLayout,
        val favicon: ImageView,
        val title: TextView,
        val url: TextView
    ) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx  = parent.context
        val dp   = ctx.resources.displayMetrics.density
        val bg   = ContextCompat.getColor(ctx, R.color.card_background)
        val textC = ContextCompat.getColor(ctx, R.color.text_primary)
        val textS = ContextCompat.getColor(ctx, R.color.text_secondary)

        val card = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (180 * dp).toInt()
            ).also { val m = (6 * dp).toInt(); it.setMargins(m, m, m, m) }
            background = ContextCompat.getDrawable(ctx, R.drawable.tab_card_bg)
            clipToOutline = true
            isClickable = true; isFocusable = true
            foreground = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)
        }

        val inner = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            val p = (16 * dp).toInt()
            setPadding(p, p, p, p)
        }

        val faviconIv = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                (32 * dp).toInt(), (32 * dp).toInt()
            ).also { it.bottomMargin = (10 * dp).toInt() }
        }

        val titleTv = TextView(ctx).apply {
            textSize = 13f; setTextColor(textC)
            maxLines = 2
            gravity = android.view.Gravity.CENTER
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val urlTv = TextView(ctx).apply {
            textSize = 10f; setTextColor(textS)
            maxLines = 1
            gravity = android.view.Gravity.CENTER
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).also { it.topMargin = (4 * dp).toInt() }
        }

        inner.addView(faviconIv)
        inner.addView(titleTv)
        inner.addView(urlTv)
        card.addView(inner)

        return VH(card, faviconIv, titleTv, urlTv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (url, title) = items[position]
        holder.title.text = title
        holder.url.text = runCatching { android.net.Uri.parse(url).host?.removePrefix("www.") ?: url }.getOrDefault(url)

        val host = runCatching { android.net.Uri.parse(url).host ?: "" }.getOrDefault("")
        if (host.isNotEmpty()) {
            Thread {
                runCatching {
                    val faviconUrl = "https://www.google.com/s2/favicons?domain=$host&sz=64"
                    val conn = java.net.URL(faviconUrl).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 2000; conn.readTimeout = 2000
                    val bmp = android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                    conn.disconnect()
                    if (bmp != null) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            holder.favicon.setImageBitmap(bmp)
                        }
                    }
                }
            }.start()
        }

        holder.root.setOnClickListener {
            holder.root.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).withEndAction {
                holder.root.animate().scaleX(1f).scaleY(1f).setDuration(80).withEndAction {
                    onSelect(url)
                }.start()
            }.start()
        }
    }

    override fun getItemCount() = items.size

    fun reload(newItems: List<Pair<String, String>>) { items = newItems; notifyDataSetChanged() }
}