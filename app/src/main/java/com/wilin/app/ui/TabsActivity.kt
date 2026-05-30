// TabsActivity.kt
package com.wilin.app.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.GridLayout
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
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class TabsActivity : AppCompatActivity() {

    private lateinit var insetsController: WindowInsetsControllerCompat
    private lateinit var gridLayout: GridLayout
    private lateinit var titleTv: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var rootLayout: FrameLayout   // FrameLayout para o efeito de sobreposição

    private var editMode   = false
    private var showNormal = true
    private var cardW      = 0
    private var cardH      = 0
    private var dp         = 1f

    private var srcWidth  = 0
    private var srcHeight = 0
    private var srcX      = 0f
    private var srcY      = 0f

    private val faviconCache = mutableMapOf<String, Bitmap>()

    // ── Cores do tema dinâmicas ───────────────────────────────────────────────
    private val isNight get() = resources.configuration.uiMode
        .and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

    // Liquid Glass: fundo translúcido com blur
    private fun glassColor(alpha: Int = 180): Int =
        if (isNight) Color.argb(alpha, 20, 20, 22) else Color.argb(alpha, 250, 250, 252)

    private fun glassStrokeColor(): Int =
        if (isNight) Color.argb(40, 255, 255, 255) else Color.argb(60, 0, 0, 0)

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("wilin_prefs", Context.MODE_PRIVATE)
        val lang  = prefs.getString("language", "") ?: ""
        val base  = if (lang.isNotEmpty()) {
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        } else newBase
        super.attachBaseContext(base)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        insetsController = WindowInsetsControllerCompat(window, window.decorView)
        applyTheme()

        TabManager.init(this)
        TabScreenshots.init(this)
        loadFaviconCache()

        srcWidth  = intent.getIntExtra("anim_src_width",  0)
        srcHeight = intent.getIntExtra("anim_src_height", 0)
        srcX      = intent.getFloatExtra("anim_src_x",    0f)
        srcY      = intent.getFloatExtra("anim_src_y",    0f)

        dp           = resources.displayMetrics.density
        val bgColor  = ContextCompat.getColor(this, R.color.background)
        val blue     = ContextCompat.getColor(this, R.color.colorPrimary)
        val textPri  = ContextCompat.getColor(this, R.color.text_primary)
        val textSec  = ContextCompat.getColor(this, R.color.icon_tint_secondary)
        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)

        val screenW = resources.displayMetrics.widthPixels
        cardW = (screenW / 2) - (24 * dp).toInt()
        cardH = (cardW * 1.5f).toInt()

        // Root é FrameLayout para sobrepor o liquid glass no topo e fundo
        rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(bgColor)
            alpha = 0f
        }

        // ── ScrollView + Grid (vai atrás de tudo) ────────────────────────────
        val scrollContainer = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            orientation  = LinearLayout.VERTICAL
        }

        // Espaçador topo (para a grid não ficar atrás do liquid glass da top bar)
        val topSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (60 * dp).toInt())
        }

        scrollView = ScrollView(this).apply {
            layoutParams   = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            overScrollMode = View.OVER_SCROLL_NEVER
        }
        gridLayout = GridLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            columnCount  = 2
            setPadding((12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt(), (80 * dp).toInt())
        }
        scrollView.addView(gridLayout)
        scrollContainer.addView(topSpacer)
        scrollContainer.addView(scrollView)
        rootLayout.addView(scrollContainer)

        // ── Liquid Glass TOP BAR ─────────────────────────────────────────────
        val topBarLp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, (60 * dp).toInt(), Gravity.TOP
        )
        val topBar = buildLiquidGlassBar(height = (60 * dp).toInt(), isTop = true)
        topBar.layoutParams = topBarLp

        val btnSearch = ImageView(this).apply {
            val lp = FrameLayout.LayoutParams((40 * dp).toInt(), (40 * dp).toInt(), Gravity.CENTER_VERTICAL or Gravity.START)
            lp.marginStart = (10 * dp).toInt()
            layoutParams   = lp
            setPadding((9 * dp).toInt(), (9 * dp).toInt(), (9 * dp).toInt(), (9 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/magnifying_glass_filled.svg", 20, iconTint))
            isClickable = true; isFocusable = true
            background  = ContextCompat.getDrawable(this@TabsActivity, R.drawable.ripple_circle)
        }

        // ── Segmented pill Liquid Glass ───────────────────────────────────────
        val pillBg = GradientDrawable()
        pillBg.shape        = GradientDrawable.RECTANGLE
        pillBg.cornerRadius = 20 * dp
        pillBg.setColor(if (isNight) Color.argb(60, 255, 255, 255) else Color.argb(50, 0, 0, 0))
        pillBg.setStroke((1 * dp).toInt(), glassStrokeColor())

        val pillContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams((200 * dp).toInt(), (34 * dp).toInt(), Gravity.CENTER)
            background   = pillBg
        }

        val indicatorBg = GradientDrawable()
        indicatorBg.shape        = GradientDrawable.RECTANGLE
        indicatorBg.cornerRadius = 17 * dp
        // Indicator Liquid Glass
        indicatorBg.setColor(if (isNight) Color.argb(140, 60, 60, 70) else Color.argb(200, 255, 255, 255))
        indicatorBg.setStroke((1 * dp).toInt(), if (isNight) Color.argb(50, 255, 255, 255) else Color.argb(40, 0, 0, 0))

        val indicator = View(this).apply {
            val lp = FrameLayout.LayoutParams((98 * dp).toInt(), (30 * dp).toInt(), Gravity.START or Gravity.CENTER_VERTICAL)
            lp.marginStart = (2 * dp).toInt()
            layoutParams   = lp
            background     = indicatorBg
            elevation      = 2 * dp
        }

        titleTv = TextView(this).apply {
            val lp = FrameLayout.LayoutParams((98 * dp).toInt(), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.START or Gravity.CENTER_VERTICAL)
            lp.marginStart = (2 * dp).toInt()
            layoutParams   = lp
            text           = "${TabManager.count()} Tabs"
            textSize       = 13f
            gravity        = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(textPri)
            elevation = 3 * dp
        }

        val incognitoTv = TextView(this).apply {
            val lp = FrameLayout.LayoutParams((98 * dp).toInt(), FrameLayout.LayoutParams.MATCH_PARENT, Gravity.END or Gravity.CENTER_VERTICAL)
            lp.marginEnd = (2 * dp).toInt()
            layoutParams = lp
            text         = getString(R.string.incognito)
            textSize     = 13f
            gravity      = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(textSec)
            elevation = 3 * dp
        }

        pillContainer.addView(indicator)
        pillContainer.addView(titleTv)
        pillContainer.addView(incognitoTv)

        topBar.addView(btnSearch)
        topBar.addView(pillContainer)
        rootLayout.addView(topBar)

        // ── Liquid Glass BOTTOM BAR ──────────────────────────────────────────
        val bottomBarH = (64 * dp).toInt()
        val bottomBarLp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, bottomBarH, Gravity.BOTTOM)
        val bottomBar = buildLiquidGlassBar(height = bottomBarH, isTop = false)
        bottomBar.layoutParams = bottomBarLp

        val btnEdit = TextView(this).apply {
            val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.START)
            lp.marginStart = (16 * dp).toInt()
            layoutParams   = lp
            text           = getString(R.string.history)  // "Edit" traduzido como Editar
            textSize       = 17f
            setTextColor(blue)
            isClickable = true; isFocusable = true
            background  = ContextCompat.getDrawable(this@TabsActivity, R.drawable.ripple_item)
            setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt())
        }
        // Re-uso: "Edit" string hardcoded para evitar ausência de recurso
        btnEdit.text = "Edit"

        val newTabOval = GradientDrawable()
        newTabOval.shape = GradientDrawable.OVAL
        newTabOval.setColor(blue)

        val btnNewTab = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams((48 * dp).toInt(), (48 * dp).toInt(), Gravity.CENTER)
            background   = newTabOval
            isClickable  = true; isFocusable = true
            elevation    = 8 * dp
        }
        // Sombra suave no botão +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            btnNewTab.outlineAmbientShadowColor = Color.argb(60, 0, 122, 255)
            btnNewTab.outlineSpotShadowColor    = Color.argb(80, 0, 122, 255)
        }
        val plusIv = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams((22 * dp).toInt(), (22 * dp).toInt(), Gravity.CENTER)
            setImageDrawable(svgDrawable("icons/svg/add.svg", 22, Color.WHITE))
        }
        btnNewTab.addView(plusIv)

        val btnDone = TextView(this).apply {
            val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END)
            lp.marginEnd = (16 * dp).toInt()
            layoutParams = lp
            text         = getString(R.string.done)
            textSize     = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(blue)
            isClickable = true; isFocusable = true
            background  = ContextCompat.getDrawable(this@TabsActivity, R.drawable.ripple_item)
            setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt())
        }

        bottomBar.addView(btnEdit)
        bottomBar.addView(btnNewTab)
        bottomBar.addView(btnDone)
        rootLayout.addView(bottomBar)

        setContentView(rootLayout)
        renderGrid()

        // Segmented control
        fun selectSegment(normal: Boolean) {
            showNormal = normal
            val targetX = if (normal) 0f else (98 * dp)
            indicator.animate()
                .translationX(targetX)
                .setDuration(250)
                .setInterpolator(OvershootInterpolator(1.5f))
                .start()
            titleTv.setTextColor(if (normal) textPri else textSec)
            incognitoTv.setTextColor(if (normal) textSec else textPri)
            titleTv.text = "${TabManager.count()} Tabs"
            renderGrid()
        }

        titleTv.setOnClickListener     { selectSegment(true) }
        incognitoTv.setOnClickListener { selectSegment(false) }
        btnDone.setOnClickListener     { finishWithAnimation() }
        btnEdit.setOnClickListener     { toggleEditMode() }
        btnNewTab.setOnClickListener   { if (showNormal) newTabAndOpen() else newIncognitoTabAndOpen() }
        btnSearch.setOnClickListener   {
            startActivity(Intent(this, SearchActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
            overridePendingTransition(0, 0)
        }

        if (srcWidth > 0) rootLayout.post { runEnterAnimation() }
        else rootLayout.alpha = 1f
    }

    // ── Liquid Glass Bar builder ──────────────────────────────────────────────

    private fun buildLiquidGlassBar(height: Int, isTop: Boolean): FrameLayout {
        val bar = FrameLayout(this).apply {
            clipToOutline = false
        }

        // Camada 1: blur (apenas API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurLayer = View(this).apply {
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, height)
                setBackgroundColor(Color.TRANSPARENT)
                setRenderEffect(
                    RenderEffect.createBlurEffect(28f, 28f, Shader.TileMode.CLAMP)
                )
            }
            bar.addView(blurLayer)
        }

        // Camada 2: fundo translúcido colorido
        val tintLayer = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, height)
            setBackgroundColor(glassColor(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 160 else 220))
        }
        bar.addView(tintLayer)

        // Camada 3: borda brilhante (1dp) — simula o specular highlight do vidro
        val borderLayer = View(this).apply {
            val borderH = (1 * dp).toInt()
            val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, borderH)
            lp.gravity = if (isTop) Gravity.BOTTOM else Gravity.TOP
            layoutParams = lp
            setBackgroundColor(glassStrokeColor())
        }
        bar.addView(borderLayer)

        bar.elevation = 4 * dp
        return bar
    }

    // ── Animação entrada ──────────────────────────────────────────────────────

    private fun runEnterAnimation() {
        val tabs      = TabManager.getTabs()
        val currentId = TabManager.getCurrentId()
        val activeIdx = tabs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)

        val col       = activeIdx % 2
        val row       = activeIdx / 2
        val padH      = 12 * dp
        val gapH      = 12 * dp
        val cardFullH = cardH + 40 * dp
        val topBarH   = 60 * dp

        val destX = padH + col * (cardW + gapH * 2)
        val destY = topBarH + padH + row * (cardFullH + gapH * 2)
        val destW = cardW.toFloat()
        val destH = cardFullH

        val ghostBg = GradientDrawable()
        ghostBg.shape        = GradientDrawable.RECTANGLE
        ghostBg.cornerRadius = 0f
        ghostBg.setColor(ContextCompat.getColor(this, R.color.card_background))

        val previewBmp = TabScreenshots.get(this, currentId)
        val tab        = tabs.find { it.id == currentId }

        val ghost = FrameLayout(this).apply {
            background      = ghostBg
            clipToOutline   = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, 0f) }
            }
        }

        val ghostHeader = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (38 * dp).toInt(), Gravity.TOP)
            orientation  = LinearLayout.HORIZONTAL
            gravity      = Gravity.CENTER_VERTICAL
            setPadding((10 * dp).toInt(), 0, (4 * dp).toInt(), 0)
            setBackgroundColor(ContextCompat.getColor(this@TabsActivity, R.color.card_background))
            alpha = 0f
        }
        val ghostFavIv = ImageView(this).apply {
            val lp = LinearLayout.LayoutParams((16 * dp).toInt(), (16 * dp).toInt())
            lp.marginEnd = (6 * dp).toInt()
            layoutParams = lp
        }
        val ghostTitleTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            text      = tab?.title?.ifEmpty { if (tab.url.isEmpty()) getString(R.string.new_tab) else tab.url } ?: getString(R.string.new_tab)
            textSize  = 12f
            maxLines  = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@TabsActivity, R.color.text_primary))
        }
        ghostHeader.addView(ghostFavIv)
        ghostHeader.addView(ghostTitleTv)

        if (previewBmp != null) {
            val previewIv = ImageView(this).apply {
                val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                lp.topMargin = (38 * dp).toInt()
                layoutParams = lp
                scaleType    = ImageView.ScaleType.CENTER_CROP
                setImageBitmap(previewBmp)
            }
            ghost.addView(previewIv)
        }
        ghost.addView(ghostHeader)

        val host = runCatching { android.net.Uri.parse(tab?.url ?: "").host ?: "" }.getOrDefault("")
        if (host.isNotEmpty()) {
            faviconCache[host]?.let { ghostFavIv.setImageBitmap(it) }
                ?: loadAndCacheFavicon(host, ghostFavIv)
        }

        val overlay = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(ContextCompat.getColor(this@TabsActivity, R.color.background))
            alpha = 0f
        }

        val ghostContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        ghostContainer.addView(overlay)

        val ghostLp = FrameLayout.LayoutParams(srcWidth, srcHeight.coerceAtLeast(1))
        ghostLp.leftMargin = srcX.toInt()
        ghostLp.topMargin  = srcY.toInt()
        ghost.layoutParams = ghostLp
        ghostContainer.addView(ghost)

        val decorView = window.decorView as FrameLayout
        decorView.addView(ghostContainer)

        val duration = 420L
        val interp   = DecelerateInterpolator(2.2f)
        val scaleX   = destW / srcWidth
        val scaleY   = destH / srcHeight

        ghost.pivotX = srcWidth / 2f
        ghost.pivotY = srcHeight / 2f

        val targetPivotX = destX + destW / 2f
        val targetPivotY = destY + destH / 2f
        val translateX   = targetPivotX - (srcX + srcWidth / 2f)
        val translateY   = targetPivotY - (srcY + srcHeight / 2f)

        val cornerAnimator = ValueAnimator.ofFloat(0f, 14 * dp).apply {
            this.duration = duration
            interpolator  = interp
            addUpdateListener { anim ->
                val r = anim.animatedValue as Float
                ghostBg.cornerRadius = r
                ghost.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, r) }
                }
            }
        }

        val headerAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator  = interp
            addUpdateListener { anim ->
                val p = anim.animatedFraction
                if (p > 0.6f) ghostHeader.alpha = ((p - 0.6f) / 0.4f).coerceIn(0f, 1f)
            }
        }

        val animSet = AnimatorSet()
        animSet.playTogether(
            ObjectAnimator.ofFloat(ghost, "translationX", 0f, translateX).apply { this.duration = duration; this.interpolator = interp },
            ObjectAnimator.ofFloat(ghost, "translationY", 0f, translateY).apply { this.duration = duration; this.interpolator = interp },
            ObjectAnimator.ofFloat(ghost, "scaleX", 1f, scaleX).apply { this.duration = duration; this.interpolator = interp },
            ObjectAnimator.ofFloat(ghost, "scaleY", 1f, scaleY).apply { this.duration = duration; this.interpolator = interp },
            ObjectAnimator.ofFloat(overlay, "alpha", 0f, 1f).apply { this.duration = duration; this.interpolator = interp },
            ObjectAnimator.ofFloat(rootLayout, "alpha", 0f, 1f).apply { this.duration = duration; this.interpolator = interp },
            cornerAnimator,
            headerAnimator
        )
        animSet.start()
        animSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                decorView.removeView(ghostContainer)
                renderGrid()
            }
        })
    }

    // ── Click no card: expande animado para o browser ─────────────────────────

    private fun openTabWithExpand(tabId: String, cardView: View) {
        val loc = IntArray(2)
        cardView.getLocationOnScreen(loc)

        val expandBg = GradientDrawable()
        expandBg.shape        = GradientDrawable.RECTANGLE
        expandBg.cornerRadius = 14 * dp
        expandBg.setColor(ContextCompat.getColor(this, R.color.card_background))

        val expandView = View(this).apply {
            background      = expandBg
            clipToOutline   = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, 14 * dp) }
            }
        }

        val expandOverlay = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val cardLp = FrameLayout.LayoutParams(cardView.width, cardView.height)
        cardLp.leftMargin = loc[0]
        cardLp.topMargin  = loc[1]
        expandView.layoutParams = cardLp
        expandOverlay.addView(expandView)

        val decorView = window.decorView as FrameLayout
        decorView.addView(expandOverlay)

        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = resources.displayMetrics.heightPixels.toFloat()

        expandView.pivotX = cardView.width / 2f
        expandView.pivotY = cardView.height / 2f

        val targetScaleX = screenW / cardView.width
        val targetScaleY = screenH / cardView.height
        val targetTransX = screenW / 2f - (loc[0] + cardView.width / 2f)
        val targetTransY = screenH / 2f - (loc[1] + cardView.height / 2f)

        val duration = 320L
        val interp   = DecelerateInterpolator(2f)

        val cornerAnim = ValueAnimator.ofFloat(14 * dp, 0f).apply {
            this.duration     = duration
            this.interpolator = interp
            addUpdateListener { anim ->
                val r = anim.animatedValue as Float
                expandBg.cornerRadius = r
                expandView.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, r) }
                }
            }
        }

        val animSet = AnimatorSet()
        animSet.playTogether(
            ObjectAnimator.ofFloat(expandView, "scaleX", 1f, targetScaleX).apply { this.duration = duration; this.interpolator = interp },
            ObjectAnimator.ofFloat(expandView, "scaleY", 1f, targetScaleY).apply { this.duration = duration; this.interpolator = interp },
            ObjectAnimator.ofFloat(expandView, "translationX", 0f, targetTransX).apply { this.duration = duration; this.interpolator = interp },
            ObjectAnimator.ofFloat(expandView, "translationY", 0f, targetTransY).apply { this.duration = duration; this.interpolator = interp },
            cornerAnim
        )
        animSet.start()
        animSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                decorView.removeView(expandOverlay)
                TabManager.switchToTab(this@TabsActivity, tabId)
                val intent = Intent(this@TabsActivity, BrowserResponseActivity::class.java).apply {
                    putExtra(BrowserResponseActivity.EXTRA_TAB_ID, tabId)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }
        })
    }

    // ── Grid ──────────────────────────────────────────────────────────────────

    private fun renderGrid() {
        gridLayout.removeAllViews()
        val tabs      = TabManager.getTabs()
        val currentId = TabManager.getCurrentId()
        val blue      = ContextCompat.getColor(this, R.color.colorPrimary)
        val bgCard    = ContextCompat.getColor(this, R.color.card_background)
        val textPri   = ContextCompat.getColor(this, R.color.text_primary)
        val textSec   = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        titleTv.text = "${tabs.size} Tabs"

        tabs.forEachIndexed { idx, tab ->
            val isActive  = tab.id == currentId
            val cardFrame = buildTabCard(tab, isActive, blue, bgCard, textPri, textSec)
            val lp = GridLayout.LayoutParams().apply {
                width      = cardW
                height     = cardH + (40 * dp).toInt()
                columnSpec = GridLayout.spec(idx % 2, 1f)
                rowSpec    = GridLayout.spec(idx / 2, 1f)
                setMargins((6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt())
            }
            cardFrame.layoutParams = lp
            gridLayout.addView(cardFrame)
        }
    }

    private fun buildTabCard(
        tab: BrowserTab, isActive: Boolean,
        blue: Int, bgCard: Int, textPri: Int, textSec: Int
    ): FrameLayout {

        val cardBg = GradientDrawable()
        cardBg.shape        = GradientDrawable.RECTANGLE
        cardBg.cornerRadius = 14 * dp
        cardBg.setColor(bgCard)
        if (isActive) cardBg.setStroke((3 * dp).toInt(), blue)
        else cardBg.setStroke((1 * dp).toInt(), Color.argb(30, 128, 128, 128))

        val card = FrameLayout(this).apply {
            clipToOutline = true
            background    = cardBg
            elevation     = if (isActive) 8 * dp else 2 * dp
            if (!isActive) { alpha = 0f; scaleX = 0.92f; scaleY = 0.92f }
            isClickable   = true
            isFocusable   = true
        }

        val header = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (38 * dp).toInt(), Gravity.TOP)
            orientation  = LinearLayout.HORIZONTAL
            gravity      = Gravity.CENTER_VERTICAL
            setPadding((10 * dp).toInt(), 0, (4 * dp).toInt(), 0)
            setBackgroundColor(bgCard)
        }

        val faviconIv = ImageView(this).apply {
            val lp = LinearLayout.LayoutParams((16 * dp).toInt(), (16 * dp).toInt())
            lp.marginEnd = (6 * dp).toInt()
            layoutParams = lp
        }

        val host = runCatching { android.net.Uri.parse(tab.url).host ?: "" }.getOrDefault("")
        if (host.isNotEmpty()) {
            faviconCache[host]?.let { faviconIv.setImageBitmap(it) }
                ?: loadAndCacheFavicon(host, faviconIv)
        }

        val cardTitleTv = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            text = when {
                tab.url.isEmpty() || tab.url == "about:blank" -> getString(R.string.new_tab)
                tab.title.isNotEmpty() -> tab.title
                else -> host.ifEmpty { tab.url }
            }
            textSize  = 12f
            maxLines  = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(textPri)
        }

        val closeBtnSz = (29 * dp).toInt()
        val closeBtn = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(closeBtnSz, closeBtnSz)
            val pad = (7 * dp).toInt()
            setPadding(pad, pad, pad, pad)
            setImageDrawable(svgDrawable("icons/svg/close.svg", 13, textSec))
            isClickable = true; isFocusable = true
            background  = ContextCompat.getDrawable(this@TabsActivity, R.drawable.ripple_circle)
        }

        header.addView(faviconIv)
        header.addView(cardTitleTv)
        header.addView(closeBtn)

        val previewLp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        previewLp.topMargin = (38 * dp).toInt()
        val previewIv = ImageView(this).apply {
            layoutParams = previewLp
            scaleType    = ImageView.ScaleType.CENTER_CROP
        }

        val preview = TabScreenshots.get(this, tab.id)
        if (preview != null) {
            val cropH = (preview.width * (cardH.toFloat() / cardW.toFloat())).toInt().coerceAtMost(preview.height)
            val cropped = if (cropH < preview.height) Bitmap.createBitmap(preview, 0, 0, preview.width, cropH) else preview
            previewIv.setImageBitmap(cropped)
        } else {
            previewIv.setBackgroundColor(ContextCompat.getColor(this, R.color.input_background))
        }

        card.addView(previewIv)
        card.addView(header)

        if (!isActive) {
            card.post {
                card.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(280).setInterpolator(DecelerateInterpolator(2f)).start()
            }
        }

        card.setOnClickListener { openTabWithExpand(tab.id, card) }

        closeBtn.setOnClickListener {
            card.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(180)
                .setInterpolator(DecelerateInterpolator(2f))
                .withEndAction {
                    TabScreenshots.remove(this, tab.id)
                    TabManager.closeTab(tab.id)
                    TabManager.save(this)
                    if (TabManager.count() == 0) newTabAndOpen()
                    else renderGrid()
                }.start()
        }

        return card
    }

    // ── Favicon cache em disco ────────────────────────────────────────────────

    private fun faviconDir(): File = File(filesDir, "favicons").also { if (!it.exists()) it.mkdirs() }
    private fun faviconFile(host: String): File = File(faviconDir(), "${host.replace(".", "_")}.png")

    private fun loadFaviconCache() {
        faviconDir().listFiles()?.forEach { f ->
            runCatching {
                val bmp = BitmapFactory.decodeFile(f.absolutePath)
                if (bmp != null) faviconCache[f.nameWithoutExtension.replace("_", ".")] = bmp
            }
        }
    }

    private fun loadAndCacheFavicon(host: String, iv: ImageView) {
        Thread {
            runCatching {
                val conn = URL("https://www.google.com/s2/favicons?domain=$host&sz=32").openConnection() as HttpURLConnection
                conn.connectTimeout = 3000; conn.readTimeout = 3000
                val bmp = BitmapFactory.decodeStream(conn.inputStream)
                conn.disconnect()
                if (bmp != null) {
                    faviconCache[host] = bmp
                    runCatching {
                        FileOutputStream(faviconFile(host)).use { out ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                    runOnUiThread { iv.setImageBitmap(bmp) }
                }
            }
        }.start()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun toggleEditMode() { editMode = !editMode }

    private fun newTabAndOpen() {
        val tab = TabManager.newTab()
        TabManager.save(this)
        startActivity(Intent(this, BrowserResponseActivity::class.java).apply {
            putExtra(BrowserResponseActivity.EXTRA_TAB_ID, tab.id)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        overridePendingTransition(0, 0)
        finish()
    }

    private fun newIncognitoTabAndOpen() {
        val tab = TabManager.newTab()
        TabManager.save(this)
        startActivity(Intent(this, BrowserResponseActivity::class.java).apply {
            putExtra(BrowserResponseActivity.EXTRA_TAB_ID, tab.id)
            putExtra(BrowserResponseActivity.EXTRA_INCOGNITO, true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        overridePendingTransition(0, 0)
        finish()
    }

    private fun finishWithAnimation() {
        finish()
        overridePendingTransition(0, android.R.anim.fade_out)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    private fun applyTheme() {
        val isLight = !isNight
        window.statusBarColor = ContextCompat.getColor(this, R.color.background)
        insetsController.isAppearanceLightStatusBars = isLight
    }

    private fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
        val px  = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        try {
            SVG.getFromAsset(assets, path).apply {
                documentWidth  = px.toFloat()
                documentHeight = px.toFloat()
                renderToCanvas(Canvas(bmp))
            }
        } catch (_: Exception) {}
        return BitmapDrawable(resources, bmp).also { it.setColorFilter(tint, PorterDuff.Mode.SRC_IN) }
    }
}