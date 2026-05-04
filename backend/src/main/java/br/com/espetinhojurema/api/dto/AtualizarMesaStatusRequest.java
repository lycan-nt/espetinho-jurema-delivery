package br.com.espetinhojurema.api.dto;

import br.com.espetinhojurema.domain.model.MesaStatus;
import jakarta.validation.constraints.NotNull;

public record AtualizarMesaStatusRequest(@NotNull MesaStatus status) {
}
