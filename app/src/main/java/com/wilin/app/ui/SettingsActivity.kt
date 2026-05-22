package com.wilin.app.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
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

    private val themeOptions = arrayOf("Sistema", "Claro", "Escuro")
    private val themeValues = arrayOf("system", "light", "dark")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = getString(R.string.settings)

        val iconTint = ContextCompat.getColor(this, R.color.icon_tint)
        val blue = ContextCompat.getColor(this, R.color.colorPrimary)
        val chevronTint = ContextCompat.getColor(this, R.color.icon_tint_secondary)

        binding.toolbar.navigationIcon = svgDrawable("icons/svg/back_arrow.svg", 24, iconTint)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.iconLanguage.setImageDrawable(svgDrawable("icons/svg/language.svg", 24, blue))
        binding.iconAppearance.setImageDrawable(svgDrawable("icons/svg/appearance.svg", 24, blue))
        binding.iconNotifications.setImageDrawable(svgDrawable("icons/svg/notifications.svg", 24, blue))
        binding.iconPrivacy.setImageDrawable(svgDrawable("icons/svg/privacy.svg", 24, blue))
        binding.iconAbout.setImageDrawable(svgDrawable("icons/svg/about.svg", 24, blue))

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

        val bgColor = ContextCompat.getColor(this, R.color.dialog_background)
        val textPrimary = ContextCompat.getColor(this, R.color.text_primary)
        val blue = ContextCompat.getColor(this, R.color.colorPrimary)
        val dividerColor = ContextCompat.getColor(this, R.color.divider)

        val listView = ListView(this).apply {
            val adapter = object : ArrayAdapter<String>(
                this@SettingsActivity,
                android.R.layout.simple_list_item_1,
                names
            ) {
                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                    val v = super.getView(position, convertView, parent)
                    (v as TextView).apply {
                        setTextColor(textPrimary)
                        setBackgroundColor(bgColor)
                        setPadding(
                            (16 * resources.displayMetrics.density).toInt(),
                            (14 * resources.displayMetrics.density).toInt(),
                            (16 * resources.displayMetrics.density).toInt(),
                            (14 * resources.displayMetrics.density).toInt()
                        )
                        textSize = 15f
                    }
                    return v
                }
            }
            setAdapter(adapter)
            divider = android.graphics.drawable.ColorDrawable(dividerColor)
            dividerHeight = 1
            setBackgroundColor(bgColor)
        }

        val titleView = TextView(this).apply {
            text = getString(R.string.select_language)
            setTextColor(textPrimary)
            textSize = 18f
            setPadding(
                (20 * resources.displayMetrics.density).toInt(),
                (20 * resources.displayMetrics.density).toInt(),
                (20 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt()
            )
            setBackgroundColor(bgColor)
        }

        val dialog = AlertDialog.Builder(this)
            .setCustomTitle(titleView)
            .setView(listView)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgColor))

        listView.setOnItemClickListener { _, _, which, _ ->
            dialog.dismiss()
            setLocale(languages[which].second)
        }

        dialog.show()
    }

    private fun showThemeDialog() {
        val prefs = getSharedPreferences("wilin_prefs", Context.MODE_PRIVATE)
        val current = prefs.getString("theme", "system")
        val currentIndex = themeValues.indexOf(current).coerceAtLeast(0)

        val bgColor = ContextCompat.getColor(this, R.color.dialog_background)
        val textPrimary = ContextCompat.getColor(this, R.color.text_primary)
        val blue = ContextCompat.getColor(this, R.color.colorPrimary)
        val dividerColor = ContextCompat.getColor(this, R.color.divider)

        val titleView = TextView(this).apply {
            text = getString(R.string.appearance)
            setTextColor(textPrimary)
            textSize = 18f
            setPadding(
                (20 * resources.displayMetrics.density).toInt(),
                (20 * resources.displayMetrics.density).toInt(),
                (20 * resources.displayMetrics.density).toInt(),
                (12 * resources.displayMetrics.density).toInt()
            )
            setBackgroundColor(bgColor)
        }

        var selectedIndex = currentIndex
        var dialog: AlertDialog? = null

        val listView = ListView(this).apply {
            val adapter = object : ArrayAdapter<String>(
                this@SettingsActivity,
                android.R.layout.simple_list_item_single_choice,
                themeOptions
            ) {
                override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                    val v = super.getView(position, convertView, parent)
                    (v as android.widget.CheckedTextView).apply {
                        setTextColor(textPrimary)
                        setBackgroundColor(bgColor)
                        setPadding(
                            (16 * resources.displayMetrics.density).toInt(),
                            (14 * resources.displayMetrics.density).toInt(),
                            (16 * resources.displayMetrics.density).toInt(),
                            (14 * resources.displayMetrics.density).toInt()
                        )
                        textSize = 15f
                        compoundDrawableTintList = android.content.res.ColorStateList.valueOf(blue)
                    }
                    return v
                }
            }
            setAdapter(adapter)
            choiceMode = ListView.CHOICE_MODE_SINGLE
            setItemChecked(currentIndex, true)
            divider = android.graphics.drawable.ColorDrawable(dividerColor)
            dividerHeight = 1
            setBackgroundColor(bgColor)
        }

        dialog = AlertDialog.Builder(this)
            .setCustomTitle(titleView)
            .setView(listView)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(bgColor))

        listView.setOnItemClickListener { _, _, which, _ ->
            selectedIndex = which
            val selected = themeValues[which]
            prefs.edit().putString("theme", selected).apply()
            when (selected) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark"  -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else    -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            dialog?.dismiss()
        }

        dialog.show()
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