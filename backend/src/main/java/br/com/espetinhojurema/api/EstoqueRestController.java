package br.com.espetinhojurema.api;

import br.com.espetinhojurema.api.dto.AjusteEstoqueRequest;
import br.com.espetinhojurema.api.dto.EntradaEstoqueRequest;
import br.com.espetinhojurema.api.dto.EstoqueConfigUpdateRequest;
import br.com.espetinhojurema.application.model.EstoqueConfigView;
import br.com.espetinhojurema.application.service.EstoqueOperacaoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estoque")
public class EstoqueRestController {

    private final EstoqueOperacaoService estoqueOperacaoService;

    public EstoqueRestController(EstoqueOperacaoService estoqueOperacaoService) {
        this.estoqueOperacaoService = estoqueOperacaoService;
    }

    @GetMapping("/config")
    public EstoqueConfigView obterConfig() {
        return new EstoqueConfigView(estoqueOperacaoService.isEstoqueObrigatorio());
    }

    @PatchMapping("/config")
    @PreAuthorize("hasRole('ATENDIMENTO')")
    public EstoqueConfigView atualizarConfig(@Valid @RequestBody EstoqueConfigUpdateRequest body) {
        estoqueOperacaoService.definirEstoqueObrigatorio(body.estoqueObrigatorio());
        return new EstoqueConfigView(estoqueOperacaoService.isEstoqueObrigatorio());
    }

    @PostMapping("/entradas")
    @PreAuthorize("hasRole('ATENDIMENTO')")
    public void entrada(@Valid @RequestBody EntradaEstoqueRequest body) {
        estoqueOperacaoService.registrarEntrada(body.produtoId(), body.quantidade(), body.referencia());
    }

    @PostMapping("/ajustes")
    @PreAuthorize("hasRole('ATENDIMENTO')")
    public void ajuste(@Valid @RequestBody AjusteEstoqueRequest body) {
        estoqueOperacaoService.ajustarSaldo(body.produtoId(), body.novoSaldo());
    }
}
