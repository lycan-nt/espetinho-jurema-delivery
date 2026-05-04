package br.com.espetinhojurema.application.port.out;

import br.com.espetinhojurema.application.model.CategoriaView;
import br.com.espetinhojurema.application.model.ProdutoView;
import java.util.List;

public interface CatalogoPersistencePort {

    List<CategoriaView> listarCategorias();

    List<ProdutoView> listarProdutosAtivos();
}
