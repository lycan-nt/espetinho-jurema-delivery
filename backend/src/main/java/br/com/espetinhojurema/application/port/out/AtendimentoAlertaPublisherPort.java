package br.com.espetinhojurema.application.port.out;

public interface AtendimentoAlertaPublisherPort {

    /** @param tipoEvento ex.: {@code COMANDA_ENVIADA} */
    void notificarAtendimento(String tipoEvento, Long pedidoId, Integer mesaNumero, String alertaId);
}
