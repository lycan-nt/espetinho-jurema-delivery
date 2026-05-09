package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.application.model.ComandaCabecalhoCampos;
import br.com.espetinhojurema.application.model.EmpresaDadosPatch;
import br.com.espetinhojurema.application.model.EmpresaDadosView;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.infrastructure.persistence.entity.ConfiguracaoSistemaEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.ConfiguracaoSistemaJpaRepository;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmpresaDadosOperacaoService {

    private static final Pattern EMAIL_SIMPLES =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository;

    public EmpresaDadosOperacaoService(ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository) {
        this.configuracaoSistemaJpaRepository = configuracaoSistemaJpaRepository;
    }

    @Transactional(readOnly = true)
    public EmpresaDadosView obter() {
        return configuracaoSistemaJpaRepository
                .findById(ConfiguracaoSistemaEntity.ID_UNICO)
                .map(this::mapear)
                .orElse(vazio());
    }

    private static EmpresaDadosView vazio() {
        return new EmpresaDadosView(null, null, null, null, null, null, ComandaCabecalhoCampos.todosVisiveis());
    }

    @Transactional
    public EmpresaDadosView atualizar(EmpresaDadosPatch patch) {
        ConfiguracaoSistemaEntity e = configuracaoSistemaJpaRepository
                .findById(ConfiguracaoSistemaEntity.ID_UNICO)
                .orElseGet(() -> {
                    var c = new ConfiguracaoSistemaEntity();
                    c.setId(ConfiguracaoSistemaEntity.ID_UNICO);
                    return c;
                });

        String cnpj = trimOuNull(patch.cnpj(), 20);
        String nome = trimOuNull(patch.nomeEmpresa(), 200);
        String endereco = trimOuNull(patch.endereco(), 500);
        String telefone = trimOuNull(patch.telefone(), 40);
        String email = trimOuNull(patch.email(), 120);
        String instagram = trimOuNull(patch.instagram(), 120);

        if (email != null && !EMAIL_SIMPLES.matcher(email).matches()) {
            throw new BusinessException("E-mail inválido.");
        }

        e.setEmpresaCnpj(cnpj);
        e.setEmpresaNome(nome);
        e.setEmpresaEndereco(endereco);
        e.setEmpresaTelefone(telefone);
        e.setEmpresaEmail(email);
        e.setEmpresaInstagram(instagram);

        if (patch.comandaCabecalho() != null) {
            ComandaCabecalhoCampos c = patch.comandaCabecalho();
            e.setComandaCabecalhoExibirCnpj(c.cnpj());
            e.setComandaCabecalhoExibirNome(c.nomeEmpresa());
            e.setComandaCabecalhoExibirEndereco(c.endereco());
            e.setComandaCabecalhoExibirTelefone(c.telefone());
            e.setComandaCabecalhoExibirEmail(c.email());
            e.setComandaCabecalhoExibirInstagram(c.instagram());
        }

        configuracaoSistemaJpaRepository.save(e);
        return mapear(e);
    }

    private EmpresaDadosView mapear(ConfiguracaoSistemaEntity e) {
        ComandaCabecalhoCampos cab = new ComandaCabecalhoCampos(
                e.isComandaCabecalhoExibirCnpj(),
                e.isComandaCabecalhoExibirNome(),
                e.isComandaCabecalhoExibirEndereco(),
                e.isComandaCabecalhoExibirTelefone(),
                e.isComandaCabecalhoExibirEmail(),
                e.isComandaCabecalhoExibirInstagram());
        return new EmpresaDadosView(
                e.getEmpresaCnpj(),
                e.getEmpresaNome(),
                e.getEmpresaEndereco(),
                e.getEmpresaTelefone(),
                e.getEmpresaEmail(),
                e.getEmpresaInstagram(),
                cab);
    }

    private static String trimOuNull(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.strip();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > max) {
            throw new BusinessException("Campo excede o tamanho máximo permitido (" + max + " caracteres).");
        }
        return t;
    }
}
