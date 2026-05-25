package com.wilin.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wilin.app.R
import com.wilin.app.databinding.FragmentHomeBinding

data class SiteItem(
    val label: String,
    val url: String,
    val iconAsset: String,
    val isMore: Boolean = false
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sites = mutableListOf(
        SiteItem("Google",     "https://google.com",            "icons/png/google.png"),
        SiteItem("YouTube",    "https://youtube.com",           "icons/png/youtube.png"),
        SiteItem("Facebook",   "https://facebook.com",          "icons/png/facebook.png"),
        SiteItem("Instagram",  "https://instagram.com",         "icons/png/instagram.png"),
        SiteItem("WhatsApp",   "https://web.whatsapp.com",      "icons/png/whatsapp.png"),
        SiteItem("X",          "https://x.com",                 "icons/png/x.png"),
        SiteItem("ChatGPT",    "https://chat.openai.com",       "icons/png/chatgpt.png"),
        SiteItem("TikTok",     "https://tiktok.com",            "icons/png/tiktok.png"),
        SiteItem("Reddit",     "https://reddit.com",            "icons/png/reddit.png"),
        SiteItem("Mais",       "",                              "", isMore = true)
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
            if (!item.isMore) {
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
        val dp  = ctx.resources.displayMetrics.density

        val root = LinearLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER_HORIZONTAL
            setPadding(0, (10 * dp).toInt(), 0, (10 * dp).toInt())
            isClickable = true
            isFocusable = true
            background  = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)
        }

        val iconSize = (52 * dp).toInt()
        val icon = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
            scaleType    = ImageView.ScaleType.FIT_CENTER
        }

        val label = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (6 * dp).toInt() }
            textSize  = 11f
            maxLines  = 1
            gravity   = Gravity.CENTER_HORIZONTAL
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        root.addView(icon)
        root.addView(label)
        return VH(root, icon, label)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item     = items[position]
        val ctx      = holder.root.context
        val dp       = ctx.resources.displayMetrics.density
        val iconSize = (52 * dp).toInt()

        holder.label.text = item.label

        if (item.isMore) {
            holder.icon.setImageBitmap(makeMoreBitmap(iconSize))
        } else {
            holder.icon.setImageBitmap(makeCirclePlaceholder(iconSize))
            try {
                val stream  = ctx.assets.open(item.iconAsset)
                val decoded = BitmapFactory.decodeStream(stream)
                stream.close()
                holder.icon.setImageBitmap(toCircle(decoded, iconSize))
            } catch (_: Exception) {}
        }

        holder.root.setOnClickListener { onClick(item) }
    }

    // Recorta qualquer bitmap num círculo perfeito
    private fun toCircle(src: Bitmap, size: Int): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)

        // Máscara circular
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Aplica o PNG sobre a máscara
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val scaled = Bitmap.createScaledBitmap(src, size, size, true)
        canvas.drawBitmap(scaled, 0f, 0f, paint)

        return output
    }

    private fun makeCirclePlaceholder(size: Int): Bitmap {
        val bmp    = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#E5E5EA")
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return bmp
    }

    private fun makeMoreBitmap(size: Int): Bitmap {
        val bmp    = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#E5E5EA")
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.color = Color.parseColor("#888888")
        val dotR = size * 0.08f
        val off  = size * 0.25f
        val cx   = size / 2f
        val cy   = size / 2f
        for (row in 0..1) for (col in 0..1) {
            canvas.drawCircle(cx - off + col * off * 2, cy - off + row * off * 2, dotR, paint)
        }
        return bmp
    }

    override fun getItemCount() = items.size
}