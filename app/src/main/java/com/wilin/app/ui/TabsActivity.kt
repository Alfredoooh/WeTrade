package com.wilin.app.ui

import android.content.Intent
import android.graphics.Bitmap
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.caverock.androidsvg.SVG
import com.wilin.app.R

class TabsActivity : AppCompatActivity() {

    private lateinit var insetsController: WindowInsetsControllerCompat
    private lateinit var stackRoot: FrameLayout
    private lateinit var scrollContainer: FrameLayout
    private val tabCardViews = mutableListOf<FrameLayout>()

    // Estado do scroll manual
    private var scrollY       = 0f
    private var maxScrollY    = 0f
    private var lastTouchY    = 0f
    private var velocityY     = 0f
    private var lastEventTime = 0L
    private val flingHandler  = Handler(Looper.getMainLooper())

    companion object {
        // Altura visível do topo de cada card atrás do da frente (como na imagem)
        private const val CARD_PEEK_DP   = 64f   // quanto de cada card fica visível
        private const val CARD_HEIGHT_DP = 220f   // altura total de cada card
        private const val TOOLBAR_DP     = 56f
        private const val BOTTOM_PAD_DP  = 100f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        insetsController = WindowInsetsControllerCompat(window, window.decorView)
        applyTheme()

        TabManager.init(this)
        TabScreenshots.init(this)

        val dp       = resources.displayMetrics.density
        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)
        val blue     = ContextCompat.getColor(this, R.color.colorPrimary)

        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ContextCompat.getColor(this@TabsActivity, R.color.background))
        }

        // Toolbar fixa no topo
        val toolbar = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (TOOLBAR_DP * dp).toInt(),
                Gravity.TOP
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((4 * dp).toInt(), 0, (8 * dp).toInt(), 0)
            setBackgroundColor(ContextCompat.getColor(this@TabsActivity, R.color.appbar_background))
            elevation = 4f * dp
        }

        val btnClose = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((44 * dp).toInt(), (44 * dp).toInt())
            setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/back_arrow.svg", 24, iconTint))
            isClickable = true; isFocusable = true
            background = with(android.util.TypedValue()) {
                theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
                ContextCompat.getDrawable(this@TabsActivity, resourceId)
            }
            setOnClickListener { finish() }
        }

        val tabCountTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .also { it.marginStart = (4 * dp).toInt() }
            val count = TabManager.getTabs().size
            text = if (count == 1) "1 Aba" else "$count Abas"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@TabsActivity, R.color.text_primary))
        }

        val btnNew = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((44 * dp).toInt(), (44 * dp).toInt())
            setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/add.svg", 24, iconTint))
            isClickable = true; isFocusable = true
            background = with(android.util.TypedValue()) {
                theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
                ContextCompat.getDrawable(this@TabsActivity, resourceId)
            }
            setOnClickListener { newTabAndOpen() }
        }

        toolbar.addView(btnClose)
        toolbar.addView(tabCountTv)
        toolbar.addView(btnNew)

        // Botão + fixo em baixo (como na imagem)
        val btnNewBottom = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (56 * dp).toInt(), (56 * dp).toInt(),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).also { it.bottomMargin = (28 * dp).toInt() }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(blue)
            }
            elevation = 10f * dp
            isClickable = true; isFocusable = true
        }
        val plusIv = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (24 * dp).toInt(), (24 * dp).toInt(), Gravity.CENTER
            )
            setImageDrawable(svgDrawable("icons/svg/add.svg", 24, Color.WHITE))
        }
        btnNewBottom.addView(plusIv)
        btnNewBottom.setOnClickListener { newTabAndOpen() }

        // Área do stack
        stackRoot = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        scrollContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        stackRoot.addView(scrollContainer)
        root.addView(stackRoot)
        root.addView(toolbar)
        root.addView(btnNewBottom)
        setContentView(root)

        // Touch no stackRoot para scroll manual
        stackRoot.setOnTouchListener { _, event -> handleTouch(event) }

        stackRoot.post { buildStack(dp, blue, iconTint) }
    }

    /**
     * Constrói o baralho de cartas vertical exatamente como na imagem:
     * - Cada card tem altura fixa (CARD_HEIGHT_DP)
     * - Só o topo de cada card (CARD_PEEK_DP) fica visível sob o card da frente
     * - O card mais recente (último da lista) fica no topo
     * - Ao fazer scroll para baixo os cards vão descendo, revelando os de trás
     */
    private fun buildStack(dp: Float, blue: Int, iconTint: Int) {
        scrollContainer.removeAllViews()
        tabCardViews.clear()

        val tabs      = TabManager.getTabs()
        val currentId = TabManager.getCurrentId()
        val screenW   = stackRoot.width.toFloat()
        val screenH   = stackRoot.height.toFloat()

        val toolbarH  = TOOLBAR_DP * dp
        val cardW     = (screenW - 32 * dp).toInt()   // margem de 16dp de cada lado
        val cardH     = (CARD_HEIGHT_DP * dp).toInt()
        val cardX     = (16 * dp).toInt()
        val peekH     = (CARD_PEEK_DP * dp).toInt()   // altura visível do cabeçalho de cada card

        // O primeiro card começa logo abaixo da toolbar
        // Cada card subsequente fica peekH abaixo do anterior
        // Assim o stack ocupa: toolbarH + peekH * (n-1) + cardH
        tabs.forEachIndexed { index, tab ->
            val isActive = tab.id == currentId

            // Posição Y: o card de índice 0 fica em baixo (atrás), o último fica em cima (frente)
            // Na imagem os mais recentes estão em cima
            // cardY = toolbarH + (tabs.size - 1 - index) * peekH
            // Mas como o scroll vai deslocar tudo para baixo, mantemos posições absolutas
            val stackIndex = tabs.size - 1 - index  // 0 = topo (mais recente)
            val cardY = (toolbarH + stackIndex * peekH).toInt()

            val card = FrameLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(cardW, cardH).also {
                    it.leftMargin = cardX
                    it.topMargin  = cardY
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 14 * dp
                    setColor(ContextCompat.getColor(this@TabsActivity, R.color.card_background))
                    if (isActive) setStroke((2 * dp).toInt(), blue)
                }
                clipToOutline = true
                elevation     = (tabs.size - stackIndex).toFloat() * dp + if (isActive) 8 * dp else 0f
            }

            // Preview do conteúdo (abaixo do header)
            val previewIv = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                ).also { it.topMargin = peekH }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(Color.parseColor("#1A1A1A"))
            }
            val bmp = TabScreenshots.get(this, tab.id)
            if (bmp != null) previewIv.setImageBitmap(bmp)

            // Header — sempre visível mesmo quando coberto pelos outros cards
            val header = LinearLayout(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    peekH
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((14 * dp).toInt(), 0, (10 * dp).toInt(), 0)
                setBackgroundColor(ContextCompat.getColor(this@TabsActivity,
                    R.color.card_background))
            }

            // Favicon ou ícone do site
            val faviconIv = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams((20 * dp).toInt(), (20 * dp).toInt())
                    .also { it.marginEnd = (10 * dp).toInt() }
                setImageDrawable(svgDrawable("icons/svg/globe.svg", 20,
                    ContextCompat.getColor(this@TabsActivity, R.color.icon_tint_secondary)))
            }
            if (tab.url.isNotEmpty()) loadFavicon(tab.url, faviconIv)

            val titleTv = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = tab.title.ifEmpty { if (tab.url.contains("duckduckgo") || tab.url.isEmpty()) "Início" else tab.url }
                textSize = 14f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(ContextCompat.getColor(this@TabsActivity, R.color.text_primary))
            }

            // Botão X fechar
            val closeBtn = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams((36 * dp).toInt(), (36 * dp).toInt())
                setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt())
                setImageDrawable(svgDrawable("icons/svg/close.svg", 16,
                    ContextCompat.getColor(this@TabsActivity, R.color.icon_tint_secondary)))
                isClickable = true; isFocusable = true
                background = with(android.util.TypedValue()) {
                    theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
                    ContextCompat.getDrawable(this@TabsActivity, resourceId)
                }
                setOnClickListener {
                    closeCard(card, tab)
                }
            }

            header.addView(faviconIv)
            header.addView(titleTv)
            header.addView(closeBtn)

            card.addView(previewIv)
            card.addView(header)

            // Clique no card — expande para ecrã cheio
            card.setOnClickListener {
                if (tab.id != TabManager.getCurrentId() || true) {
                    expandCard(card, tab)
                }
            }

            scrollContainer.addView(card)
            tabCardViews.add(0, card) // índice 0 = topo da lista visual
        }

        // Altura total do conteúdo de scroll
        val totalH = (toolbarH + (tabs.size - 1) * peekH + cardH + BOTTOM_PAD_DP * dp).toInt()
        scrollContainer.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            totalH
        )
        maxScrollY = maxOf(0f, totalH - screenH)
    }

    private fun closeCard(card: FrameLayout, tab: BrowserTab) {
        card.animate()
            .translationX(card.width.toFloat() + 50f)
            .alpha(0f)
            .setDuration(220)
            .withEndAction {
                scrollContainer.removeView(card)
                tabCardViews.remove(card)
                TabScreenshots.remove(this, tab.id)
                TabManager.closeTab(tab.id)
                TabManager.save(this)
                if (TabManager.count() == 0) {
                    newTabAndOpen()
                } else {
                    val dp   = resources.displayMetrics.density
                    val blue = ContextCompat.getColor(this, R.color.colorPrimary)
                    val iconTint = ContextCompat.getColor(this, R.color.icon_tint)
                    buildStack(dp, blue, iconTint)
                }
            }.start()
    }

    private fun expandCard(card: View, tab: BrowserTab) {
        TabManager.setCurrentId(tab.id)
        TabManager.save(this)
        card.animate()
            .scaleX(1.04f).scaleY(1.04f)
            .setDuration(100)
            .withEndAction {
                startActivity(Intent(this, BrowserResponseActivity::class.java).apply {
                    putExtra(BrowserResponseActivity.EXTRA_TAB_ID, tab.id)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
                finish()
            }.start()
    }

    // ── Scroll manual com fling ──────────────────────────────────────────────

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                flingHandler.removeCallbacksAndMessages(null)
                lastTouchY    = event.rawY
                velocityY     = 0f
                lastEventTime = System.currentTimeMillis()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val now   = System.currentTimeMillis()
                val delta = lastTouchY - event.rawY
                val dt    = (now - lastEventTime).toFloat().coerceAtLeast(1f)
                velocityY     = delta / dt * 16f  // pixels por frame aprox
                lastTouchY    = event.rawY
                lastEventTime = now
                applyScroll(delta)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                startFling()
                return true
            }
        }
        return false
    }

    private fun applyScroll(delta: Float) {
        scrollY = (scrollY + delta).coerceIn(0f, maxScrollY)
        scrollContainer.translationY = -scrollY
    }

    private fun startFling() {
        if (kotlin.math.abs(velocityY) < 1f) return
        flingHandler.post(object : Runnable {
            override fun run() {
                if (kotlin.math.abs(velocityY) < 0.5f) return
                applyScroll(velocityY)
                velocityY *= 0.92f
                flingHandler.post(this)
            }
        })
    }

    // ── Utilitários ──────────────────────────────────────────────────────────

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