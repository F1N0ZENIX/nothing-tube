<div align="center">

# ● NOTHING TUBE

**Sua experiência de vídeo. Nativa, escura e feita para descobrir.**

Aplicativo Android independente, com interface AMOLED inspirada no minimalismo da Nothing, player nativo e recomendações personalizadas no próprio aparelho.

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-Nativo-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Media3](https://img.shields.io/badge/Player-Media3%20%2F%20ExoPlayer-E53935)](https://developer.android.com/media/media3)
[![Build APK](https://github.com/F1N0ZENIX/nothing-tube/actions/workflows/main.yml/badge.svg)](https://github.com/F1N0ZENIX/nothing-tube/actions)

</div>

---

## ✦ O que é?

> **Ferramenta extra:** [Seed Reader — Legacy Lab](https://github.com/F1N0ZENIX/nothing-tube/tree/main/seed-reader), um caderno web para seeds e coordenadas de Minecraft PE 0.15.x. O leitor ainda não prevê spawners automaticamente; veja as limitações na documentação.

O **Nothing Tube** é um projeto experimental de player de vídeo Android em Kotlin. Ele combina uma interface escura com detalhes vermelhos, pesquisa de vídeos e reprodução em um player nativo, sem depender de um player de vídeo embutido em WebView.

## Recursos

| Recurso | Como funciona |
|---|---|
| **Página inicial recomendada** | Mostra vídeos populares e tenta personalizar a seleção de acordo com pesquisas recentes e vídeos assistidos. |
| **Algoritmo local de recomendação** | Combina tendências públicas, correspondência de palavras-chave, pesquisas recentes e histórico de vídeos vistos. Não exige login. |
| **Controle de recomendações** | Desative a seção “Para você” nas configurações. Também é possível limpar os dados usados para personalizar as sugestões. |
| **Pesquisa** | Cartões nativos com miniatura, título, canal, visualizações e duração; carregamento de mais resultados quando disponível. |
| **Player nativo** | Reprodução com AndroidX Media3 / ExoPlayer. |
| **Picture-in-Picture (PiP)** | Ações de vídeo anterior, pausar/continuar e próximo vídeo, usando a fila carregada. |
| **Velocidade de reprodução** | A velocidade escolhida no player é mantida ao trocar de vídeo. |
| **Tela cheia** | Alternância entre player compacto e tela cheia, com reset do estado ao mudar de vídeo. |
| **Detalhes do vídeo** | Tentativa de exibir descrição e comentários públicos em modo de leitura. |
| **Histórico** | Pesquisas recentes armazenadas localmente e opção de apagar. |
| **Atalhos** | Acesso a páginas do YouTube e ao YouTube Music. |

## Como funciona a recomendação?

1. Busca vídeos da página pública de tendências do YouTube.
2. Usa algumas das pesquisas recentes e os títulos/canais de vídeos assistidos como sinais de interesse.
3. Calcula uma pontuação de relevância por correspondência de palavras-chave.
4. Reduz a pontuação de vídeos já vistos e ordena a lista resultante.

É um algoritmo experimental e local — **não é o algoritmo oficial do YouTube**. As sugestões dependem dos dados públicos que o YouTube disponibilizar no momento.

## Instalar o APK

1. Abra **[GitHub Actions](https://github.com/F1N0ZENIX/nothing-tube/actions)**.
2. Selecione a execução mais recente que terminou com sucesso.
3. Na seção **Artifacts**, baixe `Nothing-Tube-debug`.
4. Extraia o ZIP e instale `app-debug.apk` no Android. Talvez seja necessário permitir instalações pela aplicação usada para abrir o arquivo.

> O artefato é de depuração (debug), destinado a testes; não é uma versão publicada na Play Store.

## Compilar

- **JDK:** 17
- **Android:** API 26 ou superior para o PiP descrito neste projeto
- **Build:** Gradle / Android Gradle Plugin configurados no repositório

No terminal, na raiz do projeto:

```bash
./gradlew assembleDebug
```

O APK será gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

O GitHub Actions também compila o APK automaticamente quando há alterações na branch principal.

## Limitações conhecidas

- A pesquisa, as recomendações, a descrição e os comentários dependem de páginas internas públicas do YouTube e de formatos que podem mudar.
- A resolução do stream tenta usar NewPipe Extractor e serviços públicos alternativos; nenhuma instância externa tem disponibilidade garantida.
- O PiP percorre os vídeos que já estão na fila carregada pelo app. Ele não cria uma playlist oficial do YouTube.
- Os comentários são somente para leitura nesta versão. Login Google e publicação de comentários estão temporariamente fora do escopo.
- Alguns vídeos podem não oferecer stream compatível, descrição ou comentários acessíveis. Isso não significa necessariamente um problema no aparelho.
- A personalização é armazenada localmente; limpar os dados de personalização remove as pesquisas e os vídeos assistidos usados pelo ranking.
- O projeto é independente, não oficial e não afiliado à Nothing Technology Limited, Google ou YouTube.

## Tecnologias

- Kotlin e Android SDK
- AndroidX Media3 / ExoPlayer
- NewPipe Extractor
- Android WebView para algumas páginas auxiliares
- GitHub Actions para compilação automática

## Identificador

`com.f1n0.nothingtube`

## Aviso de marca

Nothing, YouTube e Google são marcas de seus respectivos titulares. Este projeto é experimental, não oficial e não representa essas empresas.

---

<div align="center">

**NOTHING TUBE · VERSÃO 1.0**

*Feito para explorar. Ainda em desenvolvimento.*

</div>
