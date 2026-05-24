package com.wilin.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
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
import com.wilin.app.ui.BrowserResponseActivity
import com.wilin.app.ui.GamesFragment
import com.wilin.app.ui.HomeFragment
import com.wilin.app.ui.SearchFragment
import com.wilin.app.ui.SettingsActivity

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    private val homeFragment   = HomeFragment()
    private val searchFragment = SearchFragment()
    private val gamesFragment  = GamesFragment()

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
        applyStatusBarTheme()
        window.decorView.post { applyStatusBarTheme() }

        val iconTint  = ContextCompat.getColor(this, R.color.icon_tint)
        val iconSec   = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        // Menu button
        binding.btnMenu.setImageDrawable(svgDrawable("icons/svg/menu.svg", 24, iconTint))
        binding.btnMenu.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START))
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            else
                binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Search pill no AppBar — abre BrowserResponseActivity directamente
        binding.searchPillIcon.setImageDrawable(svgDrawable("icons/svg/magnifying_glass_outline.svg", 18, iconSec))
        binding.searchPill.setOnClickListener {
            startActivity(Intent(this, BrowserResponseActivity::class.java))
        }

        // Drawer
        binding.drawerIconSettings.setImageDrawable(svgDrawable("icons/svg/settings.svg", 18, iconTint))
        binding.drawerIconAbout.setImageDrawable(svgDrawable("icons/svg/about.svg", 18, iconTint))
        binding.drawerChevronSettings.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 16, iconSec))
        binding.drawerChevronAbout.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 16, iconSec))

        binding.drawerItemSettings.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.drawerItemAbout.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        // Tab icons
        fun setIcons(activeTab: Int) {
            binding.tabHomeIcon.setImageDrawable(
                svgDrawable("icons/svg/home_${if (activeTab == R.id.tabHome) "filled" else "outline"}.svg",
                    24, if (activeTab == R.id.tabHome) iconTint else iconSec))
            binding.tabSearchIcon.setImageDrawable(
                svgDrawable("icons/svg/magnifying_glass_${if (activeTab == R.id.tabSearch) "filled" else "outline"}.svg",
                    24, if (activeTab == R.id.tabSearch) iconTint else iconSec))
            binding.tabGamesIcon.setImageDrawable(
                svgDrawable("icons/svg/game_${if (activeTab == R.id.tabGames) "filled" else "outline"}.svg",
                    24, if (activeTab == R.id.tabGames) iconTint else iconSec))
        }

        // AppBar: mostra pill quando tab search, título + menu quando outras tabs
        fun updateAppBar(tabId: Int) {
            if (tabId == R.id.tabSearch) {
                binding.toolbarTitle.visibility = android.view.View.GONE
                binding.btnMenu.visibility      = android.view.View.GONE
                binding.searchPill.visibility   = android.view.View.VISIBLE
            } else {
                binding.searchPill.visibility   = android.view.View.GONE
                binding.toolbarTitle.visibility = android.view.View.VISIBLE
                binding.btnMenu.visibility      = android.view.View.VISIBLE
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
                R.id.tabGames  -> showFragment(gamesFragment)
            }
        }

        binding.tabHome.setOnClickListener   { selectTab(R.id.tabHome) }
        binding.tabSearch.setOnClickListener { selectTab(R.id.tabSearch) }
        binding.tabGames.setOnClickListener  { selectTab(R.id.tabGames) }

        supportFragmentManager.beginTransaction()
            .add(R.id.container, homeFragment, "home")
            .add(R.id.container, searchFragment, "search")
            .add(R.id.container, gamesFragment, "games")
            .hide(searchFragment)
            .hide(gamesFragment)
            .commit()

        setIcons(R.id.tabHome)
        updateAppBar(R.id.tabHome)
    }

    override fun onResume() {
        super.onResume()
        applyStatusBarTheme()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyStatusBarTheme()
    }

    private fun applyStatusBarTheme() {
        val isLight = !resources.configuration.isNightModeActive
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars     = isLight
        controller.isAppearanceLightNavigationBars = isLight
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            return
        }
        super.onBackPressed()
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
            .hide(homeFragment)
            .hide(searchFragment)
            .hide(gamesFragment)
            .show(fragment)
            .commit()
    }
}