param([int]$WaitPipelineId = 11460)
$ErrorActionPreference = 'Stop'
$root = 'D:\NEXUS_7\development\nuvio'
$scriptDir = Join-Path $root 'DaNEXUS-FireTV\scripts\danexus'
$pipeline = Get-CimInstance Win32_Process -Filter "ProcessId=$WaitPipelineId"
if ($pipeline -and ($pipeline.Name -ne 'powershell.exe' -or $pipeline.CommandLine -notlike '*complete-delivery.ps1*')) { throw 'Unexpected pipeline' }
if ($pipeline) { Write-Output 'Waiting for final Shield build check'; Wait-Process -Id $WaitPipelineId -ErrorAction SilentlyContinue }
Set-Location (Join-Path $root 'DaNEXUS-FireTV')
python (Join-Path $scriptDir 'package-deliveries.py') --edition Shield
if ($LASTEXITCODE -ne 0) { throw 'Shield verification failed; downloads are not published.' }
Write-Output 'SHIELD DELIVERY VERIFIED'
$listener = Get-NetTCPConnection -LocalPort 8798 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($listener) {
    $server = Get-CimInstance Win32_Process -Filter "ProcessId=$($listener.OwningProcess)"
    if ($server.CommandLine -notlike '*serve-deliveries.py*') { throw 'Port 8798 belongs to another service' }
    Write-Output 'Verified download page updated'
} else { python (Join-Path $scriptDir 'serve-deliveries.py') }
