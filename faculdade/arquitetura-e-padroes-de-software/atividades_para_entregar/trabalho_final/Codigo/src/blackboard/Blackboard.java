package blackboard;

import model.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Memória compartilhada da arquitetura Blackboard.
 *
 * Padrões: Singleton (instância única) e Subject do Observer.
 * Todo o estado de domínio vive aqui: vagas, estadias (o histórico), requisição pendente,
 * último resultado e último relatório. Os KS são stateless.
 */
public final class Blackboard {

    private static Blackboard instancia;

    private final Map<Setor, Map<String, Vaga>> vagas = new EnumMap<>(Setor.class);
    private final List<Estadia> estadias = new ArrayList<>();
    private final List<Observer> observers = new ArrayList<>();

    private Requisicao requisicaoPendente;
    private ResultadoOperacao ultimoResultado;
    private Relatorio ultimoRelatorio;
    private int lotesAbertos;            // > 0 enquanto executarEmLote() roda

    private Blackboard() {
        popularVagas();
    }

    public static synchronized Blackboard getInstance() {
        if (instancia == null) {
            instancia = new Blackboard();
        }
        return instancia;
    }

    /** Configuração fixa: EXTERNO A1–C5 (15 comuns); SUBSOLO A1–B5 (10), com A1 e A2 preferenciais. */
    private void popularVagas() {
        Map<String, Vaga> externo = new LinkedHashMap<>();
        for (char f = 'A'; f <= 'C'; f++) {
            for (int p = 1; p <= 5; p++) {
                Vaga v = new Vaga(f, p, TipoVaga.COMUM);
                externo.put(v.getId(), v);
            }
        }
        Map<String, Vaga> subsolo = new LinkedHashMap<>();
        for (char f = 'A'; f <= 'B'; f++) {
            for (int p = 1; p <= 5; p++) {
                TipoVaga tipo = (f == 'A' && p <= 2) ? TipoVaga.PREFERENCIAL : TipoVaga.COMUM;
                Vaga v = new Vaga(f, p, tipo);
                subsolo.put(v.getId(), v);
            }
        }
        vagas.put(Setor.EXTERNO, externo);
        vagas.put(Setor.SUBSOLO, subsolo);
    }

    // ------------------------------------------------------------------ ciclo

    public synchronized Requisicao getRequisicaoPendente() {
        return requisicaoPendente;
    }

    /** Não notifica observers: evita telas intermediárias. */
    public synchronized void publicarRequisicao(Requisicao r) {
        this.requisicaoPendente = r;
    }

    public synchronized void consumirRequisicao() {
        this.requisicaoPendente = null;
    }

    // ---------------------------------------------------------------- leitura

    public synchronized Vaga getVaga(Setor setor, String idVaga) {
        if (setor == null || idVaga == null) return null;
        return vagas.get(setor).get(idVaga.trim().toUpperCase());
    }

    public synchronized List<Vaga> getVagas(Setor setor) {
        return Collections.unmodifiableList(new ArrayList<>(vagas.get(setor).values()));
    }

    public synchronized List<Estadia> getEstadias() {
        return Collections.unmodifiableList(new ArrayList<>(estadias));
    }

    public synchronized List<Estadia> buscarPorPlaca(String placa) {
        List<Estadia> r = new ArrayList<>();
        for (Estadia e : estadias) {
            if (e.getPlaca().equalsIgnoreCase(placa.trim())) r.add(e);
        }
        return Collections.unmodifiableList(r);
    }

    /** Filtra pela data/hora de ENTRADA dentro de [inicio, fim]. */
    public synchronized List<Estadia> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        List<Estadia> r = new ArrayList<>();
        for (Estadia e : estadias) {
            if (!e.getEntrada().isBefore(inicio) && !e.getEntrada().isAfter(fim)) r.add(e);
        }
        return Collections.unmodifiableList(r);
    }

    /** Estadia em andamento da placa, em qualquer setor, ou null se o veículo está fora. */
    public synchronized Estadia getEstadiaAberta(String placa) {
        if (placa == null) return null;
        for (Estadia e : estadias) {
            if (e.isAberta() && e.getPlaca().equalsIgnoreCase(placa.trim())) return e;
        }
        return null;
    }

    /** Veículos que entraram no setor e não estão numa vaga (ainda não estacionaram ou já desocuparam). */
    public synchronized List<Estadia> getVeiculosCirculando(Setor setor) {
        List<Estadia> r = new ArrayList<>();
        for (Estadia e : estadias) {
            if (e.getSetor() == setor && e.getSituacao() == SituacaoVeiculo.CIRCULANDO) r.add(e);
        }
        return Collections.unmodifiableList(r);
    }

    public synchronized ResultadoOperacao getUltimoResultado() {
        return ultimoResultado;
    }

    public synchronized Relatorio getUltimoRelatorio() {
        return ultimoRelatorio;
    }

    // ------------------------------------------------- contadores derivados

    public synchronized int totalVagas(Setor setor) {
        return vagas.get(setor).size();
    }

    public synchronized int vagasOcupadas(Setor setor) {
        return (int) vagas.get(setor).values().stream().filter(v -> !v.isLivre()).count();
    }

    public synchronized int vagasLivres(Setor setor) {
        return totalVagas(setor) - vagasOcupadas(setor);
    }

    public synchronized int totalComuns(Setor setor) {
        return (int) vagas.get(setor).values().stream().filter(v -> !v.isPreferencial()).count();
    }

    public synchronized int totalPreferenciais(Setor setor) {
        return totalVagas(setor) - totalComuns(setor);
    }

    public synchronized int vagasLivresComuns(Setor setor) {
        return (int) vagas.get(setor).values().stream()
                .filter(v -> v.isLivre() && !v.isPreferencial()).count();
    }

    public synchronized int vagasLivresPreferenciais(Setor setor) {
        return (int) vagas.get(setor).values().stream()
                .filter(v -> v.isLivre() && v.isPreferencial()).count();
    }

    // ------------------------------------- escrita atômica (uma por evento)

    public synchronized void registrarEntrada(Estadia estadia) {
        estadias.add(estadia);
    }

    public synchronized void registrarOcupacao(Estadia estadia, Vaga vaga, Ocupacao ocupacao) {
        estadia.adicionarOcupacao(ocupacao);
        vaga.setStatus(StatusVaga.OCUPADA);
        vaga.setEstadiaAtual(estadia);
    }

    public synchronized void registrarDesocupacao(Vaga vaga, LocalDateTime fim) {
        vaga.getEstadiaAtual().getOcupacaoAtual().setFim(fim);
        vaga.setStatus(StatusVaga.LIVRE);
        vaga.setEstadiaAtual(null);
    }

    public synchronized void registrarSaida(Estadia estadia, LocalDateTime saida) {
        estadia.setSaida(saida);
    }

    // ------------------------------------------------ publicação de resultados

    public void publicarResultado(ResultadoOperacao resultado) {
        synchronized (this) {
            this.ultimoResultado = resultado;
        }
        notificarObservers();
    }

    public void publicarRelatorio(Relatorio relatorio) {
        synchronized (this) {
            this.ultimoRelatorio = relatorio;
        }
        notificarObservers();
    }

    // ------------------------------------------------------------- manutenção

    /** Volta ao estado inicial: todas as vagas livres e nenhuma estadia. */
    public void reiniciar() {
        synchronized (this) {
            vagas.clear();
            popularVagas();
            estadias.clear();
            requisicaoPendente = null;
            ultimoResultado = null;
            ultimoRelatorio = null;
        }
        notificarObservers();
    }

    /**
     * Executa várias requisições seguidas (por exemplo, ao reler o registro de eventos)
     * sem notificar os observers a cada uma; eles são avisados uma vez, no fim.
     */
    public void executarEmLote(Runnable tarefa) {
        synchronized (this) {
            lotesAbertos++;
        }
        try {
            tarefa.run();
        } finally {
            synchronized (this) {
                lotesAbertos--;
            }
        }
        notificarObservers();
    }

    // ---------------------------------------------------------------- observer

    public synchronized void adicionarObserver(Observer o) {
        observers.add(o);
    }

    private void notificarObservers() {
        List<Observer> copia;
        synchronized (this) {
            if (lotesAbertos > 0) return;
            copia = new ArrayList<>(observers);
        }
        for (Observer o : copia) {
            o.atualizar();
        }
    }
}
