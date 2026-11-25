package ui;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Configuração do terminal: força UTF-8 na saída para os caracteres de desenho da matriz. */
public final class Terminal {

    private Terminal() {}

    public static void configurarUtf8() {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
    }
}
