package com.wilin.app

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.caverock.androidsvg.SVG
import com.wilin.app.databinding.ActivityMainBinding
import com.wilin.app.ui.AiSearchActivity
import com.wilin.app.ui.BrowserResponseActivity
import com.wilin.app.ui.BrowserTab
import com.wilin.app.ui.HomeFragment
import com.wilin.app.ui.HomeScrollCallback
import com.wilin.app.ui.SearchActivity
import com.wilin.app.ui.SearchFragment
import com.wilin.app.ui.SettingsActivity
import com.wilin.app.ui.TabManager
import com.wilin.app.ui.TabScreenshots
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(), HomeScrollCallback {

    lateinit var binding: ActivityMainBinding
    private lateinit var insetsController: WindowInsetsControllerCompat

    private val homeFragment   = HomeFragment()
    private val searchFragment = SearchFragment()
    private var currentTab = R.id.tabHome

    // Scroll hide/show
    private var appBarOffset       = 0f
    private var bottomNavOffset    = 0f
    private var maxAppBarOffset    = 0f
    private var maxBottomNavOffset = 0f

    // Tab switcher state
    private var switcherOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        val prefs = getSharedPreferences("wilin_prefs", MODE_PRIVATE)
        when (prefs.getString("theme", "system")) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark"  -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else    -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        insetsController = WindowInsetsControllerCompat(window, window.decorView)
        window.statusBarColor = ContextCompat.getColor(this, R.color.appbar_background)
        applyStatusBarTheme()

        TabManager.init(this)
        TabScreenshots.init(this)

        binding.appBarLayout.post {
            maxAppBarOffset    = binding.appBarLayout.height.toFloat()
            maxBottomNavOffset = binding.bottomNav.height.toFloat()
        }

        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)
        val iconSec  = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        binding.btnAskAiIcon.setImageDrawable(svgDrawable("icons/svg/ai.svg", 13, iconSec))
        binding.btnAskAi.setOnClickListener { startActivity(Intent(this, AiSearchActivity::class.java)) }

        binding.btnMenu.setImageDrawable(svgDrawable("icons/svg/menu.svg", 16, iconTint))
        binding.btnMenu.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.END))
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            else
                binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        binding.searchPillIcon.setImageDrawable(svgDrawable("icons/svg/magnifying_glass_outline.svg", 18, iconSec))
        binding.searchPill.setOnClickListener { startActivity(Intent(this, SearchActivity::class.java)) }

        binding.drawerIconSettings.setImageDrawable(svgDrawable("icons/svg/settings.svg", 16, iconTint))
        binding.drawerIconAbout.setImageDrawable(svgDrawable("icons/svg/about.svg", 16, iconTint))
        binding.drawerChevronSettings.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 14, iconSec))
        binding.drawerChevronAbout.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 14, iconSec))

        binding.drawerItemSettings.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.drawerItemAbout.setOnClickListener { binding.drawerLayout.closeDrawer(GravityCompat.END) }

        updateTabsBadge()

        fun setIcons(activeTab: Int) {
            binding.tabHomeIcon.setImageDrawable(
                if (activeTab == R.id.tabHome) svgDrawableGradient("icons/svg/home_filled.svg", 24)
                else svgDrawable("icons/svg/home_outline.svg", 24, iconSec)
            )
            binding.tabSearchIcon.setImageDrawable(
                if (activeTab == R.id.tabSearch) svgDrawableGradient("icons/svg/magnifying_glass_filled.svg", 24)
                else svgDrawable("icons/svg/magnifying_glass_outline.svg", 24, iconSec)
            )
        }

        fun updateAppBar(tabId: Int) {
            if (tabId == R.id.tabSearch) {
                binding.toolbarTitle.visibility = View.GONE
                binding.btnMenu.visibility      = View.GONE
                binding.btnAskAi.visibility     = View.GONE
                binding.searchPill.visibility   = View.VISIBLE
            } else {
                binding.searchPill.visibility   = View.GONE
                binding.toolbarTitle.visibility = View.VISIBLE
                binding.btnMenu.visibility      = View.VISIBLE
                binding.btnAskAi.visibility     = View.VISIBLE
            }
        }

        fun selectTab(tabId: Int) {
            if (currentTab == tabId) return
            currentTab = tabId
            setIcons(tabId)
            updateAppBar(tabId)
            revealBars()
            when (tabId) {
                R.id.tabHome   -> showFragment(homeFragment)
                R.id.tabSearch -> showFragment(searchFragment)
            }
        }

        binding.tabHome.setOnClickListener   {
            if (switcherOpen) closeSwitcher() else selectTab(R.id.tabHome)
        }
        binding.tabSearch.setOnClickListener {
            if (switcherOpen) closeSwitcher() else selectTab(R.id.tabSearch)
        }
        binding.tabTabs.setOnClickListener   {
            if (switcherOpen) closeSwitcher() else openSwitcher()
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.container, homeFragment, "home")
            .add(R.id.container, searchFragment, "search")
            .hide(searchFragment)
            .commit()

        setIcons(R.id.tabHome)
        updateAppBar(R.id.tabHome)
    }

    // ── HomeScrollCallback ────────────────────────────────────────────────────

    override fun onHomeScrollDown(dy: Int) {
        if (switcherOpen) return
        appBarOffset    = (appBarOffset + dy).coerceIn(0f, maxAppBarOffset)
        bottomNavOffset = (bottomNavOffset + dy).coerceIn(0f, maxBottomNavOffset)
        applyScrollOffsets()
    }

    override fun onHomeScrollUp(dy: Int) {
        if (switcherOpen) return
        appBarOffset    = (appBarOffset - dy).coerceIn(0f, maxAppBarOffset)
        bottomNavOffset = (bottomNavOffset - dy).coerceIn(0f, maxBottomNavOffset)
        applyScrollOffsets()
    }

    private fun applyScrollOffsets() {
        binding.appBarLayout.translationY = -appBarOffset
        binding.container.translationY    = -appBarOffset
        binding.navDivider.translationY   = bottomNavOffset
        binding.bottomNav.translationY    = bottomNavOffset
    }

    fun revealBars() {
        if (appBarOffset == 0f && bottomNavOffset == 0f) return
        val fromAppBar    = appBarOffset
        val fromBottomNav = bottomNavOffset
        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 220
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                appBarOffset    = fromAppBar * f
                bottomNavOffset = fromBottomNav * f
                applyScrollOffsets()
            }
        }.start()
    }

    override fun onResume() {
        super.onResume()
        applyStatusBarTheme()
        updateTabsBadge()
        revealBars()
    }

    // ── Tab Switcher in-place (estilo HTML) ───────────────────────────────────

    private fun openSwitcher() {
        if (switcherOpen) return
        switcherOpen = true

        val dp       = resources.displayMetrics.density
        val screenW  = resources.displayMetrics.widthPixels.toFloat()
        val screenH  = resources.displayMetrics.heightPixels.toFloat()
        val overlay  = binding.tabSwitcherOverlay
        overlay.removeAllViews()
        overlay.visibility = View.VISIBLE

        // Fundo escuro atrás do main encolhido
        overlay.setBackgroundColor(Color.parseColor("#1C1C1E"))

        // ── 1. Encolher o mainContent ────────────────────────────────────────
        val main = binding.mainContent
        main.clipToOutline = true
        main.pivotX = main.width / 2f
        main.pivotY = main.height / 2f

        val targetScale = 0.84f

        // Animar border-radius do main
        ValueAnimator.ofFloat(0f, 22f * dp).apply {
            duration = 360
            interpolator = DecelerateInterpolator(2.5f)
            addUpdateListener { anim ->
                val r = anim.animatedValue as Float
                main.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, r)
                    }
                }
            }
        }.start()

        main.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .translationY(-(screenH * 0.04f))
            .setDuration(360)
            .setInterpolator(DecelerateInterpolator(2.5f))
            .start()

        // ── 2. Construir track de cards ──────────────────────────────────────
        val tabs      = TabManager.getTabs()
        val currentId = TabManager.getCurrentId()

        // Dimensões dos cards laterais
        val cardW    = (screenW * 0.72f).toInt()
        val cardH    = (screenH * 0.68f).toInt()
        val cardGap  = (20 * dp).toInt()

        val trackWrapper = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            alpha = 0f
        }

        val hScroll = HorizontalScrollView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            isHorizontalScrollBarEnabled = false
            isFillViewport = false
        }

        val track = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            setPadding((screenW * 0.14f).toInt(), 0, (screenW * 0.14f).toInt(), 0)
        }

        hScroll.addView(track)
        trackWrapper.addView(hScroll)

        // Botão fechar no fundo
        val closeBtn = TextView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                (44 * dp).toInt(),
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).also { it.bottomMargin = (32 * dp).toInt() }
            text = "Fechar"
            textSize = 15f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding((28 * dp).toInt(), 0, (28 * dp).toInt(), 0)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 22 * dp
                setColor(Color.argb(180, 255, 255, 255))
            }
            isClickable = true; isFocusable = true
            setTextColor(Color.BLACK)
            setOnClickListener { closeSwitcher() }
        }
        trackWrapper.addView(closeBtn)

        // Cards para cada tab existente
        tabs.forEach { tab ->
            val isActive = tab.id == currentId
            val preview  = TabScreenshots.get(this, tab.id)
            val card     = buildSwitcherCard(tab, isActive, preview, cardW, cardH, cardGap, dp) {
                // Ao clicar num card: troca de tab e fecha
                TabManager.setCurrentId(tab.id)
                TabManager.save(this)
                updateTabsBadge()
                closeSwitcher()
            }
            track.addView(card)
        }

        overlay.addView(trackWrapper)

        // Fade-in do track
        trackWrapper.animate()
            .alpha(1f)
            .setStartDelay(80)
            .setDuration(240)
            .setInterpolator(DecelerateInterpolator(1.5f))
            .start()

        // Scroll para o card activo centrado
        hScroll.post {
            val activeIdx = tabs.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
            val targetX   = (activeIdx * (cardW + cardGap) - (screenW - cardW) / 2).coerceAtLeast(0f)
            hScroll.scrollTo(targetX.toInt(), 0)
        }

        // Toque no fundo (fora dos cards) fecha o switcher
        overlay.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Verificar se o toque foi fora de qualquer card
                var hitCard = false
                for (i in 0 until track.childCount) {
                    val child = track.getChildAt(i)
                    val loc = IntArray(2); child.getLocationOnScreen(loc)
                    if (event.rawX >= loc[0] && event.rawX <= loc[0] + child.width &&
                        event.rawY >= loc[1] && event.rawY <= loc[1] + child.height) {
                        hitCard = true; break
                    }
                }
                if (!hitCard) closeSwitcher()
            }
            false // passa os eventos para o HScrollView
        }
    }

    private fun buildSwitcherCard(
        tab: BrowserTab,
        isActive: Boolean,
        preview: Bitmap?,
        cardW: Int,
        cardH: Int,
        cardGap: Int,
        dp: Float,
        onTap: () -> Unit
    ): FrameLayout {
        val blue = ContextCompat.getColor(this, R.color.colorPrimary)
        val ctx  = this

        val card = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(cardW, cardH).also {
                it.marginEnd = cardGap
            }
            background = GradientDrawable().apply {
                shape        = GradientDrawable.RECTANGLE
                cornerRadius = 18 * dp
                setColor(ContextCompat.getColor(ctx, R.color.card_background))
                if (isActive) setStroke((2 * dp).toInt(), blue)
            }
            clipToOutline = true
            elevation = if (isActive) 12f * dp else 4f * dp
            // Entrada: de baixo com overshoot
            alpha     = 0f
            scaleX    = 0.90f
            scaleY    = 0.90f
            translationY = 60f * dp
        }

        // Preview
        val previewIv = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).also { it.topMargin = (50 * dp).toInt() }
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.background))
        }
        preview?.let { previewIv.setImageBitmap(it) }

        // Header
        val header = LinearLayout(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                (50 * dp).toInt(),
                Gravity.TOP
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * dp).toInt(), 0, (10 * dp).toInt(), 0)
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.card_background))
        }

        val titleTv = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            text = tab.title.ifEmpty { if (tab.url.isEmpty()) "Nova aba" else tab.url }
            textSize = 13f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }

        val closeIv = ImageView(ctx).apply {
            val sz = (34 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(sz, sz)
            setPadding((7 * dp).toInt(), (7 * dp).toInt(), (7 * dp).toInt(), (7 * dp).toInt())
            setImageDrawable(svgDrawable("icons/svg/close.svg", 16,
                ContextCompat.getColor(ctx, R.color.icon_tint_secondary)))
            isClickable = true; isFocusable = true
            setOnClickListener {
                // Fechar este tab
                card.animate()
                    .translationY(card.height.toFloat() + 40f)
                    .scaleX(0.85f).scaleY(0.85f)
                    .alpha(0f)
                    .setDuration(220)
                    .setInterpolator(DecelerateInterpolator(2f))
                    .withEndAction {
                        (card.parent as? ViewGroup)?.removeView(card)
                        TabScreenshots.remove(ctx, tab.id)
                        TabManager.closeTab(tab.id)
                        TabManager.save(ctx)
                        updateTabsBadge()
                        if (TabManager.count() == 0) {
                            closeSwitcher()
                            TabManager.newTab()
                            TabManager.save(ctx)
                        }
                    }.start()
            }
        }

        header.addView(titleTv)
        header.addView(closeIv)
        card.addView(previewIv)
        card.addView(header)

        // Animação de entrada
        card.post {
            card.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(380)
                .setInterpolator(OvershootInterpolator(0.7f))
                .start()
        }

        // Toque: expandir e abrir
        card.setOnClickListener {
            val loc = IntArray(2)
            card.getLocationOnScreen(loc)
            val sw = resources.displayMetrics.widthPixels.toFloat()
            val sh = resources.displayMetrics.heightPixels.toFloat()
            val sx = sw / card.width.toFloat()
            val sy = sh / card.height.toFloat()

            card.pivotX = 0f
            card.pivotY = 0f
            card.animate()
                .scaleX(sx).scaleY(sy)
                .translationX(-loc[0].toFloat())
                .translationY(-loc[1].toFloat())
                .alpha(0.6f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator(2.5f))
                .withEndAction { onTap() }
                .start()
        }

        return card
    }

    private fun closeSwitcher() {
        if (!switcherOpen) return
        switcherOpen = false

        val main = binding.mainContent
        val overlay = binding.tabSwitcherOverlay

        // Fade out do track
        val trackWrapper = overlay.getChildAt(0)
        trackWrapper?.animate()
            ?.alpha(0f)
            ?.setDuration(200)
            ?.setInterpolator(DecelerateInterpolator(1.5f))
            ?.start()

        // Restaurar main
        main.animate()
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(320)
            .setInterpolator(DecelerateInterpolator(2.5f))
            .withEndAction {
                main.clipToOutline = false
                main.outlineProvider = ViewOutlineProvider.BACKGROUND
                overlay.visibility = View.GONE
                overlay.removeAllViews()
                overlay.setBackgroundColor(Color.TRANSPARENT)
            }
            .start()

        // Limpar border-radius
        ValueAnimator.ofFloat(22f * resources.displayMetrics.density, 0f).apply {
            duration = 320
            interpolator = DecelerateInterpolator(2.5f)
            addUpdateListener { anim ->
                val r = anim.animatedValue as Float
                main.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, main.width, main.height, r)
                    }
                }
            }
        }.start()
    }

    private fun updateTabsBadge() {
        val count = TabManager.count()
        binding.tabTabsCount.text = if (count > 99) "99" else count.toString()
    }

    override fun onBackPressed() {
        if (switcherOpen) { closeSwitcher(); return }
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            return
        }
        super.onBackPressed()
    }

    private fun applyStatusBarTheme() {
        val isLight = !resources.configuration.isNightModeActive
        window.statusBarColor = ContextCompat.getColor(this, R.color.appbar_background)
        insetsController.isAppearanceLightStatusBars = isLight
    }

    fun svgDrawableGradient(path: String, sizeDp: Int): BitmapDrawable {
        val px  = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        SVG.getFromAsset(assets, path).apply {
            documentWidth = px.toFloat(); documentHeight = px.toFloat()
            renderToCanvas(Canvas(bmp))
        }
        val gb = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val gc = Canvas(gb)
        val gp = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, px.toFloat(),
                Color.parseColor("#007AFF"), Color.parseColor("#00C6FF"), Shader.TileMode.CLAMP)
        }
        gc.drawRect(0f, 0f, px.toFloat(), px.toFloat(), gp)
        gc.drawBitmap(bmp, 0f, 0f, Paint().apply {
            xfermode = android.graphics.PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        })
        return BitmapDrawable(resources, gb)
    }

    fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
        val px  = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        SVG.getFromAsset(assets, path).apply {
            documentWidth = px.toFloat(); documentHeight = px.toFloat()
            renderToCanvas(Canvas(bmp))
        }
        return BitmapDrawable(resources, bmp).also { it.setColorFilter(tint, PorterDuff.Mode.SRC_IN) }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .hide(homeFragment).hide(searchFragment)
            .show(fragment).commit()
    }
