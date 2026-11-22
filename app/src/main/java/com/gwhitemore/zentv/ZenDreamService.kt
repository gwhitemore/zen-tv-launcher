package com.gwhitemore.zentv

import android.graphics.BitmapFactory
import android.service.dreams.DreamService
import android.util.Log
import android.view.LayoutInflater
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ZenDreamService : DreamService() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false // Dismiss dream on any remote keypress or D-Pad movement
        isFullscreen = true

        val layout = LayoutInflater.from(this).inflate(R.layout.view_screensaver, null)
        setContentView(layout)

        val ivWallpaper = layout.findViewById<ImageView>(R.id.ivDreamWallpaper)
        val tvDate = layout.findViewById<TextView>(R.id.tvDreamDate)

        // Uppercase localized date
        try {
            val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            tvDate.text = dateFormat.format(Date()).uppercase(Locale.getDefault())
        } catch (e: Exception) {
            tvDate.text = ""
        }

        // Load Bing Daily Nature Wallpaper
        scope.launch {
            try {
                val cacheFile = File(filesDir, "bing_daily.jpg")
                val bitmap = withContext(Dispatchers.IO) {
                    if (cacheFile.exists()) {
                        BitmapFactory.decodeFile(cacheFile.absolutePath)
                    } else {
                        val engine = WallpaperEngine(this@ZenDreamService)
                        val (bmp, _) = engine.getDailyBingWallpaper()
                        bmp
                    }
                }

                if (bitmap != null) {
                    ivWallpaper.setImageBitmap(bitmap)
                    ivWallpaper.alpha = 0f
                    ivWallpaper.animate().alpha(1f).setDuration(800).start()

                    // Subtle, cinematic Ken Burns slow zoom (1.00 -> 1.05 over 60s)
                    val kenBurns = ScaleAnimation(
                        1.00f, 1.05f, 1.00f, 1.05f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f
                    ).apply {
                        duration = 60000
                        repeatCount = Animation.INFINITE
                        repeatMode = Animation.REVERSE
                        fillAfter = true
                    }
                    ivWallpaper.startAnimation(kenBurns)
                }
            } catch (e: Exception) {
                Log.e("ZenDreamService", "Failed to load screensaver image", e)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope.cancel()
    }
}
