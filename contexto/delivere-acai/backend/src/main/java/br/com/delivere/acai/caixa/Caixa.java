package br.com.delivere.acai.caixa;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "caixa")
public class Caixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valorAbertura;

    @Column(nullable = false)
    private LocalDateTime dataHoraAbertura;

    private LocalDateTime dataHoraFechamento;

    @Column(length = 128)
    private String openedByUsername;

    @Column(length = 128)
    private String closedByUsername;

    @Column(nullable = false, length = 20)
    private String status = "ABERTO";

    /** ID da planilha Google Sheets onde ficam as abas diárias (uma planilha fixa, uma aba por dia). */
    @Column(length = 128)
    private String spreadsheetId;

    /** Nome da aba (tab) do dia nesta planilha (ex.: "2025-02-12"). */
    @Column(length = 64)
    private String sheetName;

    /** Valor informado no fechamento (conferência do caixa). */
    @Column(precision = 12, scale = 2)
    private BigDecimal valorFechamento;

    /** Valor de retirada de caixa (sangria), se houver. */
    @Column(precision = 12, scale = 2)
    private BigDecimal valorRetirada;

    /** True quando for uma reabertura no mesmo dia (caixa já tinha sido fechado). */
    @Column(nullable = false)
    private boolean reabertura = false;

    public Caixa() {
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

    public BigDecimal getValorAbertura() {
        return valorAbertura;
    }

    public void setValorAbertura(BigDecimal valorAbertura) {
        this.valorAbertura = valorAbertura;
    }

    public LocalDateTime getDataHoraAbertura() {
        return dataHoraAbertura;
    }

    public void setDataHoraAbertura(LocalDateTime dataHoraAbertura) {
        this.dataHoraAbertura = dataHoraAbertura;
    }

    public LocalDateTime getDataHoraFechamento() {
        return dataHoraFechamento;
    }

    public void setDataHoraFechamento(LocalDateTime dataHoraFechamento) {
        this.dataHoraFechamento = dataHoraFechamento;
    }

    public String getOpenedByUsername() {
        return openedByUsername;
    }

    public void setOpenedByUsername(String openedByUsername) {
        this.openedByUsername = openedByUsername;
    }

    public String getClosedByUsername() {
        return closedByUsername;
    }

    public void setClosedByUsername(String closedByUsername) {
        this.closedByUsername = closedByUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public void setSpreadsheetId(String spreadsheetId) {
        this.spreadsheetId = spreadsheetId;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public BigDecimal getValorFechamento() {
        return valorFechamento;
    }

    public void setValorFechamento(BigDecimal valorFechamento) {
        this.valorFechamento = valorFechamento;
    }

    public BigDecimal getValorRetirada() {
        return valorRetirada;
    }

    public void setValorRetirada(BigDecimal valorRetirada) {
        this.valorRetirada = valorRetirada;
    }

    public boolean isReabertura() {
        return reabertura;
    }

    public void setReabertura(boolean reabertura) {
        this.reabertura = reabertura;
    }
}
