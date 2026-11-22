package com.gwhitemore.zentv

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WallpaperEngine(private val context: Context) {

    private val cacheFile = File(context.filesDir, "bing_daily.jpg")
    private val metaFile = File(context.filesDir, "bing_meta.txt")

    suspend fun getDailyBingWallpaper(): Pair<Bitmap?, String?> = withContext(Dispatchers.IO) {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val cachedDate = if (metaFile.exists()) metaFile.readText().trim() else ""

        // If today's image is already downloaded and valid, load it directly from flash storage
        if (cachedDate == today && cacheFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
            if (bitmap != null) {
                return@withContext Pair(bitmap, null)
            }
        }

        // Fetch fresh daily nature/landscape image from Bing Wallpaper API
        try {
            val apiUrl = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=0&n=1&mkt=en-US"
            val conn = URL(apiUrl).openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val jsonObj = JSONObject(jsonStr)
                val images = jsonObj.getJSONArray("images")
                if (images.length() > 0) {
                    val imgObj = images.getJSONObject(0)
                    val imgUrlPath = imgObj.optString("url")
                    val title = imgObj.optString("title")
                    val fullUrl = if (imgUrlPath.startsWith("http")) imgUrlPath else "https://www.bing.com$imgUrlPath"

                    // Download the high-res 1080p/4K landscape image
                    val imgConn = URL(fullUrl).openConnection() as HttpURLConnection
                    imgConn.connectTimeout = 10000
                    imgConn.readTimeout = 10000
                    if (imgConn.responseCode == 200) {
                        imgConn.inputStream.use { input ->
                            FileOutputStream(cacheFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        metaFile.writeText(today)
                        val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                        return@withContext Pair(bitmap, title)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Offline fallback: load previous cached wallpaper if available
        if (cacheFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                if (bitmap != null) return@withContext Pair(bitmap, null)
            } catch (e: Exception) {
                // Ignore
            }
        }

        Pair(null, null)
    }
}
