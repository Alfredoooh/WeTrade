package com.wilin.app.ui

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object TabScreenshots {
    private const val DIR_NAME = "tab_screenshots"

    private val cache = mutableMapOf<String, Bitmap>()
    private var baseDir: File? = null

    fun init(context: Context) {
        if (baseDir != null) return
        baseDir = File(context.filesDir, DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    private fun fileFor(tabId: String): File? {
        val dir = baseDir ?: return null
        return File(dir, "$tabId.png")
    }

    fun save(context: Context, tabId: String, bitmap: Bitmap) {
        init(context)
        cache[tabId]?.recycle()
        cache[tabId] = bitmap

        val outFile = fileFor(tabId) ?: return
        runCatching {
            FileOutputStream(outFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
        }
    }

    fun get(context: Context, tabId: String): Bitmap? {
        init(context)
        cache[tabId]?.let { cached ->
            if (!cached.isRecycled) return cached
            cache.remove(tabId)
        }

        val outFile = fileFor(tabId) ?: return null
        if (!outFile.exists()) return null

        return runCatching { BitmapFactoryHelper.decode(outFile) }.getOrNull()?.also {
            cache[tabId] = it
        }
    }

    fun remove(context: Context, tabId: String) {
        init(context)
        cache.remove(tabId)?.recycle()
        fileFor(tabId)?.delete()
    }

    private object BitmapFactoryHelper {
        fun decode(file: File): Bitmap? = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
    }
}
