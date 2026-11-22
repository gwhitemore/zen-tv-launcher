package com.gwhitemore.zentv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

object ColorExtractor {

    /**
     * Extracts a vivid, saturated brand color from an app icon/drawable.
     * Samples a small grid (fast & lightweight, <1ms), prioritizing high saturation
     * and medium-to-high brightness.
     */
    fun extractVividColor(drawable: Drawable?): Int {
        if (drawable == null) return Color.parseColor("#3B82F6") // default calm blue

        try {
            val bitmap = when (drawable) {
                is BitmapDrawable -> drawable.bitmap
                else -> {
                    val w = drawable.intrinsicWidth.coerceIn(48, 128)
                    val h = drawable.intrinsicHeight.coerceIn(48, 128)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable.setBounds(0, 0, w, h)
                    drawable.draw(canvas)
                    bmp
                }
            } ?: return Color.parseColor("#3B82F6")

            val stepX = (bitmap.width / 8).coerceAtLeast(1)
            val stepY = (bitmap.height / 8).coerceAtLeast(1)
            var maxScore = -1f
            var bestColor = Color.parseColor("#3B82F6")
            val hsv = FloatArray(3)

            for (x in 0 until bitmap.width step stepX) {
                for (y in 0 until bitmap.height step stepY) {
                    val pixel = bitmap.getPixel(x, y)
                    val alpha = Color.alpha(pixel)
                    if (alpha < 140) continue

                    Color.colorToHSV(pixel, hsv)
                    val saturation = hsv[1]
                    val value = hsv[2]

                    // Filter out near-blacks, near-whites, and pure grays
                    if (saturation > 0.35f && value > 0.30f && value < 0.95f) {
                        val score = saturation * value
                        if (score > maxScore) {
                            maxScore = score
                            bestColor = pixel
                        }
                    }
                }
            }
            return bestColor
        } catch (e: Exception) {
            return Color.parseColor("#3B82F6")
        }
    }
}
