package com.gwhitemore.zentv

import android.content.ComponentName
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gwhitemore.zentv.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var appScanner: AppScanner
    private lateinit var wallpaperEngine: WallpaperEngine
    private lateinit var adapter: AppAdapter

    private var currentApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        appScanner = AppScanner(this)
        wallpaperEngine = WallpaperEngine(this)

        setupImmersiveMode()
        setupClockAndDate()
        setupRecyclerView()
        setupSettingsButton()
        applyWallpaper()
    }

    override fun onResume() {
        super.onResume()
        setupImmersiveMode()
        updateDate()
        refreshApps()
    }

    private fun setupImmersiveMode() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    private fun setupClockAndDate() {
        updateDate()
        // Format clock according to settings or system default
        val format = if (prefs.use24Hour) {
            getString(R.string.clock_format_24)
        } else {
            getString(R.string.clock_format_12)
        }
        binding.textClock.format12Hour = format
        binding.textClock.format24Hour = format
    }

    private fun updateDate() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(Date())
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter(
            apps = emptyList(),
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> showAppOptionsDialog(app) }
        )

        binding.rvApps.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvApps.adapter = adapter
    }

    private fun setupSettingsButton() {
        binding.btnSettings.setOnClickListener {
            showLauncherSettingsDialog()
        }
    }

    private fun refreshApps() {
        lifecycleScope.launch {
            val apps = appScanner.loadInstalledApps()
            currentApps = apps
            adapter.updateApps(apps)

            if (apps.isEmpty()) {
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.tvEmpty.visibility = View.GONE
                // Give focus to the first item if nothing currently focused
                if (binding.rvApps.findFocus() == null && binding.btnSettings.findFocus() == null) {
                    binding.rvApps.post {
                        val firstHolder = binding.rvApps.findViewHolderForAdapterPosition(0)
                        firstHolder?.itemView?.requestFocus()
                    }
                }
            }
        }
    }

    private fun launchApp(app: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                val explicitIntent = Intent(Intent.ACTION_MAIN).apply {
                    component = ComponentName(app.packageName, app.className)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(explicitIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppOptionsDialog(app: AppInfo) {
        val pinOption = if (app.isPinned) getString(R.string.unfavorite_app) else getString(R.string.favorite_app)
        val options = arrayOf(
            getString(R.string.launch_app),
            pinOption,
            getString(R.string.hide_app),
            getString(R.string.app_info)
        )

        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchApp(app)
                    1 -> {
                        if (app.isPinned) {
                            prefs.unpinApp(app.packageName)
                        } else {
                            prefs.pinApp(app.packageName)
                        }
                        refreshApps()
                    }
                    2 -> {
                        prefs.hideApp(app.packageName)
                        refreshApps()
                        Toast.makeText(this, "${app.label} hidden", Toast.LENGTH_SHORT).show()
                    }
                    3 -> openAppInfo(app.packageName)
                }
            }
            .show()
    }

    private fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open app info", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLauncherSettingsDialog() {
        val options = arrayOf(
            getString(R.string.choose_wallpaper),
            "Unhide All Hidden Apps (${prefs.getHiddenApps().size} hidden)",
            "Toggle 12h / 24h Clock",
            "Open Fire TV Settings"
        )

        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle(getString(R.string.settings))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showWallpaperSelectionDialog()
                    1 -> {
                        prefs.clearAllHidden()
                        refreshApps()
                        Toast.makeText(this, "All apps unhidden", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        prefs.use24Hour = !prefs.use24Hour
                        setupClockAndDate()
                    }
                    3 -> {
                        startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                }
            }
            .show()
    }

    private fun showWallpaperSelectionDialog() {
        val wallpapers = arrayOf(
            "Bing Daily Nature Wallpaper (Automatic)",
            getString(R.string.wallpaper_gradient_aurora),
            getString(R.string.wallpaper_gradient_deep_space),
            getString(R.string.wallpaper_gradient_sunset),
            "Custom Wallpaper (/sdcard/Download/wallpaper.jpg)"
        )

        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle(getString(R.string.choose_wallpaper))
            .setItems(wallpapers) { _, which ->
                when (which) {
                    0 -> {
                        prefs.selectedWallpaper = PreferencesManager.WALLPAPER_BING
                        applyWallpaper()
                    }
                    1 -> {
                        prefs.selectedWallpaper = PreferencesManager.WALLPAPER_AURORA
                        applyWallpaper()
                    }
                    2 -> {
                        prefs.selectedWallpaper = PreferencesManager.WALLPAPER_SPACE
                        applyWallpaper()
                    }
                    3 -> {
                        prefs.selectedWallpaper = PreferencesManager.WALLPAPER_CYBER
                        applyWallpaper()
                    }
                    4 -> {
                        val defaultCustom = "/sdcard/Download/wallpaper.jpg"
                        prefs.customWallpaperPath = defaultCustom
                        prefs.selectedWallpaper = PreferencesManager.WALLPAPER_CUSTOM
                        applyWallpaper()
                    }
                }
            }
            .show()
    }

    private fun applyWallpaper() {
        when (prefs.selectedWallpaper) {
            PreferencesManager.WALLPAPER_BING -> {
                // Initial fallback gradient while loading
                binding.ivWallpaper.setImageResource(R.drawable.bg_wallpaper_aurora)
                lifecycleScope.launch {
                    val (bitmap, title) = wallpaperEngine.getDailyBingWallpaper()
                    if (bitmap != null) {
                        binding.ivWallpaper.animate().alpha(0.3f).setDuration(150).withEndAction {
                            binding.ivWallpaper.setImageDrawable(BitmapDrawable(resources, bitmap))
                            binding.ivWallpaper.animate().alpha(1.0f).setDuration(300).start()
                        }.start()
                        if (title != null) {
                            Toast.makeText(this@MainActivity, "Bing: $title", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            PreferencesManager.WALLPAPER_SPACE -> {
                binding.ivWallpaper.setImageResource(R.drawable.bg_wallpaper_space)
            }
            PreferencesManager.WALLPAPER_CYBER -> {
                binding.ivWallpaper.setImageResource(R.drawable.bg_wallpaper_cyber)
            }
            PreferencesManager.WALLPAPER_CUSTOM -> {
                val path = prefs.customWallpaperPath ?: "/sdcard/Download/wallpaper.jpg"
                val file = File(path)
                if (file.exists()) {
                    try {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            binding.ivWallpaper.setImageDrawable(BitmapDrawable(resources, bitmap))
                            return
                        }
                    } catch (e: Exception) {
                        // Fallback
                    }
                }
                binding.ivWallpaper.setImageResource(R.drawable.bg_wallpaper_aurora)
            }
            else -> {
                binding.ivWallpaper.setImageResource(R.drawable.bg_wallpaper_aurora)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // TV Remote Navigation Overrides
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                // Prevent exiting launcher on back press
                return true
            }
            KeyEvent.KEYCODE_MENU -> {
                // Menu button on remote opens app options for currently focused app
                val focusedChild = binding.rvApps.findFocus()
                if (focusedChild != null) {
                    val pos = adapter.focusedPosition
                    if (pos in currentApps.indices) {
                        showAppOptionsDialog(currentApps[pos])
                        return true
                    }
                } else {
                    showLauncherSettingsDialog()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
