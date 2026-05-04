package br.com.espetinhojurema.infrastructure.persistence.entity;

import br.com.espetinhojurema.domain.model.MesaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "mesas", uniqueConstraints = @UniqueConstraint(columnNames = "numero"))
public class MesaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MesaStatus status = MesaStatus.LIVRE;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public MesaStatus getStatus() {
        return status;
    }

    public void setStatus(MesaStatus status) {
        this.status = status;
    }
}
