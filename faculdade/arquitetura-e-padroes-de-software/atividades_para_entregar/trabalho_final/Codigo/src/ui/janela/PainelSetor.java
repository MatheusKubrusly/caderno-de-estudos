package ui.janela;

import blackboard.Blackboard;
import model.Estadia;
import model.Ocupacao;
import model.Setor;
import model.Vaga;
import util.Formatador;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Mapa de um setor: uma grade de botões, um por vaga (verde = livre, vermelho = ocupada,
 * azul com contorno = preferencial livre), o resumo de vagas livres e os veículos circulando.
 * Só lê o Blackboard; o clique numa vaga é repassado à janela.
 */
public class PainelSetor {

    private final Blackboard blackboard;
    private final Setor setor;
    private final JPanel painel = new JPanel(new BorderLayout(6, 6));
    private final JLabel resumo = new JLabel();
    private final JLabel circulando = new JLabel();
    private final Map<String, JButton> botoes = new LinkedHashMap<>();

    PainelSetor(Blackboard blackboard, Setor setor, BiConsumer<Setor, String> aoClicar) {
        this.blackboard = blackboard;
        this.setor = setor;

        List<Vaga> vagas = blackboard.getVagas(setor);
        List<Character> fileiras = new ArrayList<>();
        int colunas = 0;
        for (Vaga v : vagas) {
            if (!fileiras.contains(v.getFileira())) fileiras.add(v.getFileira());
            colunas = Math.max(colunas, v.getPosicao());
        }
        JPanel grade = new JPanel(new GridLayout(fileiras.size(), colunas, 6, 6));
        for (Vaga v : vagas) {
            String id = v.getId();
            JButton botao = new JButton();
            botao.setFocusPainted(false);
            botao.setPreferredSize(new Dimension(96, 50));
            botao.addActionListener(e -> aoClicar.accept(setor, id));
            botoes.put(id, botao);
            grade.add(botao);
        }

        painel.setBorder(BorderFactory.createTitledBorder("SETOR " + setor.descricao().toUpperCase()
                + (setor == Setor.SUBSOLO ? " (coberto)" : "")));
        painel.add(resumo, BorderLayout.NORTH);
        painel.add(grade, BorderLayout.CENTER);
        painel.add(circulando, BorderLayout.SOUTH);
        atualizar();
    }

    JComponent componente() {
        return painel;
    }

    /** Relê o Blackboard e redesenha as vagas e os resumos. As vagas são buscadas pelo id, porque reiniciar o Blackboard cria vagas novas. */
    void atualizar() {
        for (Map.Entry<String, JButton> item : botoes.entrySet()) {
            Vaga v = blackboard.getVaga(setor, item.getKey());
            JButton botao = item.getValue();
            botao.setText(textoDaVaga(v));
            botao.setBackground(corDaVaga(v));
            botao.setToolTipText(dicaDaVaga(v));
            botao.setBorder(v.isPreferencial()
                    ? BorderFactory.createLineBorder(Componentes.AZUL_ESCURO, 3)
                    : BorderFactory.createLineBorder(Color.GRAY, 1));
        }
        resumo.setText(textoDoResumo());
        circulando.setText(textoDosCirculando());
    }

    /** Texto do botão: o id (com "P" se for preferencial) e, embaixo, "livre" ou a placa de quem ocupa. */
    public static String textoDaVaga(Vaga v) {
        String id = v.getId() + (v.isPreferencial() ? " P" : "");
        Ocupacao o = v.getOcupacaoAtual();
        String embaixo = v.isLivre() ? "livre" : o == null ? "ocupada" : o.getPlaca();
        return "<html><center><b>" + id + "</b><br>" + embaixo + "</center></html>";
    }

    public static Color corDaVaga(Vaga v) {
        if (!v.isLivre()) return Componentes.VERMELHO;
        return v.isPreferencial() ? Componentes.AZUL : Componentes.VERDE;
    }

    public static String dicaDaVaga(Vaga v) {
        String tipo = v.isPreferencial() ? "preferencial" : "comum";
        if (v.isLivre()) return "Vaga " + v.getId() + " (" + tipo + "), livre: clique para ocupar";
        Ocupacao o = v.getOcupacaoAtual();
        String quem = o == null ? "" : " por " + o.getPlaca();
        return "Vaga " + v.getId() + " (" + tipo + "), ocupada" + quem + ": clique para desocupar";
    }

    private String textoDoResumo() {
        String texto = "Livres: " + blackboard.vagasLivres(setor) + " de " + blackboard.totalVagas(setor);
        int preferenciais = blackboard.totalPreferenciais(setor);
        if (preferenciais > 0) {
            texto += "   (comuns " + blackboard.vagasLivresComuns(setor) + "/" + blackboard.totalComuns(setor)
                    + ", preferenciais " + blackboard.vagasLivresPreferenciais(setor) + "/" + preferenciais + ")";
        }
        return texto;
    }

    private String textoDosCirculando() {
        List<Estadia> lista = blackboard.getVeiculosCirculando(setor);
        if (lista.isEmpty()) return "Circulando no setor: nenhum veículo";
        LocalDateTime agora = LocalDateTime.now();
        List<String> itens = new ArrayList<>();
        for (Estadia e : lista) {
            itens.add(e.getPlaca() + " (há " + Formatador.duracao(Duration.between(e.getUltimoEvento(), agora)) + ")");
        }
        return "<html>Circulando no setor: " + String.join(", ", itens) + "</html>";
    }
}
