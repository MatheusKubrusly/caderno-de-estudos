import aplicacao.FachadaEstacionamento;
import blackboard.Blackboard;
import blackboard.BlackboardController;
import knowledge_sources.ExternoKS;
import knowledge_sources.RelatorioKS;
import knowledge_sources.SubsoloKS;
import model.*;
import persistencia.RegistroDeEventos;
import ui.CLI;
import ui.Display;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Teste de aceitação: executa, pela fachada, o roteiro de demonstração do roteiro-de-uso.md
 * (passos D1 a D13) e uma sessão da interface de texto com a entrada simulada.
 * Usa um arquivo temporário; o dados/eventos.csv não é tocado.
 */
public class TesteDeAceitacao {

    private static final Path CENARIO = Path.of("dados/cenario-demonstracao.csv");

    private static int ok = 0;
    private static int falhas = 0;

    private static Blackboard bb;
    private static FachadaEstacionamento fachada;

    private interface Secao {
        void executar() throws Exception;
    }

    public static void main(String[] args) throws Exception {
        if (!Files.exists(CENARIO)) {
            System.out.println("Rode o teste de dentro da pasta Codigo: não encontrei " + CENARIO.toAbsolutePath());
            System.exit(1);
        }
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
        bb = Blackboard.getInstance();
        BlackboardController controlador = new BlackboardController(bb,
                List.of(new ExternoKS(bb), new SubsoloKS(bb), new RelatorioKS(bb)));
        fachada = new FachadaEstacionamento(bb, controlador);

        Path arquivo = Files.createTempFile("eventos-aceitacao", ".csv");
        try {
            secao(() -> roteiroDeDemonstracao(arquivo));
            secao(TesteDeAceitacao::interfaceDeTexto);
        } finally {
            Files.deleteIfExists(arquivo);
        }

        System.out.println();
        System.out.println("Resultado: " + ok + " ok, " + falhas + " falha(s)");
        System.exit(falhas == 0 ? 0 : 1);
    }

    // ------------------------------------------------- roteiro de demonstração

    private static void roteiroDeDemonstracao(Path arquivo) throws IOException {
        titulo("Roteiro de demonstração (roteiro-de-uso.md, seção 8)");
        RegistroDeEventos registro = new RegistroDeEventos(bb, fachada, arquivo);
        bb.adicionarObserver(registro);

        registro.zerar();
        check("D1 Zerar: os dois setores livres e nenhum histórico",
                bb.vagasLivres(Setor.EXTERNO) == 15 && bb.vagasLivres(Setor.SUBSOLO) == 10
                        && bb.getEstadias().isEmpty());

        ResultadoOperacao r = fachada.entrar(Setor.SUBSOLO, "DEM1A01", 2.30);
        check("D2 Altura 2,30 no Subsolo: recusa do SubsoloKS",
                recusa(r, "SubsoloKS", "excede o máximo de 2,10 m"));

        r = fachada.entrar(Setor.SUBSOLO, "DEM1A01", 1.85);
        check("D3 Altura 1,85: o veículo entra e fica circulando",
                aceito(r) && circulando(Setor.SUBSOLO, "DEM1A01"));

        r = fachada.entrar(Setor.EXTERNO, "DEM1A01", null);
        check("D4 A mesma placa no Externo: recusa do ExternoKS, que leu o quadro",
                recusa(r, "ExternoKS", "já está no setor Subsolo"));

        r = fachada.ocupar(Setor.SUBSOLO, "DEM1A01", "A1", null);
        check("D5 A1 sem motivo: recusa, e a vaga continua livre",
                recusa(r, "SubsoloKS", "exige um motivo válido") && bb.getVaga(Setor.SUBSOLO, "A1").isLivre());

        r = fachada.ocupar(Setor.SUBSOLO, "DEM1A01", "A1", MotivoPreferencial.IDOSO);
        check("D6 A1 com o motivo Idoso: a vaga fica com DEM1A01",
                aceito(r) && ocupadaPor(Setor.SUBSOLO, "A1", "DEM1A01"));

        boolean entrou = aceito(fachada.entrar(Setor.EXTERNO, "DEM2B02", null));
        r = fachada.ocupar(Setor.EXTERNO, "DEM2B02", "B3", null);
        check("D7 DEM2B02 entra no Externo e ocupa a B3",
                entrou && aceito(r) && ocupadaPor(Setor.EXTERNO, "B3", "DEM2B02"));

        r = fachada.sair(Setor.SUBSOLO, "DEM1A01");
        boolean saidaRecusada = recusa(r, "SubsoloKS", "ainda ocupa a vaga A1");
        ResultadoOperacao desocupou = fachada.desocupar(Setor.SUBSOLO, "A1");
        ResultadoOperacao saiu = fachada.sair(Setor.SUBSOLO, "DEM1A01");
        check("D8 Saída com o carro na vaga: recusada; as duas requisições do atalho são aceitas",
                saidaRecusada && aceito(desocupou) && aceito(saiu)
                        && bb.getVaga(Setor.SUBSOLO, "A1").isLivre() && bb.getEstadiaAberta("DEM1A01") == null);

        desocupou = fachada.desocupar(Setor.EXTERNO, "B3");
        saiu = fachada.sair(Setor.EXTERNO, "DEM2B02");
        check("D9 A B3 é desocupada, e a saída de DEM2B02 mostra o tempo de permanência",
                aceito(desocupou) && aceito(saiu) && saiu.getMensagem().contains("Tempo de permanência")
                        && bb.getVaga(Setor.EXTERNO, "B3").isLivre());

        Relatorio historico = fachada.gerarRelatorio(TipoRelatorio.HISTORICO_POR_PLACA, "DEM1A01", null, null);
        check("D10 Histórico de DEM1A01: uma passagem só, no Subsolo, na A1, com o motivo Idoso",
                historico != null && historico.getTitulo().equals("HISTÓRICO DA PLACA DEM1A01")
                        && unicaPassagem(historico, "DEM1A01", "SUBSOLO", "A1", "IDOSO"));

        check("D1–D9 O registro gravou só os 8 eventos aceitos, sem as recusas",
                Files.readAllLines(arquivo).size() == 1 + 8);

        bb.reiniciar();
        RegistroDeEventos.Leitura releitura = registro.carregar();
        Relatorio relido = fachada.gerarRelatorio(TipoRelatorio.HISTORICO_POR_PLACA, "DEM1A01", null, null);
        check("Reabrir depois do D9: os 8 eventos são relidos, sem duplicar linhas, e refazem a passagem de DEM1A01",
                releitura.resumo().equals("8 eventos relidos.") && Files.readAllLines(arquivo).size() == 1 + 8
                        && unicaPassagem(relido, "DEM1A01", "SUBSOLO", "A1", "IDOSO"));

        RegistroDeEventos.Leitura leitura = registro.carregarCenario(CENARIO);
        check("D11 Carregar o cenário: \"2204 eventos relidos.\"",
                leitura.resumo().equals("2204 eventos relidos."));

        Relatorio pico = fachada.gerarRelatorio(TipoRelatorio.HORARIOS_DE_PICO, null, null, null);
        check("D12 Horários de pico: pico às 13h, sábado o dia mais cheio, maior lotação de 22 de 25 vagas",
                pico != null && contem(pico, "Pico: 13h") && "Sábado".equals(diaMaisCheio(pico))
                        && contem(pico, "Maior lotação: 22 de 25 vagas (88,0%)"));

        Relatorio frequentes = fachada.gerarRelatorio(TipoRelatorio.CLIENTES_FREQUENTES, null, null, null);
        check("D12 Clientes frequentes: 386 placas, 33 voltaram, UTN8X74 em primeiro com 10 visitas",
                frequentes != null
                        && contem(frequentes, "Placas distintas: 386 · Voltaram (2 visitas ou mais): 33")
                        && primeiroFrequente(frequentes).matches("1\\s+UTN8X74\\s+10\\s.*"));

        Relatorio giro = fachada.gerarRelatorio(TipoRelatorio.GIRO_POR_VAGA, null, null, null);
        check("D12 Giro por vaga: a vaga mais disputada e a mais ociosa",
                giro != null && contem(giro, "Mais disputada:") && contem(giro, "Mais ociosa:"));

        Relatorio completo = fachada.gerarRelatorio(TipoRelatorio.COMPLETO, null, null, null);
        check("D12 Relatório completo: inclui as seções de pico, clientes frequentes e giro",
                completo != null && contem(completo, "HORÁRIOS DE PICO")
                        && contem(completo, "CLIENTES FREQUENTES") && contem(completo, "GIRO POR VAGA"));

        int estadias = bb.getEstadias().size();
        boolean copiaDoCenario = Files.readAllLines(arquivo).equals(Files.readAllLines(CENARIO));
        bb.reiniciar();
        leitura = registro.carregar();
        bb.removerObserver(registro);
        check("D13 Reabrir: o registro é o cenário, e os mesmos 2204 eventos são relidos",
                copiaDoCenario && leitura.resumo().equals("2204 eventos relidos.")
                        && bb.getEstadias().size() == estadias);
    }

    // -------------------------------------------------------- interface de texto

    private static void interfaceDeTexto() {
        titulo("Interface de texto com a entrada simulada");
        bb.reiniciar();
        String entrada = String.join("\n",
                "1", "2", "2.30", "DEM3C03", "1.85",
                "2", "2", "1", "A1", "7", "A3",
                "4", "2", "DEM3C03",
                "3", "2", "A3",
                "4", "2", "DEM3C03",
                "5", "3", "DEM3C03",
                "0") + "\n";

        InputStream entradaOriginal = System.in;
        PrintStream saidaOriginal = System.out;
        ByteArrayOutputStream capturada = new ByteArrayOutputStream();
        Display display = new Display(bb);
        bb.adicionarObserver(display);
        try {
            System.setIn(new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(capturada, true, StandardCharsets.UTF_8));
            new CLI(bb, fachada, display).executar();
        } finally {
            System.setOut(saidaOriginal);
            System.setIn(entradaOriginal);
            bb.removerObserver(display);
        }
        String saida = capturada.toString(StandardCharsets.UTF_8);

        int recusaAltura = saida.indexOf("⚠ Veículo não permitido no Subsolo");
        int entrada1 = saida.indexOf("✓ Veículo DEM3C03 entrou no setor Subsolo");
        String entreAsDuas = recusaAltura >= 0 && entrada1 > recusaAltura
                ? saida.substring(recusaAltura, entrada1) : "";
        check("CLI: a altura 2.30 é recusada, e só a altura é pedida de novo",
                entreAsDuas.contains("Altura do veículo") && !entreAsDuas.contains("Placa do veículo"));
        check("CLI: a A1 sem motivo é recusada, e a A3 é aceita na nova escolha",
                saida.contains("⚠ Vaga A1 é preferencial e exige um motivo válido")
                        && saida.contains("✓ Veículo DEM3C03 estacionado em A3"));
        int recusaSaida = saida.indexOf("⚠ O veículo DEM3C03 ainda ocupa a vaga A3");
        int liberada = saida.indexOf("✓ Vaga A3 liberada");
        int saiu = saida.indexOf("✓ Veículo DEM3C03 saiu do setor Subsolo");
        check("CLI: a saída com o carro na vaga é recusada; depois de desocupar, é aceita",
                recusaSaida >= 0 && recusaSaida < liberada && liberada < saiu);
        check("CLI: o histórico por placa é impresso",
                saida.contains("HISTÓRICO DA PLACA DEM3C03"));
        check("CLI: no fim, nenhum veículo dentro e a A3 livre",
                bb.getEstadiaAberta("DEM3C03") == null && bb.getVaga(Setor.SUBSOLO, "A3").isLivre());
        bb.reiniciar();
    }

    // -------------------------------------------------------------------- apoio

    /** Roda uma seção; uma exceção vira uma falha, e o teste segue até o resumo. */
    private static void secao(Secao secao) {
        try {
            secao.executar();
        } catch (Exception e) {
            check("A seção terminou sem exceção (" + e + ")", false);
        }
    }

    /** Uma só linha da placa no histórico, com todos os trechos, e o total de uma estadia. */
    private static boolean unicaPassagem(Relatorio r, String placa, String... trechos) {
        if (r == null) return false;
        List<String> daPlaca = r.getLinhas().stream().filter(l -> l.startsWith(placa)).toList();
        return daPlaca.size() == 1 && List.of(trechos).stream().allMatch(daPlaca.get(0)::contains)
                && r.getLinhas().contains("Total: 1 estadia");
    }

    private static boolean aceito(ResultadoOperacao r) {
        return r != null && r.isSucesso();
    }

    private static boolean recusa(ResultadoOperacao r, String origem, String trecho) {
        return r != null && !r.isSucesso() && origem.equals(r.getOrigem()) && r.getMensagem().contains(trecho);
    }

    private static boolean circulando(Setor setor, String placa) {
        return bb.getVeiculosCirculando(setor).stream().anyMatch(e -> e.getPlaca().equals(placa));
    }

    private static boolean ocupadaPor(Setor setor, String idVaga, String placa) {
        Vaga vaga = bb.getVaga(setor, idVaga);
        return !vaga.isLivre() && vaga.getOcupacaoAtual() != null
                && placa.equals(vaga.getOcupacaoAtual().getPlaca());
    }

    private static boolean contem(Relatorio r, String trecho) {
        return r.getLinhas().stream().anyMatch(l -> l.contains(trecho));
    }

    /** O dia com mais entradas, lido das linhas "Entradas por dia da semana". */
    private static String diaMaisCheio(Relatorio r) {
        List<String> linhas = r.getLinhas();
        int inicio = linhas.indexOf("Entradas por dia da semana:");
        if (inicio < 0) return null;
        String dia = null;
        int maximo = -1;
        for (int i = inicio + 1; i < linhas.size() && !linhas.get(i).isBlank(); i++) {
            String[] partes = linhas.get(i).trim().split("\\s+");
            int entradas = Integer.parseInt(partes[partes.length - 1]);
            if (entradas > maximo) {
                maximo = entradas;
                dia = partes[0];
            }
        }
        return dia;
    }

    /** A primeira linha da tabela de clientes frequentes, logo abaixo do cabeçalho "#". */
    private static String primeiroFrequente(Relatorio r) {
        List<String> linhas = r.getLinhas();
        for (int i = 0; i < linhas.size() - 1; i++) {
            if (linhas.get(i).startsWith("#")) return linhas.get(i + 1).trim();
        }
        return "";
    }

    private static void titulo(String t) {
        System.out.println("\n== " + t);
    }

    private static void check(String descricao, boolean condicao) {
        if (condicao) ok++; else falhas++;
        System.out.println((condicao ? "  [OK]    " : "  [FALHA] ") + descricao);
    }
}
