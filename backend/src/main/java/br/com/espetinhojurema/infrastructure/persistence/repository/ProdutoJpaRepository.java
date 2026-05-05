package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.infrastructure.persistence.entity.ProdutoEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProdutoJpaRepository extends JpaRepository<ProdutoEntity, Long> {

    @Query(
            """
            select p from ProdutoEntity p
            left join fetch p.categoria c
            where p.ativo = true
            order by coalesce(c.ordem, 999999), p.nome
            """)
    List<ProdutoEntity> findAllAtivosComCategoria();

    @Query(
            """
            select p from ProdutoEntity p left join fetch p.categoria c
            order by coalesce(c.ordem, 999999), p.nome
            """)
    List<ProdutoEntity> findAllComCategoria();
}
