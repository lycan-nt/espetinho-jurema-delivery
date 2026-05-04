package br.com.espetinhojurema.api;

import br.com.espetinhojurema.api.dto.LoginRequest;
import br.com.espetinhojurema.api.dto.LoginResponse;
import br.com.espetinhojurema.api.dto.UsuarioLogadoResponse;
import br.com.espetinhojurema.infrastructure.security.JwtService;
import br.com.espetinhojurema.infrastructure.security.UsuarioPrincipal;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long expirationSeconds;

    public AuthRestController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${app.jwt.expiration-minutes:480}") long expirationMinutes) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.expirationSeconds = expirationMinutes * 60;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.login().trim(), request.senha()));
            var principal = (UsuarioPrincipal) auth.getPrincipal();
            var u = principal.getUsuario();
            String token = jwtService.gerarToken(u.getLogin(), u.getPerfil(), u.getNomeExibicao());
            return new LoginResponse(token, "Bearer", expirationSeconds, u.getNomeExibicao(), u.getPerfil(), u.getLogin());
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Login ou senha inválidos");
        }
    }

    @GetMapping("/me")
    public UsuarioLogadoResponse me(@AuthenticationPrincipal UsuarioPrincipal principal) {
        var u = principal.getUsuario();
        return new UsuarioLogadoResponse(u.getLogin(), u.getNomeExibicao(), u.getPerfil());
    }
}
