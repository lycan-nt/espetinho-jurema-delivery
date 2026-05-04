package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.domain.model.PedidoStatus;
import br.com.espetinhojurema.infrastructure.persistence.entity.ItemPedidoEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemPedidoJpaRepository extends JpaRepository<ItemPedidoEntity, Long> {

    /**
     * Soma (qtd × preço) por produto em pedidos pagos cuja data de encerramento ({@code pedido.atualizadoEm})
     * está no intervalo [início, fim).
     */
    @Query(
            """
            select pr.id, pr.nome, coalesce(sum(i.quantidade * i.precoUnitario), 0)
            from ItemPedidoEntity i
            join i.pedido ped
            join i.produto pr
            where ped.status = :status
            and ped.atualizadoEm >= :inicio
            and ped.atualizadoEm < :fim
            and (i.cancelado is null or i.cancelado = false)
            group by pr.id, pr.nome
            order by sum(i.quantidade * i.precoUnitario) desc
            """)
    List<Object[]> totaisPorProdutoPedidosPagosNoPeriodo(
            @Param("status") PedidoStatus status,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim);
}
