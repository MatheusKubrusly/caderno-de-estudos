package model;

/** Qual relatório a gerência pediu. A ordem segue o menu da CLI. */
public enum TipoRelatorio {
    OCUPACAO,
    HISTORICO_GERAL,
    HISTORICO_POR_PLACA,
    HISTORICO_POR_PERIODO,
    TEMPO_MEDIO,
    PREFERENCIAIS,
    /** Seções de todos os relatórios em um único documento. */
    COMPLETO
}
