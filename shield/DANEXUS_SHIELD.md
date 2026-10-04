# DaNexus Shield — nt4

Base: ysosrs123/NuvioTV-Fork, tag 1.1.0-beta-nt4, commit 26e959c97.
Interface DaNexus portada do checkpoint Fire 7faf8969d202d056d0b32e3bec66f2315a346c54.

A integração mantém o player e os recursos nativos nt4, com a marca DaNexus, PT-BR, Fonte Inteligente, espera e troca de fonte, prévias Seekr, controle remoto e recomendações por perfil.

Package: com.danexus.tv.shield.debug. ABI de entrega: arm64-v8a. VersionCode: 1458.
O atualizador usa o repositório danilopagotto82/DaNexus-TV e filtra a edição Shield.

Build local (JDK 21 e Android SDK instalado):

```powershell
python scripts/danexus/prepare-shield-build.py
.\gradlew.bat :app:assembleFullDebug -PdanexusDeliveryAbi=arm64-v8a --max-workers=1
```

Os arquivos local.properties e local.dev.properties são locais e não acompanham os fontes.
Resultados de testes, assinatura e homologação são registrados separadamente por APK e SHA-256. A compilação sozinha não comprova reprodução física.

Licenças e créditos NuvioMedia/NuvioTV e ysosrs123/NuvioTV-Fork permanecem nos respectivos arquivos.
