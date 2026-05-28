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

class TabsActivity : AppCompatActivity() {

    private lateinit var insetsController: WindowInsetsControllerCompat
    private lateinit var gridLayout: GridLayout
    private var editMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        insetsController = WindowInsetsControllerCompat(window, window.decorView)
        applyTheme()

        TabManager.init(this)
        TabScreenshots.init(this)

        val dp      = resources.displayMetrics.density
        val bgColor = ContextCompat.getColor(this, R.color.background)
        val blue    = ContextCompat.getColor(this, R.color.colorPrimary)
        val textPri = ContextCompat.getColor(this, R.color.text_primary)
        val textSec = ContextCompat.getColor(this, R.color.icon_tint_secondary)
        val isDark  = !resources.configuration.isNightModeActive.not()
            .also { /* just reading */ }
            .let { resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES }

        // ── Root ──────────────────────────────────────────────────────────────
        val root = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
        }

        // ── Top bar: Edit | [título] | Done ───────────────────────────────────
        val topBar = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (52 * dp).toInt())
            setBackgroundColor(bgColor)
        }

        val btnEdit = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.START).also {
                (it as FrameLayout.LayoutParams).marginStart = (16 * dp).toInt()
            }
            text = "Edit"; textSize = 17f
            setTextColor(blue)
            isClickable = true; isFocusable = true
            background = ContextCompat.getDrawable(this@TabsActivity, R.drawable.ripple_item)
            setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt())
        }

        val titleTv = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER)
            text = "${TabManager.count()} Tabs"; textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(textPri)
        }

        val btnDone = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL or Gravity.END).also {
                (it as FrameLayout.LayoutParams).marginEnd = (16 * dp).toInt()
            }
            text = "Done"; textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(blue)
            isClickable = true; isFocusable = true
            background = ContextCompat.getDrawable(this@TabsActivity, R.drawable.ripple_item)
            setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt())
        }

        topBar.addView(btnEdit); topBar.addView(titleTv); topBar.addView(btnDone)

        // Divider
        val dividerTop = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt())
            setBackgroundColor(ContextCompat.getColor(this@TabsActivity, R.color.divider))
        }

        // ── Selector tabs tipo Chrome (Normal | Incognito) ────────────────────
        val selectorBar = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (44 * dp).toInt())
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER
            setBackgroundColor(bgColor)
        }

        val cardRadius = 20 * dp
        val selectorBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cardRadius
            setColor(ContextCompat.getColor(this@TabsActivity, R.color.input_background))
        }

        val selectorPill = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams((220 * dp).toInt(), (32 * dp).toInt())
            background = selectorBg
        }

        val normalTabBtn = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT).also {
                it.width = (110 * dp).toInt()
                it.gravity = Gravity.START
            }
            text = "${TabManager.count()}"; textSize = 13f; gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(textPri)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE; cornerRadius = cardRadius
                setColor(bgColor)
            }
        }

        val incognitoBtn = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT).also {
                it.gravity = Gravity.END
                it.marginStart = (110 * dp).toInt()
            }
            text = "Incógnito"; textSize = 13f; gravity = Gravity.CENTER
            setTextColor(textSec)
        }

        selectorPill.addView(incognitoBtn); selectorPill.addView(normalTabBtn)
        selectorBar.addView(selectorPill)

        // ── ScrollView com Grid ───────────────────────────────────────────────
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val screenW  = resources.displayMetrics.widthPixels
        val cardW    = (screenW / 2) - (24 * dp).toInt() // 2 colunas com gap
        val cardH    = (cardW * 1.5f).toInt()             // proporção 2:3

        gridLayout = GridLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            columnCount = 2
            setPadding((12 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt(), (80 * dp).toInt())
        }

        scrollView.addView(gridLayout)

        // ── Bottom bar: [Edit] [+] [Done] ─────────────────────────────────────
        val bottomBar = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (60 * dp).toInt())
            setBackgroundColor(bgColor)
        }

        val dividerBottom = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt(), Gravity.TOP)
            setBackgroundColor(ContextCompat.getColor(this@TabsActivity, R.color.divider))
        }
        bottomBar.addView(dividerBottom)

        val btnNewTab = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams((48 * dp).toInt(), (48 * dp).toInt(), Gravity.CENTER)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL; setColor(blue)
            }
            isClickable = true; isFocusable = true
            elevation = 6 * dp
        }
        val plusIv = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams((22 * dp).toInt(), (22 * dp).toInt(), Gravity.CENTER)
            setImageDrawable(svgDrawable("icons/svg/add.svg", 22, Color.WHITE))
        }
        btnNewTab.addView(plusIv)
        btnNewTab.setOnClickListener { newTabAndOpen() }

        bottomBar.addView(btnNewTab)

        // ── Monta a view ─────────────────────────────────────────────────────
        root.addView(topBar)
        root.addView(dividerTop)
        root.addView(selectorBar)
        root.addView(scrollView)
        root.addView(bottomBar)
        setContentView(root)

        // ── Preenche o grid ──────────────────────────────────────────────────
        renderGrid(cardW, cardH, dp)

        // ── Listeners ────────────────────────────────────────────────────────
        btnDone.setOnClickListener { finishWithAnimation() }
        btnEdit.setOnClickListener { toggleEditMode() }
    }

    // ── Renderiza o grid de tabs ──────────────────────────────────────────────

    private fun renderGrid(cardW: Int, cardH: Int, dp: Float) {
        gridLayout.removeAllViews()
        val tabs      = TabManager.getTabs()
        val currentId = TabManager.getCurrentId()
        val blue      = ContextCompat.getColor(this, R.color.colorPrimary)
        val bgCard    = ContextCompat.getColor(this, R.color.card_background)
        val textPri   = ContextCompat.getColor(this, R.color.text_primary)
        val textSec   = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        tabs.forEachIndexed { idx, tab ->
            val isActive = tab.id == currentId
            val cardFrame = buildTabCard(tab, isActive, cardW, cardH, dp, blue, bgCard, textPri, textSec)

            val lp = GridLayout.LayoutParams().apply {
                width  = cardW
                height = cardH + (40 * dp).toInt() // header
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
        cardW: Int, cardH: Int, dp: Float,
        blue: Int, bgCard: Int, textPri: Int, textSec: Int
    ): FrameLayout {
        val ctx = this

        val card = FrameLayout(ctx).apply {
            clipToOutline = true
            background = GradientDrawable().apply {
                shape        = GradientDrawable.RECTANGLE
                cornerRadius = 14 * dp
                setColor(bgCard)
                // Borda azul no tab ativo — estilo Chrome
                if (isActive) setStroke((3 * dp).toInt(), blue)
                else setStroke((1 * dp).toInt(), Color.argb(30, 128, 128, 128))
            }
            elevation     = if (isActive) 8 * dp else 2 * dp
            alpha         = 0f
            scaleX        = 0.92f
            scaleY        = 0.92f
            isClickable   = true
            isFocusable   = true
        }

        // Header do card (título + fechar)
        val header = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (38 * dp).toInt(), Gravity.TOP)
            orientation  = LinearLayout.HORIZONTAL
            gravity      = Gravity.CENTER_VERTICAL
            setPadding((10 * dp).toInt(), 0, (4 * dp).toInt(), 0)
            setBackgroundColor(bgCard)
        }

        val faviconIv = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((16 * dp).toInt(), (16 * dp).toInt()).also { it.marginEnd = (6 * dp).toInt() }
        }

        val titleTv = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            text      = tab.title.ifEmpty { if (tab.url.isEmpty()) "Nova aba" else tab.url }
            textSize  = 12f
            maxLines  = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(textPri)
        }

        val closeBtnSz = (32 * dp).toInt()
        val closeBtn = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(closeBtnSz, closeBtnSz)
            setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/close.svg", 14, textSec))
            isClickable = true; isFocusable = true
            background  = ContextCompat.getDrawable(ctx, R.drawable.ripple_circle)
        }

        header.addView(faviconIv); header.addView(titleTv); header.addView(closeBtn)

        // Preview screenshot — cortado na parte inferior para encaixar no card
        val previewLp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).also {
            it.topMargin = (38 * dp).toInt()
        }
        val previewIv = ImageView(ctx).apply {
            layoutParams = previewLp
            scaleType    = ImageView.ScaleType.CENTER_CROP // corta de forma inteligente, mostra o topo da página
        }

        val preview = TabScreenshots.get(this, tab.id)
        if (preview != null) {
            // Corta o bitmap para mostrar apenas a parte de cima (sem área vazia em baixo)
            val cropH = (preview.width * (cardH.toFloat() / cardW.toFloat())).toInt().coerceAtMost(preview.height)
            val cropped = if (cropH < preview.height) {
                Bitmap.createBitmap(preview, 0, 0, preview.width, cropH)
            } else preview
            previewIv.setImageBitmap(cropped)
        } else {
            previewIv.setBackgroundColor(ContextCompat.getColor(this, R.color.input_background))
        }

        // Favicon
        if (tab.favicon.isNotEmpty()) {
            Thread {
                runCatching {
                    val host = android.net.Uri.parse(tab.url).host ?: return@runCatching
                    val conn = java.net.URL("https://www.google.com/s2/favicons?domain=$host&sz=16").openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 2000; conn.readTimeout = 2000
                    val bmp = android.graphics.BitmapFactory.decodeStream(conn.inputStream)
                    conn.disconnect()
                    if (bmp != null) runOnUiThread { faviconIv.setImageBitmap(bmp) }
                }
            }.start()
        }

        card.addView(previewIv)
        card.addView(header)

        // Entrada animada
        card.post {
            card.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(300).setInterpolator(DecelerateInterpolator(2f)).start()
        }

        // Click no card — abre o tab
        card.setOnClickListener {
            // Highlight de click
            card.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction {
                card.animate().scaleX(1f).scaleY(1f).setDuration(80).withEndAction {
                    openTab(tab.id)
                }.start()
            }.start()
        }

        // Fechar tab
        closeBtn.setOnClickListener {
            card.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(180)
                .setInterpolator(DecelerateInterpolator(2f))
                .withEndAction {
                    TabScreenshots.remove(this, tab.id)
                    TabManager.closeTab(tab.id)
                    TabManager.save(this)
                    if (TabManager.count() == 0) newTabAndOpen()
                    else {
                        val sw = resources.displayMetrics.widthPixels
                        val cw = (sw / 2) - (24 * dp).toInt()
                        val ch = (cw * 1.5f).toInt()
                        renderGrid(cw, ch, dp)
                    }
                }.start()
        }

        return card
    }

    private fun toggleEditMode() {
        editMode = !editMode
        // Em edit mode podia mostrar checkmarks para selecionar — por agora reservado
    }

    private fun openTab(tabId: String) {
        TabManager.switchToTab(this, tabId)
        val intent = Intent(this, BrowserResponseActivity::class.java).apply {
            putExtra(BrowserResponseActivity.EXTRA_TAB_ID, tabId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun newTabAndOpen() {
        val tab = TabManager.newTab()
        TabManager.save(this)
        val intent = Intent(this, BrowserResponseActivity::class.java).apply {
            putExtra(BrowserResponseActivity.EXTRA_TAB_ID, tab.id)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun finishWithAnimation() {
        finish()
        overridePendingTransition(0, android.R.anim.fade_out)
    }

    override fun finish() { super.finish(); overridePendingTransition(0, 0) }

    private fun applyTheme() {
        val isLight = !resources.configuration.uiMode.let {
            it and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        window.statusBarColor = ContextCompat.getColor(this, R.color.background)
        insetsController.isAppearanceLightStatusBars = isLight
    }

    private fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
        val px  = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        try {
            SVG.getFromAsset(assets, path).apply {
                documentWidth = px.toFloat(); documentHeight = px.toFloat()
                renderToCanvas(Canvas(bmp))
            }
        } catch (_: Exception) {}
        return BitmapDrawable(resources, bmp).also { it.setColorFilter(tint, PorterDuff.Mode.SRC_IN) }
    }
}