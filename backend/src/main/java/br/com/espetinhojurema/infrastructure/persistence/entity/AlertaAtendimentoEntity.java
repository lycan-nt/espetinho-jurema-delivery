package br.com.espetinhojurema.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "alertas_atendimento")
public class AlertaAtendimentoEntity {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "mesa_numero", nullable = false)
    private Integer mesaNumero;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    private Instant reconhecidoEm;

    @Column(length = 120)
    private String reconhecidoPor;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Long pedidoId) {
        this.pedidoId = pedidoId;
    }

    public Integer getMesaNumero() {
        return mesaNumero;
    }

    public void setMesaNumero(Integer mesaNumero) {
        this.mesaNumero = mesaNumero;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getReconhecidoEm() {
        return reconhecidoEm;
    }

    public void setReconhecidoEm(Instant reconhecidoEm) {
        this.reconhecidoEm = reconhecidoEm;
    }

    public String getReconhecidoPor() {
        return reconhecidoPor;
    }

    public void setReconhecidoPor(String reconhecidoPor) {
        this.reconhecidoPor = reconhecidoPor;
    }
}
