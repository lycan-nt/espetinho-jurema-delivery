package br.com.delivere.acai.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Verifica permissões do usuário autenticado (ex.: acesso ao módulo de gestão).
 * Gestão: primeiro verifica setor GESTAO no usuário; se setor for null, usa app.gestao.usuarios.
 */
@Service
public class PermissaoService {

    @Value("${app.gestao.usuarios:}")
    private String gestaoUsuariosConfig;

    private Set<String> gestaoUsuarios;
    private final UserRepository userRepository;

    public PermissaoService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private Set<String> getGestaoUsuarios() {
        if (gestaoUsuarios == null) {
            gestaoUsuarios = Arrays.stream(gestaoUsuariosConfig.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toSet());
        }
        return gestaoUsuarios;
    }

    /**
     * Retorna o username do usuário autenticado ou null.
     */
    public String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return null;
        }
        return auth.getName();
    }

    /**
     * True se o usuário atual tem permissão para acessar o módulo de gestão.
     * Usa setor GESTAO do usuário no banco; se setor for null, usa a lista app.gestao.usuarios.
     */
    public boolean hasGestao() {
        String username = getCurrentUsername();
        if (username == null || username.isBlank()) {
            return false;
        }
        return userRepository.findByUsername(username)
                .map(user -> "GESTAO".equals(user.getSetor()))
                .orElse(getGestaoUsuarios().contains(username));
    }
}
