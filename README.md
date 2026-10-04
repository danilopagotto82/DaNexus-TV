# DaNexus TV

Duas ediÃ§Ãµes com a mesma identidade DaNexus e bases adequadas a cada aparelho.

- **Fire TV**: Nuvio oficial 1.1.0-beta.4, APK armeabi-v7a (32 bits).
- **Shield**: NuvioTV-Fork 1.1.0-beta-nt4, APK arm64-v8a (64 bits), em integraÃ§Ã£o e validaÃ§Ã£o.

[Downloads](https://danilopagotto82.github.io/DaNexus-TV/) Â· [Releases](https://github.com/danilopagotto82/DaNexus-TV/releases)

CÃ³digo do Fire na pasta fire. O cÃ³digo Shield serÃ¡ acrescentado apÃ³s validaÃ§Ã£o.
Cada pasta preserva LICENSE, avisos e crÃ©ditos de sua base.

## Compilar o Fire

JDK 21, Android SDK 36, NDK e CMake indicados em fire/app/build.gradle.kts.
Configure local.properties e as propriedades de serviÃ§os localmente. Essas
configuraÃ§Ãµes e a chave de assinatura nÃ£o sÃ£o publicadas.

    cd fire
    .\gradlew.bat :app:assembleFullDebug -PdanexusDeliveryAbi=armeabi-v7a --no-daemon --max-workers=1

Para atualizar a instalaÃ§Ã£o existente, use a mesma chave de assinatura. Uma chave
nova nÃ£o atualiza o APK jÃ¡ instalado. O APK distribuÃ­do usa o pacote jÃ¡ existente
com.danexus.tv.fire.debug.

## ValidaÃ§Ã£o

Fire: compilaÃ§Ã£o registrada e 130 testes sem falhas; SHA-256 da entrega reconferido.
A homologaÃ§Ã£o fÃ­sica do Fire depende do teste no aparelho do usuÃ¡rio.
Shield: nÃ£o hÃ¡ versÃ£o nt4 liberada atÃ© os testes da integraÃ§Ã£o serem concluÃ­dos.

Os serviÃ§os e conteÃºdos configurados pelo usuÃ¡rio nÃ£o fazem parte deste repositÃ³rio.
