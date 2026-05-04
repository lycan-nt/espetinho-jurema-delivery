package br.com.espetinhojurema.infrastructure.messaging;

import br.com.espetinhojurema.application.port.out.AtendimentoAlertaPublisherPort;
import java.time.Instant;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompAtendimentoAlertaPublisher implements AtendimentoAlertaPublisherPort {

    public static final String TOPIC_ALERTAS_ATENDIMENTO = "/topic/atendimento/alertas";

    private final SimpMessagingTemplate messagingTemplate;

    public StompAtendimentoAlertaPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void notificarAtendimento(String tipoEvento, Long pedidoId, Integer mesaNumero, String alertaId) {
        var payload = new AlertaAtendimentoPayload(tipoEvento, pedidoId, mesaNumero, alertaId, Instant.now());
        messagingTemplate.convertAndSend(TOPIC_ALERTAS_ATENDIMENTO, payload);
    }
}
