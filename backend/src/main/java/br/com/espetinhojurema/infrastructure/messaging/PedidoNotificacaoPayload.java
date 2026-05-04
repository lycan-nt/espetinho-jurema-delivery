package br.com.espetinhojurema.infrastructure.messaging;

import java.time.Instant;

public record PedidoNotificacaoPayload(String tipo, Long pedidoId, Instant quando) {
}
