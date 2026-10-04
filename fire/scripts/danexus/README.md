# DaNexus — FASE 12

Esta pasta contém a integração DaNexus. A documentação atual está em RELATORIO_SIMPLES.md e hardware.json.

Na edição FireTV, README_EDITIONS.md descreve as duas bases, os aparelhos e a validação. build-editions.ps1 compila sequencialmente e grava resultados reais na entrega/FASE12. serve-deliveries.py disponibiliza somente os arquivos de entrega pela porta 8798.

README_FASE06_HISTORICO.md descreve uma fase antiga, com outros pacotes e limites. Não é um guia de instalação dos APKs da FASE 12.

Os scripts Python de configuração, port e tradução documentam a migração já aplicada. Não precisam ser executados novamente para compilar; alguns são migradores de execução única. audit-editions.py é uma conferência sem alterações.

As credenciais locais, local.properties e local.dev.properties são ignoradas pelo Git. Para uma nova máquina, configure o SDK e os parâmetros locais exigidos pela base e use Java 21.

Os créditos do Nuvio, Cxsmo e Yso e a licença GPL-3.0 estão preservados. O código-fonte correspondente deve acompanhar qualquer distribuição dos APKs.