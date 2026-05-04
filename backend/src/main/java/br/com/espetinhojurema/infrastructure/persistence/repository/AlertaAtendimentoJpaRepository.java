package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.infrastructure.persistence.entity.AlertaAtendimentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaAtendimentoJpaRepository extends JpaRepository<AlertaAtendimentoEntity, String> {
}
