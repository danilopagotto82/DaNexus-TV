# DaNexus — FASE 06 — NuvioTV de teste

Fonte oficial: https://github.com/NuvioMedia/NuvioTV
Base: e374881a78e546bbda45f52f3dccd73fbbe47cb6 (1.1.0-beta.3).
Branch local: danexus/phase06-smart-source-resume.

O fork usa `com.nuvio.tv.danexus` e assinatura de desenvolvimento, para instalação separada.
Ele preserva o player original fora de sessões DaNexus habilitadas. A conexão oficial usa somente a configuração pública do cliente publicada em `https://api.nuvio.tv/.well-known/nuvio`, em `local.dev.properties` ignorado pelo Git.

## O que observar na TV

1. Instale `development/nuvio/entrega/NUVIO_DaNexus_FASE06.apk` e abra **Nuvio DaNexus Teste**.
2. Adicione `http://192.168.0.7:8797/manifest.json` como addon (PC e TV na mesma rede).
3. Em **FASE 06 · testes controlados**, abra **01 · Fonte 404 → vídeo válido** e escolha a primeira opção uma vez. O vídeo colorido deve iniciar após a troca automática.
4. Repita **02 · Sem primeiro frame → vídeo válido**. A espera deve terminar com o vídeo válido.
5. Em **03 · Travamento → vídeo válido**, a primeira fonte deve parar de avançar, e a alternativa deve iniciar. Pause e saia: nenhuma nova tentativa deve continuar após a saída.
6. Em **04 · Todas ruins → fim da espera**, deve aparecer a mensagem de indisponibilidade, encerrando a busca.

A compilação e a simulação da política não comprovam reprodução física. A FASE 06 permanece aberta até essa prova.

## Operador

- Gerar os vídeos de prova: `powershell -ExecutionPolicy Bypass -File scripts/danexus/prepare-smoke.ps1`.
- Iniciar o servidor: `node scripts/danexus/smoke-server.mjs 192.168.0.7 8797`.
- Diagnóstico controlado: `http://192.168.0.7:8797/events`. Pedidos HTTP não comprovam primeiro frame; procure também os eventos de playback.
- Configuração: `app/src/main/assets/danexus-smart-source.json`; um arquivo com esse nome no diretório privado do aplicativo tem precedência.
- Apenas addons explicitamente permitidos pela configuração de cada perfil entram no fallback. Infantil usa limites locais mais curtos.
- Journal privado: `files/danexus-smart-source-feedback.json`, limitado a 200 eventos sanitizados.
- A API do DaNexus no PC aceita somente loopback. O fork não abre essa API na rede. O smoke tem uma ponte isolada; os perfis reais guardam eventos no journal até configurar uma conexão válida ao backend.
- Com ADB disponível, pode-se usar `adb reverse tcp:8787 tcp:8787` e uma configuração privada apontando para `http://127.0.0.1:8787`, com o ID correto do perfil. O PC precisa estar executando DaNexus nessa porta.
- Para ler o journal do APK de teste: `adb exec-out run-as com.nuvio.tv.danexus cat files/danexus-smart-source-feedback.json`.

## Reprodução da validação

`gradlew.bat :app:testFullDebugUnitTest --tests '*SmartSource*' -Pdebuggable=true --max-workers=2`

`gradlew.bat :app:testFullDebugUnitTest -I scripts/danexus/gradle-test-observer.gradle -Pdebuggable=true --max-workers=2`

`gradlew.bat :app:assembleFullDebug -Pdebuggable=true --max-workers=2`

`node --test scripts/danexus/smoke-server.test.mjs`

A suíte completa tem limite de quatro minutos na tarefa de testes para evitar repetir uma espera interminável. Falhas anteriores do upstream devem ser registradas separadamente.
