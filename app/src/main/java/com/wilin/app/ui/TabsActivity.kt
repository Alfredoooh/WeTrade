package com.wilin.app.ui

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.caverock.androidsvg.SVG
import com.wilin.app.R
import java.io.File

/**
 * TabsActivity — redesenhada completamente.
 *
 * A ideia:
 * - O ecrã torna-se um HorizontalScrollView de cards flutuantes
 * - O card mais à direita é o ecrã atual (Home / última tab ativa)
 * - Os cards à esquerda são as tabs recentes
 * - Deslizar para a esquerda revela as tabs anteriores
 * - Clicar num card abre essa tab
 * - O bottom bar NÃO aparece aqui — permanece na MainActivity
 */
class TabsActivity : AppCompatActivity() {

    private lateinit var insetsController: WindowInsetsControllerCompat
    private var transitionPreview: Bitmap? = null

    companion object {
        const val EXTRA_TRANSITION_SCREENSHOT_PATH = "transition_screenshot_path"
        private const val CARD_WIDTH_DP   = 300f
        private const val CARD_HEIGHT_DP  = 520f
        private const val CARD_GAP_DP     = 16f
        private const val CARD_SIDE_DP    = 20f  // margem lateral mínima
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        insetsController = WindowInsetsControllerCompat(window, window.decorView)
        applyTheme()

        TabManager.init(this)
        TabScreenshots.init(this)

        transitionPreview = intent.getStringExtra(EXTRA_TRANSITION_SCREENSHOT_PATH)
            ?.let { path -> runCatching { BitmapFactory.decodeFile(File(path).absolutePath) }.getOrNull() }

        val dp       = resources.displayMetrics.density
        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)
        val blue     = ContextCompat.getColor(this, R.color.colorPrimary)
        val bg       = ContextCompat.getColor(this, R.color.background)

        // Root: fundo do app
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(bg)
        }

        // ── Toolbar ──────────────────────────────────────────────────────────
        val toolbarH = (56 * dp).toInt()
        val toolbar = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, toolbarH, Gravity.TOP
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((4 * dp).toInt(), 0, (12 * dp).toInt(), 0)
            setBackgroundColor(ContextCompat.getColor(this@TabsActivity, R.color.appbar_background))
        }

        val btnBack = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((44 * dp).toInt(), (44 * dp).toInt())
            setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/back_arrow.svg", 24, iconTint))
            isClickable = true; isFocusable = true
            background = rippleDrawable()
            setOnClickListener { finish() }
        }

        val tabCountTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .also { it.marginStart = (4 * dp).toInt() }
            val count = TabManager.getTabs().size
            text = if (count == 1) "1 Aba" else "$count Abas"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@TabsActivity, R.color.text_primary))
        }

        val btnNewTab = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((44 * dp).toInt(), (44 * dp).toInt())
            setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/add.svg", 24, iconTint))
            isClickable = true; isFocusable = true
            background = rippleDrawable()
            setOnClickListener { newTabAndOpen() }
        }

        toolbar.addView(btnBack)
        toolbar.addView(tabCountTv)
        toolbar.addView(btnNewTab)

        // ── Área dos cards: HorizontalScrollView ─────────────────────────────
        val cardW   = (CARD_WIDTH_DP  * dp).toInt()
        val cardH   = (CARD_HEIGHT_DP * dp).toInt()
        val cardGap = (CARD_GAP_DP   * dp).toInt()
        val sideM   = (CARD_SIDE_DP  * dp).toInt()

        val hScrollView = HorizontalScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
        }

        val cardsRow = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(sideM, toolbarH + (16 * dp).toInt(), sideM, (80 * dp).toInt())
        }

        hScrollView.addView(cardsRow)

        // Construir lista de items: Home (screenshot) + tabs recentes
        data class CardItem(
            val id: String,
            val title: String,
            val url: String,
            val preview: Bitmap?,
            val isHome: Boolean = false
        )

        val tabs = TabManager.getTabs()
        val currentId = TabManager.getCurrentId()
        val items = mutableListOf<CardItem>()

        // Tabs recentes (à esquerda, as mais antigas primeiro)
        tabs.forEach { tab ->
            items.add(CardItem(
                id = tab.id,
                title = tab.title.ifEmpty { if (tab.url.isEmpty()) "Nova aba" else tab.url },
                url = tab.url,
                preview = TabScreenshots.get(this, tab.id)
            ))
        }

        // Card do ecrã atual (à direita de tudo — Home / screenshot)
        transitionPreview?.let {
            items.add(CardItem(id = "__home__", title = "Início", url = "", preview = it, isHome = true))
        }

        items.forEachIndexed { index, item ->
            val isActive = item.id == currentId || item.isHome
            val card = buildCard(item.id, item.title, item.url, item.preview, item.isHome, isActive,
                cardW, cardH, cardGap, dp, blue, iconTint, tabs) {
                // onClose
                tabCountTv.text = run {
                    val c = TabManager.getTabs().size
                    if (c == 1) "1 Aba" else "$c Abas"
                }
            }
            cardsRow.addView(card)
        }

        // Botão + fixo em baixo ao centro
        val btnNewBottom = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (56 * dp).toInt(), (56 * dp).toInt(),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).also { it.bottomMargin = (24 * dp).toInt() }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(blue)
            }
            elevation = 10f * dp
            isClickable = true; isFocusable = true
            setOnClickListener { newTabAndOpen() }
        }
        val plusIv = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams((24 * dp).toInt(), (24 * dp).toInt(), Gravity.CENTER)
            setImageDrawable(svgDrawable("icons/svg/add.svg", 24, Color.WHITE))
        }
        btnNewBottom.addView(plusIv)

        root.addView(hScrollView)
        root.addView(toolbar)
        root.addView(btnNewBottom)
        setContentView(root)

        // Scroll para o último card (mais recente) após layout
        hScrollView.post {
            hScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
        }
    }

    private fun buildCard(
        id: String,
        title: String,
        url: String,
        preview: Bitmap?,
        isHome: Boolean,
        isActive: Boolean,
        cardW: Int,
        cardH: Int,
        cardGap: Int,
        dp: Float,
        blue: Int,
        iconTint: Int,
        tabs: List<BrowserTab>,
        onClose: () -> Unit
    ): FrameLayout {
        val ctx = this

        val card = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(cardW, cardH).also {
                it.marginEnd = cardGap
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18 * dp
                setColor(ContextCompat.getColor(ctx, R.color.card_background))
                if (isActive) setStroke((2 * dp).toInt(), blue)
            }
            clipToOutline = true
            elevation = if (isActive) 12f * dp else 4f * dp
        }

        // Preview da página
        val previewIv = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).also { it.topMargin = (52 * dp).toInt() }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.background))
        }
        preview?.let { previewIv.setImageBitmap(it) }

        // Header do card
        val header = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (52 * dp).toInt(),
                Gravity.TOP
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * dp).toInt(), 0, (8 * dp).toInt(), 0)
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.card_background))
        }

        val faviconIv = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((20 * dp).toInt(), (20 * dp).toInt())
                .also { it.marginEnd = (10 * dp).toInt() }
            setImageDrawable(
                if (isHome) svgDrawable("icons/svg/home_filled.svg", 20, blue)
                else svgDrawable("icons/svg/hub.svg", 20,
                    ContextCompat.getColor(ctx, R.color.icon_tint_secondary))
            )
        }
        if (!isHome && url.isNotEmpty()) loadFavicon(url, faviconIv)

        val titleTv = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            text = title
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        val closeBtn = ImageView(ctx).apply {
            val sz = (36 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz)
            setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/close.svg", 16,
                ContextCompat.getColor(ctx, R.color.icon_tint_secondary)))
            isClickable = true; isFocusable = true
            background = rippleDrawable()
            visibility = if (isHome) View.INVISIBLE else View.VISIBLE
            setOnClickListener {
                if (!isHome) {
                    val tab = tabs.firstOrNull { it.id == id } ?: return@setOnClickListener
                    card.animate()
                        .translationY(card.height.toFloat() + 40f)
                        .alpha(0f)
                        .setDuration(220)
                        .withEndAction {
                            (card.parent as? ViewGroup)?.removeView(card)
                            TabScreenshots.remove(ctx, tab.id)
                            TabManager.closeTab(tab.id)
                            TabManager.save(ctx)
                            onClose()
                            if (TabManager.count() == 0) newTabAndOpen()
                        }.start()
                }
            }
        }

        header.addView(faviconIv)
        header.addView(titleTv)
        header.addView(closeBtn)

        card.addView(previewIv)
        card.addView(header)

        card.setOnClickListener {
            if (isHome) {
                finish()
            } else {
                val tab = tabs.firstOrNull { it.id == id } ?: return@setOnClickListener
                TabManager.setCurrentId(tab.id)
                TabManager.save(ctx)
                card.animate()
                    .scaleX(1.03f).scaleY(1.03f)
                    .setDuration(90)
                    .withEndAction {
                        startActivity(Intent(ctx, BrowserResponseActivity::class.java).apply {
                            putExtra(BrowserResponseActivity.EXTRA_TAB_ID, tab.id)
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        })
                        finish()
                    }.start()
            }
        }

        return card
    }

    private fun newTabAndOpen() {
        TabManager.newTab()
        TabManager.save(this)
        startActivity(Intent(this, BrowserResponseActivity::class.java).apply {
            putExtra(BrowserResponseActivity.EXTRA_TAB_ID, TabManager.getCurrentId())
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }

    private fun loadFavicon(url: String, iv: ImageView) {
        val host = runCatching { android.net.Uri.parse(url).host ?: "" }.getOrDefault("")
        if (host.isEmpty()) return
        Thread {
            runCatching {
                val conn = java.net.URL("https://www.google.com/s2/favicons?domain=$host&sz=32")
                    .openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 2000; conn.readTimeout = 2000
                val bmp = android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                conn.disconnect()
                if (bmp != null) Handler(Looper.getMainLooper()).post { iv.setImageBitmap(bmp) }
            }
        }.start()
    }

    private fun applyTheme() {
        val isLight = !resources.configuration.isNightModeActive
        insetsController.isAppearanceLightStatusBars = isLight
        window.statusBarColor = ContextCompat.getColor(this, R.color.appbar_background)
    }

    private fun rippleDrawable() = with(android.util.TypedValue()) {
        theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
        ContextCompat.getDrawable(this@TabsActivity, resourceId)
    }

    private fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
        val px  = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        try {
            val svg = SVG.getFromAsset(assets, path)
            svg.documentWidth  = px.toFloat()
            svg.documentHeight = px.toFloat()
            svg.renderToCanvas(Canvas(bmp))
        } catch (_: Exception) {}
        val drawable = BitmapDrawable(resources, bmp)
        drawable.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
        return drawable
    }
}