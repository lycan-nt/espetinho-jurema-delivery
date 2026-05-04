package br.com.espetinhojurema.api;

import br.com.espetinhojurema.application.model.ColaboradorView;
import br.com.espetinhojurema.application.port.out.ColaboradorPersistencePort;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/colaboradores")
public class ColaboradorRestController {

    private final ColaboradorPersistencePort colaboradorPersistencePort;

    public ColaboradorRestController(ColaboradorPersistencePort colaboradorPersistencePort) {
        this.colaboradorPersistencePort = colaboradorPersistencePort;
    }

    @GetMapping
    public List<ColaboradorView> listar() {
        return colaboradorPersistencePort.listarAtivos();
    }
}
