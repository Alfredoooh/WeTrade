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
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wilin.app.R
import com.wilin.app.databinding.FragmentHomeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class SiteItem(
    val label: String,
    val url: String,
    val iconAsset: String,
    val isMore: Boolean = false
)

data class NewsItem(
    val title: String,
    val description: String,
    val imageUrl: String,
    val sourceUrl: String,
    val sourceName: String
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sites = listOf(
        SiteItem("Google",    "https://google.com",       "icons/png/google.png"),
        SiteItem("YouTube",   "https://youtube.com",      "icons/png/youtube.png"),
        SiteItem("Facebook",  "https://facebook.com",     "icons/png/facebook.png"),
        SiteItem("Instagram", "https://instagram.com",    "icons/png/instagram.png"),
        SiteItem("WhatsApp",  "https://web.whatsapp.com", "icons/png/whatsapp.png"),
        SiteItem("X",         "https://x.com",            "icons/png/x.png"),
        SiteItem("ChatGPT",   "https://chat.openai.com",  "icons/png/chatgpt.png"),
        SiteItem("TikTok",    "https://tiktok.com",       "icons/png/tiktok.png"),
        SiteItem("Reddit",    "https://reddit.com",       "icons/png/reddit.png"),
        SiteItem("Mais",      "",                         "", isMore = true)
    )

    private val newsCategories = listOf("Mundo", "Tecnologia", "Saúde", "Desporto", "Ciência", "Entretenimento")
    private val newsCategoryKeys = listOf("world", "technology", "health", "sports", "science", "entertainment")
    private var selectedCategoryIndex = 0
    private val newsItems = mutableListOf<NewsItem>()
    private lateinit var newsAdapter: NewsAdapter
    private val httpClient = OkHttpClient()
    private val API_KEY = "pub_7d7d1ac2f86b4bc6b4662fd5d6dad47c"

    // Toggle chips — lista para actualizar estado visual
    private val mainToggleChips = mutableListOf<TextView>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Grid de sites
        binding.sitesGrid.layoutManager = GridLayoutManager(requireContext(), 5)
        binding.sitesGrid.adapter = SitesAdapter(sites) { item ->
            if (!item.isMore) {
                val intent = Intent(requireContext(), BrowserResponseActivity::class.java).apply {
                    putExtra(BrowserResponseActivity.EXTRA_QUERY, item.url)
                }
                startActivity(intent)
                requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }

        // Toggles de categoria
        buildCategoryToggles()

        // RecyclerView de notícias
        newsAdapter = NewsAdapter(newsItems) { url ->
            val intent = Intent(requireContext(), BrowserResponseActivity::class.java).apply {
                putExtra(BrowserResponseActivity.EXTRA_QUERY, url)
            }
            startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        binding.newsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.newsRecycler.adapter = newsAdapter
        binding.newsRecycler.isNestedScrollingEnabled = false

        fetchNews(newsCategoryKeys[0])
    }

    private fun buildCategoryToggles() {
        val ctx = requireContext()
        val dp  = ctx.resources.displayMetrics.density

        mainToggleChips.clear()
        binding.categoryToggleContainer.removeAllViews()

        newsCategories.forEachIndexed { i, label ->
            val chip = makeChip(label, i == selectedCategoryIndex, dp)
            chip.setOnClickListener { selectCategory(i) }
            mainToggleChips.add(chip)
            binding.categoryToggleContainer.addView(chip)
        }
    }

    private fun makeChip(label: String, selected: Boolean, dp: Float): TextView {
        val ctx = requireContext()
        return TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (32 * dp).toInt()
            ).also { it.marginEnd = (8 * dp).toInt() }
            text = label
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding((14 * dp).toInt(), 0, (14 * dp).toInt(), 0)
            isClickable = true
            isFocusable = true
            applyChipStyle(this, selected, dp)
        }
    }

    private fun applyChipStyle(chip: TextView, selected: Boolean, dp: Float) {
        val ctx = requireContext()
        val bg = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 16 * dp
            if (selected) {
                setColor(ContextCompat.getColor(ctx, R.color.colorPrimary))
            } else {
                setColor(0)
                setStroke(1, ContextCompat.getColor(ctx, R.color.divider))
            }
        }
        chip.background = bg
        chip.setTextColor(
            if (selected) Color.WHITE
            else ContextCompat.getColor(ctx, R.color.text_secondary)
        )
        chip.setTypeface(chip.typeface,
            if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }

    private fun selectCategory(index: Int) {
        if (index == selectedCategoryIndex) return
        val dp = requireContext().resources.displayMetrics.density
        applyChipStyle(mainToggleChips[selectedCategoryIndex], false, dp)
        applyChipStyle(mainToggleChips[index], true, dp)
        selectedCategoryIndex = index
        newsItems.clear()
        newsAdapter.notifyDataSetChanged()
        fetchNews(newsCategoryKeys[index])
    }

    private fun fetchNews(category: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://newsdata.io/api/1/news?apikey=$API_KEY&language=pt&category=$category&size=10"
                val req  = Request.Builder().url(url).build()
                val resp = httpClient.newCall(req).execute()
                val body = resp.body?.string() ?: return@launch
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@launch
                val fetched = mutableListOf<NewsItem>()
                for (i in 0 until results.length()) {
                    val obj   = results.getJSONObject(i)
                    val title = obj.optString("title", "")
                    val desc  = obj.optString("description", "")
                    val img   = obj.optString("image_url", "")
                    val link  = obj.optString("link", "")
                    val src   = obj.optString("source_id", "")
                    if (title.isNotEmpty()) fetched.add(NewsItem(title, desc, img, link, src))
                }
                withContext(Dispatchers.Main) {
                    newsItems.clear()
                    newsItems.addAll(fetched)
                    newsAdapter.notifyDataSetChanged()
                }
            } catch (_: Exception) {}
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// ── SitesAdapter ──────────────────────────────────────────────────────────────

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

    private fun toCircle(src: Bitmap, size: Int): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint  = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
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
        for (row in 0..1) for (col in 0..1)
            canvas.drawCircle(cx - off + col * off * 2, cy - off + row * off * 2, dotR, paint)
        return bmp
    }

    override fun getItemCount() = items.size
}

// ── NewsAdapter ───────────────────────────────────────────────────────────────

class NewsAdapter(
    private val items: List<NewsItem>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<NewsAdapter.VH>() {

    inner class VH(
        val card: LinearLayout,
        val image: ImageView,
        val title: TextView,
        val desc: TextView,
        val source: TextView
    ) : RecyclerView.ViewHolder(card)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val dp  = ctx.resources.displayMetrics.density

        val card = LinearLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).also {
                it.marginStart  = (16 * dp).toInt()
                it.marginEnd    = (16 * dp).toInt()
                it.bottomMargin = (12 * dp).toInt()
            }
            orientation   = LinearLayout.VERTICAL
            clipToOutline = true
            isClickable   = true
            isFocusable   = true
            elevation     = (2 * dp)
            background    = neutralCardBg(ctx, dp)
        }

        val image = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (160 * dp).toInt()
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val textArea = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding((12 * dp).toInt(), (10 * dp).toInt(), (12 * dp).toInt(), (12 * dp).toInt())
        }

        val title = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 14f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        val desc = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (4 * dp).toInt() }
            textSize = 12f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
        }

        val source = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (6 * dp).toInt() }
            textSize = 11f
            setTextColor(ContextCompat.getColor(ctx, R.color.colorPrimary))
        }

        textArea.addView(title)
        textArea.addView(desc)
        textArea.addView(source)
        card.addView(image)
        card.addView(textArea)

        return VH(card, image, title, desc, source)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx  = holder.card.context
        val dp   = ctx.resources.displayMetrics.density

        holder.title.text  = item.title
        holder.desc.text   = item.description
        holder.source.text = item.sourceName

        // Reset card para estado neutro antes de carregar imagem
        holder.card.background = neutralCardBg(ctx, dp)
        holder.title.setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        holder.desc.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
        holder.image.setImageDrawable(
            android.graphics.drawable.ColorDrawable(Color.parseColor("#E8E8E8"))
        )

        holder.card.setOnClickListener { onClick(item.sourceUrl) }

        if (item.imageUrl.isNotEmpty()) {
            val adapterPos = position
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val req   = Request.Builder().url(item.imageUrl).build()
                    val resp  = OkHttpClient().newCall(req).execute()
                    val bytes = resp.body?.bytes() ?: return@launch
                    val bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@launch
                    val dominant = dominantColor(bmp)

                    withContext(Dispatchers.Main) {
                        if (holder.bindingAdapterPosition != adapterPos) return@withContext

                        holder.image.setImageBitmap(bmp)

                        // Fundo do card = cor dominante com alta opacidade
                        val cardBg = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = 12 * dp
                            setColor(withAlpha(dominant, 0.92f))
                        }
                        holder.card.background = cardBg

                        // Texto adapta-se ao fundo
                        val onColor = if (isDark(dominant)) Color.WHITE else Color.BLACK
                        val onColorSec = if (isDark(dominant))
                            Color.argb(180, 255, 255, 255)
                        else
                            Color.argb(160, 0, 0, 0)

                        holder.title.setTextColor(onColor)
                        holder.desc.setTextColor(onColorSec)
                        holder.source.setTextColor(
                            if (isDark(dominant)) Color.argb(200, 255, 255, 255) else
                                ContextCompat.getColor(ctx, R.color.colorPrimary)
                        )
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun neutralCardBg(ctx: android.content.Context, dp: Float) =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 12 * dp
            setColor(ContextCompat.getColor(ctx, R.color.card_background))
        }

    /** Média simples dos pixels num bitmap reduzido a 16×16 */
    private fun dominantColor(bmp: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(bmp, 16, 16, true)
        var r = 0L; var g = 0L; var b = 0L; val n = small.width * small.height
        for (x in 0 until small.width) for (y in 0 until small.height) {
            val c = small.getPixel(x, y)
            r += Color.red(c); g += Color.green(c); b += Color.blue(c)
        }
        return Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }

    private fun isDark(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b < 0.45
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    override fun getItemCount() = items.size
}