package blackboard;

import knowledge_sources.KnowledgeSource;
import model.Requisicao;
import model.ResultadoOperacao;

import java.util.List;

/**
 * Árbitro da arquitetura Blackboard (controle dirigido pelo estado).
 * É o único componente que conhece todos os KS.
 */
public class BlackboardController {

    private static final int MAX_CICLOS = 10;
    private static final String ORIGEM = "Controlador";

    private final Blackboard blackboard;
    private final List<KnowledgeSource> knowledgeSources;

    public BlackboardController(Blackboard blackboard, List<KnowledgeSource> knowledgeSources) {
        this.blackboard = blackboard;
        this.knowledgeSources = List.copyOf(knowledgeSources);
    }

    /** Executa até a quiescência; a requisição é SEMPRE consumida (inclusive em rejeição e exceção). */
    public void executarCiclo() {
        for (int ciclo = 0; ciclo < MAX_CICLOS; ciclo++) {
            if (!executarVarredura()) break;
        }

        Requisicao pendente = blackboard.getRequisicaoPendente();
        if (pendente != null) {
            blackboard.publicarResultado(ResultadoOperacao.erro(
                    ORIGEM, pendente, "Nenhum especialista apto para a requisição."));
            blackboard.consumirRequisicao();
        }
    }

    /** Uma varredura: o primeiro KS apto executa e consome a requisição. */
    private boolean executarVarredura() {
        for (KnowledgeSource ks : knowledgeSources) {
            try {
                if (ks.run()) {
                    blackboard.consumirRequisicao();
                    return true;
                }
            } catch (RuntimeException e) {
                blackboard.publicarResultado(ResultadoOperacao.erro(
                        ks.nome(), blackboard.getRequisicaoPendente(), "Erro inesperado: " + e.getMessage()));
                blackboard.consumirRequisicao();
                return true;
            }
        }
        return false;
    }
}
