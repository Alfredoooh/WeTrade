package com.wesports.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.wesports.app.databinding.ActivityMainBinding
import com.wesports.app.ui.HomeFragment
import com.wesports.app.ui.ServersFragment
import com.wesports.app.ui.TweaksFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var pendingVpnAction: (() -> Unit)? = null

    companion object {
        const val VPN_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadFragment(HomeFragment())

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { loadFragment(HomeFragment()); true }
                R.id.nav_servers -> { loadFragment(ServersFragment()); true }
                R.id.nav_tweaks -> { loadFragment(TweaksFragment()); true }
                else -> false
            }
        }
    }

    fun requestVpnPermission(onGranted: () -> Unit) {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            pendingVpnAction = onGranted
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            onGranted()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            pendingVpnAction?.invoke()
            pendingVpnAction = null
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}