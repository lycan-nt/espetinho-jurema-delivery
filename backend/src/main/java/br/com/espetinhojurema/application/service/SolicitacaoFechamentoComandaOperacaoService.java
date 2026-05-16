package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.PedidoDetalheView;
import br.com.espetinhojurema.application.port.out.AlertasAtendimentoPersistencePort;
import br.com.espetinhojurema.application.port.out.AtendimentoAlertaPublisherPort;
import br.com.espetinhojurema.application.port.out.MesasPersistencePort;
import br.com.espetinhojurema.application.port.out.PedidoEventPublisherPort;
import br.com.espetinhojurema.application.port.out.PedidosPersistencePort;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.domain.model.MesaStatus;
import br.com.espetinhojurema.domain.model.PedidoStatus;
import br.com.espetinhojurema.domain.model.PedidoTipo;
import br.com.espetinhojurema.domain.model.TipoAlertaAtendimento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Churrasqueiro notifica o balcão que a mesa quer fechar a conta. Não encerra o pedido. No PC, ao dar OK no
 * alerta, imprime-se a mesma comanda de cozinha gerada pelo fluxo habitual (`ComandaCozinhaTextoService`).
 */
@Service
public class SolicitacaoFechamentoComandaOperacaoService {

    private final PedidosPersistencePort pedidosPersistencePort;
    private final MesasPersistencePort mesasPersistencePort;
    private final AlertasAtendimentoPersistencePort alertasAtendimentoPersistencePort;
    private final AtendimentoAlertaPublisherPort atendimentoAlertaPublisherPort;
    private final PedidoEventPublisherPort pedidoEventPublisherPort;

    public SolicitacaoFechamentoComandaOperacaoService(
            PedidosPersistencePort pedidosPersistencePort,
            MesasPersistencePort mesasPersistencePort,
            AlertasAtendimentoPersistencePort alertasAtendimentoPersistencePort,
            AtendimentoAlertaPublisherPort atendimentoAlertaPublisherPort,
            PedidoEventPublisherPort pedidoEventPublisherPort) {
        this.pedidosPersistencePort = pedidosPersistencePort;
        this.mesasPersistencePort = mesasPersistencePort;
        this.alertasAtendimentoPersistencePort = alertasAtendimentoPersistencePort;
        this.atendimentoAlertaPublisherPort = atendimentoAlertaPublisherPort;
        this.pedidoEventPublisherPort = pedidoEventPublisherPort;
    }

    @Transactional
    public void solicitarParaMesa(Long mesaId) {
        Long pedidoId = pedidosPersistencePort
                .obterIdPedidoAbertoNaMesa(mesaId)
                .orElseThrow(() -> new BusinessException("Esta mesa não possui pedido em aberto"));
        PedidoDetalheView pedido =
                pedidosPersistencePort.buscarDetalhe(pedidoId).orElseThrow(() -> new BusinessException("Pedido não encontrado"));

        if (pedido.tipo() != PedidoTipo.MESA) {
            throw new BusinessException("Apenas pedidos do tipo mesa podem usar esta solicitação");
        }
        if (pedido.status() == PedidoStatus.PAGO || pedido.status() == PedidoStatus.CANCELADO) {
            throw new BusinessException("Pedido já encerrado");
        }
        Integer mesaNumero = pedido.mesaNumero();
        if (mesaNumero == null) {
            throw new BusinessException("Pedido sem número de mesa");
        }

        var tipo = TipoAlertaAtendimento.SOLICITACAO_FECHAMENTO_COMANDA;
        String alertaId = alertasAtendimentoPersistencePort
                .encontrarAlertaIdPendente(pedidoId, tipo)
                .orElseGet(() -> alertasAtendimentoPersistencePort.criarPendente(pedidoId, mesaNumero, tipo, null));

        mesasPersistencePort.atualizarStatus(mesaId, MesaStatus.ENCERRANDO_SERVICO);
        pedidoEventPublisherPort.notificarMudancaPedido(pedidoId);

        atendimentoAlertaPublisherPort.notificarAtendimento(
                tipo.name(), pedidoId, mesaNumero, alertaId);
    }
}
