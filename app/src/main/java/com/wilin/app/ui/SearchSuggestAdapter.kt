package com.wilin.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
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

    inner class VH(val row: LinearLayout, val icon: ImageView, val tv: TextView) :
        RecyclerView.ViewHolder(row)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val dp  = ctx.resources.displayMetrics.density
        val iconTint = ContextCompat.getColor(ctx, R.color.icon_tint_secondary)
        val textColor = ContextCompat.getColor(ctx, R.color.text_primary)

        val row = LinearLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val h = (20 * dp).toInt()
            val v = (13 * dp).toInt()
            setPadding(h, v, h, v)
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)
        }

        val px = (18 * dp).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val svg = SVG.getFromAsset(ctx.assets, "icons/svg/history.svg")
        svg.documentWidth = px.toFloat(); svg.documentHeight = px.toFloat()
        svg.renderToCanvas(Canvas(bmp))
        val icon = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(px, px).also {
                it.marginEnd = (12 * dp).toInt()
            }
            val d = BitmapDrawable(ctx.resources, bmp)
            d.setColorFilter(iconTint, PorterDuff.Mode.SRC_IN)
            setImageDrawable(d)
        }

        val tv = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 15f
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(textColor)
        }

        row.addView(icon)
        row.addView(tv)
        return VH(row, icon, tv)
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