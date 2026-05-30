// MainActivity.kt
package com.wilin.app

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
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
import com.wilin.app.ui.TabScreenshots
import com.wilin.app.ui.TabsActivity
import java.util.Locale

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

    override fun attachBaseContext(newBase: Context) {
        // Aplica o locale guardado antes de inflar qualquer view
        val prefs  = newBase.getSharedPreferences("wilin_prefs", Context.MODE_PRIVATE)
        val lang   = prefs.getString("language", "") ?: ""
        val base   = if (lang.isNotEmpty()) {
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        } else newBase
        super.attachBaseContext(base)
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

        // Edge-to-edge mas com barra de sistema gerida manualmente
        WindowCompat.setDecorFitsSystemWindows(window, true)
        insetsController = WindowInsetsControllerCompat(window, window.decorView)

        // Aplica o tema da status bar DEPOIS de a view estar pronta
        binding.root.post { applyStatusBarTheme() }

        TabManager.init(this)
        TabScreenshots.init(this)

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
        binding.drawerChevronSettings.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 14, iconSec))
        binding.drawerChevronAbout.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 14, iconSec))

        binding.drawerItemSettings.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.drawerItemAbout.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        }

        updateTabsBadge()

        fun setIcons(activeTab: Int) {
            val isNight  = resources.configuration.isNightModeActive
            val active   = if (isNight) Color.WHITE else Color.BLACK
            val inactive = Color.parseColor("#888888")

            binding.tabHomeIcon.setImageDrawable(
                if (activeTab == R.id.tabHome)
                    svgDrawable("icons/svg/home_filled.svg", 24, active)
                else
                    svgDrawable("icons/svg/home_outline.svg", 24, inactive)
            )
            binding.tabSearchIcon.setImageDrawable(
                if (activeTab == R.id.tabSearch)
                    svgDrawable("icons/svg/magnifying_glass_filled.svg", 24, active)
                else
                    svgDrawable("icons/svg/magnifying_glass_outline.svg", 24, inactive)
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

        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val tabId = intent?.getStringExtra(BrowserResponseActivity.EXTRA_TAB_ID)
        val query = intent?.getStringExtra(BrowserResponseActivity.EXTRA_QUERY)
        if (!tabId.isNullOrEmpty() || !query.isNullOrEmpty()) {
            val browserIntent = Intent(this, BrowserResponseActivity::class.java)
            if (!tabId.isNullOrEmpty()) browserIntent.putExtra(BrowserResponseActivity.EXTRA_TAB_ID, tabId)
            if (!query.isNullOrEmpty()) browserIntent.putExtra(BrowserResponseActivity.EXTRA_QUERY, query)
            startActivity(browserIntent)
        }
    }

    override fun onHomeScrollDown(dy: Int) {}
    override fun onHomeScrollUp(dy: Int)   {}

    @Suppress("UNCHECKED_CAST")
    fun showBottomNav() {
        val lp = binding.bottomNavWrapper.layoutParams as CoordinatorLayout.LayoutParams
        val behavior = lp.behavior as? HideBottomViewOnScrollBehavior<View> ?: return
        behavior.slideUp(binding.bottomNavWrapper)
    }

    override fun onResume() {
        super.onResume()
        // Garante que ao voltar do browser/settings a statusBar fica correcta
        binding.root.post { applyStatusBarTheme() }
        updateTabsBadge()
        showBottomNav()
    }

    // Chamado sempre que a janela recupera o foco (ex.: após voltar de outra Activity)
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyStatusBarTheme()
    }

    private fun openTabsWithTransform() {
        val root = binding.root
        val screenshot = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(screenshot))
        TabScreenshots.saveCurrent(this, TabManager.getCurrentId(), screenshot)

        val intent = Intent(this, TabsActivity::class.java).apply {
            putExtra("anim_src_width",  root.width)
            putExtra("anim_src_height", root.height)
            putExtra("anim_src_x",      0f)
            putExtra("anim_src_y",      0f)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
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

    // ── StatusBar: sempre consistente com o tema actual ───────────────────────
    private fun applyStatusBarTheme() {
        val isLight = !resources.configuration.isNightModeActive
        window.statusBarColor = ContextCompat.getColor(this, R.color.appbar_background)
        insetsController.isAppearanceLightStatusBars = isLight
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