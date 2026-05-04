package br.com.delivere.acai.loja;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * Cadastro de loja (unidade). id, nome, endereco, responsavel.
 * O id é o identificador da loja (igual ao app.loja.id de cada instalação).
 */
@Entity
@Table(name = "loja")
public class Loja {

    @Id
    @Column(length = 32, nullable = false)
    @NotBlank(message = "ID da loja é obrigatório")
    private String id;

    @Column(nullable = false, length = 120)
    @NotBlank(message = "Nome da loja é obrigatório")
    private String nome;

    @Column(length = 255)
    private String endereco;

    @Column(length = 120)
    private String responsavel;

    public Loja() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public String getResponsavel() {
        return responsavel;
    }

    public void setResponsavel(String responsavel) {
        this.responsavel = responsavel;
    }
}
