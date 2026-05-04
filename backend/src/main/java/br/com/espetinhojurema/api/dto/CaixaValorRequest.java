package br.com.espetinhojurema.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CaixaValorRequest(@NotNull BigDecimal valor) {
}
