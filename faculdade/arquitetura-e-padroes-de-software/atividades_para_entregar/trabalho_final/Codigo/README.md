# Sistema inteligente de gerenciamento de estacionamento — Blackboard

Trabalho Final de Arquitetura e Padrões de Software — 2026.2 — Grupo 4  
Tema: **Arquitetura de memória compartilhada (Blackboard)**. Java puro, apenas biblioteca padrão.

## Requisitos

- JDK 17 ou superior (`javac` e `java` no PATH).
- Terminal com UTF-8 (Windows: execute `chcp 65001` antes).

## Como executar

```bash
./run.sh
```

Ou manualmente:

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -Dfile.encoding=UTF-8 -cp out Main
```

## Estrutura

```
src/
├── Main.java                      composition root
├── blackboard/
│   ├── Blackboard.java            Singleton + Subject (todo o estado de domínio)
│   ├── BlackboardController.java  árbitro: ativa o KS cuja condition() é verdadeira
│   └── Observer.java
├── knowledge_sources/
│   ├── KnowledgeSource.java       Template Method (run() final)
│   ├── ExternoKS.java             especialista do setor EXTERNO
│   ├── SubsoloKS.java             especialista do SUBSOLO (altura, preferencial)
│   └── RelatorioKS.java           especialista de relatórios (6 tipos + completo)
├── model/                         enums e classes de dados
├── ui/
│   ├── CLI.java                   coleta dados e monta a Requisicao
│   ├── Display.java               Observer: único que renderiza
│   └── Terminal.java              força UTF-8 na saída
└── util/Formatador.java
testes/SmokeTest.java              teste de fumaça (45 verificações)
```

## Decisões arquiteturais

1. **Blackboard**: todo o estado (vagas, histórico, requisição pendente, resultado e relatório) vive nele. Os KS são *stateless*.
2. **Sem comunicação direta entre especialistas**: nenhum KS importa outro. Só o `BlackboardController` conhece os três.
3. **Fluxo**: a CLI monta a `Requisicao` e a publica completa → o Controller ativa o KS apto → o KS valida, atualiza o Blackboard e publica um `ResultadoOperacao`/`Relatorio` → o Blackboard notifica o `Display` (Observer) → o Controller consome a requisição.
4. **Padrões**: Singleton (`Blackboard`), Template Method (`KnowledgeSource.run()`), Observer (`Blackboard` → `Display`).
5. **Fora do desenho**: Strategy, State e Command foram descartados deliberadamente (ver `descricao-padroes_de_software.pdf`).
6. **Regras de negócio só nos KS**: a CLI valida apenas formato (número, data, placa, vaga existente na matriz).

## Regras do estacionamento

| Setor | Vagas | Preferenciais | Restrição |
|---|---|---|---|
| EXTERNO | 15 (A1–C5) | — | nenhuma |
| SUBSOLO | 10 (A1–B5) | A1 e A2 | altura máxima 2,10 m |

- Alocação manual: o operador escolhe a vaga na matriz; o KS valida e atualiza o Blackboard.
- Vaga preferencial exige motivo (opções 1–6). A opção 7 (“Nenhum dos anteriores”) resulta em motivo `null` e o `SubsoloKS` rejeita a entrada.
- A altura do veículo é validada pelo `SubsoloKS` **assim que é digitada** (`VALIDAR_ALTURA`); se exceder 2,10 m, o sistema pede a altura novamente antes de prosseguir.
- Saída por vaga. Sem regra de placa duplicada.
- Em caso de rejeição, o fluxo permanece na matriz do setor e pede apenas outra vaga — setor, placa e altura validada são preservados. Altura inválida é pedida novamente na hora.
- Relatórios: ocupação por setor, histórico geral, por placa, por período (pela data/hora de **entrada**), tempo médio de permanência e uso de vagas preferenciais.
- **Relatório completo (opção 7)**: condensa todas as seções acima em um único documento, com tabelas compactas e sem repetir títulos.
- O estado é em memória: ao encerrar o programa, tudo é zerado.

## Roteiro de teste manual (demo)

1. Entrada no **Externo**, vaga `B3` → vaga fica ocupada.
2. **Subsolo**, altura `2,20` → rejeitado **na hora**; o sistema pede a altura novamente.
3. **Subsolo**, altura `1,85`, vaga comum (`A3`) → sucesso.
4. **Subsolo**, vaga preferencial `A1`, motivo `3` (Idoso) → sucesso.
5. **Subsolo**, vaga preferencial `A2`, opção `7` → rejeitado, vaga continua livre.
6. **Saída** por vaga (`A1`) → mostra horário de saída e permanência.
7. **Relatórios**: ocupação, histórico geral, por placa, por período, tempo médio, preferenciais e **relatório completo** (opção 7).
8. Encerrar (`0`) e executar de novo → estado zerado.

## Teste automatizado de fumaça

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src testes -name "*.java")
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out SmokeTest
```

Cobre estrutura (Singleton, `run()` final, `condition()` sem efeito colateral), os cenários de setor, os sete tipos de relatório e a análise do código-fonte (nenhum KS referencia outro KS; nenhum `System.out` fora de `ui`).
