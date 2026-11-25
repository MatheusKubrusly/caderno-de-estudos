package model;

/** Resultado publicado pelo KS no Blackboard; o Display apenas renderiza. */
public class ResultadoOperacao {
    private final boolean sucesso;
    private final String mensagem;

    private ResultadoOperacao(boolean sucesso, String mensagem) {
        this.sucesso = sucesso;
        this.mensagem = mensagem;
    }

    public static ResultadoOperacao ok(String mensagem) {
        return new ResultadoOperacao(true, mensagem);
    }

    public static ResultadoOperacao erro(String mensagem) {
        return new ResultadoOperacao(false, mensagem);
    }

    public boolean isSucesso() { return sucesso; }
    public String getMensagem() { return mensagem; }
}
