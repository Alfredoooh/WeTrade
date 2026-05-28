package com.wilin.app.ui

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class NewsAdapter(
    private val items: List<NewsItem>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<NewsAdapter.VH>() {

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    inner class VH(val root: LinearLayout) : RecyclerView.ViewHolder(root) {
        val thumb: ImageView    = root.findViewWithTag("thumb")
        val title: TextView     = root.findViewWithTag("title")
        val desc: TextView      = root.findViewWithTag("desc")
        val source: TextView    = root.findViewWithTag("source")
        val textWrap: LinearLayout = root.findViewWithTag("textWrap")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx     = parent.context
        val dp      = ctx.resources.displayMetrics.density
        val isDark  = (ctx.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val cardBgColor  = if (isDark) Color.parseColor("#1C1C1E") else Color.WHITE
        val textWrapBg   = if (isDark) Color.parseColor("#1C1C1E") else Color.WHITE
        val titleColor   = if (isDark) Color.parseColor("#F2F2F7") else Color.parseColor("#1C1C1E")
        val descColor    = if (isDark) Color.parseColor("#ABABAB") else Color.parseColor("#636366")
        val sourceColor  = if (isDark) Color.parseColor("#6E6E73") else Color.parseColor("#8E8E93")

        val card = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also {
                it.bottomMargin = (10 * dp).toInt()
                it.marginStart  = (16 * dp).toInt()
                it.marginEnd    = (16 * dp).toInt()
            }
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                shape        = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14 * dp
                setColor(cardBgColor)
                if (isDark) setStroke(1, Color.parseColor("#2C2C2E"))
            }
            elevation     = if (isDark) 0f else 2 * dp
            clipToOutline = true
            isClickable   = true
            isFocusable   = true
        }

        val thumb = ImageView(ctx).apply {
            tag = "thumb"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (180 * dp).toInt()
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#F2F2F7"))
        }

        // Linha de cor no topo do card (accent) — só aparece ao carregar imagem
        val accentBar = android.view.View(ctx).apply {
            tag = "accentBar"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (3 * dp).toInt()
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        val textWrap = LinearLayout(ctx).apply {
            tag = "textWrap"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding((14 * dp).toInt(), (10 * dp).toInt(), (14 * dp).toInt(), (12 * dp).toInt())
            setBackgroundColor(textWrapBg)
        }

        val titleTv = TextView(ctx).apply {
            tag = "title"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 14f; maxLines = 2
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(titleColor)
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val descTv = TextView(ctx).apply {
            tag = "desc"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (3 * dp).toInt() }
            textSize = 12f; maxLines = 2
            setTextColor(descColor)
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val sourceTv = TextView(ctx).apply {
            tag = "source"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (6 * dp).toInt() }
            textSize = 10f
            setTextColor(sourceColor)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        textWrap.addView(titleTv)
        textWrap.addView(descTv)
        textWrap.addView(sourceTv)
        card.addView(thumb)
        card.addView(accentBar)
        card.addView(textWrap)

        return VH(card)
    }

    override fun onBindViewHolder(vh: VH, position: Int) {
        val ctx    = vh.root.context
        val dp     = ctx.resources.displayMetrics.density
        val isDark = (ctx.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val item = items[position]
        vh.title.text  = item.title
        vh.desc.text   = item.description
        vh.source.text = "• ${item.sourceName.uppercase()}"

        val placeholderColor = if (isDark) Color.parseColor("#2C2C2E") else Color.parseColor("#F2F2F7")
        vh.thumb.setImageBitmap(null)
        vh.thumb.setBackgroundColor(placeholderColor)

        // Reset accent bar
        val accentBar = vh.root.findViewWithTag<android.view.View>("accentBar")
        accentBar?.setBackgroundColor(Color.TRANSPARENT)

        if (item.imageUrl.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val req  = Request.Builder().url(item.imageUrl).build()
                    val resp = http.newCall(req).execute()
                    val bmp  = resp.body?.byteStream()?.let { BitmapFactory.decodeStream(it) }

                    if (bmp != null) {
                        // Extrai cor dominante manualmente (sem Palette) — sample de pixels no centro
                        val accentColor = extractDominantColor(bmp)

                        withContext(Dispatchers.Main) {
                            if (vh.adapterPosition == position) {
                                vh.thumb.setImageBitmap(bmp)

                                // Linha de accent com a cor dominante
                                accentBar?.setBackgroundColor(accentColor)

                                // No modo escuro: fundo do textWrap adapta levemente com a cor
                                if (isDark) {
                                    val r = Color.red(accentColor)
                                    val g = Color.green(accentColor)
                                    val b = Color.blue(accentColor)
                                    // Mistura muito subtil com o fundo escuro
                                    val blendedBg = Color.rgb(
                                        (r * 0.06f + 28 * 0.94f).toInt(),
                                        (g * 0.06f + 28 * 0.94f).toInt(),
                                        (b * 0.06f + 28 * 0.94f).toInt()
                                    )
                                    vh.textWrap.setBackgroundColor(blendedBg)
                                    // Card border com accent subtil
                                    val cardBg = vh.root.background as? android.graphics.drawable.GradientDrawable
                                    cardBg?.setStroke((1 * dp).toInt(), Color.argb(80,
                                        Color.red(accentColor),
                                        Color.green(accentColor),
                                        Color.blue(accentColor)
                                    ))
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        vh.root.setOnClickListener { onClick(item.sourceUrl) }
    }

    // Extrai cor dominante por sampling sem biblioteca externa
    private fun extractDominantColor(bmp: Bitmap): Int {
        val scaled = Bitmap.createScaledBitmap(bmp, 24, 24, true)
        val pixels = IntArray(24 * 24)
        scaled.getPixels(pixels, 0, 24, 0, 0, 24, 24)
        scaled.recycle()

        var rSum = 0L; var gSum = 0L; var bSum = 0L; var count = 0
        for (px in pixels) {
            val r = Color.red(px); val g = Color.green(px); val b = Color.blue(px)
            // Ignora pixels muito escuros ou muito claros
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
            if (luminance > 30 && luminance < 225) {
                rSum += r; gSum += g; bSum += b; count++
            }
        }
        if (count == 0) return Color.parseColor("#007AFF")
        // Boost de saturação para a cor ficar mais viva
        val avgR = (rSum / count).toInt()
        val avgG = (gSum / count).toInt()
        val avgB = (bSum / count).toInt()
        val max = maxOf(avgR, avgG, avgB).coerceAtLeast(1)
        val factor = 255f / max
        return Color.rgb(
            (avgR * factor).toInt().coerceIn(0, 255),
            (avgG * factor).toInt().coerceIn(0, 255),
            (avgB * factor).toInt().coerceIn(0, 255)
        )
    }

    override fun getItemCount() = items.size
}