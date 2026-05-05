package br.com.espetinhojurema.application.model;

import br.com.espetinhojurema.domain.model.PontoCarne;
import java.math.BigDecimal;
import java.time.Instant;

public record ItemPedidoView(
        Long id,
        Long produtoId,
        String produtoNome,
        int quantidade,
        BigDecimal precoUnitario,
        String observacao,
        PontoCarne pontoCarne,
        boolean cancelado,
        Instant canceladoEm,
        String canceladoPorLogin) {}
