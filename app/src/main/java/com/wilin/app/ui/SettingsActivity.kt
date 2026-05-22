package com.wilin.app.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.caverock.androidsvg.SVG
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val blue = Color.parseColor("#007AFF")
        val grey = Color.parseColor("#CCCCCC")

        binding.iconLanguage.setImageDrawable(svgDrawable("icons/svg/language.svg", 24, blue))
        binding.iconAppearance.setImageDrawable(svgDrawable("icons/svg/appearance.svg", 24, blue))
        binding.iconNotifications.setImageDrawable(svgDrawable("icons/svg/notifications.svg", 24, blue))
        binding.iconPrivacy.setImageDrawable(svgDrawable("icons/svg/privacy.svg", 24, blue))
        binding.iconAbout.setImageDrawable(svgDrawable("icons/svg/about.svg", 24, blue))

        binding.iconChevronLanguage.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 20, grey))
        binding.iconChevronAppearance.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 20, grey))
        binding.iconChevronNotifications.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 20, grey))
        binding.iconChevronPrivacy.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 20, grey))
        binding.iconChevronAbout.setImageDrawable(svgDrawable("icons/svg/chevron_right.svg", 20, grey))

        val pInfo = packageManager.getPackageInfo(packageName, 0)
        binding.tvVersion.text = pInfo.versionName

        binding.itemLanguage.setOnClickListener { showLanguageDialog() }
    }

    private fun showLanguageDialog() {
        val names = languages.map { it.first }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.select_language))
            .setItems(names) { _, which ->
                setLocale(languages[which].second)
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
        resources.updateConfiguration(config, resources.displayMetrics)

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun svgDrawable(path: String, sizeDp: Int, tint: Int): BitmapDrawable {
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
}