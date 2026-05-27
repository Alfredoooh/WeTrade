// HistoryActivity.kt
package com.wilin.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wilin.app.R
import com.wilin.app.databinding.ActivityHistoryBinding
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private val history = mutableListOf<String>()

    companion object {
        private const val PREFS_HISTORY = "wilin_search_history"
        private const val KEY_HISTORY   = "history"
        private const val FAVICON_CACHE = "favicons"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)
        binding.btnBack.setImageDrawable(svgDrawable("icons/svg/back_arrow.svg", 24, iconTint))
        binding.btnBack.setOnClickListener { finish() }
        binding.btnClear.setOnClickListener { showClearConfirmDialog() }

        loadHistory()

        val adapter = HistoryActivityAdapter(
            items        = history,
            context      = this,
            onClick      = { query ->
                TabManager.init(this)
                val url = if (query.startsWith("http")) query
                          else "https://duckduckgo.com/?q=${Uri.encode(query)}&kae=d&k1=-1"
                val tab = TabManager.newTab(url)
                TabManager.save(this)
                startActivity(
                    Intent(this, BrowserResponseActivity::class.java).apply {
                        putExtra(BrowserResponseActivity.EXTRA_TAB_ID, tab.id)
                    }
                )
                finish()
            },
            onDelete     = { pos ->
                history.removeAt(pos)
                saveHistory()
                binding.emptyState.visibility =
                    if (history.isEmpty()) View.VISIBLE else View.GONE
            },
            svgFn        = { path, size, tint -> svgDrawable(path, size, tint) },
            faviconCache = faviconCacheDir()
        )

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter
        binding.emptyState.visibility = if (history.isEmpty()) View.VISIBLE else View.GONE
    }

    // ─── Dialog de confirmação ────────────────────────────────────────────────

    private fun showClearConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Limpar histórico")
            .setMessage("Todos os registos do histórico serão apagados permanentemente. Esta ação não pode ser desfeita.")
            .setNegativeButton("Não") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("Sim") { dialog, _ ->
                dialog.dismiss()
                history.clear()
                getSharedPreferences(PREFS_HISTORY, MODE_PRIVATE).edit().remove(KEY_HISTORY).apply()
                faviconCacheDir().listFiles()?.forEach { it.delete() }
                binding.recycler.adapter?.notifyDataSetChanged()
                binding.emptyState.visibility = View.VISIBLE
            }
            .show()
    }

    // ─── Histórico ────────────────────────────────────────────────────────────

    private fun loadHistory() {
        val raw = getSharedPreferences(PREFS_HISTORY, MODE_PRIVATE)
            .getString(KEY_HISTORY, "") ?: ""
        history.addAll(raw.split("|||").filter { it.isNotEmpty() })
    }

    private fun saveHistory() {
        getSharedPreferences(PREFS_HISTORY, MODE_PRIVATE)
            .edit().putString(KEY_HISTORY, history.joinToString("|||")).apply()
    }

    private fun faviconCacheDir(): File {
        val dir = File(cacheDir, FAVICON_CACHE)
        if (!dir.exists()) dir.mkdirs()
        return dir
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

// ─── Adapter com favicon cacheado ─────────────────────────────────────────────

class HistoryActivityAdapter(
    private val items: MutableList<String>,
    private val context: Context,
    private val onClick: (String) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val svgFn: (String, Int, Int) -> BitmapDrawable,
    private val faviconCache: File
) : RecyclerView.Adapter<HistoryActivityAdapter.VH>() {

    inner class VH(val root: LinearLayout) : RecyclerView.ViewHolder(root) {
        val favicon: ImageView = root.findViewWithTag("favicon")
        val text:    TextView  = root.findViewWithTag("text")
        val delete:  ImageView = root.findViewWithTag("delete")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx       = parent.context
        val dp        = ctx.resources.displayMetrics.density
        val textColor = ContextCompat.getColor(ctx, R.color.text_primary)
        val iconSec   = ContextCompat.getColor(ctx, R.color.icon_tint_secondary)

        val tv = TypedValue()
        ctx.theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
        val rowRipple = tv.resourceId

        val tv2 = TypedValue()
        ctx.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv2, true)
        val borderlessRipple = tv2.resourceId

        val row = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            val h = (16 * dp).toInt()
            val v = (13 * dp).toInt()
            setPadding(h, v, h, v)
            isClickable = true; isFocusable = true
            setBackgroundResource(rowRipple)
        }

        val faviconIv = ImageView(ctx).apply {
            tag = "favicon"
            val sz = (18 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz).also {
                it.marginEnd = (12 * dp).toInt()
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val textTv = TextView(ctx).apply {
            tag = "text"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 14f
            setTextColor(textColor)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }

        val deleteBtn = ImageView(ctx)
        deleteBtn.tag = "delete"
        val sz = (40 * dp).toInt()
        deleteBtn.layoutParams = LinearLayout.LayoutParams(sz, sz)
        val p = (10 * dp).toInt()
        deleteBtn.setPadding(p, p, p, p)
        deleteBtn.setImageDrawable(svgFn("icons/svg/close.svg", 18, iconSec))
        deleteBtn.isClickable = true
        deleteBtn.isFocusable = true
        deleteBtn.setBackgroundResource(borderlessRipple)

        row.addView(faviconIv)
        row.addView(textTv)
        row.addView(deleteBtn)
        return VH(row)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val query = items[position]
        holder.text.text = query
        holder.root.setOnClickListener { onClick(query) }
        holder.delete.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos >= 0) { onDelete(pos); notifyItemRemoved(pos) }
        }

        val host = extractHost(query)
        if (host != null) {
            loadFavicon(host, holder.favicon)
        } else {
            val tint = ContextCompat.getColor(context, R.color.icon_tint_secondary)
            holder.favicon.setImageDrawable(
                svgFn("icons/svg/magnifying_glass_outline.svg", 18, tint)
            )
        }
    }

    override fun getItemCount() = items.size

    private fun extractHost(query: String): String? {
        return try {
            when {
                query.startsWith("http://") || query.startsWith("https://") ->
                    Uri.parse(query).host?.removePrefix("www.")
                query.contains(".") && !query.contains(" ") ->
                    Uri.parse("https://$query").host?.removePrefix("www.")
                else -> null
            }
        } catch (_: Exception) { null }
    }

    private fun loadFavicon(host: String, imageView: ImageView) {
        val safeKey   = host.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val cacheFile = File(faviconCache, "$safeKey.png")

        if (cacheFile.exists()) {
            val bmp = BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (bmp != null) { imageView.setImageBitmap(bmp); return }
        }

        val tint = ContextCompat.getColor(context, R.color.icon_tint_secondary)
        imageView.setImageDrawable(svgFn("icons/svg/magnifying_glass_outline.svg", 18, tint))

        Thread {
            try {
                val conn = URL("https://www.google.com/s2/favicons?domain=$host&sz=32")
                    .openConnection() as HttpURLConnection
                conn.connectTimeout = 4000; conn.readTimeout = 4000
                val bmp = BitmapFactory.decodeStream(conn.inputStream)
                conn.disconnect()
                if (bmp != null) {
                    cacheFile.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
                    (context as? AppCompatActivity)
                        ?.runOnUiThread { imageView.setImageBitmap(bmp) }
                }
            } catch (_: Exception) { /* silencioso */ }
        }.start()
    }
}