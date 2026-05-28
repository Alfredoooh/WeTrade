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
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.caverock.androidsvg.SVG
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.wilin.app.databinding.ActivityMainBinding
import com.wilin.app.ui.AiSearchActivity
import com.wilin.app.ui.BrowserResponseActivity
import com.wilin.app.ui.HomeFragment
import com.wilin.app.ui.HomeScrollCallback
import com.wilin.app.ui.SearchActivity
import com.wilin.app.ui.SearchFragment
import com.wilin.app.ui.SettingsActivity
import com.wilin.app.ui.TabManager
import com.wilin.app.ui.TabsActivity

class MainActivity : AppCompatActivity(), HomeScrollCallback {

    lateinit var binding: ActivityMainBinding
    private lateinit var insetsController: WindowInsetsControllerCompat

    private val homeFragment   = HomeFragment()
    private val searchFragment = SearchFragment()
    private var currentTab     = R.id.tabHome

    private val bottomNavBehavior by lazy {
        val lp = binding.bottomNavWrapper.layoutParams as CoordinatorLayout.LayoutParams
        lp.behavior as? HideBottomViewOnScrollBehavior<*>
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

        binding.searchPillIcon.setImageDrawable(
            svgDrawable("icons/svg/magnifying_glass_outline.svg", 18, iconSec))
        binding.searchPill.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        binding.drawerIconSettings.setImageDrawable(svgDrawable("icons/svg/settings.svg", 16, iconTint))
        binding.drawerIconAbout.setImageDrawable(svgDrawable("icons/svg/about.svg", 16, iconTint))
        binding.drawerChevronSettings.setImageDrawable(
            svgDrawable("icons/svg/chevron_right.svg", 14, iconSec))
        binding.drawerChevronAbout.setImageDrawable(
            svgDrawable("icons/svg/chevron_right.svg", 14, iconSec))

        binding.drawerItemSettings.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.drawerItemAbout.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }

        updateTabsBadge()

        fun setIcons(activeTab: Int) {
            // Ícones 20dp para o bottom bar compacto
            binding.tabHomeIcon.setImageDrawable(
                if (activeTab == R.id.tabHome)
                    svgDrawableGradient("icons/svg/home_filled.svg", 20)
                else
                    svgDrawable("icons/svg/home_outline.svg", 20, iconSec)
            )
            binding.tabSearchIcon.setImageDrawable(
                if (activeTab == R.id.tabSearch)
                    svgDrawableGradient("icons/svg/magnifying_glass_filled.svg", 20)
                else
                    svgDrawable("icons/svg/magnifying_glass_outline.svg", 20, iconSec)
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
            showBottomNav()
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

        // Se foi lançado com intent de abrir browser (ex: atalho externo), redireciona
        handleIncomingIntent(intent)
    }

    // Chamado quando a MainActivity já existe no stack e recebe um novo intent
    // Isso resolve o bug: ao sair do browser e voltar ao app, o sistema
    // faz onNewIntent na MainActivity (singleTask) em vez de criar nova instância
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        // Se o intent tiver um tab_id ou query, abrir directamente o browser
        val tabId = intent?.getStringExtra(BrowserResponseActivity.EXTRA_TAB_ID)
        val query = intent?.getStringExtra(BrowserResponseActivity.EXTRA_QUERY)
        if (!tabId.isNullOrEmpty() || !query.isNullOrEmpty()) {
            val browserIntent = Intent(this, BrowserResponseActivity::class.java)
            if (!tabId.isNullOrEmpty()) browserIntent.putExtra(BrowserResponseActivity.EXTRA_TAB_ID, tabId)
            if (!query.isNullOrEmpty()) browserIntent.putExtra(BrowserResponseActivity.EXTRA_QUERY, query)
            startActivity(browserIntent)
        }
    }

    override fun onHomeScrollDown(dy: Int) { /* CoordinatorLayout trata automaticamente */ }
    override fun onHomeScrollUp(dy: Int)   { /* CoordinatorLayout trata automaticamente */ }

    @Suppress("UNCHECKED_CAST")
    fun showBottomNav() {
        val lp = binding.bottomNavWrapper.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = lp.behavior as? HideBottomViewOnScrollBehavior<View> ?: return
        behavior.slideUp(binding.bottomNavWrapper)
    }

    override fun onResume() {
        super.onResume()
        applyStatusBarTheme()
        updateTabsBadge()
        showBottomNav()
        restoreRootTransform()
    }

    private fun openTabsWithTransform() {
        val root    = binding.root
        val dp      = resources.displayMetrics.density
        val screenH = resources.displayMetrics.heightPixels.toFloat()

        window.setBackgroundDrawable(ColorDrawable(Color.parseColor("#1C1C1E")))

        root.pivotX       = root.width / 2f
        root.pivotY       = root.height / 2f
        root.clipToOutline = true

        ValueAnimator.ofFloat(0f, 22f * dp).apply {
            duration = 340
            interpolator = DecelerateInterpolator(2.5f)
            addUpdateListener { anim ->
                val r = anim.animatedValue as Float
                root.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, r)
                    }
                }
            }
        }.start()

        root.animate()
            .scaleX(0.84f)
            .scaleY(0.84f)
            .translationY(-(screenH * 0.04f))
            .setDuration(340)
            .setInterpolator(DecelerateInterpolator(2.5f))
            .withEndAction {
                startActivity(Intent(this, TabsActivity::class.java))
                overridePendingTransition(0, 0)
            }
            .start()
    }

    private fun restoreRootTransform() {
        val root = binding.root
        if (root.scaleX == 1f && root.scaleY == 1f && root.translationY == 0f) return

        val dp = resources.displayMetrics.density
        ValueAnimator.ofFloat(22f * dp, 0f).apply {
            duration = 300
            interpolator = DecelerateInterpolator(2f)
            addUpdateListener { anim ->
                val r = anim.animatedValue as Float
                root.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, root.width, root.height, r)
                    }
                }
            }
        }.start()

        root.animate()
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator(2f))
            .withEndAction {
                root.clipToOutline = false
                root.outlineProvider = ViewOutlineProvider.BACKGROUND
                window.setBackgroundDrawable(
                    ColorDrawable(ContextCompat.getColor(this, R.color.background))
                )
            }
            .start()
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
            documentWidth  = px.toFloat()
            documentHeight = px.toFloat()
            renderToCanvas(Canvas(bmp))
        }
        val gb = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val gc = Canvas(gb)
        val gp = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, px.toFloat(),
                Color.parseColor("#007AFF"),
                Color.parseColor("#00C6FF"),
                Shader.TileMode.CLAMP
            )
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
            documentWidth  = px.toFloat()
            documentHeight = px.toFloat()
            renderToCanvas(Canvas(bmp))
        }
        return BitmapDrawable(resources, bmp).also {
            it.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .hide(homeFragment)
            .hide(searchFragment)
            .show(fragment)
            .commit()
    }
}