package aplicacao;

import blackboard.Blackboard;
import blackboard.BlackboardController;
import model.*;

import java.time.LocalDateTime;

/**
 * Fachada (Facade) do estacionamento para os front-ends e para a releitura do registro.
 *
 * Monta a requisição, publica no Blackboard, roda o ciclo do controlador e devolve o
 * resultado. Não decide nenhuma regra: quem aceita ou recusa são os especialistas.
 */
public class FachadaEstacionamento {

    private final Blackboard blackboard;
    private final BlackboardController controlador;

    public FachadaEstacionamento(Blackboard blackboard, BlackboardController controlador) {
        this.blackboard = blackboard;
        this.controlador = controlador;
    }

    public ResultadoOperacao entrar(Setor setor, String placa, Double altura) {
        return enviar(new Requisicao().setTipo(TipoRequisicao.ENTRAR)
                .setSetor(setor).setPlaca(placa).setAltura(altura));
    }

    public ResultadoOperacao ocupar(Setor setor, String placa, String idVaga, MotivoPreferencial motivo) {
        return enviar(new Requisicao().setTipo(TipoRequisicao.OCUPAR)
                .setSetor(setor).setPlaca(placa).setIdVaga(idVaga).setMotivo(motivo));
    }

    public ResultadoOperacao desocupar(Setor setor, String idVaga) {
        return enviar(new Requisicao().setTipo(TipoRequisicao.DESOCUPAR).setSetor(setor).setIdVaga(idVaga));
    }

    public ResultadoOperacao sair(Setor setor, String placa) {
        return enviar(new Requisicao().setTipo(TipoRequisicao.SAIR).setSetor(setor).setPlaca(placa));
    }

    /** Relatório pedido, ou null se o especialista recusou (o motivo fica no último resultado do Blackboard). */
    public Relatorio gerarRelatorio(TipoRelatorio tipo, String placa, LocalDateTime inicio, LocalDateTime fim) {
        Relatorio anterior = blackboard.getUltimoRelatorio();
        enviar(new Requisicao().setTipo(TipoRequisicao.RELATORIO).setTipoRelatorio(tipo)
                .setPlaca(placa).setInicio(inicio).setFim(fim));
        Relatorio atual = blackboard.getUltimoRelatorio();
        return atual != anterior ? atual : null;
    }

    /**
     * Publica a requisição e roda o ciclo do controlador; sem horário, usa o de agora.
     * Devolve a resposta a esta requisição, ou null se ela não gerou um resultado
     * (um relatório aceito, por exemplo, gera um Relatorio).
     */
    public ResultadoOperacao enviar(Requisicao requisicao) {
        if (requisicao.getMomento() == null) requisicao.setMomento(LocalDateTime.now());
        blackboard.publicarRequisicao(requisicao);
        controlador.executarCiclo();
        ResultadoOperacao r = blackboard.getUltimoResultado();
        return r != null && r.getRequisicao() == requisicao ? r : null;
    }
}
