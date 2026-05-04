package br.com.espetinhojurema.application.model;

import br.com.espetinhojurema.domain.model.PedidoStatus;
import br.com.espetinhojurema.domain.model.PedidoTipo;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PedidoDetalheView(
        Long id,
        PedidoTipo tipo,
        PedidoStatus status,
        Long mesaId,
        Integer mesaNumero,
        Long clienteId,
        String clienteNome,
        Long colaboradorId,
        String colaboradorNome,
        String descricaoMesa,
        Integer pessoas,
        boolean documentoFiscal,
        Instant criadoEm,
        List<ItemPedidoView> itens,
        BigDecimal total,
        List<PagamentoPedidoView> pagamentos,
        BigDecimal totalPago,
        BigDecimal restante
) {
}
