package br.com.delivere.acai.caixa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CaixaRepository extends JpaRepository<Caixa, Long> {

    Optional<Caixa> findByDataAndStatus(LocalDate data, String status);

    /** Verifica se existe algum caixa (aberto ou fechado) na data. */
    boolean existsByDataAndStatus(LocalDate data, String status);

    /** Lista caixas do período (para relatório), ordenado por data e abertura. */
    List<Caixa> findByDataBetweenOrderByDataAscDataHoraAberturaAsc(LocalDate dataInicio, LocalDate dataFim);
}
