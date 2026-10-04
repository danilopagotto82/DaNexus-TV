param([string[]]$Editions = @('FireTV','Shield'), [string]$LogSuffix = 'serial', [int]$MaxWorkers = 2)
$ErrorActionPreference = 'Continue'
$env:JAVA_HOME = 'C:\Users\danil\.jdks\jbr-21.0.11'
$env:PATH = $env:JAVA_HOME + '\bin;' + $env:PATH
$root = 'D:\NEXUS_7\development\nuvio'
$deliver = Join-Path $root 'entrega\FASE12'
New-Item $deliver -ItemType Directory -Force | Out-Null
$results = @()
$statusPath = Join-Path $deliver 'BUILD_STATUS.json'
if (Test-Path $statusPath) { $existing = Get-Content $statusPath -Raw | ConvertFrom-Json; $results = @(foreach ($entry in $existing) { if ($entry.edition) { $entry } }) }
foreach ($edition in $Editions) {
    if ($edition -notin @('FireTV','Shield')) { throw 'Unknown edition' }
    $dir = Join-Path $root ('DaNEXUS-' + $edition)
    Set-Location $dir
    # Windows read-only directory attributes prevent Java deleting generated caches.
    foreach ($generated in @('app\build', 'build', '.gradle')) {
        if (Test-Path $generated) {
            & attrib.exe -R $generated
            & attrib.exe -R (Join-Path $generated '*') /S /D
        }
    }
    $log = Join-Path $dir ($edition.ToLower() + '-' + $LogSuffix + '-build.log')
    $commit = git rev-parse HEAD
    Write-Output ("START " + $edition + " " + (Get-Date -Format o))
    & .\gradlew.bat :app:testFullDebugUnitTest --tests '*SmartSource*' --tests '*SourceReputation*' :app:assembleFullDebug --no-daemon --console=plain "--max-workers=$MaxWorkers" '-Dorg.gradle.jvmargs=-Xmx3072m -XX:MaxMetaspaceSize=768m' '-Pkotlin.compiler.execution.strategy=in-process' *> $log
    $exit = $LASTEXITCODE
    $row = @{edition=$edition;exitCode=$exit;log=$log;commit=$commit;finished=(Get-Date -Format o)}
    if ($exit -eq 0) {
        $apks = Get-ChildItem 'app\build\outputs\apk\full\debug' -Filter '*.apk'
        if (!$apks) { throw 'Gradle succeeded but APK output is absent' }
        foreach ($apk in $apks) { Copy-Item $apk.FullName (Join-Path $deliver ('DaNEXUS-' + $edition + '-' + $apk.Name)) }
        $row.apkCount=$apks.Count
        $suites = @(Get-ChildItem 'app\build\test-results\testFullDebugUnitTest' -Filter 'TEST-*.xml' | ForEach-Object {
            [xml]$xml = Get-Content $_.FullName
            @{name=$xml.testsuite.name;tests=[int]$xml.testsuite.tests;failures=[int]$xml.testsuite.failures;errors=[int]$xml.testsuite.errors}
        })
        $row.testSuites=$suites
    }
    $results = @($results | Where-Object {$_.edition -ne $edition}) + @($row)
    $results | ConvertTo-Json -Depth 7 | Set-Content $statusPath -Encoding UTF8
    Get-ChildItem $deliver -Filter '*.apk' | Sort-Object Name | ForEach-Object {
        $hash = Get-FileHash $_.FullName -Algorithm SHA256
        $hash.Hash + '  ' + $_.Name
    } | Set-Content (Join-Path $deliver 'SHA256SUMS.txt') -Encoding ASCII
    Write-Output ("END " + $edition + " exit=" + $exit)
}
Write-Output 'SERIAL BUILD COMPLETE'
