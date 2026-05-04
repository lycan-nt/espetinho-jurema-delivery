package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.ImpressaoConfigView;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.infrastructure.persistence.entity.ConfiguracaoSistemaEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.ConfiguracaoSistemaJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImpressaoConfigOperacaoService {

    private final ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository;
    private final ImpressaoCupsService impressaoCupsService;

    public ImpressaoConfigOperacaoService(
            ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository,
            ImpressaoCupsService impressaoCupsService) {
        this.configuracaoSistemaJpaRepository = configuracaoSistemaJpaRepository;
        this.impressaoCupsService = impressaoCupsService;
    }

    @Transactional(readOnly = true)
    public ImpressaoConfigView obterConfig() {
        return configuracaoSistemaJpaRepository
                .findById(ConfiguracaoSistemaEntity.ID_UNICO)
                .map(e -> new ImpressaoConfigView(e.getNomeImpressoraLp()))
                .orElse(new ImpressaoConfigView(null));
    }

    /** Nome da fila configurado e não vazio, para usar com {@link ImpressaoCupsService}. */
    @Transactional(readOnly = true)
    public Optional<String> nomeImpressoraLpOuVazio() {
        return configuracaoSistemaJpaRepository
                .findById(ConfiguracaoSistemaEntity.ID_UNICO)
                .map(ConfiguracaoSistemaEntity::getNomeImpressoraLp)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim);
    }

    @Transactional
    public ImpressaoConfigView atualizar(String nomeImpressoraLp) {
        ConfiguracaoSistemaEntity e = configuracaoSistemaJpaRepository
                .findById(ConfiguracaoSistemaEntity.ID_UNICO)
                .orElseGet(() -> {
                    var c = new ConfiguracaoSistemaEntity();
                    c.setId(ConfiguracaoSistemaEntity.ID_UNICO);
                    return c;
                });
        if (nomeImpressoraLp == null || nomeImpressoraLp.isBlank()) {
            e.setNomeImpressoraLp(null);
        } else {
            String t = nomeImpressoraLp.trim();
            if (!impressaoCupsService.nomeFilaValido(t)) {
                throw new BusinessException(
                        "Nome da impressora inválido. Use apenas letras, números, espaços, ponto, traço e sublinhado.");
            }
            e.setNomeImpressoraLp(t);
        }
        configuracaoSistemaJpaRepository.save(e);
        return obterConfig();
    }
}
