package com.wilin.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.caverock.androidsvg.SVG
import com.google.android.material.search.SearchView
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

        binding.searchView.setupWithSearchBar(binding.searchBar)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        val iconTint          = ContextCompat.getColor(this, R.color.icon_tint)
        val iconTintSecondary = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        // Toolbar
        binding.toolbar.navigationIcon = svgDrawable("icons/svg/menu.svg", 24, iconTint)
        binding.toolbar.setNavigationOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // SearchBar — substituir ícone de lupa Material pelo SVG do projecto
        binding.searchBar.navigationIcon = svgDrawable("icons/svg/magnifying_glass_outline.svg", 24, iconTintSecondary)

        // SearchView — substituir ícone de voltar e limpar
        binding.searchView.toolbar.navigationIcon = svgDrawable("icons/svg/back_arrow.svg", 24, iconTint)

        // Drawer
        binding.drawerIconSettings.setImageDrawable(svgDrawable("icons/svg/settings.svg", 24, iconTint))
        binding.drawerIconAbout.setImageDrawable(svgDrawable("icons/svg/about.svg", 24, iconTint))

        binding.drawerItemSettings.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.drawerItemAbout.setOnClickListener {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        // SearchView — ao submeter navega para BrowserResponseActivity
        binding.searchView.editText.setOnEditorActionListener { textView, _, _ ->
            val query = textView.text.toString().trim()
            if (query.isNotEmpty()) {
                binding.searchView.hide()
                startActivity(
                    Intent(this, BrowserResponseActivity::class.java)
                        .putExtra("query", query)
                )
            }
            false
        }

        // Bloquear/desbloquear drawer conforme SearchView
        binding.searchView.addTransitionListener { _, _, newState ->
            if (newState == SearchView.TransitionState.SHOWING ||
                newState == SearchView.TransitionState.SHOWN) {
                binding.drawerLayout.setDrawerLockMode(
                    androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                )
            } else {
                if (currentTab != R.id.tabSearch) {
                    binding.drawerLayout.setDrawerLockMode(
                        androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED
                    )
                }
            }
        }

        val iconActive   = ContextCompat.getColor(this, R.color.icon_tint)
        val iconInactive = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        fun setIcons(activeTab: Int) {
            binding.tabHomeIcon.setImageDrawable(
                svgDrawable("icons/svg/home_${if (activeTab == R.id.tabHome) "filled" else "outline"}.svg",
                    24, if (activeTab == R.id.tabHome) iconActive else iconInactive))
            binding.tabSearchIcon.setImageDrawable(
                svgDrawable("icons/svg/magnifying_glass_${if (activeTab == R.id.tabSearch) "filled" else "outline"}.svg",
                    24, if (activeTab == R.id.tabSearch) iconActive else iconInactive))
            binding.tabGamesIcon.setImageDrawable(
                svgDrawable("icons/svg/game_${if (activeTab == R.id.tabGames) "filled" else "outline"}.svg",
                    24, if (activeTab == R.id.tabGames) iconActive else iconInactive))
        }

        fun selectTab(tabId: Int) {
            if (currentTab == tabId) return
            currentTab = tabId
            setIcons(tabId)

            if (tabId == R.id.tabSearch) {
                binding.appBarLayout.visibility = View.GONE
                binding.searchBar.visibility = View.VISIBLE
                binding.drawerLayout.setDrawerLockMode(
                    androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                )
            } else {
                binding.appBarLayout.visibility = View.VISIBLE
                binding.searchBar.visibility = View.GONE
                binding.drawerLayout.setDrawerLockMode(
                    androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED
                )
            }

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
    }

    override fun onBackPressed() {
        if (binding.searchView.isShowing) {
            binding.searchView.hide()
            return
        }
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            return
        }
        super.onBackPressed()
    }

    fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
        val px = (sizeDp * resources.displayMetrics.density).toInt()
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val svg = SVG.getFromAsset(assets, path)
        svg.documentWidth = px.toFloat()
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