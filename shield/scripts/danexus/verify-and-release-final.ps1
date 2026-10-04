param([int]$FirePipelineId = 14892, [switch]$SkipShieldBuild)
$ErrorActionPreference = 'Stop'
$root = 'D:\NEXUS_7\development\nuvio'
$scriptDir = Join-Path $root 'DaNEXUS-FireTV\scripts\danexus'
$delivery = Join-Path $root 'entrega\FASE12'
$checkpoint = Join-Path $delivery 'checkpoints\shield-package-recovery'
New-Item $checkpoint -ItemType Directory -Force | Out-Null
if (!$SkipShieldBuild) {
Copy-Item (Join-Path $root 'DaNEXUS-Shield\shield-final-build.log') (Join-Path $checkpoint 'first-full-build.log')
foreach ($name in @('shield-package-stacktrace.log','shield-package-only-stacktrace.log')) {
    $file = Join-Path $root ('DaNEXUS-Shield\' + $name)
    if (Test-Path $file) { Move-Item $file (Join-Path $checkpoint $name) -Force }
}
$oldFire = Join-Path $delivery 'checkpoints\before-five-second-prompt\apks'
New-Item $oldFire -ItemType Directory -Force | Out-Null
Get-ChildItem $delivery -Filter 'DaNEXUS-FireTV-*.apk' | Copy-Item -Destination $oldFire
& (Join-Path $scriptDir 'build-editions.ps1') -Editions @('Shield') -LogSuffix 'final' -MaxWorkers 1
}
$parsed = Get-Content (Join-Path $delivery 'BUILD_STATUS.json') -Raw | ConvertFrom-Json
$rows = @(foreach ($row in $parsed) { if ($row.edition) { $row } })
$shield = $rows | Where-Object edition -eq 'Shield' | Select-Object -First 1
if ($shield.exitCode -ne 0) { throw 'Full Shield verification failed' }
$shield | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $checkpoint 'Shield_SUCCESS.json') -Encoding UTF8
Set-Location (Join-Path $root 'DaNEXUS-FireTV')
python (Join-Path $scriptDir 'package-deliveries.py') --edition Shield
if ($LASTEXITCODE -ne 0) { throw 'Shield APK verification failed' }
$listener = Get-NetTCPConnection -LocalPort 8798 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if (!$listener) {
    Start-Process python -ArgumentList @((Join-Path $scriptDir 'serve-deliveries.py')) -NoNewWindow -RedirectStandardOutput (Join-Path $delivery 'delivery-server.log') -RedirectStandardError (Join-Path $delivery 'delivery-server-errors.log')
} else {
    $server = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    if ($server.CommandLine -notlike '*serve-deliveries.py*') { throw 'Port is owned by another service' }
}
Write-Output 'SHIELD READY: http://192.168.0.7:8798/'
$fireProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$FirePipelineId"
if ($fireProcess) {
    if ($fireProcess.Name -ne 'powershell.exe' -or $fireProcess.CommandLine -notlike '*resume-fire-delivery.ps1*') { throw 'Unexpected Fire pipeline' }
    Wait-Process -Id $FirePipelineId -ErrorAction SilentlyContinue
}
$parsed = Get-Content (Join-Path $delivery 'BUILD_STATUS.json') -Raw | ConvertFrom-Json
$rows = @(foreach ($row in $parsed) { if ($row.edition) { $row } })
$rows = @($rows | Where-Object {$_.edition -and $_.edition -ne 'Shield'}) + @($shield)
ConvertTo-Json -InputObject $rows -Depth 8 | Set-Content (Join-Path $delivery 'BUILD_STATUS.json') -Encoding UTF8
python (Join-Path $scriptDir 'package-deliveries.py')
if ($LASTEXITCODE -ne 0) { throw 'Final APK verification failed' }
Write-Output 'ALL FILES VERIFIED AND RELEASED: http://192.168.0.7:8798/'
