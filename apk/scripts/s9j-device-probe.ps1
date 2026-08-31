$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

function Get-UiXml {
  & $adb shell uiautomator dump /sdcard/ui.xml | Out-Null
  return (& $adb shell cat /sdcard/ui.xml)
}

function Click-ByText([string]$label) {
  $xml = Get-UiXml
  $pattern = 'text="' + [regex]::Escape($label) + '"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"'
  $m = [regex]::Match($xml, $pattern)
  if (-not $m.Success) {
    Write-Host ("missing: " + $label)
    return $false
  }
  $x = [int](([int]$m.Groups[1].Value + [int]$m.Groups[3].Value) / 2)
  $y = [int](([int]$m.Groups[2].Value + [int]$m.Groups[4].Value) / 2)
  Write-Host ("tap " + $label + " @ " + $x + "," + $y)
  & $adb shell input tap $x $y
  return $true
}

Click-ByText 'Skip' | Out-Null
Start-Sleep -Seconds 2
Click-ByText 'Skip' | Out-Null
Start-Sleep -Seconds 2
Click-ByText 'Continue' | Out-Null
Start-Sleep -Seconds 1
Click-ByText 'Get started' | Out-Null
Start-Sleep -Seconds 2
Click-ByText 'Continue with Google' | Out-Null
Start-Sleep -Seconds 2

$xml = Get-UiXml
$texts = [regex]::Matches($xml, 'text="([^"]{1,60})"') | ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique -First 40
Write-Host '=== visible texts ==='
$texts

& $adb logcat -c
& $adb shell am force-stop com.example.momentra
Start-Sleep -Seconds 1
Write-Host '=== cold-ish start -W ==='
& $adb shell am start -W -n com.example.momentra/.MainActivity
Start-Sleep -Seconds 4
Write-Host '=== MomentraPerf ==='
& $adb logcat -d -s MomentraPerf:I

& $adb shell input keyevent KEYCODE_HOME
Start-Sleep -Seconds 2
& $adb logcat -c
& $adb shell monkey -p com.example.momentra -c android.intent.category.LAUNCHER 1
Start-Sleep -Seconds 3
Write-Host '=== MomentraPerf resume ==='
& $adb logcat -d -s MomentraPerf:I

& $adb exec-out screencap -p > g:\momentra_v2\docs\implementation\S9_J_ANDROID_SCREEN2.png
Write-Host 'screenshot2 saved'
