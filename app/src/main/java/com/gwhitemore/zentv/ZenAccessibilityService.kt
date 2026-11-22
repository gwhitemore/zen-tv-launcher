package com.gwhitemore.zentv

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class ZenAccessibilityService : AccessibilityService() {

    private var lastLaunchTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("ZenAccessibility", "ZenAccessibilityService CONNECTED!")

        // Listen only to window changes to keep CPU usage minimal
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_WINDOWS_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            notificationTimeout = 20
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: ""
        val cls = event.className?.toString() ?: ""

        // Intercept whenever Amazon launcher or starter takes over
        val isAmazonHome = pkg.contains("amazon.tv.launcher") ||
                           pkg.contains("firehomestarter") ||
                           (pkg.contains("amazon") && cls.contains("HomeActivity"))

        if (isAmazonHome) {
            Log.i("ZenAccessibility", "Amazon Home intercepted (pkg=$pkg, cls=$cls)! Returning to ZenTV...")
            launchZenTV()
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false

        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            Log.d("ZenAccessibility", "Intercepted KEYCODE_HOME action=${event.action}")
            if (event.action == KeyEvent.ACTION_UP) {
                launchZenTV()
            }
            return true // Consume HOME key event so Amazon framework doesn't get it
        }
        return super.onKeyEvent(event)
    }

    private fun launchZenTV() {
        val now = System.currentTimeMillis()
        if (now - lastLaunchTime < 100) {
            return
        }
        lastLaunchTime = now

        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                setAction(Intent.ACTION_MAIN)
                addCategory(Intent.CATEGORY_HOME)
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ZenAccessibility", "Failed to launch ZenTV", e)
        }
    }

    override fun onInterrupt() {}
}
