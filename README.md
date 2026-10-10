# Nothing Tube — MVP 0.2.0

Aplicativo Android em Kotlin, com interface escura inspirada na estética minimalista da Nothing.

## Recursos atuais
- Interface escura com detalhes vermelhos e ícone próprio do Nothing Tube.
- Pesquisa com cartões nativos, miniaturas, título, canal e duração.
- Carregamento automático de mais resultados ao rolar a lista, usando continuação da pesquisa do YouTube.
- Player incorporado na tela do app ao tocar em um vídeo.
- Histórico local de pesquisas, com opção para limpar.
- Atalhos para explorar vídeos populares e abrir o YouTube Music.
- Ajustes de área segura para evitar que a barra de status/notificações cubra os controles.
- Build automatizado do APK debug pelo GitHub Actions.

## Observações
- A pesquisa usa dados da página e da interface interna do YouTube, sem uma chave oficial de API. O formato pode mudar ou o serviço pode limitar requisições; nesse caso, a pesquisa ou o carregamento contínuo pode falhar temporariamente.
- A reprodução usa o player incorporado oficial do YouTube dentro de uma WebView. Por isso, anúncios, restrições de idade/região, disponibilidade do vídeo e políticas de reprodução continuam sob controle do YouTube. Este build ainda não extrai streams para um player nativo Media3 e não implementa reprodução em segundo plano.
- O ícone e a interface são criações deste projeto. “Nothing Tube” é um projeto independente e não oficial.

## Créditos e tecnologias
- **YouTube Embedded Player / IFrame Player** — reprodução incorporada; documentação: https://developers.google.com/youtube/iframe_api_reference
- **Android WebView** — componente Android usado para apresentar o player incorporado.
- **Kotlin, Android SDK e Gradle** — tecnologias usadas no aplicativo.
- A pesquisa e a paginação usam formatos internos do YouTube; não são uma API oficial garantida e podem exigir manutenção.

Nenhum código do aplicativo NewPipe foi copiado ou incorporado nesta versão. O projeto não inclui atualmente a biblioteca AndroidX Media3/ExoPlayer; ela não reproduz diretamente URLs de páginas do YouTube sem uma etapa separada de obtenção do stream.

## Compilar pelo GitHub usando o celular
1. Abra o repositório e vá para a aba **Actions**.
2. Entre na execução mais recente do workflow **Build Nothing Tube APK**.
3. Quando a execução terminar com sucesso, baixe o artefato **Nothing-Tube-debug**.
4. Extraia o ZIP e instale o arquivo `app-debug.apk`. Talvez seja necessário autorizar a instalação pelo app usado para abrir o arquivo.

## Requisitos
- Android 8.0 ou superior (API 26+).
- Java 17 e Gradle 8.9 no ambiente de build, configurados automaticamente.

## Identificador
`com.f1n0.nothingtube`

## Licença e marca
Projeto independente e não oficial, sem afiliação com Nothing Technology Limited ou Google/YouTube. O nome e a marca YouTube pertencem aos seus respectivos titulares.
