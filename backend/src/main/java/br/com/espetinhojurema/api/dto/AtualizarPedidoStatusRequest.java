package br.com.espetinhojurema.api.dto;

import br.com.espetinhojurema.domain.model.PedidoStatus;
import jakarta.validation.constraints.NotNull;

public record AtualizarPedidoStatusRequest(@NotNull PedidoStatus status) {
}
