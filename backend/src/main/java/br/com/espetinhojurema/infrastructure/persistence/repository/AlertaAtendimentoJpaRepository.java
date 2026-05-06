package br.com.espetinhojurema.infrastructure.persistence.repository;

import br.com.espetinhojurema.domain.model.TipoAlertaAtendimento;
import br.com.espetinhojurema.infrastructure.persistence.entity.AlertaAtendimentoEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaAtendimentoJpaRepository extends JpaRepository<AlertaAtendimentoEntity, String> {

    Optional<AlertaAtendimentoEntity> findFirstByPedidoIdAndTipoAndReconhecidoEmIsNullOrderByCriadoEmDesc(
            Long pedidoId, TipoAlertaAtendimento tipoAlerta);

    /** Último alerta COMANDA_ENVIADA do pedido criado ANTES de {@code anteriorA} — para obter o corte de itens. */
    Optional<AlertaAtendimentoEntity> findFirstByPedidoIdAndTipoAndCriadoEmBeforeOrderByCriadoEmDesc(
            Long pedidoId, TipoAlertaAtendimento tipo, Instant anteriorA);
}
