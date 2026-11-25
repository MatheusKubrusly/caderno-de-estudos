package ui;

import blackboard.Blackboard;
import blackboard.Observer;
import model.*;
import util.Formatador;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Observer do Blackboard e único componente que renderiza o estado.
 * Não guarda estado de domínio: lê o Blackboard. Só lembra quais objetos
 * já exibiu, para mostrar cada resultado/relatório uma única vez.
 */
public class Display implements Observer {

    private static final int LARGURA = 58;

    private final Blackboard blackboard;
    private ResultadoOperacao resultadoExibido;
    private Relatorio relatorioExibido;

    public Display(Blackboard blackboard) {
        this.blackboard = blackboard;
    }

    /** Chamado pelo Blackboard ao publicar um resultado ou um relatório. */
    @Override
    public void atualizar() {
        ResultadoOperacao resultado = blackboard.getUltimoResultado();
        if (resultado != null && resultado != resultadoExibido) {
            resultadoExibido = resultado;
            renderizarResultado(resultado);
        }
        Relatorio relatorio = blackboard.getUltimoRelatorio();
        if (relatorio != null && relatorio != relatorioExibido) {
            relatorioExibido = relatorio;
            renderizarRelatorio(relatorio);
        }
    }

    // ------------------------------------------------------------------ menus

    public void renderizarMenu() {
        int w = 38;
        System.out.println();
        System.out.println("╔" + "═".repeat(w) + "╗");
        System.out.println("║" + String.format("%-" + w + "s", "   ESTACIONAMENTO — SHOPPING CENTER") + "║");
        System.out.println("╠" + "═".repeat(w) + "╣");
        System.out.println("║" + String.format("%-" + w + "s", "  1. Entrar no setor (cancela)") + "║");
        System.out.println("║" + String.format("%-" + w + "s", "  2. Ocupar uma vaga") + "║");
        System.out.println("║" + String.format("%-" + w + "s", "  3. Desocupar uma vaga") + "║");
        System.out.println("║" + String.format("%-" + w + "s", "  4. Sair do setor (cancela)") + "║");
        System.out.println("║" + String.format("%-" + w + "s", "  5. Relatórios") + "║");
        System.out.println("║" + String.format("%-" + w + "s", "  0. Encerrar o sistema") + "║");
        System.out.println("╚" + "═".repeat(w) + "╝");
    }

    public void renderizarSetores() {
        Setor ex = Setor.EXTERNO;
        Setor su = Setor.SUBSOLO;
        System.out.println();
        System.out.println("Selecione o setor:");
        System.out.println("  1. " + ex.descricao() + "  — Comuns: " + blackboard.vagasLivresComuns(ex) + "/"
                + blackboard.totalComuns(ex) + " livres  |  Circulando: "
                + blackboard.getVeiculosCirculando(ex).size());
        System.out.println("  2. " + su.descricao() + "  — Comuns: " + blackboard.vagasLivresComuns(su) + "/"
                + blackboard.totalComuns(su) + " livres  |  Preferenciais: "
                + blackboard.vagasLivresPreferenciais(su) + "/" + blackboard.totalPreferenciais(su)
                + " livres  |  Circulando: " + blackboard.getVeiculosCirculando(su).size());
        System.out.println("  0. Voltar");
    }

    public void renderizarMenuMotivos() {
        int w = 54;
        System.out.println();
        System.out.println("┌─ VAGA PREFERENCIAL " + "─".repeat(w - 20) + "┐");
        System.out.println("│" + String.format("%-" + w + "s", " Informe o motivo de uso:") + "│");
        MotivoPreferencial[] motivos = MotivoPreferencial.values();
        for (int i = 0; i < motivos.length; i++) {
            System.out.println("│" + String.format("%-" + w + "s", " " + (i + 1) + ". " + motivos[i].descricao()) + "│");
        }
        System.out.println("│" + String.format("%-" + w + "s", " " + (motivos.length + 1) + ". Nenhum dos anteriores") + "│");
        System.out.println("└" + "─".repeat(w) + "┘");
    }

    public void renderizarMenuRelatorios() {
        System.out.println();
        System.out.println("RELATÓRIOS");
        System.out.println("  1. Ocupação por setor");
        System.out.println("  2. Histórico geral");
        System.out.println("  3. Histórico por placa");
        System.out.println("  4. Histórico por período");
        System.out.println("  5. Tempo médio de permanência");
        System.out.println("  6. Uso de vagas preferenciais");
        System.out.println("  7. Relatório completo (todos os relatórios)");
        System.out.println("  0. Voltar");
    }

    /** Lista numerada, na mesma ordem em que a CLI a recebeu do Blackboard. */
    public void renderizarVeiculosCirculando(List<Estadia> circulando) {
        LocalDateTime agora = LocalDateTime.now();
        System.out.println();
        System.out.println("Veículos circulando:");
        for (int i = 0; i < circulando.size(); i++) {
            Estadia e = circulando.get(i);
            System.out.println("  " + (i + 1) + ". " + e.getPlaca() + "  — circulando há "
                    + Formatador.duracao(Duration.between(e.getUltimoEvento(), agora)));
        }
        System.out.println("  0. Voltar");
    }

    // ---------------------------------------------------------------- matriz

    public void renderizarMatriz(Setor setor) {
        List<Vaga> vagas = blackboard.getVagas(setor);

        Map<Character, List<Vaga>> porFileira = new TreeMap<>();
        int colunas = 0;
        for (Vaga v : vagas) {
            porFileira.computeIfAbsent(v.getFileira(), k -> new ArrayList<>()).add(v);
            colunas = Math.max(colunas, v.getPosicao());
        }

        System.out.println();
        System.out.println(borda('╔', '╗'));
        if (setor == Setor.EXTERNO) {
            System.out.println(linha("  SETOR " + setor.descricao().toUpperCase() + "   │  Livres: "
                    + blackboard.vagasLivres(setor) + "/" + blackboard.totalVagas(setor)));
        } else {
            System.out.println(linha("  SETOR " + setor.descricao().toUpperCase() + " (Coberto)"));
            System.out.println(linha("  Comuns: " + blackboard.vagasLivresComuns(setor) + "/"
                    + blackboard.totalComuns(setor) + " livres  |  Preferenciais: "
                    + blackboard.vagasLivresPreferenciais(setor) + "/"
                    + blackboard.totalPreferenciais(setor) + " livres"));
        }
        System.out.println(divisoria());

        StringBuilder cab = new StringBuilder("     ");
        for (int c = 1; c <= colunas; c++) {
            cab.append("  ").append(c).append("   ").append(c < colunas ? "  " : "");
        }
        System.out.println(linha(cab.toString()));

        for (Map.Entry<Character, List<Vaga>> e : porFileira.entrySet()) {
            StringBuilder sb = new StringBuilder("  " + e.getKey() + "  ");
            List<Vaga> fila = e.getValue();
            for (int i = 0; i < fila.size(); i++) {
                sb.append(celula(fila.get(i)));
                if (i < fila.size() - 1) sb.append("  ");
            }
            System.out.println(linha(sb.toString()));
        }

        System.out.println(divisoria());
        System.out.println(linha("  [ A1 ] Livre    [XXXX] Ocupada"));
        if (blackboard.totalPreferenciais(setor) > 0) {
            System.out.println(linha("  [*A1 ] Preferencial livre   [**A2] Preferencial ocupada"));
        }
        System.out.println(borda('╚', '╝'));
    }

    private String celula(Vaga v) {
        if (v.isPreferencial()) {
            return v.isLivre() ? "[*" + v.getId() + " ]" : "[**" + v.getId() + "]";
        }
        return v.isLivre() ? "[ " + v.getId() + " ]" : "[XXXX]";
    }

    private String borda(char esq, char dir) {
        return esq + "═".repeat(LARGURA) + dir;
    }

    private String divisoria() {
        return "╠" + "═".repeat(LARGURA) + "╣";
    }

    private String linha(String conteudo) {
        return "║" + String.format("%-" + LARGURA + "s", conteudo) + "║";
    }

    // ------------------------------------------------ resultados e relatórios

    private void renderizarResultado(ResultadoOperacao r) {
        System.out.println();
        System.out.println((r.isSucesso() ? "✓ " : "⚠ ") + r.getMensagem());
    }

    private void renderizarRelatorio(Relatorio r) {
        System.out.println();
        System.out.println("═══ " + r.getTitulo() + " ═══");
        for (String linha : r.getLinhas()) {
            System.out.println(linha);
        }
        System.out.println();
    }

    /** Escreve o prompt sem quebrar linha; a leitura continua na CLI. */
    public void renderizarPrompt(String prompt) {
        System.out.print(prompt);
        System.out.flush();
    }

    /** Aviso de formato inválido durante a coleta; não é regra de negócio. */
    public void renderizarAviso(String mensagem) {
        System.out.println(mensagem);
    }

    public void renderizarEncerramento() {
        System.out.println();
        System.out.println("Sistema encerrado. Até logo!");
    }
}
