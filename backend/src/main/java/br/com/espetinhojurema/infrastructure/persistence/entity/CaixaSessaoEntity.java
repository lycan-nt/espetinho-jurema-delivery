package br.com.espetinhojurema.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "caixa_sessoes")
public class CaixaSessaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean aberto = true;

    @Column(nullable = false)
    private Instant abertoEm = Instant.now();

    private Instant fechadoEm;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoAbertura = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal saldoFechamento;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isAberto() {
        return aberto;
    }

    public void setAberto(boolean aberto) {
        this.aberto = aberto;
    }

    public Instant getAbertoEm() {
        return abertoEm;
    }

    public void setAbertoEm(Instant abertoEm) {
        this.abertoEm = abertoEm;
    }

    public Instant getFechadoEm() {
        return fechadoEm;
    }

    public void setFechadoEm(Instant fechadoEm) {
        this.fechadoEm = fechadoEm;
    }

    public BigDecimal getSaldoAbertura() {
        return saldoAbertura;
    }

    public void setSaldoAbertura(BigDecimal saldoAbertura) {
        this.saldoAbertura = saldoAbertura;
    }

    public BigDecimal getSaldoFechamento() {
        return saldoFechamento;
    }

    public void setSaldoFechamento(BigDecimal saldoFechamento) {
        this.saldoFechamento = saldoFechamento;
    }
}
