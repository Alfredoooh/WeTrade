package com.wilin.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
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

data class AppItem(
    val label: String,
    val url: String,
    val iconAsset: String,
    val category: String
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Todos os apps numa lista plana — a lógica de páginas é calculada dinamicamente
    private val allApps = mutableListOf(
        SiteItem("Google",    "https://google.com",       "icons/png/google.png"),
        SiteItem("YouTube",   "https://youtube.com",      "icons/png/youtube.png"),
        SiteItem("Facebook",  "https://facebook.com",     "icons/png/facebook.png"),
        SiteItem("Instagram", "https://instagram.com",    "icons/png/instagram.png"),
        SiteItem("WhatsApp",  "https://web.whatsapp.com", "icons/png/whatsapp.png"),
        SiteItem("X",         "https://x.com",            "icons/png/x.png"),
        SiteItem("ChatGPT",   "https://chat.openai.com",  "icons/png/chatgpt.png"),
        SiteItem("TikTok",    "https://tiktok.com",       "icons/png/tiktok.png"),
        SiteItem("Reddit",    "https://reddit.com",       "icons/png/reddit.png"),
    )

    // Quantos apps cabem por página (excluindo o botão "Mais")
    private val ITEMS_PER_PAGE = 8 // 8 apps + 1 botão "Mais" = 9 células por página de 2 linhas

    private val availableApps = listOf(
        AppItem("Gmail",         "https://mail.google.com",       "icons/png/google.png",    "Google"),
        AppItem("Drive",         "https://drive.google.com",      "icons/png/google.png",    "Google"),
        AppItem("Maps",          "https://maps.google.com",       "icons/png/google.png",    "Google"),
        AppItem("Docs",          "https://docs.google.com",       "icons/png/google.png",    "Google"),
        AppItem("LinkedIn",      "https://linkedin.com",          "icons/png/x.png",         "Social"),
        AppItem("Telegram",      "https://web.telegram.org",      "icons/png/whatsapp.png",  "Social"),
        AppItem("Discord",       "https://discord.com/app",       "icons/png/reddit.png",    "Social"),
        AppItem("Pinterest",     "https://pinterest.com",         "icons/png/instagram.png", "Social"),
        AppItem("Twitch",        "https://twitch.tv",             "icons/png/youtube.png",   "Entretenimento"),
        AppItem("Netflix",       "https://netflix.com",           "icons/png/youtube.png",   "Entretenimento"),
        AppItem("Spotify",       "https://open.spotify.com",      "icons/png/youtube.png",   "Entretenimento"),
        AppItem("Amazon",        "https://amazon.com",            "icons/png/google.png",    "Compras"),
        AppItem("AliExpress",    "https://aliexpress.com",        "icons/png/google.png",    "Compras"),
        AppItem("Shein",         "https://shein.com",             "icons/png/instagram.png", "Compras"),
        AppItem("GitHub",        "https://github.com",            "icons/png/x.png",         "Dev"),
        AppItem("StackOverflow", "https://stackoverflow.com",     "icons/png/reddit.png",    "Dev"),
        AppItem("Gemini",        "https://gemini.google.com",     "icons/png/google.png",    "IA"),
        AppItem("Claude",        "https://claude.ai",             "icons/png/chatgpt.png",   "IA"),
        AppItem("Perplexity",    "https://perplexity.ai",         "icons/png/x.png",         "IA"),
        AppItem("Copilot",       "https://copilot.microsoft.com", "icons/png/x.png",         "IA"),
    )

    private val newsCategories   = listOf("Mundo", "Tecnologia", "Saúde", "Desporto", "Ciência", "Entretenimento")
    private val newsCategoryKeys = listOf("world", "technology", "health", "sports", "science", "entertainment")
    private var selectedCategoryIndex = 0
    private val newsItems = mutableListOf<NewsItem>()
    private lateinit var newsAdapter: NewsAdapter

    private val NEWS_API_BASE = "https://globeapiservice001.onrender.com"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val mainToggleChips   = mutableListOf<TextView>()
    private val stickyToggleChips = mutableListOf<TextView>()
    private lateinit var sitesHScroll: HorizontalScrollView
    private lateinit var sitesRowContainer: LinearLayout
    private lateinit var dotsContainer: LinearLayout
    private var currentPage = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildSitesCarousel()
        buildCategoryToggles()
        setupStickyScroll()

        newsAdapter = NewsAdapter(newsItems) { url ->
            startActivity(Intent(requireContext(), BrowserResponseActivity::class.java).apply {
                putExtra(BrowserResponseActivity.EXTRA_QUERY, url)
            })
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
        binding.newsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.newsRecycler.adapter = newsAdapter
        binding.newsRecycler.isNestedScrollingEnabled = false

        fetchNews(newsCategoryKeys[0])
    }

    // ── Sticky scroll ──────────────────────────────────────────────────────────

    private fun setupStickyScroll() {
        binding.homeScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            when {
                dy > 3  -> (activity as? HomeScrollCallback)?.onHomeScrollDown(dy)
                dy < -3 -> (activity as? HomeScrollCallback)?.onHomeScrollUp(-dy)
            }

            val stickySection  = binding.stickyToggleSection
            val overlayLayout  = binding.stickyToggleOverlay
            val location       = IntArray(2)
            val parentLocation = IntArray(2)
            stickySection.getLocationInWindow(location)
            binding.homeScrollView.getLocationInWindow(parentLocation)
            val stickyTop = location[1] - parentLocation[1]

            if (stickyTop <= 0) {
                if (overlayLayout.visibility != View.VISIBLE) {
                    overlayLayout.visibility = View.VISIBLE
                    syncStickyChips()
                }
            } else {
                if (overlayLayout.visibility != View.GONE) {
                    overlayLayout.visibility = View.GONE
                }
            }
        })
    }

    private fun syncStickyChips() {
        val dp = requireContext().resources.displayMetrics.density
        binding.categoryToggleContainerSticky.removeAllViews()
        stickyToggleChips.clear()
        newsCategories.forEachIndexed { i, label ->
            val chip = makeChip(label, i == selectedCategoryIndex, dp)
            chip.setOnClickListener { selectCategory(i) }
            stickyToggleChips.add(chip)
            binding.categoryToggleContainerSticky.addView(chip)
        }
    }

    // ── Carrossel ──────────────────────────────────────────────────────────────

    private fun buildSitesCarousel() {
        val ctx = requireContext()
        val dp  = ctx.resources.displayMetrics.density
        binding.sitesCarouselContainer.removeAllViews()

        sitesHScroll = HorizontalScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
            isSmoothScrollingEnabled = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        sitesRowContainer = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
        }

        sitesHScroll.addView(sitesRowContainer)
        binding.sitesCarouselContainer.addView(sitesHScroll)

        dotsContainer = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.topMargin = (6 * dp).toInt() }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        binding.sitesCarouselContainer.addView(dotsContainer)

        renderPages()
    }

    // Divide allApps em páginas de ITEMS_PER_PAGE, com botão "Mais" sempre no fim
    private fun getPages(): List<List<SiteItem>> {
        val pages = mutableListOf<List<SiteItem>>()
        var idx = 0
        while (idx < allApps.size || pages.isEmpty()) {
            val chunk = allApps.subList(idx, minOf(idx + ITEMS_PER_PAGE, allApps.size))
            pages.add(chunk)
            idx += ITEMS_PER_PAGE
            if (idx >= allApps.size) break
        }
        return pages
    }

    private fun renderPages() {
        val ctx     = requireContext()
        val dp      = ctx.resources.displayMetrics.density
        val screenW = resources.displayMetrics.widthPixels

        sitesRowContainer.removeAllViews()
        dotsContainer.removeAllViews()

        val pages = getPages()

        pages.forEachIndexed { pageIdx, pageApps ->
            // Na última página, adiciona o botão "Mais" no fim
            val items = pageApps.toMutableList()
            if (pageIdx == pages.size - 1) {
                items.add(SiteItem("Mais", "", "", isMore = true))
            }
            sitesRowContainer.addView(buildPage(items, screenW, dp))
        }

        val pageCount = pages.size
        if (pageCount > 1) {
            for (i in 0 until pageCount) {
                val dot = View(ctx).apply {
                    val sz = (6 * dp).toInt()
                    layoutParams = LinearLayout.LayoutParams(sz, sz).also {
                        it.marginStart = (4 * dp).toInt()
                        it.marginEnd   = (4 * dp).toInt()
                    }
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(
                            if (i == currentPage) ContextCompat.getColor(ctx, R.color.colorPrimary)
                            else ContextCompat.getColor(ctx, R.color.divider)
                        )
                    }
                }
                dotsContainer.addView(dot)
            }

            // Snap por página ao soltar o scroll
            sitesHScroll.viewTreeObserver.addOnScrollChangedListener {
                val newPage = (sitesHScroll.scrollX + screenW / 2) / screenW
                val clampedPage = newPage.coerceIn(0, pageCount - 1)
                if (clampedPage != currentPage) {
                    currentPage = clampedPage
                    updateDots()
                }
            }
        }
    }

    private fun buildPage(items: List<SiteItem>, screenW: Int, dp: Float): LinearLayout {
        val ctx = requireContext()
        // 5 colunas por linha, 2 linhas por página (máx 10 células, 8 apps + 1 botão + 1 vazia)
        return LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(screenW, LinearLayout.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            items.chunked(5).forEach { row ->
                addView(LinearLayout(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    orientation = LinearLayout.HORIZONTAL
                    row.forEach { addView(buildCell(it, dp, screenW / 5)) }
                    // Preenche células vazias na última linha
                    repeat(5 - row.size) {
                        addView(View(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(screenW / 5, 1)
                        })
                    }
                })
            }
        }
    }

    private fun buildCell(item: SiteItem, dp: Float, width: Int): LinearLayout {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER_HORIZONTAL
            setPadding(0, (10 * dp).toInt(), 0, (10 * dp).toInt())
            isClickable = true; isFocusable = true
            background  = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)

            val iconSz = (40 * dp).toInt()
            val container = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams((48 * dp).toInt(), (48 * dp).toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.WHITE)
                }
                elevation = 2 * dp
            }
            val icon = ImageView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(iconSz, iconSz, Gravity.CENTER)
                scaleType    = ImageView.ScaleType.FIT_CENTER
            }
            when {
                item.isMore -> icon.setImageBitmap(makeMoreBmp(iconSz))
                item.iconAsset.isNotEmpty() -> {
                    icon.setImageBitmap(makePlaceholderBmp(iconSz))
                    try {
                        val s = ctx.assets.open(item.iconAsset)
                        val bmp = BitmapFactory.decodeStream(s); s.close()
                        icon.setImageBitmap(Bitmap.createScaledBitmap(bmp, iconSz, iconSz, true))
                    } catch (_: Exception) {}
                }
                else -> icon.setImageBitmap(makePlaceholderBmp(iconSz))
            }
            container.addView(icon)

            val label = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = (5 * dp).toInt() }
                text = item.label; textSize = 10f; maxLines = 1
                gravity   = Gravity.CENTER_HORIZONTAL
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            }
            addView(container); addView(label)

            setOnClickListener {
                if (item.isMore) showMoreModal()
                else startActivity(Intent(ctx, BrowserResponseActivity::class.java).apply {
                    putExtra(BrowserResponseActivity.EXTRA_QUERY, item.url)
                }).also { requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left) }
            }

            if (!item.isMore) {
                setOnLongClickListener {
                    showRemoveConfirm(item)
                    true
                }
            }
        }
    }

    private fun showRemoveConfirm(item: SiteItem) {
        val ctx = requireContext()
        android.app.AlertDialog.Builder(ctx)
            .setTitle("Remover app")
            .setMessage("Remover \"${item.label}\"?")
            .setPositiveButton("Remover") { _, _ ->
                allApps.removeAll { it.url == item.url }
                val totalPages = getPages().size
                if (currentPage >= totalPages) currentPage = (totalPages - 1).coerceAtLeast(0)
                renderPages()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateDots() {
        val ctx = requireContext()
        for (i in 0 until dotsContainer.childCount) {
            (dotsContainer.getChildAt(i).background as? android.graphics.drawable.GradientDrawable)
                ?.setColor(
                    if (i == currentPage) ContextCompat.getColor(ctx, R.color.colorPrimary)
                    else ContextCompat.getColor(ctx, R.color.divider)
                )
        }
    }

    // ── Modal Mais Apps ────────────────────────────────────────────────────────

    private fun showMoreModal() {
        val ctx     = requireContext()
        val dp      = ctx.resources.displayMetrics.density
        val rootView = requireActivity().window.decorView as FrameLayout
        val sheetH  = (resources.displayMetrics.heightPixels * 0.72f).toInt()
        val cornerR = 28 * dp

        val overlay = FrameLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.argb(140, 0, 0, 0))
            isClickable = true; isFocusable = true
        }

        val sheet = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, sheetH, Gravity.BOTTOM
            )
            orientation = LinearLayout.VERTICAL
            background  = android.graphics.drawable.GradientDrawable().apply {
                shape       = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadii = floatArrayOf(cornerR, cornerR, cornerR, cornerR, 0f, 0f, 0f, 0f)
                setColor(ContextCompat.getColor(ctx, R.color.surface))
            }
            translationY = sheetH.toFloat()
        }

        val handleWrap = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (28 * dp).toInt()
            )
        }
        handleWrap.addView(View(ctx).apply {
            layoutParams = FrameLayout.LayoutParams((40 * dp).toInt(), (4 * dp).toInt(), Gravity.CENTER)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 2 * dp
                setColor(ContextCompat.getColor(ctx, R.color.divider))
            }
        })

        val title = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginStart = (20 * dp).toInt(); it.bottomMargin = (16 * dp).toInt() }
            text = "Adicionar App"; textSize = 20f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        val scroll = ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        val content = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding((16 * dp).toInt(), 0, (16 * dp).toInt(), (32 * dp).toInt())
        }

        val appW = (resources.displayMetrics.widthPixels - (32 * dp).toInt()) / 4

        availableApps.groupBy { it.category }.forEach { (cat, apps) ->
            content.addView(TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = (16 * dp).toInt(); it.bottomMargin = (10 * dp).toInt() }
                text = cat; textSize = 13f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            })
            apps.chunked(4).forEach { row ->
                content.addView(LinearLayout(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    orientation = LinearLayout.HORIZONTAL
                    row.forEach { app ->
                        addView(buildModalCell(app, appW.toInt(), dp) {
                            addApp(app)
                            dismissModal(overlay, sheet, sheetH, rootView)
                            showMoreModal()
                        })
                    }
                    repeat(4 - row.size) {
                        addView(View(ctx).apply { layoutParams = LinearLayout.LayoutParams(appW.toInt(), 1) })
                    }
                })
            }
        }

        scroll.addView(content)
        sheet.addView(handleWrap)
        sheet.addView(title)
        sheet.addView(scroll)
        overlay.addView(sheet)
        rootView.addView(overlay)

        sheet.animate()
            .translationY(0f)
            .setDuration(320)
            .setInterpolator(android.view.animation.DecelerateInterpolator(2f))
            .start()

        overlay.setOnClickListener { dismissModal(overlay, sheet, sheetH, rootView) }
    }

    // REGRA CORRIGIDA:
    // - O app vai sempre para a primeira posição disponível na lista (sequencial)
    // - Páginas são calculadas dinamicamente: allApps[0..7] = pág1, allApps[8..15] = pág2, etc.
    // - O botão "Mais" fica sempre no último slot da última página
    // - Ao adicionar, vai para a primeira página que ainda tem espaço (< ITEMS_PER_PAGE)
    private fun addApp(app: AppItem) {
        val alreadyAdded = allApps.any { it.url == app.url }
        if (alreadyAdded) {
            Toast.makeText(requireContext(), "${app.label} já adicionado", Toast.LENGTH_SHORT).show()
            return
        }
        // Adiciona sempre ao fim da lista — a paginação trata do resto
        allApps.add(SiteItem(app.label, app.url, app.iconAsset))

        // Calcula em que página ficou o novo app
        val newPageIdx = (allApps.size - 1) / ITEMS_PER_PAGE
        currentPage = newPageIdx

        renderPages()

        // Scroll para a página onde foi adicionado
        sitesHScroll.post {
            val screenW = resources.displayMetrics.widthPixels
            sitesHScroll.smoothScrollTo(currentPage * screenW, 0)
        }

        Toast.makeText(requireContext(), "${app.label} adicionado", Toast.LENGTH_SHORT).show()
    }

    private fun buildModalCell(app: AppItem, width: Int, dp: Float, onAdd: () -> Unit): LinearLayout {
        val ctx = requireContext()
        val added = allApps.any { it.url == app.url }
        return LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT)
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER_HORIZONTAL
            setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
            isClickable = !added; isFocusable = !added
            alpha = if (added) 0.45f else 1f
            if (!added) background = ContextCompat.getDrawable(ctx, R.drawable.ripple_item)

            val iconSz = (36 * dp).toInt()
            val container = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams((46 * dp).toInt(), (46 * dp).toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.WHITE)
                }
                elevation = 2 * dp
            }
            val icon = ImageView(ctx).apply {
                layoutParams = FrameLayout.LayoutParams(iconSz, iconSz, Gravity.CENTER)
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            try {
                val s = ctx.assets.open(app.iconAsset)
                icon.setImageBitmap(Bitmap.createScaledBitmap(BitmapFactory.decodeStream(s), iconSz, iconSz, true))
                s.close()
            } catch (_: Exception) { icon.setImageBitmap(makePlaceholderBmp(iconSz)) }

            if (added) {
                container.addView(View(ctx).apply {
                    val bsz = (14 * dp).toInt()
                    layoutParams = FrameLayout.LayoutParams(bsz, bsz, Gravity.BOTTOM or Gravity.END)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(Color.parseColor("#34C759"))
                    }
                })
            }
            container.addView(icon)

            addView(container)
            addView(TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.topMargin = (4 * dp).toInt() }
                text = app.label; textSize = 10f; maxLines = 1
                gravity   = Gravity.CENTER_HORIZONTAL
                ellipsize = android.text.TextUtils.TruncateAt.END
                setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            })
            if (!added) setOnClickListener { onAdd() }
        }
    }

    private fun dismissModal(overlay: FrameLayout, sheet: LinearLayout, sheetH: Int, root: FrameLayout) {
        sheet.animate()
            .translationY(sheetH.toFloat())
            .setDuration(240)
            .setInterpolator(android.view.animation.AccelerateInterpolator(1.5f))
            .withEndAction { root.removeView(overlay) }
            .start()
    }

    // ── Bitmap helpers ─────────────────────────────────────────────────────────

    private fun makePlaceholderBmp(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = Color.parseColor("#E5E5EA")
            Canvas(bmp).drawCircle(size / 2f, size / 2f, size / 2f, it)
        }
        return bmp
    }

    private fun makeMoreBmp(size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.parseColor("#E5E5EA")
        c.drawCircle(size / 2f, size / 2f, size / 2f, p)
        p.color = Color.parseColor("#888888")
        val dotR = size * 0.08f; val off = size * 0.25f
        for (row in 0..1) for (col in 0..1)
            c.drawCircle(size / 2f - off + col * off * 2, size / 2f - off + row * off * 2, dotR, p)
        return bmp
    }

    // ── Category toggles ───────────────────────────────────────────────────────

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
                LinearLayout.LayoutParams.WRAP_CONTENT, (32 * dp).toInt()
            ).also { it.marginEnd = (8 * dp).toInt() }
            text = label; textSize = 13f; gravity = Gravity.CENTER
            setPadding((14 * dp).toInt(), 0, (14 * dp).toInt(), 0)
            isClickable = true; isFocusable = true
            applyChipStyle(this, selected, dp)
        }
    }

    private fun applyChipStyle(chip: TextView, selected: Boolean, dp: Float) {
        val ctx = requireContext()
        chip.background = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 16 * dp
            if (selected) setColor(ContextCompat.getColor(ctx, R.color.colorPrimary))
            else { setColor(0); setStroke(1, ContextCompat.getColor(ctx, R.color.divider)) }
        }
        chip.setTextColor(if (selected) Color.WHITE else ContextCompat.getColor(ctx, R.color.text_secondary))
        chip.setTypeface(chip.typeface, if (selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
    }

    private fun selectCategory(index: Int) {
        if (index == selectedCategoryIndex) return
        val dp = requireContext().resources.displayMetrics.density
        applyChipStyle(mainToggleChips[selectedCategoryIndex], false, dp)
        applyChipStyle(mainToggleChips[index], true, dp)
        if (stickyToggleChips.size > selectedCategoryIndex) applyChipStyle(stickyToggleChips[selectedCategoryIndex], false, dp)
        if (stickyToggleChips.size > index) applyChipStyle(stickyToggleChips[index], true, dp)
        selectedCategoryIndex = index
        newsItems.clear(); newsAdapter.notifyDataSetChanged()
        fetchNews(newsCategoryKeys[index])
    }

    // ── Fetch notícias ─────────────────────────────────────────────────────────

    private fun fetchNews(category: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val fetched = mutableListOf<NewsItem>()
            try {
                val req  = Request.Builder().url("$NEWS_API_BASE/news?category=$category&limit=20").build()
                val resp = httpClient.newCall(req).execute()
                if (resp.isSuccessful) {
                    val arr = JSONArray(resp.body?.string() ?: "[]")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        fetched.add(NewsItem(
                            title       = o.optString("title"),
                            description = o.optString("description"),
                            imageUrl    = o.optString("image_url"),
                            sourceUrl   = o.optString("url"),
                            sourceName  = o.optString("source_name"),
                            faviconUrl  = o.optString("favicon_url"),
                            category    = o.optString("category")
                        ))
                    }
                }
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                newsItems.clear(); newsItems.addAll(fetched)
                newsAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}