package model;

import java.time.LocalDateTime;

/**
 * Registro histórico de uma estadia. Não guarda o tipo da vaga:
 * ele é derivado de {@link Vaga} a partir de setor + idVaga.
 */
public class RegistroLog {
    private final String placa;
    private final Setor setor;
    private final String idVaga;
    private final LocalDateTime entrada;
    private LocalDateTime saida;            // null = estadia em aberto
    private final MotivoPreferencial motivo;

    public RegistroLog(String placa, Setor setor, String idVaga,
                       LocalDateTime entrada, MotivoPreferencial motivo) {
        this.placa = placa;
        this.setor = setor;
        this.idVaga = idVaga;
        this.entrada = entrada;
        this.motivo = motivo;
    }

    public String getPlaca() { return placa; }
    public Setor getSetor() { return setor; }
    public String getIdVaga() { return idVaga; }
    public LocalDateTime getEntrada() { return entrada; }
    public LocalDateTime getSaida() { return saida; }
    public void setSaida(LocalDateTime saida) { this.saida = saida; }
    public MotivoPreferencial getMotivo() { return motivo; }

    public boolean isAberto() {
        return saida == null;
    }
}
