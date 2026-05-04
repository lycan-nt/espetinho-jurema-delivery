package br.com.espetinhojurema.infrastructure.persistence.adapter;

import br.com.espetinhojurema.application.model.CategoriaView;
import br.com.espetinhojurema.application.model.ProdutoView;
import br.com.espetinhojurema.application.port.out.CatalogoPersistencePort;
import br.com.espetinhojurema.infrastructure.persistence.entity.ProdutoEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.CategoriaJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.ProdutoJpaRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CatalogoPersistenceAdapter implements CatalogoPersistencePort {

    private final CategoriaJpaRepository categoriaJpaRepository;
    private final ProdutoJpaRepository produtoJpaRepository;

    public CatalogoPersistenceAdapter(
            CategoriaJpaRepository categoriaJpaRepository,
            ProdutoJpaRepository produtoJpaRepository) {
        this.categoriaJpaRepository = categoriaJpaRepository;
        this.produtoJpaRepository = produtoJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaView> listarCategorias() {
        return categoriaJpaRepository.findAll(Sort.by("ordem", "nome")).stream()
                .map(c -> new CategoriaView(c.getId(), c.getNome(), c.getOrdem()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProdutoView> listarProdutosAtivos() {
        return produtoJpaRepository.findAllAtivosComCategoria().stream()
                .map(this::mapearProduto)
                .toList();
    }

    private ProdutoView mapearProduto(ProdutoEntity p) {
        Long catId = p.getCategoria() != null ? p.getCategoria().getId() : null;
        String catNome = p.getCategoria() != null ? p.getCategoria().getNome() : null;
        int saldo = p.getSaldoEstoque() != null ? p.getSaldoEstoque() : 0;
        return new ProdutoView(
                p.getId(),
                p.getNome(),
                p.getDescricao(),
                p.getPreco(),
                catId,
                catNome,
                p.getCodigoImpressao(),
                p.isAtivo(),
                saldo);
    }
}
