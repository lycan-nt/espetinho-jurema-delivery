package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.infrastructure.persistence.entity.ColaboradorEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ColaboradorJpaRepository extends JpaRepository<ColaboradorEntity, Long> {

    @Query("select c from ColaboradorEntity c where c.ativo = true order by c.nome")
    List<ColaboradorEntity> findAllAtivos();
}
