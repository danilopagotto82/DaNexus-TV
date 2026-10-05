# Revisão 13.1 — DaNexus clássico

A tela de Recomendações recebeu uma revisão completa de aparência, preservando a navegação clássica.

- Preto, grafite e azul-marinho muito escuro com transparências discretas.
- Capas maiores; imagem de fundo quando disponível; título e remetente/destinatário com hierarquia clara.
- Foco com contorno claro e escala discreta; seleção de perfis com check.
- Um painel para escolher um ou vários perfis, marcar todos, confirmar ou cancelar o envio.
- Remover abre confirmação no mesmo estilo; Cancelar recebe o foco inicial.
- A linguagem também está nos diálogos, barra superior, painéis de configurações/Sobre, fallback e overlays.
- Recomendações antigas continuam disponíveis; novas recomendações também preservam a imagem de fundo.

| Edição | Versão | Testes essenciais | Falhas |
| --- | --- | ---: | ---: |
| Fire TV | 1.1.0-beta.4-danexus.4 | 71 | 0 |
| Shield | 1.1.0-beta-nt4-danexus.2 | 119 | 0 |

As assinaturas, os pacotes e as arquiteturas dos dois APKs foram conferidos.
O teste de recomendações no candidato Shield foi concluído: perfis e histórico preservados na atualização, abas Recebidas e Enviadas, foco de controle remoto nos cards e nas ações, capas e imagem de fundo visíveis quando carregadas, seleção múltipla com check e desmarcação, envio confirmado para dois destinatários, marcar todos sem envio automático, cancelamento do seletor pelo Voltar, envio confirmado para cinco destinatários, filtro por destinatário, Cancelamento da remoção com Cancelar em foco inicial, remoção dos cinco registros temporários e recuperação do foco, preservando a recomendação anterior. Depois, quatro textos foram encurtados e conferidos nos APKs finais. Com autorização do Dan, o APK final foi instalado na Shield e conferido novamente: APK final instalado e SHA-256 instalado conferido, seis perfis anteriores preservados, histórico visível ao retornar à tela inicial, recomendação anterior preservada ao reabrir o app, foco visível no card, texto final da confirmação de remoção, Cancelar em foco inicial e cancelamento sem apagar a recomendação.
Os textos encurtados do seletor foram conferidos nos APKs; a conferência visual final deles no aparelho fica para Dan.
Teste breve de reprodução na Shield final: Pausa, retomada, foco nos controles, avanço, retrocesso, seleção manual de fonte, tentativa de outra fonte e retomada do vídeo no player interno; saída para detalhes e início. Isso não substitui a homologação de áudio, HDR, Dolby Vision e HDMI.
No Fire TV, a verificação direta no aparelho está pendente. A aprovação visual final pelo Dan e a homologação completa de reprodução permanecem na FASE 12.

[APK Fire TV](https://github.com/danilopagotto82/DaNexus-TV/releases/download/v1.1.0-beta.4-danexus.4/DaNexus-FireTV-1.1.0-beta.4-danexus.4-armeabi-v7a.apk) · [APK Shield](https://github.com/danilopagotto82/DaNexus-TV/releases/download/v1.1.0-beta-nt4-danexus.2/DaNexus-Shield-1.1.0-beta-nt4-danexus.2-arm64-v8a.apk) · [Checklist no celular](https://danilopagotto82.github.io/DaNexus-TV/)
