# DaNexus — Fire TV

Customized Fire TV edition based on Nuvio 1.1.0-beta.4. The visible app name is
**DaNexus**. Package `com.danexus.tv.fire.debug` and the existing signing key are
preserved so installation can update the previous Fire edition without removing
profiles. Shield is maintained separately.

The edition adds configurable source fallback, a complete source queue, a
translucent source decision card, top or side navigation, a clock, portrait
branding, local profile recommendations and a paired phone remote on TV port
8790. The official upstream update check reports versions; future integrations
require merging source, translating new PT-BR resources and building a new
DaNexus APK.

Default source timing: wait 12 seconds, then 6 decision seconds; detect stalled
playback after 20 seconds. Source limit defaults to all eligible distinct
requests. Learning prioritizes alternatives using source performance; disabling
learning preserves the addon's order. The explicitly selected source remains
first. Options are applied to the next playback session.

Recommendations are stored locally and scoped to the active sender or
recipient. They do not synchronize across TVs. The phone remote needs TV and
phone on the same LAN, the app open, and the six digit pairing code. It handles
app navigation, play/pause and next source, not system volume or power.

## Reproduce on the existing Windows project

Use JDK 21, the Android SDK configured locally, and the existing debug signing
key. Do not version local configuration, signing keys or credentials.

```powershell
$env:JAVA_HOME='C:\Users\danil\.jdks\jbr-21.0.11'
$env:PATH=$env:JAVA_HOME+'\bin;'+$env:PATH
python scripts/danexus/prepare-firetv-build.py
.\gradlew.bat :app:testFullDebugUnitTest --tests '*smartsource*' --tests '*SettingsCatalogTest' --tests '*core.danexus*' --tests '*autosync*' --tests '*SubtitleRoutingTest' --tests '*SubtitleCredentialScopeTest' --tests '*DanexusMpvDeadlineTest' --tests '*VersionUtilsTest' --tests '*DanexusSeekPreviewPolicyTest' :app:assembleFullDebug -PdanexusDeliveryAbi=armeabi-v7a --init-script scripts/danexus/firetv-beta4-init.gradle --no-daemon --console=plain --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=768m' '-Pkotlin.compiler.execution.strategy=in-process'
```

The optional init script moves this edition's build output to the sibling
`build-firetv-beta4/app` folder, preserving the old build for rollback. The
preparation script only clears Windows read-only flags on temporary output.
Other environments can use the normal Gradle build.

The recommended Fire TV APK is the `armeabi-v7a` split. Keep the existing app's
signing key when building an update. A fresh machine's default debug key will
not match the installed APK.

## Artwork and license

The portrait master logo is in `assets/brand/danexus-logo-v2.png`. The APK uses
the same logo and avatar in `app/src/main/res/drawable-nodpi`. Launcher themes
use Android drawables so the six background colors retain the same branding.
Addon logos and movie art continue to come from existing metadata.

Upstream Nuvio credits, source notices and the repository LICENSE are retained.
Physical Fire TV playback and HDMI behavior require the user's device test.

## Prévias de cenas Seekr

Ajustes → DaNexus → Prévias de cenas. A pessoa cria uma conta em https://seekr.tv e informa sua chave, validada e criptografada com Android Keystore em noBackupFilesDir. A barra exibe cenas disponíveis durante o avanço/retrocesso. No player, Mais ações → Sincronizar prévias ajusta ±120 segundos por fonte/sessão. A posição da imagem é identificada como aproximada; o avanço mantém a posição escolhida pelo usuário. Não se aplica diferença de duração como ajuste automático.

Usa apenas tv.seekr:seekr-core:0.2.0 (Apache-2.0), com decodificação regional própria para o Fire TV. Cache de uma folha comprimida, limitado a 8 MB; decodificação apenas do recorte solicitado. Não extrai quadros do vídeo nem envia o URL de reprodução. O fluxo atual de login e perfis permanece no DaNexus. O funcionamento ao vivo requer chave válida e título disponível no serviço.

Referências estudadas: https://seekr.tv/docs ; https://github.com/AKhalil609/seekr-android-sdk ; https://github.com/DavidVamaiotu/NuvioTV-Reshaped (SeekrContentMapping / BoundedSeekrTrack). Não foi copiado o fork inteiro.


## Optional Nuvio V2 and approved artwork

Settings → DaNexus offers the Nuvio V2 appearance adapted from
ysosrs123/NuvioTV-Fork 1.1.0-beta-nt4. It preserves the DaNexus navigation
order and uses the fork's static performance material on Fire TV. Choose
cinematic glass or pure liquid dark, transparency and focus. UI scale is 95%
by default and adjustable from 85% to 115%. Names are visible without icons
by default; compact icons and expanding labels are optional. The header
occupies its own layout row so content is placed below it.

The approved TV banner and its five lettering colours are in assets/brand/banners
and drawable-nodpi. The HTML review page accepts replacements and exports
them; changes require applying the images to source and rebuilding the APK.
See NOTICE_DaNexus_V2.md for upstream credits and the Fire TV adaptation.

The Shield edition uses the complete nt4 fork and its native profile entry animation.
That animation is not included in this Fire TV build. Physical Fire TV validation
is pending the user's test. Public app downloads and hosting were authorized
on 2026-10-04. Private project data and credentials remain excluded.
