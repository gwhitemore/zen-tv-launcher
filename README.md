# ZenTV Launcher

An ultra-clean, minimalist launcher for **Amazon Fire TV**, **Google TV**, and **Android TV** devices.

Built to replace bloated, ad-ridden stock interfaces with pure simplicity, speed, and elegance.

---

## ✨ Features

- **Large Center Clock & Date:** Floating minimalist digital clock and date centered on screen. Supports both 12-hour (`h:mm`) and 24-hour (`HH:mm`) formats.
- **Minimalist App Ribbon (Option A):** A sleek, horizontal scrolling row of your installed apps across the lower third of the screen.
- **Both Official & Sideloaded Apps:** Automatically scans standard Android TV apps *and* sideloaded mobile APKs (e.g. SmartTube, Kodi, Downloader) so nothing is hidden.
- **Fire TV Remote D-Pad Navigation:**
  - Silky smooth focus animations (subtle 1.14x scaling + glowing cyan border + title marquee).
  - Press **Select** to launch.
  - Press **Menu** (or long-press Select) to pin apps to the front, hide unwanted apps, or inspect app details.
- **Wallpaper Engine:**
  - Built-in wallpapers: Aurora Borealis (Dark Blue), Deep Space (Pure OLED Black), and Midnight Cyber (Moody Purple).
  - Custom wallpaper support: drop any image into `/sdcard/Download/wallpaper.jpg`.
  - Ambient vignette scrim ensuring text and icons are 100% legible on any background.
- **Pure Zero-Bloat:**
  - **Size:** ~3.3 MB APK.
  - **RAM Footprint:** ~20 MB (compared to Amazon's 1,400 MB).
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
