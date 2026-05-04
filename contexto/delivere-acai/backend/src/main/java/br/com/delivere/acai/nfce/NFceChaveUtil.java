package br.com.delivere.acai.nfce;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Gera a chave de acesso da NFC-e (44 dígitos) e código numérico (cNF).
 * cUF=29 (Bahia), mod=65 (NFC-e), tpEmis=1 (normal).
 */
public final class NFceChaveUtil {

    private static final int UF_BA = 29;
    private static final int MOD_NFCE = 65;
    private static final int TP_EMIS_NORMAL = 1;
    private static final SecureRandom RND = new SecureRandom();

    private NFceChaveUtil() {
    }

    /**
     * Gera chave de 44 dígitos: cUF(2) + AAMM(4) + CNPJ(14) + mod(2) + serie(3) + nNF(9) + tpEmis(1) + cNF(8) + cDV(1).
     */
    public static String gerarChave(String cnpj14, int serie, int numeroNf) {
        String aamm = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMM"));
        String cnpj = cnpj14.replaceAll("\\D", "");
        if (cnpj.length() != 14) throw new IllegalArgumentException("CNPJ deve ter 14 dígitos");
        int cnf = RND.nextInt(100_000_000); // 0 a 99.999.999 (8 dígitos)
        String cnfStr = String.format("%08d", cnf);
        String parte = ""
                + String.format("%02d", UF_BA)
                + aamm
                + cnpj
                + String.format("%02d", MOD_NFCE)
                + String.format("%03d", serie)
                + String.format("%09d", numeroNf)
                + TP_EMIS_NORMAL
                + cnfStr;
        if (parte.length() != 43) throw new IllegalStateException("Parte da chave deve ter 43 dígitos");
        int dv = modulo11(parte);
        return parte + dv;
    }

    /** Módulo 11: pesos 2-9 da direita para esquerda; resto 0 ou 1 -> cDV=0, senão cDV=11-resto. */
    public static int modulo11(String s) {
        int peso = 2;
        int soma = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            soma += (s.charAt(i) - '0') * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int r = soma % 11;
        return (r == 0 || r == 1) ? 0 : (11 - r);
    }
}
