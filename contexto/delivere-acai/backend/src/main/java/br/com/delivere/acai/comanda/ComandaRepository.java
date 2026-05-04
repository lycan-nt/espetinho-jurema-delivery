package br.com.delivere.acai.comanda;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComandaRepository extends JpaRepository<Comanda, Long> {

    /** Todas as comandas de um tipo (para calcular próximo número sequencial pelo identificador). */
    List<Comanda> findByTipo(TipoComanda tipo);

    List<Comanda> findByStatusOrderByDataHoraDesc(String status);

    List<Comanda> findByTipoAndIdentificador(TipoComanda tipo, String identificador);

    Optional<Comanda> findFirstByTipoAndIdentificadorAndStatus(TipoComanda tipo, String identificador, String status);

    List<Comanda> findByStatusOrderByDataFechamentoDesc(String status);

    List<Comanda> findByStatusAndDataFechamentoBetweenOrderByDataFechamentoDesc(
            String status, LocalDateTime dataInicio, LocalDateTime dataFim);

    /** Comandas abertas cuja abertura foi hoje (dataHora >= início do dia). */
    long countByStatusAndDataHoraGreaterThanEqual(String status, LocalDateTime dataHoraMin);

    /** Comandas abertas cuja abertura foi antes de hoje. */
    long countByStatusAndDataHoraLessThan(String status, LocalDateTime dataHoraMax);

    /** Comandas fechadas ainda sem NFC-e emitida. */
    @Query("SELECT COUNT(c) FROM Comanda c WHERE c.status = 'FECHADA' AND (c.chaveNfce IS NULL OR c.chaveNfce = '')")
    long countFechadasSemNfce();
}
