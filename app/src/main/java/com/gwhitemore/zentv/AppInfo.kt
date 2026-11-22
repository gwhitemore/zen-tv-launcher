package com.gwhitemore.zentv

import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val className: String,
    val icon: Drawable?,
    var isPinned: Boolean = false,
    var isHidden: Boolean = false
)
