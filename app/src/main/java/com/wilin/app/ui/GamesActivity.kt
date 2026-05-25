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
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.wilin.app.R

class GamesActivity : AppCompatActivity() {

    private lateinit var insetsController: WindowInsetsControllerCompat

    private val games = listOf(
        GameItem("2048",        "https://play2048.co/",                              Color.parseColor("#F4A261"), "2"),
        GameItem("Tetris",      "https://tetris.com/play-tetris",                   Color.parseColor("#E63946"), "T"),
        GameItem("Snake",       "https://playsnake.org/",                           Color.parseColor("#2A9D8F"), "S"),
        GameItem("Pac-Man",     "https://www.google.com/logos/2010/pacman10-i.html", Color.parseColor("#E9C46A"), "P"),
        GameItem("Sudoku",      "https://sudoku.com/",                              Color.parseColor("#457B9D"), "9"),
        GameItem("Chess",       "https://www.chess.com/play/online",                Color.parseColor("#1D3557"), "♟"),
        GameItem("Flappy Bird", "https://flappybird.io/",                           Color.parseColor("#48CAE4"), "F"),
        GameItem("Wordle",      "https://www.nytimes.com/games/wordle/index.html",  Color.parseColor("#6A994E"), "W"),
        GameItem("Crossword",   "https://www.nytimes.com/crosswords",               Color.parseColor("#8338EC"), "X"),
        GameItem("Solitaire",   "https://www.solitr.com/",                          Color.parseColor("#023E8A"), "♣"),
        GameItem("Minesweeper", "https://minesweeperonline.com/",                   Color.parseColor("#606C38"), "M"),
        GameItem("Candy Crush", "https://candycrushsaga.com/",                     Color.parseColor("#FB5607"), "C"),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        insetsController = WindowInsetsControllerCompat(window, window.decorView)
        applyTheme()

        val ctx = this
        val dp  = resources.displayMetrics.density
        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)
        val iconSec  = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.background))
        }

        // Toolbar
        val toolbar = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (56 * dp).toInt())
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((4 * dp).toInt(), 0, (16 * dp).toInt(), 0)
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.appbar_background))
        }

        val btnBack = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams((44 * dp).toInt(), (44 * dp).toInt())
            setPadding((10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt(), (10 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/back_arrow.svg", 24, iconTint))
            isClickable = true; isFocusable = true
            background = with(android.util.TypedValue()) {
                theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, this, true)
                ContextCompat.getDrawable(ctx, resourceId)
            }
            setOnClickListener { finish() }
        }

        val titleTv = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "Jogos"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding((4 * dp).toInt(), 0, 0, 0)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        toolbar.addView(btnBack)
        toolbar.addView(titleTv)

        val divider = android.view.View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.divider))
        }

        val recycler = RecyclerView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            setPadding((8 * dp).toInt(), (8 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt())
            clipToPadding = false
            layoutManager = GridLayoutManager(ctx, 3)
            adapter = GamesAdapter(games) { game ->
                val intent = Intent(ctx, BrowserResponseActivity::class.java).apply {
                    putExtra(BrowserResponseActivity.EXTRA_QUERY, game.url)
                }
                startActivity(intent)
            }
        }

        root.addView(toolbar)
        root.addView(divider)
        root.addView(recycler)
        setContentView(root)
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