package br.com.delivere.acai.sheets;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface PlanilhaDiaRepository extends JpaRepository<PlanilhaDia, Long> {

    Optional<PlanilhaDia> findByData(LocalDate data);
}
