package br.com.delivere.acai.produto;

import br.com.delivere.acai.auth.PermissaoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * API de produtos/configuração. Preço do kilo (açaí) e configuração da balança - apenas gestores podem alterar.
 * GET preco-kg e GET config-balança são acessíveis para uso na comanda; PUT apenas gestão.
 */
@RestController
@RequestMapping("/api/produtos")
@CrossOrigin(origins = "http://localhost:4200")
public class ProdutoController {

    private static final String CHAVE_PRECO_KG = "preco_kg_acai";
    private static final String CHAVE_BALANCA = "balanca_serial";
    private static final String CONFIG_BALANCA_DEFAULT = "{\"baudRate\":4800,\"serialConfig\":\"8n1\",\"enviarEnq\":true}";

    private final ConfiguracaoRepository configuracaoRepository;
    private final PermissaoService permissaoService;
    private final ObjectMapper objectMapper;

    public ProdutoController(ConfiguracaoRepository configuracaoRepository, PermissaoService permissaoService,
                             ObjectMapper objectMapper) {
        this.configuracaoRepository = configuracaoRepository;
        this.permissaoService = permissaoService;
        this.objectMapper = objectMapper;
    }

    /** Retorna o preço do kilo configurado (para a tela de comanda - qualquer usuário logado). */
    @GetMapping("/preco-kg")
    public ResponseEntity<PrecoKgDTO> getPrecoKg() {
        BigDecimal preco = ProdutoService.getPrecoKg(configuracaoRepository, CHAVE_PRECO_KG);
        return ResponseEntity.ok(new PrecoKgDTO(preco));
    }

    /** Atualiza o preço do kilo. Apenas gestão. */
    @PutMapping("/preco-kg")
    public ResponseEntity<?> putPrecoKg(@Valid @RequestBody PrecoKgDTO body) {
        if (!permissaoService.hasGestao()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Você não tem permissão para acessar o módulo de gestão."));
        }
        ProdutoService.setPrecoKg(configuracaoRepository, CHAVE_PRECO_KG, body.getPrecoPorKilo());
        return ResponseEntity.ok(body);
    }

    /** Retorna a configuração da balança (velocidade, serial, ENQ). Qualquer usuário logado pode ler. */
    @GetMapping("/config-balanca")
    public ResponseEntity<ConfigBalancaDTO> getConfigBalança() {
        String json = configuracaoRepository.findByChave(CHAVE_BALANCA)
                .map(c -> c.getValor() != null && !c.getValor().isBlank() ? c.getValor() : CONFIG_BALANCA_DEFAULT)
                .orElse(CONFIG_BALANCA_DEFAULT);
        try {
            JsonNode node = objectMapper.readTree(json);
            ConfigBalancaDTO dto = new ConfigBalancaDTO();
            dto.setBaudRate(node.has("baudRate") ? node.get("baudRate").asInt(4800) : 4800);
            dto.setSerialConfig(node.has("serialConfig") ? node.get("serialConfig").asText("8n1") : "8n1");
            dto.setEnviarEnq(node.has("enviarEnq") ? node.get("enviarEnq").asBoolean(true) : true);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            ConfigBalancaDTO dto = new ConfigBalancaDTO();
            dto.setBaudRate(4800);
            dto.setSerialConfig("8n1");
            dto.setEnviarEnq(true);
            return ResponseEntity.ok(dto);
        }
    }

    /** Atualiza a configuração da balança. Apenas gestão. */
    @PutMapping("/config-balanca")
    public ResponseEntity<?> putConfigBalança(@Valid @RequestBody ConfigBalancaDTO body) {
        if (!permissaoService.hasGestao()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Você não tem permissão para acessar o módulo de gestão."));
        }
        String json = String.format("{\"baudRate\":%d,\"serialConfig\":\"%s\",\"enviarEnq\":%s}",
                body.getBaudRate(), body.getSerialConfig() != null ? body.getSerialConfig() : "8n1", body.isEnviarEnq());
        configuracaoRepository.findByChave(CHAVE_BALANCA)
                .ifPresentOrElse(c -> { c.setValor(json); configuracaoRepository.save(c); },
                        () -> configuracaoRepository.save(new Configuracao(CHAVE_BALANCA, json)));
        return ResponseEntity.ok(body);
    }

    public static class ConfigBalancaDTO {
        private int baudRate = 4800;
        private String serialConfig = "8n1";
        private boolean enviarEnq = true;

        public int getBaudRate() { return baudRate; }
        public void setBaudRate(int baudRate) { this.baudRate = baudRate; }
        public String getSerialConfig() { return serialConfig; }
        public void setSerialConfig(String serialConfig) { this.serialConfig = serialConfig != null ? serialConfig : "8n1"; }
        public boolean isEnviarEnq() { return enviarEnq; }
        public void setEnviarEnq(boolean enviarEnq) { this.enviarEnq = enviarEnq; }
    }

    public static class PrecoKgDTO {
        @NotNull
        @DecimalMin("0")
        private java.math.BigDecimal precoPorKilo;

        public PrecoKgDTO() {
            this.precoPorKilo = BigDecimal.ZERO;
        }

        public PrecoKgDTO(java.math.BigDecimal precoPorKilo) {
            this.precoPorKilo = precoPorKilo != null ? precoPorKilo : BigDecimal.ZERO;
        }

        public java.math.BigDecimal getPrecoPorKilo() {
            return precoPorKilo;
        }

        public void setPrecoPorKilo(java.math.BigDecimal precoPorKilo) {
            this.precoPorKilo = precoPorKilo != null ? precoPorKilo : BigDecimal.ZERO;
        }
    }
}
