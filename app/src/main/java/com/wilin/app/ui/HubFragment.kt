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
    val color: Int,
    val action: () -> Unit
)

class HubFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val dp  = ctx.resources.displayMetrics.density
        val act = requireActivity() as MainActivity

        val options = listOf(
            HubOption("Jogos", "icons/svg/game_outline.svg", Color.parseColor("#5856D6")) {
                startActivity(Intent(ctx, GamesActivity::class.java))
            },
            HubOption("Histórico", "icons/svg/history.svg", Color.parseColor("#FF9500")) {
                startActivity(Intent(ctx, HistoryActivity::class.java))
            },
            HubOption("Favoritos", "icons/svg/bookmark_outline.svg", Color.parseColor("#FF3B30")) {
                startActivity(Intent(ctx, BookmarksActivity::class.java))
            },
            HubOption("Downloads", "icons/svg/download.svg", Color.parseColor("#34C759")) {
                // futuro
            },
            HubOption("Incógnito", "icons/svg/incognito.svg", Color.parseColor("#636366")) {
                startActivity(Intent(ctx, IncognitoActivity::class.java))
            },
            HubOption("Definições", "icons/svg/settings.svg", Color.parseColor("#007AFF")) {
                startActivity(Intent(ctx, SettingsActivity::class.java))
            },
        )

        val scroll = ScrollView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val root = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt(), (32 * dp).toInt())
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.background))
        }

        val title = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (20 * dp).toInt() }
            text = "Hub"
            textSize = 26f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }
        root.addView(title)

        // Grid 3 colunas
        val cols = 3
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
                    val opt = options[idx]
                    val cell = buildHubCell(opt, dp)
                    cell.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        .also { if (col < cols - 1) it.marginEnd = (10 * dp).toInt() }
                    rowLayout.addView(cell)
                } else {
                    val spacer = View(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
                    }
                    rowLayout.addView(spacer)
                }
            }
            root.addView(rowLayout)
        }

        scroll.addView(root)
        return scroll
    }

    private fun buildHubCell(opt: HubOption, dp: Float): LinearLayout {
        val ctx = requireContext()
        val iconSize = (48 * dp).toInt()

        val cell = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, (12 * dp).toInt(), 0, (12 * dp).toInt())
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)
        }

        val iconFrame = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14 * dp
                setColor(withAlpha(opt.color, 0.15f))
            }
        }

        val iconIv = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                (26 * dp).toInt(), (26 * dp).toInt(), Gravity.CENTER
            )
            try {
                val px  = (26 * dp).toInt()
                val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
                val svg = SVG.getFromAsset(ctx.assets, opt.iconSvg)
                svg.documentWidth  = px.toFloat()
                svg.documentHeight = px.toFloat()
                svg.renderToCanvas(Canvas(bmp))
                val drawable = BitmapDrawable(ctx.resources, bmp)
                drawable.setColorFilter(opt.color, PorterDuff.Mode.SRC_IN)
                setImageDrawable(drawable)
            } catch (_: Exception) {}
        }
        iconFrame.addView(iconIv)

        val label = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (8 * dp).toInt() }
            text = opt.label
            textSize = 12f
            gravity = Gravity.CENTER
            maxLines = 1
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        cell.addView(iconFrame)
        cell.addView(label)

        cell.setOnClickListener { opt.action() }
        return cell
    }

    private fun withAlpha(color: Int, alpha: Float) =
        Color.argb((alpha * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))
}