package br.com.espetinhojurema.application.model;

import br.com.espetinhojurema.domain.model.PedidoStatus;
import br.com.espetinhojurema.domain.model.PedidoTipo;
import java.math.BigDecimal;
import java.time.Instant;

public record PedidoListaView(
        Long id,
        PedidoTipo tipo,
        PedidoStatus status,
        Long mesaId,
        Integer mesaNumero,
        String colaboradorNome,
        Instant criadoEm,
        BigDecimal subtotalItens,
        BigDecimal valorCouvertArtistico,
        BigDecimal valorTaxaGarcom,
        BigDecimal total
) {
}
