package com.wilin.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
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

class TabsActivity : AppCompatActivity() {

    private lateinit var insetsController: WindowInsetsControllerCompat

    companion object {
        const val EXTRA_TRANSITION_SCREENSHOT_PATH = "transition_screenshot_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        insetsController = WindowInsetsControllerCompat(window, window.decorView)
        applyTheme()

        TabManager.init(this)
        TabScreenshots.init(this)

        val dp      = resources.displayMetrics.density
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = resources.displayMetrics.heightPixels.toFloat()

        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)
        val iconSec  = ContextCompat.getColor(this, R.color.icon_tint_secondary)
        val blue     = ContextCompat.getColor(this, R.color.colorPrimary)

        // Dimensões dos cards
        val cardW   = (screenW * 0.72f).toInt()
        val cardH   = (screenH * 0.66f).toInt()
        val cardGap = (20 * dp).toInt()
        val sidePad = ((screenW - cardW) / 2f).toInt()

        // ── Root translúcido com fundo escuro ────────────────────────────────
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        // Fundo escuro semitransparente (fade-in ao abrir)
        val dimBg = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#CC1C1C1E"))
            alpha = 0f
        }

        // ── HorizontalScrollView com cards ──────────────────────────────────
        val hScroll = HorizontalScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
        }

        val track = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            setPadding(sidePad, 0, sidePad, 0)
        }

        hScroll.addView(track)

        // ── Botão "Nova aba" no fundo ao centro ──────────────────────────────
        val btnNew = FrameLayout(this).apply {
            val sz = (52 * dp).toInt()
            layoutParams = FrameLayout.LayoutParams(sz, sz,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).also { it.bottomMargin = (36 * dp).toInt() }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(blue)
            }
            elevation = 10f * dp
            isClickable = true; isFocusable = true
            setOnClickListener { newTabAndOpen() }
        }
        btnNew.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (22 * dp).toInt(), (22 * dp).toInt(), Gravity.CENTER
            )
            setImageDrawable(svgDrawable("icons/svg/add.svg", 22, Color.WHITE))
        })

        // ── Botão "Fechar" (fecha o switcher e volta à MainActivity) ─────────
        val btnClose = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (42 * dp).toInt(),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).also { it.bottomMargin = (36 * dp + 52 * dp + 12 * dp).toInt() }
            text = "Fechar"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding((24 * dp).toInt(), 0, (24 * dp).toInt(), 0)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 21 * dp
                setColor(Color.argb(160, 80, 80, 90))
            }
            isClickable = true; isFocusable = true
            setOnClickListener { closeWithAnimation() }
        }

        root.addView(dimBg)
        root.addView(hScroll)
        root.addView(btnClose)
        root.addView(btnNew)
        setContentView(root)

        // ── Construir cards ──────────────────────────────────────────────────
        val tabs      = TabManager.getTabs()
        val currentId = TabManager.getCurrentId()

        tabs.forEach { tab ->
            val isActive = tab.id == currentId
            val preview  = TabScreenshots.get(this, tab.id)
            val card     = buildCard(tab, isActive, preview, cardW, cardH, cardGap, dp, blue, iconSec)
            track.addView(card)
        }

        // Scroll para o card activo centrado
        hScroll.post {
            val activeIdx = tabs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
            val targetX   = (activeIdx * (cardW + cardGap) - (screenW - cardW) / 2f)
                .coerceAtLeast(0f).toInt()
            hScroll.scrollTo(targetX, 0)
        }

        // Fade-in do fundo escuro
        dimBg.animate()
            .alpha(1f)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        // Toque no fundo fecha
        dimBg.setOnClickListener { closeWithAnimation() }
    }

    private fun buildCard(
        tab: BrowserTab,
        isActive: Boolean,
        preview: Bitmap?,
        cardW: Int,
        cardH: Int,
        cardGap: Int,
        dp: Float,
        blue: Int,
        iconSec: Int
    ): FrameLayout {
        val ctx = this

        val card = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(cardW, cardH).also {
                it.marginEnd = cardGap
            }
            background = GradientDrawable().apply {
                shape        = GradientDrawable.RECTANGLE
                cornerRadius = 20 * dp
                setColor(ContextCompat.getColor(ctx, R.color.card_background))
                if (isActive) setStroke((2 * dp).toInt(), blue)
            }
            clipToOutline = true
            elevation = if (isActive) 14f * dp else 5f * dp
            // Entrada de baixo
            alpha    = 0f
            scaleX   = 0.88f
            scaleY   = 0.88f
            translationY = 64f * dp
        }

        // Preview
        val previewIv = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).also { it.topMargin = (52 * dp).toInt() }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.background))
        }
        preview?.let { previewIv.setImageBitmap(it) }

        // Header
        val header = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (52 * dp).toInt(),
                Gravity.TOP
            )
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            setPadding((14 * dp).toInt(), 0, (10 * dp).toInt(), 0)
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.card_background))
        }

        val titleTv = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            text = tab.title.ifEmpty { if (tab.url.isEmpty()) "Nova aba" else tab.url }
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        val closeIv = ImageView(ctx).apply {
            val sz = (34 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz)
            setPadding((7 * dp).toInt(), (7 * dp).toInt(), (7 * dp).toInt(), (7 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/close.svg", 16, iconSec))
            isClickable = true; isFocusable = true
            setOnClickListener {
                card.animate()
                    .translationY(card.height.toFloat() + 40f)
                    .scaleX(0.82f).scaleY(0.82f)
                    .alpha(0f)
                    .setDuration(220)
                    .setInterpolator(DecelerateInterpolator(2f))
                    .withEndAction {
                        (card.parent as? ViewGroup)?.removeView(card)
                        TabScreenshots.remove(ctx, tab.id)
                        TabManager.closeTab(tab.id)
                        TabManager.save(ctx)
                        if (TabManager.count() == 0) newTabAndOpen()
                    }.start()
            }
        }

        header.addView(titleTv)
        header.addView(closeIv)
        card.addView(previewIv)
        card.addView(header)

        // Animação de entrada
        card.post {
            card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(380)
                .setInterpolator(OvershootInterpolator(0.65f))
                .start()
        }

        // Clique: expandir até cobrir o ecrã → fechar e activar o tab
        card.setOnClickListener {
            val loc = IntArray(2)
            card.getLocationOnScreen(loc)
            val sw = resources.displayMetrics.widthPixels.toFloat()
            val sh = resources.displayMetrics.heightPixels.toFloat()
            val sx = sw / card.width.toFloat()
            val sy = sh / card.height.toFloat()

            card.pivotX = 0f
            card.pivotY = 0f
            card.animate()
                .scaleX(sx).scaleY(sy)
                .translationX(-loc[0].toFloat())
                .translationY(-loc[1].toFloat())
                .alpha(0.55f)
                .setDuration(280)
                .setInterpolator(DecelerateInterpolator(2.5f))
                .withEndAction {
                    TabManager.switchToTab(ctx, tab.id)
                    finish()
                    overridePendingTransition(0, 0)
                }
                .start()
        }

        return card
    }

    private fun closeWithAnimation() {
        // Fade-out do fundo e fecha
        val dimBg = (contentView() as? FrameLayout)?.getChildAt(0)
        dimBg?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.setInterpolator(DecelerateInterpolator(1.5f))
            ?.start()
        finish()
        overridePendingTransition(0, 0)
    }

    private fun newTabAndOpen() {
        TabManager.newTab()
        TabManager.save(this)
        startActivity(Intent(this, BrowserResponseActivity::class.java).apply {
            putExtra(BrowserResponseActivity.EXTRA_TAB_ID, TabManager.getCurrentId())
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
        overridePendingTransition(0, 0)
    }

    private fun contentView(): View? =
        window.decorView.findViewById(android.R.id.content)

    private fun applyTheme() {
        val isLight = !resources.configuration.isNightModeActive
        insetsController.isAppearanceLightStatusBars = isLight
        window.statusBarColor = Color.TRANSPARENT
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
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