param (
    [string]$DeviceIp = "192.168.1.29"
)

$adb = "C:\Users\geoff\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    $adb = "adb"
}

$target = "$DeviceIp`:5555"
Write-Host "Connecting to $target..." -ForegroundColor Cyan
& $adb connect $target

Write-Host "`n=== DEVICE INFORMATION ===" -ForegroundColor Green
$model = & $adb -s $target shell getprop ro.product.model
$androidVer = & $adb -s $target shell getprop ro.build.version.release
$fireOsVer = & $adb -s $target shell getprop ro.build.version.name
$buildId = & $adb -s $target shell getprop ro.build.display.id

Write-Host "Model:           $model"
Write-Host "Fire OS Version: $fireOsVer (Android $androidVer)"
Write-Host "Build ID:        $buildId"

Write-Host "`n=== RAM USAGE SUMMARY ===" -ForegroundColor Green
$meminfo = & $adb -s $target shell dumpsys meminfo
$meminfo | Select-String "Total RAM:", "Free RAM:", "Used RAM:", "Lost RAM:", "ZRAM:"

Write-Host "`n=== TOP 20 MEMORY CONSUMING PROCESSES ===" -ForegroundColor Green
$memLines = $meminfo -split "`n"
$startIndex = 0
$endIndex = 0
for ($i = 0; $i -lt $memLines.Count; $i++) {
    if ($memLines[$i] -match "Total PSS by process:") {
        $startIndex = $i
        break
    }
}
if ($startIndex -gt 0) {
    for ($i = $startIndex; $i -lt ($startIndex + 25) -and $i -lt $memLines.Count; $i++) {
        Write-Host $memLines[$i]
    }
}

Write-Host "`n=== RUNNING PACKAGES COUNT ===" -ForegroundColor Green
$allPackages = & $adb -s $target shell pm list packages
$amazonPackages = $allPackages | Where-Object { $_ -match "amazon" }
Write-Host "Total Installed Packages:  $($allPackages.Count)"
Write-Host "Amazon System Packages:    $($amazonPackages.Count)"
