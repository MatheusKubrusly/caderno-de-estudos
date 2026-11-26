# Roteiro de uso do sistema de vagas

Este roteiro guia quem vai instalar, usar e apresentar o sistema de gerenciamento das vagas do estacionamento do shopping. Ele cobre a instalação, cada operação da janela e da interface de texto, o significado de cada recusa, os relatórios, os dados de demonstração, um roteiro de apresentação de 10 minutos e a solução dos problemas mais comuns.

O sistema registra quatro eventos para cada veículo: a **entrada** na cancela do setor, a **ocupação** de uma vaga, a **desocupação** da vaga e a **saída** pela cancela. Entre a entrada e a ocupação, e entre a desocupação e a saída, o veículo está **circulando** no setor, procurando vaga ou indo embora.

| Setor | Vagas | Regras |
|---|---|---|
| Externo | 15 vagas comuns, de A1 a C5 | nenhuma regra extra |
| Subsolo | 10 vagas, de A1 a B5 | altura máxima de 2,10 m na entrada; A1 e A2 são preferenciais e exigem um motivo de uso |

## 1. Antes de começar

O sistema precisa só do **Java (JDK) 17 ou mais novo**. Para conferir se ele está instalado, abra um terminal e digite:

```
java -version
javac -version
```

As duas linhas devem mostrar a versão 17 ou maior. Se aparecer "comando não encontrado", instale o JDK (por exemplo, o [Eclipse Temurin](https://adoptium.net/)) e abra um terminal novo.

## 2. Abrir o sistema

No Linux ou no macOS, dentro da pasta `Codigo`:

| Comando | O que abre |
|---|---|
| `./run.sh` | a janela, com os mapas das vagas |
| `./run.sh --cli` | a interface de texto, no próprio terminal |

O `run.sh` compila o código e abre o sistema; o terminal fica ocupado enquanto o sistema estiver aberto. Num computador sem tela gráfica (um servidor acessado por SSH, por exemplo), o sistema abre a interface de texto sozinho e avisa.

No Windows, o `run.sh` não funciona no Prompt de Comando. Dentro da pasta `Codigo`, rode:

```
chcp 65001
mkdir out
dir /s /b src\*.java > fontes.txt
javac -encoding UTF-8 -d out @fontes.txt
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out Main
```

O `chcp 65001` faz o terminal mostrar os acentos; o `dir` lista os arquivos do código num arquivo, que o `javac` lê com o `@`. Para a interface de texto, acrescente ` --cli` no fim da última linha.

## 3. A janela

A janela tem quatro áreas:

- **À esquerda, os mapas dos dois setores.** Cada vaga é um botão: verde para livre, azul com contorno e a letra "P" para preferencial livre, vermelho para ocupada (com a placa de quem ocupa). Abaixo de cada mapa, o resumo de vagas livres e a lista "Circulando no setor", com há quanto tempo cada veículo está circulando.
- **Embaixo dos mapas, a CANCELA:** o setor, a placa, a altura (só no Subsolo) e os botões **Registrar entrada** e **Registrar saída**.
- **À direita, duas abas.** A **Ciclo do Blackboard** mostra cada requisição, o especialista que respondeu (`ExternoKS`, `SubsoloKS`, `RelatorioKS` ou o `Controlador`) e o resultado, com ✓ para aceito e ⚠ para recusado. A **Relatórios** gera os relatórios da gerência.
- **No rodapé, a barra de status**, com o resultado da última operação, em verde ou em vermelho.

Os menus ficam no alto: **Dados** (carregar o cenário de demonstração e zerar os dados) e **Ajuda** (um resumo destes passos).

### 3.1 Registrar a entrada

1. Na CANCELA, escolha o setor e digite a placa (de 5 a 8 letras, números ou hífen, como `ABC1D23`).
2. No Subsolo, digite também a altura do veículo em metros (`1,85` ou `1.85`).
3. Clique em **Registrar entrada**.

Se aceita, os campos se esvaziam e a placa aparece em "Circulando no setor". Se recusada, a placa e a altura continuam nos campos, para corrigir.

### 3.2 Ocupar uma vaga

1. Clique numa vaga livre do mapa.
2. Na caixa "Ocupar a vaga…", escolha a placa entre os veículos que estão circulando naquele setor.
3. Numa vaga preferencial, escolha também o motivo de uso: pessoa com deficiência (PCD), pessoa com transtorno do espectro autista (TEA), idoso (60 anos ou mais), gestante, lactante ou pessoa com criança de colo. A opção "Nenhum dos anteriores" é recusada.
4. Clique em **OK**.

A vaga fica vermelha, e a placa sai da lista de circulando. No sistema real, quem faria isso seria o sensor da vaga; no protótipo, o operador faz o papel dele.

### 3.3 Desocupar uma vaga

Clique numa vaga ocupada e responda **Sim** à pergunta "Desocupar a vaga…?". A vaga volta a ficar livre, e o veículo volta para a lista de circulando.

### 3.4 Registrar a saída

Escolha o setor, digite a placa e clique em **Registrar saída**. A barra de status mostra o tempo de permanência.

Se o veículo ainda estiver numa vaga, a saída é recusada, e a janela pergunta "Desocupar … e registrar a saída?". Com **Sim**, ela envia as duas operações, uma depois da outra, e cada uma é conferida pelo especialista do setor. Com **Não**, nada muda.

## 4. O que significa cada recusa

Toda recusa vem de um especialista e explica o que fazer. Nada muda no estacionamento quando uma operação é recusada.

| Mensagem (início) | Por quê | O que fazer |
|---|---|---|
| "Veículo não permitido no Subsolo: … excede o máximo de 2,10 m" | O Subsolo tem teto baixo | Encaminhar o veículo para o Externo |
| "Altura do veículo é obrigatória no Subsolo" | A cancela do Subsolo confere a altura | Digitar a altura |
| "O veículo … já está no setor …" | A placa entrou e ainda não saiu | Registrar a saída antes de uma nova entrada |
| "O veículo … não entrou no setor …" | Ninguém pode estacionar sem passar pela cancela | Registrar a entrada antes |
| "O veículo … já ocupa a vaga …" | Um veículo ocupa uma vaga por vez | Desocupar a vaga atual antes |
| "Vaga … está ocupada. Escolha outra." | A vaga já tem um veículo | Escolher outra vaga |
| "Vaga … é preferencial e exige um motivo válido" | Foi escolhido "Nenhum dos anteriores" | Escolher outra vaga ou informar o motivo |
| "Vaga … já está desocupada." | A vaga já estava livre | Nada a fazer |
| "O veículo … ainda ocupa a vaga …" | A saída veio antes da desocupação | Aceitar o atalho "desocupar e sair" |
| "O veículo … não está no setor …" | A placa não entrou neste setor | Conferir a placa e o setor |
| "Horário do evento (…) é anterior ao último evento do veículo…" | O horário é anterior ao último evento do veículo | Acontece só com arquivos editados à mão; conferir o horário |
| "Informe a placa para o histórico por placa." | O relatório precisa de uma placa | Digitar a placa |
| "Período inválido: o início é posterior ao fim." | As datas do período estão trocadas | Corrigir as datas |

A caixa **"Formato inválido"** é diferente: ela aparece antes de a operação chegar aos especialistas, quando o texto digitado não tem o formato esperado (uma placa com menos de 5 caracteres, uma altura que não é número ou uma data que não existe, como `31/02/2026`). Por isso ela não aparece no Ciclo do Blackboard.

## 5. Relatórios

Na aba **Relatórios**, escolha o relatório e clique em **Gerar**.

| Relatório | O que mostra |
|---|---|
| Ocupação por setor | Vagas livres e ocupadas de cada setor e os veículos presentes agora |
| Histórico geral | Todas as passagens, com entrada, saída, vagas e motivo |
| Histórico por placa | As passagens de uma placa (pede a placa) |
| Histórico por período | As passagens que entraram entre duas datas (pede o início e o fim, no formato `dd/MM/aaaa HH:mm`) |
| Tempo médio de permanência | A média geral e por setor |
| Uso de vagas preferenciais | Cada ocupação de vaga preferencial, com o motivo e o tempo na vaga |
| Horários de pico | Entradas por hora e por dia da semana, em barras, e a maior lotação |
| Clientes frequentes | As placas com mais visitas, com a permanência média e a última visita |
| Giro por vaga | Para cada vaga, quantas vezes foi ocupada e quanto do período ficou ocupada |
| Tempo até estacionar | Quanto tempo os veículos circulam entre a entrada e a vaga, e quem está circulando agora |
| Relatório completo | Todas as seções acima num só documento |

Com poucos dados, os relatórios ficam vazios ou curtos. Para vê-los com uma semana de movimento, carregue o cenário de demonstração (seção 6).

## 6. Os dados

- **Onde fica o uso registrado:** cada operação aceita vira uma linha do arquivo `Codigo/dados/eventos.csv`. Ao abrir, o sistema relê esse arquivo, e o Ciclo do Blackboard mostra uma nota como "Registro de uso (dados/eventos.csv): 2204 eventos relidos.". O arquivo fica fora do git, porque guardaria placas reais.
- **Carregar o cenário de demonstração** (menu Dados): substitui o uso registrado por uma semana simulada, de 16 a 22/11/2026, com 2204 eventos e placas fictícias. O sistema pede confirmação.
- **Zerar os dados** (menu Dados): apaga o uso registrado e deixa todas as vagas livres. O sistema pede confirmação.
- **Cópia de segurança:** copie o `eventos.csv` para outro lugar. Para restaurar, devolva o arquivo à pasta `dados` e abra o sistema.

## 7. A interface de texto

Com `./run.sh --cli`, o sistema mostra este menu:

```
1. Entrar no setor (cancela)
2. Ocupar uma vaga
3. Desocupar uma vaga
4. Sair do setor (cancela)
5. Relatórios
0. Encerrar o sistema
```

- Em toda pergunta, digite o número da opção e tecle Enter. **`0` volta** para a pergunta anterior.
- **Entrar:** escolha o setor (1 Externo, 2 Subsolo), a altura (só no Subsolo) e a placa. Se a altura for recusada, o sistema pede só a altura de novo; se a placa já estiver dentro, pede só a placa.
- **Ocupar:** escolha o setor, o veículo na lista de circulando e a vaga pelo código da matriz (`A1`, `B3`…). Numa vaga preferencial, escolha o motivo de 1 a 6; a opção 7, "Nenhum dos anteriores", é recusada, e o sistema pede outra vaga.
- **Desocupar:** escolha o setor e a vaga.
- **Sair:** escolha o setor e digite a placa. Na interface de texto não há o atalho da janela: desocupe a vaga antes.
- **Relatórios:** escolha de 1 a 11, na mesma ordem da tabela da seção 5.

A interface de texto não tem as opções de carregar o cenário e de zerar os dados; use a janela para isso. As duas interfaces usam o mesmo arquivo de uso.

## 8. Roteiro de demonstração (10 minutos)

Este roteiro mostra, em ordem, as regras de cada setor, a independência dos especialistas, o atalho da janela, a gravação do uso e os relatórios. O teste de aceitação (seção 10) executa estes mesmos passos e confere os resultados.

| Passo | Ação na janela | O que mostrar |
|---|---|---|
| D1 | Menu Dados → Zerar dados… → OK | Os dois mapas livres e a nota "Dados zerados" |
| D2 | Subsolo, placa `DEM1A01`, altura `2,30`, Registrar entrada | Recusa do `SubsoloKS`: "excede o máximo de 2,10 m" |
| D3 | Troque a altura para `1,85` e registre de novo | Aceita; `DEM1A01` aparece em "Circulando no setor" |
| D4 | Externo, placa `DEM1A01`, Registrar entrada | Recusa do **`ExternoKS`**: "já está no setor Subsolo". O especialista do Externo soube disso lendo o quadro, sem falar com o do Subsolo |
| D5 | Clique na A1 do Subsolo, motivo "Nenhum dos anteriores", OK | Recusa: "exige um motivo válido"; a A1 continua azul |
| D6 | Clique de novo na A1, motivo "Idoso (60 anos ou mais)", OK | A A1 fica vermelha com `DEM1A01` |
| D7 | Externo, placa `DEM2B02`, Registrar entrada; clique na B3 e escolha `DEM2B02` | A B3 fica vermelha |
| D8 | Subsolo, placa `DEM1A01`, Registrar saída; responda **Sim** | A saída é recusada, a janela oferece o atalho, e o Ciclo mostra duas linhas aceitas: `DESOCUPAR` e `SAIR` |
| D9 | Clique na B3 → Sim; depois Externo, placa `DEM2B02`, Registrar saída | A B3 volta a ficar verde, e a saída é aceita com o tempo de permanência |
| D10 | Relatórios → Histórico por placa → `DEM1A01` → Gerar | Uma passagem no Subsolo, na vaga A1, com o motivo IDOSO |
| D11 | Menu Dados → Carregar cenário de demonstração… → OK | A nota "Cenário de demonstração carregado: 2204 eventos relidos." |
| D12 | Relatórios → Horários de pico, Clientes frequentes, Giro por vaga e Relatório completo | Pico às 13h, sábado como o dia mais cheio e "Maior lotação: 22 de 25 vagas (88,0%)"; 386 placas distintas, 33 que voltaram e `UTN8X74` em primeiro, com 10 visitas |
| D13 | Feche a janela e rode `./run.sh` de novo | A nota "Registro de uso (dados/eventos.csv): 2204 eventos relidos.": o uso sobreviveu ao fechamento |

Ao apresentar, vale apontar para a aba Ciclo do Blackboard a cada passo: ela mostra qual especialista respondeu a cada requisição, que é o funcionamento da arquitetura.

## 9. Solução de problemas

| Problema | Causa provável | Solução |
|---|---|---|
| "java: comando não encontrado" | O JDK não está instalado ou não está no PATH | Instalar o JDK 17 ou mais novo e abrir um terminal novo |
| "./run.sh: Permissão negada" | O arquivo perdeu a permissão de execução (por exemplo, ao extrair um zip) | Rodar `chmod +x run.sh` uma vez, ou usar `bash run.sh` |
| A janela não abre, e aparece a interface de texto | O computador não tem tela gráfica | Usar a interface de texto ou rodar num computador com tela |
| Acentos trocados por símbolos no Windows | O terminal não está em UTF-8 | Rodar `chcp 65001` antes de abrir o sistema |
| Aviso "Não foi possível gravar em dados/eventos.csv" | A pasta não tem permissão de escrita | Conferir a permissão da pasta `Codigo/dados`; a operação continua valendo até o sistema fechar |
| Nota "… linhas puladas" ao abrir | O `eventos.csv` foi editado à mão e tem linhas inválidas | A nota mostra a primeira linha com problema; corrigir ou apagar essa linha |
| Relatórios vazios | Ainda não há uso registrado | Carregar o cenário de demonstração |

## 10. Conferir que o sistema funciona

Dentro da pasta `Codigo`, compile tudo e rode os dois testes automáticos:

```
mkdir -p out
javac -encoding UTF-8 -d out $(find src testes ferramentas -name "*.java")
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out SmokeTest
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out TesteDeAceitacao
```

- O **`SmokeTest`** confere a estrutura, as regras de cada evento, os relatórios, a gravação e a releitura do arquivo, a parte da janela que não depende da tela e as regras de arquitetura (por exemplo, que nenhum especialista cita outro).
- O **`TesteDeAceitacao`** executa o roteiro de demonstração da seção 8 e uma sessão da interface de texto com a entrada simulada.

Cada teste termina com uma linha "Resultado: … ok, 0 falha(s)". Os dois usam um arquivo temporário e não mexem no `dados/eventos.csv`. A parte da janela que depende de cliques é conferida à mão pelo [roteiro de testes da janela](Codigo/testes/roteiro-de-testes-da-janela.md).
