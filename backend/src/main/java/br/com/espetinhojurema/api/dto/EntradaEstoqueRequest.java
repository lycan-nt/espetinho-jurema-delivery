package br.com.espetinhojurema.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EntradaEstoqueRequest(
        @NotNull Long produtoId,
        @NotNull @Min(1) Integer quantidade,
        String referencia) {}
