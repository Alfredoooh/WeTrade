package com.wilin.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.palette.graphics.Palette
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
        val thumb: ImageView = root.findViewWithTag("thumb")
        val title: TextView  = root.findViewWithTag("title")
        val desc: TextView   = root.findViewWithTag("desc")
        val source: TextView = root.findViewWithTag("source")
        val textWrap: LinearLayout = root.findViewWithTag("textWrap")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val dp  = ctx.resources.displayMetrics.density

        val card = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also {
                it.bottomMargin = (12 * dp).toInt()
                it.marginStart  = (16 * dp).toInt()
                it.marginEnd    = (16 * dp).toInt()
            }
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                shape        = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 14 * dp
                setColor(Color.WHITE)
            }
            elevation     = 3 * dp
            clipToOutline = true
            isClickable   = true
            isFocusable   = true
        }

        val thumb = ImageView(ctx).apply {
            tag = "thumb"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (190 * dp).toInt()
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.parseColor("#F2F2F7"))
        }

        val textWrap = LinearLayout(ctx).apply {
            tag = "textWrap"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding((14 * dp).toInt(), (12 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt())
            setBackgroundColor(Color.WHITE)
        }

        val titleTv = TextView(ctx).apply {
            tag = "title"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 15f; maxLines = 2
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#1C1C1E"))
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val descTv = TextView(ctx).apply {
            tag = "desc"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (4 * dp).toInt() }
            textSize = 13f; maxLines = 2
            setTextColor(Color.parseColor("#636366"))
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        val sourceTv = TextView(ctx).apply {
            tag = "source"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (8 * dp).toInt() }
            textSize = 11f
            setTextColor(Color.parseColor("#8E8E93"))
        }

        textWrap.addView(titleTv)
        textWrap.addView(descTv)
        textWrap.addView(sourceTv)
        card.addView(thumb)
        card.addView(textWrap)

        return VH(card)
    }

    override fun onBindViewHolder(vh: VH, position: Int) {
        val item = items[position]
        vh.title.text  = item.title
        vh.desc.text   = item.description
        vh.source.text = item.sourceName

        // Reset para defaults enquanto carrega
        vh.thumb.setImageBitmap(null)
        vh.thumb.setBackgroundColor(Color.parseColor("#F2F2F7"))
        vh.textWrap.setBackgroundColor(Color.WHITE)
        vh.title.setTextColor(Color.parseColor("#1C1C1E"))
        vh.desc.setTextColor(Color.parseColor("#636366"))
        vh.source.setTextColor(Color.parseColor("#8E8E93"))

        val cardBg = vh.root.background as? android.graphics.drawable.GradientDrawable
        cardBg?.setColor(Color.WHITE)

        if (item.imageUrl.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val req  = Request.Builder().url(item.imageUrl).build()
                    val resp = http.newCall(req).execute()
                    val bmp  = resp.body?.byteStream()?.let { BitmapFactory.decodeStream(it) }

                    if (bmp != null) {
                        // Palette — extrai cor dominante
                        val palette = Palette.from(bmp).generate()
                        val dominant = palette.getDominantColor(Color.WHITE)
                        val vibrant  = palette.getVibrantColor(dominant)
                        val muted    = palette.getMutedColor(dominant)
                        val base     = if (vibrant != dominant) vibrant else muted

                        // Versão clara do base para o fundo do textWrap
                        val r = Color.red(base); val g = Color.green(base); val b = Color.blue(base)
                        // Mistura com branco a 88% para ficar subtil
                        val bgColor = Color.rgb(
                            (r * 0.12f + 255 * 0.88f).toInt(),
                            (g * 0.12f + 255 * 0.88f).toInt(),
                            (b * 0.12f + 255 * 0.88f).toInt()
                        )
                        // Cor do texto: se fundo escuro usa branco, se claro usa escuro
                        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                        val titleColor  = if (luminance < 0.35) Color.WHITE else Color.parseColor("#1C1C1E")
                        val descColor   = if (luminance < 0.35) Color.argb(200, 255, 255, 255) else Color.parseColor("#636366")
                        val sourceColor = if (luminance < 0.35) Color.argb(160, 255, 255, 255) else Color.parseColor("#8E8E93")
                        // Accent para a borda esquerda do source
                        val accentColor = base

                        withContext(Dispatchers.Main) {
                            if (vh.adapterPosition == position) {
                                vh.thumb.setImageBitmap(bmp)
                                // Card bg com a cor base muito suave
                                cardBg?.setColor(bgColor)
                                vh.textWrap.setBackgroundColor(bgColor)
                                vh.title.setTextColor(titleColor)
                                vh.desc.setTextColor(descColor)
                                vh.source.setTextColor(sourceColor)
                                // Accent strip à esquerda do source via paddingStart e linha de cor
                                vh.source.setShadowLayer(0f, 0f, 0f, accentColor)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        vh.root.setOnClickListener { onClick(item.sourceUrl) }
    }

    override fun getItemCount() = items.size
}