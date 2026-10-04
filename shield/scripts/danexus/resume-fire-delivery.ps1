param([int]$WaitPipelineId = 0)
$ErrorActionPreference = 'Stop'
$root = 'D:\NEXUS_7\development\nuvio'
$scriptDir = Join-Path $root 'DaNEXUS-FireTV\scripts\danexus'
if ($WaitPipelineId -gt 0) {
    $pipeline = Get-CimInstance Win32_Process -Filter "ProcessId=$WaitPipelineId"
    if ($pipeline -and ($pipeline.Name -ne 'powershell.exe' -or $pipeline.CommandLine -notlike '*complete-delivery.ps1*')) { throw 'Unexpected process; refusing to wait on unrelated work' }
    if ($pipeline) { Write-Output 'Waiting for current Shield build'; Wait-Process -Id $WaitPipelineId -ErrorAction SilentlyContinue }
}
$cachePaths = @('incremental\fullDebug\mergeFullDebugResources', 'merged_res_blame_folder\fullDebug\mergeFullDebugResources')
foreach ($relative in $cachePaths) {
    $cache = Join-Path $root ('DaNEXUS-FireTV\app\build\intermediates\' + $relative)
    if (Test-Path $cache) { Remove-Item $cache -Recurse -Force }
}
& (Join-Path $scriptDir 'build-editions.ps1') -Editions @('FireTV') -LogSuffix 'final'
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
