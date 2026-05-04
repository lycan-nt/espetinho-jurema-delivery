package br.com.espetinhojurema.application.service;

import br.com.espetinhojurema.api.dto.AtualizarProdutoRequest;
import br.com.espetinhojurema.api.dto.AtualizarUsuarioRequest;
import br.com.espetinhojurema.api.dto.CriarProdutoRequest;
import br.com.espetinhojurema.api.dto.CriarUsuarioRequest;
import br.com.espetinhojurema.api.dto.UsuarioAdminView;
import br.com.espetinhojurema.application.model.ProdutoView;
import br.com.espetinhojurema.domain.exception.BusinessException;
import br.com.espetinhojurema.domain.model.PerfilUsuario;
import br.com.espetinhojurema.infrastructure.persistence.entity.ProdutoEntity;
import br.com.espetinhojurema.infrastructure.persistence.entity.UsuarioEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.CategoriaJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.ProdutoJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.UsuarioJpaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCadastrosService {

    private final UsuarioJpaRepository usuarioJpaRepository;
    private final ProdutoJpaRepository produtoJpaRepository;
    private final CategoriaJpaRepository categoriaJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminCadastrosService(
            UsuarioJpaRepository usuarioJpaRepository,
            ProdutoJpaRepository produtoJpaRepository,
            CategoriaJpaRepository categoriaJpaRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioJpaRepository = usuarioJpaRepository;
        this.produtoJpaRepository = produtoJpaRepository;
        this.categoriaJpaRepository = categoriaJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioAdminView> listarUsuarios() {
        return usuarioJpaRepository.findAllByOrderByLoginAsc().stream()
                .map(u -> new UsuarioAdminView(
                        u.getId(), u.getLogin(), u.getNomeExibicao(), u.getPerfil(), u.isAtivo()))
                .toList();
    }

    @Transactional
    public UsuarioAdminView criarUsuario(CriarUsuarioRequest req) {
        String login = req.login().trim().toLowerCase();
        if (login.isEmpty()) {
            throw new BusinessException("Login inválido.");
        }
        if (usuarioJpaRepository.existsByLoginIgnoreCase(login)) {
            throw new BusinessException("Já existe usuário com este login.");
        }
        var u = new UsuarioEntity();
        u.setLogin(login);
        u.setNomeExibicao(req.nomeExibicao().trim());
        u.setPerfil(req.perfil());
        u.setSenhaHash(passwordEncoder.encode(req.senha()));
        u.setAtivo(true);
        usuarioJpaRepository.save(u);
        return new UsuarioAdminView(u.getId(), u.getLogin(), u.getNomeExibicao(), u.getPerfil(), u.isAtivo());
    }

    @Transactional
    public UsuarioAdminView atualizarUsuario(Long id, AtualizarUsuarioRequest req, Long usuarioLogadoId) {
        var u = usuarioJpaRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado."));
        if (!req.ativo() && u.getId().equals(usuarioLogadoId)) {
            throw new BusinessException("Você não pode desativar o seu próprio usuário.");
        }
        garantirUltimoAtendimento(u, req.perfil(), req.ativo());
        u.setNomeExibicao(req.nomeExibicao().trim());
        u.setPerfil(req.perfil());
        u.setAtivo(req.ativo());
        if (req.senha() != null && !req.senha().isBlank()) {
            if (req.senha().length() < 6) {
                throw new BusinessException("Senha deve ter no mínimo 6 caracteres.");
            }
            u.setSenhaHash(passwordEncoder.encode(req.senha()));
        }
        usuarioJpaRepository.save(u);
        return new UsuarioAdminView(u.getId(), u.getLogin(), u.getNomeExibicao(), u.getPerfil(), u.isAtivo());
    }

    private void garantirUltimoAtendimento(UsuarioEntity atual, PerfilUsuario novoPerfil, boolean novoAtivo) {
        if (atual.getPerfil() != PerfilUsuario.ATENDIMENTO || !atual.isAtivo()) {
            return;
        }
        boolean deixaDeSerAtendimentoAtivo =
                novoPerfil != PerfilUsuario.ATENDIMENTO || !novoAtivo;
        if (!deixaDeSerAtendimentoAtivo) {
            return;
        }
        long outros = usuarioJpaRepository.countByPerfilAndAtivoTrueAndIdNot(PerfilUsuario.ATENDIMENTO, atual.getId());
        if (outros == 0) {
            throw new BusinessException("Deve existir pelo menos outro usuário de atendimento ativo antes desta alteração.");
        }
    }

    @Transactional(readOnly = true)
    public List<ProdutoView> listarTodosProdutos() {
        return produtoJpaRepository.findAllComCategoria().stream()
                .map(this::mapearProduto)
                .toList();
    }

    @Transactional
    public ProdutoView criarProduto(CriarProdutoRequest req) {
        var cat = categoriaJpaRepository
                .findById(req.categoriaId())
                .orElseThrow(() -> new BusinessException("Categoria não encontrada."));
        var p = new ProdutoEntity();
        p.setNome(req.nome().trim());
        p.setDescricao(req.descricao() != null && !req.descricao().isBlank() ? req.descricao().trim() : null);
        p.setPreco(req.preco().setScale(2, RoundingMode.HALF_UP));
        p.setCategoria(cat);
        p.setCodigoImpressao(
                req.codigoImpressao() != null && !req.codigoImpressao().isBlank()
                        ? req.codigoImpressao().trim()
                        : null);
        p.setAtivo(req.ativo());
        if (p.getSaldoEstoque() == null) {
            p.setSaldoEstoque(0);
        }
        produtoJpaRepository.save(p);
        return mapearProduto(p);
    }

    @Transactional
    public ProdutoView atualizarProduto(Long id, AtualizarProdutoRequest req) {
        var p = produtoJpaRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException("Produto não encontrado."));
        var cat = categoriaJpaRepository
                .findById(req.categoriaId())
                .orElseThrow(() -> new BusinessException("Categoria não encontrada."));
        p.setNome(req.nome().trim());
        p.setDescricao(req.descricao() != null && !req.descricao().isBlank() ? req.descricao().trim() : null);
        p.setPreco(req.preco().setScale(2, RoundingMode.HALF_UP));
        p.setCategoria(cat);
        p.setCodigoImpressao(
                req.codigoImpressao() != null && !req.codigoImpressao().isBlank()
                        ? req.codigoImpressao().trim()
                        : null);
        p.setAtivo(req.ativo());
        produtoJpaRepository.save(p);
        return mapearProduto(p);
    }

    private ProdutoView mapearProduto(ProdutoEntity p) {
        Long catId = p.getCategoria() != null ? p.getCategoria().getId() : null;
        String catNome = p.getCategoria() != null ? p.getCategoria().getNome() : null;
        int saldo = p.getSaldoEstoque() != null ? p.getSaldoEstoque() : 0;
        BigDecimal preco = p.getPreco() != null ? p.getPreco() : BigDecimal.ZERO;
        return new ProdutoView(
                p.getId(),
                p.getNome(),
                p.getDescricao(),
                preco,
                catId,
                catNome,
                p.getCodigoImpressao(),
                p.isAtivo(),
                saldo);
    }
}
