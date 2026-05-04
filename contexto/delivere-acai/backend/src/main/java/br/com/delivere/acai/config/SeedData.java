package br.com.delivere.acai.config;

import br.com.delivere.acai.auth.User;
import br.com.delivere.acai.auth.UserRepository;
import br.com.delivere.acai.loja.Loja;
import br.com.delivere.acai.loja.LojaRepository;
import br.com.delivere.acai.produto.Configuracao;
import br.com.delivere.acai.produto.ConfiguracaoRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria usuários iniciais, configuração de preço do kilo e loja de teste se não existirem.
 */
@Component
public class SeedData implements ApplicationRunner {

    private static final String CHAVE_PRECO_KG = "preco_kg_acai";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfiguracaoRepository configuracaoRepository;
    private final LojaRepository lojaRepository;

    public SeedData(UserRepository userRepository, PasswordEncoder passwordEncoder,
                    ConfiguracaoRepository configuracaoRepository,
                    LojaRepository lojaRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuracaoRepository = configuracaoRepository;
        this.lojaRepository = lojaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        criarSeNaoExistir("mix", "mix", "VENDAS");
        criarSeNaoExistir("paulo", "mixacai", "GESTAO");
        garantirGestores();
        criarPrecoKgSeNaoExistir();
        criarLojaTesteSeNaoExistir();
    }

    private void criarLojaTesteSeNaoExistir() {
        criarLojaSe("123", "Mix Açaí Loja 1", "Rua Exemplo, 100 - Salvador, BA", "Felipe D. Santos");
        criarLojaSe("456", "Mix Açaí Loja 2", "Rua Exemplo, 200 - Salvador, BA", "Felipe D. Santos");
    }

    private void criarLojaSe(String id, String nome, String endereco, String responsavel) {
        if (lojaRepository.existsById(id)) return;
        Loja loja = new Loja();
        loja.setId(id);
        loja.setNome(nome);
        loja.setEndereco(endereco);
        loja.setResponsavel(responsavel);
        lojaRepository.save(loja);
    }

    private void criarPrecoKgSeNaoExistir() {
        if (configuracaoRepository.findByChave(CHAVE_PRECO_KG).isEmpty()) {
            configuracaoRepository.save(new Configuracao(CHAVE_PRECO_KG, "0"));
        }
    }

    private void criarSeNaoExistir(String username, String password, String setor) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = new User(username, passwordEncoder.encode(password), "ROLE_USER", setor);
        userRepository.save(user);
    }

    /** Garante que paulo tenha setor GESTAO (para quem já existia no banco antes do campo setor). */
    private void garantirGestores() {
        userRepository.findByUsername("paulo").ifPresent(user -> {
            if (user.getSetor() == null || user.getSetor().isBlank()) {
                user.setSetor("GESTAO");
                userRepository.save(user);
            }
        });
    }
}
