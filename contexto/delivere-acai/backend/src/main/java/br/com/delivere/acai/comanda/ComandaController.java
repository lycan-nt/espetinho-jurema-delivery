package br.com.delivere.acai.comanda;

import br.com.delivere.acai.auth.PermissaoService;
import br.com.delivere.acai.sheets.GoogleSheetsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class ComandaController {

    private final ComandaService service;
    private final GoogleSheetsService googleSheetsService;
    private final PermissaoService permissaoService;

    public ComandaController(ComandaService service, GoogleSheetsService googleSheetsService,
                             PermissaoService permissaoService) {
        this.service = service;
        this.googleSheetsService = googleSheetsService;
        this.permissaoService = permissaoService;
    }

    private ResponseEntity<?> requireGestao() {
        if (!permissaoService.hasGestao()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Você não tem permissão para acessar o módulo de gestão."));
        }
        return null;
    }

    @PostMapping("/comandas")
    public ResponseEntity<Comanda> criar(@Valid @RequestBody Comanda comanda) {
        Comanda criada = service.criar(comanda);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping("/comandas")
    public List<Comanda> listar(@RequestParam(required = false) Boolean abertas) {
        if (Boolean.TRUE.equals(abertas)) {
            return service.listarAbertas();
        }
        return service.listarTodas();
    }

    @GetMapping("/comandas/proximo-identificador")
    public java.util.Map<String, String> proximoIdentificador(@RequestParam TipoComanda tipo) {
        return java.util.Map.of("identificador", service.proximoIdentificador(tipo));
    }

    @GetMapping("/comandas/{id}/itens")
    public java.util.List<ComandaItem> listarItens(@PathVariable Long id) {
        return service.listarItens(id);
    }

    @GetMapping("/comandas/{id}")
    public Comanda buscar(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PutMapping("/comandas/{id}")
    public Comanda atualizar(@PathVariable Long id, @Valid @RequestBody Comanda comanda) {
        return service.atualizar(id, comanda);
    }

    @PatchMapping("/comandas/{id}/cabecalho")
    public ResponseEntity<?> alterarCabecalho(
            @PathVariable Long id,
            @Valid @RequestBody AlterarCabecalhoComandaRequest request) {
        try {
            return ResponseEntity.ok(service.alterarCabecalho(id, request.getTipo(), request.getIdentificador()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/comandas/{id}/remover-item")
    public Comanda removerItem(@PathVariable Long id, @Valid @RequestBody RemoverItemRequest request) {
        return service.removerItem(id, request.getPesoKg(), request.getValorTotal());
    }

    @PatchMapping("/comandas/{id}/fechar")
    public Comanda fechar(@PathVariable Long id, @Valid @RequestBody FecharComandaRequest request) {
        return service.fechar(id, request.getFormaPagamento());
    }

    /**
     * Emite NFC-e em homologação para a comanda fechada.
     * Requer configuração de certificado e emitente (app.nfce.*).
     */
    @PostMapping("/comandas/{id}/nfce/emitir")
    public ResponseEntity<?> emitirNfce(@PathVariable Long id) {
        try {
            Comanda comanda = service.emitirNfce(id);
            return ResponseEntity.ok(comanda);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao emitir NFC-e: " + e.getMessage());
        }
    }

    @GetMapping("/relatorio")
    public ResponseEntity<?> relatorio(
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim) {
        ResponseEntity<?> forbidden = requireGestao();
        if (forbidden != null) return forbidden;
        return ResponseEntity.ok(service.relatorio(dataInicio, dataFim));
    }

    /**
     * Envia o relatório (com os filtros de data que o usuário escolheu na tela) para a planilha de
     * relatórios manuais (app.google.sheets.spreadsheet-id). Não altera a planilha diária do caixa.
     */
    @PostMapping("/relatorio/enviar-google-sheets")
    public ResponseEntity<?> enviarParaGoogleSheets(
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim) {
        ResponseEntity<?> forbidden = requireGestao();
        if (forbidden != null) return forbidden;
        try {
            LocalDate inicio = dataInicio != null ? dataInicio : LocalDate.now();
            LocalDate fim = dataFim != null ? dataFim : LocalDate.now();
            if (fim.isBefore(inicio)) fim = inicio;

            RelatorioDTO relatorio = service.relatorio(inicio, fim);
            googleSheetsService.enviarRelatorio(relatorio);
            return ResponseEntity.ok().body(java.util.Map.of("message", "Relatório enviado para o Google Sheets com sucesso."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao enviar para Google Sheets: " + e.getMessage());
        }
    }
}
