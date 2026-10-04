# DaNexus — diferenças em relação ao Nuvio

Criamos duas edições independentes para seus aparelhos.

| Edição | Base | Diferença principal |
| --- | --- | --- |
| DaNexus Fire TV | Nuvio oficial 1.1.0-beta.3 | Mantém a base oficial para o Fire TV Stick 4K Max de 2ª geração, com barra superior de navegação e as integrações DaNexus. APK recomendado: armeabi-v7a. |
| DaNexus Shield | Cxsmo NuvioTV-Custom, derivado do fork Yso | Acrescenta as integrações DaNexus a uma base voltada a REMUX de alta taxa de dados, com navegação superior ativada por padrão. APK recomendado: arm64-v8a. |

## O que acrescentamos nas duas

- **Fonte Inteligente:** acompanha o início e o avanço da reprodução; tenta outra fonte quando a atual falha ou trava, nos addons DaNexus habilitados.
- **Aviso lateral após 5 segundos:** se o vídeo ainda não começou, mostra Mudar fonte, Continuar aguardando e uma contagem até a troca automática. Voltar fecha o aviso; a contagem respeita pausa e navegação nos menus. O aviso aparece uma vez por tentativa e some assim que a reprodução começa.
- **Tentativas com limite:** evita repetir indefinidamente a mesma fonte; respeita pausa e saída do player. Em erro de codec, pode tentar o outro motor disponível.
- **Histórico local de qualidade:** usa resultados anteriores para ordenar alternativas e registra eventos sanitizados, sem guardar URLs de reprodução no histórico.
- **Integração com o painel DaNexus:** status de reprodução e comandos de pausa/continuar ou próxima fonte, quando o PC e a conexão com o backend estão disponíveis.
- **QR do painel:** acesso pelo celular na tela Sobre.
- **Português brasileiro:** completamos os textos que faltavam e conferimos as variáveis de formatação nas duas edições.
- **Identidade DaNexus:** nome, logos e banners próprios; pacotes separados permitem manter o Nuvio e os APKs anteriores instalados.
- **Atualizações por edição:** o atualizador filtra o prefixo do APK para não misturar Fire TV e Shield. Depende da publicação dos arquivos no repositório.

## O que a base Shield traz a mais

Mantivemos os recursos do fork Cxsmo/Yso: buffer com orçamento de memória do aparelho, controles de passthrough por formato, diagnóstico de áudio/HDR/rede, avaliação de configurações e teste da fonte real. Ativamos o motor de buffer por padrão, mantendo o gerenciamento de memória ativado e o cache em disco desligado. A espera inicial da Fonte Inteligente na Shield pode chegar a 12 segundos por padrão para acomodar fontes pesadas; os limites infantis e os tempos explícitos da fonte continuam respeitados.

Também estão presentes calendário, prévias Seekr com ajuste de sincronização, dimmer do aplicativo, controles de pular trechos e opções de recomendação após o filme. Alguns dependem de addons, contas ou metadados compatíveis. Essas melhorias vieram dos forks, com créditos preservados.

## O que este trabalho não comprova sozinho

O APK não aumenta a velocidade da internet nem a capacidade do hardware. REMUX, HDR e áudio sem perdas dependem da fonte, da rede, das conexões HDMI e dos formatos aceitos pelos equipamentos. Não prometemos Dolby Vision na Shield 2017 nem Atmos no Denon AVR-S510BT.

A auditoria dos recursos PT-BR passou nas duas edições. Resultados finais de compilação, testes, assinatura e tamanho serão registrados junto dos APKs. Navegação com controle, reprodução real e saída HDMI ainda precisam ser testadas nos seus aparelhos.
