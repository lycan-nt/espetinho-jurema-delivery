package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.infrastructure.persistence.entity.ConfiguracaoSistemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoSistemaJpaRepository extends JpaRepository<ConfiguracaoSistemaEntity, Long> {}
