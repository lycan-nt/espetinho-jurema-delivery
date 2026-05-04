package br.com.delivere.acai.gestaoapp;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Documento no MongoDB para login do app (independente do backend).
 * Sincronizado a partir dos usuários do sistema (H2). O app usa a Data API para findOne por username e valida a senha localmente (bcrypt).
 */
@Document(collection = "app_usuarios")
public class AppUsuarioDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;
    private String passwordHash;
    private String setor;

    public AppUsuarioDocument() {
    }

    public AppUsuarioDocument(String username, String passwordHash, String setor) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.setor = setor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }
}
