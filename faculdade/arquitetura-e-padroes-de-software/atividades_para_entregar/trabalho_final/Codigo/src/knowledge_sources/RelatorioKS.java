package knowledge_sources;

import blackboard.Blackboard;
import model.*;
import util.Formatador;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Especialista de relatórios gerenciais. Lê o Blackboard e publica um {@link Relatorio}.
 * Os tipos são despachados por switch + métodos privados (sem Strategy):
 * compartilham o mesmo contexto e não são escolhidos por um cliente estratégico.
 * O relatório COMPLETO reaproveita os mesmos blocos dos relatórios individuais.
 */
public class RelatorioKS extends KnowledgeSource {

    private static final String SEM_DADOS = "sem dados";
    private static final int LARGURA_SECAO = 80;
    private static final String FORMATO_OCUPACAO = "%-8s %6s %7s %7s %7s %6s %7s";
    private static final String FORMATO_PRESENTES = "%-8s  %-7s  %-10s  %-11s  %s";
    private static final String FORMATO_HISTORICO = "%-8s  %-7s  %-6s  %-11s  %-11s  %-13s  %s";

    public RelatorioKS(Blackboard blackboard) {
        super(blackboard);
    }

    @Override
    protected boolean condition() {
        Requisicao r = blackboard.getRequisicaoPendente();
        return r != null && r.getTipo() == TipoRequisicao.RELATORIO;
    }

    @Override
    protected void execute() {
        Requisicao r = blackboard.getRequisicaoPendente();

        String problema = validar(r);
        if (problema != null) {
            erro(problema);
            return;
        }

        Relatorio relatorio = switch (r.getTipoRelatorio()) {
            case OCUPACAO -> gerarOcupacao();
            case HISTORICO_GERAL -> gerarHistoricoGeral();
            case HISTORICO_POR_PLACA -> gerarHistoricoPorPlaca(r.getPlaca());
            case HISTORICO_POR_PERIODO -> gerarHistoricoPorPeriodo(r.getInicio(), r.getFim());
            case TEMPO_MEDIO -> gerarTempoMedio();
            case PREFERENCIAIS -> gerarPreferenciais();
            case COMPLETO -> gerarCompleto();
        };
        blackboard.publicarRelatorio(relatorio);
    }

    private String validar(Requisicao r) {
        TipoRelatorio tipo = r.getTipoRelatorio();
        if (tipo == null) {
            return "Tipo de relatório inválido.";
        }
        if (tipo == TipoRelatorio.HISTORICO_POR_PLACA
                && (r.getPlaca() == null || r.getPlaca().isBlank())) {
            return "Informe a placa para o histórico por placa.";
        }
        if (tipo == TipoRelatorio.HISTORICO_POR_PERIODO) {
            if (r.getInicio() == null || r.getFim() == null) {
                return "Informe o início e o fim do período.";
            }
            if (r.getInicio().isAfter(r.getFim())) {
                return "Período inválido: o início é posterior ao fim.";
            }
        }
        return null;
    }

    // --------------------------------------------------------------- COMPLETO

    /** Um único relatório com as seções dos demais, separadas por título e linha. */
    private Relatorio gerarCompleto() {
        List<String> linhas = new ArrayList<>();
        adicionarSecao(linhas, "OCUPAÇÃO POR SETOR", linhasOcupacao());
        adicionarSecao(linhas, "TEMPO MÉDIO DE PERMANÊNCIA", linhasTempoMedio());
        adicionarSecao(linhas, "USO DE VAGAS PREFERENCIAIS", linhasPreferenciais());
        adicionarSecao(linhas, "HISTÓRICO GERAL", linhasHistorico(blackboard.getEstadias()));

        while (!linhas.isEmpty() && linhas.get(linhas.size() - 1).isEmpty()) {
            linhas.remove(linhas.size() - 1);
        }
        return new Relatorio(TipoRelatorio.COMPLETO, "RELATÓRIO COMPLETO", linhas);
    }

    private void adicionarSecao(List<String> destino, String titulo, List<String> conteudo) {
        if (!destino.isEmpty()) destino.add("");
        destino.add("── " + titulo + " " + "─".repeat(Math.max(1, LARGURA_SECAO - titulo.length() - 4)));
        destino.addAll(conteudo);
    }

    // ---------------------------------------------------------------- OCUPACAO

    private Relatorio gerarOcupacao() {
        return new Relatorio(TipoRelatorio.OCUPACAO, "OCUPAÇÃO POR SETOR", linhasOcupacao());
    }

    private List<String> linhasOcupacao() {
        List<String> linhas = new ArrayList<>();
        linhas.add(String.format(FORMATO_OCUPACAO,
                "Setor", "Total", "Ocup.", "Livres", "Comuns", "Pref.", "Uso"));

        int total = 0, ocupadas = 0, comunsLivres = 0, comunsTotal = 0;
        int prefLivres = 0, prefTotal = 0;
        for (Setor s : Setor.values()) {
            total += blackboard.totalVagas(s);
            ocupadas += blackboard.vagasOcupadas(s);
            comunsLivres += blackboard.vagasLivresComuns(s);
            comunsTotal += blackboard.totalComuns(s);
            prefLivres += blackboard.vagasLivresPreferenciais(s);
            prefTotal += blackboard.totalPreferenciais(s);
            linhas.add(linhaSetor(s));
        }
        linhas.add(String.format(FORMATO_OCUPACAO,
                "Total", total, ocupadas, total - ocupadas,
                comunsLivres + "/" + comunsTotal,
                prefTotal > 0 ? prefLivres + "/" + prefTotal : "—",
                Formatador.percentual(ocupadas, total)));

        linhas.add("");
        linhas.addAll(linhasVeiculosPresentes());
        return linhas;
    }

    private String linhaSetor(Setor s) {
        int total = blackboard.totalVagas(s);
        int ocupadas = blackboard.vagasOcupadas(s);
        int preferenciais = blackboard.totalPreferenciais(s);
        String prefLivres = preferenciais > 0
                ? blackboard.vagasLivresPreferenciais(s) + "/" + preferenciais
                : "—";
        return String.format(FORMATO_OCUPACAO,
                s.name(), total, ocupadas, total - ocupadas,
                blackboard.vagasLivresComuns(s) + "/" + blackboard.totalComuns(s),
                prefLivres,
                Formatador.percentual(ocupadas, total));
    }

    private List<String> linhasVeiculosPresentes() {
        List<String> linhas = new ArrayList<>();
        List<Estadia> presentes = filtrarHistorico(Estadia::isAberta);

        linhas.add("Veículos no estacionamento: " + presentes.size());
        if (presentes.isEmpty()) {
            linhas.add(SEM_DADOS);
            return linhas;
        }
        linhas.add(String.format(FORMATO_PRESENTES,
                "Placa", "Setor", "Vaga", "Entrada", "Permanência"));
        LocalDateTime agora = LocalDateTime.now();
        for (Estadia e : presentes) {
            Ocupacao atual = e.getOcupacaoAtual();
            linhas.add(String.format(FORMATO_PRESENTES,
                    e.getPlaca(), e.getSetor().name(), atual != null ? atual.getIdVaga() : "circulando",
                    Formatador.dataHoraCurta(e.getEntrada()),
                    Formatador.duracao(Duration.between(e.getEntrada(), agora))));
        }
        return linhas;
    }

    // --------------------------------------------------------------- HISTÓRICOS

    private Relatorio gerarHistoricoGeral() {
        return new Relatorio(TipoRelatorio.HISTORICO_GERAL, "HISTÓRICO GERAL",
                linhasHistorico(blackboard.getEstadias()));
    }

    private Relatorio gerarHistoricoPorPlaca(String placa) {
        String p = placa.trim().toUpperCase();
        return new Relatorio(TipoRelatorio.HISTORICO_POR_PLACA, "HISTÓRICO DA PLACA " + p,
                linhasHistorico(blackboard.buscarPorPlaca(p)));
    }

    private Relatorio gerarHistoricoPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        List<String> linhas = new ArrayList<>();
        linhas.add("Critério: data/hora de ENTRADA dentro do período.");
        linhas.addAll(linhasHistorico(blackboard.buscarPorPeriodo(inicio, fim)));
        return new Relatorio(TipoRelatorio.HISTORICO_POR_PERIODO,
                "HISTÓRICO DE " + Formatador.dataHora(inicio) + " A " + Formatador.dataHora(fim), linhas);
    }

    private List<String> linhasHistorico(List<Estadia> estadias) {
        List<String> linhas = new ArrayList<>();
        if (estadias.isEmpty()) {
            linhas.add(SEM_DADOS);
            return linhas;
        }
        linhas.add(String.format(FORMATO_HISTORICO,
                "Placa", "Setor", "Vaga", "Entrada", "Saída", "Permanência", "Motivo"));
        for (Estadia e : estadias) {
            String saida = e.isAberta() ? "—" : Formatador.dataHoraCurta(e.getSaida());
            String perm = e.isAberta() ? "em aberto"
                    : Formatador.duracao(Duration.between(e.getEntrada(), e.getSaida()));
            String motivo = e.getMotivo() == null ? "—" : e.getMotivo().rotulo().toUpperCase();
            linhas.add(String.format(FORMATO_HISTORICO,
                    e.getPlaca(), e.getSetor().name(), e.getVagas(),
                    Formatador.dataHoraCurta(e.getEntrada()), saida, perm, motivo));
        }
        linhas.add("");
        linhas.add("Total: " + estadias.size() + (estadias.size() == 1 ? " estadia" : " estadias"));
        return linhas;
    }

    // -------------------------------------------------------------- TEMPO_MEDIO

    private Relatorio gerarTempoMedio() {
        return new Relatorio(TipoRelatorio.TEMPO_MEDIO, "TEMPO MÉDIO DE PERMANÊNCIA", linhasTempoMedio());
    }

    private List<String> linhasTempoMedio() {
        List<Estadia> fechados = filtrarHistorico(e -> !e.isAberta());
        List<String> linhas = new ArrayList<>();
        if (fechados.isEmpty()) {
            linhas.add(SEM_DADOS + " (nenhuma estadia encerrada)");
            return linhas;
        }
        linhas.add(linhaMedia("Geral", fechados));
        linhas.add(linhaMedia("Externo", filtrar(fechados, e -> e.getSetor() == Setor.EXTERNO)));
        linhas.add(linhaMedia("Subsolo", filtrar(fechados, e -> e.getSetor() == Setor.SUBSOLO)));
        linhas.add(linhaMedia("Comum", filtrar(fechados, e -> !usouPreferencial(e))));
        linhas.add(linhaMedia("Preferencial", filtrar(fechados, this::usouPreferencial)));
        linhas.add("");
        linhas.add("Estadias em aberto não entram no cálculo.");
        return linhas;
    }

    private String linhaMedia(String rotulo, List<Estadia> estadias) {
        String prefixo = String.format("%-14s", rotulo + ":");
        if (estadias.isEmpty()) {
            return prefixo + SEM_DADOS;
        }
        return prefixo + Formatador.duracao(mediaPermanencia(estadias)) + " ("
                + estadias.size() + (estadias.size() == 1 ? " estadia fechada)" : " estadias fechadas)");
    }

    private Duration mediaPermanencia(List<Estadia> fechadas) {
        List<Duration> duracoes = new ArrayList<>();
        for (Estadia e : fechadas) {
            duracoes.add(Duration.between(e.getEntrada(), e.getSaida()));
        }
        return media(duracoes);
    }

    private Duration media(List<Duration> duracoes) {
        long segundos = 0;
        for (Duration d : duracoes) {
            segundos += d.getSeconds();
        }
        return Duration.ofSeconds(segundos / duracoes.size());
    }

    // ------------------------------------------------------------ PREFERENCIAIS

    private Relatorio gerarPreferenciais() {
        return new Relatorio(TipoRelatorio.PREFERENCIAIS, "USO DE VAGAS PREFERENCIAIS", linhasPreferenciais());
    }

    private List<String> linhasPreferenciais() {
        List<String> linhas = new ArrayList<>();

        List<String> situacao = new ArrayList<>();
        for (Vaga v : blackboard.getVagas(Setor.SUBSOLO)) {
            if (v.isPreferencial()) {
                situacao.add(v.getId() + (v.isLivre() ? " livre" : " ocupada"));
            }
        }
        linhas.add("Ocupação atual: " + String.join(" | ", situacao));

        List<Ocupacao> usos = ocupacoesPreferenciais();
        if (usos.isEmpty()) {
            linhas.add("Total de usos: 0");
            linhas.add("Por motivo: " + SEM_DADOS);
            linhas.add("Tempo médio na vaga: " + SEM_DADOS);
            return linhas;
        }

        linhas.add("Total de usos: " + usos.size() + " ("
                + Formatador.percentual(usos.size(), totalOcupacoes()) + " das ocupações)");

        List<String> porMotivo = new ArrayList<>();
        for (MotivoPreferencial m : MotivoPreferencial.values()) {
            long n = usos.stream().filter(o -> o.getMotivo() == m).count();
            porMotivo.add(m.rotulo().toUpperCase() + " " + n);
        }
        for (int i = 0; i < porMotivo.size(); i += 3) {
            String prefixo = i == 0 ? "Por motivo: " : "            ";
            linhas.add(prefixo + String.join(" · ",
                    porMotivo.subList(i, Math.min(i + 3, porMotivo.size()))));
        }

        List<Duration> encerradas = new ArrayList<>();
        for (Ocupacao o : usos) {
            if (!o.isAberta()) encerradas.add(Duration.between(o.getInicio(), o.getFim()));
        }
        linhas.add("Tempo médio na vaga: " + (encerradas.isEmpty()
                ? SEM_DADOS + " (nenhuma ocupação encerrada)"
                : Formatador.duracao(media(encerradas))));
        return linhas;
    }

    // ------------------------------------------------------------------ apoio

    /** O tipo da vaga não é guardado na estadia: é derivado de Vaga.tipo (setor + idVaga). */
    private boolean isPreferencial(Setor setor, String idVaga) {
        Vaga v = blackboard.getVaga(setor, idVaga);
        return v != null && v.isPreferencial();
    }

    private boolean usouPreferencial(Estadia e) {
        for (Ocupacao o : e.getOcupacoes()) {
            if (isPreferencial(e.getSetor(), o.getIdVaga())) return true;
        }
        return false;
    }

    private List<Ocupacao> ocupacoesPreferenciais() {
        List<Ocupacao> r = new ArrayList<>();
        for (Estadia e : blackboard.getEstadias()) {
            for (Ocupacao o : e.getOcupacoes()) {
                if (isPreferencial(e.getSetor(), o.getIdVaga())) r.add(o);
            }
        }
        return r;
    }

    private int totalOcupacoes() {
        int total = 0;
        for (Estadia e : blackboard.getEstadias()) {
            total += e.getOcupacoes().size();
        }
        return total;
    }

    private List<Estadia> filtrarHistorico(Predicate<Estadia> filtro) {
        return filtrar(blackboard.getEstadias(), filtro);
    }

    private List<Estadia> filtrar(List<Estadia> estadias, Predicate<Estadia> filtro) {
        List<Estadia> r = new ArrayList<>();
        for (Estadia e : estadias) {
            if (filtro.test(e)) r.add(e);
        }
        return r;
    }
}
