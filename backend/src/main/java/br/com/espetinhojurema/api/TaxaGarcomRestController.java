package br.com.espetinhojurema.api;

import br.com.espetinhojurema.api.dto.TaxaGarcomConfigUpdateRequest;
import br.com.espetinhojurema.application.model.TaxaGarcomConfigView;
import br.com.espetinhojurema.application.service.TaxaGarcomOperacaoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/taxa-garcom")
public class TaxaGarcomRestController {

    private final TaxaGarcomOperacaoService taxaGarcomOperacaoService;

    public TaxaGarcomRestController(TaxaGarcomOperacaoService taxaGarcomOperacaoService) {
        this.taxaGarcomOperacaoService = taxaGarcomOperacaoService;
    }

    @GetMapping("/config")
    public TaxaGarcomConfigView obterConfig() {
        return taxaGarcomOperacaoService.obterConfig();
    }

    @PatchMapping("/config")
    @PreAuthorize("hasRole('ATENDIMENTO')")
    public TaxaGarcomConfigView atualizarConfig(@Valid @RequestBody TaxaGarcomConfigUpdateRequest body) {
        return taxaGarcomOperacaoService.atualizarConfig(body.habilitada(), body.percentual());
    }
}
