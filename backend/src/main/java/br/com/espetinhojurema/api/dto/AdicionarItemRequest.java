package br.com.espetinhojurema.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdicionarItemRequest(
        @NotNull Long produtoId,
        @NotNull @Min(1) Integer quantidade,
        String observacao
) {
}
