# ZenTV Launcher

An ultra-clean, minimalist launcher for **Amazon Fire TV**, **Google TV**, and **Android TV** devices.

Built to replace bloated, ad-ridden stock interfaces with pure simplicity, speed, and elegance.

---

## ✨ Features

- **Apple TV / tvOS Frosted Glass Styling:** High-contrast illuminated glass cards with centered momentum inertia gliding inspired by Apple TV and CleanYourTV.
- **Dynamic Ambient Color Glow:** High-performance real-time color extractor samples the focused app's vibrant brand colors (e.g. magenta for BBC iPlayer, red for Netflix, blue for Disney+) and bathes the background in a subtle radial ambient glow.
- **Top Bar & Dynamic Greeting:** Time-of-day greeting (`GOOD MORNING` / `GOOD AFTERNOON` / `GOOD EVENING`), minimalist 50sp thin typography clock, uppercase date, and live device debloat stats badge (`RAM: X MB FREE · ZERO ADS · FIRE OS DEBLOATED`).
- **Frosted Quick-Access Top Pills:**
  - **Inputs:** Fire TV Cube HDMI passthrough, TV tuner, and equipment control.
  - **Wi-Fi:** Displays connected Wi-Fi SSID in real-time with one-click network settings.
  - **Settings:** Instant access to device and display preferences.
- **Dynamic Top Shelf Hero:** Smoothly updates to the active app name (44sp light) and category metadata as you glide across the dock.
- **Bing Daily 4K Wallpaper Engine:**
  - Automatically fetches the world's most stunning daily landscape & nature photography in high resolution from Bing.
  - Offline built-in themes: Aurora Borealis, Deep Space (Pure OLED Black), and Midnight Cyber.
  - Custom wallpaper support: drop any image into `/sdcard/Download/wallpaper.jpg`.
  - Cinematic dark vignetting ensures text, clocks, and cards remain razor-sharp and legible.
- **Hardware Home & Boot Interceptor:**
  - Built-in `ZenAccessibilityService` immediately intercepts stock Amazon launcher transitions with `FLAG_ACTIVITY_NO_ANIMATION` for instantaneous zero-flicker return.
  - Auto-launches on cold boot and wake (`BOOT_COMPLETED` & `QUICKBOOT_POWERON`).
- **35-Package Brick-Proof Debloater:**
  - Automated PowerShell tool (`cubetool.ps1`) to disable telemetry, ACR tracking (`com.amazon.tv.acr`), ad bidding (`com.amazon.kso.blackbird`), Freevee ads (`com.amazon.firebat`), Live Shopping, Kids, and sponsored feeds using non-destructive AppOps & user-space freezing.
- **Pure Zero-Bloat:**
  - **Size:** ~3.8 MB APK.
  - **RAM Footprint:** ~25 MB (compared to Amazon's 1,400+ MB).
  - **100% Free & Open Source:** Zero telemetry, zero tracking, zero in-app purchases, and no Google Play Services dependency.

---

## 📺 Layout Preview

```text
+-------------------------------------------------------------+
|                                                      [ ⚙ ]  |
|                                                             |
|                          12:45                              |
|                   Sunday, November 22                       |
|                                                             |
|                                                             |
|       +-----+    +-----+   +=========+   +-----+            |
|       | App |    | App |   | FOCUS * |   | App |   ...      |
|       |  1  |    |  2  |   | (Plex)  |   |  4  |            |
|       +-----+    +-----+   +=========+   +-----+            |
|                                                             |
+-------------------------------------------------------------+
```

---

## 🚀 Installation on Fire TV Cube

### Method 1: Via Downloader App (Recommended)
1. On your Fire TV Cube, install the **Downloader** app from the Amazon Appstore.
2. In **Settings** > **My Fire TV** > **Developer Options** > **Install Unknown Apps**, toggle **Downloader** to **ON**.
3. Open **Downloader**, enter the URL to the release APK:
   `https://github.com/gwhitemore/zen-tv-launcher/releases/latest/download/ZenTV-Launcher.apk`
4. Click **Download**, then click **Install**.

---

### Method 2: Via ADB (From your PC)
Connect to your Fire TV Cube over Wi-Fi:
```bash
adb connect <FIRE_TV_IP>:5555
adb install releases/ZenTV-Launcher-v1.0.0.apk
```

---

## 🛠️ Making it the Default Launcher on Fire TV

Amazon actively attempts to restrict third-party default launchers on Fire OS. You can set ZenTV as your primary launcher using either:

1. **Accessibility Intercept (Easiest):**
   - Use an accessibility helper like *Button Mapper* or *Launcher Manager* to remap the **Home** button on your remote to launch **ZenTV**.
2. **Launch on Boot:**
   - ZenTV registers `android.permission.RECEIVE_BOOT_COMPLETED` so it can automatically launch as soon as the Cube powers on.

---

## 💻 Building from Source

This project uses modern Android SDK 35 and Gradle 9.3:

```bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```
The compiled APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

---

## 📄 License
MIT License. Free to use, modify, and distribute.
