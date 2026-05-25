package com.wilin.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wilin.app.R
import com.wilin.app.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class SiteItem(
    val label: String,
    val url: String,
    val faviconUrl: String,
    val isMore: Boolean = false
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sites = mutableListOf(
        SiteItem("Google",     "https://google.com",       "https://www.google.com/favicon.ico"),
        SiteItem("YouTube",    "https://youtube.com",      "https://www.youtube.com/favicon.ico"),
        SiteItem("Facebook",   "https://facebook.com",     "https://www.facebook.com/favicon.ico"),
        SiteItem("WeScore",    "https://wescore.com",      "https://wescore.com/favicon.ico"),
        SiteItem("X",          "https://x.com",            "https://abs.twimg.com/favicons/twitter.3.ico"),
        SiteItem("BantuBet",   "https://bantubet.com",     "https://bantubet.com/favicon.ico"),
        SiteItem("PremierBet", "https://premierbet.co.mz", "https://premierbet.co.mz/favicon.ico"),
        SiteItem("Instagram",  "https://instagram.com",    "https://www.instagram.com/favicon.ico"),
        SiteItem("WhatsApp",   "https://web.whatsapp.com", "https://web.whatsapp.com/favicon.ico"),
        SiteItem("Mais",       "",                         "", isMore = true)
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
        val adapter = SitesAdapter(sites, viewLifecycleOwner.lifecycleScope) { item ->
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

    private fun showMoreSites() {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class SitesAdapter(
    private val items: List<SiteItem>,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val onClick: (SiteItem) -> Unit
) : RecyclerView.Adapter<SitesAdapter.VH>() {

    private val httpClient = OkHttpClient()

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

        // Placeholder: círculo cinza claro vazio
        holder.icon.setImageBitmap(makeCirclePlaceholder(iconSize))

        if (item.isMore) {
            // Círculo cinza com 4 dots
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
        } else if (item.faviconUrl.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                try {
                    val request = Request.Builder().url(item.faviconUrl).build()
                    val response = httpClient.newCall(request).execute()
                    val bytes = response.body?.bytes() ?: return@launch
                    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        ?: return@launch

                    // Escala o favicon para caber bem dentro do círculo
                    val faviconSize = (iconSize * 0.65f).toInt()
                    val scaled = Bitmap.createScaledBitmap(decoded, faviconSize, faviconSize, true)

                    // Compõe: círculo branco + favicon centrado com clip circular
                    val final = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                    val c = Canvas(final)
                    val p = Paint(Paint.ANTI_ALIAS_FLAG)

                    // Fundo do círculo: branco puro (aparece bem em ambos os temas)
                    p.color = Color.parseColor("#F2F2F7")
                    c.drawCircle(iconSize / 2f, iconSize / 2f, iconSize / 2f, p)

                    // Favicon centrado sem distorção
                    val left = (iconSize - scaled.width) / 2f
                    val top  = (iconSize - scaled.height) / 2f
                    c.drawBitmap(scaled, left, top, null)

                    withContext(Dispatchers.Main) {
                        holder.icon.setImageBitmap(final)
                    }
                } catch (_: Exception) {}
            }
        }

        holder.root.setOnClickListener { onClick(item) }
    }

    private fun makeCirclePlaceholder(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#E5E5EA")
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return bmp
    }

    override fun getItemCount() = items.size
}