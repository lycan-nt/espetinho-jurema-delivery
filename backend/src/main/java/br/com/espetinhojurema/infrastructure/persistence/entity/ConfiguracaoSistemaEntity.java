package br.com.espetinhojurema.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracao_sistema")
public class ConfiguracaoSistemaEntity {

    public static final long ID_UNICO = 1L;

    @Id
    private Long id = ID_UNICO;

    @Column(nullable = false)
    private boolean estoqueObrigatorio = false;

    /** Nome da fila CUPS (`lpstat -p`), ex.: térmica USB no Mac. Se vazio, comanda/cupom só no navegador. */
    @Column(name = "nome_impressora_lp", length = 200)
    private String nomeImpressoraLp;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEstoqueObrigatorio() {
        return estoqueObrigatorio;
    }

    public void setEstoqueObrigatorio(boolean estoqueObrigatorio) {
        this.estoqueObrigatorio = estoqueObrigatorio;
    }

    public String getNomeImpressoraLp() {
        return nomeImpressoraLp;
    }

    public void setNomeImpressoraLp(String nomeImpressoraLp) {
        this.nomeImpressoraLp = nomeImpressoraLp;
    }
}
