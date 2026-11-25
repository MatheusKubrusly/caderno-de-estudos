import aplicacao.FachadaEstacionamento;
import blackboard.Blackboard;
import blackboard.BlackboardController;
import knowledge_sources.ExternoKS;
import knowledge_sources.RelatorioKS;
import knowledge_sources.SubsoloKS;
import model.MotivoPreferencial;
import model.Setor;
import model.TipoRelatorio;
import persistencia.RegistroDeEventos;
import ui.janela.JanelaPrincipal;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Gera imagens da janela sem mostrá-la na tela, para conferir o visual e ilustrar o roteiro
 * de uso. Carrega o cenário num registro temporário (o dados/eventos.csv não é tocado),
 * faz algumas operações de exemplo e grava duas imagens: uma com o ciclo do Blackboard
 * e outra com um relatório.
 *
 * Uso: java -cp out CapturaDaJanela pasta-de-destino
 */
public class CapturaDaJanela {

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
        Path pasta = Path.of(args.length > 0 ? args[0] : ".");
        Files.createDirectories(pasta);
        Path cenario = Path.of("dados", "cenario-demonstracao.csv");

        Blackboard blackboard = Blackboard.getInstance();
        BlackboardController controlador = new BlackboardController(blackboard,
                List.of(new ExternoKS(blackboard), new SubsoloKS(blackboard), new RelatorioKS(blackboard)));
        FachadaEstacionamento fachada = new FachadaEstacionamento(blackboard, controlador);
        Path temporario = Files.createTempFile("eventos", ".csv");
        RegistroDeEventos registro = new RegistroDeEventos(blackboard, fachada, temporario);
        RegistroDeEventos.Leitura leitura = registro.carregarCenario(cenario);

        JanelaPrincipal[] janela = new JanelaPrincipal[1];
        SwingUtilities.invokeAndWait(() -> {
            janela[0] = new JanelaPrincipal(blackboard, fachada, registro, cenario);
            blackboard.adicionarObserver(janela[0]);
            janela[0].anotar("Cenário de demonstração carregado: " + leitura.resumo());
            fachada.entrar(Setor.EXTERNO, "ABC1D23", null);
            fachada.ocupar(Setor.EXTERNO, "ABC1D23", "B3", null);
            fachada.entrar(Setor.SUBSOLO, "XYZ9K88", 1.85);
            fachada.ocupar(Setor.SUBSOLO, "XYZ9K88", "A1", MotivoPreferencial.IDOSO);
            fachada.entrar(Setor.SUBSOLO, "VAN2E40", 2.30);
            fachada.entrar(Setor.EXTERNO, "QWE4R56", null);
            fachada.sair(Setor.EXTERNO, "ABC1D23");
        });
        SwingUtilities.invokeAndWait(() -> salvar(janela[0].getJanela(), pasta.resolve("janela-ciclo.png")));

        SwingUtilities.invokeAndWait(() -> fachada.gerarRelatorio(TipoRelatorio.HORARIOS_DE_PICO, null, null, null));
        SwingUtilities.invokeAndWait(() -> salvar(janela[0].getJanela(), pasta.resolve("janela-relatorios.png")));

        Files.deleteIfExists(temporario);
        System.out.println("Imagens gravadas em " + pasta.toAbsolutePath());
        System.exit(0);
    }

    /** Desenha a janela (com o menu) numa imagem na memória e grava o PNG, sem a janela aparecer. */
    private static void salvar(JFrame janela, Path destino) {
        janela.pack();
        janela.setSize(1366, 768);
        janela.validate();
        JRootPane raiz = janela.getRootPane();
        BufferedImage imagem = new BufferedImage(raiz.getWidth(), raiz.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        raiz.printAll(g);
        g.dispose();
        try {
            ImageIO.write(imagem, "png", destino.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
