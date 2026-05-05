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
import br.com.espetinhojurema.infrastructure.persistence.repository.ItemPedidoJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.MovimentoEstoqueJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.PagamentoPedidoJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.ProdutoJpaRepository;
import br.com.espetinhojurema.infrastructure.persistence.repository.UsuarioJpaRepository;
import br.com.espetinhojurema.domain.model.MesaStatus;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CategoriaJpaRepository categoriaJpaRepository;
    private final ProdutoJpaRepository produtoJpaRepository;
    private final ItemPedidoJpaRepository itemPedidoJpaRepository;
    private final PagamentoPedidoJpaRepository pagamentoPedidoJpaRepository;
    private final MovimentoEstoqueJpaRepository movimentoEstoqueJpaRepository;
    private final MesaJpaRepository mesaJpaRepository;
    private final ColaboradorJpaRepository colaboradorJpaRepository;
    private final CaixaPersistencePort caixaPersistencePort;
    private final CaixaSessaoJpaRepository caixaSessaoJpaRepository;
    private final UsuarioJpaRepository usuarioJpaRepository;
    private final ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final int quantidadeMesas;
    private final boolean forcarReseedCatalogo;
    private final int versaoSeedOficial;

    public DataInitializer(
            CategoriaJpaRepository categoriaJpaRepository,
            ProdutoJpaRepository produtoJpaRepository,
            ItemPedidoJpaRepository itemPedidoJpaRepository,
            PagamentoPedidoJpaRepository pagamentoPedidoJpaRepository,
            MovimentoEstoqueJpaRepository movimentoEstoqueJpaRepository,
            MesaJpaRepository mesaJpaRepository,
            ColaboradorJpaRepository colaboradorJpaRepository,
            CaixaPersistencePort caixaPersistencePort,
            CaixaSessaoJpaRepository caixaSessaoJpaRepository,
            UsuarioJpaRepository usuarioJpaRepository,
            ConfiguracaoSistemaJpaRepository configuracaoSistemaJpaRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.mesas.quantidade-inicial:40}") int quantidadeMesas,
            @Value("${app.catalogo.forcar-reseed:false}") boolean forcarReseedCatalogo,
            @Value("${app.catalogo.versao-seed-oficial:2}") int versaoSeedOficial) {
        this.categoriaJpaRepository = categoriaJpaRepository;
        this.produtoJpaRepository = produtoJpaRepository;
        this.itemPedidoJpaRepository = itemPedidoJpaRepository;
        this.pagamentoPedidoJpaRepository = pagamentoPedidoJpaRepository;
        this.movimentoEstoqueJpaRepository = movimentoEstoqueJpaRepository;
        this.mesaJpaRepository = mesaJpaRepository;
        this.colaboradorJpaRepository = colaboradorJpaRepository;
        this.caixaPersistencePort = caixaPersistencePort;
        this.caixaSessaoJpaRepository = caixaSessaoJpaRepository;
        this.usuarioJpaRepository = usuarioJpaRepository;
        this.configuracaoSistemaJpaRepository = configuracaoSistemaJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.quantidadeMesas = quantidadeMesas;
        this.forcarReseedCatalogo = forcarReseedCatalogo;
        this.versaoSeedOficial = versaoSeedOficial;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int versaoGravada =
                configuracaoSistemaJpaRepository
                        .findById(ConfiguracaoSistemaEntity.ID_UNICO)
                        .map(c -> c.getVersaoCatalogoSeed() != null ? c.getVersaoCatalogoSeed() : 0)
                        .orElse(0);
        boolean reseedPorVersao = versaoGravada < versaoSeedOficial;
        if (forcarReseedCatalogo) {
            limparDadosParaReseedCatalogo();
            log.warn(
                    "app.catalogo.forcar-reseed ativo: cardápio e lançamentos vinculados foram apagados e serão recriados neste startup.");
        } else if (reseedPorVersao) {
            limparDadosParaReseedCatalogo();
            log.info(
                    "Cardápio: versão seed no banco ({}) menor que app.catalogo.versao-seed-oficial ({}); aplicando cardápio oficial.",
                    versaoGravada,
                    versaoSeedOficial);
        }
        /*
         * Cardápio conforme material oficial (contexto/cardapio.jpg — Espetinho Jurema).
         * A categoria "Espetinhos" deve manter esse nome (fluxo de ponto da carne).
         * Códigos de impressão usam prefixo por seção — no cardápio o número 11 aparece duas vezes
         * (último espetinho e primeira porção).
         */
        if (categoriaJpaRepository.count() == 0) {
            var catEsp = categoria("Espetinhos", 1);
            var catPor = categoria("Porção", 2);
            var catCl = categoria("Cerveja long neck", 3);
            var catLt = categoria("Cerveja lata", 4);
            var catVinho = categoria("Vinhos", 5);
            var catRefri = categoria("Refrigerante", 6);
            var catOut = categoria("Outros", 7);
            var catDoc = categoria("Doces", 8);

            produto("Contra filé", null, new BigDecimal("8.00"), catEsp, "ESP01");
            produto("Frango", null, new BigDecimal("7.00"), catEsp, "ESP02");
            produto("Calabresa", null, new BigDecimal("7.00"), catEsp, "ESP03");
            produto("Lombinho suíno", null, new BigDecimal("7.00"), catEsp, "ESP04");
            produto("Coração", null, new BigDecimal("8.00"), catEsp, "ESP05");
            produto("Medalhão de frango", null, new BigDecimal("9.00"), catEsp, "ESP06");
            produto("Camarão", null, new BigDecimal("9.00"), catEsp, "ESP07");
            produto("Queijo coalho", null, new BigDecimal("9.00"), catEsp, "ESP08");
            produto("Kafta de carne", null, new BigDecimal("8.00"), catEsp, "ESP09");
            produto("Tulipa", null, new BigDecimal("8.00"), catEsp, "ESP10");
            produto("Linguiça artesanal apimentada", null, new BigDecimal("8.00"), catEsp, "ESP11");

            produto("Aimpim (grande)", null, new BigDecimal("12.00"), catPor, "POR01");
            produto("Aimpim (meia porção)", null, new BigDecimal("6.00"), catPor, "POR02");
            produto("Farofa de manteiga", null, new BigDecimal("6.00"), catPor, "POR03");
            produto("Vinagrete", null, new BigDecimal("6.00"), catPor, "POR04");

            produto("Heineken", null, new BigDecimal("10.00"), catCl, "CL01");
            produto("Budweiser", null, new BigDecimal("8.00"), catCl, "CL02");
            produto("Stella Artois puro malte", null, new BigDecimal("10.00"), catCl, "CL03");
            produto("Heineken 0°", null, new BigDecimal("10.00"), catCl, "CL04");

            produto("Brahma Chopp", null, new BigDecimal("5.00"), catLt, "LT01");
            produto("Skol", null, new BigDecimal("5.00"), catLt, "LT02");
            produto("Itaipava", null, new BigDecimal("5.00"), catLt, "LT03");
            produto("Devassa puro malte", null, new BigDecimal("5.00"), catLt, "LT04");
            produto("Brahma duplo malte", null, new BigDecimal("5.00"), catLt, "LT05");
            produto("Amstel", null, new BigDecimal("5.50"), catLt, "LT06");

            produto("Vinho suave", null, new BigDecimal("7.00"), catVinho, "VI01");
            produto("Vinho seco", null, new BigDecimal("7.00"), catVinho, "VI02");

            produto("Coca-Cola lata", null, new BigDecimal("5.00"), catRefri, "RF01");
            produto("Coca-Cola Zero lata", null, new BigDecimal("5.00"), catRefri, "RF02");
            produto("Coca-Cola 1 L", null, new BigDecimal("9.00"), catRefri, "RF03");
            produto("Coca-Cola Zero 1 L", null, new BigDecimal("9.00"), catRefri, "RF04");
            produto("Guaraná Antarctica lata", null, new BigDecimal("5.00"), catRefri, "RF05");
            produto("Guaraná Antarctica 1 L", null, new BigDecimal("9.00"), catRefri, "RF06");
            produto("Fanta lata", null, new BigDecimal("5.00"), catRefri, "RF07");

            produto("Água", null, new BigDecimal("3.00"), catOut, "OU01");
            produto("Água com gás", null, new BigDecimal("4.00"), catOut, "OU02");
            produto("H²O", null, new BigDecimal("6.00"), catOut, "OU03");
            produto("Suco Del Valle", null, new BigDecimal("6.00"), catOut, "OU04");

            produto("Mousse de maracujá", null, new BigDecimal("9.00"), catDoc, "DO01");
            produto("Mousse de limão", null, new BigDecimal("9.00"), catDoc, "DO02");
            produto("Mousse de morango", null, new BigDecimal("9.00"), catDoc, "DO03");
            produto("Trident", null, new BigDecimal("3.00"), catDoc, "DO04");
            produto("Sonho de Valsa", null, new BigDecimal("2.50"), catDoc, "DO05");
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

        garantirConfiguracaoComVersaoCatalogo();
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
        p.setDescricao(desc != null && !desc.isBlank() ? desc.trim() : null);
        p.setPreco(preco);
        p.setCategoria(cat);
        p.setCodigoImpressao(codImpressao);
        p.setAtivo(true);
        p.setSaldoEstoque(100);
        produtoJpaRepository.save(p);
    }

    /**
     * Remove vínculos com {@link ProdutoEntity} e o catálogo, para o seed oficial poder rodar de novo.
     * Mantém {@code pedidos}, mesas, usuários e demais cadastros.
     */
    private void limparDadosParaReseedCatalogo() {
        itemPedidoJpaRepository.deleteAllInBatch();
        pagamentoPedidoJpaRepository.deleteAllInBatch();
        movimentoEstoqueJpaRepository.deleteAllInBatch();
        produtoJpaRepository.deleteAllInBatch();
        categoriaJpaRepository.deleteAllInBatch();
    }

    private void garantirConfiguracaoComVersaoCatalogo() {
        var cfg =
                configuracaoSistemaJpaRepository
                        .findById(ConfiguracaoSistemaEntity.ID_UNICO)
                        .orElseGet(
                                () -> {
                                    var c = new ConfiguracaoSistemaEntity();
                                    c.setId(ConfiguracaoSistemaEntity.ID_UNICO);
                                    c.setEstoqueObrigatorio(false);
                                    return c;
                                });
        cfg.setVersaoCatalogoSeed(versaoSeedOficial);
        configuracaoSistemaJpaRepository.save(cfg);
    }
}
