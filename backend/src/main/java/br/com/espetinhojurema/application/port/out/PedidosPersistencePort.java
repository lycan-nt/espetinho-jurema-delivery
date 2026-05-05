package br.com.espetinhojurema.application.port.out;

import br.com.espetinhojurema.application.command.CriarPedidoCommand;
import br.com.espetinhojurema.application.model.MesaTransferenciaView;
import br.com.espetinhojurema.application.model.PedidoDetalheView;
import br.com.espetinhojurema.application.model.PedidoListaView;
import br.com.espetinhojurema.domain.model.FormaPagamento;
import br.com.espetinhojurema.domain.model.PontoCarne;
import br.com.espetinhojurema.domain.model.PedidoStatus;
import br.com.espetinhojurema.domain.model.PedidoTipo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PedidosPersistencePort {

    Optional<PedidoDetalheView> buscarDetalhe(Long id);

    List<PedidoListaView> filtrar(PedidoStatus status, PedidoTipo tipo);

    Optional<Long> obterIdPedidoAbertoNaMesa(Long mesaId);

    PedidoDetalheView criarPedido(CriarPedidoCommand command);

    PedidoDetalheView adicionarItem(
            Long pedidoId, Long produtoId, int quantidade, String observacao, PontoCarne pontoCarne);

    PedidoDetalheView cancelarItemPedido(
            Long pedidoId, Long itemPedidoId, String usuarioLogin, Integer quantidadeCancelar);

    PedidoDetalheView atualizarStatus(Long pedidoId, PedidoStatus novoStatus);

    PedidoDetalheView registrarPagamento(
            Long pedidoId, FormaPagamento forma, BigDecimal valor, BigDecimal valorRecebidoDinheiro);

    PedidoDetalheView transferirPedidoParaMesa(Long pedidoId, Long mesaDestinoId, String usuarioLogin);

    List<MesaTransferenciaView> listarTransferenciasMesa(Long pedidoId);
}
