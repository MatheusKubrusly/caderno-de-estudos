import aplicacao.FachadaEstacionamento;
import blackboard.Blackboard;
import blackboard.BlackboardController;
import knowledge_sources.ExternoKS;
import knowledge_sources.KnowledgeSource;
import knowledge_sources.RelatorioKS;
import knowledge_sources.SubsoloKS;
import persistencia.RegistroDeEventos;
import ui.CLI;
import ui.Display;
import ui.Terminal;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Composition root: cria e conecta todos os componentes. */
public class Main {

    private static final Path REGISTRO = Path.of("dados", "eventos.csv");

    public static void main(String[] args) {
        Terminal.configurarUtf8();

        Blackboard blackboard = Blackboard.getInstance();

        List<KnowledgeSource> especialistas = List.of(
                new ExternoKS(blackboard),
                new SubsoloKS(blackboard),
                new RelatorioKS(blackboard));
        BlackboardController controller = new BlackboardController(blackboard, especialistas);
        FachadaEstacionamento fachada = new FachadaEstacionamento(blackboard, controller);

        // O uso registrado é relido antes de a tela observar o Blackboard,
        // para a releitura não aparecer como operações novas.
        RegistroDeEventos registro = new RegistroDeEventos(blackboard, fachada, REGISTRO);
        String aviso = carregarRegistro(registro);
        blackboard.adicionarObserver(registro);

        Display display = new Display(blackboard);
        blackboard.adicionarObserver(display);
        if (aviso != null) display.renderizarAviso(aviso);

        new CLI(blackboard, controller, display).executar();
    }

    private static String carregarRegistro(RegistroDeEventos registro) {
        try {
            RegistroDeEventos.Leitura leitura = registro.carregar();
            if (leitura.aceitos() == 0 && leitura.problemas().isEmpty()) return null;
            return "Registro de uso (" + REGISTRO + "): " + leitura.resumo();
        } catch (IOException e) {
            return "Não foi possível ler " + REGISTRO + " (" + e.getMessage() + "). O sistema começa vazio.";
        }
    }
}
