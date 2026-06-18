package br.com.espetinhojurema.application.model;

import java.math.BigDecimal;

/** Resultado do cálculo da taxa de serviço (garçom) para um pedido de mesa. */
public record TaxaGarcomCalculo(BigDecimal percentualAplicado, BigDecimal valorTotal) {

    public static TaxaGarcomCalculo naoAplicavel() {
        return new TaxaGarcomCalculo(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public boolean aplicavel() {
        return valorTotal != null && valorTotal.compareTo(BigDecimal.ZERO) > 0;
    }
}
