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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.caverock.androidsvg.SVG
import com.wilin.app.MainActivity
import com.wilin.app.R

data class HubOption(
    val label: String,
    val iconSvg: String,
    val gradStart: Int,
    val gradEnd: Int,
    val action: () -> Unit
)

class HubFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val dp  = ctx.resources.displayMetrics.density

        val options = listOf(
            HubOption("Jogos", "icons/svg/game_outline.svg",
                Color.parseColor("#5856D6"), Color.parseColor("#7B79E8")) {
                startActivity(Intent(ctx, GamesActivity::class.java))
            },
            HubOption("Histórico", "icons/svg/history.svg",
                Color.parseColor("#FF9500"), Color.parseColor("#FFB340")) {
                startActivity(Intent(ctx, HistoryActivity::class.java))
            },
            HubOption("Favoritos", "icons/svg/bookmark_outline.svg",
                Color.parseColor("#FF3B30"), Color.parseColor("#FF6B63")) {
                startActivity(Intent(ctx, BookmarksActivity::class.java))
            },
            HubOption("Downloads", "icons/svg/download.svg",
                Color.parseColor("#34C759"), Color.parseColor("#5DD879")) {
                // futuro
            },
            HubOption("Incógnito", "icons/svg/incognito.svg",
                Color.parseColor("#48484A"), Color.parseColor("#636366")) {
                startActivity(Intent(ctx, IncognitoActivity::class.java))
            },
            HubOption("Definições", "icons/svg/settings.svg",
                Color.parseColor("#007AFF"), Color.parseColor("#409CFF")) {
                startActivity(Intent(ctx, SettingsActivity::class.java))
            },
        )

        val scroll = ScrollView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.background))
        }

        val root = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding((16 * dp).toInt(), (20 * dp).toInt(), (16 * dp).toInt(), (32 * dp).toInt())
        }

        val title = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (20 * dp).toInt() }
            text = "Hub"
            textSize = 28f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }
        root.addView(title)

        // Grid 2 colunas — cards maiores e mais apelativo
        val cols = 2
        val rows = (options.size + cols - 1) / cols
        for (row in 0 until rows) {
            val rowLayout = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = (12 * dp).toInt() }
                orientation = LinearLayout.HORIZONTAL
            }
            for (col in 0 until cols) {
                val idx = row * cols + col
                if (idx < options.size) {
                    val opt  = options[idx]
                    val cell = buildCell(opt, dp)
                    cell.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        .also { if (col == 0) it.marginEnd = (6 * dp).toInt() else it.marginStart = (6 * dp).toInt() }
                    rowLayout.addView(cell)
                } else {
                    rowLayout.addView(View(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                    })
                }
            }
            root.addView(rowLayout)
        }

        scroll.addView(root)
        return scroll
    }

    private fun buildCell(opt: HubOption, dp: Float): FrameLayout {
        val ctx = requireContext()

        // Card com fundo gradiente suave
        val card = FrameLayout(ctx).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(withAlpha(opt.gradStart, 0.13f), withAlpha(opt.gradEnd, 0.06f))
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20 * dp
            }
            isClickable = true
            isFocusable = true
            foreground = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)
            clipToOutline = true
        }

        val inner = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (110 * dp).toInt()
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((20 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt(), (16 * dp).toInt())
        }

        // Ícone com fundo circular colorido
        val iconFrame = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((44 * dp).toInt(), (44 * dp).toInt())
                .also { it.bottomMargin = (10 * dp).toInt() }
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(opt.gradStart, opt.gradEnd)
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12 * dp
            }
        }

        val iconIv = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                (24 * dp).toInt(), (24 * dp).toInt(), Gravity.CENTER
            )
            try {
                val px  = (24 * dp).toInt()
                val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
                val svg = SVG.getFromAsset(ctx.assets, opt.iconSvg)
                svg.documentWidth  = px.toFloat()
                svg.documentHeight = px.toFloat()
                svg.renderToCanvas(Canvas(bmp))
                val d = BitmapDrawable(ctx.resources, bmp)
                d.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                setImageDrawable(d)
            } catch (_: Exception) {}
        }
        iconFrame.addView(iconIv)

        val label = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = opt.label
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        inner.addView(iconFrame)
        inner.addView(label)
        card.addView(inner)
        card.setOnClickListener { opt.action() }
        return card
    }

    private fun withAlpha(color: Int, alpha: Float) =
        Color.argb((alpha * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))
}