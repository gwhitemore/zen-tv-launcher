package com.gwhitemore.zentv

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("zentv_prefs", Context.MODE_PRIVATE)

    companion object {
        const val WALLPAPER_AURORA = "aurora"
        const val WALLPAPER_SPACE = "space"
        const val WALLPAPER_CYBER = "cyber"
        const val WALLPAPER_CUSTOM = "custom"

        private const val KEY_WALLPAPER = "key_wallpaper"
        private const val KEY_CUSTOM_WALLPAPER_PATH = "key_custom_wallpaper_path"
        private const val KEY_PINNED_APPS = "key_pinned_apps"
        private const val KEY_HIDDEN_APPS = "key_hidden_apps"
        private const val KEY_USE_24_HOUR = "key_use_24_hour"
    }

    var selectedWallpaper: String
        get() = prefs.getString(KEY_WALLPAPER, WALLPAPER_AURORA) ?: WALLPAPER_AURORA
        set(value) = prefs.edit().putString(KEY_WALLPAPER, value).apply()

    var customWallpaperPath: String?
        get() = prefs.getString(KEY_CUSTOM_WALLPAPER_PATH, null)
        set(value) = prefs.edit().putString(KEY_CUSTOM_WALLPAPER_PATH, value).apply()

    fun getPinnedApps(): Set<String> {
        return prefs.getStringSet(KEY_PINNED_APPS, emptySet()) ?: emptySet()
    }

    fun pinApp(packageName: String) {
        val current = getPinnedApps().toMutableSet()
        current.add(packageName)
        prefs.edit().putStringSet(KEY_PINNED_APPS, current).apply()
    }

    fun unpinApp(packageName: String) {
        val current = getPinnedApps().toMutableSet()
        current.remove(packageName)
        prefs.edit().putStringSet(KEY_PINNED_APPS, current).apply()
    }

    fun getHiddenApps(): Set<String> {
        return prefs.getStringSet(KEY_HIDDEN_APPS, emptySet()) ?: emptySet()
    }

    fun hideApp(packageName: String) {
        val current = getHiddenApps().toMutableSet()
        current.add(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, current).apply()
    }

    fun unhideApp(packageName: String) {
        val current = getHiddenApps().toMutableSet()
        current.remove(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN_APPS, current).apply()
    }

    fun clearAllHidden() {
        prefs.edit().remove(KEY_HIDDEN_APPS).apply()
    }

    var use24Hour: Boolean
        get() = prefs.getBoolean(KEY_USE_24_HOUR, false)
        set(value) = prefs.edit().putBoolean(KEY_USE_24_HOUR, value).apply()
}
