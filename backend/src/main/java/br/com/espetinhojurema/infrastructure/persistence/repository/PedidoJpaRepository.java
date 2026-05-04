package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.domain.model.PedidoStatus;
import br.com.espetinhojurema.domain.model.PedidoTipo;
import br.com.espetinhojurema.infrastructure.persistence.entity.PedidoEntity;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoJpaRepository extends JpaRepository<PedidoEntity, Long> {

    Optional<PedidoEntity> findTopByMesa_IdAndStatusInOrderByCriadoEmDesc(
            Long mesaId,
            Collection<PedidoStatus> statuses);

    @Query("""
            select distinct p from PedidoEntity p
            left join fetch p.mesa m
            left join fetch p.cliente
            left join fetch p.colaborador
            left join fetch p.itens i
            left join fetch i.produto pr
            left join fetch pr.categoria
            where p.id = :id
            """)
    Optional<PedidoEntity> findDetalhePorId(@Param("id") Long id);

    @Query("""
            select distinct p from PedidoEntity p
            left join fetch p.mesa
            left join fetch p.cliente
            left join fetch p.colaborador
            left join fetch p.itens i
            left join fetch i.produto
            where (:status is null or p.status = :status)
            and (:tipo is null or p.tipo = :tipo)
            order by p.criadoEm desc
            """)
    List<PedidoEntity> filtrar(
            @Param("status") PedidoStatus status,
            @Param("tipo") PedidoTipo tipo);

    boolean existsByMesa_IdAndIdNotAndStatusIn(
            Long mesaId,
            Long pedidoId,
            Collection<PedidoStatus> statuses);

    @Query(
            """
            select count(p) from PedidoEntity p
            where p.status = :status
            and p.atualizadoEm >= :inicio
            and p.atualizadoEm < :fim
            """)
    long countEncerradosPagosNoPeriodo(
            @Param("status") PedidoStatus status,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim);
}
