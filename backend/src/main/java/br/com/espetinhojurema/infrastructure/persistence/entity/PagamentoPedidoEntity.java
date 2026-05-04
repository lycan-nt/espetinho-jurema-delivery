package br.com.espetinhojurema.infrastructure.persistence.entity;

import br.com.espetinhojurema.domain.model.FormaPagamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pedidos_pagamentos")
public class PagamentoPedidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private PedidoEntity pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private FormaPagamento forma;

    /** Valor aplicado ao total da conta. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    /**
     * Quando a forma é dinheiro e o cliente entrega mais que o valor aplicado (para troco).
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal valorRecebidoDinheiro;

    /** Pode ser nulo em registros antigos; novos pagamentos sempre preenchem. */
    @Column
    private Instant criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PedidoEntity getPedido() {
        return pedido;
    }

    public void setPedido(PedidoEntity pedido) {
        this.pedido = pedido;
    }

    public FormaPagamento getForma() {
        return forma;
    }

    public void setForma(FormaPagamento forma) {
        this.forma = forma;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal getValorRecebidoDinheiro() {
        return valorRecebidoDinheiro;
    }

    public void setValorRecebidoDinheiro(BigDecimal valorRecebidoDinheiro) {
        this.valorRecebidoDinheiro = valorRecebidoDinheiro;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }
}
