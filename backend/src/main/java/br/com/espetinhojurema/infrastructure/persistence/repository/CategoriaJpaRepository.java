package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.infrastructure.persistence.entity.CategoriaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaJpaRepository extends JpaRepository<CategoriaEntity, Long> {
}
