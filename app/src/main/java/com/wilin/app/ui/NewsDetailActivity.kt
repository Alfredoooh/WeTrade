package com.wilin.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.caverock.androidsvg.SVG
import com.wilin.app.R
import com.wilin.app.news.NewsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class NewsDetailActivity : AppCompatActivity() {

    private lateinit var insetsController: WindowInsetsControllerCompat

    companion object {
        const val EXTRA_URL         = "detail_url"
        const val EXTRA_TITLE       = "detail_title"
        const val EXTRA_IMAGE       = "detail_image"
        const val EXTRA_SOURCE      = "detail_source"
        const val EXTRA_DESCRIPTION = "detail_description"
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        insetsController = WindowInsetsControllerCompat(window, window.decorView)
        window.statusBarColor = ContextCompat.getColor(this, R.color.appbar_background)
        val isLight = !resources.configuration.isNightModeActive
        insetsController.isAppearanceLightStatusBars = isLight

        val dp     = resources.displayMetrics.density
        val isDark = !isLight

        val url         = intent.getStringExtra(EXTRA_URL) ?: ""
        val titleInit   = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val imageInit   = intent.getStringExtra(EXTRA_IMAGE) ?: ""
        val sourceInit  = intent.getStringExtra(EXTRA_SOURCE) ?: ""
        val descInit    = intent.getStringExtra(EXTRA_DESCRIPTION) ?: ""

        val bgColor   = ContextCompat.getColor(this, R.color.background)
        val textPrim  = ContextCompat.getColor(this, R.color.text_primary)
        val textSec   = ContextCompat.getColor(this, R.color.text_secondary)
        val appbarBg  = ContextCompat.getColor(this, R.color.appbar_background)
        val iconTint  = ContextCompat.getColor(this, R.color.icon_tint)

        // ── Root ──────────────────────────────────────────────────────────────
        val root = FrameLayout(this).apply {
            setBackgroundColor(bgColor)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // ── AppBar ────────────────────────────────────────────────────────────
        val appbar = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, (56 * dp).toInt()
            )
            setBackgroundColor(appbarBg)
            elevation = 4 * dp
        }

        val btnBack = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (48 * dp).toInt(), (48 * dp).toInt(), Gravity.START or Gravity.CENTER_VERTICAL
            ).also { it.marginStart = (4 * dp).toInt() }
            scaleType = ImageView.ScaleType.CENTER
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(this@NewsDetailActivity, R.drawable.ripple_item)
            setImageDrawable(svgDrawable("icons/svg/arrow_left.svg", 24, iconTint))
        }
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        val btnBrowser = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (48 * dp).toInt(), (48 * dp).toInt(), Gravity.END or Gravity.CENTER_VERTICAL
            ).also { it.marginEnd = (4 * dp).toInt() }
            scaleType = ImageView.ScaleType.CENTER
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(this@NewsDetailActivity, R.drawable.ripple_item)
            setImageDrawable(svgDrawable("icons/svg/magnifying_glass_outline.svg", 20, iconTint))
        }
        btnBrowser.setOnClickListener {
            startActivity(Intent(this, BrowserResponseActivity::class.java).apply {
                putExtra(BrowserResponseActivity.EXTRA_QUERY, url)
            })
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        val appbarTitle = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            text = sourceInit.uppercase()
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(textSec)
        }

        appbar.addView(btnBack)
        appbar.addView(appbarTitle)
        appbar.addView(btnBrowser)

        // ── ScrollView com conteúdo ───────────────────────────────────────────
        val scroll = ScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).also { it.topMargin = (56 * dp).toInt() }
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val content = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }

        // Imagem de destaque
        val heroImage = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (220 * dp).toInt()
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#F2F2F7"))
        }

        // Padding do texto
        val textContent = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt(), (32 * dp).toInt())
        }

        val sourceBadge = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (10 * dp).toInt() }
            text = "• ${sourceInit.uppercase()}"
            textSize = 11f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@NewsDetailActivity, R.color.colorPrimary))
        }

        val titleTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (12 * dp).toInt() }
            text = titleInit
            textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(textPrim)
            lineSpacingMultiplier = 1.2f
        }

        val metaTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (16 * dp).toInt() }
            textSize = 12f
            setTextColor(textSec)
        }

        // Divider
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
            ).also {
                it.bottomMargin = (16 * dp).toInt()
                it.topMargin = (4 * dp).toInt()
            }
            setBackgroundColor(ContextCompat.getColor(this@NewsDetailActivity, R.color.divider))
        }

        val bodyTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (24 * dp).toInt() }
            text = if (descInit.isNotEmpty()) descInit else "A carregar..."
            textSize = 16f
            setTextColor(textPrim)
            lineSpacingMultiplier = 1.6f
        }

        // Botão "Ler artigo completo no browser"
        val btnFull = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (48 * dp).toInt()
            )
            text = "Abrir no browser"
            textSize = 15f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14 * dp
                setColor(ContextCompat.getColor(this@NewsDetailActivity, R.color.colorPrimary))
            }
            isClickable = true
            isFocusable = true
        }
        btnFull.setOnClickListener {
            startActivity(Intent(this, BrowserResponseActivity::class.java).apply {
                putExtra(BrowserResponseActivity.EXTRA_QUERY, url)
            })
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Loading spinner simples
        val loadingTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (8 * dp).toInt() }
            text = "A carregar artigo..."
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(textSec)
            visibility = View.VISIBLE
        }

        textContent.addView(sourceBadge)
        textContent.addView(titleTv)
        textContent.addView(metaTv)
        textContent.addView(divider)
        textContent.addView(loadingTv)
        textContent.addView(bodyTv)
        textContent.addView(btnFull)

        content.addView(heroImage)
        content.addView(textContent)
        scroll.addView(content)

        root.addView(scroll)
        root.addView(appbar)
        setContentView(root)

        // ── Carrega imagem de destaque ────────────────────────────────────────
        if (imageInit.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val resp = http.newCall(Request.Builder().url(imageInit).build()).execute()
                    val bmp  = resp.body?.byteStream()?.let { BitmapFactory.decodeStream(it) }
                    if (bmp != null) withContext(Dispatchers.Main) { heroImage.setImageBitmap(bmp) }
                } catch (_: Exception) {}
            }
        }

        // ── Carrega detalhe completo da API ───────────────────────────────────
        if (url.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val detail = NewsRepository.fetchDetail(url)
                    loadingTv.visibility = View.GONE

                    if (detail.title.isNotEmpty()) titleTv.text = detail.title

                    val meta = buildString {
                        if (detail.author.isNotEmpty()) append(detail.author)
                        if (detail.author.isNotEmpty() && detail.publishedAt.isNotEmpty()) append("  ·  ")
                        if (detail.publishedAt.isNotEmpty()) append(detail.publishedAt.take(16))
                    }
                    metaTv.text = meta
                    metaTv.visibility = if (meta.isEmpty()) View.GONE else View.VISIBLE

                    val bodyText = when {
                        detail.body.isNotEmpty()        -> detail.body
                        detail.description.isNotEmpty() -> detail.description
                        else                            -> descInit
                    }
                    bodyTv.text = bodyText

                    // Carrega imagem se não havia ainda
                    if (imageInit.isEmpty() && detail.imageUrl.isNotEmpty()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val resp = http.newCall(Request.Builder().url(detail.imageUrl).build()).execute()
                                val bmp  = resp.body?.byteStream()?.let { BitmapFactory.decodeStream(it) }
                                if (bmp != null) withContext(Dispatchers.Main) { heroImage.setImageBitmap(bmp) }
                            } catch (_: Exception) {}
                        }
                    }
                } catch (_: Exception) {
                    loadingTv.text = "Não foi possível carregar o artigo."
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onResume() {
        super.onResume()
        window.statusBarColor = ContextCompat.getColor(this, R.color.appbar_background)
        val isLight = !resources.configuration.isNightModeActive
        insetsController.isAppearanceLightStatusBars = isLight
    }

    private fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
        val px  = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        SVG.getFromAsset(assets, path).apply {
            documentWidth  = px.toFloat()
            documentHeight = px.toFloat()
            renderToCanvas(android.graphics.Canvas(bmp))
        }
        return BitmapDrawable(resources, bmp).also {
            it.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
        }
    }
}