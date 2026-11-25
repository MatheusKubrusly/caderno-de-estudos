# Roteiro de testes da janela

Este roteiro confere, clicando, a parte da janela que o teste automático (`SmokeTest`) não alcança: as caixas de diálogo, o atalho "desocupar e sair", o menu Dados, os relatórios e a releitura do registro ao reabrir o sistema. As regras de negócio em si (altura, preferenciais, ordem dos eventos) já são conferidas pelo teste automático; aqui se confere que a janela as mostra direito.

Cada teste tem um código (J01, J02…), os passos e o resultado esperado. A tabela do fim registra o resultado de cada rodada.

## Antes de começar

1. No terminal do VS Code, abra o sistema:

   ```
   cd ~/Repositories/caderno-de-estudos/faculdade/arquitetura-e-padroes-de-software/atividades_para_entregar/trabalho_final/Codigo
   ./run.sh
   ```

   O `run.sh` compila o código e abre a janela. O terminal fica ocupado enquanto a janela estiver aberta.

2. Para começar do zero, use o menu **Dados → Zerar dados…** e confirme com **OK**. Isso apaga o uso registrado em `dados/eventos.csv`, que o git ignora.

### Como gravar

- Grave **só a janela do sistema**, para notificações e outras janelas não entrarem no vídeo.
  - No OBS: uma fonte do tipo "Captura de janela", apontando para a janela "Estacionamento do shopping".
  - No terminal: `wf-recorder -g "$(slurp)" -f ~/Vídeos/testes-janela/J01.mp4`. O `slurp` deixa selecionar a área da janela com o mouse; **Ctrl+C** encerra a gravação. Crie a pasta antes com `mkdir -p ~/Vídeos/testes-janela`.
- O mais fácil de conferir é **um vídeo por teste**, com o código no nome (`J01.mp4`, `J02.mp4`…). Um vídeo só também serve, desde que siga a ordem do roteiro.
- Ao terminar cada passo, **espere uns 3 segundos parado**, para o resultado aparecer nos quadros do vídeo.
- Os vídeos ficam fora do repositório.

## Testes

### J01. Janela vazia

**Passos:** depois de zerar os dados, olhe a janela.

**Esperado:**
- À esquerda, os mapas "SETOR EXTERNO" (15 vagas, de A1 a C5) e "SETOR SUBSOLO (coberto)" (10 vagas, de A1 a B5), todas livres: verdes, com a A1 e a A2 do Subsolo em azul, contorno azul e "P" de preferencial.
- Os resumos dizem "Livres: 15 de 15" e "Livres: 10 de 10 (comuns 8/8, preferenciais 2/2)", e os dois setores dizem "Circulando no setor: nenhum veículo".
- À direita, a aba "Ciclo do Blackboard", com a nota "Dados zerados".

### J02. Altura acima do máximo no Subsolo

**Passos:** em CANCELA, escolha o setor **Subsolo**, digite a placa `TST1A11` e a altura `2,30` e clique em **Registrar entrada**.

**Esperado:**
- Barra de baixo, em vermelho: "⚠ Veículo não permitido no Subsolo: 2,30 m excede o máximo de 2,10 m."
- No ciclo, a linha `ENTRAR SUBSOLO TST1A11 → SubsoloKS ⚠ …`.
- A placa e a altura continuam nos campos.

### J03. Entrada no Subsolo

**Passos:** troque a altura para `1,85` e clique em **Registrar entrada**.

**Esperado:**
- Barra de baixo, em verde: "✓ Veículo TST1A11 entrou no setor Subsolo — (hora). Está circulando."
- No ciclo, `ENTRAR SUBSOLO TST1A11 → SubsoloKS ✓ …`.
- Os campos ficam vazios, e o Subsolo passa a mostrar "Circulando no setor: TST1A11 (há menos de 1min)".

### J04. Placa com formato inválido

**Passos:** escolha o setor **Externo**, digite a placa `AB` e clique em **Registrar entrada**.

**Esperado:**
- Abre a caixa "Formato inválido", dizendo que a placa deve ter de 5 a 8 letras ou números.
- Nada aparece no ciclo: a requisição nem chega ao Blackboard, porque formato é conferido pela janela, e não pelos especialistas.

### J05. Placa que já está no estacionamento

**Passos:** com o setor **Externo**, digite a placa `TST1A11` e clique em **Registrar entrada**.

**Esperado:**
- Barra de baixo: "⚠ O veículo TST1A11 já está no setor Subsolo. Registre a saída antes de uma nova entrada."
- No ciclo, a linha da recusa vem de `ExternoKS`: o especialista do Externo leu no Blackboard que a placa está no Subsolo, sem falar com o especialista do Subsolo.

### J06. Vaga preferencial sem motivo

**Passos:** no mapa do Subsolo, clique na vaga **A1**. Na caixa "Ocupar a vaga A1", deixe o veículo `TST1A11`, escolha o motivo **Nenhum dos anteriores** e clique em **OK**.

**Esperado:**
- Barra de baixo: "⚠ Vaga A1 é preferencial e exige um motivo válido. Escolha outra vaga."
- A A1 continua azul, e o TST1A11 continua circulando.

### J07. Vaga preferencial com motivo

**Passos:** clique de novo na **A1**, escolha o motivo **Idoso (60 anos ou mais)** e clique em **OK**.

**Esperado:**
- Barra de baixo: "✓ Veículo TST1A11 estacionado em A1 (Preferencial - Idoso) — (hora)".
- A A1 fica vermelha, com "TST1A11" escrito nela, e o Subsolo volta a "Circulando no setor: nenhum veículo".

### J08. Vaga livre sem ninguém circulando

**Passos:** no mapa do Externo, clique na vaga **B3**.

**Esperado:** abre a caixa "Ocupar B3", avisando que nenhum veículo está circulando no setor Externo e que é preciso registrar uma entrada antes. Nada muda nos mapas.

### J09. Saída com o carro na vaga, sem usar o atalho

**Passos:** escolha o setor **Subsolo**, digite a placa `TST1A11`, clique em **Registrar saída** e, na pergunta "Desocupar A1 e registrar a saída?", responda **Não**.

**Esperado:**
- Barra de baixo: "⚠ O veículo TST1A11 ainda ocupa a vaga A1. Registre a desocupação antes da saída."
- Os botões da pergunta aparecem em português: **Sim** e **Não**.
- Depois do "Não", nada muda: a A1 continua vermelha.

### J10. Saída com o atalho "desocupar e sair"

**Passos:** clique em **Registrar saída** de novo e, na pergunta, responda **Sim**.

**Esperado:**
- No ciclo, duas linhas novas, as duas de `SubsoloKS` e com ✓: `DESOCUPAR SUBSOLO A1` ("voltou a circular") e `SAIR SUBSOLO TST1A11` ("saiu do setor", com o tempo de permanência).
- A A1 volta a ficar azul, e o Subsolo mostra "Circulando no setor: nenhum veículo".

### J11. Desocupar pelo clique na vaga

**Passos:**
1. Com o setor **Externo**, entre com a placa `ABC1D23`.
2. Clique na vaga **B3**, escolha `ABC1D23` e clique em **OK**.
3. Clique de novo na **B3** e, na pergunta "Desocupar a vaga B3 (ABC1D23)?", responda **Sim**.
4. Registre a saída de `ABC1D23` no Externo.

**Esperado:** a B3 fica vermelha no passo 2 e verde de novo no passo 3, quando o ABC1D23 volta a aparecer em "Circulando no setor" do Externo. O passo 4 é aceito, sem a pergunta do atalho, porque o carro já não está na vaga.

### J12. Histórico por placa

**Passos:** na aba **Relatórios**, escolha **Histórico por placa** e clique em **Gerar** com o campo da placa **vazio**. Depois digite `TST1A11` e clique em **Gerar** de novo.

**Esperado:**
- Na primeira vez, barra de baixo: "⚠ Informe a placa para o histórico por placa."
- Na segunda, o relatório "HISTÓRICO DA PLACA TST1A11", com uma estadia no Subsolo, a vaga A1 e o motivo IDOSO.

### J13. Carregar o cenário de demonstração

**Passos:** menu **Dados → Carregar cenário de demonstração…**, e confirme com **OK**.

**Esperado:**
- No ciclo, a nota "Cenário de demonstração carregado: 2204 eventos relidos."
- Os mapas ficam todos livres, porque o cenário termina com todos os carros fora.

### J14. Relatórios com o cenário

**Passos:** na aba **Relatórios**, gere, um de cada vez: **Horários de pico**, **Clientes frequentes**, **Giro por vaga**, **Tempo até estacionar** e **Relatório completo**.

**Esperado:**
- Horários de pico: barras de 10h a 21h, com o pico às 13h, o sábado como o dia mais cheio e a linha "Maior lotação: 22 de 25 vagas (88,0%) em 22/11 19:26".
- Clientes frequentes: "Placas distintas: 386 · Voltaram (2 visitas ou mais): 33", com a placa `UTN8X74` em primeiro (10 visitas).
- Giro por vaga: o período de 16/11 a 22/11 e uma linha por vaga dos dois setores, com a mais disputada e a mais ociosa no fim.
- Tempo até estacionar: a média e o máximo no geral e por setor, e "Circulando agora: 0".
- Relatório completo: as oito seções, uma depois da outra.

### J15. Histórico por período

**Passos:** escolha **Histórico por período**. Os campos já vêm com `20/11/2026 18:00` e `20/11/2026 22:00`; clique em **Gerar**. Depois troque o início por `31/02/2026 10:00` e clique em **Gerar**.

**Esperado:**
- Na primeira vez, o relatório "HISTÓRICO DE 20/11/2026 18:00 A 20/11/2026 22:00", só com entradas nesse intervalo.
- Na segunda, a caixa "Formato inválido", porque 31/02 não existe.

### J16. Reabrir o sistema

**Passos:** feche a janela e rode `./run.sh` de novo no terminal.

**Esperado:** no ciclo, logo ao abrir, a nota "Registro de uso (dados/eventos.csv): 2204 eventos relidos." Os relatórios mostram os mesmos números do J14.

### J17. Zerar os dados

**Passos:** menu **Dados → Zerar dados…**, confirme com **OK**, feche a janela e abra de novo.

**Esperado:** depois de zerar, a nota "Dados zerados" e os mapas livres. Ao reabrir, nenhuma nota de "Registro de uso", porque o registro ficou só com o cabeçalho.

### J18. Ajuda

**Passos:** menu **Ajuda → Como usar**.

**Esperado:** uma caixa com os sete passos de uso e a indicação do `roteiro-de-uso.md`.

## Resultados

Cada linha é um teste numa rodada. Numa rodada nova, acrescente as linhas dos testes refeitos, com a nova data. Em "Passou", use **sim**, **não**, **parcial** ou **não executado**; em "Observação", descreva o que apareceu de diferente.

As duas rodadas de 25/11/2026 foram gravadas em vídeo pelo Matheus e conferidas pelo Claude nos quadros do vídeo. A segunda refez só os testes que ficaram em aberto na primeira.

| Teste | Data | Passou | Observação |
|---|---|---|---|
| J01 | 25/11/2026 | sim | Janela aberta com a nota "Dados zerados" |
| J02 | 25/11/2026 | sim |  |
| J03 | 25/11/2026 | sim |  |
| J04 | 25/11/2026 | sim | Feito com o setor Subsolo; a caixa "Formato inválido" apareceu, e nada entrou no ciclo |
| J05 | 25/11/2026 | sim | A recusa veio de `ExternoKS` |
| J06 | 25/11/2026 | sim |  |
| J07 | 25/11/2026 | sim | Feito na vaga A2, também preferencial |
| J08 | 25/11/2026 | sim |  |
| J09 | 25/11/2026 | não executado | A pergunta do atalho foi respondida com "Sim" logo na primeira vez |
| J10 | 25/11/2026 | sim | `DESOCUPAR` e `SAIR` com ✓, os dois de `SubsoloKS` |
| J11 | 25/11/2026 | sim | Feito com a placa `ABCD123` |
| J12 | 25/11/2026 | sim | O histórico mostrou a vaga A2, de acordo com o J07 |
| J13 | 25/11/2026 | sim |  |
| J14 | 25/11/2026 | sim |  |
| J15 | 25/11/2026 | não | Com "Histórico por período", o botão Gerar ficava fora da área visível numa tela de 1366 px. Corrigido: os filtros passaram a ter duas linhas, e uma conferência de layout no teste automático impede a volta do defeito. Refazer |
| J16 | 25/11/2026 | sim |  |
| J17 | 25/11/2026 | parcial | O "zerar" funcionou; faltou fechar e reabrir |
| J18 | 25/11/2026 | sim |  |
| J09 | 25/11/2026 (2ª rodada) | sim | Feito na vaga A1, com o motivo PCD; depois do "Não", a A1 continuou vermelha. Achado: os botões da pergunta apareciam em inglês ("Yes" e "No"); corrigido para português, com um teste automático |
| J15 | 25/11/2026 (2ª rodada) | parcial | O botão Gerar apareceu na linha de cima, e o relatório do período foi gerado. A data `31/02/2026 10:00` não foi testada na janela (o teste automático do formato já a recusa) |
| J17 | 25/11/2026 (2ª rodada) | sim | Ao reabrir depois de zerar, nenhuma nota de "Registro de uso" |
