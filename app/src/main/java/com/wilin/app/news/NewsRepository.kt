package com.wilin.app.news

import com.wilin.app.ui.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object NewsRepository {

    private const val BASE_URL = "https://globeapiservice001.onrender.com"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun fetchList(category: String, limit: Int = 20): List<NewsItem> =
        withContext(Dispatchers.IO) {
            try {
                val req  = Request.Builder()
                    .url("$BASE_URL/news?category=$category&limit=$limit")
                    .build()
                val body = client.newCall(req).execute().body?.string() ?: return@withContext emptyList()
                val arr  = JSONArray(body)
                List(arr.length()) { i ->
                    val o = arr.getJSONObject(i)
                    NewsItem(
                        title       = o.optString("title"),
                        description = o.optString("description"),
                        imageUrl    = o.optString("image_url"),
                        sourceUrl   = o.optString("url"),
                        sourceName  = o.optString("source_name"),
                        faviconUrl  = o.optString("favicon_url"),
                        category    = o.optString("category"),
                        body        = o.optString("body"),
                        author      = o.optString("author"),
                        publishedAt = o.optString("published_at")
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

    suspend fun fetchDetail(url: String): NewsDetailItem =
        withContext(Dispatchers.IO) {
            try {
                val req  = Request.Builder()
                    .url("$BASE_URL/article?url=${java.net.URLEncoder.encode(url, "UTF-8")}")
                    .build()
                val body = client.newCall(req).execute().body?.string() ?: return@withContext NewsDetailItem()
                val o    = JSONObject(body)
                NewsDetailItem(
                    title       = o.optString("title"),
                    body        = o.optString("body"),
                    description = o.optString("description"),
                    imageUrl    = o.optString("image_url"),
                    author      = o.optString("author"),
                    publishedAt = o.optString("published_at"),
                    sourceName  = o.optString("source_name"),
                    sourceUrl   = url
                )
            } catch (_: Exception) {
                NewsDetailItem()
            }
        }
}

data class NewsDetailItem(
    val title       : String = "",
    val body        : String = "",
    val description : String = "",
    val imageUrl    : String = "",
    val author      : String = "",
    val publishedAt : String = "",
    val sourceName  : String = "",
    val sourceUrl   : String = ""
)