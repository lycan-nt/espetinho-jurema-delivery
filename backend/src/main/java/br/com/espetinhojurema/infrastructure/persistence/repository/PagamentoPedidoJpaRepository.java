package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.infrastructure.persistence.entity.PagamentoPedidoEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagamentoPedidoJpaRepository extends JpaRepository<PagamentoPedidoEntity, Long> {

    @Query(
            """
            select p.forma, coalesce(sum(p.valor), 0)
            from PagamentoPedidoEntity p
            join p.pedido ped
            where coalesce(p.criadoEm, ped.atualizadoEm) >= :inicio
            and coalesce(p.criadoEm, ped.atualizadoEm) < :fim
            group by p.forma
            order by p.forma
            """)
    List<Object[]> totaisPorFormaNoPeriodo(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
}
