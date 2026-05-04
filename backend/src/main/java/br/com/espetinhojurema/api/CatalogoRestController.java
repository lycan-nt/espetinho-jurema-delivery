package br.com.espetinhojurema.api;

import br.com.espetinhojurema.application.model.CategoriaView;
import br.com.espetinhojurema.application.model.ProdutoView;
import br.com.espetinhojurema.application.port.out.CatalogoPersistencePort;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CatalogoRestController {

    private final CatalogoPersistencePort catalogoPersistencePort;

    public CatalogoRestController(CatalogoPersistencePort catalogoPersistencePort) {
        this.catalogoPersistencePort = catalogoPersistencePort;
    }

    @GetMapping("/categorias")
    public List<CategoriaView> categorias() {
        return catalogoPersistencePort.listarCategorias();
    }

    @GetMapping("/produtos")
    public List<ProdutoView> produtos() {
        return catalogoPersistencePort.listarProdutosAtivos();
    }
}
