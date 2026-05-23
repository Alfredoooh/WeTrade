package com.wilin.app.ui

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.wilin.app.R
import com.wilin.app.databinding.ActivityTabsBinding

class TabsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTabsBinding
    private lateinit var adapter: TabsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTabsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        TabManager.init(this)

        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)

        binding.btnCloseTabsActivity.setImageDrawable(
            svgDrawable("icons/svg/back_arrow.svg", 24, iconTint))
        binding.btnCloseTabsActivity.setOnClickListener { finish() }

        binding.btnNewTab.setImageDrawable(
            svgDrawable("icons/svg/add.svg", 24, iconTint))
        binding.btnNewTab.setOnClickListener {
            TabManager.newTab()
            TabManager.save(this)
            openCurrentTab()
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
                adapter.updateTabs(TabManager.getTabs().toMutableList(), TabManager.getCurrentId())
                if (TabManager.count() == 0) finish()
            },
            svgFn = { path, size, tint -> svgDrawable(path, size, tint) }
        )

        binding.tabsRecycler.layoutManager = GridLayoutManager(this, 2)
        binding.tabsRecycler.adapter = adapter

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
        binding.tabsTitle.text = "${getString(R.string.tabs)} (${TabManager.count()})"
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

    inner class VH(val root: FrameLayout) : RecyclerView.ViewHolder(root) {
        val title: TextView   = root.findViewWithTag("title")
        val url: TextView     = root.findViewWithTag("url")
        val close: ImageView  = root.findViewWithTag("close")
        val active: View      = root.findViewWithTag("active")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx    = parent.context
        val dp     = ctx.resources.displayMetrics.density
        val bgColor   = ContextCompat.getColor(ctx, R.color.card_background)
        val textColor = ContextCompat.getColor(ctx, R.color.text_primary)
        val subColor  = ContextCompat.getColor(ctx, R.color.text_secondary)
        val blue      = ContextCompat.getColor(ctx, R.color.colorPrimary)
        val iconTint  = ContextCompat.getColor(ctx, R.color.icon_tint)

        val card = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (120 * dp).toInt()
            ).also {
                val m = (6 * dp).toInt()
                it.setMargins(m, m, m, m)
            }
            setBackgroundColor(bgColor)
        }

        // Active border
        val activeBorder = View(ctx).apply {
            tag = "active"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, (3 * dp).toInt()
            ).also { it.gravity = android.view.Gravity.TOP }
            setBackgroundColor(blue)
            visibility = View.GONE
        }

        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            val p = (10 * dp).toInt()
            setPadding(p, p, p, p)
        }

        val titleTv = TextView(ctx).apply {
            tag = "title"
            textSize = 13f
            setTextColor(textColor)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val urlTv = TextView(ctx).apply {
            tag = "url"
            textSize = 11f
            setTextColor(subColor)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        content.addView(titleTv)
        content.addView(urlTv)

        val closeBtn = ImageView(ctx).apply {
            tag = "close"
            layoutParams = FrameLayout.LayoutParams(
                (32 * dp).toInt(), (32 * dp).toInt()
            ).also { it.gravity = android.view.Gravity.TOP or android.view.Gravity.END }
            val p = (6 * dp).toInt()
            setPadding(p, p, p, p)
            setImageDrawable(svgFn("icons/svg/close.svg", 16, iconTint))
            background = ContextCompat.getDrawable(ctx,
                android.R.drawable.list_selector_background)
            isClickable = true
            isFocusable = true
        }

        card.addView(activeBorder)
        card.addView(content)
        card.addView(closeBtn)

        return VH(card)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tab = tabs[position]
        holder.title.text = tab.title.ifEmpty { tab.url }
        holder.url.text   = tab.url
        holder.active.visibility = if (tab.id == currentId) View.VISIBLE else View.GONE
        holder.root.setOnClickListener { onSelect(tab) }
        holder.close.setOnClickListener { onClose(tab) }
    }

    override fun getItemCount() = tabs.size

    fun updateTabs(newTabs: MutableList<BrowserTab>, newCurrentId: String) {
        tabs      = newTabs
        currentId = newCurrentId
        notifyDataSetChanged()
    }
}