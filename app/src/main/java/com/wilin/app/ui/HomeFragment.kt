package com.wilin.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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
    val sourceName: String,
    val faviconUrl: String = "",
    val category: String = ""
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

    private val newsCategories    = listOf("Mundo", "Tecnologia", "Saúde", "Desporto", "Ciência", "Entretenimento")
    private val newsCategoryKeys  = listOf("world", "technology", "health", "sports", "science", "entertainment")
    private var selectedCategoryIndex = 0
    private val newsItems = mutableListOf<NewsItem>()
    private lateinit var newsAdapter: NewsAdapter

    // URL da tua API no Render — substitui pelo teu URL real
    private val NEWS_API_BASE = "https://wetrade-news-api.onrender.com"
    // Fallback: newsdata.io
    private val NEWSDATA_KEY  = "pub_7d7d1ac2f86b4bc6b4662fd5d6dad47c"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val mainToggleChips = mutableListOf<TextView>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        buildCategoryToggles()

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
            if (selected) setColor(ContextCompat.getColor(ctx, R.color.colorPrimary))
            else { setColor(0); setStroke(1, ContextCompat.getColor(ctx, R.color.divider)) }
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

    /**
     * Tenta primeiro a API do Render. Se falhar, cai no newsdata.io.
     * Carrega 20 notícias por categoria.
     */
    private fun fetchNews(category: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val fetched = mutableListOf<NewsItem>()
            try {
                // Tenta API Render
                val renderUrl = "$NEWS_API_BASE/news?category=$category&lang=pt&limit=20"
                val req  = Request.Builder().url(renderUrl).build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val arr  = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        fetched.add(parseRenderItem(obj))
                    }
                }
            } catch (_: Exception) {}

            // Fallback newsdata.io se não trouxe nada
            if (fetched.isEmpty()) {
                try {
                    val url  = "https://newsdata.io/api/1/news?apikey=$NEWSDATA_KEY&language=pt&category=$category&size=20"
                    val req  = Request.Builder().url(url).build()
                    val resp = httpClient.newCall(req).execute()
                    val body = resp.body?.string() ?: ""
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results") ?: JSONArray()
                    for (i in 0 until results.length()) {
                        val obj   = results.getJSONObject(i)
                        val title = obj.optString("title", "")
                        val desc  = obj.optString("description", "")
                        val img   = obj.optString("image_url", "")
                        val link  = obj.optString("link", "")
                        val src   = obj.optString("source_id", "")
                        val srcUrl = obj.optString("source_url", "")
                        val host  = extractHost(srcUrl.ifEmpty { link })
                        val favicon = if (host.isNotEmpty()) "https://www.google.com/s2/favicons?domain=$host&sz=32" else ""
                        if (title.isNotEmpty()) {
                            fetched.add(NewsItem(title, desc, img, link, src, favicon, category))
                        }
                    }
                } catch (_: Exception) {}
            }

            withContext(Dispatchers.Main) {
                newsItems.clear()
                newsItems.addAll(fetched)
                newsAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun parseRenderItem(obj: JSONObject): NewsItem {
        val title   = obj.optString("title", "")
        val desc    = obj.optString("description", "")
        val img     = obj.optString("image_url", "")
        val link    = obj.optString("url", "")
        val src     = obj.optString("source_name", "")
        val srcDomain = obj.optString("source_domain", "")
        val favicon = if (srcDomain.isNotEmpty())
            "https://www.google.com/s2/favicons?domain=$srcDomain&sz=32" else ""
        val cat = obj.optString("category", "")
        return NewsItem(title, desc, img, link, src, favicon, cat)
    }

    private fun extractHost(url: String): String =
        runCatching { android.net.Uri.parse(url).host ?: "" }.getOrDefault("")

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

        // Ícone menor: 40dp em vez de 52dp
        val iconSize = (40 * dp).toInt()
        // Container circular branco com padding
        val iconContainer = FrameLayout(ctx).apply {
            val containerSize = (48 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(containerSize, containerSize)
            // Fundo branco circular
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.WHITE)
            }
            // Sombra leve
            elevation = 2 * dp
        }

        val icon = ImageView(ctx).apply {
            val padding = (4 * dp).toInt()
            layoutParams = FrameLayout.LayoutParams(iconSize, iconSize, Gravity.CENTER)
            scaleType    = ImageView.ScaleType.FIT_CENTER
        }
        iconContainer.addView(icon)

        val label = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (5 * dp).toInt() }
            textSize  = 10f
            maxLines  = 1
            gravity   = Gravity.CENTER_HORIZONTAL
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        root.addView(iconContainer)
        root.addView(label)
        return VH(root, icon, label)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx  = holder.root.context
        val dp   = ctx.resources.displayMetrics.density
        val iconSize = (40 * dp).toInt()

        holder.label.text = item.label

        if (item.isMore) {
            holder.icon.setImageBitmap(makeMoreBitmap(iconSize))
        } else {
            holder.icon.setImageBitmap(makeCirclePlaceholder(iconSize))
            try {
                val stream  = ctx.assets.open(item.iconAsset)
                val decoded = BitmapFactory.decodeStream(stream)
                stream.close()
                // Sem cortar em círculo — deixa o PNG original com fundo branco do container
                val scaled = Bitmap.createScaledBitmap(decoded, iconSize, iconSize, true)
                holder.icon.setImageBitmap(scaled)
            } catch (_: Exception) {}
        }

        holder.root.setOnClickListener { onClick(item) }
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
        val cx   = size / 2f; val cy = size / 2f
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

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    inner class VH(
        val card: LinearLayout,
        val image: ImageView,
        val title: TextView,
        val desc: TextView,
        val sourceRow: LinearLayout,
        val faviconIv: ImageView,
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
                (180 * dp).toInt()
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        val textArea = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding((14 * dp).toInt(), (12 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt())
        }

        val title = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 15f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            // Texto sempre branco — estilo Perplexity
            setTextColor(Color.WHITE)
        }

        val desc = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (6 * dp).toInt() }
            textSize = 13f
            maxLines = 3
            ellipsize = android.text.TextUtils.TruncateAt.END
            // Descrição branca semi-transparente
            setTextColor(Color.argb(200, 255, 255, 255))
        }

        // Linha da fonte: favicon + nome
        val sourceRow = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (10 * dp).toInt() }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val faviconIv = ImageView(ctx).apply {
            val sz = (14 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz)
                .also { it.marginEnd = (6 * dp).toInt() }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val source = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 11f
            setTextColor(Color.argb(160, 255, 255, 255))
        }

        sourceRow.addView(faviconIv)
        sourceRow.addView(source)
        textArea.addView(title)
        textArea.addView(desc)
        textArea.addView(sourceRow)
        card.addView(image)
        card.addView(textArea)

        return VH(card, image, title, desc, sourceRow, faviconIv, source)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx  = holder.card.context
        val dp   = ctx.resources.displayMetrics.density

        holder.title.text  = item.title
        holder.desc.text   = item.description.ifEmpty { item.title }
        holder.source.text = item.sourceName.replaceFirstChar { it.uppercase() }

        // Reset
        holder.card.background = neutralCardBg(ctx, dp)
        holder.title.setTextColor(Color.WHITE)
        holder.desc.setTextColor(Color.argb(200, 255, 255, 255))
        holder.image.setImageDrawable(
            android.graphics.drawable.ColorDrawable(Color.parseColor("#2C2C2E"))
        )
        holder.faviconIv.setImageDrawable(null)

        holder.card.setOnClickListener { onClick(item.sourceUrl) }

        // Carrega favicon da fonte
        if (item.faviconUrl.isNotEmpty()) {
            val faviconUrl = item.faviconUrl
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val req  = Request.Builder().url(faviconUrl).build()
                    val resp = httpClient.newCall(req).execute()
                    val bytes = resp.body?.bytes() ?: return@launch
                    val bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@launch
                    withContext(Dispatchers.Main) {
                        if (holder.bindingAdapterPosition == position) {
                            holder.faviconIv.setImageBitmap(bmp)
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // Carrega imagem da notícia
        if (item.imageUrl.isNotEmpty()) {
            val adapterPos = position
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val req   = Request.Builder().url(item.imageUrl).build()
                    val resp  = httpClient.newCall(req).execute()
                    val bytes = resp.body?.bytes() ?: return@launch
                    val bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@launch
                    val dominant = dominantColor(bmp)
                    val isWhitish = isVeryLight(dominant)

                    withContext(Dispatchers.Main) {
                        if (holder.bindingAdapterPosition != adapterPos) return@withContext
                        holder.image.setImageBitmap(bmp)

                        // Se a cor dominante for muito clara (branco/cinza claro),
                        // força fundo escuro para não estragar o texto branco
                        val cardColor = if (isWhitish) {
                            Color.parseColor("#1E1E1E")
                        } else {
                            // Escurece a cor dominante para garantir texto legível
                            darkenColor(dominant, 0.55f)
                        }

                        holder.card.background = android.graphics.drawable.GradientDrawable().apply {
                            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                            cornerRadius = 12 * dp
                            setColor(withAlpha(cardColor, 0.95f))
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun neutralCardBg(ctx: android.content.Context, dp: Float) =
        android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 12 * dp
            // Card base sempre escuro para texto branco funcionar
            setColor(Color.parseColor("#1E1E1E"))
        }

    private fun dominantColor(bmp: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(bmp, 16, 16, true)
        var r = 0L; var g = 0L; var b = 0L
        val n = small.width * small.height
        for (x in 0 until small.width) for (y in 0 until small.height) {
            val c = small.getPixel(x, y)
            r += Color.red(c); g += Color.green(c); b += Color.blue(c)
        }
        return Color.rgb((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }

    /** Luminância > 0.70 = muito claro (branco/bege) */
    private fun isVeryLight(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        return 0.2126 * r + 0.7152 * g + 0.0722 * b > 0.70
    }

    /** Escurece uma cor multiplicando os canais por factor (0..1) */
    private fun darkenColor(color: Int, factor: Float): Int = Color.rgb(
        (Color.red(color)   * factor).toInt().coerceIn(0, 255),
        (Color.green(color) * factor).toInt().coerceIn(0, 255),
        (Color.blue(color)  * factor).toInt().coerceIn(0, 255)
    )

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    override fun getItemCount() = items.size
}