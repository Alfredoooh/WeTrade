package com.wilin.app.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
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

    private val sites = mutableListOf(
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

    private val newsCategories = listOf("mundo", "tecnologia", "saúde", "desporto", "ciência", "entretenimento")
    private val newsCategoryKeys = listOf("world", "technology", "health", "sports", "science", "entertainment")
    private var selectedCategoryIndex = 0
    private val newsItems = mutableListOf<NewsItem>()
    private var newsAdapter: NewsAdapter? = null
    private var newsRecycler: RecyclerView? = null
    private val httpClient = OkHttpClient()
    private val API_KEY = "pub_7d7d1ac2f86b4bc6b4662fd5d6dad47c"

    // Views para sticky
    private var stickyTogglesContainer: HorizontalScrollView? = null
    private var stickyTogglesInner: LinearLayout? = null
    private var toggleButtons = mutableListOf<TextView>()
    private var stickyBarShown = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildUI()
    }

    private fun buildUI() {
        val ctx = requireContext()
        val dp  = ctx.resources.displayMetrics.density

        // Root é um FrameLayout para poder sobrepor sticky
        val rootFrame = FrameLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // NestedScrollView principal
        val scrollView = NestedScrollView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isFillViewport = true
        }

        val contentLinear = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(0, (16 * dp).toInt(), 0, (32 * dp).toInt())
        }

        // Grid de sites
        val sitesRecycler = RecyclerView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding((8 * dp).toInt(), 0, (8 * dp).toInt(), 0)
            clipToPadding = false
            isNestedScrollingEnabled = false
            layoutManager = GridLayoutManager(ctx, 5)
        }
        val sitesAdapter = SitesAdapter(sites) { item ->
            if (!item.isMore) {
                val intent = Intent(requireContext(), BrowserResponseActivity::class.java).apply {
                    putExtra(BrowserResponseActivity.EXTRA_QUERY, item.url)
                }
                startActivity(intent)
                requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }
        sitesRecycler.adapter = sitesAdapter
        contentLinear.addView(sitesRecycler)

        // Separador
        val sep = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
            ).also { it.topMargin = (20 * dp).toInt(); it.bottomMargin = 0 }
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.divider))
        }
        contentLinear.addView(sep)

        // Título Notícias
        val newsTitle = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (16 * dp).toInt(); it.bottomMargin = (8 * dp).toInt() }
            text = "Notícias"
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding((16 * dp).toInt(), 0, (16 * dp).toInt(), 0)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }
        contentLinear.addView(newsTitle)

        // Toggles de categoria (no scroll)
        val togglesScroll = buildTogglesScroll(dp)
        contentLinear.addView(togglesScroll)

        // RecyclerView de notícias
        newsRecycler = RecyclerView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (8 * dp).toInt() }
            isNestedScrollingEnabled = false
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(ctx)
        }
        newsAdapter = NewsAdapter(newsItems)
        newsRecycler!!.adapter = newsAdapter
        contentLinear.addView(newsRecycler)

        scrollView.addView(contentLinear)

        // Sticky toggles (ocultos inicialmente, sobrepostos no topo)
        stickyTogglesContainer = buildStickyToggles(dp)
        stickyTogglesContainer!!.visibility = View.GONE

        rootFrame.addView(scrollView)
        rootFrame.addView(stickyTogglesContainer)

        // Substituir conteúdo do fragment
        (binding.root as? ViewGroup)?.let {
            if (it is FrameLayout) {
                it.removeAllViews()
                it.addView(rootFrame)
            }
        }

        // Sticky scroll listener
        scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            val togglesY = getTogglesScrollY(sitesRecycler, sep, newsTitle, dp)
            if (scrollY >= togglesY && !stickyBarShown) {
                stickyBarShown = true
                stickyTogglesContainer?.visibility = View.VISIBLE
            } else if (scrollY < togglesY && stickyBarShown) {
                stickyBarShown = false
                stickyTogglesContainer?.visibility = View.GONE
            }
        })

        fetchNews(newsCategoryKeys[selectedCategoryIndex])
    }

    private fun getTogglesScrollY(grid: RecyclerView, sep: View, title: TextView, dp: Float): Int {
        // Aprox: altura do grid + sep + titulo (sem medir exatamente, usa estimativa)
        val gridH = grid.height
        val sepH  = (1 * dp).toInt()
        val titleH = title.height
        val topPad = (16 * dp).toInt()
        val margins = (20 + 16 + 8).toInt()
        return topPad + gridH + sepH + (margins * dp).toInt() + titleH
    }

    private fun buildTogglesScroll(dp: Float): HorizontalScrollView {
        val ctx = requireContext()
        val scroll = HorizontalScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
        }
        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
        }

        newsCategories.forEachIndexed { i, cat ->
            val tv = buildToggleChip(cat, i == selectedCategoryIndex, dp)
            tv.setOnClickListener { selectCategory(i) }
            toggleButtons.add(tv)
            inner.addView(tv)
        }

        scroll.addView(inner)
        return scroll
    }

    private fun buildStickyToggles(dp: Float): HorizontalScrollView {
        val ctx = requireContext()
        val scroll = HorizontalScrollView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).also { it.gravity = Gravity.TOP }
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.appbar_background))
            elevation = 4f * dp
        }
        stickyTogglesInner = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), (8 * dp).toInt())
        }

        newsCategories.forEachIndexed { i, cat ->
            val tv = buildToggleChip(cat, i == selectedCategoryIndex, dp)
            tv.setOnClickListener { selectCategory(i) }
            stickyTogglesInner!!.addView(tv)
        }

        scroll.addView(stickyTogglesInner)
        return scroll
    }

    private fun buildToggleChip(label: String, selected: Boolean, dp: Float): TextView {
        val ctx = requireContext()
        return TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (32 * dp).toInt()
            ).also { it.marginEnd = (8 * dp).toInt() }
            text = label.replaceFirstChar { it.uppercase() }
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding((14 * dp).toInt(), 0, (14 * dp).toInt(), 0)
            isClickable = true
            isFocusable = true
            setToggleStyle(this, selected, dp)
        }
    }

    private fun setToggleStyle(tv: TextView, selected: Boolean, dp: Float) {
        val ctx = requireContext()
        if (selected) {
            tv.setBackgroundColor(0) // limpar antes
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 16 * dp
                setColor(ContextCompat.getColor(ctx, R.color.colorPrimary))
            }
            tv.background = bg
            tv.setTextColor(Color.WHITE)
            tv.setTypeface(tv.typeface, android.graphics.Typeface.BOLD)
        } else {
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 16 * dp
                setColor(ContextCompat.getColor(ctx, R.color.card_background))
            }
            tv.background = bg
            tv.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            tv.setTypeface(tv.typeface, android.graphics.Typeface.NORMAL)
        }
    }

    private fun selectCategory(index: Int) {
        if (index == selectedCategoryIndex) return
        val dp = requireContext().resources.displayMetrics.density

        // Atualiza toggles principais
        toggleButtons.getOrNull(selectedCategoryIndex)?.let { setToggleStyle(it, false, dp) }
        toggleButtons.getOrNull(index)?.let { setToggleStyle(it, true, dp) }

        // Atualiza toggles sticky
        val stickyChildren = stickyTogglesInner?.let {
            (0 until it.childCount).map { i -> it.getChildAt(i) as? TextView }
        }
        stickyChildren?.getOrNull(selectedCategoryIndex)?.let { setToggleStyle(it, false, dp) }
        stickyChildren?.getOrNull(index)?.let { setToggleStyle(it, true, dp) }

        selectedCategoryIndex = index
        newsItems.clear()
        newsAdapter?.notifyDataSetChanged()
        fetchNews(newsCategoryKeys[index])
    }

    private fun fetchNews(category: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://newsdata.io/api/1/news?apikey=$API_KEY&language=pt&category=$category&size=10"
                val req = Request.Builder().url(url).build()
                val resp = httpClient.newCall(req).execute()
                val body = resp.body?.string() ?: return@launch
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@launch

                val fetched = mutableListOf<NewsItem>()
                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    val title = obj.optString("title", "")
                    val desc  = obj.optString("description", "")
                    val img   = obj.optString("image_url", "")
                    val link  = obj.optString("link", "")
                    val src   = obj.optJSONObject("source_icon")?.optString("name") ?: obj.optString("source_id", "")
                    if (title.isNotEmpty()) {
                        fetched.add(NewsItem(title, desc, img, link, src))
                    }
                }

                withContext(Dispatchers.Main) {
                    newsItems.clear()
                    newsItems.addAll(fetched)
                    newsAdapter?.notifyDataSetChanged()
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
            // Ícone add.svg para "Mais"
            try {
                val px  = iconSize
                val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                // fundo cinzento circular
                val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E5E5EA") }
                canvas.drawCircle(px / 2f, px / 2f, px / 2f, bgPaint)
                // SVG add no centro
                val svg = SVG.getFromAsset(ctx.assets, "icons/svg/add.svg")
                val svgSize = (px * 0.45f).toInt()
                val offset = (px - svgSize) / 2f
                svg.documentWidth  = svgSize.toFloat()
                svg.documentHeight = svgSize.toFloat()
                canvas.save()
                canvas.translate(offset, offset)
                svg.renderToCanvas(canvas)
                canvas.restore()
                // Tint cinzento
                val tintPaint = Paint().apply {
                    colorFilter = android.graphics.PorterDuffColorFilter(
                        Color.parseColor("#666666"), PorterDuff.Mode.SRC_IN
                    )
                }
                canvas.drawBitmap(bmp, 0f, 0f, tintPaint)
                holder.icon.setImageBitmap(bmp)
            } catch (_: Exception) {
                holder.icon.setImageBitmap(makeCirclePlaceholder(iconSize))
            }
        } else {
            holder.icon.setImageBitmap(makeCirclePlaceholder(iconSize))
            try {
                val stream  = ctx.assets.open(item.iconAsset)
                val decoded = BitmapFactory.decodeStream(stream)
                stream.close()
                holder.icon.setImageBitmap(toCircle(decoded, iconSize))
            } catch (_: Exception) {}
        }

        holder.root.setOnClickListener {
            // Pulse animation
            pulseView(holder.icon)
            holder.root.postDelayed({ onClick(item) }, 180)
        }
    }

    private fun pulseView(v: View) {
        val scaleX1 = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.82f, 1.08f, 1f)
        val scaleY1 = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.82f, 1.08f, 1f)
        val alpha   = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.6f, 1f)
        AnimatorSet().apply {
            playTogether(scaleX1, scaleY1, alpha)
            duration = 280
            start()
        }
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

    override fun getItemCount() = items.size
}

// ── NewsAdapter ───────────────────────────────────────────────────────────────

class NewsAdapter(
    private val items: List<NewsItem>
) : RecyclerView.Adapter<NewsAdapter.VH>() {

    inner class VH(
        val root: LinearLayout,
        val cardImage: ImageView,
        val titleTv: TextView,
        val descTv: TextView,
        val sourceTv: TextView
    ) : RecyclerView.ViewHolder(root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val dp  = ctx.resources.displayMetrics.density

        // Card root
        val root = LinearLayout(ctx).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).also {
                it.marginStart  = (16 * dp).toInt()
                it.marginEnd    = (16 * dp).toInt()
                it.bottomMargin = (12 * dp).toInt()
            }
            orientation = LinearLayout.VERTICAL
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = (12 * dp)
                setColor(ContextCompat.getColor(ctx, R.color.card_background))
            }
            background = bg
            clipToOutline = true
            isClickable = true
            isFocusable = true
            elevation = 2f * dp
        }

        // Imagem
        val img = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (160 * dp).toInt()
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        // Conteúdo
        val content = LinearLayout(ctx).apply {
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
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
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

        content.addView(title)
        content.addView(desc)
        content.addView(source)

        root.addView(img)
        root.addView(content)

        return VH(root, img, title, desc, source)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val ctx  = holder.root.context
        val dp   = ctx.resources.displayMetrics.density

        holder.titleTv.text  = item.title
        holder.descTv.text   = item.description
        holder.sourceTv.text = item.sourceName

        // Placeholder cinzento
        holder.cardImage.setImageDrawable(
            android.graphics.drawable.ColorDrawable(Color.parseColor("#E5E5EA"))
        )

        if (item.imageUrl.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val req  = okhttp3.Request.Builder().url(item.imageUrl).build()
                    val resp = OkHttpClient().newCall(req).execute()
                    val bytes = resp.body?.bytes() ?: return@launch
                    val bmp  = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@launch

                    // Extrair cor dominante
                    val dominantColor = getDominantColor(bmp)
                    val textColor     = if (isColorDark(dominantColor)) Color.WHITE else Color.BLACK
                    val contentBgColor = adjustAlpha(dominantColor, 0.08f)

                    withContext(Dispatchers.Main) {
                        if (holder.bindingAdapterPosition == position) {
                            holder.cardImage.setImageBitmap(bmp)

                            // Adaptar cores do card à imagem
                            val cardBg = android.graphics.drawable.GradientDrawable().apply {
                                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                                cornerRadius = (12 * dp)
                                setColor(Color.WHITE)
                            }
                            holder.root.background = cardBg

                            val contentBg = android.graphics.drawable.GradientDrawable().apply {
                                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                                setColor(contentBgColor)
                            }
                            (holder.root.getChildAt(1) as? LinearLayout)?.background = contentBg

                            holder.titleTv.setTextColor(
                                if (isColorDark(contentBgColor)) Color.WHITE
                                else ContextCompat.getColor(ctx, R.color.text_primary)
                            )
                            holder.descTv.setTextColor(
                                if (isColorDark(contentBgColor)) 0xCCFFFFFF.toInt()
                                else ContextCompat.getColor(ctx, R.color.text_secondary)
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        holder.root.setOnClickListener {
            if (item.sourceUrl.isNotEmpty()) {
                val intent = Intent(ctx, BrowserResponseActivity::class.java).apply {
                    putExtra(BrowserResponseActivity.EXTRA_QUERY, item.sourceUrl)
                }
                ctx.startActivity(intent)
            }
        }
    }

    private fun getDominantColor(bmp: Bitmap): Int {
        val scaled = Bitmap.createScaledBitmap(bmp, 24, 24, true)
        var r = 0L; var g = 0L; var b = 0L; var count = 0
        for (x in 0 until scaled.width) {
            for (y in 0 until scaled.height) {
                val c = scaled.getPixel(x, y)
                r += Color.red(c); g += Color.green(c); b += Color.blue(c); count++
            }
        }
        return if (count == 0) Color.LTGRAY
        else Color.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
    }

    private fun isColorDark(color: Int): Boolean {
        val r = Color.red(color) / 255.0
        val g = Color.green(color) / 255.0
        val b = Color.blue(color) / 255.0
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return luminance < 0.4
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val alpha = (factor * 255).toInt().coerceIn(0, 255)
        return Color.argb(alpha, r, g, b)
    }

    override fun getItemCount() = items.size
}