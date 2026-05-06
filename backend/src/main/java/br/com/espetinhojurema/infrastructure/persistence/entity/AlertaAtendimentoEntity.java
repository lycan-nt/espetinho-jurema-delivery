package br.com.espetinhojurema.infrastructure.persistence.entity;

import br.com.espetinhojurema.domain.model.TipoAlertaAtendimento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    /**
     * Bases antigas podem ter NULL após DDL — tratar como {@link TipoAlertaAtendimento#COMANDA_ENVIADA}.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 48)
    private TipoAlertaAtendimento tipo;

    /**
     * Snapshot do maior item_id presente no pedido no momento em que a comanda foi enviada.
     * Permite que a próxima comanda imprima apenas os itens adicionados depois deste ponto.
     * {@code null} para alertas que não sejam {@code COMANDA_ENVIADA}.
     */
    @Column(name = "item_id_max")
    private Long itemIdMax;

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

    public TipoAlertaAtendimento getTipo() {
        return tipo;
    }

    public void setTipo(TipoAlertaAtendimento tipo) {
        this.tipo = tipo;
    }

    public Long getItemIdMax() {
        return itemIdMax;
    }

    public void setItemIdMax(Long itemIdMax) {
        this.itemIdMax = itemIdMax;
    }
}
