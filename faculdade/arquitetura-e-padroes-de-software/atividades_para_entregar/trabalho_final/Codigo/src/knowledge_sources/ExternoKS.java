package knowledge_sources;

import blackboard.Blackboard;
import model.Setor;

/** Especialista do setor EXTERNO: segue as regras comuns dos setores, sem restrição própria. */
public class ExternoKS extends EspecialistaDeSetor {

    public ExternoKS(Blackboard blackboard) {
        super(blackboard);
    }

    @Override
    protected Setor setor() {
        return Setor.EXTERNO;
    }
}
