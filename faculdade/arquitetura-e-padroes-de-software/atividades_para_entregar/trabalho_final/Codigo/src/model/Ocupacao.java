package model;

import java.time.LocalDateTime;

/** Período em que um veículo ficou numa vaga. Fim null = o veículo ainda está na vaga. */
public class Ocupacao {
    private final String idVaga;
    private final LocalDateTime inicio;
    private LocalDateTime fim;
    private final MotivoPreferencial motivo;   // só em vaga preferencial; null nas comuns

    public Ocupacao(String idVaga, LocalDateTime inicio, MotivoPreferencial motivo) {
        this.idVaga = idVaga;
        this.inicio = inicio;
        this.motivo = motivo;
    }

    public String getIdVaga() { return idVaga; }
    public LocalDateTime getInicio() { return inicio; }
    public LocalDateTime getFim() { return fim; }
    public void setFim(LocalDateTime fim) { this.fim = fim; }
    public MotivoPreferencial getMotivo() { return motivo; }

    public boolean isAberta() {
        return fim == null;
    }
}
