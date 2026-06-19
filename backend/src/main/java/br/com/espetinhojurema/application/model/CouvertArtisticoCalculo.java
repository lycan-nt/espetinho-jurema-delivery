package br.com.espetinhojurema.application.model;

import br.com.espetinhojurema.domain.model.CouvertArtisticoModo;
import java.math.BigDecimal;

/** Resultado do cálculo de couvert artístico para um pedido de mesa. */
public record CouvertArtisticoCalculo(
        CouvertArtisticoModo modo,
        BigDecimal valorUnitario,
        int pessoasCobradas,
        BigDecimal valorTotal) {

    public static CouvertArtisticoCalculo naoAplicavel() {
        return new CouvertArtisticoCalculo(null, BigDecimal.ZERO, 0, BigDecimal.ZERO);
    }

    public boolean aplicavel() {
        return valorTotal != null && valorTotal.compareTo(BigDecimal.ZERO) > 0;
    }
}
