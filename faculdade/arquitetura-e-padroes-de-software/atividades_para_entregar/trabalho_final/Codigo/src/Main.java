import blackboard.Blackboard;
import blackboard.BlackboardController;
import knowledge_sources.ExternoKS;
import knowledge_sources.KnowledgeSource;
import knowledge_sources.RelatorioKS;
import knowledge_sources.SubsoloKS;
import ui.CLI;
import ui.Display;
import ui.Terminal;

import java.util.List;

/** Composition root: cria e conecta todos os componentes. */
public class Main {

    public static void main(String[] args) {
        Terminal.configurarUtf8();

        Blackboard blackboard = Blackboard.getInstance();

        Display display = new Display(blackboard);
        blackboard.adicionarObserver(display);

        List<KnowledgeSource> especialistas = List.of(
                new ExternoKS(blackboard),
                new SubsoloKS(blackboard),
                new RelatorioKS(blackboard));
        BlackboardController controller = new BlackboardController(blackboard, especialistas);

        new CLI(blackboard, controller, display).executar();
    }
}
