package br.com.espetinhojurema.api;

import br.com.espetinhojurema.api.dto.CouvertArtisticoConfigUpdateRequest;
import br.com.espetinhojurema.application.model.CouvertArtisticoConfigView;
import br.com.espetinhojurema.application.service.CouvertArtisticoOperacaoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/couvert-artistico")
public class CouvertArtisticoRestController {

    private final CouvertArtisticoOperacaoService couvertArtisticoOperacaoService;

    public CouvertArtisticoRestController(CouvertArtisticoOperacaoService couvertArtisticoOperacaoService) {
        this.couvertArtisticoOperacaoService = couvertArtisticoOperacaoService;
    }

    @GetMapping("/config")
    public CouvertArtisticoConfigView obterConfig() {
        return couvertArtisticoOperacaoService.obterConfig();
    }

    @PatchMapping("/config")
    @PreAuthorize("hasRole('ATENDIMENTO')")
    public CouvertArtisticoConfigView atualizarConfig(@Valid @RequestBody CouvertArtisticoConfigUpdateRequest body) {
        return couvertArtisticoOperacaoService.atualizarConfig(body.ativo(), body.valorPorPessoa());
    }
}
