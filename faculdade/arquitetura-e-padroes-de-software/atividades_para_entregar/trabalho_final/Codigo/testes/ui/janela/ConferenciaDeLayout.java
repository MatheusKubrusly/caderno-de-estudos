package ui.janela;

import model.TipoRelatorio;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;

/**
 * Conferências de layout da janela que rodam sem abrir a tela, usadas pelo SmokeTest.
 * Fica no pacote da janela para enxergar os painéis sem que eles precisem ser públicos.
 */
public final class ConferenciaDeLayout {

    private ConferenciaDeLayout() {}

    /**
     * Em cada tipo de relatório, o botão Gerar cabe inteiro na largura dada.
     * Origem: no teste gravado de 25/11/2026 (J15), o botão sumia com "Histórico por período" numa tela de 1366 px.
     */
    public static boolean botaoGerarSempreVisivel(int largura) {
        PainelRelatorios painel = new PainelRelatorios(() -> { });
        JComponent raiz = painel.componente();
        for (TipoRelatorio relatorio : TipoRelatorio.values()) {
            painel.escolher(relatorio);
            raiz.setSize(largura, 500);
            arrumar(raiz);
            JButton gerar = painel.botaoGerar();
            if (!gerar.isVisible() || !inteiroDentro(gerar, raiz)) return false;
        }
        return true;
    }

    /**
     * O componente cabe inteiro, na largura e na altura, em cada contêiner até a raiz.
     * Só olhar a largura não basta: o FlowLayout joga o que sobra para uma segunda fileira,
     * que fica cortada embaixo quando o contêiner só reserva altura para uma.
     */
    private static boolean inteiroDentro(Component componente, Component raiz) {
        for (Component atual = componente; atual != raiz; atual = atual.getParent()) {
            Container pai = atual.getParent();
            if (pai == null) return false;
            Rectangle r = atual.getBounds();
            if (r.width == 0 || r.height == 0 || r.x < 0 || r.y < 0
                    || r.x + r.width > pai.getWidth() || r.y + r.height > pai.getHeight()) {
                return false;
            }
        }
        return true;
    }

    /** Fora de uma janela aberta o Swing não posiciona nada sozinho: arruma cada nível, de cima para baixo. */
    private static void arrumar(Component componente) {
        if (componente instanceof Container k) {
            k.doLayout();
            for (Component filho : k.getComponents()) {
                arrumar(filho);
            }
        }
    }
}
