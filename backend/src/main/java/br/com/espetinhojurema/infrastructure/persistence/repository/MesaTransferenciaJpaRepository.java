package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.infrastructure.persistence.entity.MesaTransferenciaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MesaTransferenciaJpaRepository extends JpaRepository<MesaTransferenciaEntity, Long> {

    List<MesaTransferenciaEntity> findByPedido_IdOrderByCriadoEmAsc(Long pedidoId);
}
