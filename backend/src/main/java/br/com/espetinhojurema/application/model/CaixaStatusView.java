package br.com.espetinhojurema.application.model;

import java.math.BigDecimal;
import java.time.Instant;

public record CaixaStatusView(
        boolean aberto,
        Instant abertoEm,
        Instant fechadoEm,
        BigDecimal saldoAbertura,
        BigDecimal saldoFechamento,
        Long sessaoId
) {
}
