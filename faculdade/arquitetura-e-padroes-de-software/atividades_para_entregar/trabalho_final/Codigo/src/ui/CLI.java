package ui;

import blackboard.Blackboard;
import blackboard.BlackboardController;
import model.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Scanner;

/**
 * Interface de linha de comando. Apenas coleta dados, valida o FORMATO,
 * monta a Requisicao progressivamente e a publica já completa.
 * Nenhuma regra de negócio (altura, ocupação, motivo) é decidida aqui.
 */
public class CLI {

    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);

    /** Fim da entrada padrão (Ctrl+D / pipe encerrado). */
    private static class FimDeEntrada extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private final Blackboard blackboard;
    private final BlackboardController controller;
    private final Display display;
    private final Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);

    public CLI(Blackboard blackboard, BlackboardController controller, Display display) {
        this.blackboard = blackboard;
        this.controller = controller;
        this.display = display;
    }

    public void executar() {
        try {
            boolean ativo = true;
            while (ativo) {
                display.renderizarMenu();
                int opcao = lerInteiro("Opção: ", 0, 3);
                switch (opcao) {
                    case 1 -> fluxoEntrada();
                    case 2 -> fluxoSaida();
                    case 3 -> fluxoRelatorio();
                    default -> ativo = false;
                }
            }
        } catch (FimDeEntrada e) {
            // saída normal por fim de entrada
        }
        display.renderizarEncerramento();
    }

    // ------------------------------------------------------------------ fluxos

    /**
     * Mantém o operador na mesma matriz em caso de rejeição: apenas pede outra vaga,
     * preservando setor, placa e altura já validada. `0` na vaga volta à seleção de setor
     * (para trocar placa/altura); `0` no setor volta ao menu principal.
     */
    private void fluxoEntrada() {
        while (true) {
            Setor setor = escolherSetor();
            if (setor == null) return;

            // Buffer de entrada: existe só até a publicação da Requisicao.
            // Não é estado de domínio — o Blackboard ainda não foi tocado.
            Double alturaValidada = null;
            if (setor == Setor.SUBSOLO) {
                alturaValidada = lerAlturaValidada();
                if (alturaValidada == null) continue;  // 0 na altura: volta ao setor
            }
            String placaInformada = lerPlaca();

            if (escolherVagaEEstacionar(setor, placaInformada, alturaValidada)) return;
            // 0 na vaga: volta ao setor para revisar placa/altura
        }
    }

    /** Repete apenas a escolha de vaga até o KS aceitar ou o operador desistir. */
    private boolean escolherVagaEEstacionar(Setor setor, String placaInformada, Double alturaValidada) {
        while (true) {
            display.renderizarMatriz(setor);
            String idVaga = lerVaga(setor, "Escolha uma vaga (ex.: A1, 0 para voltar): ");
            if (idVaga == null) return false;

            Requisicao req = new Requisicao()
                    .setTipo(TipoRequisicao.ENTRADA)
                    .setSetor(setor)
                    .setPlaca(placaInformada)
                    .setAltura(alturaValidada)
                    .setIdVaga(idVaga);

            Vaga vaga = blackboard.getVaga(setor, idVaga);
            if (vaga.isPreferencial() && vaga.isLivre()) {
                req.setMotivo(lerMotivo());
            }

            if (publicarEExecutar(req)) {
                display.renderizarMatriz(setor);
                return true;
            }
            // Rejeitado: o Display já mostrou o motivo; repete a matriz para outra vaga.
        }
    }

    private void fluxoSaida() {
        while (true) {
            Setor setor = escolherSetor();
            if (setor == null) return;
            if (escolherVagaELiberar(setor)) return;
        }
    }

    private boolean escolherVagaELiberar(Setor setor) {
        while (true) {
            display.renderizarMatriz(setor);
            String idVaga = lerVaga(setor, "Escolha a vaga para liberar (ex.: B3, 0 para voltar): ");
            if (idVaga == null) return false;

            Requisicao req = new Requisicao()
                    .setTipo(TipoRequisicao.SAIDA).setSetor(setor).setIdVaga(idVaga);

            if (publicarEExecutar(req)) {
                display.renderizarMatriz(setor);
                return true;
            }
            // Rejeitado: repete a matriz para escolher outra vaga.
        }
    }

    private void fluxoRelatorio() {
        display.renderizarMenuRelatorios();
        int opcao = lerInteiro("Relatório: ", 0, TipoRelatorio.values().length);
        if (opcao == 0) return;

        TipoRelatorio tipo = TipoRelatorio.values()[opcao - 1];
        Requisicao req = new Requisicao().setTipo(TipoRequisicao.RELATORIO).setTipoRelatorio(tipo);

        if (tipo == TipoRelatorio.HISTORICO_POR_PLACA) {
            req.setPlaca(lerPlaca());
        } else if (tipo == TipoRelatorio.HISTORICO_POR_PERIODO) {
            req.setInicio(lerDataHora("Data/hora inicial (dd/MM/aaaa HH:mm): "));
            req.setFim(lerDataHora("Data/hora final   (dd/MM/aaaa HH:mm): "));
        }

        publicarEExecutar(req);
    }

    /** A requisição só chega ao Blackboard quando está completa. Devolve true se o KS aceitou. */
    private boolean publicarEExecutar(Requisicao req) {
        ResultadoOperacao anterior = blackboard.getUltimoResultado();
        blackboard.publicarRequisicao(req);
        controller.executarCiclo();
        ResultadoOperacao atual = blackboard.getUltimoResultado();
        return atual != null && atual != anterior && atual.isSucesso();
    }

    // ----------------------------------------------------------------- coleta

    private Setor escolherSetor() {
        display.renderizarSetores();
        int opcao = lerInteiro("Setor: ", 0, 2);
        return switch (opcao) {
            case 1 -> Setor.EXTERNO;
            case 2 -> Setor.SUBSOLO;
            default -> null;
        };
    }

    private Double lerAltura() {
        while (true) {
            String txt = lerLinha("Altura do veículo (m, 0 para voltar): ").replace(',', '.');
            if (txt.equals("0")) return null;
            try {
                double altura = Double.parseDouble(txt);
                if (altura > 0) return altura;
            } catch (NumberFormatException e) {
                // formato inválido
            }
            display.renderizarAviso("Valor inválido. Informe a altura em metros (ex.: 1.85).");
        }
    }

    /**
     * A altura é validada pelo SubsoloKS ANTES de coletar placa e vaga, para o operador
     * não avançar sabendo que o veículo não pode entrar. A regra continua no especialista:
     * a CLI apenas publica uma Requisicao(VALIDAR_ALTURA) e lê o ResultadoOperacao.
     */
    private Double lerAlturaValidada() {
        while (true) {
            Double altura = lerAltura();
            if (altura == null) return null;

            Requisicao validacao = new Requisicao()
                    .setTipo(TipoRequisicao.VALIDAR_ALTURA)
                    .setSetor(Setor.SUBSOLO)
                    .setAltura(altura);

            if (publicarEExecutar(validacao)) return altura;
            // Rejeitada: o Display já mostrou o motivo; pede a altura novamente.
        }
    }

    private String lerPlaca() {
        while (true) {
            String placa = lerLinha("Placa do veículo: ").toUpperCase();
            if (placa.matches("[A-Z0-9-]{5,8}")) return placa;
            display.renderizarAviso("Placa inválida. Use de 5 a 8 letras/números (ex.: ABC1D23).");
        }
    }

    /** Devolve o id de uma vaga existente no setor, ou null se o operador digitar 0. */
    private String lerVaga(Setor setor, String prompt) {
        while (true) {
            String txt = lerLinha(prompt).toUpperCase();
            if (txt.equals("0")) return null;
            if (blackboard.getVaga(setor, txt) != null) return txt;
            display.renderizarAviso("Vaga inexistente neste setor. Digite um código da matriz (ex.: A1).");
        }
    }

    /** Opção 7 ("Nenhum dos anteriores") vira null; quem rejeita é o SubsoloKS. */
    private MotivoPreferencial lerMotivo() {
        display.renderizarMenuMotivos();
        MotivoPreferencial[] motivos = MotivoPreferencial.values();
        int opcao = lerInteiro("Motivo: ", 1, motivos.length + 1);
        return opcao <= motivos.length ? motivos[opcao - 1] : null;
    }

    private LocalDateTime lerDataHora(String prompt) {
        while (true) {
            try {
                return LocalDateTime.parse(lerLinha(prompt), FORMATO_DATA_HORA);
            } catch (DateTimeParseException e) {
                display.renderizarAviso("Data inválida. Use o formato dd/MM/aaaa HH:mm (ex.: 14/11/2026 18:00).");
            }
        }
    }

    private int lerInteiro(String prompt, int min, int max) {
        while (true) {
            String txt = lerLinha(prompt);
            try {
                int n = Integer.parseInt(txt);
                if (n >= min && n <= max) return n;
            } catch (NumberFormatException e) {
                // formato inválido
            }
            display.renderizarAviso("Opção inválida. Digite um número de " + min + " a " + max + ".");
        }
    }

    private String lerLinha(String prompt) {
        display.renderizarPrompt(prompt);
        if (!in.hasNextLine()) {
            throw new FimDeEntrada();
        }
        return in.nextLine().trim();
    }
}
