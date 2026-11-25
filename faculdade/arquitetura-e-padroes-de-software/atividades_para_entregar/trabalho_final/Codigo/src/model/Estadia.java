package model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Passagem de um veículo por um setor, da entrada pela cancela até a saída.
 * Guarda as ocupações de vaga feitas nesse meio-tempo (pode haver mais de uma).
 * Não guarda o tipo da vaga: ele é derivado de {@link Vaga} a partir de setor + idVaga.
 */
public class Estadia {
    private final String placa;
    private final Setor setor;
    private final Double altura;            // null quando o setor não pede
    private final LocalDateTime entrada;
    private LocalDateTime saida;            // null = veículo ainda no setor
    private final List<Ocupacao> ocupacoes = new ArrayList<>();

    public Estadia(String placa, Setor setor, Double altura, LocalDateTime entrada) {
        this.placa = placa;
        this.setor = setor;
        this.altura = altura;
        this.entrada = entrada;
    }

    public String getPlaca() { return placa; }
    public Setor getSetor() { return setor; }
    public Double getAltura() { return altura; }
    public LocalDateTime getEntrada() { return entrada; }
    public LocalDateTime getSaida() { return saida; }
    public void setSaida(LocalDateTime saida) { this.saida = saida; }
    public List<Ocupacao> getOcupacoes() { return Collections.unmodifiableList(ocupacoes); }
    public void adicionarOcupacao(Ocupacao ocupacao) { ocupacoes.add(ocupacao); }

    public boolean isAberta() {
        return saida == null;
    }

    /** Ocupação em andamento, ou null se o veículo está circulando ou já saiu. */
    public Ocupacao getOcupacaoAtual() {
        if (ocupacoes.isEmpty()) return null;
        Ocupacao ultima = ocupacoes.get(ocupacoes.size() - 1);
        return ultima.isAberta() ? ultima : null;
    }

    /** Situação calculada a partir das ocupações; null se a estadia já terminou. */
    public SituacaoVeiculo getSituacao() {
        if (!isAberta()) return null;
        return getOcupacaoAtual() != null ? SituacaoVeiculo.ESTACIONADO : SituacaoVeiculo.CIRCULANDO;
    }

    /** Horário do evento mais recente desta estadia: entrada, ocupação, desocupação ou saída. */
    public LocalDateTime getUltimoEvento() {
        LocalDateTime ultimo = entrada;
        for (Ocupacao o : ocupacoes) {
            ultimo = maisRecente(ultimo, o.getInicio());
            if (o.getFim() != null) ultimo = maisRecente(ultimo, o.getFim());
        }
        return saida != null ? maisRecente(ultimo, saida) : ultimo;
    }

    /** "A3", ou "A3→B2" quando o veículo trocou de vaga; "—" se não chegou a estacionar. */
    public String getVagas() {
        if (ocupacoes.isEmpty()) return "—";
        return ocupacoes.stream().map(Ocupacao::getIdVaga).collect(Collectors.joining("→"));
    }

    /** Primeiro motivo preferencial informado na estadia, ou null. */
    public MotivoPreferencial getMotivo() {
        for (Ocupacao o : ocupacoes) {
            if (o.getMotivo() != null) return o.getMotivo();
        }
        return null;
    }

    private static LocalDateTime maisRecente(LocalDateTime a, LocalDateTime b) {
        return b.isAfter(a) ? b : a;
    }
}
