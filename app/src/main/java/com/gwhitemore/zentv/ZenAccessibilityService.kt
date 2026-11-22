package com.gwhitemore.zentv

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class ZenAccessibilityService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false

        // Intercept physical remote HOME button at the lowest input layer
        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            if (event.action == KeyEvent.ACTION_UP) {
                launchZenTV()
            }
            // Return true to consume the key event! Amazon PhoneWindowManager never receives it!
            return true
        }

        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Intercept if Amazon stock launcher attempts to take the window
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            val pkg = event.packageName?.toString() ?: return

            if (pkg == "com.amazon.tv.launcher" || pkg == "com.amazon.firehomestarter") {
                launchZenTV()
            }
        }
    }

    private fun launchZenTV() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        }
        startActivity(intent)
    }

    override fun onInterrupt() {}
}
