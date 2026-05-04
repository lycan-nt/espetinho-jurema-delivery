package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.infrastructure.persistence.entity.MesaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MesaJpaRepository extends JpaRepository<MesaEntity, Long> {

    Optional<MesaEntity> findByNumero(int numero);
}
