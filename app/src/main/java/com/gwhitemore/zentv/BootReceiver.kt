package com.gwhitemore.zentv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i("ZenBootReceiver", "Received boot broadcast: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON") {

            try {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    setAction(Intent.ACTION_MAIN)
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                context.startActivity(launchIntent)
                Log.i("ZenBootReceiver", "ZenTV successfully launched on device boot!")
            } catch (e: Exception) {
                Log.e("ZenBootReceiver", "Failed to launch ZenTV on boot", e)
            }
        }
    }
}
