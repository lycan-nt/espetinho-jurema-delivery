package br.com.espetinhojurema.api;

import br.com.espetinhojurema.api.dto.AdicionarItemRequest;
import br.com.espetinhojurema.api.dto.AtualizarPedidoStatusRequest;
import br.com.espetinhojurema.api.dto.CriarPedidoAvulsoRequest;
import br.com.espetinhojurema.api.dto.RegistroPagamentoRequest;
import br.com.espetinhojurema.api.dto.TransferirMesaRequest;
import br.com.espetinhojurema.application.command.CriarPedidoCommand;
import br.com.espetinhojurema.application.model.MesaTransferenciaView;
import br.com.espetinhojurema.application.model.PedidoDetalheView;
import br.com.espetinhojurema.application.model.PedidoListaView;
import br.com.espetinhojurema.application.port.out.PedidosPersistencePort;
import br.com.espetinhojurema.application.model.ImprimirLocalResult;
import br.com.espetinhojurema.application.service.ComprovanteTextoService;
import br.com.espetinhojurema.application.service.EnvioComandaAtendimentoService;
import br.com.espetinhojurema.application.service.PedidoImpressaoLocalService;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.domain.model.PedidoTipo;
import br.com.espetinhojurema.infrastructure.security.UsuarioPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoRestController {

    private final PedidosPersistencePort pedidosPersistencePort;
    private final ComprovanteTextoService comprovanteTextoService;
    private final EnvioComandaAtendimentoService envioComandaAtendimentoService;
    private final PedidoImpressaoLocalService pedidoImpressaoLocalService;

    public PedidoRestController(
            PedidosPersistencePort pedidosPersistencePort,
            ComprovanteTextoService comprovanteTextoService,
            EnvioComandaAtendimentoService envioComandaAtendimentoService,
            PedidoImpressaoLocalService pedidoImpressaoLocalService) {
        this.pedidosPersistencePort = pedidosPersistencePort;
        this.comprovanteTextoService = comprovanteTextoService;
        this.envioComandaAtendimentoService = envioComandaAtendimentoService;
        this.pedidoImpressaoLocalService = pedidoImpressaoLocalService;
    }

    @GetMapping
    public List<PedidoListaView> listar(
            @RequestParam(required = false) br.com.espetinhojurema.domain.model.PedidoStatus status,
            @RequestParam(required = false) PedidoTipo tipo) {
        return pedidosPersistencePort.filtrar(status, tipo);
    }

    @GetMapping("/{id}")
    public PedidoDetalheView porId(@PathVariable Long id) {
        return pedidosPersistencePort.buscarDetalhe(id)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado"));
    }

    @PostMapping("/avulsos")
    public PedidoDetalheView criarAvulso(@Valid @RequestBody CriarPedidoAvulsoRequest request) {
        if (request.tipo() == PedidoTipo.MESA) {
            throw new BusinessException("Use a rota de abertura de mesa para pedidos do tipo MESA");
        }
        var command = new CriarPedidoCommand(
                request.tipo(),
                null,
                request.colaboradorId(),
                request.clienteId(),
                null,
                request.descricao(),
                request.pessoas(),
                request.documentoFiscal());
        return pedidosPersistencePort.criarPedido(command);
    }

    @PostMapping("/{id}/itens")
    public PedidoDetalheView adicionarItem(
            @PathVariable Long id,
            @Valid @RequestBody AdicionarItemRequest request) {
        return pedidosPersistencePort.adicionarItem(
                id,
                request.produtoId(),
                request.quantidade(),
                request.observacao());
    }

    @PostMapping("/{id}/itens/{itemId}/cancelar")
    public PedidoDetalheView cancelarItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        String login = principal != null ? principal.getUsername() : null;
        return pedidosPersistencePort.cancelarItemPedido(id, itemId, login);
    }

    @PostMapping("/{id}/pagamentos")
    @PreAuthorize("hasRole('ATENDIMENTO')")
    public PedidoDetalheView registrarPagamento(
            @PathVariable Long id, @Valid @RequestBody RegistroPagamentoRequest request) {
        return pedidosPersistencePort.registrarPagamento(
                id, request.forma(), request.valor(), request.valorRecebidoDinheiro());
    }

    @PostMapping("/{id}/comanda/enviar")
    @PreAuthorize("hasAnyRole('GARCOM', 'CHURRASQUEIRO')")
    public PedidoDetalheView enviarComanda(@PathVariable Long id) {
        return envioComandaAtendimentoService.enviarComanda(id);
    }

    @PatchMapping("/{id}/status")
    public PedidoDetalheView atualizarStatus(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarPedidoStatusRequest request) {
        return pedidosPersistencePort.atualizarStatus(id, request.status());
    }

    @PostMapping("/{id}/mesa/transferir")
    public PedidoDetalheView transferirMesa(
            @PathVariable Long id,
            @Valid @RequestBody TransferirMesaRequest request,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        String login = principal != null ? principal.getUsername() : null;
        return pedidosPersistencePort.transferirPedidoParaMesa(id, request.mesaDestinoId(), login);
    }

    @GetMapping("/{id}/mesa/transferencias")
    public List<MesaTransferenciaView> listarTransferenciasMesa(@PathVariable Long id) {
        return pedidosPersistencePort.listarTransferenciasMesa(id);
    }

    @GetMapping(value = "/{id}/comprovante", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
    public String comprovante(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean fiscal,
            @RequestParam(defaultValue = "false") boolean fechamento) {
        var pedido = pedidosPersistencePort.buscarDetalhe(id)
                .orElseThrow(() -> new BusinessException("Pedido não encontrado"));
        if (fechamento) {
            return comprovanteTextoService.gerarFechamento(pedido, fiscal);
        }
        return comprovanteTextoService.gerar(pedido, fiscal);
    }

    /** Envia texto do cupom/fechamento para a fila CUPS configurada no servidor (Mac/Linux), quando houver. */
    @PostMapping("/{id}/imprimir-local")
    @PreAuthorize("hasRole('ATENDIMENTO')")
    public ImprimirLocalResult imprimirLocal(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean fiscal,
            @RequestParam(defaultValue = "false") boolean fechamento) {
        return pedidoImpressaoLocalService.imprimirComprovanteLocal(id, fiscal, fechamento);
    }
}
