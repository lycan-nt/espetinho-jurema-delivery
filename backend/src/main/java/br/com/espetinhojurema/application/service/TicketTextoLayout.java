package br.com.espetinhojurema.application.service;

/**
 * Largura única ({@link #COLUNAS}) para traços e cabeçalhos na bobina — mesmo comprimento que
 * {@code ======== COMANDA COZINHA ========} (~32 cols) para alinhar com referência visual sem quebra.
 */
public final class TicketTextoLayout {

    public static final int COLUNAS = 32;

    private TicketTextoLayout() {}

    public static String linhaIguais() {
        return "=".repeat(COLUNAS) + '\n';
    }

    public static String linhaMenos() {
        return "-".repeat(COLUNAS) + '\n';
    }

    /** Uma linha de exatamente {@link #COLUNAS} caracteres: {@code === título ===} com bordas de =. */
    public static String linhaTituloEntreIguais(String titulo) {
        String inner = " " + titulo.strip() + " ";
        int w = COLUNAS;
        if (inner.length() > w) {
            return inner.substring(0, w) + '\n';
        }
        int bordas = w - inner.length();
        int esq = bordas / 2;
        int dir = bordas - esq;
        return "=".repeat(esq) + inner + "=".repeat(dir) + '\n';
    }

    /** Centraliza em espaços em uma linha de {@link #COLUNAS} caracteres (trunca se maior). */
    public static String linhaCentralizada(String texto) {
        String t = texto.strip();
        if (t.length() >= COLUNAS) {
            return t.substring(0, COLUNAS) + '\n';
        }
        int pad = COLUNAS - t.length();
        int left = pad / 2;
        int right = pad - left;
        return " ".repeat(left) + t + " ".repeat(right) + '\n';
    }
}
