import blackboard.Blackboard;
import blackboard.BlackboardController;
import knowledge_sources.ExternoKS;
import knowledge_sources.KnowledgeSource;
import knowledge_sources.RelatorioKS;
import knowledge_sources.SubsoloKS;
import model.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

/**
 * Teste de fumaça sem dependências externas.
 * Executa: javac -d out $(find src testes -name "*.java") && java -cp out SmokeTest
 */
public class SmokeTest {

    private static int ok = 0;
    private static int falhas = 0;

    private static Blackboard bb;
    private static BlackboardController ctrl;
    private static int notificacoes = 0;

    public static void main(String[] args) throws Exception {
        bb = Blackboard.getInstance();
        bb.adicionarObserver(() -> notificacoes++);
        List<KnowledgeSource> ks = List.of(new ExternoKS(bb), new SubsoloKS(bb), new RelatorioKS(bb));
        ctrl = new BlackboardController(bb, ks);

        estrutura(ks);
        cenarios();
        relatorios();
        arquitetura();

        System.out.println();
        System.out.println("Resultado: " + ok + " ok, " + falhas + " falha(s)");
        System.exit(falhas == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------ estrutura

    private static void estrutura(List<KnowledgeSource> ks) throws Exception {
        titulo("Estrutura");
        check("Singleton devolve sempre a mesma instância", Blackboard.getInstance() == bb);
        check("15 vagas no EXTERNO", bb.totalVagas(Setor.EXTERNO) == 15);
        check("10 vagas no SUBSOLO", bb.totalVagas(Setor.SUBSOLO) == 10);
        check("Só A1/A2 do SUBSOLO são preferenciais",
                bb.totalPreferenciais(Setor.SUBSOLO) == 2 && bb.totalPreferenciais(Setor.EXTERNO) == 0
                        && bb.getVaga(Setor.SUBSOLO, "A1").isPreferencial()
                        && bb.getVaga(Setor.SUBSOLO, "A2").isPreferencial()
                        && !bb.getVaga(Setor.SUBSOLO, "A3").isPreferencial());
        Method run = KnowledgeSource.class.getMethod("run");
        check("run() é final (Template Method)", Modifier.isFinal(run.getModifiers()));

        bb.publicarRequisicao(new Requisicao().setTipo(TipoRequisicao.ENTRADA).setSetor(Setor.EXTERNO));
        boolean a = true, b = true;
        for (int i = 0; i < 2; i++) {
            for (KnowledgeSource k : ks) {
                Method c = KnowledgeSource.class.getDeclaredMethod("condition");
                c.setAccessible(true);
                boolean v = (boolean) c.invoke(k);
                if (k instanceof ExternoKS) a &= v;
                if (k instanceof SubsoloKS) b &= !v;
            }
        }
        check("condition() é idempotente e seletiva", a && b);
        int antes = notificacoes;
        bb.publicarRequisicao(new Requisicao().setTipo(TipoRequisicao.ENTRADA).setSetor(Setor.EXTERNO));
        check("publicarRequisicao não notifica observers", notificacoes == antes);
        bb.consumirRequisicao();
        check("consumirRequisicao limpa a requisição", bb.getRequisicaoPendente() == null);
    }

    // ------------------------------------------------ 8 cenários dos KS (C4)

    private static void cenarios() {
        titulo("Cenários de setor");

        executar(validarAltura(2.20));
        check("Pré-validação 2,20 m: erro sem ocupar vaga",
                !sucesso() && bb.vagasOcupadas(Setor.SUBSOLO) == 0);
        executar(validarAltura(1.85));
        check("Pré-validação 1,85 m: sucesso sem ocupar vaga",
                sucesso() && bb.vagasOcupadas(Setor.SUBSOLO) == 0);

        executar(entrada(Setor.EXTERNO, "AAA1A11", null, "B3", null));
        check("Externo, vaga livre: ocupa e abre log",
                sucesso() && !bb.getVaga(Setor.EXTERNO, "B3").isLivre()
                        && bb.getVaga(Setor.EXTERNO, "B3").getRegistroAtual().isAberto());

        int logs = bb.getHistorico().size();
        executar(entrada(Setor.EXTERNO, "ZZZ9Z99", null, "B3", null));
        check("Externo, vaga ocupada: erro e nada muda",
                !sucesso() && bb.getHistorico().size() == logs
                        && bb.getVaga(Setor.EXTERNO, "B3").getRegistroAtual().getPlaca().equals("AAA1A11"));

        executar(entrada(Setor.SUBSOLO, "BBB2B22", 2.20, "A3", null));
        check("Subsolo 2,20 m: erro de altura",
                !sucesso() && msg().contains("excede") && bb.getVaga(Setor.SUBSOLO, "A3").isLivre());

        executar(entrada(Setor.SUBSOLO, "CCC3C33", 1.85, "A3", null));
        check("Subsolo 1,85 m em vaga comum: sucesso",
                sucesso() && !bb.getVaga(Setor.SUBSOLO, "A3").isLivre()
                        && bb.getVaga(Setor.SUBSOLO, "A3").getRegistroAtual().getMotivo() == null);

        executar(entrada(Setor.SUBSOLO, "DDD4D44", 1.85, "A1", MotivoPreferencial.IDOSO));
        check("Preferencial com motivo: ocupa e grava motivo",
                sucesso() && bb.getVaga(Setor.SUBSOLO, "A1").getRegistroAtual().getMotivo() == MotivoPreferencial.IDOSO);

        executar(entrada(Setor.SUBSOLO, "EEE5E55", 1.85, "A2", null));
        check("Preferencial sem motivo: erro e vaga continua livre",
                !sucesso() && bb.getVaga(Setor.SUBSOLO, "A2").isLivre());

        executar(entrada(Setor.SUBSOLO, "FFF6F66", 1.85, "B1", MotivoPreferencial.PCD));
        check("Motivo em vaga comum é ignorado",
                sucesso() && bb.getVaga(Setor.SUBSOLO, "B1").getRegistroAtual().getMotivo() == null);

        executar(entrada(Setor.SUBSOLO, "GGG7G77", null, "B2", null));
        check("Subsolo sem altura: erro", !sucesso());

        executar(entrada(Setor.EXTERNO, "HHH8H88", null, "Z9", null));
        check("Vaga inexistente: erro", !sucesso());

        executar(saida(Setor.SUBSOLO, "A1"));
        check("Saída de vaga ocupada: fecha log, libera e informa permanência",
                sucesso() && bb.getVaga(Setor.SUBSOLO, "A1").isLivre()
                        && bb.getVaga(Setor.SUBSOLO, "A1").getRegistroAtual() == null
                        && msg().contains("permanência")
                        && !bb.buscarPorPlaca("DDD4D44").get(0).isAberto());

        int antes = bb.getHistorico().size();
        executar(saida(Setor.SUBSOLO, "A1"));
        check("Saída de vaga livre: erro e nada muda", !sucesso() && bb.getHistorico().size() == antes);

        executar(saida(Setor.EXTERNO, "B3"));
        check("Saída externa: sucesso", sucesso() && bb.getVaga(Setor.EXTERNO, "B3").isLivre());

        executar(new Requisicao().setTipo(TipoRequisicao.ENTRADA));
        check("Requisição sem setor: nenhum especialista apto",
                !sucesso() && msg().contains("Nenhum especialista"));
        check("Requisição sempre consumida", bb.getRequisicaoPendente() == null);
    }

    // ----------------------------------------------------------- relatórios

    private static void relatorios() {
        titulo("Relatórios");
        executar(entrada(Setor.EXTERNO, "XYZ9K88", null, "C2", null));
        executar(entrada(Setor.SUBSOLO, "DDD4D44", 1.85, "A2", MotivoPreferencial.GESTANTE));

        for (TipoRelatorio t : TipoRelatorio.values()) {
            Requisicao r = new Requisicao().setTipo(TipoRequisicao.RELATORIO).setTipoRelatorio(t)
                    .setPlaca("DDD4D44")
                    .setInicio(LocalDateTime.now().minusHours(1)).setFim(LocalDateTime.now().plusHours(1));
            executar(r);
            Relatorio rel = bb.getUltimoRelatorio();
            check("Relatório " + t + " gerado",
                    rel != null && rel.getTipo() == t && !rel.getTitulo().isEmpty() && !rel.getLinhas().isEmpty());
        }

        executar(relatorio(TipoRelatorio.OCUPACAO, null, null, null));
        check("OCUPACAO lista veículos presentes", contem("XYZ9K88") && contem("DDD4D44"));

        executar(relatorio(TipoRelatorio.HISTORICO_POR_PLACA, "DDD4D44", null, null));
        check("Histórico por placa traz só a placa pedida", contem("DDD4D44") && !contem("XYZ9K88"));

        executar(relatorio(TipoRelatorio.HISTORICO_POR_PLACA, "NAOEXISTE", null, null));
        check("Placa sem histórico: 'sem dados'", contem("sem dados"));

        executar(relatorio(TipoRelatorio.HISTORICO_POR_PLACA, null, null, null));
        check("Placa ausente vira erro", !sucesso());

        executar(relatorio(TipoRelatorio.HISTORICO_POR_PERIODO, null,
                LocalDateTime.now().plusHours(2), LocalDateTime.now().minusHours(2)));
        check("Período invertido vira erro", !sucesso());

        executar(relatorio(TipoRelatorio.HISTORICO_POR_PERIODO, null,
                LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(2)));
        check("Período sem registros: 'sem dados'", contem("sem dados"));

        executar(relatorio(TipoRelatorio.PREFERENCIAIS, null, null, null));
        check("PREFERENCIAIS conta usos A1/A2", contem("Total de usos: 2"));

        executar(relatorio(TipoRelatorio.TEMPO_MEDIO, null, null, null));
        check("TEMPO_MEDIO usa só estadias fechadas", contem("Geral:") && contem("fechada"));

        executar(relatorio(TipoRelatorio.COMPLETO, null, null, null));
        Relatorio completo = bb.getUltimoRelatorio();
        check("COMPLETO reúne as quatro seções",
                completo != null && completo.getTitulo().equals("RELATÓRIO COMPLETO")
                        && contem("OCUPAÇÃO POR SETOR")
                        && contem("TEMPO MÉDIO DE PERMANÊNCIA")
                        && contem("USO DE VAGAS PREFERENCIAIS")
                        && contem("HISTÓRICO GERAL"));
        check("COMPLETO cabe em 90 colunas",
                completo != null && completo.getLinhas().stream().allMatch(l -> l.length() <= 90));
    }

    // ---------------------------------------------------------- arquitetura

    private static void arquitetura() throws IOException {
        titulo("Arquitetura (análise do código-fonte)");
        Path ksDir = Path.of("src/knowledge_sources");
        boolean semCruzamento = true, semPrint = true;
        List<String> nomes = List.of("ExternoKS", "SubsoloKS", "RelatorioKS");
        try (Stream<Path> arquivos = Files.list(ksDir)) {
            for (Path p : (Iterable<Path>) arquivos::iterator) {
                String nome = p.getFileName().toString().replace(".java", "");
                String src = Files.readString(p);
                if (src.contains("System.out")) semPrint = false;
                for (String outro : nomes) {
                    if (!outro.equals(nome) && src.contains(outro)) semCruzamento = false;
                }
            }
        }
        check("Nenhum KS referencia outro KS", semCruzamento);
        check("Nenhum KS usa System.out", semPrint);

        boolean foraDeUi = false;
        try (Stream<Path> todos = Files.walk(Path.of("src"))) {
            for (Path p : (Iterable<Path>) todos.filter(x -> x.toString().endsWith(".java"))::iterator) {
                if (!p.toString().contains("/ui/") && Files.readString(p).contains("System.out")) foraDeUi = true;
            }
        }
        check("Nenhum System.out fora do pacote ui", !foraDeUi);

        boolean bbLimpo = !Files.readString(Path.of("src/blackboard/Blackboard.java")).contains("knowledge_sources");
        check("Blackboard não importa KS", bbLimpo);
    }

    // ---------------------------------------------------------------- apoio

    private static Requisicao entrada(Setor s, String placa, Double altura, String vaga, MotivoPreferencial m) {
        return new Requisicao().setTipo(TipoRequisicao.ENTRADA).setSetor(s).setPlaca(placa)
                .setAltura(altura).setIdVaga(vaga).setMotivo(m);
    }

    private static Requisicao saida(Setor s, String vaga) {
        return new Requisicao().setTipo(TipoRequisicao.SAIDA).setSetor(s).setIdVaga(vaga);
    }

    private static Requisicao validarAltura(Double altura) {
        return new Requisicao().setTipo(TipoRequisicao.VALIDAR_ALTURA)
                .setSetor(Setor.SUBSOLO).setAltura(altura);
    }

    private static Requisicao relatorio(TipoRelatorio t, String placa, LocalDateTime ini, LocalDateTime fim) {
        return new Requisicao().setTipo(TipoRequisicao.RELATORIO).setTipoRelatorio(t)
                .setPlaca(placa).setInicio(ini).setFim(fim);
    }

    private static void executar(Requisicao r) {
        bb.publicarRequisicao(r);
        ctrl.executarCiclo();
    }

    private static boolean sucesso() {
        return bb.getUltimoResultado() != null && bb.getUltimoResultado().isSucesso();
    }

    private static String msg() {
        return bb.getUltimoResultado() == null ? "" : bb.getUltimoResultado().getMensagem();
    }

    private static boolean contem(String trecho) {
        Relatorio rel = bb.getUltimoRelatorio();
        if (rel != null && String.join("\n", rel.getLinhas()).contains(trecho)) return true;
        return msg().contains(trecho);
    }

    private static void titulo(String t) {
        System.out.println("\n== " + t);
    }

    private static void check(String descricao, boolean condicao) {
        if (condicao) ok++; else falhas++;
        System.out.println((condicao ? "  [OK]    " : "  [FALHA] ") + descricao);
    }
}
