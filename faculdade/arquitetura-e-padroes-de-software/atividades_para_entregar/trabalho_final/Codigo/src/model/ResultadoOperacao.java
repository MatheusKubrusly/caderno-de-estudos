package model;

/**
 * Resultado publicado no Blackboard; o Display apenas renderiza.
 * Guarda quem respondeu (o especialista ou o controlador) e a requisição que o gerou.
 */
public class ResultadoOperacao {
    private final boolean sucesso;
    private final String origem;
    private final Requisicao requisicao;
    private final String mensagem;

    private ResultadoOperacao(boolean sucesso, String origem, Requisicao requisicao, String mensagem) {
        this.sucesso = sucesso;
        this.origem = origem;
        this.requisicao = requisicao;
        this.mensagem = mensagem;
    }

    public static ResultadoOperacao ok(String origem, Requisicao requisicao, String mensagem) {
        return new ResultadoOperacao(true, origem, requisicao, mensagem);
    }

    public static ResultadoOperacao erro(String origem, Requisicao requisicao, String mensagem) {
        return new ResultadoOperacao(false, origem, requisicao, mensagem);
    }

    public boolean isSucesso() { return sucesso; }
    public String getOrigem() { return origem; }
    public Requisicao getRequisicao() { return requisicao; }
    public String getMensagem() { return mensagem; }
}
