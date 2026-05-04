package br.com.espetinhojurema.api;

import br.com.espetinhojurema.api.dto.CriarClienteRequest;
import br.com.espetinhojurema.application.model.ClienteView;
import br.com.espetinhojurema.application.port.out.ClientePersistencePort;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteRestController {

    private final ClientePersistencePort clientePersistencePort;

    public ClienteRestController(ClientePersistencePort clientePersistencePort) {
        this.clientePersistencePort = clientePersistencePort;
    }

    @GetMapping
    public List<ClienteView> listar() {
        return clientePersistencePort.listar();
    }

    @PostMapping
    public ClienteView criar(@Valid @RequestBody CriarClienteRequest request) {
        return clientePersistencePort.criar(request.nome(), request.telefone(), request.endereco());
    }
}
