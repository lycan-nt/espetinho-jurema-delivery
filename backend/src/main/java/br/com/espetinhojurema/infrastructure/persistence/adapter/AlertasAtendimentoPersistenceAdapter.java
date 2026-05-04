package br.com.espetinhojurema.infrastructure.persistence.adapter;

import br.com.espetinhojurema.application.model.AlertaAtendimentoRegistroView;
import br.com.espetinhojurema.application.port.out.AlertasAtendimentoPersistencePort;
import br.com.espetinhojurema.infrastructure.persistence.entity.AlertaAtendimentoEntity;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.infrastructure.persistence.repository.AlertaAtendimentoJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AlertasAtendimentoPersistenceAdapter implements AlertasAtendimentoPersistencePort {

    private final AlertaAtendimentoJpaRepository repository;

    public AlertasAtendimentoPersistenceAdapter(AlertaAtendimentoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public String criarPendente(Long pedidoId, Integer mesaNumero) {
        var e = new AlertaAtendimentoEntity();
        e.setId(UUID.randomUUID().toString());
        e.setPedidoId(pedidoId);
        e.setMesaNumero(mesaNumero);
        e.setCriadoEm(Instant.now());
        repository.save(e);
        return e.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AlertaAtendimentoRegistroView> buscarPorId(String alertaId) {
        return repository.findById(alertaId).map(this::mapear);
    }

    @Override
    @Transactional
    public void marcarReconhecido(String alertaId, String loginUsuario) {
        AlertaAtendimentoEntity e = repository.findById(alertaId).orElseThrow(() -> new BusinessException("Alerta não encontrado"));
        e.setReconhecidoEm(Instant.now());
        e.setReconhecidoPor(loginUsuario);
        repository.save(e);
    }

    private AlertaAtendimentoRegistroView mapear(AlertaAtendimentoEntity e) {
        return new AlertaAtendimentoRegistroView(
                e.getId(), e.getPedidoId(), e.getMesaNumero(), e.getCriadoEm(), e.getReconhecidoEm(), e.getReconhecidoPor());
    }
}
