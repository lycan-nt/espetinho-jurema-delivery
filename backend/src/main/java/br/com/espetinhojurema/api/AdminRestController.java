package br.com.espetinhojurema.api;

import br.com.espetinhojurema.api.dto.AtualizarProdutoRequest;
import br.com.espetinhojurema.api.dto.AtualizarUsuarioRequest;
import br.com.espetinhojurema.api.dto.CriarProdutoRequest;
import br.com.espetinhojurema.api.dto.CriarUsuarioRequest;
import br.com.espetinhojurema.api.dto.UsuarioAdminView;
import br.com.espetinhojurema.application.model.ProdutoView;
import br.com.espetinhojurema.application.service.AdminCadastrosService;
import br.com.espetinhojurema.infrastructure.security.UsuarioPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ATENDIMENTO')")
public class AdminRestController {

    private final AdminCadastrosService adminCadastrosService;

    public AdminRestController(AdminCadastrosService adminCadastrosService) {
        this.adminCadastrosService = adminCadastrosService;
    }

    @GetMapping("/usuarios")
    public List<UsuarioAdminView> listarUsuarios() {
        return adminCadastrosService.listarUsuarios();
    }

    @PostMapping("/usuarios")
    public UsuarioAdminView criarUsuario(@Valid @RequestBody CriarUsuarioRequest body) {
        return adminCadastrosService.criarUsuario(body);
    }

    @PutMapping("/usuarios/{id}")
    public UsuarioAdminView atualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody AtualizarUsuarioRequest body,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        return adminCadastrosService.atualizarUsuario(id, body, principal.getUsuario().getId());
    }

    @GetMapping("/produtos")
    public List<ProdutoView> listarProdutos() {
        return adminCadastrosService.listarTodosProdutos();
    }

    @PostMapping("/produtos")
    public ProdutoView criarProduto(@Valid @RequestBody CriarProdutoRequest body) {
        return adminCadastrosService.criarProduto(body);
    }

    @PutMapping("/produtos/{id}")
    public ProdutoView atualizarProduto(@PathVariable Long id, @Valid @RequestBody AtualizarProdutoRequest body) {
        return adminCadastrosService.atualizarProduto(id, body);
    }
}
