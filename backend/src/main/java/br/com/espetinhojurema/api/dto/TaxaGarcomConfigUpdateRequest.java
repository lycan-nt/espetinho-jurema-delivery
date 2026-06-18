package br.com.espetinhojurema.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TaxaGarcomConfigUpdateRequest(
        @NotNull Boolean habilitada,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) @DecimalMax(value = "100.0", inclusive = true)
                BigDecimal percentual) {}
