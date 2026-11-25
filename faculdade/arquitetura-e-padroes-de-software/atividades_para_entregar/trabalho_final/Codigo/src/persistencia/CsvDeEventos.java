package persistencia;

import model.MotivoPreferencial;
import model.Requisicao;
import model.Setor;
import model.TipoRequisicao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Formato de uma linha do registro de eventos, com os campos separados por ponto e vírgula:
 * momento;tipo;setor;placa;altura;vaga;motivo. Campo vazio = não se aplica ao evento.
 * Exemplo: 2026-11-18T09:15:00;OCUPAR;SUBSOLO;ABC1D23;;A1;IDOSO
 */
public final class CsvDeEventos {

    public static final String CABECALHO = "momento;tipo;setor;placa;altura;vaga;motivo";
    private static final String SEPARADOR = ";";
    private static final int CAMPOS = 7;
    private static final DateTimeFormatter MOMENTO = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

    private CsvDeEventos() {}

    /** Linha de um evento; o horário é gravado até os segundos. */
    public static String linha(Requisicao r) {
        return String.join(SEPARADOR,
                r.getMomento().format(MOMENTO),
                r.getTipo().name(),
                r.getSetor().name(),
                texto(r.getPlaca()),
                r.getAltura() == null ? "" : String.valueOf(r.getAltura()),
                texto(r.getIdVaga()),
                r.getMotivo() == null ? "" : r.getMotivo().name());
    }

    /** Requisição de uma linha; lança IllegalArgumentException, com o motivo, se a linha estiver malformada. */
    public static Requisicao requisicao(String linha) {
        String[] c = linha.split(SEPARADOR, -1);
        if (c.length != CAMPOS) {
            throw new IllegalArgumentException("esperava " + CAMPOS + " campos e encontrou " + c.length);
        }
        LocalDateTime momento;
        try {
            momento = LocalDateTime.parse(c[0], MOMENTO);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("horário inválido: " + c[0]);
        }
        TipoRequisicao tipo = TipoRequisicao.valueOf(c[1]);
        if (!tipo.isEvento()) {
            throw new IllegalArgumentException(c[1] + " não é um evento");
        }
        return new Requisicao()
                .setMomento(momento)
                .setTipo(tipo)
                .setSetor(Setor.valueOf(c[2]))
                .setPlaca(vazioViraNull(c[3]))
                .setAltura(c[4].isEmpty() ? null : Double.valueOf(c[4]))
                .setIdVaga(vazioViraNull(c[5]))
                .setMotivo(c[6].isEmpty() ? null : MotivoPreferencial.valueOf(c[6]));
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private static String vazioViraNull(String valor) {
        return valor.isEmpty() ? null : valor;
    }
}
