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
 * Todo o estado de domínio vive aqui: vagas, histórico, requisição pendente,
 * último resultado e último relatório. Os KS são stateless.
 */
public final class Blackboard {

    private static Blackboard instancia;

    private final Map<Setor, Map<String, Vaga>> vagas = new EnumMap<>(Setor.class);
    private final List<RegistroLog> historico = new ArrayList<>();
    private final List<Observer> observers = new ArrayList<>();

    private Requisicao requisicaoPendente;
    private ResultadoOperacao ultimoResultado;
    private Relatorio ultimoRelatorio;

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

    public synchronized List<RegistroLog> getHistorico() {
        return Collections.unmodifiableList(new ArrayList<>(historico));
    }

    public synchronized List<RegistroLog> buscarPorPlaca(String placa) {
        List<RegistroLog> r = new ArrayList<>();
        for (RegistroLog l : historico) {
            if (l.getPlaca().equalsIgnoreCase(placa.trim())) r.add(l);
        }
        return Collections.unmodifiableList(r);
    }

    /** Filtra pela data/hora de ENTRADA dentro de [inicio, fim]. */
    public synchronized List<RegistroLog> buscarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        List<RegistroLog> r = new ArrayList<>();
        for (RegistroLog l : historico) {
            if (!l.getEntrada().isBefore(inicio) && !l.getEntrada().isAfter(fim)) r.add(l);
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

    // -------------------------------------------- escrita atômica (vaga + log)

    public synchronized void registrarEntrada(Vaga vaga, RegistroLog log) {
        vaga.setStatus(StatusVaga.OCUPADA);
        vaga.setRegistroAtual(log);
        historico.add(log);
    }

    public synchronized void registrarSaida(Vaga vaga, LocalDateTime saida) {
        vaga.getRegistroAtual().setSaida(saida);
        vaga.setStatus(StatusVaga.LIVRE);
        vaga.setRegistroAtual(null);
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

    // ---------------------------------------------------------------- observer

    public synchronized void adicionarObserver(Observer o) {
        observers.add(o);
    }

    private void notificarObservers() {
        List<Observer> copia;
        synchronized (this) {
            copia = new ArrayList<>(observers);
        }
        for (Observer o : copia) {
            o.atualizar();
        }
    }
}
