package model;

import java.time.LocalDateTime;

/**
 * Item de trabalho publicado no Blackboard (DTO, não é Command).
 * Campos não aplicáveis ao tipo ficam null.
 */
public class Requisicao {
    private TipoRequisicao tipo;
    private Setor setor;
    private String placa;
    private Double altura;
    private String idVaga;
    private MotivoPreferencial motivo;
    private TipoRelatorio tipoRelatorio;
    private LocalDateTime inicio;
    private LocalDateTime fim;

    public TipoRequisicao getTipo() { return tipo; }
    public Setor getSetor() { return setor; }
    public String getPlaca() { return placa; }
    public Double getAltura() { return altura; }
    public String getIdVaga() { return idVaga; }
    public MotivoPreferencial getMotivo() { return motivo; }
    public TipoRelatorio getTipoRelatorio() { return tipoRelatorio; }
    public LocalDateTime getInicio() { return inicio; }
    public LocalDateTime getFim() { return fim; }

    public Requisicao setTipo(TipoRequisicao tipo) { this.tipo = tipo; return this; }
    public Requisicao setSetor(Setor setor) { this.setor = setor; return this; }
    public Requisicao setPlaca(String placa) { this.placa = placa; return this; }
    public Requisicao setAltura(Double altura) { this.altura = altura; return this; }
    public Requisicao setIdVaga(String idVaga) { this.idVaga = idVaga; return this; }
    public Requisicao setMotivo(MotivoPreferencial motivo) { this.motivo = motivo; return this; }
    public Requisicao setTipoRelatorio(TipoRelatorio tipoRelatorio) { this.tipoRelatorio = tipoRelatorio; return this; }
    public Requisicao setInicio(LocalDateTime inicio) { this.inicio = inicio; return this; }
    public Requisicao setFim(LocalDateTime fim) { this.fim = fim; return this; }
}
