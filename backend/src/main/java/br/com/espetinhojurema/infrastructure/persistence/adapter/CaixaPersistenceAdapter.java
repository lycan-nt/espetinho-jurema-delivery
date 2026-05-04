package br.com.espetinhojurema.infrastructure.persistence.adapter;

import br.com.espetinhojurema.application.model.CaixaStatusView;
import br.com.espetinhojurema.application.port.out.CaixaPersistencePort;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.infrastructure.persistence.entity.CaixaSessaoEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.CaixaSessaoJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CaixaPersistenceAdapter implements CaixaPersistencePort {

    private final CaixaSessaoJpaRepository caixaSessaoJpaRepository;

    public CaixaPersistenceAdapter(CaixaSessaoJpaRepository caixaSessaoJpaRepository) {
        this.caixaSessaoJpaRepository = caixaSessaoJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CaixaStatusView statusAtual() {
        return caixaSessaoJpaRepository.findFirstByAbertoTrueOrderByAbertoEmDesc()
                .map(this::mapear)
                .orElse(new CaixaStatusView(false, null, null, null, null, null));
    }

    @Override
    @Transactional
    public CaixaStatusView abrirSessao(BigDecimal saldoAbertura) {
        if (caixaSessaoJpaRepository.findFirstByAbertoTrueOrderByAbertoEmDesc().isPresent()) {
            throw new BusinessException("Caixa já está aberto");
        }
        var e = new CaixaSessaoEntity();
        e.setAberto(true);
        e.setAbertoEm(Instant.now());
        e.setSaldoAbertura(saldoAbertura != null ? saldoAbertura : BigDecimal.ZERO);
        return mapear(caixaSessaoJpaRepository.save(e));
    }

    @Override
    @Transactional
    public CaixaStatusView fecharSessao(BigDecimal saldoFechamento) {
        CaixaSessaoEntity e = caixaSessaoJpaRepository.findFirstByAbertoTrueOrderByAbertoEmDesc()
                .orElseThrow(() -> new BusinessException("Não há caixa aberto"));
        e.setAberto(false);
        e.setFechadoEm(Instant.now());
        e.setSaldoFechamento(saldoFechamento != null ? saldoFechamento : BigDecimal.ZERO);
        return mapear(caixaSessaoJpaRepository.save(e));
    }

    private CaixaStatusView mapear(CaixaSessaoEntity e) {
        return new CaixaStatusView(
                e.isAberto(),
                e.getAbertoEm(),
                e.getFechadoEm(),
                e.getSaldoAbertura(),
                e.getSaldoFechamento(),
                e.getId());
    }
}
