# Caderno de estudos

Este repositório guarda o que o Matheus estuda. Ele tem duas partes:

- os **materiais** das disciplinas da faculdade (aulas, exercícios, trabalhos e provas), do jeito que foram produzidos;
- o **caderno** do método de estudo com IA (o [ai-tutor](https://github.com/MatheusKubrusly/ai-tutor)), que registra os conceitos aprendidos, as receitas, as respostas e os números do aprendizado.

## Pastas

| Pasta | O que guarda |
|---|---|
| [`caderno/`](caderno/) | Os conceitos, organizados por assunto, e os registros do método: perfil, fila de revisão, dívidas de aprendizado e desempenho. Os formatos estão no [README do caderno](caderno/README.md) |
| [`faculdade/`](faculdade/) | Os materiais de cada disciplina, com os nomes e as subpastas originais |
| `gabaritos/` | Quando houver: gabaritos de exercícios, lidos pelo avaliador do método. Não abrir antes de tentar o exercício |
| `evolucao/` | Quando existir: gráficos e dados da evolução, gerados automaticamente e lidos pelo portfólio |

## Como as partes se ligam

O material fica junto da sua origem (a disciplina em que foi feito); o conceito fica junto do seu assunto. Um trabalho de Banco de Dados, por exemplo, mora em `faculdade/projeto_de_banco_de_dados/`, enquanto os conceitos que ele usa moram em `caderno/conceitos/ciencia-da-computacao/banco-de-dados/`.

Os dois lados apontam um para o outro: cada página de conceito lista os materiais em que ele aparece, e o README de cada disciplina lista os conceitos que ela cobriu. Assim, um conceito visto na faculdade e retomado depois, num curso ou num projeto, continua num lugar só.

## Notas do Samsung Notes

As anotações feitas no tablet entram como PDF, ao lado da sua transcrição (um arquivo `.md`, com as fórmulas em LaTeX), na pasta da fonte da nota: as de uma disciplina em `faculdade/<disciplina>/notas/`; as outras, na pasta da origem delas, decidida na hora de importar. As páginas de conceito apontam para a nota.

## Disciplinas

| Disciplina | Pasta |
|---|---|
| Arquitetura e Padrões de Software | [`faculdade/arquitetura-e-padroes-de-software/`](faculdade/arquitetura-e-padroes-de-software/) |
| Arquitetura de Linguagens de Programação | [`faculdade/arquitetura_de_linguagens_de_programacao/`](faculdade/arquitetura_de_linguagens_de_programacao/) |
| Cálculo Numérico | [`faculdade/calculo_numerico/`](faculdade/calculo_numerico/) |
| Inferência Estatística | [`faculdade/inferencia_estatistica/`](faculdade/inferencia_estatistica/) |
| Metodologia Científica | [`faculdade/metodologia_cientifica/`](faculdade/metodologia_cientifica/) |
| Organização de Estruturas de Arquivos | [`faculdade/organizacao_de_estruturas_de_arquivos/`](faculdade/organizacao_de_estruturas_de_arquivos/) |
| Programação Orientada a Objetos | [`faculdade/programacao_orientada_a_objetos/`](faculdade/programacao_orientada_a_objetos/) |
| Projeto de Banco de Dados | [`faculdade/projeto_de_banco_de_dados/`](faculdade/projeto_de_banco_de_dados/) |

## Convenções

- Nomes novos de pastas e arquivos: minúsculas, com hífen e sem acento (por exemplo, `banco-de-dados`). As pastas da faculdade mantêm os nomes originais.
- Formatos simples, que qualquer ferramenta lê: Markdown para textos e CSV para planilhas.
- O repositório é público: nada que não deva ser visto por qualquer pessoa.
- Os arquivos `.dat` são guardados pelo Git LFS, um complemento do git para arquivos grandes (ver `.gitattributes`).
