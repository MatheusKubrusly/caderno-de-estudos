import blackboard.Blackboard;
import model.MotivoPreferencial;
import model.Requisicao;
import model.Setor;
import model.TipoRequisicao;
import model.Vaga;
import persistencia.CsvDeEventos;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

/**
 * Gera o cenário de demonstração: uma semana simulada de uso (16 a 22/11/2026), com
 * placas fictícias, no mesmo formato do registro de eventos. A semente do sorteio é
 * fixa, então o arquivo sai sempre igual. A simulação só propõe eventos coerentes;
 * quem os valida, ao carregar o cenário, continuam sendo os especialistas.
 *
 * Uso: java -cp out GeradorDeCenario dados/cenario-demonstracao.csv
 */
public class GeradorDeCenario {

    private static final long SEMENTE = 20261116L;
    private static final LocalDate PRIMEIRO_DIA = LocalDate.of(2026, 11, 16);
    private static final int[] VISITAS_POR_DIA = {60, 62, 66, 70, 85, 105, 95};       // segunda a domingo
    private static final int[] PESO_POR_HORA = {3, 5, 9, 9, 6, 5, 5, 6, 9, 10, 8, 3}; // das 10h às 21h
    private static final int FREQUENTES = 40;
    private static final double ALTURA_MAXIMA_SUBSOLO = 2.10;
    private static final MotivoPreferencial[] MOTIVOS = MotivoPreferencial.values();

    private record Veiculo(String placa, double altura, MotivoPreferencial motivo) {}

    private enum Passo { CHEGAR, ESTACIONAR, LIBERAR_VAGA, SAIR }

    /** Uma visita em andamento: o veículo, o setor e a vaga que ocupa agora. */
    private static final class Visita {
        Veiculo veiculo;
        Setor setor;
        String vaga;
        int tentativas;
        LocalDateTime fimDaPermanencia;
        boolean vaiTrocarDeVaga;
    }

    private record Acao(LocalDateTime quando, long ordem, Passo passo, Visita visita) {}

    private final Random sorteio = new Random(SEMENTE);
    private final PriorityQueue<Acao> agenda =
            new PriorityQueue<>(Comparator.comparing(Acao::quando).thenComparingLong(Acao::ordem));
    private final Map<Setor, List<Vaga>> vagas = new EnumMap<>(Setor.class);
    private final Set<String> ocupadas = new HashSet<>();      // "SETOR:A1"
    private final Set<String> dentro = new HashSet<>();        // placas no estacionamento
    private final Set<String> placasUsadas = new HashSet<>();
    private final List<Veiculo> frequentes = new ArrayList<>();
    private final int[] pesoDosFrequentes = new int[FREQUENTES];
    private final List<String> linhas = new ArrayList<>();
    private long proximaOrdem;

    public static void main(String[] args) throws IOException {
        Path destino = Path.of(args.length > 0 ? args[0] : "dados/cenario-demonstracao.csv");
        GeradorDeCenario gerador = new GeradorDeCenario();
        gerador.simular();

        List<String> arquivo = new ArrayList<>();
        arquivo.add(CsvDeEventos.CABECALHO);
        arquivo.addAll(gerador.linhas);
        Path pasta = destino.toAbsolutePath().getParent();
        if (pasta != null) Files.createDirectories(pasta);
        Files.write(destino, arquivo, StandardCharsets.UTF_8);
        System.out.println(gerador.linhas.size() + " eventos gravados em " + destino);
    }

    private void simular() {
        Blackboard blackboard = Blackboard.getInstance();
        for (Setor s : Setor.values()) vagas.put(s, blackboard.getVagas(s));
        for (int i = 0; i < FREQUENTES; i++) {
            frequentes.add(novoVeiculo(i % 9 == 4));
            pesoDosFrequentes[i] = FREQUENTES - i;
        }

        for (int d = 0; d < VISITAS_POR_DIA.length; d++) {
            LocalDate dia = PRIMEIRO_DIA.plusDays(d);
            for (int i = 0; i < VISITAS_POR_DIA[d]; i++) {
                int hora = 10 + sortearPosicao(PESO_POR_HORA);
                agendar(dia.atTime(hora, sorteio.nextInt(60), sorteio.nextInt(60)), Passo.CHEGAR, new Visita());
            }
        }
        while (!agenda.isEmpty()) {
            executar(agenda.poll());
        }
    }

    private void executar(Acao acao) {
        Visita v = acao.visita();
        LocalDateTime agora = acao.quando();
        switch (acao.passo()) {
            case CHEGAR -> chegar(v, agora);
            case ESTACIONAR -> estacionar(v, agora);
            case LIBERAR_VAGA -> liberarVaga(v, agora);
            case SAIR -> sair(v, agora);
        }
    }

    private void chegar(Visita v, LocalDateTime agora) {
        v.veiculo = escolherVeiculo();
        Veiculo carro = v.veiculo;
        if (carro.altura() > ALTURA_MAXIMA_SUBSOLO) {
            v.setor = Setor.EXTERNO;
        } else if (carro.motivo() != null) {
            v.setor = sorteio.nextDouble() < 0.7 ? Setor.SUBSOLO : Setor.EXTERNO;
        } else {
            v.setor = sorteio.nextDouble() < 0.4 ? Setor.SUBSOLO : Setor.EXTERNO;
        }
        v.vaiTrocarDeVaga = sorteio.nextDouble() < 0.04;
        dentro.add(carro.placa());
        emitir(agora, new Requisicao().setTipo(TipoRequisicao.ENTRAR).setSetor(v.setor).setPlaca(carro.placa())
                .setAltura(v.setor == Setor.SUBSOLO ? carro.altura() : null));
        int espera = 1 + sorteio.nextInt(4) + (livres(v.setor, false) < 3 ? 3 : 0);
        agendar(agora.plusMinutes(espera).plusSeconds(sorteio.nextInt(60)), Passo.ESTACIONAR, v);
    }

    private void estacionar(Visita v, LocalDateTime agora) {
        boolean preferencial = v.veiculo.motivo() != null && v.setor == Setor.SUBSOLO && livres(v.setor, true) > 0;
        String vaga = sortearVagaLivre(v.setor, preferencial, v.vaga);
        if (vaga == null) {
            v.tentativas++;
            if (v.tentativas < 3) {
                agendar(agora.plusMinutes(3 + sorteio.nextInt(3)), Passo.ESTACIONAR, v);
            } else {
                agendar(agora.plusMinutes(1 + sorteio.nextInt(2)), Passo.SAIR, v);   // desistiu sem estacionar
            }
            return;
        }
        v.vaga = vaga;
        ocupadas.add(chave(v.setor, vaga));
        emitir(agora, new Requisicao().setTipo(TipoRequisicao.OCUPAR).setSetor(v.setor)
                .setPlaca(v.veiculo.placa()).setIdVaga(vaga).setMotivo(preferencial ? v.veiculo.motivo() : null));
        if (v.fimDaPermanencia == null) {
            LocalDateTime limite = agora.toLocalDate().atTime(22, 20);
            LocalDateTime fim = agora.plusMinutes(sortearPermanencia());
            v.fimDaPermanencia = fim.isAfter(limite) ? max(limite, agora.plusMinutes(10)) : fim;
        }
        LocalDateTime liberar = v.vaiTrocarDeVaga
                ? agora.plusSeconds(Duration.between(agora, v.fimDaPermanencia).getSeconds() / 2)
                : v.fimDaPermanencia;
        agendar(liberar, Passo.LIBERAR_VAGA, v);
    }

    private void liberarVaga(Visita v, LocalDateTime agora) {
        ocupadas.remove(chave(v.setor, v.vaga));
        emitir(agora, new Requisicao().setTipo(TipoRequisicao.DESOCUPAR).setSetor(v.setor).setIdVaga(v.vaga));
        if (v.vaiTrocarDeVaga) {
            v.vaiTrocarDeVaga = false;
            v.tentativas = 0;
            agendar(agora.plusMinutes(2), Passo.ESTACIONAR, v);
        } else {
            agendar(agora.plusMinutes(1 + sorteio.nextInt(4)), Passo.SAIR, v);
        }
    }

    private void sair(Visita v, LocalDateTime agora) {
        dentro.remove(v.veiculo.placa());
        emitir(agora, new Requisicao().setTipo(TipoRequisicao.SAIR).setSetor(v.setor).setPlaca(v.veiculo.placa()));
    }

    // ------------------------------------------------------------- sorteios

    private Veiculo escolherVeiculo() {
        if (sorteio.nextDouble() < 0.35) {
            for (int tentativa = 0; tentativa < 5; tentativa++) {
                Veiculo candidato = frequentes.get(sortearPosicao(pesoDosFrequentes));
                if (!dentro.contains(candidato.placa())) return candidato;
            }
        }
        return novoVeiculo(sorteio.nextDouble() < 0.05);
    }

    private Veiculo novoVeiculo(boolean comMotivo) {
        String placa;
        do {
            placa = "" + letra() + letra() + letra() + sorteio.nextInt(10) + letra()
                    + sorteio.nextInt(10) + sorteio.nextInt(10);
        } while (!placasUsadas.add(placa));
        double altura = sorteio.nextDouble() < 0.06
                ? 2.15 + sorteio.nextInt(26) / 100.0
                : 1.40 + sorteio.nextInt(56) / 100.0;
        MotivoPreferencial motivo = comMotivo ? MOTIVOS[sorteio.nextInt(MOTIVOS.length)] : null;
        return new Veiculo(placa, altura, motivo);
    }

    private char letra() {
        return (char) ('A' + sorteio.nextInt(26));
    }

    /** Sorteia uma posição do vetor, com chance proporcional ao peso de cada uma. */
    private int sortearPosicao(int[] pesos) {
        int total = 0;
        for (int p : pesos) total += p;
        int r = sorteio.nextInt(total);
        for (int i = 0; i < pesos.length; i++) {
            r -= pesos[i];
            if (r < 0) return i;
        }
        return pesos.length - 1;
    }

    /** Minutos na vaga: muitas visitas curtas e poucas longas (média perto de 1h45), entre 20 min e 5 h. */
    private long sortearPermanencia() {
        double exponencial = -Math.log(1 - sorteio.nextDouble()) * 85;
        return Math.min(300, 20 + Math.round(exponencial));
    }

    private String sortearVagaLivre(Setor setor, boolean preferencial, String evitar) {
        List<String> candidatas = new ArrayList<>();
        for (Vaga vaga : vagas.get(setor)) {
            if (vaga.isPreferencial() == preferencial && !ocupadas.contains(chave(setor, vaga.getId()))
                    && !vaga.getId().equals(evitar)) {
                candidatas.add(vaga.getId());
            }
        }
        return candidatas.isEmpty() ? null : candidatas.get(sorteio.nextInt(candidatas.size()));
    }

    private int livres(Setor setor, boolean preferencial) {
        int n = 0;
        for (Vaga vaga : vagas.get(setor)) {
            if (vaga.isPreferencial() == preferencial && !ocupadas.contains(chave(setor, vaga.getId()))) n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- apoio

    private void agendar(LocalDateTime quando, Passo passo, Visita visita) {
        agenda.add(new Acao(quando, proximaOrdem++, passo, visita));
    }

    private void emitir(LocalDateTime quando, Requisicao requisicao) {
        linhas.add(CsvDeEventos.linha(requisicao.setMomento(quando)));
    }

    private static String chave(Setor setor, String idVaga) {
        return setor + ":" + idVaga;
    }

    private static LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }
}
