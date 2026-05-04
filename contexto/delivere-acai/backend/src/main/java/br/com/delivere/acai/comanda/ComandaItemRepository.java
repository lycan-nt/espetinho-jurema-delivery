package br.com.delivere.acai.comanda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComandaItemRepository extends JpaRepository<ComandaItem, Long> {

    List<ComandaItem> findByComandaIdOrderByIdAsc(Long comandaId);

    Optional<ComandaItem> findFirstByComandaIdAndPesoKgAndValorTotalOrderByIdDesc(
            Long comandaId, BigDecimal pesoKg, BigDecimal valorTotal);
}
