package ui.janela;

import model.Relatorio;
import model.TipoRelatorio;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Relatórios da gerência: a escolha do tipo, os filtros que ele pede (placa ou período)
 * e o texto do relatório publicado no Blackboard pelo especialista de relatórios.
 */
public class PainelRelatorios {

    private final JPanel painel = new JPanel(new BorderLayout(4, 4));
    private final JComboBox<TipoRelatorio> tipo = new JComboBox<>(TipoRelatorio.values());
    private final JLabel rotuloPlaca = new JLabel("Placa:");
    private final JTextField placa = new JTextField(9);
    private final JLabel rotuloPeriodo = new JLabel("De:");
    private final JTextField inicio = new JTextField("20/11/2026 18:00", 12);
    private final JLabel rotuloAte = new JLabel("até:");
    private final JTextField fim = new JTextField("20/11/2026 22:00", 12);
    private final JButton gerar = new JButton("Gerar");
    private final JPanel linhaDosCampos = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
    private final JTextArea texto = new JTextArea();

    PainelRelatorios(Runnable aoGerar) {
        tipo.setRenderer(Componentes.rotulo(TipoRelatorio.class, TipoRelatorio::rotulo));
        tipo.addActionListener(e -> ajustarFiltros());
        inicio.setToolTipText("dd/MM/aaaa HH:mm");
        fim.setToolTipText("dd/MM/aaaa HH:mm");
        gerar.addActionListener(e -> aoGerar.run());

        // Duas linhas: a escolha e o botão Gerar sempre à vista; embaixo, só os campos que o relatório pede.
        JPanel linhaDaEscolha = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        linhaDaEscolha.add(new JLabel("Relatório:"));
        linhaDaEscolha.add(tipo);
        linhaDaEscolha.add(gerar);
        linhaDosCampos.add(rotuloPlaca);
        linhaDosCampos.add(placa);
        linhaDosCampos.add(rotuloPeriodo);
        linhaDosCampos.add(inicio);
        linhaDosCampos.add(rotuloAte);
        linhaDosCampos.add(fim);
        JPanel filtros = new JPanel(new BorderLayout());
        filtros.add(linhaDaEscolha, BorderLayout.NORTH);
        filtros.add(linhaDosCampos, BorderLayout.SOUTH);

        texto.setEditable(false);
        texto.setFont(Componentes.MONO);
        painel.add(filtros, BorderLayout.NORTH);
        painel.add(new JScrollPane(texto), BorderLayout.CENTER);
        ajustarFiltros();
    }

    JComponent componente() {
        return painel;
    }

    TipoRelatorio getTipo() {
        return (TipoRelatorio) tipo.getSelectedItem();
    }

    String getPlaca() {
        return placa.getText();
    }

    String getInicio() {
        return inicio.getText();
    }

    String getFim() {
        return fim.getText();
    }

    void mostrar(Relatorio relatorio) {
        texto.setText("═══ " + relatorio.getTitulo() + " ═══\n\n" + String.join("\n", relatorio.getLinhas()));
        texto.setCaretPosition(0);
    }

    void limpar() {
        texto.setText("");
    }

    /** Mostra só os filtros que o relatório escolhido usa. */
    private void ajustarFiltros() {
        boolean porPlaca = getTipo() == TipoRelatorio.HISTORICO_POR_PLACA;
        boolean porPeriodo = getTipo() == TipoRelatorio.HISTORICO_POR_PERIODO;
        rotuloPlaca.setVisible(porPlaca);
        placa.setVisible(porPlaca);
        rotuloPeriodo.setVisible(porPeriodo);
        inicio.setVisible(porPeriodo);
        rotuloAte.setVisible(porPeriodo);
        fim.setVisible(porPeriodo);
        linhaDosCampos.setVisible(porPlaca || porPeriodo);
        painel.revalidate();
    }

    // Usados pela conferência de layout dos testes (mesmo pacote).

    void escolher(TipoRelatorio relatorio) {
        tipo.setSelectedItem(relatorio);
    }

    JButton botaoGerar() {
        return gerar;
    }
}
