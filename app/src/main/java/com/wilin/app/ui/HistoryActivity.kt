// HistoryActivity.kt
package com.wilin.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
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
        val dp       = resources.displayMetrics.density
        val textPrim = ContextCompat.getColor(this, R.color.text_primary)
        val textSec  = ContextCompat.getColor(this, R.color.text_secondary)
        val blue     = ContextCompat.getColor(this, R.color.colorPrimary)
        val red      = Color.parseColor("#FF3B30")
        val divColor = ContextCompat.getColor(this, R.color.divider)

        // Resolver ripple fora de qualquer apply para evitar reatribuição de val
        val tv = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
        val rippleBg = tv.resourceId

        // Overlay escuro
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(120, 0, 0, 0))
            isClickable = true
        }

        // Card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background  = ContextCompat.getDrawable(this@HistoryActivity, R.drawable.rounded_card_bg)
            val hPad = (24 * dp).toInt()
            setPadding(hPad, (28 * dp).toInt(), hPad, (20 * dp).toInt())
            elevation = 24f
        }

        // Ícone
        val iconWarn = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (16 * dp).toInt() }
            setImageDrawable(svgDrawable("icons/svg/history.svg", 32, red))
            alpha = 0.9f
        }

        // Título
        val title = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (10 * dp).toInt() }
            text = "Limpar histórico"
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(textPrim)
        }

        // Mensagem
        val message = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (8 * dp).toInt() }
            text = "Todos os registos do histórico serão apagados permanentemente."
            textSize = 14f
            setTextColor(textSec)
            setlineSpacing =(0f, 1.4f)
        }

        // Aviso irreversível
        val warning = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (24 * dp).toInt() }
            text = "⚠ Esta ação não pode ser desfeita."
            textSize = 12f
            setTextColor(red)
        }

        // Divisor horizontal
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).also { it.bottomMargin = (4 * dp).toInt() }
            setBackgroundColor(divColor)
        }

        // Linha de botões
        val btnRow = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }

        // Botão NÃO — background resolvido antes do apply
        val btnNo = TextView(this)
        btnNo.layoutParams = LinearLayout.LayoutParams(0, (48 * dp).toInt(), 1f)
        btnNo.text = "Não"
        btnNo.textSize = 15f
        btnNo.gravity = Gravity.CENTER
        btnNo.setTypeface(btnNo.typeface, android.graphics.Typeface.BOLD)
        btnNo.setTextColor(blue)
        btnNo.isClickable = true
        btnNo.isFocusable = true
        btnNo.setBackgroundResource(rippleBg)

        // Divisor vertical entre botões
        val btnDivider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(1, LinearLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(divColor)
        }

        // Botão SIM
        val btnYes = TextView(this)
        btnYes.layoutParams = LinearLayout.LayoutParams(0, (48 * dp).toInt(), 1f)
        btnYes.text = "Sim"
        btnYes.textSize = 15f
        btnYes.gravity = Gravity.CENTER
        btnYes.setTypeface(btnYes.typeface, android.graphics.Typeface.BOLD)
        btnYes.setTextColor(red)
        btnYes.isClickable = true
        btnYes.isFocusable = true
        btnYes.setBackgroundResource(rippleBg)

        btnRow.addView(btnNo)
        btnRow.addView(btnDivider)
        btnRow.addView(btnYes)

        card.addView(iconWarn)
        card.addView(title)
        card.addView(message)
        card.addView(warning)
        card.addView(divider)
        card.addView(btnRow)

        val cardParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ).apply {
            val m = (32 * dp).toInt()
            leftMargin = m; rightMargin = m
        }
        overlay.addView(card, cardParams)

        val root = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        root.addView(overlay, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Animação entrada
        overlay.alpha = 0f
        card.scaleX = 0.92f; card.scaleY = 0.92f
        overlay.animate().alpha(1f).setDuration(200).start()
        card.animate().scaleX(1f).scaleY(1f).setDuration(220)
            .setInterpolator(DecelerateInterpolator(2f)).start()

        fun dismiss() {
            overlay.animate().alpha(0f).setDuration(160).withEndAction {
                root.removeView(overlay)
            }.start()
        }

        overlay.setOnClickListener { dismiss() }
        btnNo.setOnClickListener   { dismiss() }
        btnYes.setOnClickListener  {
            dismiss()
            history.clear()
            getSharedPreferences(PREFS_HISTORY, MODE_PRIVATE).edit().remove(KEY_HISTORY).apply()
            faviconCacheDir().listFiles()?.forEach { it.delete() }
            binding.recycler.adapter?.notifyDataSetChanged()
            binding.emptyState.visibility = View.VISIBLE
        }
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

        // Resolver ripple fora de apply
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
            gravity     = android.view.Gravity.CENTER_VERTICAL
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
                    (context as? androidx.appcompat.app.AppCompatActivity)
                        ?.runOnUiThread { imageView.setImageBitmap(bmp) }
                }
            } catch (_: Exception) { /* silencioso */ }
        }.start()
    }
}