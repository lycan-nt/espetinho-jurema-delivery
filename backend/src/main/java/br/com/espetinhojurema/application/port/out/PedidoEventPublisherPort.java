package br.com.espetinhojurema.application.port.out;

public interface PedidoEventPublisherPort {

    void notificarMudancaPedido(Long pedidoId);
}
