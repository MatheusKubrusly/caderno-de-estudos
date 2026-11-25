package model;

/**
 * O que o front-end pede ao Blackboard. Os quatro primeiros são os eventos
 * de um veículo num setor: entrar pela cancela, ocupar uma vaga, desocupá-la e sair.
 */
public enum TipoRequisicao {
    ENTRAR,
    OCUPAR,
    DESOCUPAR,
    SAIR,
    RELATORIO;

    /** Eventos que mudam o estado do estacionamento. */
    public boolean isEvento() {
        return this != RELATORIO;
    }
}
