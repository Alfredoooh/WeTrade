package com.wilin.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.caverock.androidsvg.SVG
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.wilin.app.MainActivity
import com.wilin.app.R
import com.wilin.app.databinding.ActivitySettingsBinding
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val languages = arrayOf(
        "Português" to "pt",
        "English" to "en",
        "Español" to "es",
        "Français" to "fr",
        "Deutsch" to "de",
        "Italiano" to "it",
        "日本語" to "ja",
        "中文" to "zh",
        "한국어" to "ko",
        "Русский" to "ru",
        "العربية" to "ar",
        "हिन्दी" to "hi",
        "Türkçe" to "tr",
        "Afrikaans" to "af",
        "Nederlands" to "nl",
        "Polski" to "pl",
        "Svenska" to "sv",
        "Dansk" to "da",
        "Suomi" to "fi",
        "Norsk" to "no",
        "Čeština" to "cs",
        "Slovenčina" to "sk",
        "Magyar" to "hu",
        "Română" to "ro",
        "Українська" to "uk",
        "Ελληνικά" to "el",
        "עברית" to "he",
        "Bahasa Indonesia" to "id",
        "Bahasa Melayu" to "ms",
        "ภาษาไทย" to "th",
        "Tiếng Việt" to "vi"
    )

    private val themeOptions = arrayOf("Sistema", "Claro", "Escuro")
    private val themeValues  = arrayOf("system", "light", "dark")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        val isLight = !resources.configuration.isNightModeActive
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = getString(R.string.settings)

        val iconTint    = ContextCompat.getColor(this, R.color.icon_tint)
        val chevronTint = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        binding.toolbar.navigationIcon = svgDrawable("icons/svg/back_arrow.svg", 24, iconTint)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.iconLanguage.setImageDrawable(svgDrawable("icons/svg/language.svg", 24, iconTint))
        binding.iconAppearance.setImageDrawable(svgDrawable("icons/svg/appearance.svg", 24, iconTint))
        binding.iconNotifications.setImageDrawable(svgDrawable("icons/svg/notifications.svg", 24, iconTint))
        binding.iconPrivacy.setImageDrawable(svgDrawable("icons/svg/privacy.svg", 24, iconTint))
        binding.iconAbout.setImageDrawable(svgDrawable("icons/svg/about.svg", 24, iconTint))

        binding.iconChevronLanguage.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 20, chevronTint))
        binding.iconChevronAppearance.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 20, chevronTint))
        binding.iconChevronNotifications.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 20, chevronTint))
        binding.iconChevronPrivacy.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 20, chevronTint))
        binding.iconChevronAbout.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 20, chevronTint))

        val pInfo = packageManager.getPackageInfo(packageName, 0)
        binding.tvVersion.text = pInfo.versionName

        binding.itemLanguage.setOnClickListener { showLanguageDialog() }
        binding.itemAppearance.setOnClickListener { showThemeDialog() }
        binding.itemNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        binding.itemPrivacy.setOnClickListener {
            startActivity(Intent(this, PrivacyActivity::class.java))
        }
    }

    private fun showLanguageDialog() {
        val names = languages.map { it.first }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.select_language))
            .setItems(names) { _, which -> setLocale(languages[which].second) }
            .show()
    }

    private fun showThemeDialog() {
        val prefs        = getSharedPreferences("wilin_prefs", Context.MODE_PRIVATE)
        val current      = prefs.getString("theme", "system")
        val currentIndex = themeValues.indexOf(current).coerceAtLeast(0)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.appearance))
            .setSingleChoiceItems(themeOptions, currentIndex) { dialog, which ->
                val selected = themeValues[which]
                prefs.edit().putString("theme", selected).apply()
                when (selected) {
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "dark"  -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    else    -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun setLocale(langCode: String) {
        getSharedPreferences("wilin_prefs", Context.MODE_PRIVATE)
            .edit().putString("language", langCode).apply()

        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }

    private fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
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
}