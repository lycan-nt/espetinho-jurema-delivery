package br.com.espetinhojurema.api;

import br.com.espetinhojurema.api.dto.AbrirMesaRequest;
import br.com.espetinhojurema.api.dto.AtualizarMesaStatusRequest;
import br.com.espetinhojurema.application.model.MesaComOcupacaoView;
import br.com.espetinhojurema.application.model.PedidoDetalheView;
import br.com.espetinhojurema.application.service.MesaOperacoesService;
import br.com.espetinhojurema.application.service.SolicitacaoFechamentoComandaOperacaoService;
import br.com.espetinhojurema.domain.model.MesaStatus;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mesas")
public class MesaRestController {

    private final MesaOperacoesService mesaOperacoesService;
    private final SolicitacaoFechamentoComandaOperacaoService solicitacaoFechamentoComandaOperacaoService;

    public MesaRestController(
            MesaOperacoesService mesaOperacoesService,
            SolicitacaoFechamentoComandaOperacaoService solicitacaoFechamentoComandaOperacaoService) {
        this.mesaOperacoesService = mesaOperacoesService;
        this.solicitacaoFechamentoComandaOperacaoService = solicitacaoFechamentoComandaOperacaoService;
    }

    @GetMapping
    public List<MesaComOcupacaoView> listar() {
        return mesaOperacoesService.listarMesas();
    }

    @GetMapping("/resumo")
    public Map<String, Object> resumo() {
        List<MesaComOcupacaoView> mesas = mesaOperacoesService.listarMesas();
        long total = mesas.size();
        long ocupadas = mesas.stream().filter(MesaComOcupacaoView::ocupada).count();
        long encerrando = mesas.stream().filter(m -> m.status() == MesaStatus.ENCERRANDO_SERVICO).count();
        Map<String, Object> map = new HashMap<>();
        map.put("total", total);
        map.put("ocupadas", ocupadas);
        map.put("livres", Math.max(0, total - ocupadas));
        map.put("encerrandoServico", encerrando);
        return map;
    }

    @PostMapping("/{id}/abrir")
    public PedidoDetalheView abrir(@PathVariable Long id, @Valid @RequestBody AbrirMesaRequest request) {
        return mesaOperacoesService.abrirMesa(
                id,
                request.colaboradorId(),
                request.clienteId(),
                request.clienteNome(),
                request.descricao(),
                request.pessoas(),
                request.documentoFiscal());
    }

    /**
     * Churrasqueiro: notifica atendimento (balcão) para fechar a comanda na mesa. Não encerra o pedido.
     * No balcão, OK no alerta gera/imprime comanda igual ao fluxo de comanda enviada.
     */
    @PostMapping("/{id}/solicitar-fechamento-comanda")
    @PreAuthorize("hasRole('CHURRASQUEIRO')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void solicitarFechamentoComanda(@PathVariable Long id) {
        solicitacaoFechamentoComandaOperacaoService.solicitarParaMesa(id);
    }

    @PatchMapping("/{id}/status")
    public void status(@PathVariable Long id, @Valid @RequestBody AtualizarMesaStatusRequest request) {
        mesaOperacoesService.atualizarStatusMesa(id, request.status());
    }
}
