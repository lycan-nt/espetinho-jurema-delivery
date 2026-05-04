package br.com.espetinhojurema.api.dto;

import jakarta.validation.constraints.NotNull;

public record AbrirMesaRequest(
        @NotNull Long colaboradorId,
        Long clienteId,
        /** Nome livre (opcional). Usado se {@code clienteId} for nulo. */
        String clienteNome,
        String descricao,
        Integer pessoas,
        boolean documentoFiscal
) {
}
