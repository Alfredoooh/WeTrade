package com.wilin.app.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wilin.app.R

data class GameItem(
    val name: String,
    val url: String,
    val color: Int,
    val iconChar: String
)

class GamesFragment : Fragment() {

    private val games = listOf(
        GameItem("2048",          "https://play2048.co/",                             Color.parseColor("#F4A261"), "2"),
        GameItem("Tetris",        "https://tetris.com/play-tetris",                  Color.parseColor("#E63946"), "T"),
        GameItem("Snake",         "https://playsnake.org/",                          Color.parseColor("#2A9D8F"), "S"),
        GameItem("Pac-Man",       "https://www.google.com/logos/2010/pacman10-i.html", Color.parseColor("#E9C46A"), "P"),
        GameItem("Sudoku",        "https://sudoku.com/",                             Color.parseColor("#457B9D"), "9"),
        GameItem("Chess",         "https://www.chess.com/play/online",               Color.parseColor("#1D3557"), "♟"),
        GameItem("Flappy Bird",   "https://flappybird.io/",                          Color.parseColor("#48CAE4"), "F"),
        GameItem("Wordle",        "https://www.nytimes.com/games/wordle/index.html", Color.parseColor("#6A994E"), "W"),
        GameItem("Crossword",     "https://www.nytimes.com/crosswords",              Color.parseColor("#8338EC"), "X"),
        GameItem("Candy Crush",   "https://candycrushsaga.com/",                    Color.parseColor("#FB5607"), "C"),
        GameItem("Solitaire",     "https://www.solitr.com/",                         Color.parseColor("#023E8A"), "♣"),
        GameItem("Minesweeper",   "https://minesweeperonline.com/",                  Color.parseColor("#606C38"), "💣".let { Color.parseColor("#606C38") }.let { "M" }),
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val dp  = ctx.resources.displayMetrics.density

        val root = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.background))
        }

        val title = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.topMargin    = (20 * dp).toInt()
                it.bottomMargin = (12 * dp).toInt()
            }
            text = "Jogos"
            textSize = 22f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding((16 * dp).toInt(), 0, (16 * dp).toInt(), 0)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        val recycler = RecyclerView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
            setPadding((8 * dp).toInt(), 0, (8 * dp).toInt(), (16 * dp).toInt())
            clipToPadding = false
            layoutManager = GridLayoutManager(ctx, 3)
            adapter = GamesAdapter(games) { game ->
                val intent = Intent(ctx, BrowserResponseActivity::class.java).apply {
                    putExtra(BrowserResponseActivity.EXTRA_QUERY, game.url)
                }
                startActivity(intent)
                requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        root.addView(title)
        root.addView(recycler)
        return root
    }
}

class GamesAdapter(
    private val items: List<GameItem>,
    private val onClick: (GameItem) -> Unit
) : RecyclerView.Adapter<GamesAdapter.VH>() {

    inner class VH(val root: LinearLayout, val iconView: FrameLayout, val label: TextView) :
        RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val dp  = ctx.resources.displayMetrics.density

        val root = LinearLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (8 * dp).toInt() }
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding((8 * dp).toInt(), (10 * dp).toInt(), (8 * dp).toInt(), (10 * dp).toInt())
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)
        }

        val iconSize = (64 * dp).toInt()
        val iconFrame = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
        }

        val label = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (8 * dp).toInt() }
            textSize = 12f
            maxLines = 1
            gravity = Gravity.CENTER_HORIZONTAL
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        root.addView(iconFrame)
        root.addView(label)
        return VH(root, iconFrame, label)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx  = holder.root.context
        val dp   = ctx.resources.displayMetrics.density
        val iconSize = (64 * dp).toInt()

        holder.label.text = item.name
        holder.iconView.removeAllViews()

        // Ícone colorido com letra
        val bg = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = (16 * dp)
            setColor(item.color)
        }
        val iconContainer = FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize)
            background = bg
            elevation = 3f * dp
        }
        val letter = TextView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            text = item.iconChar
            textSize = (iconSize / dp * 0.38f)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        iconContainer.addView(letter)
        holder.iconView.addView(iconContainer)

        holder.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}