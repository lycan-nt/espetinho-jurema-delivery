package br.com.delivere.acai.comanda;

import br.com.delivere.acai.caixa.CaixaService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class DashboardController {

    private final ComandaService comandaService;
    private final CaixaService caixaService;

    public DashboardController(ComandaService comandaService, CaixaService caixaService) {
        this.comandaService = comandaService;
        this.caixaService = caixaService;
    }

    @GetMapping("/dashboard")
    public DashboardDTO dashboard() {
        DashboardDTO dto = comandaService.dashboard();
        dto.setCaixaAberto(caixaService.getCaixaAbertoHoje().isPresent());
        return dto;
    }
}
