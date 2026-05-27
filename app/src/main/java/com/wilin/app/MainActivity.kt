package com.wilin.app

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
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
import com.wilin.app.ui.HomeFragment
import com.wilin.app.ui.SearchActivity
import com.wilin.app.ui.SearchFragment
import com.wilin.app.ui.SettingsActivity
import com.wilin.app.ui.TabManager
import com.wilin.app.ui.TabsActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var insetsController: WindowInsetsControllerCompat

    private val homeFragment   = HomeFragment()
    private val searchFragment = SearchFragment()
    private var currentTab = R.id.tabHome

    // ── Scroll-hide state ─────────────────────────────────────────────────────
    private var appBarHidden     = false
    private var bottomNavHidden  = false
    private var isTabsAnimating  = false
    private var actionBarHideAmt = 0   // toolbar height in px (excluding status bar)

    // Outline para animação de corner radius na transição de tabs
    private var tabsCornerRadius = 0f
    private val tabsOutlineProvider = object : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, tabsCornerRadius)
        }
    }

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

        // Calcular altura do toolbar (sem status bar) após layout
        binding.appBarLayout.post {
            val tv = TypedValue()
            theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)
            actionBarHideAmt = TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
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
            // Ao mudar de aba, garantir que as barras estão visíveis
            snapBarsVisible()
            when (tabId) {
                R.id.tabHome   -> showFragment(homeFragment)
                R.id.tabSearch -> showFragment(searchFragment)
            }
        }

        binding.tabHome.setOnClickListener   { selectTab(R.id.tabHome) }
        binding.tabSearch.setOnClickListener { selectTab(R.id.tabSearch) }
        binding.tabTabs.setOnClickListener   { openTabsWithTransform() }

        supportFragmentManager.beginTransaction()
            .add(R.id.container, homeFragment, "home")
            .add(R.id.container, searchFragment, "search")
            .hide(searchFragment)
            .commit()

        setIcons(R.id.tabHome)
        updateAppBar(R.id.tabHome)
    }

    override fun onResume() {
        super.onResume()
        applyStatusBarTheme()
        updateTabsBadge()
        // Voltar dos tabs — garantir que as barras voltam à posição normal
        snapBarsVisible()
    }

    // ── Scroll hook chamado pelo HomeFragment ─────────────────────────────────

    fun onHomeScrolled(dy: Int, scrollY: Int) {
        if (isTabsAnimating || actionBarHideAmt == 0) return

        if (dy > 10 && scrollY > 80) {
            // Scrolling down → esconder barras
            if (!appBarHidden) hideMainBars()
        } else if (dy < -10) {
            // Scrolling up → mostrar barras
            if (appBarHidden) showMainBars()
        }
    }

    private fun hideMainBars() {
        if (appBarHidden) return
        appBarHidden    = true
        bottomNavHidden = true
        val dur = 220L
        val interp = DecelerateInterpolator(2f)

        binding.appBarLayout.animate()
            .translationY(-actionBarHideAmt.toFloat())
            .setDuration(dur).setInterpolator(interp).start()

        binding.bottomNav.animate()
            .translationY(binding.bottomNav.height.toFloat())
            .setDuration(dur).setInterpolator(interp).start()

        binding.navDivider.animate()
            .translationY(binding.bottomNav.height.toFloat())
            .setDuration(dur).setInterpolator(interp).start()

        notifyHomeFragmentBarsTranslation(-actionBarHideAmt.toFloat())
    }

    private fun showMainBars() {
        if (!appBarHidden) return
        appBarHidden    = false
        bottomNavHidden = false
        val dur = 220L
        val interp = DecelerateInterpolator(2f)

        binding.appBarLayout.animate()
            .translationY(0f)
            .setDuration(dur).setInterpolator(interp).start()

        binding.bottomNav.animate()
            .translationY(0f)
            .setDuration(dur).setInterpolator(interp).start()

        binding.navDivider.animate()
            .translationY(0f)
            .setDuration(dur).setInterpolator(interp).start()

        notifyHomeFragmentBarsTranslation(0f)
    }

    /** Snap instantâneo para visível (sem animação) — usado ao mudar de aba ou voltar de outros ecrãs */
    private fun snapBarsVisible() {
        if (!appBarHidden && !bottomNavHidden) return
        appBarHidden    = false
        bottomNavHidden = false
        binding.appBarLayout.animate().cancel()
        binding.bottomNav.animate().cancel()
        binding.navDivider.animate().cancel()
        binding.appBarLayout.animate()
            .translationY(0f).setDuration(180)
            .setInterpolator(DecelerateInterpolator(2f)).start()
        binding.bottomNav.animate()
            .translationY(0f).setDuration(180)
            .setInterpolator(DecelerateInterpolator(2f)).start()
        binding.navDivider.animate()
            .translationY(0f).setDuration(180)
            .setInterpolator(DecelerateInterpolator(2f)).start()
        notifyHomeFragmentBarsTranslation(0f)
    }

    private fun notifyHomeFragmentBarsTranslation(appBarTransY: Float) {
        (supportFragmentManager.findFragmentByTag("home") as? HomeFragment)
            ?.updateStickyOverlayTranslation(appBarTransY)
    }

    // ── iOS App Switcher — Container Transform ────────────────────────────────
    //
    // Ao clicar em Abas:
    //   1. O ecrã inteiro "descola" — encolhe com corner radius crescente + scrim escurece
    //   2. A escala alvo é exatamente a largura do card no TabsActivity (300dp / screenW)
    //   3. TabsActivity abre com overridePendingTransition(0,0) — transição invisível
    //   4. Estado reset imediato para quando o utilizador voltar
    //
    private fun openTabsWithTransform() {
        if (isTabsAnimating) return
        isTabsAnimating = true

        val dp      = resources.displayMetrics.density
        val screenW = resources.displayMetrics.widthPixels.toFloat()
        val screenH = resources.displayMetrics.heightPixels.toFloat()

        // Garantir barras visíveis antes de animar
        binding.appBarLayout.animate().cancel()
        binding.bottomNav.animate().cancel()
        binding.navDivider.animate().cancel()
        binding.appBarLayout.translationY = 0f
        binding.bottomNav.translationY    = 0f
        binding.navDivider.translationY   = 0f
        appBarHidden    = false
        bottomNavHidden = false

        // mainContent = LinearLayout (appbar + container + bottomnav)
        val mainContent = (binding.root as ViewGroup).getChildAt(0)

        // Escala alvo = largura do card (300dp) / largura do ecrã
        val cardWpx     = 300f * dp
        val targetScale = (cardWpx / screenW).coerceIn(0.55f, 0.85f)
        val cornerPx    = 22f * dp

        // ── Scrim por detrás ──────────────────────────────────────────────────
        val scrim = View(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            alpha = 0f
        }
        // Inserir em index 0 (atrás do mainContent que fica em index 1)
        (binding.root as ViewGroup).addView(scrim, 0)

        // Captura screenshot para o TabsActivity
        val snapshotPath = captureScreenSnapshot("tabs_transition_main.png")

        // ── Corner radius via ViewOutlineProvider + ValueAnimator ─────────────
        // (recicla o mesmo provider, apenas muda o raio — sem GC pressure)
        tabsCornerRadius = 0f
        mainContent.outlineProvider = tabsOutlineProvider
        mainContent.clipToOutline   = true

        val cornerAnim = ValueAnimator.ofFloat(0f, cornerPx).apply {
            duration    = TABS_ANIM_DURATION
            interpolator = DecelerateInterpolator(2.5f)
            addUpdateListener { anim ->
                tabsCornerRadius = anim.animatedValue as Float
                mainContent.invalidateOutline()
            }
        }
        cornerAnim.start()

        // ── Scale + translate (pivot ao centro) ───────────────────────────────
        mainContent.pivotX = mainContent.width / 2f
        mainContent.pivotY = mainContent.height / 2f

        mainContent.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .translationY(-(screenH * 0.025f))    // sobe ligeiramente, como no iOS
            .setDuration(TABS_ANIM_DURATION)
            .setInterpolator(DecelerateInterpolator(2.5f))
            .withEndAction {
                // Abrir TabsActivity sem qualquer transição de Activity
                startActivity(Intent(this, TabsActivity::class.java).apply {
                    snapshotPath?.let { putExtra(TabsActivity.EXTRA_TRANSITION_SCREENSHOT_PATH, it) }
                    putExtra(TabsActivity.EXTRA_SOURCE_SCALE, targetScale)
                })
                overridePendingTransition(0, 0)

                // Reset INSTANTÂNEO — quando o utilizador voltar o ecrã está limpo
                mainContent.scaleX        = 1f
                mainContent.scaleY        = 1f
                mainContent.translationY  = 0f
                tabsCornerRadius          = 0f
                mainContent.outlineProvider = ViewOutlineProvider.BACKGROUND
                mainContent.clipToOutline = false

                scrim.alpha = 0f
                (binding.root as ViewGroup).removeView(scrim)
                isTabsAnimating = false
            }
            .start()

        // ── Scrim escurece em paralelo ────────────────────────────────────────
        scrim.animate()
            .alpha(0.60f)
            .setDuration(TABS_ANIM_DURATION)
            .setInterpolator(DecelerateInterpolator(2f))
            .start()
    }

    private fun captureScreenSnapshot(fileName: String): String? {
        val root = binding.root
        if (root.width <= 0 || root.height <= 0) return null
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        val file = File(cacheDir, fileName)
        runCatching {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                out.flush()
            }
        }
        bitmap.recycle()
        return if (file.exists()) file.absolutePath else null
    }

    private fun updateTabsBadge() {
        val count = TabManager.count()
        binding.tabTabsCount.text = if (count > 99) "99" else count.toString()
    }

    override fun onBackPressed() {
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
        val gb  = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val gc  = Canvas(gb)
        val gp  = Paint().apply {
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

    companion object {
        private const val TABS_ANIM_DURATION = 340L
    }
}
