# Nothing Tube — versão 1.0

Aplicativo Android nativo em Kotlin, com visual AMOLED escuro inspirado na linguagem minimalista da Nothing e detalhes vermelhos.

## Recursos

- Pesquisa de vídeos do YouTube com cartões nativos, miniaturas, canal, duração e carregamento contínuo de resultados.
- Reprodução por player nativo Media3/ExoPlayer, com tentativa de resolver o stream por NewPipe Extractor e serviços públicos alternativos.
- Modo picture-in-picture (PiP) com ações para **vídeo anterior** e **próximo vídeo** dentro dos resultados carregados da pesquisa.
- Modo de tela cheia e preservação do estado da interface ao girar o aparelho.
- Descrição do vídeo e tentativa de carregar comentários públicos.
- Comentários em modo de leitura nesta versão. **Login Google e publicação de comentários foram removidos temporariamente** para simplificar o aplicativo enquanto essa parte é retrabalhada.
- Histórico de pesquisas salvo localmente, com opção para apagar.
- Atalhos para explorar vídeos populares e abrir o YouTube Music.
- Interface atualizada na tela inicial, resultados, player e páginas internas.
- Compilação automática do APK debug pelo GitHub Actions.

## Limitações conhecidas

- A pesquisa, a extração do stream, a descrição e os comentários usam páginas/formats internos do YouTube e/ou instâncias públicas não oficiais. Esses serviços podem mudar, ficar indisponíveis, limitar solicitações ou não retornar dados para determinados vídeos.
- Os botões anterior/próximo do PiP navegam pela fila de vídeos dos resultados carregados. Eles não criam uma playlist oficial do YouTube; se não houver outro resultado na direção escolhida, o app avisa.
- Comentários podem não aparecer se estiverem desativados, se a página não fornecer os dados ou se o YouTube limitar a consulta.
- A reprodução em segundo plano e o PiP dependem do suporte do Android e das condições de reprodução do stream.
- O aplicativo não é oficial e não tem afiliação com Nothing Technology Limited nem com Google/YouTube.

## Tecnologias

- Kotlin, Android SDK, Gradle e Java 17.
- AndroidX Media3 / ExoPlayer.
- NewPipe Extractor, usado como uma das tentativas de resolver streams.
- Android WebView para algumas páginas internas.

## Gerar o APK pelo celular

1. Abra [Actions do repositório](https://github.com/F1N0ZENIX/nothing-tube/actions).
2. Entre na execução mais recente do workflow de build.
3. Aguarde o job terminar com sucesso.
4. Baixe o artefato **Nothing-Tube-debug**, extraia o ZIP e instale `app-debug.apk`. Se necessário, permita a instalação pelo aplicativo usado para abrir o arquivo.

## Requisitos

- Android 8.0 ou superior (API 26+).
- Java 17 no ambiente de compilação.

## Identificador do aplicativo

`com.f1n0.nothingtube`

## Licença e marca

Projeto independente e não oficial. Os nomes e marcas de Nothing, Google e YouTube pertencem aos seus respectivos titulares.
