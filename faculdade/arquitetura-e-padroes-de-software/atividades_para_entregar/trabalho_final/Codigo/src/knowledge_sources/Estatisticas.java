package knowledge_sources;

import model.Estadia;
import model.Ocupacao;
import model.Setor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Cálculos sobre as estadias usados nos relatórios gerenciais: médias, contagens,
 * lotação, giro por vaga e clientes que voltaram. Só calcula, a partir das listas
 * que recebe; quem lê o Blackboard e monta o texto é o especialista de relatórios.
 * Não é um especialista: não participa do ciclo do controlador.
 */
final class Estatisticas {

    /** Maior número de veículos estacionados ao mesmo tempo, e quando isso aconteceu. */
    record Lotacao(int veiculos, LocalDateTime quando) {}

    /** Trecho de tempo coberto pelos dados. */
    record Periodo(LocalDateTime inicio, LocalDateTime fim) {
        Duration duracao() {
            return Duration.between(inicio, fim);
        }
    }

    /** Quantas vezes uma vaga foi ocupada e por quanto tempo, somando as ocupações. */
    record UsoDeVaga(long ocupacoes, Duration tempoOcupado) {}

    /** Placa com mais de uma visita. */
    record Cliente(String placa, List<Estadia> visitas, LocalDateTime ultimaVisita) {}

    private Estatisticas() {}

    // ------------------------------------------------------------------ médias

    static Duration media(List<Duration> duracoes) {
        long segundos = 0;
        for (Duration d : duracoes) {
            segundos += d.getSeconds();
        }
        return Duration.ofSeconds(segundos / duracoes.size());
    }

    /** Média da entrada até a saída; recebe só estadias encerradas. */
    static Duration mediaPermanencia(List<Estadia> fechadas) {
        List<Duration> duracoes = new ArrayList<>();
        for (Estadia e : fechadas) {
            duracoes.add(Duration.between(e.getEntrada(), e.getSaida()));
        }
        return media(duracoes);
    }

    /** Tempo da cancela até a primeira vaga, de cada estadia que chegou a estacionar. */
    static List<Duration> esperasAteEstacionar(List<Estadia> estadias) {
        List<Duration> esperas = new ArrayList<>();
        for (Estadia e : estadias) {
            if (!e.getOcupacoes().isEmpty()) {
                esperas.add(Duration.between(e.getEntrada(), e.getOcupacoes().get(0).getInicio()));
            }
        }
        return esperas;
    }

    /** Duração de cada ocupação já encerrada. */
    static List<Duration> duracoesEncerradas(List<Ocupacao> ocupacoes) {
        List<Duration> duracoes = new ArrayList<>();
        for (Ocupacao o : ocupacoes) {
            if (!o.isAberta()) duracoes.add(Duration.between(o.getInicio(), o.getFim()));
        }
        return duracoes;
    }

    // --------------------------------------------------------------- contagens

    /** Entradas por hora do dia: posição 0 = 0h, ..., 23 = 23h. */
    static int[] entradasPorHora(List<Estadia> estadias) {
        int[] porHora = new int[24];
        for (Estadia e : estadias) {
            porHora[e.getEntrada().getHour()]++;
        }
        return porHora;
    }

    /** Entradas por dia da semana: posição 0 = segunda, ..., 6 = domingo. */
    static int[] entradasPorDia(List<Estadia> estadias) {
        int[] porDia = new int[7];
        for (Estadia e : estadias) {
            porDia[e.getEntrada().getDayOfWeek().getValue() - 1]++;
        }
        return porDia;
    }

    static int totalOcupacoes(List<Estadia> estadias) {
        int total = 0;
        for (Estadia e : estadias) {
            total += e.getOcupacoes().size();
        }
        return total;
    }

    // ---------------------------------------------------------- lotação e giro

    /** Varre as ocupações em ordem de horário; null se nenhuma vaga foi ocupada. */
    static Lotacao maiorLotacao(List<Estadia> estadias) {
        record Marco(LocalDateTime quando, int variacao) {}
        List<Marco> marcos = new ArrayList<>();
        for (Estadia e : estadias) {
            for (Ocupacao o : e.getOcupacoes()) {
                marcos.add(new Marco(o.getInicio(), +1));
                if (o.getFim() != null) marcos.add(new Marco(o.getFim(), -1));
            }
        }
        if (marcos.isEmpty()) {
            return null;
        }
        marcos.sort(Comparator.comparing(Marco::quando).thenComparingInt(Marco::variacao));
        int atual = 0;
        int maior = 0;
        LocalDateTime quando = null;
        for (Marco m : marcos) {
            atual += m.variacao();
            if (atual > maior) {
                maior = atual;
                quando = m.quando();
            }
        }
        return new Lotacao(maior, quando);
    }

    /**
     * Da primeira entrada ao último evento registrado; vai até {@code agora} se ainda
     * houver veículo dentro. Recebe uma lista com pelo menos uma estadia.
     */
    static Periodo periodoObservado(List<Estadia> estadias, LocalDateTime agora) {
        LocalDateTime inicio = estadias.get(0).getEntrada();
        LocalDateTime fim = inicio;
        boolean algumaAberta = false;
        for (Estadia e : estadias) {
            if (e.getEntrada().isBefore(inicio)) inicio = e.getEntrada();
            if (e.getUltimoEvento().isAfter(fim)) fim = e.getUltimoEvento();
            if (e.isAberta()) algumaAberta = true;
        }
        return new Periodo(inicio, algumaAberta ? agora : fim);
    }

    /** Chave de uma vaga nos mapas de uso: setor e id, como em "SUBSOLO:A1". */
    static String chaveDaVaga(Setor setor, String idVaga) {
        return setor + ":" + idVaga;
    }

    /** Uso de cada vaga que teve ao menos uma ocupação; a ocupação em andamento conta até {@code agora}. */
    static Map<String, UsoDeVaga> usoPorVaga(List<Estadia> estadias, LocalDateTime agora) {
        Map<String, long[]> acumulado = new LinkedHashMap<>();   // {ocupações, segundos ocupados}
        for (Estadia e : estadias) {
            for (Ocupacao o : e.getOcupacoes()) {
                long[] a = acumulado.computeIfAbsent(chaveDaVaga(e.getSetor(), o.getIdVaga()), k -> new long[2]);
                a[0]++;
                a[1] += Duration.between(o.getInicio(), o.isAberta() ? agora : o.getFim()).getSeconds();
            }
        }
        Map<String, UsoDeVaga> uso = new LinkedHashMap<>();
        for (Map.Entry<String, long[]> item : acumulado.entrySet()) {
            long[] a = item.getValue();
            uso.put(item.getKey(), new UsoDeVaga(a[0], Duration.ofSeconds(a[1])));
        }
        return uso;
    }

    // ---------------------------------------------------------------- clientes

    /** Estadias agrupadas por placa, na ordem da primeira visita. */
    static Map<String, List<Estadia>> porPlaca(List<Estadia> estadias) {
        Map<String, List<Estadia>> grupos = new LinkedHashMap<>();
        for (Estadia e : estadias) {
            grupos.computeIfAbsent(e.getPlaca(), p -> new ArrayList<>()).add(e);
        }
        return grupos;
    }

    /** Placas com 2 visitas ou mais: da que mais voltou para a que menos; no empate, a visita mais recente primeiro. */
    static List<Cliente> clientesQueVoltaram(List<Estadia> estadias) {
        List<Cliente> voltaram = new ArrayList<>();
        for (Map.Entry<String, List<Estadia>> item : porPlaca(estadias).entrySet()) {
            if (item.getValue().size() > 1) {
                voltaram.add(new Cliente(item.getKey(), item.getValue(), ultimaEntrada(item.getValue())));
            }
        }
        voltaram.sort(Comparator.comparingInt((Cliente c) -> c.visitas().size()).reversed()
                .thenComparing(Cliente::ultimaVisita, Comparator.reverseOrder()));
        return voltaram;
    }

    private static LocalDateTime ultimaEntrada(List<Estadia> estadias) {
        LocalDateTime ultima = estadias.get(0).getEntrada();
        for (Estadia e : estadias) {
            if (e.getEntrada().isAfter(ultima)) ultima = e.getEntrada();
        }
        return ultima;
    }

    // ----------------------------------------------------------------- filtros

    static <T> List<T> filtrar(List<T> itens, Predicate<T> filtro) {
        List<T> r = new ArrayList<>();
        for (T item : itens) {
            if (filtro.test(item)) r.add(item);
        }
        return r;
    }
}
