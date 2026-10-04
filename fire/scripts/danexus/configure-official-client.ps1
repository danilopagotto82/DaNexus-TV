$ErrorActionPreference = 'Stop'
$discovery = Invoke-RestMethod 'https://api.nuvio.tv/.well-known/nuvio' -TimeoutSec 15
if ($discovery.backend_url -ne 'https://api.nuvio.tv') { throw 'Backend oficial inesperado.' }
$key = [string]$discovery.publishable_key
$public = $key -like 'sb_publishable_*'
if ($key -like 'eyJ*') {
    $encoded = $key.Split('.')[1].Replace('-', '+').Replace('_', '/')
    $encoded = $encoded.PadRight($encoded.Length + ((4 - $encoded.Length % 4) % 4), '=')
    $payload = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($encoded)) | ConvertFrom-Json
    $public = $payload.role -eq 'anon'
}
if (-not $public) { throw 'A configuração não contém uma chave pública anon/publishable.' }
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$configuration = Join-Path $root 'local.dev.properties'
if (Test-Path $configuration) { throw 'Já existe local.dev.properties; preserve e revise o arquivo antes de substituir.' }
$lines = @((('NUVIO_SUPABASE_URL=' + $discovery.backend_url)), (('NUVIO_SUPABASE_ANON_KEY=' + $key)), 'NUVIO_SUPABASE_FALLBACK_URL=', (('AVATAR_PUBLIC_BASE_URL=' + $discovery.backend_url + '/storage/v1/object/public/avatars')))
[IO.File]::WriteAllText($configuration, ($lines -join "`n")+"`n", [Text.UTF8Encoding]::new($false))
Write-Output 'Configuração pública do cliente preparada; nenhuma credencial pessoal incluída.'
