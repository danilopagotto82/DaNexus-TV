$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$generator = Get-Command ffmpeg -ErrorAction Stop
& $generator.Source -hide_banner -loglevel error -y -f lavfi -i 'testsrc2=size=640x360:rate=24' -f lavfi -i 'sine=frequency=440:sample_rate=48000' -t 30 -c:v libx264 -preset ultrafast -crf 28 -pix_fmt yuv420p -c:a aac -b:a 64k -movflags +faststart good.mp4
if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar MP4 de prova.' }
& $generator.Source -hide_banner -loglevel error -y -f lavfi -i 'testsrc2=size=640x360:rate=24' -f lavfi -i 'sine=frequency=440:sample_rate=48000' -t 3 -c:v libx264 -preset ultrafast -crf 28 -pix_fmt yuv420p -c:a aac -b:a 64k -f mpegts stall.ts
if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar TS de prova.' }
Write-Output 'Vídeos controlados da FASE 06 preparados.'
