// SearchFragment.kt
package com.wilin.app.ui

import android.content.Context
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
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.wilin.app.R

class SearchFragment : Fragment() {

    private val searchHistory = mutableListOf<String>()
    private var adapter: SearchHistoryAdapter? = null

    companion object {
        private const val PREFS_HISTORY = "wilin_search_history"
        private const val KEY_HISTORY   = "history"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val dp  = ctx.resources.displayMetrics.density

        val root = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            orientation  = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.background))
        }

        // ── Cabeçalho "Recentes" ─────────────────────────────────────────────
        val header = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            orientation  = LinearLayout.HORIZONTAL
            gravity      = Gravity.CENTER_VERTICAL
            setPadding((20 * dp).toInt(), (20 * dp).toInt(), (20 * dp).toInt(), (8 * dp).toInt())
        }
        val labelTv = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text         = ctx.getString(R.string.recent)
            textSize     = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            letterSpacing = 0.05f
        }
        val clearTv = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            text         = ctx.getString(R.string.clear_history)
            textSize     = 13f
            setTextColor(ContextCompat.getColor(ctx, R.color.colorPrimary))
            isClickable  = true
            isFocusable  = true
            background   = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)
        }
        header.addView(labelTv)
        header.addView(clearTv)

        // ── Lista de histórico ───────────────────────────────────────────────
        val recycler = RecyclerView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            layoutManager = LinearLayoutManager(ctx)
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        loadHistory(ctx)
        adapter = SearchHistoryAdapter(searchHistory.take(12)) { query ->
            // Ao clicar num item do histórico, abre SearchActivity com o texto preenchido
            val intent = Intent(ctx, SearchActivity::class.java).apply {
                putExtra("prefill_query", query)
            }
            startActivity(intent)
        }
        recycler.adapter = adapter

        clearTv.setOnClickListener {
            clearHistory(ctx)
            adapter?.updateList(emptyList())
            header.visibility = View.GONE
        }

        if (searchHistory.isEmpty()) header.visibility = View.GONE

        // ── Empty state ──────────────────────────────────────────────────────
        val emptyView = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            orientation  = LinearLayout.VERTICAL
            gravity      = Gravity.CENTER
            visibility   = if (searchHistory.isEmpty()) View.VISIBLE else View.GONE
        }
        val emptyIv = ImageView(ctx).apply {
            val sz = (48 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz).also { it.bottomMargin = (16 * dp).toInt() }
        }
        // Tenta carregar no_connection.png se não tiver histórico
        runCatching {
            val bmp = android.graphics.BitmapFactory.decodeStream(ctx.assets.open("icons/png/no_connection.png"))
            emptyIv.setImageBitmap(bmp)
        }.onFailure {
            // fallback: ícone de pesquisa
            val px  = (48 * dp).toInt()
            val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            runCatching {
                SVG.getFromAsset(ctx.assets, "icons/svg/magnifying_glass_outline.svg").apply {
                    documentWidth = px.toFloat(); documentHeight = px.toFloat()
                    renderToCanvas(Canvas(bmp))
                }
            }
            val d = BitmapDrawable(ctx.resources, bmp)
            d.setColorFilter(ContextCompat.getColor(ctx, R.color.text_hint), PorterDuff.Mode.SRC_IN)
            emptyIv.setImageDrawable(d)
        }
        val emptyTv = TextView(ctx).apply {
            text      = ctx.getString(R.string.search_empty)
            textSize  = 15f
            gravity   = Gravity.CENTER
            setTextColor(ContextCompat.getColor(ctx, R.color.text_hint))
        }
        emptyView.addView(emptyIv)
        emptyView.addView(emptyTv)

        root.addView(header)
        root.addView(recycler)
        root.addView(emptyView)

        return root
    }

    override fun onResume() {
        super.onResume()
        // Atualiza o histórico sempre que a tab de pesquisa fica visível
        val ctx = context ?: return
        loadHistory(ctx)
        adapter?.updateList(searchHistory.take(12))
        view?.let { root ->
            val header = (root as? ViewGroup)?.getChildAt(0)
            header?.visibility = if (searchHistory.isEmpty()) View.GONE else View.VISIBLE
            val emptyView = (root as? ViewGroup)?.getChildAt(2)
            emptyView?.visibility = if (searchHistory.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun loadHistory(ctx: Context) {
        val raw = ctx.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "") ?: ""
        searchHistory.clear()
        if (raw.isNotEmpty()) searchHistory.addAll(raw.split("|||").filter { it.isNotEmpty() })
    }

    private fun clearHistory(ctx: Context) {
        searchHistory.clear()
        ctx.getSharedPreferences(PREFS_HISTORY, Context.MODE_PRIVATE)
            .edit().remove(KEY_HISTORY).apply()
    }
}

// ── Adapter para histórico na tab de pesquisa ─────────────────────────────────

class SearchHistoryAdapter(
    private var items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SearchHistoryAdapter.VH>() {

    inner class VH(val row: LinearLayout, val tv: TextView) : RecyclerView.ViewHolder(row)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val dp  = ctx.resources.displayMetrics.density

        // Ícone de histórico
        val px  = (18 * dp).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        runCatching {
            SVG.getFromAsset(ctx.assets, "icons/svg/history.svg").apply {
                documentWidth = px.toFloat(); documentHeight = px.toFloat()
                renderToCanvas(Canvas(bmp))
            }
        }
        val iconDrawable = BitmapDrawable(ctx.resources, bmp).also {
            it.setColorFilter(ContextCompat.getColor(ctx, R.color.icon_tint_secondary), PorterDuff.Mode.SRC_IN)
        }

        val icon = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(px, px).also { it.marginEnd = (14 * dp).toInt() }
            setImageDrawable(iconDrawable)
        }
        val tv = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize     = 15f
            maxLines     = 1
            ellipsize    = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }
        val arrowPx = (16 * dp).toInt()
        val arrowBmp = Bitmap.createBitmap(arrowPx, arrowPx, Bitmap.Config.ARGB_8888)
        runCatching {
            SVG.getFromAsset(ctx.assets, "icons/svg/arrow_right.svg").apply {
                documentWidth = arrowPx.toFloat(); documentHeight = arrowPx.toFloat()
                renderToCanvas(Canvas(arrowBmp))
            }
        }
        val arrowDrawable = BitmapDrawable(ctx.resources, arrowBmp).also {
            it.setColorFilter(ContextCompat.getColor(ctx, R.color.text_hint), PorterDuff.Mode.SRC_IN)
        }
        val arrow = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(arrowPx, arrowPx)
            setImageDrawable(arrowDrawable)
        }

        val row = LinearLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            orientation  = LinearLayout.HORIZONTAL
            gravity      = Gravity.CENTER_VERTICAL
            val h = (20 * dp).toInt()
            val v = (14 * dp).toInt()
            setPadding(h, v, h, v)
            isClickable  = true
            isFocusable  = true
            background   = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)
        }
        row.addView(icon)
        row.addView(tv)
        row.addView(arrow)
        return VH(row, tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.tv.text = items[position]
        holder.row.setOnClickListener { onClick(items[position]) }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }
}