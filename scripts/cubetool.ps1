<#
.SYNOPSIS
    ZenTV Cube Tool - Remote Manager, Inspector & Debloater for Amazon Fire TV
#>

param (
    [string]$DeviceIp = "192.168.1.29",
    [switch]$Debloat,
    [switch]$Audit,
    [switch]$Install,
    [switch]$Restore
)

# Locate ADB
$adbCandidates = @(
    "adb",
    "C:\Users\geoff\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
)

$adb = $null
foreach ($c in $adbCandidates) {
    if (Get-Command $c -ErrorAction SilentlyContinue) {
        $adb = $c
        break
    } elseif (Test-Path $c) {
        $adb = $c
        break
    }
}

if (-not $adb) {
    Write-Host "Error: Could not locate adb.exe. Please install Android Platform Tools." -ForegroundColor Red
    exit 1
}

function Connect-Device {
    param([string]$ip)
    Write-Host "`nConnecting to $ip`:5555..." -ForegroundColor Cyan
    & $adb connect "$ip`:5555"
    & $adb devices
}

function Show-Analysis {
    param([string]$ip)
    $target = "$ip`:5555"
    & $adb connect $target | Out-Null

    Write-Host "`n========================================================" -ForegroundColor Green
    Write-Host "                FIRE TV SYSTEM AUDIT REPORT             " -ForegroundColor Green
    Write-Host "========================================================" -ForegroundColor Green

    $model = & $adb -s $target shell getprop ro.product.model
    $fireOs = & $adb -s $target shell getprop ro.build.version.name
    $android = & $adb -s $target shell getprop ro.build.version.release

    Write-Host "Device Model:    $model" -ForegroundColor White
    Write-Host "Fire OS Version: $fireOs (Android $android)" -ForegroundColor White

    Write-Host "`n[1] Memory (RAM) Analysis:" -ForegroundColor Yellow
    $meminfo = & $adb -s $target shell dumpsys meminfo
    $meminfo | Select-String "Total RAM:", "Free RAM:", "Used RAM:", "Lost RAM:", "ZRAM:"

    Write-Host "`n[2] Top Memory Consuming Processes:" -ForegroundColor Yellow
    $memLines = $meminfo -split "`n"
    $start = 0
    for ($i = 0; $i -lt $memLines.Count; $i++) {
        if ($memLines[$i] -match "Total PSS by process:") {
            $start = $i
            break
        }
    }
    if ($start -gt 0) {
        for ($i = $start; $i -lt ($start + 20) -and $i -lt $memLines.Count; $i++) {
            Write-Host $memLines[$i]
        }
    }

    Write-Host "`n[3] Package Inventory:" -ForegroundColor Yellow
    $allPkgs = & $adb -s $target shell pm list packages
    $amzPkgs = $allPkgs | Where-Object { $_ -match "amazon" }
    Write-Host "Total Installed Packages: $($allPkgs.Count)"
    Write-Host "Amazon System Packages:   $($amzPkgs.Count)"
}

function Install-ZenTV {
    param([string]$ip)
    $target = "$ip`:5555"
    & $adb connect $target | Out-Null

    $apkPath = Resolve-Path "releases\ZenTV-Launcher-v1.0.0.apk" -ErrorAction SilentlyContinue
    if (-not $apkPath) {
        $apkPath = Resolve-Path "app\build\outputs\apk\release\app-release.apk" -ErrorAction SilentlyContinue
    }

    if (-not $apkPath) {
        Write-Host "APK not found. Building with Gradle..." -ForegroundColor Yellow
        .\gradlew.bat assembleRelease
        $apkPath = Resolve-Path "app\build\outputs\apk\release\app-release.apk"
    }

    Write-Host "`nStreaming $apkPath to $target..." -ForegroundColor Cyan
    & $adb -s $target install -r $apkPath

    Write-Host "Registering ZenTV as Default Home Activity..." -ForegroundColor Cyan
    & $adb -s $target shell cmd package set-home-activity com.gwhitemore.zentv/.MainActivity | Out-Null

    Write-Host "Activating ZenTV Hardware Home Button & Boot Interceptor..." -ForegroundColor Cyan
    & $adb -s $target shell settings put secure enabled_accessibility_services com.gwhitemore.zentv/.ZenAccessibilityService
    & $adb -s $target shell settings put secure accessibility_enabled 1

    Write-Host "Launching ZenTV Launcher on TV screen..." -ForegroundColor Green
    & $adb -s $target shell am start -n com.gwhitemore.zentv/.MainActivity
}

function Debloat-Safe {
    param([string]$ip)
    $target = "$ip`:5555"
    & $adb connect $target | Out-Null

    # Bloatware targets (Amazon Telemetry, Ads, Shopping, Photos sync)
    $bloatList = @(
        @{ Pkg = "com.amazon.firebat";              Desc = "Freevee / Prime Video Ad Streamer (115MB RAM)" },
        @{ Pkg = "com.amazon.venezia";              Desc = "Amazon Appstore Background Services (60MB RAM)" },
        @{ Pkg = "com.amazon.tv.livetv";            Desc = "Amazon Live TV Guide Crawler (35MB RAM)" },
        @{ Pkg = "com.amazon.ftv.screensaver";      Desc = "Amazon Sponsored Screensaver" },
        @{ Pkg = "com.amazon.client.metrics";       Desc = "Amazon Telemetry & Click Tracker" },
        @{ Pkg = "com.amazon.tcomm";                Desc = "Amazon Telemetry Network Daemon" },
        @{ Pkg = "com.amazon.ceviche";              Desc = "Sponsored Ad & Video Carousel Preloader" },
        @{ Pkg = "com.amazon.device.sale.service";  Desc = "Amazon In-App Sales & Shopping Daemon" },
        @{ Pkg = "com.amazon.bueller.photos";       Desc = "Amazon Photos Background Sync" },
        @{ Pkg = "com.amazon.avod.thirdpartyclient";Desc = "Amazon Video Ad Injector" }
    )

    Write-Host "`n========================================================" -ForegroundColor Yellow
    Write-Host "   APPLYING REBOOT-PERSISTENT APPOPS DEBLOAT (FIRE OS 7) " -ForegroundColor Yellow
    Write-Host "========================================================" -ForegroundColor Yellow
    Write-Host "Note: Fire OS 7.6 blocks 'pm uninstall' with DELETE_FAILED_INTERNAL_ERROR." -ForegroundColor DarkGray
    Write-Host "We use persistent AppOps starvation + Network firewalling, which" -ForegroundColor DarkGray
    Write-Host "survives powercycles and freezes these services at the OS level.`n" -ForegroundColor DarkGray

    foreach ($item in $bloatList) {
        $p = $item.Pkg
        Write-Host "Processing $($item.Desc) [$p]..." -ForegroundColor Cyan

        # 1. Attempt user-0 uninstall first (for non-protected packages)
        $uninst = & $adb -s $target shell pm uninstall -k --user 0 $p 2>&1
        if ($uninst -match "Success") {
            Write-Host "  -> Uninstalled for User 0 [SUCCESS]" -ForegroundColor Green
            continue
        }

        # 2. Apply persistent AppOps restrictions (survives powercycles in /data/system/appops.xml)
        & $adb -s $target shell cmd appops set $p RUN_IN_BACKGROUND ignore
        & $adb -s $target shell cmd appops set $p RUN_ANY_IN_BACKGROUND ignore
        & $adb -s $target shell cmd appops set $p WAKE_LOCK ignore
        & $adb -s $target shell cmd appops set $p START_FOREGROUND ignore

        # 3. Restrict battery / standby bucket to 'rare'
        & $adb -s $target shell am set-standby-bucket $p rare 2>$null

        # 4. Blacklist background network usage if UID found
        $pkgDump = & $adb -s $target shell dumpsys package $p
        $uidMatch = $pkgDump | Select-String "userId=(\d+)"
        if ($uidMatch -and $uidMatch.Matches.Groups.Count -gt 1) {
            $uid = $uidMatch.Matches.Groups[1].Value
            & $adb -s $target shell cmd netpolicy add restrict-background-blacklist $uid 2>$null
        }

        # 5. Clear stored cache and database if allowed
        & $adb -s $target shell pm clear $p 2>$null | Out-Null

        Write-Host "  -> Frozen with AppOps (Background: IGNORED, Wakelock: BLOCKED, Standby: RARE) [ACTIVE]" -ForegroundColor Green
    }

    Write-Host "`nDebloat applied successfully! All rules are saved to OS policy and survive powercycles." -ForegroundColor Green
}

function Restore-Debloat {
    param([string]$ip)
    $target = "$ip`:5555"
    & $adb connect $target | Out-Null

    $bloatList = @(
        "com.amazon.client.metrics",
        "com.amazon.tcomm",
        "com.amazon.ceviche",
        "com.amazon.device.sale.service",
        "com.amazon.bueller.photos",
        "com.amazon.avod.thirdpartyclient"
    )

    Write-Host "`nRestoring original packages..." -ForegroundColor Yellow
    foreach ($p in $bloatList) {
        Write-Host "Unfreezing $p..." -NoNewline

        # Re-allow in AppOps
        & $adb -s $target shell cmd appops set $p RUN_IN_BACKGROUND allow
        & $adb -s $target shell cmd appops set $p RUN_ANY_IN_BACKGROUND allow
        & $adb -s $target shell cmd appops set $p WAKE_LOCK allow
        & $adb -s $target shell cmd appops set $p START_FOREGROUND allow
        & $adb -s $target shell am set-standby-bucket $p active 2>$null

        # Re-install if uninstalled
        & $adb -s $target shell cmd package install-existing $p 2>$null

        Write-Host " [RESTORED]" -ForegroundColor Cyan
    }
}

# Non-interactive CLI switches
if ($Debloat) {
    Debloat-Safe -ip $DeviceIp
    exit 0
}
if ($Audit) {
    Show-Analysis -ip $DeviceIp
    exit 0
}
if ($Install) {
    Install-ZenTV -ip $DeviceIp
    exit 0
}
if ($Restore) {
    Restore-Debloat -ip $DeviceIp
    exit 0
}

# Interactive CLI Loop
$currentIp = $DeviceIp

do {
    Write-Host "`n========================================================" -ForegroundColor Cyan
    Write-Host "          ZenTV Remote Cube Tool (Target: $currentIp)   " -ForegroundColor Cyan
    Write-Host "========================================================" -ForegroundColor Cyan
    Write-Host "1. Connect / Reconnect to Fire TV"
    Write-Host "2. Run Live System Audit & RAM Analysis"
    Write-Host "3. Install & Launch ZenTV Launcher"
    Write-Host "4. Safe Debloat (Disable Telemetry, Metrics & Ad Feeds)"
    Write-Host "5. Restore / Undo Debloat"
    Write-Host "6. Launch ZenTV on Screen"
    Write-Host "7. Change Target Fire TV IP"
    Write-Host "8. Exit"
    Write-Host "--------------------------------------------------------"
    $choice = Read-Host "Select an option [1-8]"

    switch ($choice) {
        "1" { Connect-Device -ip $currentIp }
        "2" { Show-Analysis -ip $currentIp }
        "3" { Install-ZenTV -ip $currentIp }
        "4" { Debloat-Safe -ip $currentIp }
        "5" { Restore-Debloat -ip $currentIp }
        "6" {
            & $adb connect "$currentIp`:5555" | Out-Null
            & $adb -s "$currentIp`:5555" shell am start -n com.gwhitemore.zentv/.MainActivity
        }
        "7" {
            $newIp = Read-Host "Enter new Fire TV IP address"
            if ($newIp) { $currentIp = $newIp }
        }
        "8" { break }
        default { Write-Host "Invalid option." -ForegroundColor Red }
    }
} while ($choice -ne "8")
