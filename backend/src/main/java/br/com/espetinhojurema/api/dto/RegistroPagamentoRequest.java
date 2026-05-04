package br.com.espetinhojurema.api.dto;

import br.com.espetinhojurema.domain.model.FormaPagamento;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RegistroPagamentoRequest(
        @NotNull FormaPagamento forma,
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal valor,
        /** Preencher em pagamentos em dinheiro quando o cliente entrega valor maior (troco). */
        BigDecimal valorRecebidoDinheiro
) {
}
