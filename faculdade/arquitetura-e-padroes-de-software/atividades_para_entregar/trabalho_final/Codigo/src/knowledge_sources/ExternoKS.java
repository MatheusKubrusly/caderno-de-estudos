package knowledge_sources;

import blackboard.Blackboard;
import model.*;
import util.Formatador;

import java.time.Duration;
import java.time.LocalDateTime;

/** Especialista do setor EXTERNO: valida a vaga escolhida e ocupa/libera. */
public class ExternoKS extends KnowledgeSource {

    public ExternoKS(Blackboard blackboard) {
        super(blackboard);
    }

    @Override
    protected boolean condition() {
        Requisicao r = blackboard.getRequisicaoPendente();
        return r != null
                && r.getSetor() == Setor.EXTERNO
                && (r.getTipo() == TipoRequisicao.ENTRADA || r.getTipo() == TipoRequisicao.SAIDA);
    }

    @Override
    protected void execute() {
        Requisicao r = blackboard.getRequisicaoPendente();
        if (r.getTipo() == TipoRequisicao.ENTRADA) {
            entrada(r);
        } else {
            saida(r);
        }
    }

    private void entrada(Requisicao r) {
        Vaga vaga = blackboard.getVaga(Setor.EXTERNO, r.getIdVaga());
        if (vaga == null) {
            erro("Vaga " + r.getIdVaga() + " não existe no setor Externo.");
            return;
        }
        if (!vaga.isLivre()) {
            erro("Vaga " + vaga.getId() + " está ocupada. Escolha outra.");
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        RegistroLog log = new RegistroLog(r.getPlaca(), Setor.EXTERNO, vaga.getId(), agora, null);
        blackboard.registrarEntrada(vaga, log);
        blackboard.publicarResultado(ResultadoOperacao.ok(
                "Veículo " + r.getPlaca() + " estacionado em " + vaga.getId()
                        + " — " + Formatador.hora(agora)));
    }

    private void saida(Requisicao r) {
        Vaga vaga = blackboard.getVaga(Setor.EXTERNO, r.getIdVaga());
        if (vaga == null) {
            erro("Vaga " + r.getIdVaga() + " não existe no setor Externo.");
            return;
        }
        if (vaga.isLivre()) {
            erro("Vaga " + vaga.getId() + " já está desocupada.");
            return;
        }
        RegistroLog log = vaga.getRegistroAtual();
        if (log == null) {
            erro("Vaga " + vaga.getId() + " não possui registro de entrada em aberto.");
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        blackboard.registrarSaida(vaga, agora);
        blackboard.publicarResultado(ResultadoOperacao.ok(
                "Vaga " + vaga.getId() + " liberada — " + log.getPlaca() + " saiu às "
                        + Formatador.hora(agora) + "\n   Tempo de permanência: "
                        + Formatador.duracao(Duration.between(log.getEntrada(), agora))));
    }
}
