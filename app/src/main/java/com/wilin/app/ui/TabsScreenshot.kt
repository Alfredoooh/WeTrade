package com.wilin.app.ui

import android.graphics.Bitmap

object TabScreenshots {
    private val cache = mutableMapOf<String, Bitmap>()

    fun save(tabId: String, bitmap: Bitmap) {
        cache[tabId]?.recycle()
        cache[tabId] = bitmap
    }

    fun get(tabId: String): Bitmap? = cache[tabId]

    fun remove(tabId: String) {
        cache.remove(tabId)?.recycle()
    }
}