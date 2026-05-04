package br.com.espetinhojurema.application.command;

import br.com.espetinhojurema.domain.model.PedidoTipo;

public record CriarPedidoCommand(
        PedidoTipo tipo,
        Long mesaId,
        Long colaboradorId,
        Long clienteId,
        /** Se {@code clienteId} for nulo e o nome não for vazio, cria um cliente na hora. */
        String clienteNome,
        String descricaoMesa,
        Integer pessoas,
        boolean documentoFiscal
) {
}
