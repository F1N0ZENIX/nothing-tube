# Seed Reader — Legacy Lab

Ferramenta web leve, pensada para celular, para catalogar seeds e coordenadas no Minecraft Pocket Edition antigo (0.15.x).

## Abrir
Abra `index.html` no navegador ou publique esta pasta com GitHub Pages.

## Recursos do MVP
- Campo para identificar a seed atual.
- Registro manual de spawners/dungeons, vilas, cavernas e outros pontos com X/Y/Z.
- Distância horizontal aproximada de cada ponto até (0, 0).
- Mapa esquemático clicável para preencher coordenadas ilustrativas.
- Biblioteca local no navegador e exportação JSON.

## Limitação importante
Esta versão **não implementa o algoritmo de geração de mundos do Minecraft PE 0.15.3** e não prevê spawners automaticamente a partir de uma seed. O mapa é esquemático, não é terreno real. Não use coordenadas exibidas por ferramentas de outras edições como se fossem confirmadas para PE 0.15.3. O objetivo deste MVP é organizar e verificar dados enquanto o algoritmo legado é pesquisado.

## Privacidade
Os dados ficam no armazenamento local do navegador. Use Exportar para criar um backup JSON. Limpar dados do navegador pode apagar a biblioteca.

## Próximo passo técnico
Implementar e validar a geração Bedrock/PE 0.15.3 contra mundos reais da mesma versão antes de habilitar previsões automáticas.
