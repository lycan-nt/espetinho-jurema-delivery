package br.com.delivere.acai.produto;

import jakarta.persistence.*;

@Entity
@Table(name = "configuracao", uniqueConstraints = @UniqueConstraint(columnNames = "chave"))
public class Configuracao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String chave;

    @Column(nullable = false, length = 100)
    private String valor;

    public Configuracao() {
    }

    public Configuracao(String chave, String valor) {
        this.chave = chave;
        this.valor = valor != null ? valor : "";
    }

    public Long getId() {
        return id;
    }

    public String getChave() {
        return chave;
    }

    public void setChave(String chave) {
        this.chave = chave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor != null ? valor : "";
    }
}
