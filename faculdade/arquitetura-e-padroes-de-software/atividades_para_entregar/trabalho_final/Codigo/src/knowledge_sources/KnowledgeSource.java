package knowledge_sources;

import blackboard.Blackboard;
import model.ResultadoOperacao;

/**
 * Template Method: o esqueleto do ciclo (condition -> execute) é fixo em run().
 * Cada especialista implementa apenas condition() e execute().
 *
 * Contrato: condition() não tem efeito colateral; execute() atualiza o Blackboard
 * e publica o resultado nele (nunca imprime).
 */
public abstract class KnowledgeSource {

    protected final Blackboard blackboard;

    protected KnowledgeSource(Blackboard blackboard) {
        this.blackboard = blackboard;
    }

    public final boolean run() {
        if (!condition()) {
            return false;
        }
        execute();
        return true;
    }

    /** Nome do especialista, usado como origem dos resultados que ele publica. */
    public String nome() {
        return getClass().getSimpleName();
    }

    /** Publica um sucesso no Blackboard; o Display é quem o exibe. */
    protected void sucesso(String mensagem) {
        blackboard.publicarResultado(
                ResultadoOperacao.ok(nome(), blackboard.getRequisicaoPendente(), mensagem));
    }

    /** Publica uma rejeição no Blackboard; o Display é quem a exibe. */
    protected void erro(String mensagem) {
        blackboard.publicarResultado(
                ResultadoOperacao.erro(nome(), blackboard.getRequisicaoPendente(), mensagem));
    }

    protected abstract boolean condition();

    protected abstract void execute();
}
