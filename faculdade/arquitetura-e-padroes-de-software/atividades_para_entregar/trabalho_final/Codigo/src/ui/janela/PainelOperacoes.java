package ui.janela;

import model.Setor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * A cancela: setor, placa e altura (pedida só no Subsolo), com os botões de entrada e saída.
 * Só coleta o que foi digitado; a janela confere o formato e envia pela fachada.
 */
public class PainelOperacoes {

    private final JPanel painel = new JPanel(new BorderLayout(4, 4));
    private final JComboBox<Setor> setor = new JComboBox<>(Setor.values());
    private final JTextField placa = new JTextField(9);
    private final JTextField altura = new JTextField(5);
    private final JLabel rotuloAltura = new JLabel("Altura (m):");

    PainelOperacoes(Runnable aoEntrar, Runnable aoSair) {
        setor.setRenderer(Componentes.rotulo(Setor.class, Setor::descricao));
        setor.addActionListener(e -> ajustarAltura());
        JButton entrar = new JButton("Registrar entrada");
        JButton sair = new JButton("Registrar saída");
        entrar.addActionListener(e -> aoEntrar.run());
        sair.addActionListener(e -> aoSair.run());

        JPanel campos = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        campos.add(new JLabel("Setor:"));
        campos.add(setor);
        campos.add(new JLabel("Placa:"));
        campos.add(placa);
        campos.add(rotuloAltura);
        campos.add(altura);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        botoes.add(entrar);
        botoes.add(sair);

        painel.setBorder(BorderFactory.createTitledBorder("CANCELA"));
        painel.add(campos, BorderLayout.NORTH);
        painel.add(botoes, BorderLayout.CENTER);
        painel.add(new JLabel("  Para ocupar ou desocupar uma vaga, clique nela no mapa do setor."), BorderLayout.SOUTH);
        ajustarAltura();
    }

    JComponent componente() {
        return painel;
    }

    Setor getSetor() {
        return (Setor) setor.getSelectedItem();
    }

    String getPlaca() {
        return placa.getText();
    }

    String getAltura() {
        return altura.getText();
    }

    void limpar() {
        placa.setText("");
        altura.setText("");
        placa.requestFocusInWindow();
    }

    private void ajustarAltura() {
        boolean subsolo = getSetor() == Setor.SUBSOLO;
        altura.setEnabled(subsolo);
        rotuloAltura.setEnabled(subsolo);
        if (!subsolo) altura.setText("");
    }
}
