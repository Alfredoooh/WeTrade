package com.wilin.app.ui

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class BrowserTab(
    val id: String,
    var url: String,
    var title: String,
    var favicon: String = ""
)

object TabManager {

    private const val PREFS = "wilin_tabs"
    private const val KEY   = "tabs"
    private const val KEY_CURRENT = "current_tab"

    private val tabs = mutableListOf<BrowserTab>()
    private var currentTabId: String = ""

    fun init(context: Context) {
        if (tabs.isNotEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY, null)
        currentTabId = prefs.getString(KEY_CURRENT, "") ?: ""
        if (json != null) {
            runCatching {
                val arr = JSONArray(json)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    tabs.add(BrowserTab(
                        id      = o.getString("id"),
                        url     = o.getString("url"),
                        title   = o.getString("title"),
                        favicon = o.optString("favicon", "")
                    ))
                }
            }
        }
        if (tabs.isEmpty()) {
            val t = newTab("https://duckduckgo.com")
            currentTabId = t.id
        }
        if (tabs.none { it.id == currentTabId }) currentTabId = tabs.first().id
    }

    fun getTabs(): List<BrowserTab> = tabs.toList()

    fun getCurrent(): BrowserTab? = tabs.find { it.id == currentTabId }

    fun setCurrentId(id: String) { currentTabId = id }

    fun getCurrentId(): String = currentTabId

    fun newTab(url: String = "https://duckduckgo.com"): BrowserTab {
        val t = BrowserTab(
            id    = System.currentTimeMillis().toString(),
            url   = url,
            title = url
        )
        tabs.add(t)
        currentTabId = t.id
        return t
    }

    fun updateTab(id: String, url: String? = null, title: String? = null, favicon: String? = null) {
        val t = tabs.find { it.id == id } ?: return
        url?.let     { t.url     = it }
        title?.let   { t.title   = it }
        favicon?.let { t.favicon = it }
    }

    fun closeTab(id: String) {
        val idx = tabs.indexOfFirst { it.id == id }
        if (idx == -1) return
        tabs.removeAt(idx)
        if (tabs.isEmpty()) {
            val t = newTab()
            currentTabId = t.id
        } else if (currentTabId == id) {
            currentTabId = tabs[minOf(idx, tabs.size - 1)].id
        }
    }

    fun count(): Int = tabs.size

    fun save(context: Context) {
        val arr = JSONArray()
        tabs.forEach { t ->
            arr.put(JSONObject().apply {
                put("id",      t.id)
                put("url",     t.url)
                put("title",   t.title)
                put("favicon", t.favicon)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, arr.toString())
            .putString(KEY_CURRENT, currentTabId)
            .apply()
    }
}