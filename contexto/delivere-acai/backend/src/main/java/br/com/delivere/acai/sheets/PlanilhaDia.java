package br.com.delivere.acai.sheets;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Associa uma data ao ID da planilha do Google Sheets criada para aquele dia (modo "uma planilha por dia").
 */
@Entity
@Table(name = "planilha_dia", uniqueConstraints = @UniqueConstraint(columnNames = "data"))
public class PlanilhaDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate data;

    @Column(nullable = false, length = 128)
    private String spreadsheetId;

    public PlanilhaDia() {
    }

    public PlanilhaDia(LocalDate data, String spreadsheetId) {
        this.data = data;
        this.spreadsheetId = spreadsheetId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public void setSpreadsheetId(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
    }
}
