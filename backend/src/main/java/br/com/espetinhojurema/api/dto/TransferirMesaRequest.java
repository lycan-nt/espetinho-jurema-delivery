package br.com.espetinhojurema.api.dto;

import jakarta.validation.constraints.NotNull;

public record TransferirMesaRequest(@NotNull Long mesaDestinoId) {}
