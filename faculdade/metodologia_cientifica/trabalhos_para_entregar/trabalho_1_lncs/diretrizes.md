# Diretrizes de escrita do artigo

Regras para construir e revisar o `artigo.tex`, que é um **planejamento de estudo primário** da disciplina de Metodologia Científica (CEFET-RJ). Elas reúnem as decisões tomadas pelo Matheus durante a escrita, as regras dos slides da disciplina (prof. Eduardo Ogasawara) e as notas de aula do Matheus (`../../aula30092026.txt`). Quem for mexer no texto, pessoa ou Claude, segue estas regras; quando uma regra nova surgir, ela entra aqui.

## 1. Natureza do texto: uma proposta, e não um relato

- O artigo planeja uma pesquisa que ainda não foi feita. Ele não apresenta nem promete resultados (template oficial).
- **Duas espécies de decisão, dois tratamentos:**
  - **Como a pesquisa testa a hipótese** (pergunta, hipótese, métricas, *baseline*, critério de sucesso, procedimento de análise): fica concreto, porque o template pede uma avaliação "concreta o suficiente para demonstrar rigor metodológico" e o slide 21 pede definir desde cedo como provar ou refutar a hipótese.
  - **Escolhas técnicas da solução** (fórmulas, algoritmos, critérios de seleção, parâmetros): ainda dependem de estudo e de prática. Entram como **alternativas candidatas**, cada uma com a fonte, o critério de escolha e a etapa em que a escolha acontece (por exemplo, o estudo piloto). O template admite que "nem todos os aspectos técnicos estejam completamente implementados".
- **Nada é apresentado como "o melhor"** sem uma fonte que o sustente ou um teste previsto que o decida. Exemplo: o texto não diz "usa-se o teste de Wilcoxon"; diz que o teste é escolhido depois do diagnóstico de normalidade (slide 25: a escolha "deve se basear em diagnóstico, não em intuição").
- **Sem fórmulas de solução.** Equações só entram para algo já estabelecido na literatura ou para uma medida da avaliação (por exemplo, o tamanho de efeito).
- **Eixo do artigo:** a proposta continua o que a iniciação científica pretendia e não alcançou: avaliação com voluntários, em sessões repetidas, com análise estatística, nos músculos da mímica. A versão atual é descrita com fidelidade, inclusive o que ela já faz (calibração a cada sessão, ajuste às sessões recentes, reescala da amplitude, modo de treino rotulado), para que a contribuição não reivindique o que já existe (decisão do Matheus). O *Matrix Profile* saiu da proposta por alerta do professor e pelo slide 21 (escopo viável): o artigo se centra no aprendizado contínuo do perfil de ativação muscular, dividido entre a faixa do sinal de cada sessão (artefato) e os parâmetros de decisão (do jogador).
- **O que a proposta promete testar (decisões do Matheus, 25/11):** reduzir o tempo de preparação **e** aumentar o acerto em relação à versão atual, com o acerto mais estável entre sessões (consistência), sem apagar o histórico. O perfil acompanha o fortalecimento da musculatura entre sessões e a fadiga dentro da partida. O **ambiente não é fixado**: as sessões acontecem em locais com mais e menos interferência, cada participante passa pelos mesmos locais nas duas condições, e cada sessão registra uma medida da interferência, para entender como o sensor se comporta. Todas as comparações entre A e C são principais, com o nível de significância corrigido pelo método de Holm.
- **Uma decisão nova chega a todos os trechos que ela toca** (resumo, pergunta, hipótese, objetivos, revisão, metodologia, figura, tabelas, avaliação, limitações e cronograma), e não só ao exemplo que o Matheus deu (decisão do Matheus, 25/11).
- **Desenho experimental e amostra:** ficam como *previstos*, com o motivo da escolha, e sujeitos à revisão depois do piloto. A amostra inicial de adultos saudáveis é um recorte de viabilidade e de ética, e as limitações dizem o que ela não permite concluir.

## 2. O que não entra no texto

- **Dados que pressupõem o trabalho pronto:** taxa de amostragem, número de participantes e de sessões, número do parecer do Comitê de Ética. O tamanho da amostra aparece como "definido por análise de poder a partir do estudo piloto".
- **O relatório da iniciação científica** (`../DOC-20260930-WA0037.pdf`) serve só para corrigir a descrição do sistema. O artigo não o cita nem usa os números dele, porque ele não passou por revisão por pares (slide 05) e os números vêm de um único usuário.
- **O sistema atual** pode ser descrito pelo relatório da IC, em palavras e sem números, como sistema dos próprios autores, o que dispensa citação (slide 08); o artigo do SBCAS (Sousa e Guedes, 2026) continua citado como origem do jogo. O músculo exato não é nomeado, porque o relatório não o confirmou; a montagem com dois zigomáticos aparece como *prevista*, e não como fato (decisão do Matheus).
- **Afirmações sem verificação**, como o filtro analógico de 20 a 450 Hz, que ainda não foi caracterizado.

## 3. Forma

- **Tempo verbal:** presente em todo o texto ("A avaliação compara..."), sem alternar tempos na mesma frase. O futuro só aparece nos trabalhos futuros (slide 08; nota de aula: "é apresentado", e não "será apresentado").
- **Parágrafos:** uma ideia por parágrafo, frases curtas, de 3 a 10 linhas, com a primeira frase anunciando o tema (slides 08 e 09). As primeiras frases, lidas em sequência, formam uma narrativa.
- **Seções:** nenhuma com menos de 3 parágrafos; quem se subdivide tem pelo menos duas subseções equilibradas (slide 09).
- **Termos:** cada termo não trivial e cada sigla são definidos uma única vez, antes do primeiro uso, sem espalhar a definição. Um só nome por conceito ("sEMG" sempre). Termos estrangeiros em itálico (slide 08; nota de aula).
- **Nomes fixados (decisões do Matheus, 25/11):** "perfil de ativação muscular", ou só "perfil", para o que o jogo guarda de cada jogador (como os músculos se ativam em repouso, na contração máxima e com o cansaço, e os valores que reconhecem os gestos); "faixa do sinal" para o nível em repouso e o da contração máxima de cada lado (antes "escala"); "gravação de gesto" para o trecho do sinal gravado com o nome do gesto pedido (antes "ensaio", que também significava "teste"); "retenção do perfil" para a medida de esquecimento, que não tem relação com a atenção ou a adesão do jogador.
- **Linguagem natural, sem cara de texto gerado (pedido do Matheus, 25/11):** evitar apostos encadeados ("X, um artefato a compensar, ou Y, um efeito a preservar"), antíteses de efeito ("mede o paciente que chegou, e não o que se recupera"), ponto e vírgula em série e a fórmula "não é X, mas Y". Preferir frases diretas, uma ideia por frase.
- **Tom:** formal e uniforme, sem adjetivos vagos ("muito eficiente", "inovador"), sem opinião, sem conselho ao leitor. "Significativo" só com teste estatístico (slide 08).
- **Fluxo:** progressivo, sem voltar a ideias já apresentadas (nota de aula).
- **Resumo:** curto, com tema, motivação, problema, hipótese, método, avaliação e contribuição esperada, sem prometer resultados (template; nota de aula).

## 4. Citações e referências

- `\citet{chave}` para a citação de autoridade ("Zhang et al. (2024) usam...") e `\citep{chave}` para a citação de ideia ("... (Zhang et al., 2024)") (slide 05; template).
- Toda ideia alheia vem com a fonte, perto da informação; parágrafos que descrevem a própria proposta não precisam de citação (slide 08).
- Só fontes revisadas por pares; pelo menos 10 artigos indexados no Scopus (template).
- **Só fontes estreitamente ligadas ao tema.** Cada referência precisa acrescentar informação ao argumento; a que não agrega sai, mesmo que esteja no Scopus. Uma referência fora do Scopus pode ficar se for central para o tema, só não conta para os 10 (decisão do Matheus).
- Cada referência nova tem o DOI conferido no Crossref, e o Matheus confere a indexação no Scopus. Os campos do `.bib` se limitam a autor, título, revista ou anais, ano, volume, páginas e DOI (slide 20).
- Nas referências citadas com mais de três autores, o `.bib` traz só o primeiro autor e `and others`, que sai como "et al." (decisão do Matheus, 25/11, para economizar espaço sem tirar fontes).
- Siglas e nomes próprios nos títulos do `.bib` vão entre chaves (`{sEMG}`), porque o estilo `plainnat` passa os títulos para minúsculas.
- Afirmações sobre um trabalho citado só com base no que foi lido dele (resumo ou texto completo), e não em resumos de busca.

## 5. Figuras e tabelas

- Citadas no texto antes de aparecer, explicadas no texto, com legenda autoexplicativa (slides 08, 12 e 13).
- Tabelas sem linhas verticais, só com as linhas de cabeçalho e rodapé (`booktabs`); texto alinhado à esquerda; cada linha cabendo numa linha, quando possível (slide 13).
- Figura em tons de cinza, sem rótulos sobrepostos, com cada símbolo explicado na legenda (slide 12). Símbolos só se já tiverem sido definidos antes da figura.

## 6. Formato e entrega

- Template oficial: classe `llncs`, `natbib` com `[round,authoryear]`, estilo `plainnat`, Agradecimentos antes das referências e a seção de IA generativa depois delas.
- As instruções pedem **entre 6 e 10 páginas**, contando referências e a seção de IA. Decisão do Matheus (25/11): passar uma ou duas páginas é aceitável, sem abusar, numa política conservadora; o risco de perder pontos pelo requisito fica registrado.
- Referências em `\small`, sem espaço extra, e DOI no formato curto do template ("doi: 10....").
- Agradecimentos: só a CAPES, pelo acesso ao Portal de Periódicos. Autores: campo a preencher.
- Seção de IA generativa: uma frase, com o texto definido pelo Matheus (uso da IA nas buscas no Scopus e na localização de referências).
- Compilação local, no VS Code, com `latexmk -pdf artigo.tex` (precisa do pacote `texlive-publishers`, que traz a `llncs`).

## 7. Como o trabalho anda

- Os conselhos dos slides da disciplina são seguidos fielmente, porque garantem a estrutura do artigo. Quando um corte ou uma mudança contrariar um slide, o Claude avisa e pergunta antes (decisão do Matheus).
- O Claude escreve e edita em trechos pequenos, para o Matheus revisar pelo diff.
- Tudo o que depende da opinião do Matheus vira pergunta com opções, com a recomendada primeiro.
- Commit e push esperam o "pode" do Matheus.
