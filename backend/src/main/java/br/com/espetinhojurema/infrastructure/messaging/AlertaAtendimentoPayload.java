package br.com.espetinhojurema.infrastructure.messaging;

import java.time.Instant;

public record AlertaAtendimentoPayload(
        String tipo,
        Long pedidoId,
        Integer mesaNumero,
        String alertaId,
        Instant quando
) {
}
