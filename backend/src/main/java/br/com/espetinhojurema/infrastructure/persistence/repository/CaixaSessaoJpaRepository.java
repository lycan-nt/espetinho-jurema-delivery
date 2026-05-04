package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.infrastructure.persistence.entity.CaixaSessaoEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaixaSessaoJpaRepository extends JpaRepository<CaixaSessaoEntity, Long> {

    Optional<CaixaSessaoEntity> findFirstByAbertoTrueOrderByAbertoEmDesc();
}
