package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.TaxaGarcomCalculo;
import br.com.espetinhojurema.application.model.TaxaGarcomConfigView;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.domain.model.PedidoTipo;
import br.com.espetinhojurema.infrastructure.persistence.entity.ConfiguracaoSistemaEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.ConfiguracaoSistemaJpaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaxaGarcomOperacaoService {

    private static final BigDecimal CEM = new BigDecimal("100");

    private final ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository;

    public TaxaGarcomOperacaoService(ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository) {
        this.configuracaoSistemaJpaRepository = configuracaoSistemaJpaRepository;
    }

    @Transactional(readOnly = true)
    public TaxaGarcomConfigView obterConfig() {
        return mapear(carregarOuCriarConfig());
    }

    @Transactional
    public TaxaGarcomConfigView atualizarConfig(boolean habilitada, BigDecimal percentual) {
        if (percentual == null) {
            throw new BusinessException("Informe o percentual da taxa.");
        }
        BigDecimal pct = percentual.setScale(2, RoundingMode.HALF_UP);
        if (pct.compareTo(BigDecimal.ZERO) < 0 || pct.compareTo(CEM) > 0) {
            throw new BusinessException("Percentual deve estar entre 0 e 100.");
        }
        if (habilitada && pct.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Para habilitar a taxa do garçom, informe um percentual maior que zero.");
        }
        ConfiguracaoSistemaEntity cfg = carregarOuCriarConfig();
        cfg.setTaxaGarcomHabilitada(habilitada);
        cfg.setTaxaGarcomPercentual(pct);
        configuracaoSistemaJpaRepository.save(cfg);
        return mapear(cfg);
    }

    @Transactional(readOnly = true)
    public TaxaGarcomCalculo calcular(PedidoTipo tipo, BigDecimal subtotalItens) {
        if (tipo != PedidoTipo.MESA) {
            return TaxaGarcomCalculo.naoAplicavel();
        }
        BigDecimal subtotal = subtotalItens != null ? subtotalItens : BigDecimal.ZERO;
        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            return TaxaGarcomCalculo.naoAplicavel();
        }
        ConfiguracaoSistemaEntity cfg = carregarOuCriarConfig();
        if (!cfg.isTaxaGarcomHabilitada()) {
            return TaxaGarcomCalculo.naoAplicavel();
        }
        BigDecimal pct = cfg.getTaxaGarcomPercentual();
        if (pct == null || pct.compareTo(BigDecimal.ZERO) <= 0) {
            return TaxaGarcomCalculo.naoAplicavel();
        }
        pct = pct.setScale(2, RoundingMode.HALF_UP);
        BigDecimal valor = subtotal
                .multiply(pct)
                .divide(CEM, 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);
        return new TaxaGarcomCalculo(pct, valor);
    }

    private TaxaGarcomConfigView mapear(ConfiguracaoSistemaEntity cfg) {
        BigDecimal pct = cfg.getTaxaGarcomPercentual();
        if (pct == null) {
            pct = new BigDecimal("10.00");
        } else {
            pct = pct.setScale(2, RoundingMode.HALF_UP);
        }
        return new TaxaGarcomConfigView(cfg.isTaxaGarcomHabilitada(), pct);
    }

    private ConfiguracaoSistemaEntity carregarOuCriarConfig() {
        return configuracaoSistemaJpaRepository
                .findById(ConfiguracaoSistemaEntity.ID_UNICO)
                .orElseGet(() -> {
                    var c = new ConfiguracaoSistemaEntity();
                    c.setId(ConfiguracaoSistemaEntity.ID_UNICO);
                    return configuracaoSistemaJpaRepository.save(c);
                });
    }
}
