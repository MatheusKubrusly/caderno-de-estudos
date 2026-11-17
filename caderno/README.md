# Caderno

O caderno é onde o método de estudo com IA ([ai-tutor](https://github.com/MatheusKubrusly/ai-tutor)) guarda o que o Matheus aprende. As ferramentas do método leem e gravam aqui; o Matheus escreve as partes que só ele pode escrever.

## Partes

| Parte | Onde | Quem escreve |
|---|---|---|
| Perfil | [`perfil.md`](perfil.md) | O Matheus, a partir de um questionário |
| Conceitos | `conceitos/<área>/<tópico>/<conceito>.md` | O Matheus escreve a explicação; o Claude transcreve o material de origem e propõe as perguntas |
| Receitas | `receitas/<área>/<receita>.md` | O Matheus |
| Respostas | `respostas/<AAAA>/<AAAA-MM-DD>-<assunto>.md` | O Matheus responde; o Claude registra a correção e a nota |
| Fila de revisão | [`fila.csv`](fila.csv) | As ferramentas do método |
| Dívidas de aprendizado | [`dividas.csv`](dividas.csv) | O Claude propõe; o Matheus confirma o nível |
| Desempenho | [`desempenho.csv`](desempenho.csv) | As ferramentas do método |
| Retrato | `retrato.md` (quando existir) | Gerado a partir do desempenho; nunca editado à mão |
| Modelos | [`modelos/`](modelos/) | Modelos vazios de conceito, receita e resposta |

## Conceitos

Um **conceito** é uma ideia que cabe numa boa pergunta de revisão (por exemplo, "índice multinível" ou "ANOVA de um fator"). Ele é organizado por assunto, em área › tópico › conceito, e cada conceito existe uma vez só. A disciplina em que ele apareceu é só uma etiqueta: o mesmo conceito pode vir da faculdade, de um curso ou de um projeto.

Cada página começa com um cabeçalho entre linhas `---`, com um campo por linha, que as ferramentas conseguem ler:

| Campo | O que guarda |
|---|---|
| `conceito` | O nome, com acentos (ex.: `Índice multinível`) |
| `area`, `topico` | O caminho da página, iguais aos nomes das pastas |
| `disciplinas` | Onde o conceito apareceu, entre colchetes (ex.: `[Organização de Estruturas de Arquivos]`); vazio se não veio de uma disciplina |
| `nivel` | De 0 a 4 (abaixo), ou `não avaliado` |
| `origem` | De onde o conceito veio (ex.: `anterior ao método (Notion)`, `sessão de estudo`) |
| `pre-requisitos` | Caminhos de outros conceitos, entre colchetes |
| `criado` | Data em que a página foi criada (AAAA-MM-DD) |

Depois do cabeçalho vêm quatro seções: **Explicação** (escrita pelo Matheus), **Material de origem** (transcrições de prints e notas, feitas pelo Claude, com marca de confiança onde a leitura for duvidosa), **Materiais** (links para os arquivos em que o conceito aparece) e **Perguntas de revisão**.

Os prints originais ficam numa pasta `anexos/`, ao lado das páginas do tópico.

### Níveis

| Nível | Significa |
|---|---|
| 0 | Não visto |
| 1 | Apresentado |
| 2 | Em prática |
| 3 | Dominado: nota 3 ou mais sem ajuda, duas vezes, com pelo menos 7 dias entre elas |
| 4 | Consolidado: o nível 3, mais um acerto depois de 21 dias ou mais, a explicação escrita e o desafio do tópico vencido |
| `não avaliado` | Conceito estudado antes do método, ainda sem medida. Fica fora da escala e da fila até voltar num estudo |

Um erro sem ajuda faz o conceito voltar ao nível 2.

### Perguntas de revisão

Cada pergunta mora na página do seu conceito e tem um código: o caminho do conceito, `#p` e um número (ex.: `ciencia-da-computacao/banco-de-dados/indice-multinivel#p1`). A fila e o desempenho usam esse código. Conceitos estudados antes do método não ganham perguntas até voltarem num estudo.

## Receitas e respostas

Uma **receita** é o passo a passo, escrito pelo Matheus com as próprias palavras, para refazer uma tarefa sem IA (por exemplo, "conectar a rede com autenticação no Hyprland"). Uma **resposta** é o que o Matheus respondeu numa sessão, ligado ao código da pergunta, com a correção e a nota. Os formatos estão nos [modelos](modelos/).

## Planilhas

As planilhas são arquivos CSV: texto simples, uma linha por registro e as colunas separadas por vírgula. Datas no formato AAAA-MM-DD. Valores de lista sem acento, para as ferramentas compararem sem erro.

### `desempenho.csv`

A fonte única dos números do aprendizado. Uma linha por evento.

| Coluna | O que guarda |
|---|---|
| `data` | Dia do evento |
| `tipo` | `resposta` (uma pergunta respondida), `bloco` (um bloco de estudo com tempo) ou `dia` (o esforço do dia) |
| `conceito` | Caminho do conceito a partir de `conceitos/`, sem o `.md` |
| `pergunta` | Código da pergunta, quando o tipo for `resposta` |
| `nota` | Qualidade da resposta, de 0 a 4 |
| `degrau` | Ajuda usada: 0 sozinho; 1 perguntas; 2 pistas; 3 pontos que faltaram ou exemplo parecido; 4 resposta comentada |
| `confianca` | Confiança antes da correção, de 0 a 100 (%) |
| `minutos` | Tempo cronometrado; vazio quando não houver |
| `esforco` | Esforço mental, de 1 a 9 |
| `origem` | Onde aconteceu (ex.: `claude-code`) |
| `observacao` | Nota curta, opcional |

### `fila.csv`

As perguntas que esperam revisão. Uma linha por pergunta.

| Coluna | O que guarda |
|---|---|
| `pergunta` | Código da pergunta |
| `conceito` | Caminho do conceito |
| `proxima_revisao` | Dia em que a pergunta vence |
| `intervalo_dias` | Intervalo atual: 1, 3, 7, 21 ou 60 |
| `prioridade` | `erro-confianca-alta` (errou com 80% ou mais de confiança), `pre-requisito` (base fraca do que vem a seguir) ou `normal` |
| `ultima_revisao` | Dia da última revisão; vazio se nunca foi revisada |

### `dividas.csv`

O que ficou para depois quando o Matheus usou um atalho (por exemplo, quando pediu a resposta direta por pressa).

| Coluna | O que guarda |
|---|---|
| `data` | Dia em que a dívida nasceu |
| `assunto` | Do que se trata, em poucas palavras |
| `nivel` | `alto`, `medio` ou `baixo`, conforme o quanto o assunto importa para o estudo |
| `o_que_falta` | O que pagar (ex.: refazer sem ajuda e escrever a receita) |
| `situacao` | `aberta` ou `paga` |
| `ligado_a` | Caminho do conceito ou da receita, quando houver |
