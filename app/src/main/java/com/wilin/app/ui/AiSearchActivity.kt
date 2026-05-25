package com.wilin.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
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

class AiSearchActivity : AppCompatActivity() {

    private lateinit var insetsController: WindowInsetsControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val dp = resources.displayMetrics.density
        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)
        val blue     = ContextCompat.getColor(this, R.color.colorPrimary)

        insetsController = WindowInsetsControllerCompat(window, window.decorView)
        applyStatusBarTheme()

        // Root
        val root = LinearLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@AiSearchActivity, R.color.background))
        }

        // Toolbar
        val toolbar = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (56 * dp).toInt()
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((4 * dp).toInt(), 0, (16 * dp).toInt(), 0)
            setBackgroundColor(ContextCompat.getColor(this@AiSearchActivity, R.color.appbar_background))
        }

        val btnBack = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((44 * dp).toInt(), (44 * dp).toInt())
            setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/back_arrow.svg", 24, iconTint))
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(this@AiSearchActivity, android.R.drawable.btn_default)
            background = with(android.util.TypedValue()) {
                theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
                ContextCompat.getDrawable(this@AiSearchActivity, resourceId)
            }
            setOnClickListener { finish(); overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) }
        }

        val titleAi = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((20 * dp).toInt(), (20 * dp).toInt()).also {
                it.marginStart = (8 * dp).toInt()
                it.marginEnd   = (6 * dp).toInt()
            }
            setImageDrawable(svgDrawable("icons/svg/ai.svg", 20, blue))
        }

        val titleText = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "Ask AI"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@AiSearchActivity, R.color.text_primary))
        }

        toolbar.addView(btnBack)
        toolbar.addView(titleAi)
        toolbar.addView(titleText)

        // Divider
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(ContextCompat.getColor(this@AiSearchActivity, R.color.divider))
        }

        // Placeholder central
        val center = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val aiIconBig = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams((64 * dp).toInt(), (64 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/ai.svg", 64, blue))
            alpha = 0.15f
        }

        val comingSoon = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (16 * dp).toInt() }
            text = "Em breve"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@AiSearchActivity, R.color.text_secondary))
        }

        center.addView(aiIconBig)
        center.addView(comingSoon)

        root.addView(toolbar)
        root.addView(divider)
        root.addView(center)

        setContentView(root)
    }

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
        val svg = SVG.getFromAsset(assets, path)
        svg.documentWidth  = px.toFloat()
        svg.documentHeight = px.toFloat()
        svg.renderToCanvas(Canvas(bmp))
        val drawable = BitmapDrawable(resources, bmp)
        drawable.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
        return drawable
    }
}