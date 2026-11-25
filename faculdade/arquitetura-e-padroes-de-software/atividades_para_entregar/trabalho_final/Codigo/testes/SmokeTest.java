import blackboard.Blackboard;
import blackboard.BlackboardController;
import knowledge_sources.EspecialistaDeSetor;
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
    private static Requisicao ultima;
    private static Relatorio relatorioAnterior;

    public static void main(String[] args) throws Exception {
        bb = Blackboard.getInstance();
        bb.adicionarObserver(() -> notificacoes++);
        List<KnowledgeSource> ks = List.of(new ExternoKS(bb), new SubsoloKS(bb), new RelatorioKS(bb));
        ctrl = new BlackboardController(bb, ks);

        estrutura(ks);
        manutencao();
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
        check("Setores herdam de EspecialistaDeSetor; relatórios não",
                EspecialistaDeSetor.class.isAssignableFrom(ExternoKS.class)
                        && EspecialistaDeSetor.class.isAssignableFrom(SubsoloKS.class)
                        && !EspecialistaDeSetor.class.isAssignableFrom(RelatorioKS.class));

        bb.publicarRequisicao(entrar(Setor.EXTERNO, "AAA1A11", null));
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
        bb.publicarRequisicao(entrar(Setor.EXTERNO, "AAA1A11", null));
        check("publicarRequisicao não notifica observers", notificacoes == antes);
        bb.consumirRequisicao();
        check("consumirRequisicao limpa a requisição", bb.getRequisicaoPendente() == null);
    }

    // ------------------------------------------------- manutenção do Blackboard

    private static void manutencao() {
        titulo("Manutenção do Blackboard");
        executar(entrar(Setor.EXTERNO, "MMM1M11", null));
        executar(ocupar(Setor.EXTERNO, "MMM1M11", "A1", null));
        bb.reiniciar();
        check("reiniciar() libera as vagas e apaga as estadias",
                bb.vagasOcupadas(Setor.EXTERNO) == 0 && bb.getEstadias().isEmpty()
                        && bb.getUltimoResultado() == null);

        int antes = notificacoes;
        int[] durante = new int[1];
        bb.executarEmLote(() -> {
            executar(entrar(Setor.EXTERNO, "LLL1L11", null));
            executar(entrar(Setor.EXTERNO, "LLL2L22", null));
            durante[0] = notificacoes - antes;
        });
        check("executarEmLote() adia as notificações e avisa uma vez no fim",
                durante[0] == 0 && notificacoes - antes == 1 && bb.getEstadias().size() == 2);
        bb.reiniciar();
    }

    // ------------------------------------- cenários de setor (os quatro eventos)

    private static void cenarios() {
        titulo("Cenários de setor (entrar, ocupar, desocupar, sair)");

        executar(entrar(Setor.EXTERNO, "AAA1A11", null));
        check("Entrar no Externo: abre a estadia e o veículo fica circulando",
                sucesso() && situacao("AAA1A11") == SituacaoVeiculo.CIRCULANDO);

        int estadias = bb.getEstadias().size();
        executar(entrar(Setor.SUBSOLO, "AAA1A11", 1.70));
        check("Placa já no estacionamento: erro e nada muda",
                recusou() && bb.getEstadias().size() == estadias);

        executar(entrar(Setor.SUBSOLO, "BBB2B22", 2.20));
        check("Subsolo 2,20 m: erro de altura e nenhuma estadia",
                recusou() && msg().contains("excede") && bb.getEstadiaAberta("BBB2B22") == null);

        executar(entrar(Setor.SUBSOLO, "GGG7G77", null));
        check("Subsolo sem altura: erro", recusou() && bb.getEstadiaAberta("GGG7G77") == null);

        executar(entrar(Setor.SUBSOLO, "CCC3C33", 1.85));
        check("Subsolo 1,85 m: entra circulando",
                sucesso() && situacao("CCC3C33") == SituacaoVeiculo.CIRCULANDO);

        executar(ocupar(Setor.EXTERNO, "ZZZ9Z99", "B3", null));
        check("Ocupar sem ter entrado: erro e vaga continua livre",
                recusou() && bb.getVaga(Setor.EXTERNO, "B3").isLivre());

        executar(ocupar(Setor.SUBSOLO, "AAA1A11", "A3", null));
        check("Ocupar vaga de outro setor: erro e vaga continua livre",
                recusou() && bb.getVaga(Setor.SUBSOLO, "A3").isLivre());

        executar(ocupar(Setor.EXTERNO, "AAA1A11", "B3", null));
        check("Externo, vaga livre: ocupa e o veículo fica estacionado",
                sucesso() && !bb.getVaga(Setor.EXTERNO, "B3").isLivre()
                        && situacao("AAA1A11") == SituacaoVeiculo.ESTACIONADO);

        executar(ocupar(Setor.EXTERNO, "AAA1A11", "B4", null));
        check("Veículo já estacionado: erro e B4 continua livre",
                recusou() && bb.getVaga(Setor.EXTERNO, "B4").isLivre());

        executar(entrar(Setor.EXTERNO, "HHH8H88", null));
        executar(ocupar(Setor.EXTERNO, "HHH8H88", "B3", null));
        check("Vaga ocupada: erro e o ocupante continua o mesmo",
                recusou() && bb.getVaga(Setor.EXTERNO, "B3").getEstadiaAtual().getPlaca().equals("AAA1A11"));

        executar(ocupar(Setor.EXTERNO, "HHH8H88", "Z9", null));
        check("Vaga inexistente: erro", recusou());

        executar(ocupar(Setor.SUBSOLO, "CCC3C33", "A1", null));
        check("Preferencial sem motivo: erro e vaga continua livre",
                recusou() && bb.getVaga(Setor.SUBSOLO, "A1").isLivre());

        executar(ocupar(Setor.SUBSOLO, "CCC3C33", "A1", MotivoPreferencial.IDOSO));
        check("Preferencial com motivo: ocupa e grava o motivo",
                sucesso() && bb.getEstadiaAberta("CCC3C33").getOcupacaoAtual().getMotivo()
                        == MotivoPreferencial.IDOSO);
        check("Resultado carimba a origem e a requisição",
                "SubsoloKS".equals(bb.getUltimoResultado().getOrigem())
                        && bb.getUltimoResultado().getRequisicao().getTipo() == TipoRequisicao.OCUPAR);

        executar(entrar(Setor.SUBSOLO, "FFF6F66", 1.85));
        executar(ocupar(Setor.SUBSOLO, "FFF6F66", "B1", MotivoPreferencial.PCD));
        check("Motivo em vaga comum é ignorado",
                sucesso() && bb.getEstadiaAberta("FFF6F66").getOcupacaoAtual().getMotivo() == null);

        executar(sair(Setor.SUBSOLO, "CCC3C33"));
        check("Sair com o veículo na vaga: erro que cita a vaga e a estadia continua aberta",
                recusou() && msg().contains("A1") && bb.getEstadiaAberta("CCC3C33") != null);

        executar(desocupar(Setor.SUBSOLO, "A1"));
        check("Desocupar: libera a vaga e o veículo volta a circular",
                sucesso() && bb.getVaga(Setor.SUBSOLO, "A1").isLivre()
                        && bb.getVaga(Setor.SUBSOLO, "A1").getEstadiaAtual() == null
                        && situacao("CCC3C33") == SituacaoVeiculo.CIRCULANDO
                        && !bb.getEstadiaAberta("CCC3C33").getOcupacoes().get(0).isAberta());

        executar(desocupar(Setor.SUBSOLO, "A1"));
        check("Desocupar vaga livre: erro", recusou());

        executar(sair(Setor.SUBSOLO, "CCC3C33"));
        check("Sair circulando: fecha a estadia e informa a permanência",
                sucesso() && msg().contains("permanência") && bb.getEstadiaAberta("CCC3C33") == null
                        && !bb.buscarPorPlaca("CCC3C33").get(0).isAberta());

        executar(sair(Setor.SUBSOLO, "CCC3C33"));
        check("Sair de novo: erro, o veículo não está no setor", recusou());

        executar(entrar(Setor.EXTERNO, "CCC3C33", null));
        check("Nova entrada depois da saída: segunda estadia da placa",
                sucesso() && bb.buscarPorPlaca("CCC3C33").size() == 2);

        LocalDateTime agora = LocalDateTime.now();
        executar(entrar(Setor.EXTERNO, "JJJ0J00", null).setMomento(agora));
        executar(ocupar(Setor.EXTERNO, "JJJ0J00", "C1", null).setMomento(agora.minusHours(1)));
        check("Evento com horário anterior ao último: erro e vaga continua livre",
                recusou() && msg().contains("anterior") && bb.getVaga(Setor.EXTERNO, "C1").isLivre());

        executar(new Requisicao().setTipo(TipoRequisicao.ENTRAR));
        check("Requisição sem setor: nenhum especialista apto (origem Controlador)",
                recusou() && msg().contains("Nenhum especialista")
                        && "Controlador".equals(bb.getUltimoResultado().getOrigem()));
        check("Requisição sempre consumida", bb.getRequisicaoPendente() == null);
    }

    // ----------------------------------------------------------- relatórios

    private static void relatorios() {
        titulo("Relatórios");
        bb.reiniciar();
        LocalDateTime t = LocalDateTime.now().minusHours(3);
        executar(entrar(Setor.SUBSOLO, "EEE5E55", 1.60).setMomento(t));
        executar(ocupar(Setor.SUBSOLO, "EEE5E55", "A1", MotivoPreferencial.IDOSO).setMomento(t.plusMinutes(4)));
        executar(desocupar(Setor.SUBSOLO, "A1").setMomento(t.plusMinutes(94)));
        executar(sair(Setor.SUBSOLO, "EEE5E55").setMomento(t.plusMinutes(97)));
        executar(entrar(Setor.EXTERNO, "XYZ9K88", null));
        executar(ocupar(Setor.EXTERNO, "XYZ9K88", "C2", null));
        executar(entrar(Setor.SUBSOLO, "DDD4D44", 1.85));
        executar(ocupar(Setor.SUBSOLO, "DDD4D44", "A2", MotivoPreferencial.GESTANTE));
        executar(entrar(Setor.EXTERNO, "KKK1K11", null));

        for (TipoRelatorio tipo : TipoRelatorio.values()) {
            executar(relatorio(tipo, "DDD4D44", LocalDateTime.now().minusHours(4), LocalDateTime.now().plusHours(1)));
            Relatorio rel = relatorioNovo();
            check("Relatório " + tipo + " gerado",
                    rel != null && rel.getTipo() == tipo && !rel.getTitulo().isEmpty() && !rel.getLinhas().isEmpty());
        }

        executar(relatorio(TipoRelatorio.OCUPACAO, null, null, null));
        check("OCUPACAO lista os estacionados e os que estão circulando",
                contem("XYZ9K88") && contem("DDD4D44") && contem("KKK1K11") && contem("circulando"));

        executar(relatorio(TipoRelatorio.HISTORICO_POR_PLACA, "DDD4D44", null, null));
        check("Histórico por placa traz só a placa pedida", contem("DDD4D44") && !contem("XYZ9K88"));

        executar(relatorio(TipoRelatorio.HISTORICO_POR_PLACA, "NAOEXISTE", null, null));
        check("Placa sem histórico: 'sem dados'", contem("sem dados"));

        executar(relatorio(TipoRelatorio.HISTORICO_POR_PLACA, null, null, null));
        check("Placa ausente vira erro", recusou());

        executar(relatorio(TipoRelatorio.HISTORICO_POR_PERIODO, null,
                LocalDateTime.now().plusHours(2), LocalDateTime.now().minusHours(2)));
        check("Período invertido vira erro", recusou());

        executar(relatorio(TipoRelatorio.HISTORICO_POR_PERIODO, null,
                LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(2)));
        check("Período sem registros: 'sem dados'", contem("sem dados"));

        executar(relatorio(TipoRelatorio.PREFERENCIAIS, null, null, null));
        check("PREFERENCIAIS conta as ocupações de A1 e A2", contem("Total de usos: 2"));

        executar(relatorio(TipoRelatorio.TEMPO_MEDIO, null, null, null));
        check("TEMPO_MEDIO usa só estadias fechadas",
                contem("Geral:") && contem("1h 37min") && contem("(1 estadia fechada)"));

        executar(relatorio(TipoRelatorio.COMPLETO, null, null, null));
        Relatorio completo = relatorioNovo();
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
        boolean semCruzamento = true, semPrint = true, semUi = true;
        List<String> nomes = List.of("ExternoKS", "SubsoloKS", "RelatorioKS");
        try (Stream<Path> arquivos = Files.list(ksDir)) {
            for (Path p : (Iterable<Path>) arquivos::iterator) {
                String nome = p.getFileName().toString().replace(".java", "");
                String src = Files.readString(p);
                if (src.contains("System.out")) semPrint = false;
                if (src.contains("import ui")) semUi = false;
                for (String outro : nomes) {
                    if (!outro.equals(nome) && src.contains(outro)) semCruzamento = false;
                }
            }
        }
        check("Nenhum KS referencia outro KS", semCruzamento);
        check("Nenhum KS usa System.out", semPrint);
        check("Nenhum KS importa a interface (ui)", semUi);

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

    private static Requisicao entrar(Setor s, String placa, Double altura) {
        return new Requisicao().setTipo(TipoRequisicao.ENTRAR).setSetor(s).setPlaca(placa).setAltura(altura);
    }

    private static Requisicao ocupar(Setor s, String placa, String vaga, MotivoPreferencial m) {
        return new Requisicao().setTipo(TipoRequisicao.OCUPAR).setSetor(s).setPlaca(placa)
                .setIdVaga(vaga).setMotivo(m);
    }

    private static Requisicao desocupar(Setor s, String vaga) {
        return new Requisicao().setTipo(TipoRequisicao.DESOCUPAR).setSetor(s).setIdVaga(vaga);
    }

    private static Requisicao sair(Setor s, String placa) {
        return new Requisicao().setTipo(TipoRequisicao.SAIR).setSetor(s).setPlaca(placa);
    }

    private static Requisicao relatorio(TipoRelatorio t, String placa, LocalDateTime ini, LocalDateTime fim) {
        return new Requisicao().setTipo(TipoRequisicao.RELATORIO).setTipoRelatorio(t)
                .setPlaca(placa).setInicio(ini).setFim(fim);
    }

    private static void executar(Requisicao r) {
        ultima = r;
        relatorioAnterior = bb.getUltimoRelatorio();
        bb.publicarRequisicao(r);
        ctrl.executarCiclo();
    }

    /** O último resultado é a resposta à requisição que o teste acabou de enviar. */
    private static boolean respondeu() {
        ResultadoOperacao r = bb.getUltimoResultado();
        return r != null && r.getRequisicao() == ultima;
    }

    private static boolean sucesso() {
        return respondeu() && bb.getUltimoResultado().isSucesso();
    }

    private static boolean recusou() {
        return respondeu() && !bb.getUltimoResultado().isSucesso();
    }

    private static String msg() {
        return bb.getUltimoResultado() == null ? "" : bb.getUltimoResultado().getMensagem();
    }

    /** Relatório publicado pela última requisição, ou null se ela não gerou um novo. */
    private static Relatorio relatorioNovo() {
        Relatorio r = bb.getUltimoRelatorio();
        return r != relatorioAnterior ? r : null;
    }

    private static boolean contem(String trecho) {
        Relatorio rel = relatorioNovo();
        if (rel != null && String.join("\n", rel.getLinhas()).contains(trecho)) return true;
        return respondeu() && msg().contains(trecho);
    }

    private static SituacaoVeiculo situacao(String placa) {
        Estadia e = bb.getEstadiaAberta(placa);
        return e == null ? null : e.getSituacao();
    }

    private static void titulo(String t) {
        System.out.println("\n== " + t);
    }

    private static void check(String descricao, boolean condicao) {
        if (condicao) ok++; else falhas++;
        System.out.println((condicao ? "  [OK]    " : "  [FALHA] ") + descricao);
    }
}
