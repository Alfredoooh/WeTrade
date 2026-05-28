package com.wilin.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
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
        val isLight = !resources.configuration.isNightModeActive
        insetsController.isAppearanceLightStatusBars = isLight

        TabManager.init(this)
        TabScreenshots.init(this)

        val dp      = resources.displayMetrics.density
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = resources.displayMetrics.heightPixels.toFloat()
        val blue    = ContextCompat.getColor(this, R.color.colorPrimary)
        val iconSec = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        // Cards com a mesma escala que a Main quando encolhe (0.84)
        val mainScale = 0.84f
        val cardW     = (screenW * mainScale).toInt()
        val cardH     = (screenH * mainScale).toInt()
        val cardGap   = (16 * dp).toInt()

        // Padding lateral para centrar o primeiro/último card
        val sidePad = ((screenW - cardW) / 2f).toInt()

        val tabs      = TabManager.getTabs()
        val currentId = TabManager.getCurrentId()
        val activeIdx = tabs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)

        // ── Root ─────────────────────────────────────────────────────────────
        val root = FrameLayout(this)
        root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val dimBg = View(this)
        dimBg.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        dimBg.setBackgroundColor(Color.parseColor("#D91C1C1E"))
        dimBg.alpha = 0f

        // ── HorizontalScrollView centrado verticalmente ──────────────────────
        val hScrollLp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            cardH + (40 * dp).toInt(), // altura = card + espaço label
            Gravity.CENTER_VERTICAL
        )
        val hScroll = HorizontalScrollView(this)
        hScroll.layoutParams = hScrollLp
        hScroll.isHorizontalScrollBarEnabled = false
        hScroll.isFillViewport = false
        hScroll.overScrollMode = View.OVER_SCROLL_NEVER

        val track = LinearLayout(this)
        track.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        track.orientation = LinearLayout.HORIZONTAL
        track.gravity     = Gravity.CENTER_VERTICAL
        // Padding lateral para centrar o primeiro e o último card
        track.setPadding(sidePad, 0, sidePad, 0)

        hScroll.addView(track)

        // ── Cards ────────────────────────────────────────────────────────────
        tabs.forEachIndexed { idx, tab ->
            val isActive = tab.id == currentId
            val preview  = TabScreenshots.get(this, tab.id)
            val card = buildCard(
                tab      = tab,
                isActive = isActive,
                preview  = preview,
                cardW    = cardW,
                cardH    = cardH,
                cardGap  = cardGap,
                dp       = dp,
                blue     = blue,
                iconSec  = iconSec,
                dimBg    = dimBg,
                hScroll  = hScroll,
                screenW  = screenW,
                screenH  = screenH
            )
            track.addView(card)
        }

        // ── Botão Fechar ─────────────────────────────────────────────────────
        val btnCloseLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            (44 * dp).toInt(),
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )
        btnCloseLp.bottomMargin = (36 * dp).toInt()

        val btnClose = TextView(this)
        btnClose.layoutParams = btnCloseLp
        btnClose.text = "Fechar"
        btnClose.textSize = 14f
        btnClose.setTypeface(btnClose.typeface, android.graphics.Typeface.BOLD)
        btnClose.setTextColor(Color.WHITE)
        btnClose.gravity = Gravity.CENTER
        btnClose.setPadding((28 * dp).toInt(), 0, (28 * dp).toInt(), 0)
        val closeBg = GradientDrawable()
        closeBg.shape = GradientDrawable.RECTANGLE
        closeBg.cornerRadius = 22 * dp
        closeBg.setColor(Color.argb(160, 60, 60, 70))
        btnClose.background = closeBg
        btnClose.isClickable = true
        btnClose.isFocusable = true
        btnClose.alpha = 0f
        btnClose.setOnClickListener { closeWithAnimation(dimBg) }

        // ── Botão Nova Aba ───────────────────────────────────────────────────
        val btnNewLp = FrameLayout.LayoutParams(
            (52 * dp).toInt(),
            (52 * dp).toInt(),
            Gravity.BOTTOM or Gravity.END
        )
        btnNewLp.bottomMargin = (36 * dp).toInt()
        btnNewLp.marginEnd    = (24 * dp).toInt()

        val btnNew = FrameLayout(this)
        btnNew.layoutParams = btnNewLp
        val btnNewBg = GradientDrawable()
        btnNewBg.shape = GradientDrawable.OVAL
        btnNewBg.setColor(blue)
        btnNew.background = btnNewBg
        btnNew.elevation = 10f * dp
        btnNew.isClickable = true
        btnNew.isFocusable = true
        btnNew.alpha = 0f
        btnNew.setOnClickListener { newTabAndOpen() }

        val plusIv = ImageView(this)
        plusIv.layoutParams = FrameLayout.LayoutParams(
            (22 * dp).toInt(), (22 * dp).toInt(), Gravity.CENTER
        )
        plusIv.setImageDrawable(svgDrawable("icons/svg/add.svg", 22, Color.WHITE))
        btnNew.addView(plusIv)

        root.addView(dimBg)
        root.addView(hScroll)
        root.addView(btnClose)
        root.addView(btnNew)
        setContentView(root)

        // ── Scroll para centrar o card activo ────────────────────────────────
        hScroll.post {
            // Posição do card activo no track (com sidePad de início)
            val cardStart = sidePad + activeIdx * (cardW + cardGap)
            // Para centrar: queremos que o centro do card fique no centro do ecrã
            val targetScrollX = (cardStart - (screenW - cardW) / 2f).toInt().coerceAtLeast(0)
            hScroll.scrollTo(targetScrollX, 0)
        }

        // Animações de entrada
        dimBg.animate().alpha(1f).setDuration(250)
            .setInterpolator(DecelerateInterpolator(1.5f)).start()
        btnClose.animate().alpha(1f).setStartDelay(100).setDuration(220).start()
        btnNew.animate().alpha(1f).setStartDelay(100).setDuration(220).start()
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
        dimBg: View,
        hScroll: HorizontalScrollView,
        screenW: Float,
        screenH: Float
    ): FrameLayout {
        val ctx = this

        val cardLp = LinearLayout.LayoutParams(cardW, cardH)
        cardLp.marginEnd = cardGap

        val card = FrameLayout(ctx)
        card.layoutParams = cardLp

        val cardBg = GradientDrawable()
        cardBg.shape        = GradientDrawable.RECTANGLE
        cardBg.cornerRadius = 20 * dp
        cardBg.setColor(ContextCompat.getColor(ctx, R.color.card_background))
        if (isActive) cardBg.setStroke((2 * dp).toInt(), blue)
        card.background   = cardBg
        card.clipToOutline = true
        card.elevation     = if (isActive) 18f * dp else 5f * dp

        // Animação de entrada
        card.alpha      = 0f
        card.scaleX     = 0.90f
        card.scaleY     = 0.90f
        card.translationY = 60f * dp

        // Preview do WebView
        val previewLp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        previewLp.topMargin = (50 * dp).toInt()
        val previewIv = ImageView(ctx)
        previewIv.layoutParams = previewLp
        previewIv.scaleType    = ImageView.ScaleType.CENTER_CROP
        previewIv.setBackgroundColor(ContextCompat.getColor(ctx, R.color.background))
        if (preview != null) previewIv.setImageBitmap(preview)

        // Header do card
        val headerLp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            (50 * dp).toInt(),
            Gravity.TOP
        )
        val header = LinearLayout(ctx)
        header.layoutParams = headerLp
        header.orientation  = LinearLayout.HORIZONTAL
        header.gravity      = Gravity.CENTER_VERTICAL
        header.setPadding((14 * dp).toInt(), 0, (10 * dp).toInt(), 0)
        header.setBackgroundColor(ContextCompat.getColor(ctx, R.color.card_background))

        val titleTv = TextView(ctx)
        titleTv.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        titleTv.text      = tab.title.ifEmpty { if (tab.url.isEmpty()) "Nova aba" else tab.url }
        titleTv.textSize  = 13f
        titleTv.setTypeface(titleTv.typeface, android.graphics.Typeface.BOLD)
        titleTv.maxLines  = 1
        titleTv.ellipsize = android.text.TextUtils.TruncateAt.END
        titleTv.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))

        val closeIvSz = (36 * dp).toInt()
        val closeIv   = ImageView(ctx)
        closeIv.layoutParams = LinearLayout.LayoutParams(closeIvSz, closeIvSz)
        closeIv.setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt())
        closeIv.setImageDrawable(svgDrawable("icons/svg/close.svg", 16, iconSec))
        closeIv.isClickable = true
        closeIv.isFocusable = true
        closeIv.setOnClickListener {
            card.animate()
                .translationY(card.height.toFloat() + 40f)
                .scaleX(0.80f).scaleY(0.80f)
                .alpha(0f)
                .setDuration(220)
                .setInterpolator(DecelerateInterpolator(2f))
                .withEndAction {
                    val parent = card.parent as? ViewGroup
                    parent?.removeView(card)
                    TabScreenshots.remove(ctx, tab.id)
                    TabManager.closeTab(tab.id)
                    TabManager.save(ctx)
                    if (TabManager.count() == 0) newTabAndOpen()
                }.start()
        }

        header.addView(titleTv)
        header.addView(closeIv)
        card.addView(previewIv)
        card.addView(header)

        // Animação de entrada escalonada
        card.post {
            card.animate()
                .alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setDuration(360)
                .setInterpolator(OvershootInterpolator(0.65f))
                .start()
        }

        // ── Click: zoom-in container (cresce até preencher ecrã) ────────────
        card.setOnClickListener {
            // Obtém a posição absoluta do card no ecrã
            val loc = IntArray(2)
            card.getLocationOnScreen(loc)

            val scaleX = screenW / card.width.toFloat()
            val scaleY = screenH / card.height.toFloat()

            // Pivot no topo-esquerdo do card para que o zoom preencha o ecrã inteiro
            card.pivotX = 0f
            card.pivotY = 0f

            // Fade do fundo ao mesmo tempo
            dimBg.animate()
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator(2.5f))
                .start()

            card.animate()
                .scaleX(scaleX)
                .scaleY(scaleY)
                .translationX(-loc[0].toFloat())
                .translationY(-loc[1].toFloat())
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator(2.5f))
                .withEndAction {
                    TabManager.switchToTab(ctx, tab.id)
                    finish()
                    overridePendingTransition(0, 0)
                }.start()
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
        val intent = Intent(this, BrowserResponseActivity::class.java)
        intent.putExtra(BrowserResponseActivity.EXTRA_TAB_ID, TabManager.getCurrentId())
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
        overridePendingTransition(0, 0)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }

    private fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
        val px  = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, android.graphics.Bitmap.Config.ARGB_8888)
        try {
            val svg = SVG.getFromAsset(assets, path)
            svg.documentWidth  = px.toFloat()
            svg.documentHeight = px.toFloat()
            svg.renderToCanvas(Canvas(bmp))
        } catch (_: Exception) {}
        return BitmapDrawable(resources, bmp).also {
            it.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
        }
    }
}