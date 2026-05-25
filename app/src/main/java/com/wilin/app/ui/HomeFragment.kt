package com.wilin.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.wilin.app.R
import com.wilin.app.databinding.FragmentHomeBinding

data class SiteItem(
    val label: String,
    val url: String,
    val faviconUrl: String,
    val bgColor: String,   // cor hex do círculo
    val isMore: Boolean = false
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sites = mutableListOf(
        SiteItem("Google",     "https://google.com",     "https://www.google.com/favicon.ico",         "#FFFFFF"),
        SiteItem("YouTube",    "https://youtube.com",    "https://www.youtube.com/favicon.ico",         "#FF0000"),
        SiteItem("Facebook",   "https://facebook.com",   "https://www.facebook.com/favicon.ico",        "#1877F2"),
        SiteItem("WeScore",    "https://wescore.com",    "https://wescore.com/favicon.ico",             "#2ECC40"),
        SiteItem("X",          "https://x.com",          "https://abs.twimg.com/favicons/twitter.3.ico","#000000"),
        SiteItem("BantuBet",   "https://bantubet.com",   "https://bantubet.com/favicon.ico",            "#E8521A"),
        SiteItem("PremierBet", "https://premierbet.co.mz","https://premierbet.co.mz/favicon.ico",      "#1B5E20"),
        SiteItem("Instagram",  "https://instagram.com",  "https://www.instagram.com/favicon.ico",       "#C13584"),
        SiteItem("WhatsApp",   "https://web.whatsapp.com","https://web.whatsapp.com/favicon.ico",       "#25D366"),
        SiteItem("Mais",       "",                       "",                                            "#888888", isMore = true)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGrid()
    }

    private fun setupGrid() {
        val adapter = SitesAdapter(sites) { item ->
            if (item.isMore) {
                showMoreSites()
            } else {
                val intent = Intent(requireContext(), BrowserResponseActivity::class.java).apply {
                    putExtra(BrowserResponseActivity.EXTRA_QUERY, item.url)
                }
                startActivity(intent)
                requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
        binding.sitesGrid.layoutManager = GridLayoutManager(requireContext(), 5)
        binding.sitesGrid.adapter = adapter
    }

    private fun showMoreSites() {
        // Pode ser expandido para mostrar mais sites num bottom sheet futuramente
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class SitesAdapter(
    private val items: List<SiteItem>,
    private val onClick: (SiteItem) -> Unit
) : RecyclerView.Adapter<SitesAdapter.VH>() {

    inner class VH(val root: LinearLayout, val icon: ImageView, val label: TextView) :
        RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density

        val root = LinearLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, (10 * dp).toInt(), 0, (10 * dp).toInt())
            isClickable = true
            isFocusable = true
            background = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)
        }

        val iconSize = (48 * dp).toInt()
        val icon = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val label = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (6 * dp).toInt() }
            textSize = 11f
            maxLines = 1
            gravity = Gravity.CENTER_HORIZONTAL
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        root.addView(icon)
        root.addView(label)
        return VH(root, icon, label)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx = holder.root.context
        val dp = ctx.resources.displayMetrics.density
        val iconSize = (48 * dp).toInt()

        holder.label.text = item.label

        if (item.isMore) {
            // Círculo cinza com grade de 4 ícones pequenos (dots)
            val bmp = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = Color.parseColor("#E5E5EA")
            canvas.drawCircle(iconSize / 2f, iconSize / 2f, iconSize / 2f, paint)
            paint.color = Color.parseColor("#888888")
            val dotR = iconSize * 0.08f
            val off  = iconSize * 0.25f
            val cx   = iconSize / 2f
            val cy   = iconSize / 2f
            for (row in 0..1) for (col in 0..1) {
                canvas.drawCircle(cx - off + col * off * 2, cy - off + row * off * 2, dotR, paint)
            }
            holder.icon.setImageBitmap(bmp)
        } else {
            val bgColor = try { Color.parseColor(item.bgColor) } catch (e: Exception) { Color.LTGRAY }

            // Círculo de fundo com a cor da marca
            val circleBmp = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
            val circleCanvas = Canvas(circleBmp)
            val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            circlePaint.color = bgColor
            circleCanvas.drawCircle(iconSize / 2f, iconSize / 2f, iconSize / 2f, circlePaint)
            holder.icon.setImageBitmap(circleBmp)

            // Carrega o favicon por cima
            if (item.faviconUrl.isNotEmpty()) {
                val faviconSize = (iconSize * 0.55f).toInt()
                Glide.with(ctx)
                    .asBitmap()
                    .load(item.faviconUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(object : CustomTarget<Bitmap>(faviconSize, faviconSize) {
                        override fun onResourceReady(resource: Bitmap, t: Transition<in Bitmap>?) {
                            // Compõe: círculo de fundo + favicon centrado
                            val final = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                            val c = Canvas(final)
                            val p = Paint(Paint.ANTI_ALIAS_FLAG)
                            p.color = bgColor
                            c.drawCircle(iconSize / 2f, iconSize / 2f, iconSize / 2f, p)
                            val left = (iconSize - resource.width) / 2f
                            val top  = (iconSize - resource.height) / 2f
                            c.drawBitmap(resource, left, top, null)
                            holder.icon.setImageBitmap(final)
                        }
                        override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                    })
            }
        }

        holder.root.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size
}