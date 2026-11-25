package model;

/**
 * Motivo para uso de vaga preferencial.
 * Não existe valor "NENHUM": a ausência de motivo é representada por null.
 */
public enum MotivoPreferencial {
    PCD("Pessoa com Deficiência (PCD)", "PCD"),
    TEA("Pessoa com Transtorno do Espectro Autista (TEA)", "TEA"),
    IDOSO("Idoso (60 anos ou mais)", "Idoso"),
    GESTANTE("Gestante", "Gestante"),
    LACTANTE("Lactante", "Lactante"),
    CRIANCA_COLO("Pessoa com criança de colo", "Criança de colo");

    private final String descricao;
    private final String rotulo;

    MotivoPreferencial(String descricao, String rotulo) {
        this.descricao = descricao;
        this.rotulo = rotulo;
    }

    public String descricao() {
        return descricao;
    }

    public String rotulo() {
        return rotulo;
    }
}
