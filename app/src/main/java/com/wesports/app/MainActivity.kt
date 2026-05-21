package com.wesports.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.wesports.app.databinding.ActivityMainBinding
import com.wesports.app.model.Server
import com.wesports.app.ui.HomeFragment
import com.wesports.app.ui.ServersFragment
import com.wesports.app.ui.TweaksFragment

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private var pendingVpnAction: (() -> Unit)? = null
    var selectedServer: Server? = null

    companion object {
        const val VPN_REQUEST_CODE = 100
    }

    private val homeFragment = HomeFragment()
    private val serversFragment = ServersFragment()
    private val tweaksFragment = TweaksFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_settings -> {
                    Toast.makeText(this, "Definições", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_about -> {
                    Toast.makeText(this, "WeSports VPN v1.0", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        supportFragmentManager.beginTransaction()
            .add(R.id.container, homeFragment, "home")
            .add(R.id.container, serversFragment, "servers")
            .add(R.id.container, tweaksFragment, "tweaks")
            .hide(serversFragment)
            .hide(tweaksFragment)
            .commit()

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showFragment(homeFragment)
                R.id.nav_servers -> showFragment(serversFragment)
                R.id.nav_tweaks -> showFragment(tweaksFragment)
            }
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .hide(homeFragment)
            .hide(serversFragment)
            .hide(tweaksFragment)
            .show(fragment)
            .commit()
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
}