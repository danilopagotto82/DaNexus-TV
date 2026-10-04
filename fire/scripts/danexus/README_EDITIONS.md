# DaNexus — FASE 12

## Edições
- FireTV: NuvioMedia/NuvioTV 1.1.0-beta.3 (9bf4ed1e3), com Fonte Inteligente, marca DaNexus, PT-BR completo, QR e barra superior leve.
- Shield: Cxsmo-ai/NuvioTV-Custom (3e0d0fad6), derivado de ysosrs123/NuvioTV-Fork, com reprodução REMUX, navegação superior, Fonte Inteligente, PT-BR completo e QR.
- A linha anterior DaNexus-TV/Reshaped e o APK da FASE 11 estão preservados.

## Aparelhos informados pelo proprietário
Sala: NVIDIA Shield Pro 2017; Denon AVR-S510BT; TCL 65C715; Blu-ray LG BP550.
Quarto: Fire TV Stick 4K Max de segunda geração; Samsung UN55MU6300; soundbar Samsung HW-Q600F.

A ligação HDMI ainda não foi observada. O aplicativo usa as capacidades reais de áudio/tela informadas pelo Android. Não presume Atmos, DTS:X ou Dolby Vision pela marca da TV. O inventário não modifica o firmware da TV, receiver, soundbar ou Blu-ray.

## Instalação e atualizações
Pacotes separados: com.danexus.tv.fire.debug e com.danexus.tv.shield.debug.
As compilações atuais são de teste, assinadas com a chave debug local.
Cada atualizador aceita apenas APKs com o prefixo de sua edição.
FireTV: preferir armeabi-v7a. Shield Pro: preferir arm64-v8a.
Não instalar um APK de outra arquitetura.

## Validação
- [ ] Compilação final FireTV.
- [ ] Testes focados FireTV.
- [ ] Compilação final Shield.
- [ ] Testes focados Shield.
- [x] Auditoria de cobertura PT-BR nas duas edições.
- [x] Auditoria de variáveis de formatação PT-BR nas duas edições.
- [ ] Conferir assinatura e pacote dos APKs.
- [ ] Testar navegação com controle remoto.
- [ ] Testar áudio PT-BR e canais reais.
- [ ] Testar HDR com a ligação HDMI real.
- [ ] Testar fallback e pausa pelo painel móvel.
- [ ] Publicar código-fonte GPL-3.0 e APKs no GitHub quando o repositório estiver autorizado.

## Fontes
https://github.com/NuvioMedia/NuvioTV/releases/tag/1.1.0-beta.3
https://github.com/Cxsmo-ai/NuvioTV-Custom
https://github.com/ysosrs123/NuvioTV-Fork
https://developer.amazon.com/docs/fire-tv/device-specifications-fire-tv-streaming-media-player.html
https://manuals.denon.com/AVRS510BT/NA/EN/GFNFSYdtphpnek.php
https://www.samsung.com/br/audio-devices/soundbar/q-series-soundbar-subwoofer-black-hw-q600f-zd/

## Compilação econômica
Java 21; no máximo dois workers; um aparelho por vez; JVM de 3 GB; compilador Kotlin dentro do mesmo processo.
Logs e BUILD_STATUS.json registram os resultados reais. O empacotamento ocorre somente após Gradle retornar sucesso.
