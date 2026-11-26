# Sistema inteligente de gerenciamento de estacionamento — Blackboard

Trabalho Final de Arquitetura e Padrões de Software — 2026.2 — Grupo 4  
Tema: **Arquitetura de memória compartilhada (Blackboard)**. Java puro, apenas biblioteca padrão.

O sistema registra quatro eventos para cada veículo (entrar no setor, ocupar uma vaga, desocupar a vaga e sair do setor) e mostra tudo numa janela com o mapa das vagas. O passo a passo de uso, a explicação de cada recusa e um roteiro de apresentação de 10 minutos estão no **[roteiro de uso](../roteiro-de-uso.md)**.

## Requisitos

- JDK 17 ou superior (`javac` e `java` no PATH).
- Terminal com UTF-8 (Windows: execute `chcp 65001` antes).

## Como executar

```bash
./run.sh          # abre a janela
./run.sh --cli    # abre a interface de texto
```

Ou manualmente:

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out Main
```

Num computador sem tela gráfica, o sistema abre a interface de texto sozinho. Os comandos para o Windows estão no roteiro de uso.

## Estrutura

```
src/
├── Main.java                        ponto de montagem: cria o quadro, os especialistas, a fachada e a interface
├── blackboard/
│   ├── Blackboard.java              Singleton + Subject (todo o estado: vagas, estadias, requisição e resultado)
│   ├── BlackboardController.java    controle: ativa o especialista cuja condition() é verdadeira
│   └── Observer.java
├── knowledge_sources/
│   ├── KnowledgeSource.java         Template Method (run() final)
│   ├── EspecialistaDeSetor.java     os quatro eventos, com ganchos para as regras de cada setor
│   ├── ExternoKS.java               especialista do setor EXTERNO
│   ├── SubsoloKS.java               especialista do SUBSOLO (altura e vaga preferencial)
│   ├── RelatorioKS.java             especialista de relatórios (10 tipos + completo)
│   └── Estatisticas.java            cálculos de apoio dos relatórios
├── aplicacao/
│   └── FachadaEstacionamento.java   Facade: ponto de entrada da janela, da CLI e da releitura do CSV
├── persistencia/
│   ├── RegistroDeEventos.java       Observer: grava cada evento aceito em dados/eventos.csv e o relê ao abrir
│   └── CsvDeEventos.java            formato das linhas do CSV
├── model/                           Vaga, Estadia, Ocupacao, Requisicao, ResultadoOperacao, Relatorio e enums
├── ui/
│   ├── CLI.java                     interface de texto
│   ├── Display.java                 Observer: imprime os resultados na interface de texto
│   ├── FormatoDeEntrada.java        confere o formato da placa, da altura e da data
│   ├── Terminal.java                força UTF-8 na saída
│   └── janela/                      janela Swing (JanelaPrincipal é Observer) e os seus painéis
└── util/Formatador.java
dados/cenario-demonstracao.csv       uma semana de uso fictício (16 a 22/11/2026, 2204 eventos)
ferramentas/                         gerador do cenário e captura da janela em imagem
testes/
├── SmokeTest.java                   teste de fumaça (90 verificações)
├── TesteDeAceitacao.java            executa o roteiro de demonstração e uma sessão da CLI
└── roteiro-de-testes-da-janela.md   conferência manual da janela, com os resultados de cada rodada
```

O arquivo `dados/eventos.csv` é criado com o uso e fica fora do git, porque guardaria placas reais.

## Decisões arquiteturais

1. **Blackboard**: todo o estado (vagas, estadias, requisição pendente, resultado e relatório) vive nele. Os KS não guardam estado.
2. **Sem comunicação direta entre especialistas**: nenhum KS cita outro. Só o `BlackboardController` conhece os três.
3. **Fluxo**: a interface monta a `Requisicao` e a entrega à fachada → a fachada a publica e roda o ciclo → o controle ativa o KS apto → o KS valida, atualiza o Blackboard e publica um `ResultadoOperacao` ou um `Relatorio` → o Blackboard avisa os observers (a janela ou o `Display`, e o `RegistroDeEventos`) → o controle consome a requisição.
4. **Padrões**: Singleton (`Blackboard`), Observer (`Blackboard` → janela, `Display` e `RegistroDeEventos`), Template Method (`KnowledgeSource.run()` e os ganchos de `EspecialistaDeSetor`) e Facade (`FachadaEstacionamento`).
5. **Fora do desenho**: Strategy, State e Command foram descartados deliberadamente, e o controle não é um Mediator.
6. **Regras de negócio só nos KS**: as interfaces conferem apenas o formato (placa, altura, data, vaga existente na matriz).
7. **Registro do uso**: cada evento aceito vira uma linha de `dados/eventos.csv`; ao abrir, o arquivo é relido pela fachada, e os especialistas reconstroem o Blackboard com as mesmas regras.

Os motivos de cada decisão estão na Descrição Arquitetural, em `../descricao-arquitetural/`: o [PDF dos padrões de software](../descricao-arquitetural/padroes-de-software.pdf), o [PDF da descrição do sistema](../descricao-arquitetural/descricao-do-sistema.pdf) e os diagramas.

## Regras do estacionamento

| Setor | Vagas | Preferenciais | Restrição |
|---|---|---|---|
| EXTERNO | 15 (A1–C5) | — | nenhuma |
| SUBSOLO | 10 (A1–B5) | A1 e A2 | altura máxima 2,10 m |

- **Quatro eventos por veículo:** entrar no setor (cancela), ocupar uma vaga, desocupar a vaga e sair do setor (cancela). Entre eles, o veículo está circulando.
- **Entrar:** recusado se a placa já está em algum setor ou se o horário é anterior à visita anterior da mesma placa. No Subsolo, a altura é obrigatória e vai até 2,10 m.
- **Ocupar:** recusado se o veículo não entrou no setor, se já ocupa outra vaga ou se a vaga está ocupada. Vaga preferencial exige um dos seis motivos; "Nenhum dos anteriores" é recusado.
- **Sair:** recusado enquanto o veículo ocupa uma vaga. A janela oferece o atalho "desocupar e sair", que envia as duas requisições, cada uma conferida pelo especialista.
- **Ordem dos eventos:** todo evento com horário anterior ao último evento da mesma passagem é recusado.
- **Relatórios:** ocupação por setor, histórico geral, por placa, por período (pela data/hora de entrada), tempo médio de permanência, uso de vagas preferenciais, horários de pico, clientes frequentes, giro por vaga, tempo até estacionar e o relatório completo, com todas as seções.
- **Durabilidade:** o uso fica gravado em `dados/eventos.csv` e é relido ao abrir. O menu Dados da janela carrega o cenário de demonstração ou zera os dados.

## Demonstração

O roteiro de apresentação de 10 minutos (passos D1 a D13) está na seção 8 do [roteiro de uso](../roteiro-de-uso.md). A conferência manual da janela, com os resultados de cada rodada, está no [roteiro de testes da janela](testes/roteiro-de-testes-da-janela.md).

## Testes automatizados

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src testes ferramentas -name "*.java")
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out SmokeTest
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out TesteDeAceitacao
```

- **`SmokeTest`** (90 verificações): a estrutura (Singleton, `run()` final, `condition()` sem efeito colateral), os quatro eventos e as recusas de cada setor, os onze relatórios, a gravação e a releitura do CSV, o cenário de demonstração, a parte da janela que não depende da tela e a análise do código-fonte (nenhum KS cita outro; o Blackboard não importa especialistas, fachada nem persistência; nenhum `System.out` fora de `ui`).
- **`TesteDeAceitacao`** (23 verificações): executa, pela fachada, o roteiro de demonstração D1 a D13 e uma sessão da interface de texto com a entrada simulada.

Os dois rodam de dentro da pasta `Codigo`, usam um arquivo temporário e não mexem em `dados/eventos.csv`.
