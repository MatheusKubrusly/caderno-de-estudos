package knowledge_sources;

import blackboard.Blackboard;
import model.Requisicao;
import model.Setor;
import model.Vaga;
import util.Formatador;

/**
 * Especialista do setor SUBSOLO: além das regras comuns, confere a altura do
 * veículo na cancela e exige motivo nas vagas preferenciais. Não conhece nenhum outro KS.
 */
public class SubsoloKS extends EspecialistaDeSetor {

    private static final double ALTURA_MAXIMA = 2.10;

    public SubsoloKS(Blackboard blackboard) {
        super(blackboard);
    }

    @Override
    protected Setor setor() {
        return Setor.SUBSOLO;
    }

    /** Na cancela do Subsolo, a altura é obrigatória e vai até 2,10 m. */
    @Override
    protected String problemaNaEntrada(Requisicao r) {
        Double altura = r.getAltura();
        if (altura == null) {
            return "Altura do veículo é obrigatória no Subsolo.";
        }
        if (altura > ALTURA_MAXIMA) {
            return "Veículo não permitido no Subsolo: " + Formatador.altura(altura)
                    + " excede o máximo de " + Formatador.altura(ALTURA_MAXIMA) + ".";
        }
        return null;
    }

    /** Vaga preferencial exige um motivo válido (a opção "Nenhum dos anteriores" chega como null). */
    @Override
    protected String problemaNaOcupacao(Requisicao r, Vaga vaga) {
        if (vaga.isPreferencial() && r.getMotivo() == null) {
            return "Vaga " + vaga.getId() + " é preferencial e exige um motivo válido. Escolha outra vaga.";
        }
        return null;
    }
}
