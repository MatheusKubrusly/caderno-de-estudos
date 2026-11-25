package ui.janela;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ListCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.function.Function;

/** Peças pequenas reaproveitadas pelos painéis da janela: cores, fonte e rótulos das caixas de escolha. */
final class Componentes {

    static final Color VERDE = new Color(0xC8E6C9);
    static final Color VERMELHO = new Color(0xFFCDD2);
    static final Color AZUL = new Color(0xBBDEFB);
    static final Color AZUL_ESCURO = new Color(0x1565C0);
    static final Color TEXTO_OK = new Color(0x1B5E20);
    static final Color TEXTO_AVISO = new Color(0xB71C1C);
    static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    private Componentes() {}

    /** Desenhista de itens que mostra, para cada item do tipo pedido, o texto calculado pela função. */
    static <T> ListCellRenderer<Object> rotulo(Class<T> tipo, Function<T, String> texto) {
        DefaultListCellRenderer padrao = new DefaultListCellRenderer();
        return (lista, valor, indice, selecionado, comFoco) -> {
            Component c = padrao.getListCellRendererComponent(lista, valor, indice, selecionado, comFoco);
            if (tipo.isInstance(valor)) padrao.setText(texto.apply(tipo.cast(valor)));
            return c;
        };
    }
}
