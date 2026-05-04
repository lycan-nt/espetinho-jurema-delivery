package br.com.delivere.acai.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API de usuários (cadastro). Apenas gestores podem listar e criar.
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:4200")
public class UsuarioController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissaoService permissaoService;

    public UsuarioController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                             PermissaoService permissaoService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        List<UsuarioDTO> list = userRepository.findAll().stream()
                .map(UsuarioDTO::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<?> criar(@Valid @RequestBody UsuarioCreateRequest request) {
        ResponseEntity<?> forbidden = requireGestao();
        if (forbidden != null) return forbidden;
        String username = request.getUsername() != null ? request.getUsername().trim().toLowerCase() : "";
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Já existe um usuário com este nome."));
        }
        User user = new User(username, passwordEncoder.encode(request.getPassword()), "ROLE_USER", request.getSetor());
        user = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioDTO.from(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateRequest request) {
        ResponseEntity<?> forbidden = requireGestao();
        if (forbidden != null) return forbidden;
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        String username = request.getUsername() != null ? request.getUsername().trim().toLowerCase() : "";
        if (username.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nome de usuário é obrigatório."));
        }
        if (!user.getUsername().equals(username) && userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Já existe um usuário com este nome."));
        }
        user.setUsername(username);
        user.setSetor(request.getSetor());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user = userRepository.save(user);
        return ResponseEntity.ok(UsuarioDTO.from(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        ResponseEntity<?> forbidden = requireGestao();
        if (forbidden != null) return forbidden;
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        String current = permissaoService.getCurrentUsername();
        if (current != null && current.equalsIgnoreCase(user.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Você não pode excluir seu próprio usuário."));
        }
        userRepository.delete(user);
        return ResponseEntity.noContent().build();
    }
}
