package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.PedidoDetalheView;
import br.com.espetinhojurema.application.port.out.AlertasAtendimentoPersistencePort;
import br.com.espetinhojurema.application.port.out.AtendimentoAlertaPublisherPort;
import br.com.espetinhojurema.application.port.out.PedidosPersistencePort;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.domain.model.PedidoStatus;
import br.com.espetinhojurema.domain.model.PedidoTipo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvioComandaAtendimentoService {

    public static final String TIPO_COMANDA_ENVIADA = "COMANDA_ENVIADA";

    private final PedidosPersistencePort pedidosPersistencePort;
    private final AlertasAtendimentoPersistencePort alertasAtendimentoPersistencePort;
    private final AtendimentoAlertaPublisherPort atendimentoAlertaPublisherPort;

    public EnvioComandaAtendimentoService(
            PedidosPersistencePort pedidosPersistencePort,
            AlertasAtendimentoPersistencePort alertasAtendimentoPersistencePort,
            AtendimentoAlertaPublisherPort atendimentoAlertaPublisherPort) {
        this.pedidosPersistencePort = pedidosPersistencePort;
        this.alertasAtendimentoPersistencePort = alertasAtendimentoPersistencePort;
        this.atendimentoAlertaPublisherPort = atendimentoAlertaPublisherPort;
    }

    /**
     * Após o garçom/churrasqueiro lançar itens: notifica o atendimento (PC) para imprimir comanda na cozinha.
     */
    @Transactional
    public PedidoDetalheView enviarComanda(Long pedidoId) {
        PedidoDetalheView pedido = pedidosPersistencePort
                .buscarDetalhe(pedidoId)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado"));
        if (pedido.tipo() != PedidoTipo.MESA) {
            throw new BusinessException("Envio de comanda só se aplica a pedidos de mesa");
        }
        if (pedido.status() == PedidoStatus.PAGO || pedido.status() == PedidoStatus.CANCELADO) {
            throw new BusinessException("Pedido encerrado");
        }
        boolean temItemAtivo = pedido.itens().stream().anyMatch(i -> !i.cancelado());
        if (!temItemAtivo) {
            throw new BusinessException("Adicione ao menos um item antes de enviar a comanda");
        }
        if (pedido.mesaNumero() == null) {
            throw new BusinessException("Pedido sem mesa associada");
        }
        String alertaId = alertasAtendimentoPersistencePort.criarPendente(pedidoId, pedido.mesaNumero());
        atendimentoAlertaPublisherPort.notificarAtendimento(
                TIPO_COMANDA_ENVIADA, pedidoId, pedido.mesaNumero(), alertaId);
        return pedidosPersistencePort
                .buscarDetalhe(pedidoId)
                .orElseThrow();
    }
}
