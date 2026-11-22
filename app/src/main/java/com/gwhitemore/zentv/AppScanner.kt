package com.gwhitemore.zentv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppScanner(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val prefs = PreferencesManager(context)

    suspend fun loadInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val appMap = LinkedHashMap<String, AppInfo>()
        val selfPackage = context.packageName
        val pinned = prefs.getPinnedApps()
        val hidden = prefs.getHiddenApps()

        // 1. Scan Leanback (TV) Apps first
        val tvIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val tvApps = queryIntentActivitiesSafe(tvIntent)
        for (info in tvApps) {
            val pkg = info.activityInfo.packageName
            if (pkg == selfPackage || hidden.contains(pkg)) continue

            val label = info.loadLabel(packageManager)?.toString() ?: pkg
            val icon = try {
                info.loadIcon(packageManager)
            } catch (e: Exception) {
                null
            }

            appMap[pkg] = AppInfo(
                label = label,
                packageName = pkg,
                className = info.activityInfo.name,
                icon = icon,
                isPinned = pinned.contains(pkg),
                isHidden = false
            )
        }

        // 2. Scan Standard/Sideloaded Mobile Apps
        val mobileIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val mobileApps = queryIntentActivitiesSafe(mobileIntent)
        for (info in mobileApps) {
            val pkg = info.activityInfo.packageName
            if (pkg == selfPackage || hidden.contains(pkg) || appMap.containsKey(pkg)) continue

            val label = info.loadLabel(packageManager)?.toString() ?: pkg
            val icon = try {
                info.loadIcon(packageManager)
            } catch (e: Exception) {
                null
            }

            appMap[pkg] = AppInfo(
                label = label,
                packageName = pkg,
                className = info.activityInfo.name,
                icon = icon,
                isPinned = pinned.contains(pkg),
                isHidden = false
            )
        }

        // 3. Sort: Pinned apps first (alphabetically), then unpinned apps (alphabetically)
        val sortedList = appMap.values.sortedWith(
            compareByDescending<AppInfo> { it.isPinned }
                .thenBy { it.label.lowercase() }
        )

        sortedList
    }

    private fun queryIntentActivitiesSafe(intent: Intent): List<ResolveInfo> {
        return try {
            packageManager.queryIntentActivities(intent, 0)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
