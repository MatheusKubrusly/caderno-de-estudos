# Requisitos do trabalho final

Trabalho final de Arquitetura e Padrões de Software (2026.2, Grupo 4): um sistema inteligente de gerenciamento de estacionamento, construído na arquitetura de memória compartilhada **Blackboard**.

Este arquivo reúne o que o trabalho precisa entregar, para que cada parte possa ser conferida sem depender das conversas em que foi decidida. Ele tem quatro partes:

1. o enunciado;
2. os entregáveis e os requisitos do sistema, cada um com um código curto (E1, R1…) para ser citado depois;
3. as restrições e as decisões tomadas, com o motivo de cada uma;
4. a matriz de rastreabilidade, que diz onde cada item é atendido e como conferir.

Quando um item for atendido ou mudar, a matriz no fim deste arquivo deve ser atualizada.

## 1. Enunciado

Texto passado pelo Matheus em 25/11/2026. As frases de pedido da conversa ("quero que me ajude a garantir que teremos", "que estarei desenvolvendo") foram tiradas, e "todos o tipo" foi corrigido para "todo o tipo"; o resto está como veio:

> Documentação representando a Descrição Arquitetural, sendo esta composta de:
>
> - Documento em formato pdf descrevendo os padrões de software que foram utilizados e porquê. Pode incluir recortes de diagramas de classes.
> - Documento em formato pdf contendo a descrição do sistema (criar o problema e o sistema como solução) e as suas preocupações (concerns). Descrever quais são os principais pontos de vistas e perspectivas que endereçam essas preocupações.
> - Diagrama de implantação, retratando os módulos do sistema;
> - Diagramas de casos de uso, para listar funcionalidades por tipos de atores envolvidos;
> - Diagramas de atividades, para detalhar as funcionalidades e suas interações com atores e módulos;
> - Diagrama de estados, apenas se o sistema possuir alguma máquina de estado.
>
> Além dessa questão de documentação, é preciso garantir que exista um front-end minimamente simples capaz de permitir uma fácil execução e demonstração do uso da arquitetura.
>
> A arquitetura é a "Arquitetura de Memória Compartilhada: Blackboard" e o propósito principal é desenvolver um sistema inteligente de gerenciamento de estacionamento. Para isso, devo criar um sistema para gerenciar as vagas de estacionamento de um shopping center, armazenar no Blackboard dados como quantidade e localização das vagas, vagas ocupadas e disponíveis, histórico de utilização por placa, horários de entrada e saída dos veículos e informações sobre áreas preferenciais, criar dois módulos especialistas representando diferentes setores do estacionamento (por exemplo, setor A e setor B), que devem atualizar o Blackboard sempre que um veículo entrar, sair, ocupar e desocupar uma vaga e, por fim, criar um terceiro módulo especialista para emissão de relatórios. Esse módulo deve analisar as informações disponíveis no Blackboard, histórico, placas, horários e fornecer todo o tipo de informação que possa ser interessante para uma gerência. Além disso, o sistema deve permitir que os módulos especialistas utilizem o Blackboard de forma independente, sem comunicação direta entre eles.

## 2. Entregáveis e requisitos

### Entregáveis (E)

O conjunto de E1 a E6 forma a **Descrição Arquitetural**: os documentos que explicam como o sistema é organizado e por quê.

| Código | O que é pedido |
|---|---|
| E1 | PDF com os padrões de software usados e o motivo de cada um, com recortes do diagrama de classes |
| E2 | PDF com a descrição do problema e do sistema como solução, as preocupações das partes interessadas (*concerns*) e os pontos de vista e perspectivas que tratam essas preocupações |
| E3 | Diagrama de implantação, com os módulos do sistema |
| E4 | Diagramas de casos de uso, com as funcionalidades de cada tipo de ator |
| E5 | Diagramas de atividades, com as funcionalidades e as interações entre atores e módulos |
| E6 | Diagrama de estados, se o sistema tiver máquina de estados. Ele tem três: a vaga (livre ou ocupada), o veículo (circulando ou estacionado) e a requisição no Blackboard |
| E7 | Front-end simples, que facilite executar e demonstrar a arquitetura |
| E8 | Roteiro de uso, que guie a utilização adequada do sistema (pedido do Matheus em 25/11/2026, além do enunciado) |

### Requisitos do sistema (R)

| Código | O que o sistema precisa fazer |
|---|---|
| R1 | Gerenciar as vagas de estacionamento de um shopping center na arquitetura Blackboard |
| R2 | Guardar no Blackboard a quantidade e a localização das vagas, as vagas ocupadas e as livres, o histórico de uso por placa, os horários de entrada e saída e as áreas preferenciais |
| R3 | Ter dois módulos especialistas, um para cada setor do estacionamento (aqui, Externo e Subsolo) |
| R4 | Fazer os especialistas de setor atualizarem o Blackboard em quatro eventos: o veículo entra no setor, ocupa uma vaga, desocupa a vaga e sai do setor |
| R5 | Ter um terceiro especialista, de relatórios, que analisa o histórico, as placas e os horários e entrega informação útil para a gerência |
| R6 | Fazer os especialistas usarem o Blackboard de forma independente, sem comunicação direta entre eles |
| R7 | Garantir que o sistema funcione depois das mudanças no código, sem ficar sujeito a bugs (pedido do Matheus em 25/11/2026, além do enunciado) |

## 3. Restrições e decisões

### Restrições

- **Java puro, só a biblioteca padrão, JDK 17 ou mais novo.** É a escolha do grupo no código original, e permite rodar o sistema em qualquer máquina com Java, sem instalar nada além dele.
- **Repositório público.** Nada de dados pessoais reais: as placas do cenário de demonstração são fictícias, e o arquivo com o uso real (`Codigo/dados/eventos.csv`) fica fora do git.
- **Execução simples.** Um comando (`./run.sh`) compila e abre o sistema.

### Decisões tomadas em 25/11/2026

| Assunto | Decisão | Por quê |
|---|---|---|
| Front-end | Janela desktop em Swing, a biblioteca de janelas que já vem no Java. A interface de texto (CLI) continua disponível com `./run.sh --cli` | Mostra as vagas e cada ciclo do Blackboard na tela, sem navegador e sem biblioteca externa |
| Eventos | Entrar, ocupar, desocupar e sair viram quatro requisições separadas | O enunciado cita os quatro, e a separação cria a máquina de estados do veículo (E6) |
| Saída com o carro ainda na vaga | O especialista recusa e explica; a janela oferece o atalho "desocupar e sair", que envia as duas requisições | Segue o padrão do código: o especialista é rígido, e a tela ajuda a corrigir |
| Código dos setores | Uma classe-mãe, `EspecialistaDeSetor`, guarda os quatro fluxos, e cada setor só preenche as suas regras (altura e vaga preferencial no Subsolo) | Evita cerca de 150 linhas repetidas, sem que um especialista chame o outro |
| Ponto de entrada | Uma fachada, `FachadaEstacionamento`, monta e publica as requisições para a janela, a CLI e a releitura do registro | O mesmo código não fica repetido em três lugares, e as telas não precisam conhecer o controlador |
| Relatórios novos | Horários de pico, clientes frequentes, giro por vaga e tempo até estacionar, além dos sete que já existiam | Atendem ao "todo tipo de informação interessante para a gerência" de R5 |
| Registro do uso | Um arquivo CSV com cada evento aceito, relido ao abrir o sistema; um banco de dados exigiria biblioteca externa | O histórico sobrevive ao fechamento do programa sem quebrar a restrição do Java puro |
| Cenário de demonstração | Um CSV pronto com uma semana de uso fictício (16 a 22/11/2026), carregado por um botão | Numa apresentação ao vivo não há tempo para juntar dados que deixem os relatórios interessantes |
| Documentos | LaTeX (classe `article`), compilado na máquina com o `latexmk` | Mesmo estilo do trabalho de Metodologia Científica, com a revisão pelo diff |
| Diagramas | PlantUML, que descreve o UML em texto, com o Graphviz para o posicionamento | O texto do diagrama fica no repositório e também pode ser revisado pelo diff |
| Código no repositório | Extraído do `codigo_atualizado.zip` para `Codigo/`, sem as configurações do IntelliJ e os compilados; o zip original também fica guardado | O desenvolvimento passa a ser feito aqui, e o zip registra o que o grupo enviou |
| Regras a mais nos setores | Recusar uma entrada com horário anterior à visita anterior da mesma placa, e qualquer evento sem placa | A primeira evita visitas sobrepostas ao reler o CSV; a segunda evita um travamento por placa vazia |
| Relatório de preferenciais | Conta cada ocupação de vaga preferencial, com o tempo na vaga | Uma visita pode passar por mais de uma vaga, e é o uso da vaga que a gerência fiscaliza |
| Base legal das preferenciais | Sai da tela de motivos e vai para a perspectiva de regulação do PDF da descrição do sistema | A tela citava só a Lei 10.048/2000, que trata de atendimento prioritário; o PDF tem espaço para as quatro leis |
| Cálculos dos relatórios | Ficam na classe de apoio `Estatisticas`; o especialista de relatórios lê o Blackboard e monta o texto | O especialista passava de 600 linhas; a divisão não usa Strategy |
| Roteiro de uso | Arquivo `roteiro-de-uso.md` nesta pasta, com link no README do código | Guia quem vai rodar e apresentar o sistema, passo a passo |
| Garantia de funcionamento | Teste automático ampliado, teste de aceitação do roteiro, checklist manual da janela e revisão de código no fim | Cada mudança é conferida automaticamente, e o que não dá para automatizar (o clique na janela) tem lista de conferência |

## 4. Matriz de rastreabilidade

Situação: **pendente** (ainda não feito), **parcial** (existe, mas falta algo) ou **atendido**.

| Código | Onde é atendido | Como conferir | Situação |
|---|---|---|---|
| E1 | `descricao-arquitetural/padroes-de-software.pdf` | Ler o PDF: Blackboard, Singleton, Observer, Template Method e Facade, cada um com o motivo, e os padrões descartados | pendente |
| E2 | `descricao-arquitetural/descricao-do-sistema.pdf` | Ler o PDF: problema, solução, preocupações, pontos de vista e perspectivas | pendente |
| E3 | `descricao-arquitetural/diagramas/implantacao.pdf` | Abrir o diagrama: os módulos aparecem dentro da JVM | pendente |
| E4 | `descricao-arquitetural/diagramas/casos-de-uso-*.pdf` | Abrir os diagramas: operador, sensor, administrador e gerente com as suas funcionalidades | pendente |
| E5 | `descricao-arquitetural/diagramas/atividade-*.pdf` | Abrir os diagramas: raias para os atores e os módulos | pendente |
| E6 | `descricao-arquitetural/diagramas/estados-*.pdf` | Abrir os diagramas: vaga, veículo e requisição | pendente |
| E7 | `Codigo/src/ui/` (a CLI) e `Codigo/src/ui/janela/` (a janela) | Rodar `./run.sh` e seguir o `roteiro-de-uso.md` | parcial (a CLI está pronta; a janela vem na etapa 5) |
| E8 | `roteiro-de-uso.md` | Seguir o roteiro do começo ao fim; o teste de aceitação executa o roteiro de demonstração | pendente |
| R1 | `Codigo/src/blackboard/` | Ler o código e rodar o teste de fumaça (`SmokeTest`) | atendido |
| R2 | `Codigo/src/blackboard/Blackboard.java` e `Codigo/src/model/` (vagas, estadias, ocupações e vagas preferenciais) | Teste de fumaça e relatórios de ocupação e histórico | atendido |
| R3 | `Codigo/src/knowledge_sources/ExternoKS.java` e `SubsoloKS.java` | Teste de fumaça, cenários de setor | atendido |
| R4 | `Codigo/src/knowledge_sources/EspecialistaDeSetor.java` | Teste de fumaça com os quatro eventos | atendido |
| R5 | `Codigo/src/knowledge_sources/RelatorioKS.java` e `Estatisticas.java` (11 relatórios) | Teste de fumaça, um caso por relatório | atendido |
| R6 | Teste de arquitetura do `SmokeTest` | O teste confere que nenhum especialista referencia outro | atendido |
| R7 | `Codigo/testes/` (teste de fumaça e teste de aceitação), checklist da janela e revisão de código | O teste de fumaça (78 verificações hoje) e o de aceitação sem falhas; o checklist percorrido; a revisão sem pendências | parcial (o teste de fumaça cresce a cada etapa) |
