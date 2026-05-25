package com.wilin.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
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
import com.wilin.app.ui.HubFragment
import com.wilin.app.ui.SearchActivity
import com.wilin.app.ui.SearchFragment
import com.wilin.app.ui.SettingsActivity
import com.wilin.app.ui.TabManager
import com.wilin.app.ui.TabsActivity

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var insetsController: WindowInsetsControllerCompat

    private val homeFragment   = HomeFragment()
    private val searchFragment = SearchFragment()
    private val hubFragment    = HubFragment()

    private var currentTab = R.id.tabHome

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

        // Logo
        try {
            val stream = assets.open("icons/app/app_icon.png")
            val bmp = BitmapFactory.decodeStream(stream)
            stream.close()
            binding.toolbarAppIcon.setImageBitmap(bmp)
        } catch (_: Exception) {}

        // Ask AI — cinzento, à esquerda (imediatamente a seguir ao logo)
        binding.btnAskAiIcon.setImageDrawable(svgDrawable("icons/svg/ai.svg", 13, iconSec))
        binding.btnAskAi.setOnClickListener {
            startActivity(Intent(this, AiSearchActivity::class.java))
        }

        // Botão menu
        binding.btnMenu.setImageDrawable(svgDrawable("icons/svg/menu.svg", 24, iconTint))
        binding.btnMenu.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.END))
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            else
                binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        // Search pill
        binding.searchPillIcon.setImageDrawable(
            svgDrawable("icons/svg/magnifying_glass_outline.svg", 18, iconSec))
        binding.searchPill.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        // Drawer
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

        // Contador de abas
        updateTabsBadge()

        fun setIcons(activeTab: Int) {
            // Home
            binding.tabHomeIcon.setImageDrawable(
                if (activeTab == R.id.tabHome)
                    svgDrawableGradient("icons/svg/home_filled.svg", 24)
                else
                    svgDrawable("icons/svg/home_outline.svg", 24, iconSec)
            )
            // Search
            binding.tabSearchIcon.setImageDrawable(
                if (activeTab == R.id.tabSearch)
                    svgDrawableGradient("icons/svg/magnifying_glass_filled.svg", 24)
                else
                    svgDrawable("icons/svg/magnifying_glass_outline.svg", 24, iconSec)
            )
            // Hub
            binding.tabHubIcon.setImageDrawable(
                if (activeTab == R.id.tabHub)
                    svgDrawableGradient("icons/svg/hub.svg", 24)
                else
                    svgDrawable("icons/svg/hub.svg", 24, iconSec)
            )
        }

        fun updateAppBar(tabId: Int) {
            if (tabId == R.id.tabSearch) {
                binding.toolbarAppIcon.visibility = View.GONE
                binding.btnMenu.visibility        = View.GONE
                binding.btnAskAi.visibility       = View.GONE
                binding.searchPill.visibility     = View.VISIBLE
            } else {
                binding.searchPill.visibility     = View.GONE
                binding.toolbarAppIcon.visibility = View.VISIBLE
                binding.btnMenu.visibility        = View.VISIBLE
                binding.btnAskAi.visibility       = View.VISIBLE
            }
        }

        fun selectTab(tabId: Int) {
            if (currentTab == tabId) return
            currentTab = tabId
            setIcons(tabId)
            updateAppBar(tabId)
            when (tabId) {
                R.id.tabHome   -> showFragment(homeFragment)
                R.id.tabSearch -> showFragment(searchFragment)
                R.id.tabHub    -> showFragment(hubFragment)
            }
        }

        binding.tabHome.setOnClickListener   { selectTab(R.id.tabHome) }
        binding.tabSearch.setOnClickListener { selectTab(R.id.tabSearch) }
        binding.tabHub.setOnClickListener    { selectTab(R.id.tabHub) }

        // Tab Abas — container transform: a janela atual encolhe e abre TabsActivity
        binding.tabTabs.setOnClickListener {
            openTabsWithTransform()
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.container, homeFragment, "home")
            .add(R.id.container, searchFragment, "search")
            .add(R.id.container, hubFragment, "hub")
            .hide(searchFragment)
            .hide(hubFragment)
            .commit()

        setIcons(R.id.tabHome)
        updateAppBar(R.id.tabHome)
    }

    override fun onResume() {
        super.onResume()
        applyStatusBarTheme()
        updateTabsBadge()
    }

    private fun openTabsWithTransform() {
        // Anima o container a encolher — simula "a tela vira card"
        val container = binding.container
        container.animate()
            .scaleX(0.88f).scaleY(0.88f)
            .alpha(0.7f)
            .setDuration(250)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .withEndAction {
                startActivity(Intent(this, TabsActivity::class.java))
                // Restaurar após lançar
                container.animate()
                    .scaleX(1f).scaleY(1f).alpha(1f)
                    .setDuration(0).start()
            }.start()
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

    /** Ícone SVG pintado com gradiente azul #007AFF → #00C6FF de cima para baixo */
    fun svgDrawableGradient(path: String, sizeDp: Int): BitmapDrawable {
        val px  = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val svg = SVG.getFromAsset(assets, path)
        svg.documentWidth  = px.toFloat()
        svg.documentHeight = px.toFloat()
        svg.renderToCanvas(Canvas(bmp))

        // Aplica gradiente usando Porter-Duff SRC_IN
        val gradientBmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val gradCanvas  = Canvas(gradientBmp)
        val gradPaint   = Paint()
        gradPaint.shader = LinearGradient(
            0f, 0f, 0f, px.toFloat(),
            Color.parseColor("#007AFF"),
            Color.parseColor("#00C6FF"),
            Shader.TileMode.CLAMP
        )
        gradCanvas.drawRect(0f, 0f, px.toFloat(), px.toFloat(), gradPaint)

        // Mask: usa o SVG como máscara
        val maskPaint = Paint()
        maskPaint.xfermode = android.graphics.PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        gradCanvas.drawBitmap(bmp, 0f, 0f, maskPaint)

        return BitmapDrawable(resources, gradientBmp)
    }

    fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
        val px  = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val svg = SVG.getFromAsset(assets, path)
        svg.documentWidth  = px.toFloat()
        svg.documentHeight = px.toFloat()
        svg.renderToCanvas(Canvas(bmp))
        val drawable = BitmapDrawable(resources, bmp)
        drawable.setColorFilter(tint, PorterDuff.Mode.SRC_IN)
        return drawable
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .hide(homeFragment).hide(searchFragment).hide(hubFragment)
            .show(fragment).commit()
    }
}