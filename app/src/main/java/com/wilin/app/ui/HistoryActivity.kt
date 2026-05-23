package com.wilin.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.wilin.app.R
import com.wilin.app.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val history = mutableListOf<String>()

    companion object {
        private const val PREFS_HISTORY = "wilin_search_history"
        private const val KEY_HISTORY   = "history"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)

        binding.btnBack.setImageDrawable(svgDrawable("icons/svg/back_arrow.svg", 24, iconTint))
        binding.btnBack.setOnClickListener { finish() }

        binding.btnClear.setOnClickListener {
            history.clear()
            getSharedPreferences(PREFS_HISTORY, MODE_PRIVATE)
                .edit().remove(KEY_HISTORY).apply()
            binding.recycler.adapter?.notifyDataSetChanged()
            binding.emptyState.visibility = android.view.View.VISIBLE
        }

        loadHistory()

        val adapter = HistoryActivityAdapter(
            items = history,
            onClick = { query ->
                TabManager.init(this)
                val url = if (query.startsWith("http")) query
                          else "https://duckduckgo.com/?q=${android.net.Uri.encode(query)}&kae=d&k1=-1"
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
                history.removeAt(pos)
                saveHistory()
                binding.emptyState.visibility = if (history.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            },
            svgFn = { path, size, tint -> svgDrawable(path, size, tint) }
        )

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.emptyState.visibility = if (history.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun loadHistory() {
        val prefs = getSharedPreferences(PREFS_HISTORY, MODE_PRIVATE)
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        history.addAll(raw.split("|||").filter { it.isNotEmpty() })
    }

    private fun saveHistory() {
        getSharedPreferences(PREFS_HISTORY, MODE_PRIVATE)
            .edit().putString(KEY_HISTORY, history.joinToString("|||")).apply()
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

class HistoryActivityAdapter(
    private val items: MutableList<String>,
    private val onClick: (String) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val svgFn: (String, Int, Int) -> BitmapDrawable
) : RecyclerView.Adapter<HistoryActivityAdapter.VH>() {

    inner class VH(val root: LinearLayout) : RecyclerView.ViewHolder(root) {
        val text: TextView = root.findViewWithTag("text")
        val delete: android.widget.ImageView = root.findViewWithTag("delete")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx   = parent.context
        val dp    = ctx.resources.displayMetrics.density
        val bgColor   = ContextCompat.getColor(ctx, R.color.background)
        val textColor = ContextCompat.getColor(ctx, R.color.text_primary)
        val iconTint  = ContextCompat.getColor(ctx, R.color.icon_tint_secondary)

        val row = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val h = (16 * dp).toInt()
            val v = (14 * dp).toInt()
            setPadding(h, v, h, v)
            setBackgroundColor(bgColor)
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(ctx, android.R.drawable.list_selector_background)
        }

        val textTv = TextView(ctx).apply {
            tag = "text"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 14f
            setTextColor(textColor)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val deleteBtn = android.widget.ImageView(ctx).apply {
            tag = "delete"
            layoutParams = LinearLayout.LayoutParams(
                (40 * dp).toInt(), (40 * dp).toInt())
            val p = (8 * dp).toInt()
            setPadding(p, p, p, p)
            setImageDrawable(svgFn("icons/svg/close.svg", 20, iconTint))
            background = ContextCompat.getDrawable(ctx, android.R.drawable.list_selector_background)
            isClickable = true
            isFocusable = true
        }

        row.addView(textTv)
        row.addView(deleteBtn)
        return VH(row)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.text.text = items[position]
        holder.root.setOnClickListener { onClick(items[position]) }
        holder.delete.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos >= 0) {
                onDelete(pos)
                notifyItemRemoved(pos)
            }
        }
    }

    override fun getItemCount() = items.size
}