package com.gwhitemore.zentv

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class ZenAccessibilityService : AccessibilityService() {

    private var lastLaunchTime = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("ZenAccessibility", "ZenAccessibilityService CONNECTED!")

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

        // Intercept whenever Amazon launcher takes over
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
            return true
        }
        return super.onKeyEvent(event)
    }

    private fun launchZenTV() {
        val now = System.currentTimeMillis()
        if (now - lastLaunchTime < 150) {
            return
        }
        lastLaunchTime = now

        val triggerLaunch = {
            try {
                val intent = Intent(this, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                }
                val pi = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                pi.send()
            } catch (e: Exception) {
                try {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        action = Intent.ACTION_MAIN
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                        )
                    }
                    startActivity(intent)
                } catch (ex: Exception) {
                    Log.e("ZenAccessibility", "Failed to launch ZenTV", ex)
                }
            }
        }

        // Fire immediately and a short 80ms followup to defeat window animation lock
        triggerLaunch()
        mainHandler.postDelayed({ triggerLaunch() }, 80)
    }

    override fun onInterrupt() {}
}
