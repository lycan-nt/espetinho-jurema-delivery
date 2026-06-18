package br.com.espetinhojurema.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CouvertArtisticoConfigUpdateRequest(
        @NotNull Boolean ativo,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal valorPorPessoa) {}
