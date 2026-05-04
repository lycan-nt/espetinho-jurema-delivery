package br.com.espetinhojurema.infrastructure.messaging;

import br.com.espetinhojurema.application.port.out.PedidoEventPublisherPort;
import java.time.Instant;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompPedidoEventPublisher implements PedidoEventPublisherPort {

    public static final String TOPIC_PEDIDOS = "/topic/pedidos";

    private final SimpMessagingTemplate messagingTemplate;

    public StompPedidoEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void notificarMudancaPedido(Long pedidoId) {
        var payload = new PedidoNotificacaoPayload("PEDIDO_ATUALIZADO", pedidoId, Instant.now());
        messagingTemplate.convertAndSend(TOPIC_PEDIDOS, payload);
    }
}
