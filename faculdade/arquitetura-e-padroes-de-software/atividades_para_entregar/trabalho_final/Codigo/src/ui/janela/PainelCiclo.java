package ui.janela;

import model.Requisicao;
import model.ResultadoOperacao;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro, na tela, de cada ciclo do Blackboard: a requisição publicada, quem respondeu
 * (o especialista ou o controlador) e o resultado. Mostra o controle em funcionamento.
 */
public class PainelCiclo {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JPanel painel = new JPanel(new BorderLayout(4, 4));
    private final JTextArea texto = new JTextArea();

    PainelCiclo() {
        texto.setEditable(false);
        texto.setFont(Componentes.MONO);
        texto.setLineWrap(true);
        texto.setWrapStyleWord(true);
        painel.add(new JLabel("  hora · requisição publicada → quem respondeu · resultado"), BorderLayout.NORTH);
        painel.add(new JScrollPane(texto), BorderLayout.CENTER);
    }

    JComponent componente() {
        return painel;
    }

    void registrar(ResultadoOperacao resultado) {
        acrescentar(linha(resultado));
    }

    /** Anotação que não é um ciclo (por exemplo, "cenário carregado"). */
    void anotar(String nota) {
        acrescentar(LocalTime.now().format(HORA) + "  ── " + nota);
    }

    private void acrescentar(String linha) {
        texto.append(linha + "\n");
        texto.setCaretPosition(texto.getDocument().getLength());
    }

    /** Uma linha do registro, como "14:02:10  OCUPAR SUBSOLO ABC1D23 A1 → SubsoloKS  ✓ Veículo ...". */
    public static String linha(ResultadoOperacao r) {
        Requisicao q = r.getRequisicao();
        String hora = (q != null && q.getMomento() != null ? q.getMomento().toLocalTime() : LocalTime.now()).format(HORA);
        return hora + "  " + descrever(q) + " → " + r.getOrigem() + "  " + (r.isSucesso() ? "✓ " : "⚠ ")
                + r.getMensagem().replaceAll("\\s*\\n\\s*", " · ");
    }

    private static String descrever(Requisicao q) {
        if (q == null || q.getTipo() == null) return "(requisição incompleta)";
        List<String> partes = new ArrayList<>();
        partes.add(q.getTipo().name());
        if (q.getTipoRelatorio() != null) partes.add(q.getTipoRelatorio().name());
        if (q.getSetor() != null) partes.add(q.getSetor().name());
        if (q.getPlaca() != null) partes.add(q.getPlaca());
        if (q.getIdVaga() != null) partes.add(q.getIdVaga());
        return String.join(" ", partes);
    }
}
