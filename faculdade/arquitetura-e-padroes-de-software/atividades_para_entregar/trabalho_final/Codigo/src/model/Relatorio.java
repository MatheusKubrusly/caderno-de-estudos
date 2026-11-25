package model;

import java.util.List;

/** Relatório pronto para exibição (snapshot): título + linhas formatadas. */
public class Relatorio {
    private final TipoRelatorio tipo;
    private final String titulo;
    private final List<String> linhas;

    public Relatorio(TipoRelatorio tipo, String titulo, List<String> linhas) {
        this.tipo = tipo;
        this.titulo = titulo;
        this.linhas = List.copyOf(linhas);
    }

    public TipoRelatorio getTipo() { return tipo; }
    public String getTitulo() { return titulo; }
    public List<String> getLinhas() { return linhas; }
}
