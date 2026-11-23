package com.gwhitemore.zentv

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvContract
import android.net.ConnectivityManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var appScanner: AppScanner
    private lateinit var wallpaperEngine: WallpaperEngine
    private lateinit var adapter: AppAdapter

    private var currentApps: List<AppInfo> = emptyList()
    private var currentGlowColor: Int = Color.TRANSPARENT
    private var reorderingApp: AppInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        appScanner = AppScanner(this)
        wallpaperEngine = WallpaperEngine(this)

        setupImmersiveMode()
        setupHeader()
        setupTopShelf()
        setupRecyclerView()
        applyWallpaper()
    }

    override fun onResume() {
        super.onResume()
        overridePendingTransition(0, 0)
        setupImmersiveMode()
        updateHeaderInfo()
        refreshApps()
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        overridePendingTransition(0, 0)
        setupImmersiveMode()
        // Focus first app card when Home button is pressed
        binding.rvApps.post {
            val firstHolder = binding.rvApps.findViewHolderForAdapterPosition(0)
            firstHolder?.itemView?.requestFocus()
        }
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

    private fun setupHeader() {
        updateHeaderInfo()

        // Clock Format
        val format = if (prefs.use24Hour) {
            getString(R.string.clock_format_24)
        } else {
            getString(R.string.clock_format_12)
        }
        binding.textClock.format12Hour = format
        binding.textClock.format24Hour = format

        // Button Click Handlers
        binding.btnInputs.setOnClickListener {
            showInputsDialog()
        }

        binding.btnWifi.setOnClickListener {
            openWifiSettings()
        }

        binding.btnSettings.setOnClickListener {
            showLauncherSettingsDialog()
        }
    }

    private fun updateHeaderInfo() {
        // Dynamic greeting based on time of day
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when (hour) {
            in 5..11 -> getString(R.string.good_morning)
            in 12..16 -> getString(R.string.good_afternoon)
            else -> getString(R.string.good_evening)
        }

        // Formatted uppercase date
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(Date())

        // Live RAM stats
        try {
            val mi = ActivityManager.MemoryInfo()
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.getMemoryInfo(mi)
            val freeMb = mi.availMem / (1024 * 1024)
            binding.tvStats.text = "RAM: ${freeMb} MB FREE · ZERO ADS · FIRE OS DEBLOATED"
        } catch (e: Exception) {
            binding.tvStats.text = "CLEAN & DEBLOATED · ZERO ADS"
        }

        // Live Wi-Fi SSID
        binding.btnWifi.text = getConnectedWifiName()
    }

    private fun setupTopShelf() {
        binding.tvHeroName.text = getString(R.string.app_name)
        binding.tvHeroMeta.text = "Fire OS Clean Launcher"
    }

    private fun setupRecyclerView() {
        adapter = AppAdapter(
            apps = emptyList(),
            onAppClick = { app -> launchApp(app) },
            onAppLongClick = { app -> showAppOptionsDialog(app) },
            onAppFocus = { app, position ->
                binding.tvHeroName.text = app.label
                binding.tvHeroMeta.text = if (app.isPinned) "Pinned Favorite · Installed" else "Application · Installed"
                val vibrantColor = ColorExtractor.extractVividColor(app.icon)
                animateAmbientGlow(vibrantColor)
            }
        )

        binding.rvApps.layoutManager = LinearLayoutManager(
            this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
        binding.rvApps.adapter = adapter
    }

    private fun animateAmbientGlow(targetColor: Int) {
        val startColor = currentGlowColor
        currentGlowColor = targetColor

        val animator = ValueAnimator.ofObject(ArgbEvaluator(), startColor, targetColor)
        animator.duration = 240
        animator.addUpdateListener { va ->
            val color = va.animatedValue as Int
            // Radial glow behind the top shelf and cards
            val centerAlpha = 70 // subtle, elegant luminescence
            val centerColor = Color.argb(centerAlpha, Color.red(color), Color.green(color), Color.blue(color))

            val displayMetrics = resources.displayMetrics
            val gradient = GradientDrawable().apply {
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = displayMetrics.widthPixels * 0.70f
                setGradientCenter(0.5f, 0.72f)
                colors = intArrayOf(centerColor, Color.TRANSPARENT)
            }
            binding.vAmbientGlow.background = gradient
            binding.vAmbientGlow.alpha = 1.0f
        }
        animator.start()
    }

    private fun getConnectedWifiName(): String {
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            if (activeNetwork != null && activeNetwork.isConnected) {
                if (activeNetwork.type == ConnectivityManager.TYPE_ETHERNET) {
                    return "Ethernet"
                }
                val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info = wm.connectionInfo
                if (info != null) {
                    var ssid = info.ssid ?: ""
                    if (ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length > 2) {
                        ssid = ssid.substring(1, ssid.length - 1)
                    }
                    if (ssid.isNotEmpty() && ssid != "<unknown ssid>") {
                        return ssid
                    }
                }
                return getString(R.string.wifi)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error reading Wi-Fi", e)
        }
        return getString(R.string.wifi)
    }

    private fun openWifiSettings() {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e2: Exception) {
                Toast.makeText(this, "Could not open Network Settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showInputsDialog() {
        val options = arrayOf(
            "HDMI Passthrough Input",
            "Live TV & Channel Guide",
            "Display & Sounds Settings",
            "Equipment Control"
        )
        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle(getString(R.string.inputs))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                component = ComponentName(
                                    "com.amazon.franktvinput",
                                    "com.amazon.franktvinput.playback.TvPlaybackActivity"
                                )
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                            } catch (e2: Exception) {
                                Toast.makeText(this, "HDMI Input not active", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    1 -> {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelsUriForInput(null)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Live TV input unavailable", Toast.LENGTH_SHORT).show()
                        }
                    }
                    2 -> {
                        try {
                            startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (e: Exception) {
                            startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                    3 -> {
                        try {
                            val intent = Intent().apply {
                                component = ComponentName(
                                    "com.amazon.tv.devicecontrolsettings",
                                    "com.amazon.tv.devicecontrolsettings.DeviceControlSettingsActivity"
                                )
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                }
            }
            .show()
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
                if (binding.rvApps.findFocus() == null &&
                    binding.btnSettings.findFocus() == null &&
                    binding.btnWifi.findFocus() == null &&
                    binding.btnInputs.findFocus() == null) {
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

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            // Interactive App Move Mode handler
            if (reorderingApp != null) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        moveApp(reorderingApp!!, -1)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        moveApp(reorderingApp!!, 1)
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_ENTER,
                    KeyEvent.KEYCODE_BACK -> {
                        stopReorderMode()
                        return true
                    }
                }
            }

            // Quick Menu Key handler (remote Menu button)
            if (event.keyCode == KeyEvent.KEYCODE_MENU) {
                val focusedPos = adapter.focusedPosition
                val focusedApp = adapter.getApps().getOrNull(focusedPos)
                if (focusedApp != null && binding.rvApps.findFocus() != null) {
                    showAppOptionsDialog(focusedApp)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun moveApp(app: AppInfo, direction: Int) {
        val list = adapter.getApps().toMutableList()
        val index = list.indexOfFirst { it.packageName == app.packageName }
        if (index == -1) return
        val targetIndex = index + direction
        if (targetIndex in 0 until list.size) {
            val item = list.removeAt(index)
            list.add(targetIndex, item)
            prefs.saveCustomAppOrder(list.map { it.packageName })
            adapter.updateApps(list)
            binding.rvApps.post {
                val holder = binding.rvApps.findViewHolderForAdapterPosition(targetIndex)
                holder?.itemView?.findViewById<View>(R.id.cardContainer)?.requestFocus()
            }
        }
    }

    private fun startReorderMode(app: AppInfo) {
        reorderingApp = app
        adapter.setReorderingApp(app.packageName)
        binding.tvHeroName.text = "Moving: ${app.label}"
        binding.tvHeroMeta.text = "Press ◀ / ▶ on remote to reorder. Press SELECT when done."
        Toast.makeText(this, "Use ◀ / ▶ to move ${app.label}. Press SELECT when done.", Toast.LENGTH_LONG).show()
    }

    private fun stopReorderMode() {
        val app = reorderingApp
        reorderingApp = null
        adapter.setReorderingApp(null)
        if (app != null) {
            binding.tvHeroName.text = app.label
            binding.tvHeroMeta.text = "Application · Installed"
            Toast.makeText(this, "${app.label} position saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppOptionsDialog(app: AppInfo) {
        val pinOption = if (app.isPinned) getString(R.string.unfavorite_app) else getString(R.string.favorite_app)
        val options = arrayOf(
            getString(R.string.launch_app),
            "Move Left ◀",
            "Move Right ▶",
            "Interactive Move Mode ↔",
            "Remove from Screen (Hide)",
            pinOption,
            getString(R.string.app_info)
        )

        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchApp(app)
                    1 -> moveApp(app, -1)
                    2 -> moveApp(app, 1)
                    3 -> startReorderMode(app)
                    4 -> {
                        prefs.hideApp(app.packageName)
                        refreshApps()
                        Toast.makeText(this, "${app.label} removed from screen", Toast.LENGTH_SHORT).show()
                    }
                    5 -> {
                        if (app.isPinned) {
                            prefs.unpinApp(app.packageName)
                        } else {
                            prefs.pinApp(app.packageName)
                        }
                        refreshApps()
                    }
                    6 -> openAppInfo(app.packageName)
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

    private fun showManageHiddenAppsDialog() {
        val hiddenPkgs = prefs.getHiddenApps().toList()
        if (hiddenPkgs.isEmpty()) {
            Toast.makeText(this, "No apps are currently hidden", Toast.LENGTH_SHORT).show()
            return
        }

        val hiddenLabels = hiddenPkgs.map { pkg ->
            try {
                val ai = packageManager.getApplicationInfo(pkg, 0)
                packageManager.getApplicationLabel(ai).toString()
            } catch (e: Exception) {
                pkg
            }
        }.toTypedArray()

        val items = hiddenLabels.map { "Unhide $it" }.toMutableList()
        items.add("Unhide All Apps")

        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle("Manage Hidden Apps")
            .setItems(items.toTypedArray()) { _, which ->
                if (which == items.size - 1) {
                    prefs.clearAllHidden()
                    refreshApps()
                    Toast.makeText(this, "All apps restored to screen", Toast.LENGTH_SHORT).show()
                } else {
                    val pkgToUnhide = hiddenPkgs[which]
                    prefs.unhideApp(pkgToUnhide)
                    refreshApps()
                    Toast.makeText(this, "${hiddenLabels[which]} restored", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showLauncherSettingsDialog() {
        val hiddenCount = prefs.getHiddenApps().size
        val options = arrayOf(
            getString(R.string.choose_wallpaper),
            "Manage Removed / Hidden Apps ($hiddenCount hidden)",
            "Reset App Order to Default",
            "Toggle 12h / 24h Clock",
            "Open Fire TV Settings"
        )

        AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_Dialog_Alert)
            .setTitle(getString(R.string.settings))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showWallpaperSelectionDialog()
                    1 -> showManageHiddenAppsDialog()
                    2 -> {
                        prefs.resetCustomAppOrder()
                        refreshApps()
                        Toast.makeText(this, "App order reset to default", Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        prefs.use24Hour = !prefs.use24Hour
                        val format = if (prefs.use24Hour) getString(R.string.clock_format_24) else getString(R.string.clock_format_12)
                        binding.textClock.format12Hour = format
                        binding.textClock.format24Hour = format
                    }
                    4 -> {
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
                lifecycleScope.launch {
                    val pair = wallpaperEngine.getDailyBingWallpaper()
                    val bitmap = pair.first
                    if (bitmap != null) {
                        binding.ivWallpaper.setImageBitmap(bitmap)
                    } else {
                        binding.ivWallpaper.setImageResource(R.drawable.bg_wallpaper_aurora)
                    }
                }
            }
            PreferencesManager.WALLPAPER_AURORA -> {
                binding.ivWallpaper.setImageResource(R.drawable.bg_wallpaper_aurora)
            }
            PreferencesManager.WALLPAPER_SPACE -> {
                binding.ivWallpaper.setImageResource(R.drawable.bg_wallpaper_space)
            }
            PreferencesManager.WALLPAPER_CYBER -> {
                binding.ivWallpaper.setImageResource(R.drawable.bg_wallpaper_cyber)
            }
            PreferencesManager.WALLPAPER_CUSTOM -> {
                val customPath = prefs.customWallpaperPath ?: "/sdcard/Download/wallpaper.jpg"
                val customFile = File(customPath)
                if (customFile.exists()) {
                    val bitmap = BitmapFactory.decodeFile(customFile.absolutePath)
                    binding.ivWallpaper.setImageBitmap(bitmap)
                } else {
                    binding.ivWallpaper.setImageResource(R.drawable.bg_wallpaper_aurora)
                }
            }
        }
    }
}
