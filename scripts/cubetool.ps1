<#
.SYNOPSIS
    ZenTV Cube Tool - Remote Manager, Inspector & Debloater for Amazon Fire TV
#>

param (
    [string]$DeviceIp = "192.168.1.29"
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
    Write-Host "Launching ZenTV Launcher on TV screen..." -ForegroundColor Green
    & $adb -s $target shell am start -n com.gwhitemore.zentv/.MainActivity
}

function Debloat-Safe {
    param([string]$ip)
    $target = "$ip`:5555"
    & $adb connect $target | Out-Null

    $bloatList = @(
        "com.amazon.client.metrics",          # Telemetry & click tracking
        "com.amazon.tcomm",                   # Telemetry communication sink
        "com.amazon.ceviche",                 # Sponsored carousel recommendation engine
        "com.amazon.device.sale.service",     # Amazon Shopping / in-app purchase daemons
        "com.amazon.clouddrive.photos"        # Amazon Photos background sync
    )

    Write-Host "`nSafely debloating non-essential Amazon tracking & ad services..." -ForegroundColor Yellow
    foreach ($pkg in $bloatList) {
        Write-Host "Disabling $pkg..." -NoNewline
        $res = & $adb -s $target shell pm uninstall -k --user 0 $pkg 2>&1
        Write-Host " [$res]" -ForegroundColor Cyan
    }
    Write-Host "`nDone! Background RAM has been reclaimed. (Re-run audit to verify)" -ForegroundColor Green
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
        "com.amazon.clouddrive.photos"
    )

    Write-Host "`nRestoring original packages..." -ForegroundColor Yellow
    foreach ($pkg in $bloatList) {
        Write-Host "Restoring $pkg..." -NoNewline
        $res = & $adb -s $target shell cmd package install-existing $pkg 2>&1
        Write-Host " [$res]" -ForegroundColor Cyan
    }
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
