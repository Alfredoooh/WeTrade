package com.wilin.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
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
    private lateinit var adapter: TabsAdapter
    private var showIncognito = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTabsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        val isLight = !resources.configuration.isNightModeActive
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight

        TabManager.init(this)

        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)
        val iconSec  = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        binding.btnCloseTabsActivity.setImageDrawable(svgDrawable("icons/svg/back_arrow.svg", 24, iconTint))
        binding.btnCloseTabsActivity.setOnClickListener { finish() }

        binding.btnNewTab.setImageDrawable(svgDrawable("icons/svg/add.svg", 24, iconTint))
        binding.btnNewTab.setOnClickListener {
            if (showIncognito) {
                startActivity(Intent(this, IncognitoActivity::class.java))
                finish()
            } else {
                TabManager.newTab()
                TabManager.save(this)
                openCurrentTab()
            }
        }

        binding.btnIncognito.setImageDrawable(svgDrawable("icons/svg/incognito.svg", 22, iconTint))
        binding.btnIncognito.setOnClickListener {
            startActivity(Intent(this, IncognitoActivity::class.java))
            finish()
        }

        // Selector Normal / Privado
        binding.tabModeNormal.setOnClickListener {
            showIncognito = false
            binding.tabModeNormal.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            binding.tabModeIncognito.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tabIndicatorNormal.visibility = View.VISIBLE
            binding.tabIndicatorIncognito.visibility = View.INVISIBLE
            refreshAdapter()
        }
        binding.tabModeIncognito.setOnClickListener {
            showIncognito = true
            binding.tabModeIncognito.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            binding.tabModeNormal.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tabIndicatorIncognito.visibility = View.VISIBLE
            binding.tabIndicatorNormal.visibility = View.INVISIBLE
            refreshAdapter()
        }

        adapter = TabsAdapter(
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
                refreshAdapter()
                if (TabManager.count() == 0) finish()
            },
            svgFn = { path, size, tint -> svgDrawable(path, size, tint) }
        )

        binding.tabsRecycler.layoutManager = GridLayoutManager(this, 2)
        binding.tabsRecycler.adapter = adapter

        binding.btnDone.setOnClickListener { finish() }

        updateTabCount()
    }

    private fun refreshAdapter() {
        adapter.updateTabs(TabManager.getTabs().toMutableList(), TabManager.getCurrentId())
        updateTabCount()
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

    private fun updateTabCount() {
        binding.tabsTitle.text = "${TabManager.count()} ${getString(R.string.tabs)}"
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

class TabsAdapter(
    private var tabs: MutableList<BrowserTab>,
    private var currentId: String,
    private val onSelect: (BrowserTab) -> Unit,
    private val onClose: (BrowserTab) -> Unit,
    private val svgFn: (String, Int, Int) -> BitmapDrawable
) : RecyclerView.Adapter<TabsAdapter.VH>() {

    inner class VH(
        val root: FrameLayout,
        val webView: WebView,
        val title: TextView,
        val close: ImageView,
        val activeBorder: View
    ) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx   = parent.context
        val dp    = ctx.resources.displayMetrics.density
        val bg    = ContextCompat.getColor(ctx, R.color.card_background)
        val blue  = ContextCompat.getColor(ctx, R.color.colorPrimary)
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

        // Mini WebView preview
        val wv = WebView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            isEnabled = false
            isFocusable = false
            isClickable = false
        }

        // Overlay escuro em cima do webview para legibilidade
        val overlay = View(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0x40000000)
        }

        // Título no rodapé
        val titleContainer = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (36 * dp).toInt()
            ).also { it.gravity = android.view.Gravity.BOTTOM }
            setBackgroundColor(bg)
            gravity = android.view.Gravity.CENTER_VERTICAL
            val p = (8 * dp).toInt()
            setPadding(p, 0, p, 0)
        }
        val titleTv = TextView(ctx).apply {
            textSize = 12f
            setTextColor(textC)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleContainer.addView(titleTv)

        // Borda activa no topo
        val activeBorder = View(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, (3 * dp).toInt()
            ).also { it.gravity = android.view.Gravity.TOP }
            setBackgroundColor(blue)
            visibility = View.GONE
        }

        // Close button
        val closeBtn = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                (32 * dp).toInt(), (32 * dp).toInt()
            ).also { it.gravity = android.view.Gravity.TOP or android.view.Gravity.END }
            val p = (6 * dp).toInt(); setPadding(p, p, p, p)
            setImageDrawable(svgFn("icons/svg/close.svg", 16, iconT))
            background = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)
            isClickable = true; isFocusable = true
        }

        card.addView(wv)
        card.addView(overlay)
        card.addView(titleContainer)
        card.addView(activeBorder)
        card.addView(closeBtn)

        return VH(card, wv, titleTv, closeBtn, activeBorder)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tab = tabs[position]
        holder.title.text = tab.title.ifEmpty { tab.url }
        holder.activeBorder.visibility = if (tab.id == currentId) View.VISIBLE else View.GONE
        if (tab.url.isNotEmpty()) {
            holder.webView.loadUrl(tab.url)
        }
        holder.root.setOnClickListener { onSelect(tab) }
        holder.close.setOnClickListener { onClose(tab) }
    }

    override fun getItemCount() = tabs.size

    fun updateTabs(newTabs: MutableList<BrowserTab>, newCurrentId: String) {
        tabs = newTabs; currentId = newCurrentId
        notifyDataSetChanged()
    }
}