package br.com.espetinhojurema.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "mesa_transferencias")
public class MesaTransferenciaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id")
    private PedidoEntity pedido;

    @Column(nullable = false)
    private Long mesaOrigemId;

    @Column(nullable = false)
    private Long mesaDestinoId;

    @Column(nullable = false)
    private int mesaOrigemNumero;

    @Column(nullable = false)
    private int mesaDestinoNumero;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    @Column(length = 64)
    private String usuarioLogin;

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

    public Long getMesaOrigemId() {
        return mesaOrigemId;
    }

    public void setMesaOrigemId(Long mesaOrigemId) {
        this.mesaOrigemId = mesaOrigemId;
    }

    public Long getMesaDestinoId() {
        return mesaDestinoId;
    }

    public void setMesaDestinoId(Long mesaDestinoId) {
        this.mesaDestinoId = mesaDestinoId;
    }

    public int getMesaOrigemNumero() {
        return mesaOrigemNumero;
    }

    public void setMesaOrigemNumero(int mesaOrigemNumero) {
        this.mesaOrigemNumero = mesaOrigemNumero;
    }

    public int getMesaDestinoNumero() {
        return mesaDestinoNumero;
    }

    public void setMesaDestinoNumero(int mesaDestinoNumero) {
        this.mesaDestinoNumero = mesaDestinoNumero;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public String getUsuarioLogin() {
        return usuarioLogin;
    }

    public void setUsuarioLogin(String usuarioLogin) {
        this.usuarioLogin = usuarioLogin;
    }
}
