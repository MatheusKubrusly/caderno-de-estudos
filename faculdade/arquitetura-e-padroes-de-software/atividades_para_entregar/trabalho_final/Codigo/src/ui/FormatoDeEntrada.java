package ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Validação de FORMATO do que o operador digita, comum à CLI e à janela.
 * Não decide regra de negócio (altura máxima, vaga ocupada, motivo): isso é dos especialistas.
 */
public final class FormatoDeEntrada {

    public static final DateTimeFormatter DATA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);

    private FormatoDeEntrada() {}

    /** Placa em maiúsculas, ou null se não tiver de 5 a 8 letras, números ou hífen. */
    public static String placa(String texto) {
        if (texto == null) return null;
        String placa = texto.trim().toUpperCase();
        return placa.matches("[A-Z0-9-]{5,8}") ? placa : null;
    }

    /** Altura em metros (aceita vírgula ou ponto), ou null se não for um número positivo. */
    public static Double altura(String texto) {
        if (texto == null) return null;
        try {
            double altura = Double.parseDouble(texto.trim().replace(',', '.'));
            return altura > 0 ? altura : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Data e hora no formato dd/MM/aaaa HH:mm, ou null se o texto não seguir o formato. */
    public static LocalDateTime dataHora(String texto) {
        if (texto == null) return null;
        try {
            return LocalDateTime.parse(texto.trim(), DATA_HORA);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
