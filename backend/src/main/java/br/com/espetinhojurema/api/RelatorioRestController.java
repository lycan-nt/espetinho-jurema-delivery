package br.com.espetinhojurema.api;

import br.com.espetinhojurema.application.model.FaturamentoResumoView;
import br.com.espetinhojurema.application.service.FaturamentoRelatorioService;
import br.com.espetinhojurema.domain.exception.BusinessException;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/relatorios")
public class RelatorioRestController {

    private final FaturamentoRelatorioService faturamentoRelatorioService;

    public RelatorioRestController(FaturamentoRelatorioService faturamentoRelatorioService) {
        this.faturamentoRelatorioService = faturamentoRelatorioService;
    }

    @GetMapping("/faturamento")
    @PreAuthorize("hasRole('ATENDIMENTO')")
    public FaturamentoResumoView faturamento(
            @RequestParam("inicio") String inicioIso, @RequestParam("fim") String fimIso) {
        Instant inicio;
        Instant fim;
        try {
            inicio = Instant.parse(inicioIso);
            fim = Instant.parse(fimIso);
        } catch (Exception e) {
            throw new BusinessException("Parâmetros inicio/fim devem ser instantes ISO-8601 (ex.: 2026-03-01T03:00:00Z).");
        }
        return faturamentoRelatorioService.resumo(inicio, fim);
    }
}
