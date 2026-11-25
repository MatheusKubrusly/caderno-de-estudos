package ui;

import blackboard.Blackboard;
import blackboard.BlackboardController;
import model.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
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
                int opcao = lerInteiro("Opção: ", 0, 5);
                switch (opcao) {
                    case 1 -> fluxoEntrar();
                    case 2 -> fluxoOcupar();
                    case 3 -> fluxoDesocupar();
                    case 4 -> fluxoSair();
                    case 5 -> fluxoRelatorio();
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
     * Entrada pela cancela do setor. Em caso de recusa, pede de novo só o dado recusado:
     * a placa, se o veículo já está no estacionamento; senão, a altura (regra do Subsolo).
     * `0` na altura volta à seleção de setor; `0` no setor volta ao menu principal.
     */
    private void fluxoEntrar() {
        while (true) {
            Setor setor = escolherSetor();
            if (setor == null) return;

            // Buffer de entrada: existe só até a publicação da Requisicao.
            // Não é estado de domínio — o Blackboard ainda não foi tocado.
            Double altura = null;
            if (setor == Setor.SUBSOLO) {
                altura = lerAltura();
                if (altura == null) continue;
            }
            String placa = lerPlaca();

            while (true) {
                Requisicao req = new Requisicao()
                        .setTipo(TipoRequisicao.ENTRAR)
                        .setSetor(setor)
                        .setPlaca(placa)
                        .setAltura(altura);
                if (publicarEExecutar(req)) return;

                // Recusado: o Display já mostrou o motivo; pede de novo só o dado recusado.
                if (blackboard.getEstadiaAberta(placa) != null) {
                    placa = lerPlaca();
                } else if (setor == Setor.SUBSOLO) {
                    altura = lerAltura();
                    if (altura == null) break;
                } else {
                    return;
                }
            }
        }
    }

    /** Escolhe um veículo que está circulando no setor e repete só a escolha de vaga até o KS aceitar. */
    private void fluxoOcupar() {
        while (true) {
            Setor setor = escolherSetor();
            if (setor == null) return;
            Estadia veiculo = escolherVeiculoCirculando(setor);
            if (veiculo == null) continue;
            if (escolherVagaEOcupar(setor, veiculo.getPlaca())) return;
            // 0 na vaga: volta ao setor
        }
    }

    private boolean escolherVagaEOcupar(Setor setor, String placa) {
        while (true) {
            display.renderizarMatriz(setor);
            String idVaga = lerVaga(setor, "Escolha a vaga para " + placa + " (ex.: A1, 0 para voltar): ");
            if (idVaga == null) return false;

            Requisicao req = new Requisicao()
                    .setTipo(TipoRequisicao.OCUPAR)
                    .setSetor(setor)
                    .setPlaca(placa)
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

    private void fluxoDesocupar() {
        while (true) {
            Setor setor = escolherSetor();
            if (setor == null) return;
            if (escolherVagaEDesocupar(setor)) return;
        }
    }

    private boolean escolherVagaEDesocupar(Setor setor) {
        while (true) {
            display.renderizarMatriz(setor);
            String idVaga = lerVaga(setor, "Escolha a vaga para desocupar (ex.: B3, 0 para voltar): ");
            if (idVaga == null) return false;

            Requisicao req = new Requisicao()
                    .setTipo(TipoRequisicao.DESOCUPAR).setSetor(setor).setIdVaga(idVaga);

            if (publicarEExecutar(req)) {
                display.renderizarMatriz(setor);
                return true;
            }
            // Rejeitado: repete a matriz para escolher outra vaga.
        }
    }

    /** Saída pela cancela. Se o KS recusar (por exemplo, veículo ainda na vaga), o motivo fica na tela. */
    private void fluxoSair() {
        Setor setor = escolherSetor();
        if (setor == null) return;
        publicarEExecutar(new Requisicao()
                .setTipo(TipoRequisicao.SAIR).setSetor(setor).setPlaca(lerPlaca()));
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
        if (req.getMomento() == null) req.setMomento(LocalDateTime.now());
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

    /** Lista os veículos circulando no setor e devolve o escolhido; null se não houver nenhum ou se o operador digitar 0. */
    private Estadia escolherVeiculoCirculando(Setor setor) {
        List<Estadia> circulando = blackboard.getVeiculosCirculando(setor);
        if (circulando.isEmpty()) {
            display.renderizarAviso("Nenhum veículo circulando no setor " + setor.descricao()
                    + ". Registre uma entrada antes.");
            return null;
        }
        display.renderizarVeiculosCirculando(circulando);
        int opcao = lerInteiro("Veículo: ", 0, circulando.size());
        return opcao == 0 ? null : circulando.get(opcao - 1);
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
