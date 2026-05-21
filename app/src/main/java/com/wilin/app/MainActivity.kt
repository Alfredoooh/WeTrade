package com.wilin.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.caverock.androidsvg.SVG
import com.wilin.app.databinding.ActivityMainBinding
import com.wilin.app.ui.GamesFragment
import com.wilin.app.ui.HomeFragment
import com.wilin.app.ui.SearchFragment
import com.wilin.app.ui.SettingsActivity

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    private val homeFragment = HomeFragment()
    private val searchFragment = SearchFragment()
    private val gamesFragment = GamesFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Ícone settings
        binding.btnSettings.setImageDrawable(svgDrawable("icons/svg/settings.svg", 24, Color.BLACK))

        // Ícones bottom nav
        binding.bottomNav.menu.findItem(R.id.nav_home).icon =
            svgStateDrawable("icons/svg/home_filled.svg", "icons/svg/home_outline.svg")
        binding.bottomNav.menu.findItem(R.id.nav_search).icon =
            svgStateDrawable("icons/svg/magnifying_glass_filled.svg", "icons/svg/magnifying_glass_outline.svg")
        binding.bottomNav.menu.findItem(R.id.nav_games).icon =
            svgStateDrawable("icons/svg/game_filled.svg", "icons/svg/game_outline.svg")

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.container, homeFragment, "home")
            .add(R.id.container, searchFragment, "search")
            .add(R.id.container, gamesFragment, "games")
            .hide(searchFragment)
            .hide(gamesFragment)
            .commit()

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showFragment(homeFragment)
                R.id.nav_search -> showFragment(searchFragment)
                R.id.nav_games -> showFragment(gamesFragment)
            }
            true
        }
    }

    private fun svgDrawable(path: String, sizeDp: Int, tint: Int = Color.BLACK): BitmapDrawable {
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

    private fun svgStateDrawable(filledPath: String, outlinePath: String): StateListDrawable {
        val filled = svgDrawable(filledPath, 24, Color.BLACK)
        val outline = svgDrawable(outlinePath, 24, Color.GRAY)
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_checked), filled)
            addState(intArrayOf(), outline)
        }
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