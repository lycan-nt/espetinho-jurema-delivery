package br.com.espetinhojurema.api.dto;

import br.com.espetinhojurema.domain.model.PontoCarne;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdicionarItemRequest(
        @NotNull Long produtoId,
        @NotNull @Min(1) Integer quantidade,
        String observacao,
        PontoCarne pontoCarne
) {}
