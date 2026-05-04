package br.com.espetinhojurema.api.dto;

import jakarta.validation.constraints.NotNull;

public record AjusteEstoqueRequest(@NotNull Long produtoId, @NotNull Integer novoSaldo) {}
