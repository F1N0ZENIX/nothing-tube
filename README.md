# Nothing Tube — MVP 0.1.0

Aplicativo Android nativo em Kotlin, com interface escura inspirada na estética minimalista da Nothing.

## O que esta versão já faz
- Interface inicial escura, com acento vermelho e marcação de pontos.
- Pesquisa de vídeos no YouTube, aberta no navegador/app instalado.
- Atalhos para explorar vídeos e abrir YouTube Music.
- Build automatizado do APK de depuração pelo GitHub Actions.

## O que ainda não faz
Esta é a primeira base funcional, não um cliente completo de streaming. A pesquisa e reprodução são delegadas aos serviços oficiais. Player embutido, histórico/favoritos locais, Picture-in-Picture e reprodução em segundo plano ficam para etapas posteriores.

## Compilar no GitHub pelo celular
1. Baixe e extraia o ZIP.
2. Entre em https://github.com e crie um repositório chamado `NothingTube`.
3. Envie o conteúdo da pasta para a raiz do repositório (não envie a pasta externa `NothingTube` como uma subpasta).
4. Abra a aba **Actions** do repositório.
5. Se solicitado, habilite GitHub Actions.
6. Execute **Build Nothing Tube APK** em **Run workflow**, ou aguarde o build automático após enviar os arquivos.
7. Quando terminar com sucesso, abra a execução e baixe o artefato `Nothing-Tube-debug`.
8. Extraia o artefato ZIP e instale `app-debug.apk` no Android. Talvez seja necessário autorizar a instalação pelo app usado para abrir o arquivo.

## Requisitos
- Android 8.0 ou superior (API 26+).
- Java 17 e Gradle 8.9 no ambiente de build, configurados automaticamente no GitHub Actions.

## Identificador
`com.f1n0.nothingtube`

## Licença e marca
Projeto independente e não oficial, sem afiliação com Nothing Technology Limited ou Google/YouTube.
