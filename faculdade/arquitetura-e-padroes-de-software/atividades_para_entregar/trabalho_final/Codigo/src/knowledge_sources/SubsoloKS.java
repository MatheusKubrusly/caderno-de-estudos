package knowledge_sources;

import blackboard.Blackboard;
import model.*;
import util.Formatador;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Especialista do setor SUBSOLO: valida altura, vaga, disponibilidade e motivo
 * de vaga preferencial; ocupa/libera. Não conhece nenhum outro KS.
 */
public class SubsoloKS extends KnowledgeSource {

    private static final double ALTURA_MAXIMA = 2.10;

    public SubsoloKS(Blackboard blackboard) {
        super(blackboard);
    }

    @Override
    protected boolean condition() {
        Requisicao r = blackboard.getRequisicaoPendente();
        return r != null
                && r.getSetor() == Setor.SUBSOLO
                && (r.getTipo() == TipoRequisicao.ENTRADA
                    || r.getTipo() == TipoRequisicao.SAIDA
                    || r.getTipo() == TipoRequisicao.VALIDAR_ALTURA);
    }

    @Override
    protected void execute() {
        Requisicao r = blackboard.getRequisicaoPendente();
        if (r.getTipo() == TipoRequisicao.VALIDAR_ALTURA) {
            validarAltura(r);
        } else if (r.getTipo() == TipoRequisicao.ENTRADA) {
            entrada(r);
        } else {
            saida(r);
        }
    }

    /** Pré-validação solicitada pela CLI antes de coletar placa e vaga. Não ocupa nada. */
    private void validarAltura(Requisicao r) {
        String problema = problemaAltura(r.getAltura());
        if (problema != null) {
            erro(problema);
            return;
        }
        blackboard.publicarResultado(ResultadoOperacao.ok(
                "Altura " + Formatador.altura(r.getAltura())
                        + " compatível com o Subsolo (máx. " + Formatador.altura(ALTURA_MAXIMA) + ")."));
    }

    private String problemaAltura(Double altura) {
        if (altura == null) {
            return "Altura do veículo é obrigatória no Subsolo.";
        }
        if (altura > ALTURA_MAXIMA) {
            return "Veículo não permitido no Subsolo: " + Formatador.altura(altura)
                    + " excede o máximo de " + Formatador.altura(ALTURA_MAXIMA) + ".";
        }
        return null;
    }

    private void entrada(Requisicao r) {
        String problema = problemaAltura(r.getAltura());
        if (problema != null) {
            erro(problema);
            return;
        }

        Vaga vaga = blackboard.getVaga(Setor.SUBSOLO, r.getIdVaga());
        if (vaga == null) {
            erro("Vaga " + r.getIdVaga() + " não existe no setor Subsolo.");
            return;
        }
        if (!vaga.isLivre()) {
            erro("Vaga " + vaga.getId() + " está ocupada. Escolha outra.");
            return;
        }
        if (vaga.isPreferencial() && r.getMotivo() == null) {
            erro("Vaga " + vaga.getId() + " é preferencial e exige um motivo válido. Escolha outra vaga.");
            return;
        }

        MotivoPreferencial motivo = vaga.isPreferencial() ? r.getMotivo() : null;
        LocalDateTime agora = LocalDateTime.now();
        RegistroLog log = new RegistroLog(r.getPlaca(), Setor.SUBSOLO, vaga.getId(), agora, motivo);
        blackboard.registrarEntrada(vaga, log);
        blackboard.publicarResultado(ResultadoOperacao.ok(
                "Veículo " + r.getPlaca() + " estacionado em " + vaga.getId()
                        + (motivo != null ? " (Preferencial - " + motivo.rotulo() + ")" : "")
                        + " — " + Formatador.hora(agora)));
    }

    private void saida(Requisicao r) {
        Vaga vaga = blackboard.getVaga(Setor.SUBSOLO, r.getIdVaga());
        if (vaga == null) {
            erro("Vaga " + r.getIdVaga() + " não existe no setor Subsolo.");
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
