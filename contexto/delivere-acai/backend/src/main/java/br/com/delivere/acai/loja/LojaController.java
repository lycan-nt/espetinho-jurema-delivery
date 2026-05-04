package br.com.delivere.acai.loja;

import br.com.delivere.acai.auth.PermissaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lojas")
@CrossOrigin(origins = "http://localhost:4200")
public class LojaController {

    private final LojaService service;
    private final PermissaoService permissaoService;

    public LojaController(LojaService service, PermissaoService permissaoService) {
        this.service = service;
        this.permissaoService = permissaoService;
    }

    private ResponseEntity<?> requireGestao() {
        if (!permissaoService.hasGestao()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Você não tem permissão para acessar o módulo de gestão."));
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        ResponseEntity<?> forbidden = requireGestao();
        if (forbidden != null) return forbidden;
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscar(@PathVariable String id) {
        ResponseEntity<?> forbidden = requireGestao();
        if (forbidden != null) return forbidden;
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<?> criar(@Valid @RequestBody Loja loja) {
        ResponseEntity<?> forbidden = requireGestao();
        if (forbidden != null) return forbidden;
        try {
            Loja criada = service.criar(loja);
            return ResponseEntity.status(HttpStatus.CREATED).body(criada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable String id, @Valid @RequestBody Loja loja) {
        ResponseEntity<?> forbidden = requireGestao();
        if (forbidden != null) return forbidden;
        try {
            return ResponseEntity.ok(service.atualizar(id, loja));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable String id) {
        ResponseEntity<?> forbidden = requireGestao();
        if (forbidden != null) return forbidden;
        try {
            service.excluir(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
