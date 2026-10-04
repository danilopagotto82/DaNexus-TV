$ErrorActionPreference = 'Stop'
$root = 'D:\NEXUS_7\development\nuvio'
$scriptDir = Join-Path $root 'DaNEXUS-FireTV\scripts\danexus'
& (Join-Path $scriptDir 'build-editions.ps1') -Editions @('FireTV','Shield') -LogSuffix 'final'
Set-Location (Join-Path $root 'DaNEXUS-FireTV')
python (Join-Path $scriptDir 'package-deliveries.py')
if ($LASTEXITCODE -ne 0) { throw 'Delivery verification failed; downloads are not published.' }
Write-Output 'DELIVERY VERIFIED'
$listener = Get-NetTCPConnection -LocalPort 8798 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listener) {
    $server = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    if ($server.CommandLine -notlike '*serve-deliveries.py*') { throw 'Port 8798 belongs to another service' }
    Write-Output 'Verified download page updated'
} else { python (Join-Path $scriptDir 'serve-deliveries.py') }
