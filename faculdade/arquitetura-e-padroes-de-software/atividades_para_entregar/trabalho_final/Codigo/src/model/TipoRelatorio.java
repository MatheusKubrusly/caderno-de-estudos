package model;

/** Qual relatório a gerência pediu. A ordem e os rótulos seguem os menus dos front-ends. */
public enum TipoRelatorio {
    OCUPACAO("Ocupação por setor"),
    HISTORICO_GERAL("Histórico geral"),
    HISTORICO_POR_PLACA("Histórico por placa"),
    HISTORICO_POR_PERIODO("Histórico por período"),
    TEMPO_MEDIO("Tempo médio de permanência"),
    PREFERENCIAIS("Uso de vagas preferenciais"),
    HORARIOS_DE_PICO("Horários de pico"),
    CLIENTES_FREQUENTES("Clientes frequentes"),
    GIRO_POR_VAGA("Giro por vaga"),
    TEMPO_ATE_ESTACIONAR("Tempo até estacionar"),
    /** Seções de todos os relatórios em um único documento. */
    COMPLETO("Relatório completo (todos os relatórios)");

    private final String rotulo;

    TipoRelatorio(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }
}
