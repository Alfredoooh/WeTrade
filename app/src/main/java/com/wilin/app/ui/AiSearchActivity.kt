// AiSearchActivity.kt
package com.wilin.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.caverock.androidsvg.SVG
import com.wilin.app.R

class AiSearchActivity : AppCompatActivity() {

    private lateinit var insetsController: WindowInsetsControllerCompat
    private var drawerOpen = false

    // Views que precisamos referenciar depois do onCreate
    private lateinit var drawerOverlay: FrameLayout
    private lateinit var drawerPanel:   LinearLayout
    private lateinit var btnSidePanel:  ImageView
    private lateinit var inputField:    EditText
    private lateinit var btnSend:       ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val dp        = resources.displayMetrics.density
        val iconTint  = ContextCompat.getColor(this, R.color.icon_tint)
        val iconSec   = ContextCompat.getColor(this, R.color.icon_tint_secondary)
        val blue      = ContextCompat.getColor(this, R.color.colorPrimary)
        val bgColor   = ContextCompat.getColor(this, R.color.background)
        val appbarBg  = ContextCompat.getColor(this, R.color.appbar_background)
        val inputBg   = ContextCompat.getColor(this, R.color.input_background)
        val textPrim  = ContextCompat.getColor(this, R.color.text_primary)
        val divColor  = ContextCompat.getColor(this, R.color.divider)

        insetsController = WindowInsetsControllerCompat(window, window.decorView)
        applyStatusBarTheme()

        // ── Root FrameLayout (permite drawer por cima) ────────────────────────
        val rootFrame = FrameLayout(this).apply {
            setBackgroundColor(bgColor)
        }

        // ── Conteúdo principal (vertical) ─────────────────────────────────────
        val mainContent = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
        }

        // ── AppBar ────────────────────────────────────────────────────────────
        val toolbar = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (56 * dp).toInt()
            )
            setBackgroundColor(appbarBg)
        }

        // Botão side_panel (esquerda)
        btnSidePanel = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (44 * dp).toInt(), (44 * dp).toInt(), Gravity.CENTER_VERTICAL or Gravity.START
            ).also { it.marginStart = (4 * dp).toInt() }
            val p = (10 * dp).toInt()
            setPadding(p, p, p, p)
            setImageDrawable(svgDrawable("icons/svg/side_panel.svg", 24, iconTint))
            isClickable = true; isFocusable = true
            background = with(android.util.TypedValue()) {
                theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
                ContextCompat.getDrawable(this@AiSearchActivity, resourceId)
            }
            setOnClickListener { toggleDrawer() }
        }

        // Título centrado
        val titleContainer = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
        }

        val aiIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (18 * dp).toInt(), (18 * dp).toInt()
            ).also { it.marginEnd = (6 * dp).toInt() }
            setImageDrawable(svgDrawable("icons/svg/ai.svg", 18, blue))
        }

        val titleTv = TextView(this).apply {
            text = "Clara"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(textPrim)
        }

        titleContainer.addView(aiIcon)
        titleContainer.addView(titleTv)

        // Botão more (direita)
        val btnMore = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (44 * dp).toInt(), (44 * dp).toInt(), Gravity.CENTER_VERTICAL or Gravity.END
            ).also { it.marginEnd = (4 * dp).toInt() }
            val p = (10 * dp).toInt()
            setPadding(p, p, p, p)
            setImageDrawable(svgDrawable("icons/svg/more_vertical.svg", 24, iconTint))
            isClickable = true; isFocusable = true
            background = with(android.util.TypedValue()) {
                theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
                ContextCompat.getDrawable(this@AiSearchActivity, resourceId)
            }
            setOnClickListener { v -> showMorePopup(v) }
        }

        toolbar.addView(btnSidePanel)
        toolbar.addView(titleContainer)
        toolbar.addView(btnMore)

        // Divider
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(divColor)
        }

        // ── Área central (placeholder) ────────────────────────────────────────
        val centerArea = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
        }

        val aiIconBig = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                (64 * dp).toInt(), (64 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/ai.svg", 64, blue))
            alpha = 0.12f
        }

        val subtitle = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (14 * dp).toInt() }
            text = "Como posso ajudar?"
            textSize = 15f
            setTextColor(ContextCompat.getColor(this@AiSearchActivity, R.color.text_secondary))
        }

        centerArea.addView(aiIconBig)
        centerArea.addView(subtitle)

        // ── Bottom input ──────────────────────────────────────────────────────
        val inputBar = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(appbarBg)
        }

        val inputBarTop = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(divColor)
        }

        val inputRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            val h = (12 * dp).toInt()
            val v = (10 * dp).toInt()
            setPadding(h, v, h, v)
        }

        // Input retangular com bordas arredondadas (estilo da imagem)
        val inputWrapper = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginEnd = (8 * dp).toInt()
            }
            background  = ContextCompat.getDrawable(this@AiSearchActivity, R.drawable.search_input_bg)
            val hPad = (14 * dp).toInt()
            val vPad = (12 * dp).toInt()
            setPadding(hPad, vPad, hPad, vPad)
        }

        inputField = EditText(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            hint       = "Escreve uma mensagem…"
            setHintTextColor(ContextCompat.getColor(this@AiSearchActivity, R.color.text_hint))
            setTextColor(textPrim)
            textSize   = 15f
            background = null
            minLines   = 1
            maxLines   = 4
            imeOptions = EditorInfo.IME_ACTION_SEND
            inputType  = android.text.InputType.TYPE_CLASS_TEXT or
                         android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                         android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        inputWrapper.addView(inputField)

        // Botão enviar (circular, ativo só com texto)
        btnSend = ImageView(this).apply {
            val sz = (42 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz)
            val p = (9 * dp).toInt()
            setPadding(p, p, p, p)
            setImageDrawable(svgDrawable("icons/svg/arrow_right.svg", 22, Color.WHITE))
            setBackgroundColor(blue)
            // Forma circular via post (depois do layout)
            post {
                val radius = (21 * dp)
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(blue)
                }
            }
            alpha = 0.4f
            isClickable = false
            isFocusable = false
        }

        inputField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                val hasText = !s.isNullOrBlank()
                btnSend.alpha = if (hasText) 1f else 0.4f
                btnSend.isClickable = hasText
                btnSend.isFocusable = hasText
            }
        })

        inputField.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }

        btnSend.setOnClickListener { sendMessage() }

        inputRow.addView(inputWrapper)
        inputRow.addView(btnSend)
        inputBar.addView(inputBarTop)
        inputBar.addView(inputRow)

        mainContent.addView(toolbar)
        mainContent.addView(divider)
        mainContent.addView(centerArea)
        mainContent.addView(inputBar)

        // ── Drawer overlay ────────────────────────────────────────────────────
        drawerOverlay = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            // Clicar fora do painel fecha
            setOnClickListener { closeDrawer() }
        }

        // Scrim escuro
        val scrim = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.argb(100, 0, 0, 0))
        }

        // Painel lateral (esquerda)
        drawerPanel = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                (280 * dp).toInt(),
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.START
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@AiSearchActivity, R.color.drawer_background))
            elevation = 16f
            translationX = -(280 * dp)
        }

        // Cabeçalho do drawer
        val drawerHeader = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (64 * dp).toInt())
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            val p = (20 * dp).toInt()
            setPadding(p, 0, p, 0)
            setBackgroundColor(appbarBg)
        }

        val drawerTitle = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "Conversas"
            textSize = 17f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@AiSearchActivity, R.color.text_primary))
        }

        val drawerNew = ImageView(this).apply {
            val sz = (36 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz)
            val p = (6 * dp).toInt()
            setPadding(p, p, p, p)
            setImageDrawable(svgDrawable("icons/svg/add.svg", 22, iconTint))
            isClickable = true; isFocusable = true
            background = with(android.util.TypedValue()) {
                theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
                ContextCompat.getDrawable(this@AiSearchActivity, resourceId)
            }
        }

        drawerHeader.addView(drawerTitle)
        drawerHeader.addView(drawerNew)

        val drawerDivider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(divColor)
        }

        // Placeholder de conversas
        val drawerEmpty = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            text = "Nenhuma conversa ainda"
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(this@AiSearchActivity, R.color.text_secondary))
        }

        drawerPanel.addView(drawerHeader)
        drawerPanel.addView(drawerDivider)
        drawerPanel.addView(drawerEmpty)

        drawerOverlay.addView(scrim)
        drawerOverlay.addView(drawerPanel)
        // Consumir toque no painel (não fechar ao clicar dentro)
        drawerPanel.setOnClickListener { /* bloqueia propagação */ }

        rootFrame.addView(mainContent)
        rootFrame.addView(drawerOverlay)

        setContentView(rootFrame)
    }

    // ─── Drawer toggle ────────────────────────────────────────────────────────

    private fun toggleDrawer() {
        if (drawerOpen) closeDrawer() else openDrawer()
    }

    private fun openDrawer() {
        drawerOpen = true
        drawerOverlay.visibility = View.VISIBLE
        drawerOverlay.alpha = 0f
        drawerOverlay.animate().alpha(1f).setDuration(260)
            .setInterpolator(DecelerateInterpolator(2f)).start()
        drawerPanel.animate().translationX(0f).setDuration(300)
            .setInterpolator(DecelerateInterpolator(2.5f)).start()
        // Ícone → filled
        val tint = ContextCompat.getColor(this, R.color.icon_tint)
        btnSidePanel.setImageDrawable(svgDrawable("icons/svg/side_panel_filled.svg", 24, tint))
    }

    private fun closeDrawer() {
        drawerOpen = false
        val dp = resources.displayMetrics.density
        drawerPanel.animate().translationX(-(280 * dp)).setDuration(260)
            .setInterpolator(DecelerateInterpolator(2f)).start()
        drawerOverlay.animate().alpha(0f).setDuration(220).withEndAction {
            drawerOverlay.visibility = View.GONE
        }.start()
        // Ícone → normal
        val tint = ContextCompat.getColor(this, R.color.icon_tint)
        btnSidePanel.setImageDrawable(svgDrawable("icons/svg/side_panel.svg", 24, tint))
    }

    // ─── Enviar mensagem ──────────────────────────────────────────────────────

    private fun sendMessage() {
        val text = inputField.text.toString().trim()
        if (text.isEmpty()) return
        inputField.setText("")
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(inputField.windowToken, 0)
        // Aqui integrares com a tua lógica de IA futuramente
        Toast.makeText(this, "\"$text\"", Toast.LENGTH_SHORT).show()
    }

    // ─── Popup more ───────────────────────────────────────────────────────────

    private fun showMorePopup(anchor: View) {
        val dp        = resources.displayMetrics.density
        val iconTint  = ContextCompat.getColor(this, R.color.icon_tint)
        val bgColor   = ContextCompat.getColor(this, R.color.popup_background)
        val textColor = ContextCompat.getColor(this, R.color.text_primary)

        data class Item(val icon: String, val label: String, val action: () -> Unit)

        val items = listOf(
            Item("icons/svg/add.svg",      "Nova conversa")  { /* futuro */ },
            Item("icons/svg/history.svg",  "Histórico")      { /* futuro */ },
            Item("icons/svg/settings.svg", "Definições")     { /* futuro */ },
        )

        val menuView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background  = ContextCompat.getDrawable(this@AiSearchActivity, R.drawable.popup_bg)
            val pad = (8 * dp).toInt()
            setPadding(0, pad, 0, pad)
        }

        var pop: PopupWindow? = null

        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation  = LinearLayout.HORIZONTAL
                gravity      = Gravity.CENTER_VERTICAL
                val h = (16 * dp).toInt(); val v = (12 * dp).toInt()
                setPadding(h, v, h, v)
                isClickable = true; isFocusable = true
                background = ContextCompat.getDrawable(this@AiSearchActivity, R.drawable.ripple_item)
            }
            val iv = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (20 * dp).toInt(), (20 * dp).toInt()
                ).also { it.marginEnd = (12 * dp).toInt() }
                setImageDrawable(svgDrawable(item.icon, 20, iconTint))
            }
            val tv = TextView(this).apply {
                text = item.label
                textSize = 14f
                setTextColor(textColor)
            }
            row.addView(iv); row.addView(tv)
            row.setOnClickListener { pop?.dismiss(); item.action() }
            menuView.addView(row)
        }

        menuView.scaleX = 0.85f; menuView.scaleY = 0.85f; menuView.alpha = 0f
        menuView.animate().scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(300).setInterpolator(DecelerateInterpolator(2.5f)).start()

        pop = PopupWindow(
            menuView,
            (200 * dp).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 18f
        }

        val loc = IntArray(2)
        anchor.getLocationOnScreen(loc)
        val xPos = loc[0] + anchor.width - (200 * dp).toInt() - (8 * dp).toInt()
        val yPos = loc[1] + anchor.height + (4 * dp).toInt()
        pop.showAtLocation(window.decorView, Gravity.NO_GRAVITY, xPos, yPos)
    }

    // ─── Theme ────────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        applyStatusBarTheme()
    }

    private fun applyStatusBarTheme() {
        val isLight = !resources.configuration.isNightModeActive
        insetsController.isAppearanceLightStatusBars = isLight
    }

    private fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
        val px  = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        return try {
            val svg = SVG.getFromAsset(assets, path)
            svg.documentWidth  = px.toFloat()
            svg.documentHeight = px.toFloat()
            svg.renderToCanvas(Canvas(bmp))
            val drawable = BitmapDrawable(resources, bmp)
            drawable.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
            drawable
        } catch (_: Exception) {
            // Ícone não existe ainda (side_panel, side_panel_filled) → retorna vazio sem crash
            BitmapDrawable(resources, bmp)
        }
    }
}