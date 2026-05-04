package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.infrastructure.persistence.entity.MovimentoEstoqueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentoEstoqueJpaRepository extends JpaRepository<MovimentoEstoqueEntity, Long> {}
