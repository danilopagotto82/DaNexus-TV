$status = 'D:\NEXUS_7\development\nuvio\entrega\FASE12\BUILD_STATUS.json'
$deadline = (Get-Date).AddHours(3)
Write-Output 'Waiting for the current FireTV + Shield build queue to finish.'
do {
    $rows = @()
    if (Test-Path $status) { try { $rows = @(Get-Content $status -Raw | ConvertFrom-Json) } catch {} }
    if (@($rows | Where-Object {$_.edition -eq 'Shield'}).Count -gt 0) { break }
    if ((Get-Date) -gt $deadline) { throw 'Build queue wait expired; no final build started.' }
    Start-Sleep -Seconds 20
} while ($true)
Write-Output 'Starting final incremental FireTV check to include all resource updates.'
& 'D:\NEXUS_7\development\nuvio\DaNEXUS-FireTV\scripts\danexus\build-editions.ps1' -Editions @('FireTV') -LogSuffix 'final'
