package com.wilin.app.ui

import android.content.Intent
import android.graphics.Bitmap
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

class TabsActivity : AppCompatActivity() {

    private lateinit var insetsController: WindowInsetsControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        insetsController = WindowInsetsControllerCompat(window, window.decorView)
        window.statusBarColor = Color.TRANSPARENT
        insetsController.isAppearanceLightStatusBars = !resources.configuration.isNightModeActive

        TabManager.init(this)
        TabScreenshots.init(this)

        val dp      = resources.displayMetrics.density
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = resources.displayMetrics.heightPixels.toFloat()
        val blue    = ContextCompat.getColor(this, R.color.colorPrimary)
        val iconSec = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        // A MainActivity está encolhida a 0.84 e subiu 4% da altura.
        // O card "activo" deve ter exactamente essas dimensões para alinhar com o ecrã de baixo.
        val mainScale   = 0.84f
        val cardW       = (screenW * mainScale).toInt()
        val cardH       = (screenH * mainScale).toInt()
        val cardGap     = (14 * dp).toInt()

        // O card activo fica alinhado ao centro horizontal do ecrã encolhido
        // (que está centrado, pois pivotX = width/2)
        // Os outros cards ficam à sua esquerda

        val tabs      = TabManager.getTabs()
        val currentId = TabManager.getCurrentId()
        val activeIdx = tabs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)

        // ── Root ─────────────────────────────────────────────────────────────
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Fundo escuro (fade-in)
        val dimBg = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#CC1C1C1E"))
            alpha = 0f
        }

        // ── HorizontalScrollView ─────────────────────────────────────────────
        val hScroll = HorizontalScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
            // Toque no fundo do scroll (não nos cards) fecha
            setOnClickListener { closeWithAnimation(dimBg) }
        }

        val track = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            // Padding esquerdo: espaço antes do primeiro card lateral
            // Padding direito: para que o card activo possa centrar no ecrã
            val rightPad = ((screenW - cardW) / 2f).toInt()
            val leftPad  = (screenW * 0.10f).toInt()
            setPadding(leftPad, 0, rightPad, 0)
        }

        hScroll.addView(track)

        // ── Construir cards ──────────────────────────────────────────────────
        // Ordem: todos os tabs por índice. O activo fica na posição correcta.
        // O scroll começa no card activo alinhado com o ecrã encolhido (à direita).
        tabs.forEachIndexed { idx, tab ->
            val isActive = tab.id == currentId
            val preview  = TabScreenshots.get(this, tab.id)
            val card     = buildCard(
                tab      = tab,
                isActive = isActive,
                preview  = preview,
                cardW    = cardW,
                cardH    = cardH,
                cardGap  = cardGap,
                dp       = dp,
                blue     = blue,
                iconSec  = iconSec,
                dimBg    = dimBg
            )
            track.addView(card)
        }

        // Botão fechar
        val btnClose = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                (44 * dp).toInt(),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).also { it.bottomMargin = (40 * dp).toInt() }
            text = "Fechar"
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding((28 * dp).toInt(), 0, (28 * dp).toInt(), 0)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 22 * dp
                setColor(Color.argb(150, 80, 80, 90))
            }
            isClickable = true; isFocusable = true
            alpha = 0f
            setOnClickListener { closeWithAnimation(dimBg) }
        }

        // Botão nova aba
        val btnNew = FrameLayout(this).apply {
            val sz = (52 * dp).toInt()
            layoutParams = FrameLayout.LayoutParams(sz, sz,
                Gravity.BOTTOM or Gravity.END
            ).also {
                it.bottomMargin = (40 * dp).toInt()
                it.marginEnd    = (24 * dp).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(blue)
            }
            elevation = 10f * dp
            isClickable = true; isFocusable = true
            alpha = 0f
            setOnClickListener { newTabAndOpen() }
        }
        btnNew.addView(ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (22 * dp).toInt(), (22 * dp).toInt(), Gravity.CENTER
            )
            setImageDrawable(svgDrawable("icons/svg/add.svg", 22, Color.WHITE))
        })

        root.addView(dimBg)
        root.addView(hScroll)
        root.addView(btnClose)
        root.addView(btnNew)
        setContentView(root)

        // ── Scroll para o card activo alinhado com o ecrã encolhido ──────────
        // O ecrã encolhido está centrado horizontalmente.
        // O card activo deve estar centrado no mesmo sítio.
        // scrollX = posição do início do card activo - margem esquerda para centrar
        hScroll.post {
            val trackPaddingLeft = (screenW * 0.10f).toInt()
            val rightPad         = ((screenW - cardW) / 2f).toInt()
            // posição X do início do card activo dentro do track (sem o padding esquerdo)
            val cardStartInTrack = trackPaddingLeft + activeIdx * (cardW + cardGap)
            // queremos que o centro do card activo coincida com o centro do ecrã
            val targetScrollX = cardStartInTrack - ((screenW - cardW) / 2f).toInt()
            hScroll.scrollTo(targetScrollX.coerceAtLeast(0), 0)
        }

        // Fade-in do fundo e botões
        dimBg.animate().alpha(1f).setDuration(260).setInterpolator(DecelerateInterpolator(1.5f)).start()
        btnClose.animate().alpha(1f).setStartDelay(120).setDuration(220).start()
        btnNew.animate().alpha(1f).setStartDelay(120).setDuration(220).start()
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
        iconSec: Int,
        dimBg: View
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
            elevation = if (isActive) 16f * dp else 5f * dp
            // Entrada de baixo com overshoot
            alpha        = 0f
            scaleX       = 0.88f
            scaleY       = 0.88f
            translationY = 70f * dp
        }

        // Preview
        val previewIv = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).also { it.topMargin = (50 * dp).toInt() }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.background))
        }
        preview?.let { previewIv.setImageBitmap(it) }

        // Header
        val header = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (50 * dp).toInt(),
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
            val sz = (36 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz)
            setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt())
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
                .alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setDuration(380)
                .setInterpolator(OvershootInterpolator(0.65f))
                .start()
        }

        // Clique: expande para cobrir o ecrã e fecha
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
                .alpha(0.5f)
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

    private fun closeWithAnimation(dimBg: View) {
        dimBg.animate()
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()
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
        return BitmapDrawable(resources, bmp).also { it.setColorFilter(tint, PorterDuff.Mode.SRC_IN) }
    }
