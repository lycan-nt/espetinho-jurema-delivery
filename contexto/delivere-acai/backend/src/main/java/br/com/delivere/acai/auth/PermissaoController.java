package br.com.delivere.acai.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:4200")
public class PermissaoController {

    private final PermissaoService permissaoService;

    public PermissaoController(PermissaoService permissaoService) {
        this.permissaoService = permissaoService;
    }

    /**
     * Retorna as permissões do usuário autenticado (para exibir/ocultar menus e guardar rotas).
     */
    @GetMapping("/permissoes")
    public ResponseEntity<Map<String, Boolean>> permissoes() {
        boolean gestao = permissaoService.hasGestao();
        return ResponseEntity.ok(Map.of("gestao", gestao));
    }
}
