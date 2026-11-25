package ui.janela;

import aplicacao.FachadaEstacionamento;
import blackboard.Blackboard;
import blackboard.Observer;
import model.*;
import persistencia.RegistroDeEventos;
import ui.FormatoDeEntrada;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Janela da demonstração (front-end Swing) e Observer do Blackboard.
 *
 * Coleta os dados, valida só o formato e envia as requisições pela fachada; quem aceita
 * ou recusa são os especialistas. A cada aviso do Blackboard, registra o ciclo e
 * redesenha os mapas. O menu Dados carrega o cenário de demonstração ou zera o uso.
 */
public class JanelaPrincipal implements Observer {

    private static final int ATUALIZAR_A_CADA_MS = 30_000;
    private static final String PLACA_INVALIDA = "Placa inválida. Use de 5 a 8 letras ou números (ex.: ABC1D23).";
    private static final String COMO_USAR = """
            1. Cancela: escolha o setor, digite a placa (e a altura, no Subsolo) e clique em Registrar entrada.
            2. Clique numa vaga livre para estacionar um veículo que está circulando no setor.
            3. Clique numa vaga ocupada para desocupá-la; o veículo volta a circular.
            4. Para a saída, digite a placa e clique em Registrar saída.
            5. A aba Ciclo do Blackboard mostra cada requisição e quem respondeu.
            6. A aba Relatórios gera os relatórios da gerência.
            7. Dados → Carregar cenário põe uma semana de uso simulado (16 a 22/11/2026).

            O roteiro completo está no arquivo roteiro-de-uso.md.""";

    private final Blackboard blackboard;
    private final FachadaEstacionamento fachada;
    private final RegistroDeEventos registro;
    private final Path cenario;

    private final JFrame janela = new JFrame("Estacionamento do shopping — arquitetura Blackboard");
    private final PainelSetor externo;
    private final PainelSetor subsolo;
    private final PainelOperacoes operacoes;
    private final PainelCiclo ciclo = new PainelCiclo();
    private final PainelRelatorios relatorios;
    private final JTabbedPane abas = new JTabbedPane();
    private final JLabel status = new JLabel(" ");

    private ResultadoOperacao resultadoExibido;
    private Relatorio relatorioExibido;
    private String falhaAvisada;

    public JanelaPrincipal(Blackboard blackboard, FachadaEstacionamento fachada,
                           RegistroDeEventos registro, Path cenario) {
        this.blackboard = blackboard;
        this.fachada = fachada;
        this.registro = registro;
        this.cenario = cenario;
        this.externo = new PainelSetor(blackboard, Setor.EXTERNO, this::vagaClicada);
        this.subsolo = new PainelSetor(blackboard, Setor.SUBSOLO, this::vagaClicada);
        this.operacoes = new PainelOperacoes(this::entrar, this::sair);
        this.relatorios = new PainelRelatorios(this::gerarRelatorio);
        this.resultadoExibido = blackboard.getUltimoResultado();
        this.relatorioExibido = blackboard.getUltimoRelatorio();
        montar();
    }

    public void mostrar() {
        janela.setVisible(true);
    }

    /** Anota no registro do ciclo um aviso que não é uma requisição (por exemplo, o resumo da releitura). */
    public void anotar(String nota) {
        ciclo.anotar(nota);
    }

    public void selecionarAba(int indice) {
        abas.setSelectedIndex(indice);
    }

    public JFrame getJanela() {
        return janela;
    }

    // ---------------------------------------------------------------- montagem

    private void montar() {
        JPanel esquerda = new JPanel();
        esquerda.setLayout(new BoxLayout(esquerda, BoxLayout.Y_AXIS));
        esquerda.add(externo.componente());
        esquerda.add(subsolo.componente());
        esquerda.add(operacoes.componente());

        abas.addTab("Ciclo do Blackboard", ciclo.componente());
        abas.addTab("Relatórios", relatorios.componente());

        JSplitPane divisao = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, esquerda, abas);
        divisao.setResizeWeight(0.45);

        status.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JPanel conteudo = new JPanel(new BorderLayout());
        conteudo.add(divisao, BorderLayout.CENTER);
        conteudo.add(status, BorderLayout.SOUTH);

        janela.setJMenuBar(menu());
        janela.setContentPane(conteudo);
        janela.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        janela.setSize(1280, 800);
        janela.setLocationRelativeTo(null);
        new Timer(ATUALIZAR_A_CADA_MS, e -> redesenharMapas()).start();
    }

    private JMenuBar menu() {
        JMenuItem carregar = new JMenuItem("Carregar cenário de demonstração…");
        carregar.addActionListener(e -> carregarCenario());
        JMenuItem zerar = new JMenuItem("Zerar dados…");
        zerar.addActionListener(e -> zerarDados());
        JMenu dados = new JMenu("Dados");
        dados.add(carregar);
        dados.add(zerar);

        JMenuItem comoUsar = new JMenuItem("Como usar");
        comoUsar.addActionListener(e ->
                JOptionPane.showMessageDialog(janela, COMO_USAR, "Como usar", JOptionPane.INFORMATION_MESSAGE));
        JMenu ajuda = new JMenu("Ajuda");
        ajuda.add(comoUsar);

        JMenuBar barra = new JMenuBar();
        barra.add(dados);
        barra.add(ajuda);
        return barra;
    }

    // ---------------------------------------------------------------- observer

    /** Chamado pelo Blackboard. O resultado é lido agora; o redesenho vai para a thread do Swing. */
    @Override
    public void atualizar() {
        ResultadoOperacao resultado = blackboard.getUltimoResultado();
        Relatorio relatorio = blackboard.getUltimoRelatorio();
        SwingUtilities.invokeLater(() -> redesenhar(resultado, relatorio));
    }

    private void redesenhar(ResultadoOperacao resultado, Relatorio relatorio) {
        if (resultado != null && resultado != resultadoExibido) {
            resultadoExibido = resultado;
            ciclo.registrar(resultado);
            mostrarStatus(resultado.isSucesso(), resultado.getMensagem());
        }
        if (relatorio != null && relatorio != relatorioExibido) {
            relatorioExibido = relatorio;
            relatorios.mostrar(relatorio);
            ciclo.anotar("RELATORIO " + relatorio.getTipo() + ": relatório publicado no Blackboard");
            abas.setSelectedIndex(1);
        }
        redesenharMapas();
    }

    private void redesenharMapas() {
        externo.atualizar();
        subsolo.atualizar();
    }

    private void mostrarStatus(boolean ok, String mensagem) {
        status.setForeground(ok ? Componentes.TEXTO_OK : Componentes.TEXTO_AVISO);
        status.setText((ok ? "✓ " : "⚠ ") + mensagem.replaceAll("\\s*\\n\\s*", " · "));
    }

    // ------------------------------------------------------------ cancela

    private void entrar() {
        Setor setor = operacoes.getSetor();
        String placa = FormatoDeEntrada.placa(operacoes.getPlaca());
        if (placa == null) {
            avisarFormato(PLACA_INVALIDA);
            return;
        }
        Double altura = null;
        if (setor == Setor.SUBSOLO) {
            altura = FormatoDeEntrada.altura(operacoes.getAltura());
            if (altura == null) {
                avisarFormato("Informe a altura do veículo em metros (ex.: 1,85).");
                return;
            }
        }
        ResultadoOperacao r = fachada.entrar(setor, placa, altura);
        if (r != null && r.isSucesso()) operacoes.limpar();
        conferirGravacao();
    }

    private void sair() {
        Setor setor = operacoes.getSetor();
        String placa = FormatoDeEntrada.placa(operacoes.getPlaca());
        if (placa == null) {
            avisarFormato(PLACA_INVALIDA);
            return;
        }
        ResultadoOperacao r = fachada.sair(setor, placa);
        if (r != null && r.isSucesso()) {
            operacoes.limpar();
        } else {
            oferecerDesocuparESair(setor, placa);
        }
        conferirGravacao();
    }

    /** Atalho: se o carro ainda está na vaga, oferece as duas requisições; cada uma é validada pelo especialista. */
    private void oferecerDesocuparESair(Setor setor, String placa) {
        Estadia estadia = blackboard.getEstadiaAberta(placa);
        if (estadia == null || estadia.getSetor() != setor || estadia.getOcupacaoAtual() == null) return;
        String vaga = estadia.getOcupacaoAtual().getIdVaga();
        int escolha = JOptionPane.showConfirmDialog(janela,
                "O veículo " + placa + " ainda ocupa a vaga " + vaga + ".\nDesocupar " + vaga + " e registrar a saída?",
                "Saída recusada", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (escolha != JOptionPane.YES_OPTION) return;
        ResultadoOperacao desocupou = fachada.desocupar(setor, vaga);
        if (desocupou != null && desocupou.isSucesso()) {
            ResultadoOperacao saiu = fachada.sair(setor, placa);
            if (saiu != null && saiu.isSucesso()) operacoes.limpar();
        }
    }

    // ---------------------------------------------------------------- vagas

    private void vagaClicada(Setor setor, String idVaga) {
        Vaga vaga = blackboard.getVaga(setor, idVaga);
        if (vaga == null) return;
        if (vaga.isLivre()) {
            ocuparVaga(setor, vaga);
        } else {
            desocuparVaga(setor, vaga);
        }
        conferirGravacao();
    }

    private void ocuparVaga(Setor setor, Vaga vaga) {
        List<Estadia> circulando = blackboard.getVeiculosCirculando(setor);
        if (circulando.isEmpty()) {
            JOptionPane.showMessageDialog(janela, "Nenhum veículo circulando no setor " + setor.descricao()
                    + ".\nRegistre uma entrada na cancela antes.", "Ocupar " + vaga.getId(), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JComboBox<String> placas = new JComboBox<>(circulando.stream().map(Estadia::getPlaca).toArray(String[]::new));
        JComboBox<String> motivos = new JComboBox<>(opcoesDeMotivo());
        JPanel formulario = new JPanel(new GridLayout(0, 1, 4, 4));
        formulario.add(new JLabel("Veículo circulando no setor:"));
        formulario.add(placas);
        if (vaga.isPreferencial()) {
            formulario.add(new JLabel("Vaga preferencial: motivo de uso"));
            formulario.add(motivos);
        }
        int escolha = JOptionPane.showConfirmDialog(janela, formulario, "Ocupar a vaga " + vaga.getId(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (escolha != JOptionPane.OK_OPTION) return;
        MotivoPreferencial motivo = vaga.isPreferencial() ? motivoDaOpcao(motivos.getSelectedIndex()) : null;
        fachada.ocupar(setor, (String) placas.getSelectedItem(), vaga.getId(), motivo);
    }

    private void desocuparVaga(Setor setor, Vaga vaga) {
        Ocupacao ocupacao = vaga.getOcupacaoAtual();
        String quem = ocupacao == null ? "" : " (" + ocupacao.getPlaca() + ")";
        int escolha = JOptionPane.showConfirmDialog(janela,
                "Desocupar a vaga " + vaga.getId() + quem + "?\nO veículo volta a circular no setor.",
                "Desocupar " + vaga.getId(), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (escolha == JOptionPane.YES_OPTION) fachada.desocupar(setor, vaga.getId());
    }

    /** As descrições dos motivos e, por último, "Nenhum dos anteriores" (que vira motivo nulo e é recusado). */
    private static String[] opcoesDeMotivo() {
        MotivoPreferencial[] motivos = MotivoPreferencial.values();
        String[] opcoes = new String[motivos.length + 1];
        for (int i = 0; i < motivos.length; i++) opcoes[i] = motivos[i].descricao();
        opcoes[motivos.length] = "Nenhum dos anteriores";
        return opcoes;
    }

    private static MotivoPreferencial motivoDaOpcao(int indice) {
        MotivoPreferencial[] motivos = MotivoPreferencial.values();
        return indice >= 0 && indice < motivos.length ? motivos[indice] : null;
    }

    // ------------------------------------------------------------ relatórios

    private void gerarRelatorio() {
        TipoRelatorio tipo = relatorios.getTipo();
        String placa = null;
        LocalDateTime inicio = null;
        LocalDateTime fim = null;
        if (tipo == TipoRelatorio.HISTORICO_POR_PLACA && !relatorios.getPlaca().isBlank()) {
            placa = FormatoDeEntrada.placa(relatorios.getPlaca());
            if (placa == null) {
                avisarFormato(PLACA_INVALIDA);
                return;
            }
        }
        if (tipo == TipoRelatorio.HISTORICO_POR_PERIODO) {
            String textoInicio = relatorios.getInicio();
            String textoFim = relatorios.getFim();
            inicio = textoInicio.isBlank() ? null : FormatoDeEntrada.dataHora(textoInicio);
            fim = textoFim.isBlank() ? null : FormatoDeEntrada.dataHora(textoFim);
            if ((!textoInicio.isBlank() && inicio == null) || (!textoFim.isBlank() && fim == null)) {
                avisarFormato("Data inválida. Use o formato dd/MM/aaaa HH:mm (ex.: 20/11/2026 18:00).");
                return;
            }
        }
        fachada.gerarRelatorio(tipo, placa, inicio, fim);
    }

    // ----------------------------------------------------------------- dados

    private void carregarCenario() {
        int escolha = JOptionPane.showConfirmDialog(janela,
                "Carregar o cenário de demonstração (uma semana simulada, de 16 a 22/11/2026)?\n"
                        + "O uso registrado em " + registro.getArquivo() + " será substituído.",
                "Carregar cenário", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (escolha != JOptionPane.OK_OPTION) return;
        try {
            RegistroDeEventos.Leitura leitura = registro.carregarCenario(cenario);
            depoisDeTrocarOsDados("Cenário de demonstração carregado: " + leitura.resumo());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(janela, "Não foi possível carregar " + cenario + ":\n" + e.getMessage(),
                    "Carregar cenário", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void zerarDados() {
        int escolha = JOptionPane.showConfirmDialog(janela,
                "Apagar todo o uso registrado em " + registro.getArquivo() + "?\n"
                        + "As vagas ficam livres e o histórico, vazio.",
                "Zerar dados", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (escolha != JOptionPane.OK_OPTION) return;
        try {
            registro.zerar();
            depoisDeTrocarOsDados("Dados zerados: todas as vagas livres e nenhum histórico.");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(janela, "Não foi possível zerar " + registro.getArquivo() + ":\n"
                    + e.getMessage(), "Zerar dados", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Depois de reiniciar o Blackboard, o último resultado relido não deve aparecer como operação nova. */
    private void depoisDeTrocarOsDados(String nota) {
        resultadoExibido = blackboard.getUltimoResultado();
        relatorioExibido = blackboard.getUltimoRelatorio();
        relatorios.limpar();
        ciclo.anotar(nota);
        mostrarStatus(true, nota);
        redesenharMapas();
    }

    // ----------------------------------------------------------------- apoio

    private void avisarFormato(String mensagem) {
        JOptionPane.showMessageDialog(janela, mensagem, "Formato inválido", JOptionPane.WARNING_MESSAGE);
    }

    private void conferirGravacao() {
        String falha = registro.getFalhaDeGravacao();
        if (falha != null && !falha.equals(falhaAvisada)) {
            falhaAvisada = falha;
            JOptionPane.showMessageDialog(janela, falha, "Registro de uso", JOptionPane.WARNING_MESSAGE);
        }
    }
}
