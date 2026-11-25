package util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatação de datas, durações e percentuais no padrão brasileiro. */
public final class Formatador {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATA_HORA_CURTA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Formatador() {}

    public static String dataHora(LocalDateTime t) {
        return t.format(DATA_HORA);
    }

    /** "14/11 18:52" — usada em tabelas para caber em uma linha. */
    public static String dataHoraCurta(LocalDateTime t) {
        return t.format(DATA_HORA_CURTA);
    }

    public static String hora(LocalDateTime t) {
        return t.format(HORA);
    }

    /** "23min", "1h 12min", "2h 05min" ou "menos de 1min". */
    public static String duracao(Duration d) {
        long min = d.toMinutes();
        if (min < 1) return "menos de 1min";
        long h = min / 60;
        long m = min % 60;
        if (h == 0) return m + "min";
        return h + "h " + String.format("%02d", m) + "min";
    }

    /** "66,7%". Total zero devolve "0,0%". */
    public static String percentual(long parte, long total) {
        if (total == 0) return "0,0%";
        return String.format(PT_BR, "%.1f%%", 100.0 * parte / total);
    }

    public static String altura(double metros) {
        return String.format(PT_BR, "%.2f m", metros);
    }
}
