package model;

/** Setores do estacionamento. */
public enum Setor {
    EXTERNO("Externo"),
    SUBSOLO("Subsolo");

    private final String descricao;

    Setor(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
