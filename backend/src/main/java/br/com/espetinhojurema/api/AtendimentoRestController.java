package br.com.espetinhojurema.api;

import br.com.espetinhojurema.application.model.ReconhecerAlertaResult;
import br.com.espetinhojurema.application.service.AtendimentoAlertaOperacaoService;
import br.com.espetinhojurema.infrastructure.security.UsuarioPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/atendimento")
@PreAuthorize("hasRole('ATENDIMENTO')")
public class AtendimentoRestController {

    private final AtendimentoAlertaOperacaoService atendimentoAlertaOperacaoService;

    public AtendimentoRestController(AtendimentoAlertaOperacaoService atendimentoAlertaOperacaoService) {
        this.atendimentoAlertaOperacaoService = atendimentoAlertaOperacaoService;
    }

    @PostMapping("/alertas/{alertaId}/ok")
    public ReconhecerAlertaResult reconhecer(
            @PathVariable String alertaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
        return atendimentoAlertaOperacaoService.reconhecerAlerta(alertaId, principal.getUsername());
    }
}
