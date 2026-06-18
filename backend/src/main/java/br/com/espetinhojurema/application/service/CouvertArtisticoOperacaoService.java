package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.CouvertArtisticoCalculo;
import br.com.espetinhojurema.application.model.CouvertArtisticoConfigView;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.domain.model.PedidoTipo;
import br.com.espetinhojurema.infrastructure.persistence.entity.ConfiguracaoSistemaEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.ConfiguracaoSistemaJpaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouvertArtisticoOperacaoService {

    private final ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository;

    public CouvertArtisticoOperacaoService(ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository) {
        this.configuracaoSistemaJpaRepository = configuracaoSistemaJpaRepository;
    }

    @Transactional(readOnly = true)
    public CouvertArtisticoConfigView obterConfig() {
        ConfiguracaoSistemaEntity cfg = carregarOuCriarConfig();
        return mapear(cfg);
    }

    @Transactional
    public CouvertArtisticoConfigView atualizarConfig(boolean ativo, BigDecimal valorPorPessoa) {
        if (valorPorPessoa == null) {
            throw new BusinessException("Informe o valor por pessoa.");
        }
        BigDecimal valor = valorPorPessoa.setScale(2, RoundingMode.HALF_UP);
        if (valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Valor por pessoa não pode ser negativo.");
        }
        if (ativo && valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Para ativar o couvert artístico, informe um valor por pessoa maior que zero.");
        }
        ConfiguracaoSistemaEntity cfg = carregarOuCriarConfig();
        cfg.setCouvertArtisticoAtivo(ativo);
        cfg.setCouvertArtisticoValorPorPessoa(valor);
        configuracaoSistemaJpaRepository.save(cfg);
        return mapear(cfg);
    }

    @Transactional(readOnly = true)
    public CouvertArtisticoCalculo calcular(PedidoTipo tipo, Integer pessoas) {
        if (tipo != PedidoTipo.MESA) {
            return CouvertArtisticoCalculo.naoAplicavel();
        }
        ConfiguracaoSistemaEntity cfg = carregarOuCriarConfig();
        if (!cfg.isCouvertArtisticoAtivo()) {
            return CouvertArtisticoCalculo.naoAplicavel();
        }
        BigDecimal valorPorPessoa = cfg.getCouvertArtisticoValorPorPessoa();
        if (valorPorPessoa == null || valorPorPessoa.compareTo(BigDecimal.ZERO) <= 0) {
            return CouvertArtisticoCalculo.naoAplicavel();
        }
        int n = pessoas != null && pessoas > 0 ? pessoas : 1;
        BigDecimal total = valorPorPessoa
                .multiply(BigDecimal.valueOf(n))
                .setScale(2, RoundingMode.HALF_UP);
        return new CouvertArtisticoCalculo(
                valorPorPessoa.setScale(2, RoundingMode.HALF_UP), n, total);
    }

    private CouvertArtisticoConfigView mapear(ConfiguracaoSistemaEntity cfg) {
        BigDecimal valor = cfg.getCouvertArtisticoValorPorPessoa();
        if (valor == null) {
            valor = BigDecimal.ZERO;
        } else {
            valor = valor.setScale(2, RoundingMode.HALF_UP);
        }
        return new CouvertArtisticoConfigView(cfg.isCouvertArtisticoAtivo(), valor);
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
