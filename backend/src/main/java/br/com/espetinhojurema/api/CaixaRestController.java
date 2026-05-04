package br.com.espetinhojurema.api;

import br.com.espetinhojurema.api.dto.CaixaValorRequest;
import br.com.espetinhojurema.application.model.CaixaStatusView;
import br.com.espetinhojurema.application.port.out.CaixaPersistencePort;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/caixa")
public class CaixaRestController {

    private final CaixaPersistencePort caixaPersistencePort;

    public CaixaRestController(CaixaPersistencePort caixaPersistencePort) {
        this.caixaPersistencePort = caixaPersistencePort;
    }

    @GetMapping("/status")
    public CaixaStatusView status() {
        return caixaPersistencePort.statusAtual();
    }

    @PostMapping("/abrir")
    public CaixaStatusView abrir(@Valid @RequestBody CaixaValorRequest request) {
        return caixaPersistencePort.abrirSessao(request.valor());
    }

    @PostMapping("/fechar")
    public CaixaStatusView fechar(@Valid @RequestBody CaixaValorRequest request) {
        return caixaPersistencePort.fecharSessao(request.valor());
    }
}
