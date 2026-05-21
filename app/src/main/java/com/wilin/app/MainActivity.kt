package com.wilin.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.wilin.app.databinding.ActivityMainBinding
import com.wilin.app.model.Server
import com.wilin.app.ui.HomeFragment
import com.wilin.app.ui.ServersFragment
import com.wilin.app.ui.TweaksFragment

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
        binding.toolbar.title = "wilin VPN"
        binding.toolbar.setTitleTextColor(android.graphics.Color.WHITE)

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_overflow -> {
                    showOverflowMenu()
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
                R.id.nav_home -> {
                    binding.toolbar.title = "wilin VPN"
                    showFragment(homeFragment)
                }
                R.id.nav_servers -> {
                    binding.toolbar.title = "Servidores"
                    showFragment(serversFragment)
                }
                R.id.nav_tweaks -> {
                    binding.toolbar.title = "Tweaks"
                    showFragment(tweaksFragment)
                }
            }
            true
        }
    }

    private fun showOverflowMenu() {
        val anchor = binding.toolbar.findViewById<android.view.View>(R.id.menu_overflow)
            ?: binding.toolbar
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, "Sobre")
        popup.menu.add(0, 2, 1, "Versão")
        popup.menu.add(0, 3, 2, "Configurações")
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                1 -> AlertDialog.Builder(this)
                    .setTitle("wilin VPN")
                    .setMessage("VPN com tunelamento SSL/HTTP via SNI.\nDesenvolvido para Angola.")
                    .setPositiveButton("OK", null)
                    .show()
                2 -> Toast.makeText(this, "Versão 1.0.0", Toast.LENGTH_SHORT).show()
                3 -> Toast.makeText(this, "Configurações em breve", Toast.LENGTH_SHORT).show()
            }
            true
        }
        popup.show()
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