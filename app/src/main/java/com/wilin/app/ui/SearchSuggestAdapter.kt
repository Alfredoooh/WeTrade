// SearchSuggestAdapter.kt
package com.wilin.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.wilin.app.R

class SearchSuggestAdapter(
    private var items: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<SearchSuggestAdapter.VH>() {

    // Itens que vieram do histórico mostram ícone de relógio; outros de pesquisa
    private var historyItems: Set<String> = emptySet()

    fun setHistoryItems(set: Set<String>) { historyItems = set }

    inner class VH(val row: LinearLayout, val icon: ImageView, val tv: TextView) :
        RecyclerView.ViewHolder(row)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx  = parent.context
        val dp   = ctx.resources.displayMetrics.density
        val secColor  = ContextCompat.getColor(ctx, R.color.icon_tint_secondary)
        val textColor = ContextCompat.getColor(ctx, R.color.text_primary)

        val px = (18 * dp).toInt()
        val icon = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(px, px).also { it.marginEnd = (14 * dp).toInt() }
        }

        val tv = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize  = 15f
            maxLines  = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(textColor)
        }

        val arrowPx = (14 * dp).toInt()
        val arrowBmp = Bitmap.createBitmap(arrowPx, arrowPx, Bitmap.Config.ARGB_8888)
        runCatching {
            SVG.getFromAsset(ctx.assets, "icons/svg/arrow_right.svg").apply {
                documentWidth = arrowPx.toFloat(); documentHeight = arrowPx.toFloat()
                renderToCanvas(Canvas(arrowBmp))
            }
        }
        val arrowDr = BitmapDrawable(ctx.resources, arrowBmp).also {
            it.setColorFilter(ContextCompat.getColor(ctx, R.color.text_hint), PorterDuff.Mode.SRC_IN)
        }
        val arrowIv = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(arrowPx, arrowPx)
            setImageDrawable(arrowDr)
        }

        val row = LinearLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            val h = (20 * dp).toInt()
            val v = (14 * dp).toInt()
            setPadding(h, v, h, v)
            isClickable = true
            isFocusable = true
            background  = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)
        }
        row.addView(icon)
        row.addView(tv)
        row.addView(arrowIv)
        return VH(row, icon, tv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx  = holder.row.context
        val dp   = ctx.resources.displayMetrics.density
        val secColor = ContextCompat.getColor(ctx, R.color.icon_tint_secondary)

        holder.tv.text = item

        // Ícone: histórico → relógio, sugestão → lupa
        val iconPath = if (historyItems.contains(item)) "icons/svg/history.svg"
                       else "icons/svg/magnifying_glass_outline.svg"
        val px = (18 * dp).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        runCatching {
            SVG.getFromAsset(ctx.assets, iconPath).apply {
                documentWidth = px.toFloat(); documentHeight = px.toFloat()
                renderToCanvas(Canvas(bmp))
            }
        }
        val dr = BitmapDrawable(ctx.resources, bmp).also {
            it.setColorFilter(secColor, PorterDuff.Mode.SRC_IN)
        }
        holder.icon.setImageDrawable(dr)
        holder.row.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    fun updateList(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }
}