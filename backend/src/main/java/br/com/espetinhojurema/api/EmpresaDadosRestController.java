package br.com.espetinhojurema.api;

import br.com.espetinhojurema.api.dto.EmpresaDadosUpdateRequest;
import br.com.espetinhojurema.application.model.EmpresaDadosPatch;
import br.com.espetinhojurema.application.model.EmpresaDadosView;
import br.com.espetinhojurema.application.service.EmpresaDadosOperacaoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/config/empresa")
@PreAuthorize("hasRole('ATENDIMENTO')")
public class EmpresaDadosRestController {

    private final EmpresaDadosOperacaoService empresaDadosOperacaoService;

    public EmpresaDadosRestController(EmpresaDadosOperacaoService empresaDadosOperacaoService) {
        this.empresaDadosOperacaoService = empresaDadosOperacaoService;
    }

    @GetMapping
    public EmpresaDadosView obter() {
        return empresaDadosOperacaoService.obter();
    }

    @PatchMapping
    public EmpresaDadosView atualizar(@RequestBody EmpresaDadosUpdateRequest body) {
        var patch = new EmpresaDadosPatch(
                body.cnpj(),
                body.nomeEmpresa(),
                body.endereco(),
                body.telefone(),
                body.email(),
                body.instagram(),
                body.comandaCabecalho());
        return empresaDadosOperacaoService.atualizar(patch);
    }
}
