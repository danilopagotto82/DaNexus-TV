# DaNEXUS • Badge Fusion — Cinema Edition

Pacote visual **independente** para a tela de fontes do Nuvio / DaNEXUS, sem mudanças no APK, nos perfis de AIOStreams ou nos filtros de reprodução.

## Importar no Nuvio

URL **raw** desta versão experimental (branch de homologação):

```text
https://raw.githubusercontent.com/danilopagotto82/DaNexus-TV/feature/badge-fusion-cinema-20261101/badges/fusion-cinema/badges.json
```

TV: Configurações → Layout → Streams → Fusion badge URLs → Importar. Se CineBiel já estiver carregado, retire-o antes de testar o DaNEXUS para evitar logos duplicados (o Nuvio permite até três URLs).

## Incluído

- 56 PNGs próprios (360 × 106, fundo escuro, ícone à esquerda, logotipo/wordmark na própria imagem).
- Resolução, HDR/Vision, profundidade, qualidade, codecs, áudio, canais, idioma, serviços, edições especiais e estado.
- Canais de áudio 2.0/5.1/7.1 agora independentes dos formatos Atmos / TrueHD / DTS; a informação de codec não é suprimida pela de canais.
- Áudio espacial e codec usam grupos distintos, permitindo exibir Atmos + TrueHD quando ambos constam do stream.
- 10bit independente de HDR.
- Regex de 2.0 normalizado; indicador de áudio PT só quando o título relata confirmação expressa.
- Prévia visual: [preview.png](preview.png).

## Verificações e limites

Badges **não analisam o arquivo de vídeo** nem garantem que um codec, faixa, HDR ou idioma exista. Eles só reconhecem os termos presentes nos títulos dos streams. Não derivar DUBLADO/LEGENDADO/ÁUDIO PT de sinopse, idioma original do TMDB ou legenda.

**Importante:** obra visual experimental de produção própria. Os nomes dos serviços e tecnologias identificam formatos e provedores; estes emblemas não são ativos oficiais de seus titulares. Origem da lista de 56 correspondências: [CineBiel](https://cinebiel.duckdns.org/badges/badges.json); formato: [Nuvio Badges Guide](https://nuvio.wiki/settings/badges). Não há redistribuição das imagens originais do CineBiel.

Para reconstruir: instalar `Pillow`, executar `py -3 generate.py` nesta pasta. A reconstrução usa o JSON de referência na internet; **a versão publicada não depende dele em tempo de execução**.

O pacote não foi homologado visualmente em aparelhos; exige smoke real na Shield e Fire TV antes de ser considerado definitivo.
