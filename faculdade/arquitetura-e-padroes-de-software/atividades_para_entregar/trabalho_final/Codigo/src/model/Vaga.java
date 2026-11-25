package model;

public class Vaga {
    private final String id;        // ex.: A1
    private final char fileira;
    private final int posicao;
    private final TipoVaga tipo;
    private StatusVaga status = StatusVaga.LIVRE;
    private Ocupacao ocupacaoAtual;      // ocupação em andamento, ou null se livre

    public Vaga(char fileira, int posicao, TipoVaga tipo) {
        this.fileira = fileira;
        this.posicao = posicao;
        this.tipo = tipo;
        this.id = "" + fileira + posicao;
    }

    public String getId() { return id; }
    public char getFileira() { return fileira; }
    public int getPosicao() { return posicao; }
    public void setStatus(StatusVaga status) { this.status = status; }
    public Ocupacao getOcupacaoAtual() { return ocupacaoAtual; }
    public void setOcupacaoAtual(Ocupacao ocupacaoAtual) { this.ocupacaoAtual = ocupacaoAtual; }

    public boolean isLivre() {
        return status == StatusVaga.LIVRE;
    }

    public boolean isPreferencial() {
        return tipo == TipoVaga.PREFERENCIAL;
    }
}
