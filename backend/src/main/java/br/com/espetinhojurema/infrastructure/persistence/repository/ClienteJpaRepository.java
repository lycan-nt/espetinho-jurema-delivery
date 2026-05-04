package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.infrastructure.persistence.entity.ClienteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteJpaRepository extends JpaRepository<ClienteEntity, Long> {
}
