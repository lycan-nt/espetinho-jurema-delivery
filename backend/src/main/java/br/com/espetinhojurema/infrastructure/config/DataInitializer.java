package br.com.espetinhojurema.infrastructure.config;

import br.com.espetinhojurema.application.port.out.CaixaPersistencePort;
import br.com.espetinhojurema.domain.model.PerfilUsuario;
import br.com.espetinhojurema.infrastructure.persistence.entity.UsuarioEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.CaixaSessaoJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.entity.CategoriaEntity;
import br.com.espetinhojurema.infrastructure.persistence.entity.ColaboradorEntity;
import br.com.espetinhojurema.infrastructure.persistence.entity.MesaEntity;
import br.com.espetinhojurema.infrastructure.persistence.entity.ProdutoEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.CategoriaJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.ColaboradorJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.MesaJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.entity.ConfiguracaoSistemaEntity;
import br.com.espetinhojurema.infrastructure.persistence.repository.ConfiguracaoSistemaJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.ProdutoJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.UsuarioJpaRepository;
import br.com.espetinhojurema.domain.model.MesaStatus;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {

    private final CategoriaJpaRepository categoriaJpaRepository;
    private final ProdutoJpaRepository produtoJpaRepository;
    private final MesaJpaRepository mesaJpaRepository;
    private final ColaboradorJpaRepository colaboradorJpaRepository;
    private final CaixaPersistencePort caixaPersistencePort;
    private final CaixaSessaoJpaRepository caixaSessaoJpaRepository;
    private final UsuarioJpaRepository usuarioJpaRepository;
    private final ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final int quantidadeMesas;

    public DataInitializer(
            CategoriaJpaRepository categoriaJpaRepository,
            ProdutoJpaRepository produtoJpaRepository,
            MesaJpaRepository mesaJpaRepository,
            ColaboradorJpaRepository colaboradorJpaRepository,
            CaixaPersistencePort caixaPersistencePort,
            CaixaSessaoJpaRepository caixaSessaoJpaRepository,
            UsuarioJpaRepository usuarioJpaRepository,
            ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.mesas.quantidade-inicial:40}") int quantidadeMesas) {
        this.categoriaJpaRepository = categoriaJpaRepository;
        this.produtoJpaRepository = produtoJpaRepository;
        this.mesaJpaRepository = mesaJpaRepository;
        this.colaboradorJpaRepository = colaboradorJpaRepository;
        this.caixaPersistencePort = caixaPersistencePort;
        this.caixaSessaoJpaRepository = caixaSessaoJpaRepository;
        this.usuarioJpaRepository = usuarioJpaRepository;
        this.configuracaoSistemaJpaRepository = configuracaoSistemaJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.quantidadeMesas = quantidadeMesas;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (categoriaJpaRepository.count() == 0) {
            var catEsp = categoria("Espetinhos", 1);
            var catBeb = categoria("Bebidas", 2);
            var catAco = categoria("Acompanhamentos", 3);
            produto("Espetinho de Carne", "Carne bovina grelhada", new BigDecimal("12.00"), catEsp, "E01");
            produto("Espetinho de Frango", "Peito de frango", new BigDecimal("10.00"), catEsp, "E02");
            produto("Espetinho de Linguiça", "Linguiça toscana", new BigDecimal("9.00"), catEsp, "E03");
            produto("Kafta", "Carne moída temperada", new BigDecimal("11.00"), catEsp, "E04");
            produto("Cerveja Lata", "350ml", new BigDecimal("6.00"), catBeb, "B01");
            produto("Refrigerante", "Lata", new BigDecimal("5.00"), catBeb, "B02");
            produto("Água", "500ml", new BigDecimal("3.00"), catBeb, "B03");
            produto("Farofa", "Porção", new BigDecimal("5.00"), catAco, "A01");
            produto("Vinagrete", "Porção", new BigDecimal("5.00"), catAco, "A02");
        }

        if (mesaJpaRepository.count() == 0) {
            for (int n = 1; n <= quantidadeMesas; n++) {
                var m = new MesaEntity();
                m.setNumero(n);
                m.setStatus(MesaStatus.LIVRE);
                mesaJpaRepository.save(m);
            }
        }

        if (colaboradorJpaRepository.count() == 0) {
            var c = new ColaboradorEntity();
            c.setNome("Colaborador padrão");
            c.setAtivo(true);
            colaboradorJpaRepository.save(c);
        }

        if (caixaSessaoJpaRepository.count() == 0) {
            caixaPersistencePort.abrirSessao(BigDecimal.ZERO);
        }

        if (usuarioJpaRepository.count() == 0) {
            criarUsuario("atendimento", "Atendimento (caixa)", PerfilUsuario.ATENDIMENTO, "atendimento123");
            criarUsuario("garcom", "Garçom", PerfilUsuario.GARCOM, "garcom123");
            criarUsuario("churrasqueiro", "Churrasqueiro", PerfilUsuario.CHURRASQUEIRO, "churrasqueiro123");
        }

        if (configuracaoSistemaJpaRepository.count() == 0) {
            var cfg = new ConfiguracaoSistemaEntity();
            cfg.setId(ConfiguracaoSistemaEntity.ID_UNICO);
            cfg.setEstoqueObrigatorio(false);
            configuracaoSistemaJpaRepository.save(cfg);
        }
    }

    private void criarUsuario(String login, String nome, PerfilUsuario perfil, String senhaPlana) {
        var u = new UsuarioEntity();
        u.setLogin(login);
        u.setNomeExibicao(nome);
        u.setPerfil(perfil);
        u.setSenhaHash(passwordEncoder.encode(senhaPlana));
        u.setAtivo(true);
        usuarioJpaRepository.save(u);
    }

    private CategoriaEntity categoria(String nome, int ordem) {
        var c = new CategoriaEntity();
        c.setNome(nome);
        c.setOrdem(ordem);
        return categoriaJpaRepository.save(c);
    }

    private void produto(String nome, String desc, BigDecimal preco, CategoriaEntity cat, String codImpressao) {
        var p = new ProdutoEntity();
        p.setNome(nome);
        p.setDescricao(desc);
        p.setPreco(preco);
        p.setCategoria(cat);
        p.setCodigoImpressao(codImpressao);
        p.setAtivo(true);
        p.setSaldoEstoque(100);
        produtoJpaRepository.save(p);
    }
}
