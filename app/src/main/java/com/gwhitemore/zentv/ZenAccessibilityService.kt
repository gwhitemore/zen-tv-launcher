package com.gwhitemore.zentv

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class ZenAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString() ?: return

            // If Amazon stock launcher attempts to display, immediately hijack and show ZenTV
            if (pkg == "com.amazon.tv.launcher" || pkg == "com.amazon.firehomestarter") {
                val intent = Intent(this, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }
                startActivity(intent)
            }
        }
    }

    override fun onInterrupt() {}
}
