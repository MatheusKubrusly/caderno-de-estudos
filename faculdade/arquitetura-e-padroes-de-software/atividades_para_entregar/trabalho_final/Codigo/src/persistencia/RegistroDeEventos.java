package persistencia;

import aplicacao.FachadaEstacionamento;
import blackboard.Blackboard;
import blackboard.Observer;
import model.Requisicao;
import model.ResultadoOperacao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro durável do uso do estacionamento (Observer do Blackboard).
 *
 * Anota cada evento aceito (entrar, ocupar, desocupar, sair) numa linha de um arquivo
 * CSV. Ao abrir o sistema, relê o arquivo e reenvia os eventos pela fachada: os
 * especialistas reconstroem o Blackboard, e nada é escrito nele por fora.
 * Não conhece nenhum especialista.
 */
public class RegistroDeEventos implements Observer {

    /** Resultado de uma releitura: eventos aceitos e linhas puladas, com o motivo. */
    public record Leitura(int aceitos, List<String> problemas) {

        /** Ex.: "540 eventos relidos; 2 linhas puladas (linha 8: horário inválido: ...)". */
        public String resumo() {
            String texto = aceitos + (aceitos == 1 ? " evento relido" : " eventos relidos");
            if (problemas.isEmpty()) return texto + ".";
            return texto + "; " + problemas.size() + (problemas.size() == 1 ? " linha pulada (" : " linhas puladas (")
                    + problemas.get(0) + (problemas.size() > 1 ? "; ..." : "") + ").";
        }
    }

    private final Blackboard blackboard;
    private final FachadaEstacionamento fachada;
    private final Path arquivo;
    private ResultadoOperacao ultimoVisto;
    private String falhaDeGravacao;

    public RegistroDeEventos(Blackboard blackboard, FachadaEstacionamento fachada, Path arquivo) {
        this.blackboard = blackboard;
        this.fachada = fachada;
        this.arquivo = arquivo;
    }

    public Path getArquivo() {
        return arquivo;
    }

    /** Última falha ao gravar no arquivo, ou null; o front-end decide como avisar. */
    public String getFalhaDeGravacao() {
        return falhaDeGravacao;
    }

    /** Chamado pelo Blackboard: grava o resultado novo se ele for um evento aceito. */
    @Override
    public void atualizar() {
        ResultadoOperacao r = blackboard.getUltimoResultado();
        if (r == null || r == ultimoVisto) return;
        ultimoVisto = r;
        Requisicao req = r.getRequisicao();
        if (!r.isSucesso() || req == null || req.getTipo() == null || !req.getTipo().isEvento()) return;
        try {
            if (!Files.exists(arquivo)) iniciarArquivo();
            Files.writeString(arquivo, CsvDeEventos.linha(req) + "\n",
                    StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            falhaDeGravacao = null;
        } catch (IOException e) {
            falhaDeGravacao = "Não foi possível gravar em " + arquivo + ": " + e.getMessage();
        }
    }

    /** Relê o arquivo, se ele existir, e reenvia cada evento pela fachada. Espera um Blackboard vazio. */
    public Leitura carregar() throws IOException {
        if (!Files.exists(arquivo)) return new Leitura(0, List.of());
        return reproduzir(Files.readAllLines(arquivo, StandardCharsets.UTF_8));
    }

    /** Troca o uso registrado pelo cenário: copia o cenário por cima do arquivo, reinicia o Blackboard e relê. */
    public Leitura carregarCenario(Path cenario) throws IOException {
        criarPasta();
        Files.copy(cenario, arquivo, StandardCopyOption.REPLACE_EXISTING);
        blackboard.reiniciar();
        return carregar();
    }

    /** Apaga o uso registrado (fica só o cabeçalho) e reinicia o Blackboard. */
    public void zerar() throws IOException {
        iniciarArquivo();
        blackboard.reiniciar();
    }

    private Leitura reproduzir(List<String> linhas) {
        int[] aceitos = {0};
        List<String> problemas = new ArrayList<>();
        blackboard.executarEmLote(() -> {
            for (int i = 0; i < linhas.size(); i++) {
                String linha = linhas.get(i).strip();
                if (linha.isEmpty() || linha.equals(CsvDeEventos.CABECALHO)) continue;
                Requisicao req;
                try {
                    req = CsvDeEventos.requisicao(linha);
                } catch (IllegalArgumentException e) {
                    problemas.add("linha " + (i + 1) + ": " + e.getMessage());
                    continue;
                }
                ResultadoOperacao r = fachada.enviar(req);
                if (r != null && r.isSucesso()) {
                    aceitos[0]++;
                } else {
                    problemas.add("linha " + (i + 1) + ": recusada — "
                            + (r == null ? "sem resposta" : r.getMensagem()));
                }
            }
            ultimoVisto = blackboard.getUltimoResultado();   // o que foi relido não é gravado de novo
        });
        return new Leitura(aceitos[0], problemas);
    }

    private void iniciarArquivo() throws IOException {
        criarPasta();
        Files.writeString(arquivo, CsvDeEventos.CABECALHO + "\n", StandardCharsets.UTF_8);
    }

    private void criarPasta() throws IOException {
        Path pasta = arquivo.toAbsolutePath().getParent();
        if (pasta != null) Files.createDirectories(pasta);
    }
}
