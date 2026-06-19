package br.com.espetinhojurema.api.dto;

import br.com.espetinhojurema.domain.model.CouvertArtisticoModo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CouvertArtisticoConfigUpdateRequest(
        @NotNull Boolean ativo,
        @NotNull CouvertArtisticoModo modo,
        @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal valorPorPessoa) {}
