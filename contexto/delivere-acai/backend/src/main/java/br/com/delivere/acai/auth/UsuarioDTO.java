package br.com.delivere.acai.auth;

/**
 * DTO para listagem de usuários (sem senha).
 */
public class UsuarioDTO {

    private Long id;
    private String username;
    private String setor;

    public UsuarioDTO() {
    }

    public UsuarioDTO(Long id, String username, String setor) {
        this.id = id;
        this.username = username;
        this.setor = setor;
    }

    public static UsuarioDTO from(User user) {
        return new UsuarioDTO(user.getId(), user.getUsername(), user.getSetor());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }
}
