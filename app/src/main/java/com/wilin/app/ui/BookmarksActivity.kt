package com.wilin.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.wilin.app.R
import com.wilin.app.databinding.ActivityBookmarksBinding

class BookmarksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookmarksBinding
    private val bookmarks = mutableListOf<Pair<String, String>>() // url to title

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)

        binding.btnBack.setImageDrawable(svgDrawable("icons/svg/back_arrow.svg", 24, iconTint))
        binding.btnBack.setOnClickListener { finish() }

        loadBookmarks()

        val adapter = BookmarksAdapter(
            items = bookmarks,
            onClick = { url ->
                TabManager.init(this)
                val tab = TabManager.newTab(url)
                TabManager.save(this)
                startActivity(
                    Intent(this, BrowserResponseActivity::class.java).apply {
                        putExtra(BrowserResponseActivity.EXTRA_TAB_ID, tab.id)
                    }
                )
                finish()
            },
            onDelete = { pos ->
                bookmarks.removeAt(pos)
                saveBookmarks()
                binding.emptyState.visibility = if (bookmarks.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            },
            svgFn = { path, size, tint -> svgDrawable(path, size, tint) }
        )

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.emptyState.visibility = if (bookmarks.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun loadBookmarks() {
        val prefs = getSharedPreferences("wilin_bookmarks", MODE_PRIVATE)
        val raw = prefs.getString("bookmarks", "") ?: ""
        raw.split("|||").filter { it.isNotEmpty() }.forEach { entry ->
            val parts = entry.split("::")
            if (parts.size >= 2) bookmarks.add(parts[0] to parts[1])
        }
    }

    private fun saveBookmarks() {
        val raw = bookmarks.joinToString("|||") { "${it.first}::${it.second}" }
        getSharedPreferences("wilin_bookmarks", MODE_PRIVATE)
            .edit().putString("bookmarks", raw).apply()
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

class BookmarksAdapter(
    private val items: MutableList<Pair<String, String>>,
    private val onClick: (String) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val svgFn: (String, Int, Int) -> BitmapDrawable
) : RecyclerView.Adapter<BookmarksAdapter.VH>() {

    inner class VH(val root: LinearLayout) : RecyclerView.ViewHolder(root) {
        val title: TextView  = root.findViewWithTag("title")
        val url: TextView    = root.findViewWithTag("url")
        val delete: android.widget.ImageView = root.findViewWithTag("delete")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx   = parent.context
        val dp    = ctx.resources.displayMetrics.density
        val bgColor   = ContextCompat.getColor(ctx, R.color.background)
        val textColor = ContextCompat.getColor(ctx, R.color.text_primary)
        val subColor  = ContextCompat.getColor(ctx, R.color.text_secondary)
        val iconTint  = ContextCompat.getColor(ctx, R.color.icon_tint_secondary)

        val row = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val h = (16 * dp).toInt()
            val v = (12 * dp).toInt()
            setPadding(h, v, h, v)
            setBackgroundColor(bgColor)
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(ctx, android.R.drawable.list_selector_background)
        }

        val textBlock = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val titleTv = TextView(ctx).apply {
            tag = "title"
            textSize = 14f
            setTextColor(textColor)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val urlTv = TextView(ctx).apply {
            tag = "url"
            textSize = 12f
            setTextColor(subColor)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        textBlock.addView(titleTv)
        textBlock.addView(urlTv)

        val deleteBtn = android.widget.ImageView(ctx).apply {
            tag = "delete"
            layoutParams = LinearLayout.LayoutParams(
                (40 * dp).toInt(), (40 * dp).toInt()
            )
            val p = (8 * dp).toInt()
            setPadding(p, p, p, p)
            setImageDrawable(svgFn("icons/svg/close.svg", 20, iconTint))
            background = ContextCompat.getDrawable(ctx, android.R.drawable.list_selector_background)
            isClickable = true
            isFocusable = true
        }

        row.addView(textBlock)
        row.addView(deleteBtn)
        return VH(row)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val (url, title) = items[position]
        holder.title.text = title
        holder.url.text   = url
        holder.root.setOnClickListener { onClick(url) }
        holder.delete.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_ID.toInt()) {
                onDelete(pos)
                notifyItemRemoved(pos)
            }
        }
    }

    override fun getItemCount() = items.size
}