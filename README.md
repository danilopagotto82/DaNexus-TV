# DaNexus TV

Duas edições com a mesma identidade DaNexus e bases adequadas a cada aparelho.

- **Fire TV**: Nuvio oficial 1.1.0-beta.4, APK armeabi-v7a (32 bits).
- **Shield**: NuvioTV-Fork 1.1.0-beta-nt4, APK arm64-v8a (64 bits), em integração e validação.

[Downloads](https://danilopagotto82.github.io/DaNexus-TV/) · [Releases](https://github.com/danilopagotto82/DaNexus-TV/releases)

Código do Fire na pasta fire. O código Shield será acrescentado após validação.
Cada pasta preserva LICENSE, avisos e créditos de sua base.

## Compilar o Fire

JDK 21, Android SDK 36, NDK e CMake indicados em fire/app/build.gradle.kts.
Configure local.properties e as propriedades de serviços localmente. Essas
configurações e a chave de assinatura não são publicadas.

    cd fire
    .\gradlew.bat :app:assembleFullDebug -PdanexusDeliveryAbi=armeabi-v7a --no-daemon --max-workers=1

Para atualizar a instalação existente, use a mesma chave de assinatura. Uma chave
nova não atualiza o APK já instalado. O APK distribuído usa o pacote já existente
com.danexus.tv.fire.debug.

## Validação

Fire: compilação registrada e 130 testes sem falhas; SHA-256 da entrega reconferido.
A homologação física do Fire depende do teste no aparelho do usuário.
Shield: não há versão nt4 liberada até os testes da integração serem concluídos.

Os serviços e conteúdos configurados pelo usuário não fazem parte deste repositório.
