package br.com.espetinhojurema.application.model;

import br.com.espetinhojurema.domain.model.FormaPagamento;
import java.math.BigDecimal;

public record PagamentoPedidoView(
        Long id,
        FormaPagamento forma,
        BigDecimal valor,
        BigDecimal valorRecebidoDinheiro,
        /** Troco quando recebido em espécie é maior que o valor aplicado. */
        BigDecimal troco
) {
}
