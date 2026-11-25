package knowledge_sources;

import blackboard.Blackboard;
import model.*;
import util.Formatador;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Esqueleto comum dos especialistas de setor (Template Method com ganchos).
 *
 * Os quatro eventos de um veículo seguem as mesmas regras em qualquer setor:
 * entrar pela cancela, ocupar uma vaga, desocupá-la e sair. Cada setor só
 * acrescenta as suas regras nos ganchos {@link #problemaNaEntrada} e
 * {@link #problemaNaOcupacao}. Uma requisição inconsistente é recusada com uma
 * mensagem que orienta, e o Blackboard não muda.
 *
 * Não conhece nenhum outro especialista: só lê e escreve no Blackboard.
 */
public abstract class EspecialistaDeSetor extends KnowledgeSource {

    protected EspecialistaDeSetor(Blackboard blackboard) {
        super(blackboard);
    }

    /** O setor pelo qual este especialista responde. */
    protected abstract Setor setor();

    /** Gancho: regra própria do setor na entrada. Devolve o motivo da recusa, ou null. */
    protected String problemaNaEntrada(Requisicao r) {
        return null;
    }

    /** Gancho: regra própria do setor na ocupação. Devolve o motivo da recusa, ou null. */
    protected String problemaNaOcupacao(Requisicao r, Vaga vaga) {
        return null;
    }

    @Override
    protected final boolean condition() {
        Requisicao r = blackboard.getRequisicaoPendente();
        return r != null && r.getSetor() == setor() && r.getTipo() != null && r.getTipo().isEvento();
    }

    @Override
    protected final void execute() {
        Requisicao r = blackboard.getRequisicaoPendente();
        switch (r.getTipo()) {
            case ENTRAR -> entrar(r);
            case OCUPAR -> ocupar(r);
            case DESOCUPAR -> desocupar(r);
            case SAIR -> sair(r);
            default -> erro("Requisição não tratada pelo setor " + nomeDoSetor() + ".");
        }
    }

    // ------------------------------------------------------------------ eventos

    private void entrar(Requisicao r) {
        String placa = normalizar(r.getPlaca());
        if (placa == null) {
            erro("Informe a placa do veículo.");
            return;
        }
        Estadia aberta = blackboard.getEstadiaAberta(placa);
        if (aberta != null) {
            erro("O veículo " + placa + " já está no setor " + aberta.getSetor().descricao()
                    + ". Registre a saída antes de uma nova entrada.");
            return;
        }
        LocalDateTime momento = momento(r);
        List<Estadia> anteriores = blackboard.buscarPorPlaca(placa);
        if (!anteriores.isEmpty()) {
            Estadia ultima = anteriores.get(anteriores.size() - 1);
            if (momento.isBefore(ultima.getUltimoEvento())) {
                erro(foraDeOrdem(momento, ultima));
                return;
            }
        }
        String problema = problemaNaEntrada(r);
        if (problema != null) {
            erro(problema);
            return;
        }

        blackboard.registrarEntrada(new Estadia(placa, setor(), r.getAltura(), momento));
        sucesso("Veículo " + placa + " entrou no setor " + nomeDoSetor()
                + " — " + Formatador.hora(momento) + ". Está circulando.");
    }

    private void ocupar(Requisicao r) {
        String placa = normalizar(r.getPlaca());
        if (placa == null) {
            erro("Informe a placa do veículo.");
            return;
        }
        Estadia estadia = blackboard.getEstadiaAberta(placa);
        if (estadia == null || estadia.getSetor() != setor()) {
            erro("O veículo " + placa + " não entrou no setor " + nomeDoSetor()
                    + ". Registre a entrada antes de ocupar uma vaga.");
            return;
        }
        Ocupacao atual = estadia.getOcupacaoAtual();
        if (atual != null) {
            erro("O veículo " + placa + " já ocupa a vaga " + atual.getIdVaga()
                    + ". Registre a desocupação antes.");
            return;
        }
        Vaga vaga = blackboard.getVaga(setor(), r.getIdVaga());
        if (vaga == null) {
            erro("Vaga " + r.getIdVaga() + " não existe no setor " + nomeDoSetor() + ".");
            return;
        }
        if (!vaga.isLivre()) {
            erro("Vaga " + vaga.getId() + " está ocupada. Escolha outra.");
            return;
        }
        LocalDateTime momento = momento(r);
        if (momento.isBefore(estadia.getUltimoEvento())) {
            erro(foraDeOrdem(momento, estadia));
            return;
        }
        String problema = problemaNaOcupacao(r, vaga);
        if (problema != null) {
            erro(problema);
            return;
        }

        MotivoPreferencial motivo = vaga.isPreferencial() ? r.getMotivo() : null;
        blackboard.registrarOcupacao(estadia, vaga, new Ocupacao(placa, vaga.getId(), momento, motivo));
        sucesso("Veículo " + placa + " estacionado em " + vaga.getId()
                + (motivo != null ? " (Preferencial - " + motivo.rotulo() + ")" : "")
                + " — " + Formatador.hora(momento));
    }

    private void desocupar(Requisicao r) {
        Vaga vaga = blackboard.getVaga(setor(), r.getIdVaga());
        if (vaga == null) {
            erro("Vaga " + r.getIdVaga() + " não existe no setor " + nomeDoSetor() + ".");
            return;
        }
        if (vaga.isLivre()) {
            erro("Vaga " + vaga.getId() + " já está desocupada.");
            return;
        }
        Ocupacao ocupacao = vaga.getOcupacaoAtual();
        Estadia estadia = ocupacao == null ? null : blackboard.getEstadiaAberta(ocupacao.getPlaca());
        if (estadia == null || estadia.getOcupacaoAtual() != ocupacao) {
            erro("Vaga " + vaga.getId() + " não possui ocupação em aberto.");
            return;
        }
        LocalDateTime momento = momento(r);
        if (momento.isBefore(estadia.getUltimoEvento())) {
            erro(foraDeOrdem(momento, estadia));
            return;
        }

        LocalDateTime inicio = ocupacao.getInicio();
        blackboard.registrarDesocupacao(vaga, momento);
        sucesso("Vaga " + vaga.getId() + " liberada — " + estadia.getPlaca()
                + " voltou a circular às " + Formatador.hora(momento)
                + "\n   Tempo na vaga: " + Formatador.duracao(Duration.between(inicio, momento)));
    }

    private void sair(Requisicao r) {
        String placa = normalizar(r.getPlaca());
        if (placa == null) {
            erro("Informe a placa do veículo.");
            return;
        }
        Estadia estadia = blackboard.getEstadiaAberta(placa);
        if (estadia == null || estadia.getSetor() != setor()) {
            erro("O veículo " + placa + " não está no setor " + nomeDoSetor() + ".");
            return;
        }
        Ocupacao atual = estadia.getOcupacaoAtual();
        if (atual != null) {
            erro("O veículo " + placa + " ainda ocupa a vaga " + atual.getIdVaga()
                    + ". Registre a desocupação antes da saída.");
            return;
        }
        LocalDateTime momento = momento(r);
        if (momento.isBefore(estadia.getUltimoEvento())) {
            erro(foraDeOrdem(momento, estadia));
            return;
        }

        blackboard.registrarSaida(estadia, momento);
        sucesso("Veículo " + placa + " saiu do setor " + nomeDoSetor() + " às " + Formatador.hora(momento)
                + "\n   Tempo de permanência: "
                + Formatador.duracao(Duration.between(estadia.getEntrada(), momento)));
    }

    // ------------------------------------------------------------------ apoio

    private String nomeDoSetor() {
        return setor().descricao();
    }

    /** Placa em maiúsculas e sem espaços nas pontas; null se vier vazia. */
    private static String normalizar(String placa) {
        if (placa == null || placa.isBlank()) return null;
        return placa.trim().toUpperCase();
    }

    /** Horário do evento: o que veio na requisição ou, se faltar, o de agora. */
    private static LocalDateTime momento(Requisicao r) {
        return r.getMomento() != null ? r.getMomento() : LocalDateTime.now();
    }

    private static String foraDeOrdem(LocalDateTime momento, Estadia estadia) {
        return "Horário do evento (" + Formatador.dataHoraCurta(momento)
                + ") é anterior ao último evento do veículo " + estadia.getPlaca()
                + " (" + Formatador.dataHoraCurta(estadia.getUltimoEvento()) + ").";
    }
}
