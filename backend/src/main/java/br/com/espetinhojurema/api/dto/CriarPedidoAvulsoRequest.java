package br.com.espetinhojurema.api.dto;

import br.com.espetinhojurema.domain.model.PedidoTipo;
import jakarta.validation.constraints.NotNull;

public record CriarPedidoAvulsoRequest(
        @NotNull PedidoTipo tipo,
        @NotNull Long colaboradorId,
        Long clienteId,
        String descricao,
        Integer pessoas,
        boolean documentoFiscal
) {
}
