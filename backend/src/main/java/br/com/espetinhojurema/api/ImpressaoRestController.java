package br.com.espetinhojurema.api;

import br.com.espetinhojurema.api.dto.ImpressaoConfigUpdateRequest;
import br.com.espetinhojurema.application.model.ImpressaoConfigView;
import br.com.espetinhojurema.application.model.ImpressaoFilasView;
import br.com.espetinhojurema.application.service.ImpressaoConfigOperacaoService;
import br.com.espetinhojurema.application.service.ImpressaoCupsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/impressao")
@PreAuthorize("hasRole('ATENDIMENTO')")
public class ImpressaoRestController {

    private final ImpressaoConfigOperacaoService impressaoConfigOperacaoService;
    private final ImpressaoCupsService impressaoCupsService;

    public ImpressaoRestController(
            ImpressaoConfigOperacaoService impressaoConfigOperacaoService,
            ImpressaoCupsService impressaoCupsService) {
        this.impressaoConfigOperacaoService = impressaoConfigOperacaoService;
        this.impressaoCupsService = impressaoCupsService;
    }

    @GetMapping("/config")
    public ImpressaoConfigView config() {
        return impressaoConfigOperacaoService.obterConfig();
    }

    @PatchMapping("/config")
    public ImpressaoConfigView atualizarConfig(@RequestBody ImpressaoConfigUpdateRequest body) {
        return impressaoConfigOperacaoService.atualizar(body.nomeImpressoraLp());
    }

    @GetMapping("/filas")
    public ImpressaoFilasView filas() {
        return new ImpressaoFilasView(impressaoCupsService.listarFilasImpressoras());
    }
}
