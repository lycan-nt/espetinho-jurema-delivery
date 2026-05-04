package br.com.delivere.acai.caixa;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/caixa")
@CrossOrigin(origins = "http://localhost:4200")
public class CaixaController {

    private final CaixaService service;

    public CaixaController(CaixaService service) {
        this.service = service;
    }

    /**
     * Status do caixa hoje: precisa abrir, está aberto, ou já foi fechado (reabertura).
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        boolean needsAbertura = service.precisaAberturaHoje();
        var caixaAberto = service.getCaixaAbertoHoje();
        boolean caixaFechadoHoje = service.caixaFechadoHoje();
        Map<String, Object> map = new HashMap<>();
        map.put("needsAbertura", needsAbertura);
        map.put("caixaAberto", caixaAberto.isPresent());
        map.put("caixaFechadoHoje", caixaFechadoHoje);
        map.put("caixa", caixaAberto.orElse(null));
        return map;
    }

    /**
     * Abre o caixa do dia com o valor inicial em caixa. Cria a planilha do dia para envio automático.
     */
    @PostMapping("/abrir")
    public ResponseEntity<?> abrir(@Valid @RequestBody AbrirCaixaRequest request) {
        try {
            Caixa caixa = service.abrir(request.getValorAbertura());
            return ResponseEntity.status(HttpStatus.CREATED).body(caixa);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao abrir caixa: " + e.getMessage());
        }
    }

    /**
     * Fecha o caixa do dia. Informe o valor de fechamento (conferência). Envia relatório final para a planilha.
     */
    @PostMapping("/fechar")
    public ResponseEntity<?> fechar(@Valid @RequestBody FecharCaixaRequest request) {
        try {
            Caixa caixa = service.fechar(request.getValorFechamento(), request.getValorRetirada());
            return ResponseEntity.ok(caixa);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao fechar caixa: " + e.getMessage());
        }
    }
}
