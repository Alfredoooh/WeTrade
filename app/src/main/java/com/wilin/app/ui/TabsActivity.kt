package com.wilin.app.ui

import android.content.Context
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
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
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

class TabsActivity : AppCompatActivity() {

    private lateinit var insetsController: WindowInsetsControllerCompat
    private val tabCards = mutableListOf<View>()
    private lateinit var scrollView: ScrollView
    private lateinit var stackContainer: LinearLayout

    companion object {
        const val EXTRA_SCREENSHOT = "screenshot_tab_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        insetsController = WindowInsetsControllerCompat(window, window.decorView)
        applyTheme()

        val ctx  = this
        val dp   = resources.displayMetrics.density
        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)
        val iconSec  = ContextCompat.getColor(this, R.color.icon_tint_secondary)
        val blue     = ContextCompat.getColor(this, R.color.colorPrimary)

        val root = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.background))
        }

        // Toolbar
        val toolbar = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (56 * dp).toInt(),
                Gravity.TOP
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((4 * dp).toInt(), 0, (8 * dp).toInt(), 0)
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.appbar_background))
            elevation = 4f * dp
        }

        val btnClose = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((44 * dp).toInt(), (44 * dp).toInt())
            setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/back_arrow.svg", 24, iconTint))
            isClickable = true; isFocusable = true
            background = with(android.util.TypedValue()) {
                theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
                ContextCompat.getDrawable(ctx, resourceId)
            }
            setOnClickListener { finishWithAnim() }
        }

        val tabCountTv = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .also { it.marginStart = (4 * dp).toInt() }
            val count = TabManager.getTabs().size
            text = if (count == 1) "1 Aba" else "$count Abas"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        val btnNewTab = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((44 * dp).toInt(), (44 * dp).toInt())
            setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/add.svg", 24, iconTint))
            isClickable = true; isFocusable = true
            background = with(android.util.TypedValue()) {
                theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
                ContextCompat.getDrawable(ctx, resourceId)
            }
            setOnClickListener {
                TabManager.newTab()
                TabManager.save(ctx)
                openCurrentTab()
            }
        }

        toolbar.addView(btnClose)
        toolbar.addView(tabCountTv)
        toolbar.addView(btnNewTab)

        // ScrollView com stack
        scrollView = ScrollView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        stackContainer = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(
                (16 * dp).toInt(),
                (56 * dp + 16 * dp).toInt(), // abaixo da toolbar
                (16 * dp).toInt(),
                (80 * dp).toInt()
            )
        }

        buildTabStack(dp, blue, iconTint, iconSec)

        scrollView.addView(stackContainer)
        root.addView(scrollView)
        root.addView(toolbar)

        // Botão "Nova aba" fixo em baixo
        val btnNewTabBottom = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                (48 * dp).toInt(),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).also { it.bottomMargin = (20 * dp).toInt() }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24 * dp
                setColor(blue)
            }
            setPadding((24 * dp).toInt(), 0, (24 * dp).toInt(), 0)
            elevation = 8f * dp
            isClickable = true
            isFocusable = true
        }

        val newTabIcon = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((18 * dp).toInt(), (18 * dp).toInt())
                .also { it.marginEnd = (8 * dp).toInt() }
            setImageDrawable(svgDrawable("icons/svg/add.svg", 18, Color.WHITE))
        }

        val newTabTv = TextView(ctx).apply {
            text = "Nova aba"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
        }

        btnNewTabBottom.addView(newTabIcon)
        btnNewTabBottom.addView(newTabTv)
        btnNewTabBottom.setOnClickListener {
            TabManager.newTab()
            TabManager.save(ctx)
            openCurrentTab()
        }

        root.addView(btnNewTabBottom)
        setContentView(root)

        // Animação de entrada: cards entram de baixo
        animateCardsIn()
    }

    private fun buildTabStack(dp: Float, blue: Int, iconTint: Int, iconSec: Int) {
        val ctx = this
        stackContainer.removeAllViews()
        tabCards.clear()

        val tabs = TabManager.getTabs()
        val currentId = TabManager.getCurrentId()

        tabs.forEachIndexed { index, tab ->
            val cardHeight = (200 * dp).toInt()
            val isActive = tab.id == currentId

            val card = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    cardHeight
                ).also { it.bottomMargin = (12 * dp).toInt() }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 16 * dp
                    setColor(ContextCompat.getColor(ctx, R.color.card_background))
                    if (isActive) setStroke((2 * dp).toInt(), blue)
                }
                clipToOutline = true
                elevation = if (isActive) (8 * dp) else (3 * dp)
            }

            // Preview screenshot
            val previewIv = ImageView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(ContextCompat.getColor(ctx, R.color.card_background))
            }
            val bmp = TabScreenshots.get(ctx, tab.id)
            if (bmp != null) previewIv.setImageBitmap(bmp)

            // Gradient escuro em baixo para legibilidade do título
            val gradientView = View(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    (80 * dp).toInt(),
                    Gravity.BOTTOM
                )
                background = buildBottomGradient()
            }

            // Barra do título (inferior)
            val titleBar = LinearLayout(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    (48 * dp).toInt(),
                    Gravity.BOTTOM
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((12 * dp).toInt(), 0, (8 * dp).toInt(), 0)
            }

            val faviconIv = ImageView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams((18 * dp).toInt(), (18 * dp).toInt())
                    .also { it.marginEnd = (8 * dp).toInt() }
            }
            if (tab.url.isNotEmpty()) loadFavicon(tab.url, faviconIv)

            val titleTv = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = tab.title.ifEmpty { tab.url }
                textSize = 12f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(if (bmp != null) Color.WHITE else ContextCompat.getColor(ctx, R.color.text_primary))
            }

            titleBar.addView(faviconIv)
            titleBar.addView(titleTv)

            // Badge activa
            if (isActive) {
                val activeDot = View(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        (8 * dp).toInt(), (8 * dp).toInt(), Gravity.TOP or Gravity.START
                    ).also { it.topMargin = (12 * dp).toInt(); it.marginStart = (12 * dp).toInt() }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(blue)
                    }
                }
                card.addView(activeDot)
            }

            // Botão fechar
            val closeBtn = ImageView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(
                    (36 * dp).toInt(), (36 * dp).toInt(),
                    Gravity.TOP or Gravity.END
                ).also { it.topMargin = (8 * dp).toInt(); it.marginEnd = (8 * dp).toInt() }
                setPadding((6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt(), (6 * dp).toInt())
                setImageDrawable(svgDrawable("icons/svg/close.svg", 16,
                    if (bmp != null) Color.WHITE else iconTint))
                isClickable = true; isFocusable = true
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.argb(120, 0, 0, 0))
                }
                setOnClickListener {
                    val pos = tabCards.indexOf(card)
                    if (pos >= 0) {
                        TabScreenshots.remove(ctx, tab.id)
                        TabManager.closeTab(tab.id)
                        TabManager.save(ctx)
                        card.animate().alpha(0f).translationX(card.width.toFloat())
                            .setDuration(220).withEndAction {
                                stackContainer.removeView(card)
                                tabCards.removeAt(pos)
                                if (TabManager.count() == 0) finish()
                            }.start()
                    }
                }
            }

            card.addView(previewIv)
            if (bmp != null) card.addView(gradientView)
            card.addView(titleBar)
            card.addView(closeBtn)

            card.setOnClickListener {
                card.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction {
                    card.animate().scaleX(1f).scaleY(1f).setDuration(80).withEndAction {
                        TabManager.setCurrentId(tab.id)
                        TabManager.save(ctx)
                        openCurrentTab()
                    }.start()
                }.start()
            }

            // Alpha leve para os não-activos — dá sensação de pilha
            card.alpha = if (isActive) 1f else 0.9f

            stackContainer.addView(card)
            tabCards.add(card)
        }
    }

    private fun buildBottomGradient(): android.graphics.drawable.Drawable {
        return object : android.graphics.drawable.Drawable() {
            private val paint = Paint()
            override fun draw(canvas: Canvas) {
                val shader = LinearGradient(
                    0f, 0f, 0f, bounds.height().toFloat(),
                    Color.TRANSPARENT, Color.argb(180, 0, 0, 0),
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(bounds, paint)
            }
            override fun setAlpha(alpha: Int) {}
            override fun setColorFilter(cf: android.graphics.ColorFilter?) {}
            @Deprecated("Deprecated in Java")
            override fun getOpacity() = android.graphics.PixelFormat.TRANSLUCENT
        }
    }

    private fun animateCardsIn() {
        tabCards.forEachIndexed { i, card ->
            card.translationY = 120f
            card.alpha = 0f
            card.animate()
                .translationY(0f)
                .alpha(if (TabManager.getTabs().getOrNull(i)?.id == TabManager.getCurrentId()) 1f else 0.9f)
                .setStartDelay((i * 40).toLong())
                .setDuration(320)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun loadFavicon(url: String, iv: ImageView) {
        val host = runCatching { android.net.Uri.parse(url).host ?: "" }.getOrDefault("")
        if (host.isEmpty()) return
        Thread {
            runCatching {
                val faviconUrl = "https://www.google.com/s2/favicons?domain=$host&sz=32"
                val conn = java.net.URL(faviconUrl).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 2000; conn.readTimeout = 2000
                val bmp = android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                conn.disconnect()
                if (bmp != null) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        iv.setImageBitmap(bmp)
                    }
                }
            }
        }.start()
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

    private fun finishWithAnim() {
        finish()
    }

    private fun applyTheme() {
        val isLight = !resources.configuration.isNightModeActive
        insetsController.isAppearanceLightStatusBars = isLight
        window.statusBarColor = ContextCompat.getColor(this, R.color.appbar_background)
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