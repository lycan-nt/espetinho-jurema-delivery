package br.com.delivere.acai.produto;

import java.math.BigDecimal;

public final class ProdutoService {

    private ProdutoService() {
    }

    public static BigDecimal getPrecoKg(ConfiguracaoRepository repo, String chave) {
        return repo.findByChave(chave)
                .map(c -> {
                    try {
                        return new BigDecimal(c.getValor().trim().replace(",", "."));
                    } catch (Exception e) {
                        return BigDecimal.ZERO;
                    }
                })
                .orElse(BigDecimal.ZERO);
    }

    public static void setPrecoKg(ConfiguracaoRepository repo, String chave, BigDecimal valor) {
        String valorStr = valor != null ? valor.stripTrailingZeros().toPlainString() : "0";
        repo.findByChave(chave)
                .ifPresentOrElse(
                        c -> {
                            c.setValor(valorStr);
                            repo.save(c);
                        },
                        () -> repo.save(new Configuracao(chave, valorStr))
                );
    }
}
