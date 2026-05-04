package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.command.CriarPedidoCommand;
import br.com.espetinhojurema.application.model.MesaComOcupacaoView;
import br.com.espetinhojurema.application.model.PedidoDetalheView;
import br.com.espetinhojurema.application.port.out.MesasPersistencePort;
import br.com.espetinhojurema.application.port.out.PedidosPersistencePort;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.domain.model.MesaStatus;
import br.com.espetinhojurema.domain.model.PedidoTipo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MesaOperacoesService {

    private final MesasPersistencePort mesasPersistencePort;
    private final PedidosPersistencePort pedidosPersistencePort;

    public MesaOperacoesService(MesasPersistencePort mesasPersistencePort, PedidosPersistencePort pedidosPersistencePort) {
        this.mesasPersistencePort = mesasPersistencePort;
        this.pedidosPersistencePort = pedidosPersistencePort;
    }

    @Transactional(readOnly = true)
    public List<MesaComOcupacaoView> listarMesas() {
        return mesasPersistencePort.listarTodasOrdenadas().stream()
                .map(m -> {
                    var pedidoAberto = pedidosPersistencePort.obterIdPedidoAbertoNaMesa(m.id());
                    boolean ocupada = pedidoAberto.isPresent()
                            || m.status() == MesaStatus.OCUPADA
                            || m.status() == MesaStatus.ENCERRANDO_SERVICO;
                    return new MesaComOcupacaoView(
                            m.id(),
                            m.numero(),
                            m.status(),
                            pedidoAberto.orElse(null),
                            ocupada);
                })
                .toList();
    }

    @Transactional
    public PedidoDetalheView abrirMesa(
            Long mesaId,
            Long colaboradorId,
            Long clienteId,
            String clienteNome,
            String descricao,
            Integer pessoas,
            boolean documentoFiscal) {
        var mesa = mesasPersistencePort.buscarPorId(mesaId).orElseThrow(() -> new BusinessException("Mesa não encontrada"));
        if (mesa.status() == MesaStatus.OCUPADA
                && pedidosPersistencePort.obterIdPedidoAbertoNaMesa(mesaId).isEmpty()) {
            mesasPersistencePort.atualizarStatus(mesaId, MesaStatus.LIVRE);
            mesa = mesasPersistencePort.buscarPorId(mesaId).orElseThrow(() -> new BusinessException("Mesa não encontrada"));
        }
        if (mesa.status() != MesaStatus.LIVRE) {
            throw new BusinessException("Somente mesas livres podem ser abertas por esta ação");
        }
        if (pedidosPersistencePort.obterIdPedidoAbertoNaMesa(mesaId).isPresent()) {
            throw new BusinessException("Mesa já possui pedido em andamento");
        }
        var command = new CriarPedidoCommand(
                PedidoTipo.MESA,
                mesaId,
                colaboradorId,
                clienteId,
                clienteNome,
                descricao,
                pessoas,
                documentoFiscal);
        return pedidosPersistencePort.criarPedido(command);
    }

    @Transactional
    public void atualizarStatusMesa(Long mesaId, MesaStatus novoStatus) {
        mesasPersistencePort.buscarPorId(mesaId).orElseThrow(() -> new BusinessException("Mesa não encontrada"));
        mesasPersistencePort.atualizarStatus(mesaId, novoStatus);
    }
}
