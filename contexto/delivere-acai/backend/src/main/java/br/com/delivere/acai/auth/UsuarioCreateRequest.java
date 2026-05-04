package br.com.delivere.acai.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request para criação de usuário (cadastro).
 */
public class UsuarioCreateRequest {

    @NotBlank(message = "Nome de usuário é obrigatório")
    @Size(min = 2, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Use apenas letras, números, ponto, hífen ou underscore")
    private String username;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 4, max = 100)
    private String password;

    @NotBlank(message = "Setor é obrigatório")
    @Pattern(regexp = "^(VENDAS|GESTAO)$", message = "Setor deve ser VENDAS ou GESTAO")
    private String setor;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }
}
